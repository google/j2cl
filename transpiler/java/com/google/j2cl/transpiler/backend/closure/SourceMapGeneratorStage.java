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
package com.google.j2cl.transpiler.backend.closure;

import com.google.debugging.sourcemap.FilePosition;
import com.google.debugging.sourcemap.SourceMapFormat;
import com.google.debugging.sourcemap.SourceMapGenerator;
import com.google.debugging.sourcemap.SourceMapGeneratorFactory;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.Type;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/** Generates the source maps. */
public final class SourceMapGeneratorStage {

  public static String generateSourceMaps(
      Type type, Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition)
      throws IOException {
    return renderSourceMapToString(type, javaSourcePositionByOutputSourcePosition);
  }

  private static String renderSourceMapToString(
      Type type, Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition)
      throws IOException {
    SourceMapGenerator sourceMapGenerator =
        SourceMapGeneratorFactory.getInstance(SourceMapFormat.V3);
    for (Entry<SourcePosition, SourcePosition> entry :
        javaSourcePositionByOutputSourcePosition.entrySet()) {
      SourcePosition javaSourcePosition = entry.getValue();
      SourcePosition javaScriptSourcePosition = entry.getKey();

      sourceMapGenerator.addMapping(
          javaSourcePosition.getFileName(),
          javaSourcePosition.getName(),
          toFilePosition(javaSourcePosition.getStartFilePosition()),
          toFilePosition(javaScriptSourcePosition.getStartFilePosition()),
          toFilePosition(javaScriptSourcePosition.getEndFilePosition()));
    }
    StringBuilder sb = new StringBuilder();
    String typeName = type.getDeclaration().getSimpleBinaryName();
    sourceMapGenerator.appendTo(sb, typeName + JavaScriptImplGenerator.FILE_SUFFIX);
    return sb.toString();
  }

  /**
   * Converts a j2cl File Position to a JsCompiler sourcemap File Position.
   *
   * @return JsCompiler sourcemap File Position
   */
  private static FilePosition toFilePosition(com.google.j2cl.common.FilePosition j2clFilePosition) {
    return new FilePosition(j2clFilePosition.getLine(), j2clFilePosition.getColumn());
  }

  private SourceMapGeneratorStage() {}
}
