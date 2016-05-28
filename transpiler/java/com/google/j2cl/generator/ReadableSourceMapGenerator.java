/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.generator;

import com.google.common.io.Files;
import com.google.j2cl.ast.sourcemap.SourcePosition;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

/**
 * Generates a readable version of the sourcemap.
 */
public class ReadableSourceMapGenerator {
  /**
   * The source location of the ast node to print, input or output.
   */
  public static String generate(
      SourceMapBuilder sourceMapBuilder,
      Path javaSourceFile,
      String javaScriptImplementationFilecontents) {
    StringBuilder sb = new StringBuilder();
    try {
      List<String> javaSourceLines =
          Files.readLines(javaSourceFile.toFile(), Charset.defaultCharset());
      List<String> javaScriptSourceLines =
          Arrays.asList(javaScriptImplementationFilecontents.split("\n"));
      for (Entry<SourcePosition, SourcePosition> entry :
          sourceMapBuilder.getMappings().entrySet()) {
        SourcePosition javaSourcePosition = entry.getValue();
        SourcePosition javaScriptSourcePosition = entry.getKey();
        sb.append("[" + extract(javaSourcePosition, javaSourceLines));
        sb.append("] => [");
        sb.append(extract(javaScriptSourcePosition, javaScriptSourceLines).trim());
        sb.append("]\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  private static String extract(SourcePosition sourcePosition, List<String> lines) {
    try {
      if (sourcePosition == SourcePosition.UNKNOWN) {
        return "UNKNOWN";
      }
      int startLine = sourcePosition.getStartFilePosition().getLine();
      String fragment = lines.get(startLine);
      int endLine = sourcePosition.getEndFilePosition().getLine();
      int endColumn = sourcePosition.getEndFilePosition().getColumn();
      int startColumn = sourcePosition.getStartFilePosition().getColumn();
      if (endLine != startLine || endColumn == -1) {
        return fragment.substring(startColumn) + ((endLine != startLine) ? "..." : "");
      }

      return fragment.substring(startColumn, endColumn);
    } catch (IndexOutOfBoundsException e) {
      return String.format(
          "Non existing output range (%d,%d)-(%d,%d)",
          sourcePosition.getStartFilePosition().getLine(),
          sourcePosition.getStartFilePosition().getColumn(),
          sourcePosition.getEndFilePosition().getLine(),
          sourcePosition.getEndFilePosition().getColumn());
    }
  }
}
