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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.CommandLineParser;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.common.bazel.BazelWorker;
import com.google.j2cl.transpiler.backend.Backend;
import com.google.j2cl.transpiler.frontend.Frontend;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.MapOptionHandler;

/**
 * The J2cl builder for Bazel that runs as a worker.
 *
 * <p>Bazel workers allow J2CL to run in a JVM that is not being terminated after every compile and
 * thus gain significant speedups.
 */
final class BazelJ2clBuilder extends BazelWorker {

  @Argument(
      metaVar = "<source files>",
      required = true,
      usage = "Specifies individual files and jars/zips of sources (.java, .js, .native.js).")
  List<Path> sources = new ArrayList<>();

  @Option(
      name = "-classpath",
      required = true,
      metaVar = "<path>",
      usage = "Specifies where to find user class files and annotation processors.",
      handler = CommandLineParser.MultiPathOptionHandler.class)
  List<Path> classpaths;

  @Option(
      name = "-system",
      metaVar = "<path>",
      usage = "Specifies the location of the system modules.")
  Path system;

  @Option(
      name = "-output",
      required = true,
      metaVar = "<path>",
      usage = "Directory or zip into which to place compiled output.")
  Path output;

  @Option(
      name = "-targetLabel",
      metaVar = "<target>",
      usage =
          "The Bazel target label that is being transpiled. Used for determining context-dependent"
              + " behavior, like Kotlin friendship semantics.")
  String targetLabel = null;

  @Option(
      name = "-libraryinfooutput",
      metaVar = "<path>",
      usage = "Specifies the file into which to place the call graph.")
  Path libraryInfoOutput;

  @Option(name = "-readablelibraryinfo", hidden = true)
  boolean readableLibraryInfo = false;

  @Option(name = "-readablesourcemaps", hidden = true)
  boolean readableSourceMaps = false;

  @Option(name = "-sourceMappingPathPrefix", hidden = true)
  String sourceMappingPathPrefix = "";

  @Option(name = "-generatekytheindexingmetadata", hidden = true)
  boolean generateKytheIndexingMetadata = false;

  @Option(
      name = "-optimizeautovalue",
      usage = "Enables optimizations of AutoValue types.",
      hidden = true)
  boolean optimizeAutoValue = false;

  @Option(
      name =
          "-experimentalenablejspecifysupportdonotenablewithoutjspecifystaticcheckingoryoumightcauseanoutage",
      usage =
          "Enables support for JSpecify semantics. Do not use if the code is not being actually"
              + " separately checked by a static checker. When these annotations applied the code"
              + " will mislead developers to think that a null check is unnecessary without the"
              + " compile time validation. Such misleading code has caused outages in the past for"
              + " products.",
      hidden = true)
  boolean enableJSpecifySupport = false;

  @Option(
      name = "-experimentalJavaFrontend",
      usage = "Select the java frontend to use: JDT (default), JAVAC (experimental).",
      hidden = true)
  Frontend javaFrontend = null;

  @Option(
      name = "-experimentalBackend",
      usage =
          "Select the backend to use: CLOSURE (default), WASM (experimental), "
              + "WASM_MODULAR (experimental), KOTLIN (experimental).",
      hidden = true)
  Backend backend = Backend.CLOSURE;

  @Option(
      name = "-javacOptions",
      metaVar = "<option>",
      usage = "Options to pass to Javac.",
      hidden = true)
  List<String> javacOptions = new ArrayList<>();

  @Option(name = "-kotlincOptions", hidden = true)
  List<String> kotlincOptions = new ArrayList<>();

  @Option(name = "-experimentalGenerateWasmExport", hidden = true)
  List<String> wasmEntryPoints = new ArrayList<>();

  @Option(
      name = "-experimentalEnableWasmCustomDescriptors",
      usage = "Enables generating custom descriptors for Wasm.",
      hidden = true)
  boolean enableWasmCustomDescriptors = false;

  @Option(
      name = "-experimentalEnableWasmCustomDescriptorsJsInterop",
      usage =
          "Enables JsInterop with custom descriptors for Wasm. Setting this also enables general"
              + " custom descriptors functionality.",
      hidden = true)
  boolean enableWasmCustomDescriptorsJsInterop = false;

  @Option(name = "-forbiddenAnnotation", hidden = true)
  List<String> forbiddenAnnotations = new ArrayList<>();

  @Option(
      name = "-klibs",
      metaVar = "<path>",
      usage = "Paths to cross-platform libraries in the .klib format.",
      handler = CommandLineParser.MultiPathOptionHandler.class)
  List<Path> dependencyKlibs = new ArrayList<>();

