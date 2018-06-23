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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.frontend.FrontendUtils;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/** A javac-like command line driver for J2clTranspiler. */
public final class J2clCommandLineRunner {

  private static class Flags {
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

    @Option(name = "-help", usage = "print this message")
    protected boolean help = false;

    @Option(
        name = "-readablesourcemaps",
        usage = "Coerces generated source maps to human readable form.",
        hidden = true)
    protected boolean readableSourceMaps = false;

    @Option(
        name = "-declarelegacynamespaces",
        usage =
            "Enable goog.module.declareLegacyNamespace() for generated goog.module()."
                + " For Docs during onboarding, do not use.",
        hidden = true)
    protected boolean declareLegacyNamespaces = false;

    @Option(
        name = "-generatekytheindexingmetadata",
        usage =
            "Generates Kythe indexing metadata and appends it onto the generated JavaScript files.",
        hidden = true)
    protected boolean generateKytheIndexingMetadata = false;

    /** Parses the given args list and updates values. */
    private static J2clTranspilerOptions parse(String[] args) {
      Flags flags = new Flags();
      CmdLineParser parser = new CmdLineParser(flags);
      Problems problems = new Problems();

      final String usage = "Usage: j2cl <options> <source files>";

      try {
        parser.parseArgument(args);
      } catch (CmdLineException e) {
        if (!flags.help) {
          String message = e.getMessage() + "\n";
          message += usage + "\n";
          message += "use -help for a list of possible options";
          problems.error(message);
          problems.abort();
        }
      }

      if (flags.help) {
        String message = usage + "\n";
        message += "where possible options include:\n";
        message += J2clUtils.streamToString(parser::printUsage);
        problems.info(message);
        problems.abort();
      }

      return createOptions(flags, problems);
    }

    private static J2clTranspilerOptions createOptions(Flags flags, Problems problems) {
      checkSourceFiles(flags.files, problems);

      if (flags.readableSourceMaps && flags.generateKytheIndexingMetadata) {
        problems.warning(
            "Readable source maps are not available when generating Kythe indexing metadata.");
        flags.readableSourceMaps = false;
      }

      return J2clTranspilerOptions.newBuilder()
          .setSources(
              FrontendUtils.getAllSources(flags.files, problems)
                  .filter(p -> p.sourcePath().endsWith(".java"))
                  .collect(ImmutableList.toImmutableList()))
          .setNativeSources(
              FrontendUtils.getAllSources(getPathEntries(flags.nativeSourcePath), problems)
                  .filter(p -> p.sourcePath().endsWith(".native.js"))
                  .collect(ImmutableList.toImmutableList()))
          .setClasspaths(getPathEntries(flags.classPath))
          .setOutput(
              flags.output.endsWith(".zip")
                  ? getZipOutput(flags.output, problems)
                  : getDirOutput(flags.output, problems))
          .setEmitReadableSourceMap(flags.readableSourceMaps)
          .setDeclareLegacyNamespace(flags.declareLegacyNamespaces)
          .setGenerateKytheIndexingMetadata(flags.generateKytheIndexingMetadata)
          .build();
    }

    private static void checkSourceFiles(List<String> sourceFiles, Problems problems) {
      for (String sourceFile : sourceFiles) {
        if (isValidExtension(sourceFile)) {
          File file = new File(sourceFile);
          if (!file.isFile()) {
            problems.fatal(FatalError.FILE_NOT_FOUND, sourceFile);
          }
        } else {
          // does not support non-java files.
          problems.fatal(FatalError.UNKNOWN_INPUT_TYPE, sourceFile);
        }
      }
    }

    private static boolean isValidExtension(String sourceFile) {
      return sourceFile.endsWith(".java")
          || sourceFile.endsWith(".srcjar")
          || sourceFile.endsWith("-src.jar");
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
  }

  static Problems run(String[] args) {
    try {
      J2clTranspilerOptions options = Flags.parse(args);
      return J2clTranspiler.transpile(options);
    } catch (Problems.Exit e) {
      return e.getProblems();
    }
  }

  public static void main(String[] args) {
    System.exit(run(args).reportAndGetExitCode(System.err));
  }

  private J2clCommandLineRunner() {}
}
