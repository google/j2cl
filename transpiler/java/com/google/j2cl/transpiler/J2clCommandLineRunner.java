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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.CommandLineTool;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.backend.Backend;
import com.google.j2cl.transpiler.frontend.Frontend;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
      name = "-system",
      metaVar = "<path>",
      usage = "Specifies the location of the system modules.")
  String system = "";

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

  @Option(
      name = "-libraryinfooutput",
      metaVar = "<path>",
      usage = "Specifies the file into which to place the call graph.")
  Path libraryInfoOutput;

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
  Frontend frontend = null;

  @Option(
      name = "-backend",
      metaVar = "(CLOSURE | WASM | KOTLIN)",
      usage =
          "Select the backend to use: CLOSURE (default), WASM (experimental), KOTLIN"
              + " (experimental).",
      hidden = true)
  Backend backend = Backend.CLOSURE;

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
      name = "-javacOptions",
      metaVar = "<option>",
      usage = "Options to pass to Javac.",
      hidden = true)
  List<String> javacOptions = new ArrayList<>();

  @Option(name = "-kotlincOptions", hidden = true)
  List<String> kotlincOptions = new ArrayList<>();

  @Option(name = "-generateWasmExport", hidden = true)
  List<String> wasmEntryPoints = new ArrayList<>();

  @Option(name = "-forbiddenAnnotation", hidden = true)
  List<String> forbiddenAnnotations = new ArrayList<>();

  @Option(name = "-defineForWasm", handler = MapOptionHandler.class, hidden = true)
  Map<String, String> definesForWasm = new HashMap<>();

  @Option(name = "-objCNamePrefix", hidden = true)
  String objCNamePrefix = "J2kt";

  private J2clCommandLineRunner() {
    super("j2cl");
  }

  @VisibleForTesting
  J2clCommandLineRunner(Problems problems) {
    super("j2cl", problems);
  }

  @VisibleForTesting
  void executeForTesting(Collection<String> args) {
    var unused = super.execute(args, System.out);
  }

  @Override
  protected void run() {
    problems.abortIfCancelled();
    try (Output out = OutputUtils.initOutput(this.output, problems)) {
      problems.abortIfCancelled();
      J2clTranspiler.transpile(createOptions(out), problems);
    }
  }

  private J2clTranspilerOptions createOptions(Output output) {
    checkSourceFiles(
        problems,
        files.stream().map(Path::of).collect(toImmutableList()),
        ".java",
        ".srcjar",
        ".jar",
        ".kt");

    if (this.frontend == null) {
      this.frontend = this.backend.getDefaultFrontend();
    }

    if (this.readableSourceMaps && this.generateKytheIndexingMetadata) {
      problems.warning(
          "Readable source maps are not available when generating Kythe indexing metadata.");
      this.readableSourceMaps = false;
    }

    ImmutableList<FileInfo> allSources =
        SourceUtils.getAllSources(
                this.files.stream().map(Path::of), tempDir.resolve("_source_jars"), problems)
            .collect(toImmutableList());
    problems.abortIfCancelled();

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

    ImmutableList<FileInfo> allNativeSources =
        SourceUtils.getAllSources(
                getPathEntries(this.nativeSourcePath).stream(),
                tempDir.resolve("_naitve_sources"),
                problems)
            .filter(p -> p.sourcePath().endsWith(".native.js"))
            .collect(toImmutableList());
    problems.abortIfCancelled();

    return J2clTranspilerOptions.newBuilder()
        .setSources(allKotlinSources.isEmpty() ? allJavaSources : allKotlinSources)
        .setNativeSources(allNativeSources)
        .setClasspaths(getPathEntries(this.classPath))
        .setSystem(this.system.isEmpty() ? null : Path.of(this.system))
        .setOutput(output)
        .setLibraryInfoOutput(this.libraryInfoOutput)
        .setEmitReadableLibraryInfo(false)
        .setEmitReadableSourceMap(this.readableSourceMaps)
        .setOptimizeAutoValue(this.optimizeAutoValue)
        .setGenerateKytheIndexingMetadata(this.generateKytheIndexingMetadata)
        .setFrontend(this.frontend)
        .setBackend(this.backend)
        .setWasmEntryPointStrings(wasmEntryPoints)
        .setDefinesForWasm(definesForWasm)
        .setNullMarkedSupported(this.enableJSpecifySupport)
        .setJavacOptions(javacOptions)
        .setKotlincOptions(kotlincOptions)
        .setForbiddenAnnotations(forbiddenAnnotations)
        .setDependencyKlibs(ImmutableList.of())
        .setFriendKlibs(ImmutableList.of())
        .setEnableKlibs(false)
        .setObjCNamePrefix("J2kt")
        .build(problems);
  }

  private static List<Path> getPathEntries(String path) {
    List<Path> entries = new ArrayList<>();
    for (String entry : Splitter.on(File.pathSeparatorChar).omitEmptyStrings().split(path)) {
      if (new File(entry).exists()) {
        entries.add(Path.of(entry));
      }
    }
    return entries;
  }

  /**
   * Entry point to programmatically run the transpiler.
   *
   * <p>Note: J2CL has no static state, but rather uses thread local variables. Because of this, the
   * compiler should be invoked on a different thread each time called from the same process.
   */
  public static int run(Collection<String> args, PrintStream stdErr) {
    return new J2clCommandLineRunner().execute(args, stdErr);
  }

  public static void main(String[] args) {
    System.exit(run(Arrays.asList(args), System.err));
  }
}
