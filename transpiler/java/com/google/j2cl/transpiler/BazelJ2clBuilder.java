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

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.common.bazel.BazelWorker;
import com.google.j2cl.transpiler.backend.Backend;
import com.google.j2cl.transpiler.frontend.Frontend;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

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
      usage = "Specifies the zip into which to place compiled output.")
  protected String output;

  @Option(
      name = "-libraryinfooutput",
      metaVar = "<path>",
      usage = "Specifies the file into which to place the call graph.")
  protected String libraryInfoOutput;

  @Option(name = "-readablelibraryinfo", hidden = true)
  protected boolean readableLibraryInfo = false;

  @Option(name = "-readablesourcemaps", hidden = true)
  protected boolean readableSourceMaps = false;

  @Option(name = "-generatekytheindexingmetadata", hidden = true)
  protected boolean generateKytheIndexingMetadata = false;

  @Option(
      name = "-experimentaloptimizeautovalue",
      usage = "Enables experomental optimizations for AutoValue. Not production ready.",
      hidden = true)
  protected boolean experimentalOptimizeAutovalue = false;

  /** Temporary flag to select the frontend during the transition to javac. */
  private static final Frontend FRONTEND =
      Frontend.valueOf(Ascii.toUpperCase(System.getProperty("j2cl.frontend", "jdt")));

  @Option(
      name = "-experimentalBackend",
      metaVar = "(CLOSURE | WASM)",
      usage = "Select the backend to use: CLOSURE (default), WASM (experimental).",
      hidden = true)
  protected Backend backend = Backend.CLOSURE;

  @Override
  protected void run(Problems problems) {
    J2clTranspiler.transpile(createOptions(problems), problems);
  }

  private J2clTranspilerOptions createOptions(Problems problems) {

    if (this.readableSourceMaps && this.generateKytheIndexingMetadata) {
      problems.warning(
          "Readable source maps are not available when generating Kythe indexing metadata.");
      this.readableSourceMaps = false;
    }

    Path outputPath = getZipOutput(this.output, problems);

    Path libraryInfoOutputPath = null;
    if (backend == Backend.CLOSURE) {
      if (libraryInfoOutput == null) {
        problems.fatal(FatalError.LIBRARY_INFO_OUTPUT_ARG_MISSING);
      }

      libraryInfoOutputPath = Paths.get(this.libraryInfoOutput);
    }

    List<FileInfo> allSources =
        SourceUtils.getAllSources(this.sources, problems).collect(ImmutableList.toImmutableList());

    List<FileInfo> allJavaSources =
        allSources.stream()
            .filter(p -> p.sourcePath().endsWith(".java"))
            .collect(ImmutableList.toImmutableList());

    List<FileInfo> allNativeSources =
        allSources.stream()
            .filter(p -> p.sourcePath().endsWith(".native.js"))
            .collect(ImmutableList.toImmutableList());

    // Directly put all supplied js sources into the zip file.
    allSources.stream()
        .filter(p -> p.sourcePath().endsWith(".js") && !p.sourcePath().endsWith("native.js"))
        .forEach(
            f ->
                J2clUtils.copyFile(
                    Paths.get(f.sourcePath()), outputPath.resolve(f.targetPath()), problems));

    return J2clTranspilerOptions.newBuilder()
        .setSources(allJavaSources)
        .setNativeSources(allNativeSources)
        .setClasspaths(getPathEntries(this.classPath))
        .setOutput(outputPath)
        .setLibraryInfoOutput(libraryInfoOutputPath)
        .setEmitReadableLibraryInfo(readableLibraryInfo)
        .setEmitReadableSourceMap(this.readableSourceMaps)
        .setGenerateKytheIndexingMetadata(this.generateKytheIndexingMetadata)
        .setExperimentalOptimizeAutovalue(this.experimentalOptimizeAutovalue)
        .setFrontend(FRONTEND)
        .setBackend(this.backend)
        .build();
  }

  private static Path getZipOutput(String output, Problems problems) {
    return SourceUtils.initZipOutput(output, problems).getPath("/");
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
