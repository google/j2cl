/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.j2cl.common.CommandLineParser;
import com.google.j2cl.common.EntryPointPattern;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.bazel.BazelWorker;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.WasmEntryPointBridgesCreator;
import com.google.j2cl.transpiler.backend.wasm.WasmGeneratorStage;
import com.google.j2cl.transpiler.frontend.javac.JavacParser;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import org.kohsuke.args4j.Option;

/** The J2wasm export generator for Bazel that runs as a worker. */
final class BazelJ2wasmExportsGenerator extends BazelWorker {

  @Option(
      name = "-classpath",
      required = true,
      metaVar = "<path>",
      usage = "Specifies where to find all the class files for the application.",
      handler = CommandLineParser.MultiPathOptionHandler.class)
  List<Path> classpaths;

  @Option(
      name = "-system",
      metaVar = "<path>",
      usage = "Specifies where to find the system modules.")
  Path system;

  @Option(
      name = "-output",
      required = true,
      metaVar = "<path>",
      usage = "File that is output with the exports.")
  Path output;

  @Option(
      name = "-entryPointPattern",
      usage = "A pattern describing entry points to the Wasm module.")
  List<String> wasmEntryPoints = new ArrayList<>();

  @Override
  protected void run() {
    try (Output out = OutputUtils.initOutputForBazel(this.output, problems)) {
      var entryPointPatterns =
          this.wasmEntryPoints.stream().map(EntryPointPattern::from).collect(toImmutableList());

      // TODO(b/294284380): Make this independent of the frontend.
      // Create a parser just to resolve binary names, with no sources to parse.
      var environment = JavacParser.createEnvironment(classpaths, system, problems);
      var typeDescriptors =
          getBinaryNamesOfClassesWithExports(classpaths, entryPointPatterns).stream()
              .map(environment::createTypeDescriptor)
              .filter(Predicates.notNull())
              .filter(Predicate.not(DeclaredTypeDescriptor::isAnnotation))
              .collect(toImmutableList());

      var entryPointBridgeCreator = new WasmEntryPointBridgesCreator(entryPointPatterns, problems);

      var exportedMethods =
          entryPointBridgeCreator.generateBridges(
              typeDescriptors.stream()
                  .flatMap(t -> t.getDeclaredMethodDescriptors().stream())
                  .collect(toImmutableList()));

      WasmGeneratorStage.generateWasmExportMethods(exportedMethods, out, problems);
      problems.abortIfHasErrors();
    }
  }

  private List<String> getBinaryNamesOfClassesWithExports(
      Collection<Path> classPathEntries, List<EntryPointPattern> wasmEntryPoints) {

    List<URL> classPathUrls = new ArrayList<>();
    List<String> binaryClassNames = new ArrayList<>();
    for (Path classPathEntry : classPathEntries) {
      try {
        classPathUrls.add(classPathEntry.toUri().toURL());
      } catch (MalformedURLException e) {
        problems.fatal(FatalError.CANNOT_OPEN_FILE, e.getMessage());
      }
    }

    // Create a parent-less classloader to make sure it does not load anything from the classpath
    // the compiler is running with.
    URLClassLoader resourcesClassLoader =
        new URLClassLoader(Iterables.toArray(classPathUrls, URL.class), null);

    try {
      ClassPath classPath = ClassPath.from(resourcesClassLoader);
      for (ClassInfo classInfo : classPath.getAllClasses()) {

        String qualifiedSourceName = classInfo.getName().replace('$', '.');

        if (wasmEntryPoints.stream().anyMatch(e -> e.matchesClass(qualifiedSourceName))) {
          binaryClassNames.add(classInfo.getName());
        }
      }
    } catch (IOException e) {
      problems.error("Couldn't load classpath");
    }
    return binaryClassNames;
  }

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelJ2wasmExportsGenerator::new);
  }
}
