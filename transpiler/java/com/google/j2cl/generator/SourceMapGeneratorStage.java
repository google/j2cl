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
package com.google.j2cl.generator;

import com.google.debugging.sourcemap.FilePosition;
import com.google.debugging.sourcemap.SourceMapFormat;
import com.google.debugging.sourcemap.SourceMapGenerator;
import com.google.debugging.sourcemap.SourceMapGeneratorFactory;
import com.google.j2cl.ast.Type;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Generates the source maps.
 */
public class SourceMapGeneratorStage {
  public static final String SOURCE_MAP_SUFFIX = ".js.map";

  private final Problems problems;
  private final Path outputPath;
  private final String javaSourceFile;
  private final String javaScriptImplementationFileContents;
  private final boolean generateReadableSourceMaps;
  private final String compilationUnitSourceFileName;

  public SourceMapGeneratorStage(
      String compilationUnitSourceFileName,
      Path outputPath,
      String javaSourceFile,
      String javaScriptImplementationFileContents,
      Problems problems,
      boolean generateReadableSourceMaps) {
    this.outputPath = outputPath;
    this.javaSourceFile = javaSourceFile;
    this.problems = problems;
    this.generateReadableSourceMaps = generateReadableSourceMaps;
    this.compilationUnitSourceFileName = compilationUnitSourceFileName;
    this.javaScriptImplementationFileContents = javaScriptImplementationFileContents;
  }

  public void generateSourceMaps(
      Type type, Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition) {
    try {
      String output =
          generateReadableSourceMaps
              ? ReadableSourceMapGenerator.generate(
                  javaSourcePositionByOutputSourcePosition,
                  Paths.get(javaSourceFile),
                  javaScriptImplementationFileContents)
              : renderSourceMapToString(type, javaSourcePositionByOutputSourcePosition);

      if (output.isEmpty() && generateReadableSourceMaps) {
        return;
      }
      Path absolutePathForSourceMap =
          outputPath.resolve(GeneratorUtils.getRelativePath(type) + SOURCE_MAP_SUFFIX);
      GeneratorUtils.writeToFile(absolutePathForSourceMap, output, problems);
    } catch (IOException e) {
      problems.error("Could not generate source maps for %s: %s", javaSourceFile, e.getMessage());
    }
  }

  private String renderSourceMapToString(
      Type type, Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition)
      throws IOException {
    SourceMapGenerator sourceMapGenerator =
        SourceMapGeneratorFactory.getInstance(SourceMapFormat.V3);
    for (Entry<SourcePosition, SourcePosition> entry :
        javaSourcePositionByOutputSourcePosition.entrySet()) {
      SourcePosition javaSourcePosition = entry.getValue();
      SourcePosition javaScriptSourcePosition = entry.getKey();
      if (javaSourcePosition.isUnknown() || javaScriptSourcePosition.isUnknown()) {
        continue;
      }
      sourceMapGenerator.addMapping(
          compilationUnitSourceFileName,
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
   * @param j2clFilePosition
   * @return JsCompiler sourcemap File Position
   */
  private FilePosition toFilePosition(com.google.j2cl.common.FilePosition j2clFilePosition) {
    return new FilePosition(j2clFilePosition.getLine(), j2clFilePosition.getColumn());
  }
}
