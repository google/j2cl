/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.tools.bundler;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.io.Files;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.bazel.BazelWorker;
import com.google.j2cl.common.bazel.FileCache;
import com.google.j2cl.transpiler.backend.wasm.Summary;
import com.google.j2cl.transpiler.backend.wasm.TypeHierarchyInfo;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/** Runs The J2wasmBundler as a worker. */
final class BazelJ2wasmBundler extends BazelWorker {

  private static final int CACHE_SIZE =
      Integer.parseInt(System.getProperty("j2cl.bundler.cachesize", "5000"));

  private static final FileCache<String> moduleContentsCache =
      new FileCache<>(BazelJ2wasmBundler::readModule, CACHE_SIZE);

  private static final FileCache<Summary> summaryCache =
      new FileCache<>(BazelJ2wasmBundler::readSummary, CACHE_SIZE);

  @Argument(required = true, usage = "The list of modular output directories", multiValued = true)
  List<String> inputs = null;

  @Option(
      name = "-output",
      required = true,
      metaVar = "<path>",
      usage = "Directory or zip into which to place the bundled output.")
  Path output;

  @Override
  protected void run(Problems problems) {
    createBundle(problems);
  }

  private void createBundle(Problems problems) {
    var typeGraph = new TypeGraph();
    var classes = typeGraph.build(getSummaries());

    ImmutableList<String> moduleContents =
        Streams.concat(
                Stream.of("(rec"),
                getModuleParts("types"),
                Stream.of(typeGraph.getTopLevelItableStructDeclaration()),
                classes.stream().map(TypeGraph.Type::getItableStructDeclaration),
                Stream.of(")"),
                getModuleParts("data"),
                getModuleParts("globals"),
                classes.stream().map(TypeGraph.Type::getItableInitialization),
                getModuleParts("functions"))
            .collect(toImmutableList());

    writeToFile(output.toString(), moduleContents, problems);
  }

  /** Represents the inheritance structure of the whole application. */
  private static class TypeGraph {

    private static final int NO_TYPE_INDEX = 0;
    // The list of all interfaces.
    private final List<Type> interfaces = new ArrayList<>();

    public Collection<Type> build(Stream<Summary> summaries) {
      Map<String, Type> typesByName = new LinkedHashMap<>();
      List<Type> classes = new ArrayList<>();

      // Collect all types from all summaries.
      summaries.forEachOrdered(
          summary -> {
            for (TypeHierarchyInfo typeHierarchyInfo : summary.getTypeList()) {
              String name = summary.getDeclaredTypeMap(typeHierarchyInfo.getDeclaredTypeId());
              Type type = typesByName.computeIfAbsent(name, Type::new);
              classes.add(type);
              if (typeHierarchyInfo.getExtendsDeclaredTypeId() != NO_TYPE_INDEX) {
                String superTypeName =
                    summary.getDeclaredTypeMap(typeHierarchyInfo.getExtendsDeclaredTypeId());
                type.superType = checkNotNull(typesByName.get(superTypeName));
              }
              for (int interfaceId : typeHierarchyInfo.getImplementsDeclaredTypeIdList()) {
                String interfaceName = summary.getDeclaredTypeMap(interfaceId);
                Type interfaceType =
                    typesByName.computeIfAbsent(
                        interfaceName,
                        n -> {
                          var newInterfaceType = new Type(n);
                          // Add the interface to the global list of interfaces, where the index in
                          // the list corresponds to the slot in the itable.
                          interfaces.add(newInterfaceType);
                          return newInterfaceType;
                        });
                type.implementedInterfaces.add(interfaceType);
              }
            }
          });

      return classes;
    }

    /** Emits the top-level itable struct. */
    String getTopLevelItableStructDeclaration() {
      StringBuilder sb = new StringBuilder();
      sb.append("(type $itable (sub \n");
      // In the unoptimized itables each interface has its own slot.
      for (Type i : interfaces) {
        sb.append(format("  (field $%s (ref null struct)\n", i.name));
      }
      sb.append("))\n");
      return sb.toString();
    }

    private class Type {
      private final String name;
      private Type superType;
      private final Set<Type> implementedInterfaces = new HashSet<>();

      public Type(String name) {
        this.name = name;
      }

      /** Emits the itable struct type for a class. */
      public String getItableStructDeclaration() {
        StringBuilder sb = new StringBuilder();
        sb.append(
            format(
                "(type %s.itable (sub $%s \n",
                name, superType != null ? superType.name + ".itable" : "itable"));

        for (Type i : interfaces) {
          sb.append(
              format(
                  "  (field $%s (ref %s))\n",
                  i.name, implementsInterface(i) ? i.name : "null struct"));
        }
        sb.append("))\n");
        return sb.toString();
      }

      public String getItableInitialization() {
        StringBuilder sb = new StringBuilder();
        sb.append(
            format("(global %s.itable (ref %s.itable) (struct.new %s.itable \n", name, name, name));

        for (Type ifce : interfaces) {
          sb.append(format("  %s\n", implementsInterface(ifce) ? ifce.name : "(ref.null struct)"));
        }
        sb.append("))\n");
        return sb.toString();
      }

      private boolean implementsInterface(Type type) {
        return implementedInterfaces.contains(type)
            || (superType != null && superType.implementsInterface(type));
      }
    }
  }

  private Stream<Summary> getSummaries() {
    return inputs.stream()
        .map(d -> format("%s/summary.binpb", d))
        .filter(n -> new File(n).exists())
        .map(summaryCache::get);
  }

  private Stream<String> getModuleParts(String name) {
    return inputs.stream()
        .map(d -> format("%s/%s.wat", d, name))
        .filter(n -> new File(n).exists())
        .map(BazelJ2wasmBundler.moduleContentsCache::get);
  }

  private static Summary readSummary(Path summaryPath) throws IOException {
    try (InputStream inputStream = java.nio.file.Files.newInputStream(summaryPath)) {
      return Summary.parseFrom(inputStream);
    }
  }

  private static String readModule(Path modulePath) throws IOException {
    return java.nio.file.Files.readString(modulePath);
  }

  private static void writeToFile(String filePath, List<String> contents, Problems problems) {
    try {
      Files.asCharSink(new File(filePath), UTF_8).writeLines(contents);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
    }
  }

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelJ2wasmBundler::new);
  }
}
