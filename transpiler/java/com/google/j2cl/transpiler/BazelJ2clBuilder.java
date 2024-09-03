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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.common.bazel.BazelWorker;
import com.google.j2cl.transpiler.backend.Backend;
import com.google.j2cl.transpiler.frontend.Frontend;
import java.io.File;
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
  List<String> sources = new ArrayList<>();

  @Option(
      name = "-classpath",
      required = true,
      metaVar = "<path>",
      usage = "Specifies where to find user class files and annotation processors.")
  String classPath;

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
  Frontend javaFrontend = Frontend.JDT;

  @Option(
      name = "-experimentalBackend",
      usage =
          "Select the backend to use: CLOSURE (default), WASM (experimental), "
              + "WASM_MODULAR (experimental), KOTLIN (experimental).",
      hidden = true)
  Backend backend = Backend.CLOSURE;

  @Option(name = "-kotlincOptions", hidden = true)
  List<String> kotlincOptions = new ArrayList<>();

  @Option(name = "-experimentalGenerateWasmExport", hidden = true)
  List<String> wasmEntryPoints = new ArrayList<>();

  @Option(name = "-forbiddenAnnotation", hidden = true)
  List<String> forbiddenAnnotations = new ArrayList();

  @Option(name = "-experimentalDefineForWasm", handler = MapOptionHandler.class, hidden = true)
  Map<String, String> definesForWasm = new HashMap<>();

  @Override
  protected void run(Problems problems) {
    try (Output out = OutputUtils.initOutput(this.output, problems)) {
      J2clTranspiler.transpile(createOptions(out, problems), problems);
    }
  }

  private J2clTranspilerOptions createOptions(Output output, Problems problems) {

    if (this.readableSourceMaps && this.generateKytheIndexingMetadata) {
      problems.warning(
          "Readable source maps are not available when generating Kythe indexing metadata.");
      this.readableSourceMaps = false;
    }

    if (backend == Backend.CLOSURE && libraryInfoOutput == null) {
      problems.fatal(FatalError.LIBRARY_INFO_OUTPUT_ARG_MISSING);
    }

    if (!javaFrontend.isJavaFrontend()) {
      problems.fatal(FatalError.INVALID_JAVA_FRONTEND, javaFrontend);
    }

    ImmutableList<FileInfo> allSources =
        SourceUtils.getAllSources(this.sources, problems).collect(toImmutableList());

    ImmutableList<FileInfo> allJavaSources =
        allSources.stream()
            .filter(p -> p.sourcePath().endsWith(".java"))
            .collect(toImmutableList());

    ImmutableList<FileInfo> allKotlinSources =
        allSources.stream().filter(p -> p.sourcePath().endsWith(".kt")).collect(toImmutableList());

    // For now, only package-info.java files are allowed with Kotlin sources.
    if (allJavaSources.stream().anyMatch(f -> !f.sourcePath().endsWith("package-info.java"))
        && !allKotlinSources.isEmpty()) {
      throw new AssertionError(
          "Transpilation of Java and Kotlin files together is not supported yet.");
    }

    ImmutableList<FileInfo> allNativeSources =
        allSources.stream()
            .filter(p -> p.sourcePath().endsWith(".native.js"))
            .collect(toImmutableList());

    // Directly put all supplied js sources into the zip file.
    allSources.stream()
        .filter(p -> p.sourcePath().endsWith(".js") && !p.sourcePath().endsWith("native.js"))
        .forEach(f -> output.copyFile(f.sourcePath(), f.targetPath()));

    return J2clTranspilerOptions.newBuilder()
        .setSources(
            ImmutableList.<FileInfo>builder()
                .addAll(allJavaSources)
                .addAll(allKotlinSources)
                .build())
        .setNativeSources(allNativeSources)
        .setClasspaths(getPathEntries(this.classPath))
        .setOutput(output)
        .setTargetLabel(targetLabel)
        .setLibraryInfoOutput(this.libraryInfoOutput)
        .setEmitReadableLibraryInfo(readableLibraryInfo)
        .setEmitReadableSourceMap(this.readableSourceMaps)
        .setSourceMappingPathPrefix(this.sourceMappingPathPrefix)
        .setGenerateKytheIndexingMetadata(this.generateKytheIndexingMetadata)
        .setOptimizeAutoValue(this.optimizeAutoValue)
        .setFrontend(allKotlinSources.isEmpty() ? javaFrontend : Frontend.KOTLIN)
        .setBackend(this.backend)
        .setWasmEntryPointStrings(ImmutableList.copyOf(this.wasmEntryPoints))
        .setDefinesForWasm(ImmutableMap.copyOf(definesForWasm))
        .setNullMarkedSupported(this.enableJSpecifySupport)
        .setKotlincOptions(ImmutableList.copyOf(kotlincOptions))
        .setForbiddenAnnotations(ImmutableList.copyOf(forbiddenAnnotations))
        .build(problems);
  }

  private static List<String> getPathEntries(String path) {
    List<String> entries = new ArrayList<>();
    for (String entry : Splitter.on(File.pathSeparatorChar).omitEmptyStrings().split(path)) {
      entries.add(entry);
    }
    return entries;
  }

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelJ2clBuilder::new);
  }
}
