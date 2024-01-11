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
import static java.util.function.Predicate.not;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.j2cl.common.EntryPointPattern;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.bazel.BazelWorker;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.WasmEntryPointBridgesCreator;
import com.google.j2cl.transpiler.backend.wasm.WasmGeneratorStage;
import com.google.j2cl.transpiler.frontend.jdt.JdtEnvironment;
import com.google.j2cl.transpiler.frontend.jdt.JdtParser;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.kohsuke.args4j.Option;

/** The J2wasm export generator for Bazel that runs as a worker. */
final class BazelJ2wasmExportsGenerator extends BazelWorker {

  @Option(
      name = "-classpath",
      required = true,
      metaVar = "<path>",
      usage = "Specifies where to find all the class files for the application.")
  String classPath;

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

  private static final Splitter PATH_SPLITTER = Splitter.on(File.pathSeparatorChar);

  @Override
  protected void run(Problems problems) {
    MethodDescriptor.setWasmManglingPatterns();
    try (Output out = OutputUtils.initOutput(this.output, problems)) {
      ImmutableList<EntryPointPattern> entryPointPatterns =
          this.wasmEntryPoints.stream().map(EntryPointPattern::from).collect(toImmutableList());
      List<String> binaryNames =
          getBinaryNamesOfClassesWithExports(
              PATH_SPLITTER.split(this.classPath), entryPointPatterns, problems);
      List<String> classPathEntries =
          Splitter.on(File.pathSeparatorChar).splitToList(this.classPath);

      // Create a parser just to resolve binary names, with no sources to parse.
      // TODO(b/294284380): Make this independent of the frontend.
      JdtParser parser = new JdtParser(classPathEntries, problems);
      Set<String> wellKnownTypeNames = TypeDescriptors.getWellKnownTypeNames();
      binaryNames.addAll(wellKnownTypeNames);
      var bindings =
          parser.resolveBindings(binaryNames).stream()
              // Methods in annotations can not be exported, and additionally the bindings might
              // not be complete and cannot be fully resolved to descriptors.
              .filter(not(ITypeBinding::isAnnotation))
              .collect(toImmutableList());
      var environment = new JdtEnvironment(parser, wellKnownTypeNames);

      var typeDescriptors = environment.createDescriptorsFromBindings(bindings);

      var entryPointBridgeCreator = new WasmEntryPointBridgesCreator(entryPointPatterns, problems);

      List<Method> exportedMethods =
          entryPointBridgeCreator.generateBridges(
              typeDescriptors.stream()
                  .flatMap(t -> t.getDeclaredMethodDescriptors().stream())
                  .collect(toImmutableList()));

      WasmGeneratorStage.generateWasmExportMethods(exportedMethods, out, problems);
      problems.abortIfHasErrors();
    }
  }

  private static List<String> getBinaryNamesOfClassesWithExports(
      Iterable<String> classPathEntries,
      List<EntryPointPattern> wasmEntryPoints,
      Problems problems) {

    List<URL> classPathUrls = new ArrayList<>();
    List<String> binaryClassNames = new ArrayList<>();
    for (String classPathEntry : classPathEntries) {
      try {
        classPathUrls.add(new File(classPathEntry).toURI().toURL());
      } catch (MalformedURLException e) {
        problems.fatal(FatalError.CANNOT_OPEN_FILE, e.toString());
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
