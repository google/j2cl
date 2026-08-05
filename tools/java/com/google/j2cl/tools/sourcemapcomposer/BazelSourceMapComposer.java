/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.tools.sourcemapcomposer;

import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.common.bazel.BazelWorker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.kohsuke.args4j.Option;

/** */
final class BazelSourceMapComposer extends BazelWorker {
  @Option(
      name = "-excludeTargetExtension",
      usage = "Target source file extensions to exclude from the output.")
  List<String> excludeTargetExtensions = new ArrayList<>();

  @Option(
      name = "-intermediate",
      required = true,
      metaVar = "<file|directory>",
      usage = "Files or directories containing intermediate source maps and their sources.")
  List<Path> intermediatePaths = new ArrayList<>();

  @Option(
      name = "-target",
      required = true,
      metaVar = "<file|directory>",
      usage = "Files or directories containing target source maps and their sources.")
  List<Path> targetPaths = new ArrayList<>();

  @Option(
      name = "-d",
      required = true,
      metaVar = "<file|directory>",
      usage = "Directory or zip file into which to place the composed output.")
  Path outputPath;

  @Override
  protected void run() {
    try (var output = OutputUtils.initOutputForBazel(outputPath, problems)) {

      PartitionedSources intermediate = separateOutSourceMaps(intermediatePaths);
      PartitionedSources target = separateOutSourceMaps(targetPaths);

      intermediate.sources.stream().forEach(f -> output.copyFile(f.sourcePath(), f.originalPath()));

      target.sources.stream()
          .filter(f -> shouldCopySource(f, excludeTargetExtensions))
          .forEach(f -> output.copyFile(f.sourcePath(), f.originalPath()));

      SourceMapComposer.composeSourceMaps(
          intermediate.sourceMaps, target.sourceMaps, output, problems);
    }
  }

  private static boolean shouldCopySource(FileInfo fileInfo, List<String> excludedExtensions) {
    String path = fileInfo.originalPath();
    return excludedExtensions.stream().noneMatch(path::endsWith);
  }

  /** Helper to extract and partition sources into sourcemaps (.map) and non-sourcemap files. */
  private static PartitionedSources separateOutSourceMaps(List<Path> paths) {
    Map<Boolean, List<FileInfo>> partitioned =
        paths.stream()
            .flatMap(SourceUtils::getAllSources)
            .filter(f -> Files.isRegularFile(Paths.get(f.sourcePath())))
            .collect(
                Collectors.partitioningBy(
                    f -> f.targetPath().endsWith(SourceMapComposer.MAP_EXTENSION)));

    return new PartitionedSources(partitioned.get(true), partitioned.get(false));
  }

  private record PartitionedSources(List<FileInfo> sourceMaps, List<FileInfo> sources) {}

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelSourceMapComposer::new);
  }
}
