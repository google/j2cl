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
import com.google.j2cl.bazel.BazelWorker;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.frontend.FrontendUtils;
import com.google.j2cl.frontend.FrontendUtils.FileInfo;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * The J2cl builder for Bazel that runs as a worker.
 *
 * <p>Bazel workers allow J2CL to run in a JVM that is not being terminated after every compile and
 * thus gain significant speedups.
 */
public final class BazelJ2clBuilder {

  private static class Flags {
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

    @Option(name = "-readablesourcemaps", hidden = true)
    protected boolean readableSourceMaps = false;

    @Option(
        name = "-declarelegacynamespaces",
        usage = "For Docs during onboarding, do not use.",
        hidden = true)
    protected boolean declareLegacyNamespaces = false;

    @Option(name = "-generatekytheindexingmetadata", hidden = true)
    protected boolean generateKytheIndexingMetadata = false;

    /** Parses the given args list and updates values. */
    private static J2clTranspilerOptions parse(String[] args) {
      Flags flags = new Flags();
      CmdLineParser parser = new CmdLineParser(flags);
      Problems problems = new Problems();

      try {
        parser.parseArgument(args);
      } catch (CmdLineException e) {
        problems.error(e.getMessage());
        problems.abort();
      }

      return createOptions(flags, problems);
    }

    private static J2clTranspilerOptions createOptions(Flags flags, Problems problems) {
      checkSourceFiles(flags.sources, problems);

      if (flags.readableSourceMaps && flags.generateKytheIndexingMetadata) {
        problems.warning(
            "Readable source maps are not available when generating Kythe indexing metadata.");
        flags.readableSourceMaps = false;
      }

      List<FileInfo> allSources =
          FrontendUtils.getAllSources(flags.sources, problems)
              .collect(ImmutableList.toImmutableList());

      return J2clTranspilerOptions.newBuilder()
          .setSources(
              allSources
                  .stream()
                  .filter(p -> p.sourcePath().endsWith(".java"))
                  .collect(ImmutableList.toImmutableList()))
          .setNativeSources(
              allSources
                  .stream()
                  .filter(p -> p.sourcePath().endsWith(".native.js"))
                  .collect(ImmutableList.toImmutableList()))
          .setClasspaths(getPathEntries(flags.classPath))
          .setOutput(getZipOutput(flags.output, problems))
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
          || sourceFile.endsWith(".native.js")
          || sourceFile.endsWith(".zip")
          || sourceFile.endsWith(".jar");
    }

    private static Path getZipOutput(String output, Problems problems) {
      return FrontendUtils.initZipOutput(output, problems).getPath("/");
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

  @VisibleForTesting
  static Problems run(String[] args) {
    try {
      J2clTranspilerOptions options = Flags.parse(args);
      return J2clTranspiler.transpile(options);
    } catch (Problems.Exit e) {
      return e.getProblems();
    }
  }

  public static void main(String[] workerArgs) {
    BazelWorker.start(workerArgs, (args, output) -> run(args).reportAndGetExitCode(output));
  }

  private BazelJ2clBuilder() {}
}
