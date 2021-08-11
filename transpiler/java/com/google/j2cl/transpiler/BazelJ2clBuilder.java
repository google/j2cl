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

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
  protected List<String> sources = new ArrayList<>();

  @Option(
      name = "-classpath",
      required = true,
      metaVar = "<path>",
      usage = "Specifies where to find user class files and annotation processors.")
  protected String classPath;

  @Option(
      name = "-output",
      required = true,
      metaVar = "<path>",
      usage = "Directory or zip into which to place compiled output.")
  protected Path output;

  @Option(
      name = "-libraryinfooutput",
      metaVar = "<path>",
      usage = "Specifies the file into which to place the call graph.")
  protected Path libraryInfoOutput;

  @Option(name = "-readablelibraryinfo", hidden = true)
  protected boolean readableLibraryInfo = false;

  @Option(name = "-readablesourcemaps", hidden = true)
  protected boolean readableSourceMaps = false;

  @Option(name = "-generatekytheindexingmetadata", hidden = true)
  protected boolean generateKytheIndexingMetadata = false;

  @Option(
      name = "-experimentaloptimizeautovalue",
      usage = "Enables experimental optimizations for AutoValue. Not production ready.",
      hidden = true)
  protected boolean experimentalOptimizeAutovalue = false;

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
  protected boolean enableJSpecifySupport = false;

  /** Temporary flag to select the frontend during the transition to javac. */
  private static final Frontend FRONTEND =
      Frontend.valueOf(Ascii.toUpperCase(System.getProperty("j2cl.frontend", "jdt")));

  @Option(
      name = "-experimentalBackend",
      metaVar = "(CLOSURE | WASM | KOTLIN)",
      usage =
          "Select the backend to use: CLOSURE (default), WASM (experimental), KOTLIN"
              + " (experimental).",
      hidden = true)
  protected Backend backend = Backend.CLOSURE;

  @Option(name = "-experimentalGenerateWasmExport", hidden = true)
  protected List<String> wasmEntryPoints = new ArrayList<>();

  @Option(name = "-experimentalDefineForWasm", handler = MapOptionHandler.class, hidden = true)
  Map<String, String> definesForWasm = new HashMap<>();

  @Option(name = "-experimentalWasmRemoveAssertStatement", hidden = true)
  protected boolean wasmRemoveAssertStatement = false;

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

    List<FileInfo> allSources =
        SourceUtils.getAllSources(this.sources, problems).collect(toImmutableList());

    List<FileInfo> allJavaSources =
        allSources.stream()
            .filter(p -> p.sourcePath().endsWith(".java"))
            .collect(toImmutableList());

    List<FileInfo> allNativeSources =
        allSources.stream()
            .filter(p -> p.sourcePath().endsWith(".native.js"))
            .collect(toImmutableList());

    // Directly put all supplied js sources into the zip file.
    allSources.stream()
        .filter(p -> p.sourcePath().endsWith(".js") && !p.sourcePath().endsWith("native.js"))
        .forEach(f -> output.copyFile(f.sourcePath(), f.targetPath()));

    return J2clTranspilerOptions.newBuilder()
        .setSources(allJavaSources)
        .setNativeSources(allNativeSources)
        .setClasspaths(getPathEntries(this.classPath))
        .setOutput(output)
        .setLibraryInfoOutput(this.libraryInfoOutput)
        .setEmitReadableLibraryInfo(readableLibraryInfo)
        .setEmitReadableSourceMap(this.readableSourceMaps)
        .setGenerateKytheIndexingMetadata(this.generateKytheIndexingMetadata)
        .setExperimentalOptimizeAutovalue(this.experimentalOptimizeAutovalue)
        .setFrontend(FRONTEND)
        .setBackend(this.backend)
        .setWasmEntryPoints(ImmutableSet.copyOf(wasmEntryPoints))
        .setDefinesForWasm(ImmutableMap.copyOf(definesForWasm))
        .setWasmRemoveAssertStatement(wasmRemoveAssertStatement)
        .setNullMarkedSupported(this.enableJSpecifySupport)
        .build();
  }

  private static List<String> getPathEntries(String path) {
    List<String> entries = new ArrayList<>();
    for (String entry : Splitter.on(File.pathSeparatorChar).omitEmptyStrings().split(path)) {
      if (new File(entry).exists()) {
        entries.add(entry);
      }
    }
    return entries;
  }

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelJ2clBuilder::new);
  }
}
