/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.backend.common;


import com.google.debugging.sourcemap.FilePosition;
import com.google.debugging.sourcemap.SourceMapFormat;
import com.google.debugging.sourcemap.SourceMapGeneratorFactory;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.SourceUtils.FileInfo;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/** Generates the source maps. */
public final class SourceMapGenerator {

  public static String generateSourceMaps(
      FileInfo compilationUnitSourceFile,
      String sourceMapFileName,
      Map<SourcePosition, SourcePosition> sourcePositionByOutputPosition)
      throws IOException {
    com.google.debugging.sourcemap.SourceMapGenerator sourceMapGenerator =
        SourceMapGeneratorFactory.getInstance(SourceMapFormat.V3);
    for (Entry<SourcePosition, SourcePosition> entry : sourcePositionByOutputPosition.entrySet()) {
      SourcePosition sourcePosition = entry.getValue();
      SourcePosition outputSourcePosition = entry.getKey();

      FileInfo sourcePositionFile = sourcePosition.getFileInfo();
      // TODO(b/267492636): skip mappings for cross-file references. We currently rely upon the
      //   source file being adjacent to the mapping file, which may not be the case here. Also,
      //   even if they are technically adjacent paths, if they are in separate library targets
      //   their output paths will be in different tree artifact roots.
      if (!isInCurrentCompilationUnit(compilationUnitSourceFile, sourcePositionFile)) {
        continue;
      }

      sourceMapGenerator.addMapping(
          sourcePosition.getFileName(),
          sourcePosition.getName(),
          toFilePosition(sourcePosition.getStartFilePosition()),
          toFilePosition(outputSourcePosition.getStartFilePosition()),
          toFilePosition(outputSourcePosition.getEndFilePosition()));
    }
    StringBuilder sb = new StringBuilder();
    sourceMapGenerator.appendTo(sb, sourceMapFileName);
    return sb.toString();
  }

  private static boolean isInCurrentCompilationUnit(
      FileInfo compilationUnitSourceFile, FileInfo sourcePositionFile) {
    if (sourcePositionFile == null) {
      return false;
    }
    if (sourcePositionFile.targetPath().equals(compilationUnitSourceFile.targetPath())) {
      return true;
    }

    // If the source position points to a native.js file, we'll tolerate it. We don't expect to see
    // references to native.js files cross-file or cross-compilations, so just checking the name
    // suffix should be sufficient.
    if (sourcePositionFile.targetPath().endsWith(".native_js")) {
      return true;
    }

    // Otherwise we'll assume it's coming from somewhere else; we should ignore it.
    return false;
  }

  /**
   * Converts a j2cl File Position to a JsCompiler sourcemap File Position.
   *
   * @return JsCompiler sourcemap File Position
   */
  private static FilePosition toFilePosition(com.google.j2cl.common.FilePosition j2clFilePosition) {
    return new FilePosition(j2clFilePosition.getLine(), j2clFilePosition.getColumn());
  }

  private SourceMapGenerator() {}
}
