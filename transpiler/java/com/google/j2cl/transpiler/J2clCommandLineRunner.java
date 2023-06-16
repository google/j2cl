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
import static com.google.j2cl.common.SourceUtils.checkSourceFiles;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.CommandLineTool;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.backend.Backend;
import com.google.j2cl.transpiler.frontend.Frontend;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.MapOptionHandler;

/** A javac-like command line driver for J2clTranspiler. */
public final class J2clCommandLineRunner extends CommandLineTool {

  @Argument(metaVar = "<source files>", required = true)
  List<String> files = new ArrayList<>();

  @Option(
      name = "-classpath",
      aliases = "-cp",
      metaVar = "<path>",
      usage = "Specifies where to find user class files and annotation processors.")
  String classPath = "";

  @Option(
      name = "-nativesourcepath",
      metaVar = "<path>",
      usage = "Specifies where to find zip files containing native.js files for native methods.")
  String nativeSourcePath = "";

  @Option(
      name = "-d",
      metaVar = "<path>",
      usage = "Directory or zip into which to place compiled output.")
  Path output = Paths.get(".");

  @Option(name = "-optimizeautovalue", usage = "Enables optimizations of AutoValue types.")
  boolean optimizeAutoValue = true;

  @Option(
      name = "-readablesourcemaps",
      usage = "Coerces generated source maps to human readable form.",
      hidden = true)
  boolean readableSourceMaps = false;

  @Option(
      name = "-generatekytheindexingmetadata",
      usage =
          "Generates Kythe indexing metadata and appends it onto the generated JavaScript files.",
      hidden = true)
  boolean generateKytheIndexingMetadata = false;

  @Option(
      name = "-frontend",
      metaVar = "(JDT | JAVAC)",
      usage = "Select the frontend to use: JDT (default), JAVAC (experimental).",
      hidden = true)
  Frontend frontEnd = Frontend.JDT;

  @Option(
      name = "-backend",
      metaVar = "(CLOSURE | WASM | KOTLIN)",
      usage =
          "Select the backend to use: CLOSURE (default), WASM (experimental), KOTLIN"
              + " (experimental).",
      hidden = true)
  Backend backend = Backend.CLOSURE;

  @Option(name = "-kotlincOptions", hidden = true)
  List<String> kotlincOptions = new ArrayList<>();

  @Option(name = "-generateWasmExport", hidden = true)
  List<String> wasmEntryPoints = new ArrayList<>();

  @Option(name = "-defineForWasm", handler = MapOptionHandler.class, hidden = true)
  Map<String, String> definesForWasm = new HashMap<>();

  private J2clCommandLineRunner() {
    super("j2cl");
  }

  @Override
  protected void run(Problems problems) {
    try (Output out = OutputUtils.initOutput(this.output, problems)) {
      J2clTranspiler.transpile(createOptions(out, problems), problems);
    }
  }

  private J2clTranspilerOptions createOptions(Output output, Problems problems) {
    checkSourceFiles(problems, files, ".java", ".srcjar", ".jar", ".kt");

    if (this.readableSourceMaps && this.generateKytheIndexingMetadata) {
      problems.warning(
          "Readable source maps are not available when generating Kythe indexing metadata.");
      this.readableSourceMaps = false;
    }

    ImmutableList<FileInfo> allSources =
        SourceUtils.getAllSources(this.files, problems).collect(toImmutableList());

    ImmutableList<FileInfo> allJavaSources =
        allSources.stream()
            .filter(p -> p.sourcePath().endsWith(".java"))
            .collect(toImmutableList());

    ImmutableList<FileInfo> allKotlinSources =
        allSources.stream().filter(p -> p.sourcePath().endsWith(".kt")).collect(toImmutableList());

    // TODO(b/226952880): add support for transpiling java and kotlin simultaneously.
    if (!allJavaSources.isEmpty() && !allKotlinSources.isEmpty()) {
      throw new AssertionError(
          "Transpilation of Java and Kotlin files together is not supported yet.");
    }

    return J2clTranspilerOptions.newBuilder()
        .setSources(allKotlinSources.isEmpty() ? allJavaSources : allKotlinSources)
        .setNativeSources(
            SourceUtils.getAllSources(getPathEntries(this.nativeSourcePath), problems)
                .filter(p -> p.sourcePath().endsWith(".native.js"))
                .collect(toImmutableList()))
        .setClasspaths(getPathEntries(this.classPath))
        .setOutput(output)
        .setEmitReadableSourceMap(this.readableSourceMaps)
        .setEmitReadableLibraryInfo(false)
        .setOptimizeAutoValue(this.optimizeAutoValue)
        .setGenerateKytheIndexingMetadata(this.generateKytheIndexingMetadata)
        .setFrontend(this.frontEnd)
        .setKotlincOptions(ImmutableList.copyOf(kotlincOptions))
        .setBackend(this.backend)
        .setWasmEntryPointStrings(ImmutableList.copyOf(wasmEntryPoints))
        .setDefinesForWasm(ImmutableMap.copyOf(definesForWasm))
        .build(problems);
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

  // Exists for testing, should be removed when tests stop using flags.
  static Problems runForTest(String[] args) {
    return new J2clCommandLineRunner().processRequest(args);
  }

  public static int run(String[] args) {
    return new J2clCommandLineRunner().execute(args);
  }

  public static void main(String[] args) {
    System.exit(run(args));
  }
}