  @Option(
      name = "-friendKlibs",
      metaVar = "<path>",
      usage = "Paths to cross-platform libraries in the .klib format.",
      handler = CommandLineParser.MultiPathOptionHandler.class)
  List<Path> friendKlibs = new ArrayList<>();

  @Option(name = "-experimentalEnableKlibs", usage = "Enable using klibs for the kotlin frontend.")
  boolean enableKlibs = false;

  @Option(name = "-experimentalDefineForWasm", handler = MapOptionHandler.class, hidden = true)
  Map<String, String> definesForWasm = new HashMap<>();

  @Option(name = "-objCNamePrefix", hidden = true)
  String objCNamePrefix = "J2kt";

  @Override
  protected void run() {
    problems.abortIfCancelled();
    try (Output out = OutputUtils.initOutput(output, problems)) {
      problems.abortIfCancelled();
      try {
        J2clTranspiler.transpile(createOptions(out), problems);
      } catch (RuntimeException | Error e) {
        // Cancel the writing of the output files since we don't need them anymore.
        out.cancel();
        throw e;
      }
    }
  }

  private J2clTranspilerOptions createOptions(Output output) {

    if (this.readableSourceMaps && this.generateKytheIndexingMetadata) {
      problems.warning(
          "Readable source maps are not available when generating Kythe indexing metadata.");
      this.readableSourceMaps = false;
    }

    if (javaFrontend == null) {
      javaFrontend = backend.getDefaultFrontend();
    }

    if (!javaFrontend.isJavaFrontend()) {
      problems.fatal(FatalError.INVALID_JAVA_FRONTEND, javaFrontend);
    }

    Path sourceJarDir = SourceUtils.deriveDirectory(this.output, "_source_jars");
    ImmutableList<FileInfo> allSources =
        SourceUtils.getAllSources(sources.stream(), sourceJarDir, problems)
            .collect(toImmutableList());
    problems.abortIfCancelled();

    ImmutableList<FileInfo> allJavaSources =
        allSources.stream()
            .filter(p -> p.sourcePath().endsWith(".java"))
            .collect(toImmutableList());

    ImmutableList<FileInfo> allKotlinSources =
        allSources.stream().filter(p -> p.sourcePath().endsWith(".kt")).collect(toImmutableList());

    if (!allJavaSources.isEmpty() && !allKotlinSources.isEmpty()) {
      throw new AssertionError(
          "Transpilation of Java and Kotlin files together is not supported yet.");
    }

    ImmutableList<FileInfo> allNativeSources =
        allSources.stream()
            .filter(p -> p.sourcePath().endsWith(".native.js"))
            .collect(toImmutableList());

    // Directly put all supplied js sources into the zip file.
    allSources.stream()
        .filter(p -> p.sourcePath().endsWith(".js") && !p.sourcePath().endsWith(".native.js"))
        .forEach(f -> output.copyFile(f.sourcePath(), f.targetPath()));
    problems.abortIfCancelled();

    return J2clTranspilerOptions.newBuilder()
        .setSources(
            ImmutableList.<FileInfo>builder()
                .addAll(allJavaSources)
                .addAll(allKotlinSources)
                .build())
        .setNativeSources(allNativeSources)
        .setClasspaths(this.classpaths)
        .setSystem(this.system)
        .setOutput(output)
        .setTargetLabel(targetLabel)
        .setLibraryInfoOutput(libraryInfoOutput)
        .setEmitReadableLibraryInfo(readableLibraryInfo)
        .setEmitReadableSourceMap(this.readableSourceMaps)
        .setSourceMappingPathPrefix(this.sourceMappingPathPrefix)
        .setOptimizeAutoValue(this.optimizeAutoValue)
        .setGenerateKytheIndexingMetadata(this.generateKytheIndexingMetadata)
        .setFrontend(allKotlinSources.isEmpty() ? javaFrontend : Frontend.KOTLIN)
        .setBackend(this.backend)
        .setWasmEntryPointStrings(this.wasmEntryPoints)
        .setEnableWasmCustomDescriptors(this.enableWasmCustomDescriptors)
        .setEnableWasmCustomDescriptorsJsInterop(this.enableWasmCustomDescriptorsJsInterop)
        .setDefinesForWasm(definesForWasm)
        .setNullMarkedSupported(this.enableJSpecifySupport)
        .setJavacOptions(javacOptions)
        .setKotlincOptions(kotlincOptions)
        .setForbiddenAnnotations(forbiddenAnnotations)
        .setEnableKlibs(enableKlibs)
        .setDependencyKlibs(dependencyKlibs)
        .setFriendKlibs(friendKlibs)
        .setObjCNamePrefix(objCNamePrefix)
        .build(problems);
  }

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelJ2clBuilder::new);
  }
}
