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

import com.google.debugging.sourcemap.FilePosition;
import com.google.debugging.sourcemap.SourceMapConsumerFactory;
import com.google.debugging.sourcemap.SourceMapConsumerV3;
import com.google.debugging.sourcemap.SourceMapFormat;
import com.google.debugging.sourcemap.SourceMapGenerator;
import com.google.debugging.sourcemap.SourceMapGeneratorFactory;
import com.google.debugging.sourcemap.SourceMapParseException;
import com.google.debugging.sourcemap.proto.Mapping.OriginalMapping;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils.FileInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/**
 * Composes target source maps by applying intermediate source maps to resolve output positions back
 * to original source files.
 */
final class SourceMapComposer {

  static final String MAP_EXTENSION = ".map";

  private final Map<String, SourceMapConsumerV3> intermediateConsumers;
  private final Output output;
  private final Problems problems;

  /**
   * Composes target source maps by applying intermediate source maps to resolve output positions.
   */
  static void composeSourceMaps(
      List<FileInfo> intermediateSourceMapFileInfos,
      List<FileInfo> targetSourceMapFileInfos,
      Output output,
      Problems problems) {
    Map<String, SourceMapConsumerV3> intermediateConsumers =
        parseSourceMapConsumers(intermediateSourceMapFileInfos, problems);
    new SourceMapComposer(intermediateConsumers, output, problems)
        .compose(targetSourceMapFileInfos);
  }

  /** Parses source maps into a map of source map path to consumer. */
  private static Map<String, SourceMapConsumerV3> parseSourceMapConsumers(
      List<FileInfo> sourceMapFileInfos, Problems problems) {
    Map<String, SourceMapConsumerV3> sourceMapConsumersByPath = new ConcurrentHashMap<>();

    sourceMapFileInfos.parallelStream()
        .forEach(
            fileInfo -> {
              SourceMapConsumerV3 consumer = readSourceMap(fileInfo, problems);
              sourceMapConsumersByPath.put(stripMapSuffix(fileInfo.originalPath()), consumer);
            });

    return sourceMapConsumersByPath;
  }

  private SourceMapComposer(
      Map<String, SourceMapConsumerV3> intermediateConsumers, Output output, Problems problems) {
    this.intermediateConsumers = intermediateConsumers;
    this.output = output;
    this.problems = problems;
  }

  /**
   * Composes target source maps by applying intermediate source maps to resolve output positions.
   */
  private void compose(List<FileInfo> targetFileInfos) {
    targetFileInfos.parallelStream()
        .forEach(targetFileInfo -> composeTargetFileInfo(targetFileInfo));
  }

  private void composeTargetFileInfo(FileInfo targetFileInfo) {
    Path targetFilePath = Path.of(targetFileInfo.originalPath());

    SourceMapConsumerV3 targetConsumer = readSourceMap(targetFileInfo, problems);
    SourceMapGenerator generator = SourceMapGeneratorFactory.getInstance(SourceMapFormat.V3);

    targetConsumer.visitMappings(
        (sourceName, symbolName, sourceStartPosition, startPosition, endPosition) -> {
          SourceMapConsumerV3 intermediateConsumer =
              intermediateConsumers.get(targetFilePath.resolveSibling(sourceName).toString());

          composeAndAddMapping(
              generator,
              intermediateConsumer,
              sourceName,
              symbolName,
              sourceStartPosition,
              startPosition,
              endPosition);
        });

    try {
      StringBuilder sb = new StringBuilder();
      generator.appendTo(sb, stripMapSuffix(targetConsumer.getFile()));
      output.write(targetFilePath.toString(), sb.toString());
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.getMessage());
    }
  }

  /**
   * Composes a target mapping with an intermediate mapping and adds it to the generator.
   *
   * <p>If composition cannot be performed because the intermediate consumer is null, the original
   * mapping is persisted verbatim, but if the intermediate mapping is invalid, the original mapping
   * is dropped.
   */
  private static void composeAndAddMapping(
      SourceMapGenerator generator,
      @Nullable SourceMapConsumerV3 intermediateConsumer,
      String sourceName,
      String symbolName,
      FilePosition sourceStartPosition,
      FilePosition startPosition,
      FilePosition endPosition) {
    // The intermediate consumer will be null if the source file lacks an original source
    // mapping (e.g., synthetic code generated during transpilation).
    if (intermediateConsumer == null) {
      // Persist the original mapping.
      generator.addMapping(sourceName, symbolName, sourceStartPosition, startPosition, endPosition);
      return;
    }

    OriginalMapping mapping =
        intermediateConsumer.getMappingForLine(
            sourceStartPosition.getLine() + 1, sourceStartPosition.getColumn() + 1);

    // Check if the position is unmapped or invalid in the intermediate consumer.
    if (mapping == null
        || !mapping.hasOriginalFile()
        || mapping.getLineNumber() <= 0
        || mapping.getColumnPosition() <= 0) {
      // Drop the original mapping.
      return;
    }

    FilePosition originalFilePosition =
        new FilePosition(mapping.getLineNumber() - 1, mapping.getColumnPosition() - 1);
    String resolvedSymbolName = mapping.hasIdentifier() ? mapping.getIdentifier() : symbolName;

    generator.addMapping(
        mapping.getOriginalFile(),
        resolvedSymbolName,
        originalFilePosition,
        startPosition,
        endPosition);
  }

  /** Reads and parses a source map file from the specified {@link FileInfo}. */
  private static SourceMapConsumerV3 readSourceMap(FileInfo fileInfo, Problems problems) {
    try {
      String content = Files.readString(Path.of(fileInfo.sourcePath()));
      return (SourceMapConsumerV3) SourceMapConsumerFactory.parse(content);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_OPEN_FILE, e.getMessage());
    } catch (SourceMapParseException e) {
      problems.fatal(FatalError.INVALID_SOURCE_MAP, e.getMessage());
    }
    throw new AssertionError("Unreachable");
  }

  /** Removes the .map suffix from the path, if present. */
  private static String stripMapSuffix(String path) {
    if (path.endsWith(MAP_EXTENSION)) {
      return path.substring(0, path.length() - MAP_EXTENSION.length());
    }
    return path;
  }
}
