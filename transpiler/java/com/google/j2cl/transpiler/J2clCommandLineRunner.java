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

import static com.google.j2cl.common.FrontendUtils.checkSourceFiles;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.CommandLineTool;
import com.google.j2cl.common.FrontendUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.frontend.Frontend;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/** A javac-like command line driver for J2clTranspiler. */
public final class J2clCommandLineRunner extends CommandLineTool {

  @Argument(metaVar = "<source files>", required = true)
  protected List<String> files = new ArrayList<>();

  @Option(
      name = "-classpath",
      aliases = "-cp",
      metaVar = "<path>",
      usage = "Specifies where to find user class files and annotation processors.")
  protected String classPath = "";

  @Option(
      name = "-nativesourcepath",
      metaVar = "<path>",
      usage = "Specifies where to find zip files containing native.js files for native methods.")
  protected String nativeSourcePath = "";

  @Option(
      name = "-d",
      metaVar = "<path>",
      usage = "Directory or zip into which to place compiled output.")
  @VisibleForTesting
  public String output = ".";

  @Option(
      name = "-readablesourcemaps",
      usage = "Coerces generated source maps to human readable form.",
      hidden = true)
  protected boolean readableSourceMaps = false;

  @Option(
      name = "-generatekytheindexingmetadata",
      usage =
          "Generates Kythe indexing metadata and appends it onto the generated JavaScript files.",
      hidden = true)
  protected boolean generateKytheIndexingMetadata = false;

  @Option(
      name = "-frontend",
      metaVar = "(JDT | JAVAC)",
      usage = "Select the frontend to use: JDT (default), JAVAC (experimental).",
      hidden = true)
  protected Frontend frontEnd = Frontend.JDT;

  private J2clCommandLineRunner() {
    super("j2cl");
  }

  @Override
  protected Problems run() {
    return J2clTranspiler.transpile(createOptions());
  }

  private J2clTranspilerOptions createOptions() {
    checkSourceFiles(files, ".java", ".srcjar", ".jar");

    Problems problems = new Problems();
    if (this.readableSourceMaps && this.generateKytheIndexingMetadata) {
      problems.warning(
          "Readable source maps are not available when generating Kythe indexing metadata.");
      this.readableSourceMaps = false;
    }

    return J2clTranspilerOptions.newBuilder()
        .setSources(
            FrontendUtils.getAllSources(this.files, problems)
                .filter(p -> p.sourcePath().endsWith(".java"))
                .collect(ImmutableList.toImmutableList()))
        .setNativeSources(
            FrontendUtils.getAllSources(getPathEntries(this.nativeSourcePath), problems)
                .filter(p -> p.sourcePath().endsWith(".native.js"))
                .collect(ImmutableList.toImmutableList()))
        .setClasspaths(getPathEntries(this.classPath))
        .setOutput(
            this.output.endsWith(".zip")
                ? getZipOutput(this.output, problems)
                : getDirOutput(this.output, problems))
        .setEmitReadableSourceMap(this.readableSourceMaps)
        .setEmitReadableLibraryInfo(false)
        .setGenerateKytheIndexingMetadata(this.generateKytheIndexingMetadata)
        .setFrontend(this.frontEnd)
        .build();
  }

  private static Path getDirOutput(String output, Problems problems) {
    Path outputPath = Paths.get(output);
    if (Files.isRegularFile(outputPath)) {
      problems.fatal(FatalError.OUTPUT_LOCATION, outputPath);
    }
    return outputPath;
  }

  private static Path getZipOutput(String output, Problems problems) {
    FileSystem newFileSystem = FrontendUtils.initZipOutput(output, problems);
    return newFileSystem == null ? null : newFileSystem.getPath("/");
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
