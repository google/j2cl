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

import com.google.common.base.CharMatcher;
import com.google.common.io.Files;
import com.google.j2cl.common.SourcePosition;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Generates a readable version of the sourcemap.
 */
public class ReadableSourceMapGenerator {
  /**
   * The source location of the ast node to print, input or output.
   */
  public static String generate(
      Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition,
      Path javaSourceFile,
      String javaScriptImplementationFilecontents) {
    StringBuilder sb = new StringBuilder();
    try {
      List<String> javaSourceLines =
          Files.readLines(javaSourceFile.toFile(), Charset.defaultCharset());
      List<String> javaScriptSourceLines =
          Arrays.asList(javaScriptImplementationFilecontents.split("\n"));
      for (Entry<SourcePosition, SourcePosition> entry :
          javaSourcePositionByOutputSourcePosition.entrySet()) {
        SourcePosition javaSourcePosition = entry.getValue();
        SourcePosition javaScriptSourcePosition = entry.getKey();

        // Do not display dummy mappings in readable output.
        if (javaSourcePosition == SourcePosition.DUMMY) {
          continue;
        }

        sb.append(extract(javaSourcePosition, javaSourceLines));
        sb.append(" => ");
        sb.append(extract(javaScriptSourcePosition, javaScriptSourceLines).trim());
        sb.append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  private static String extract(SourcePosition sourcePosition, List<String> lines) {
    int startLine = sourcePosition.getStartFilePosition().getLine();
    int endLine = sourcePosition.getEndFilePosition().getLine();
    if (sourcePosition == SourcePosition.UNKNOWN) {
      return "[UNKNOWN]";
    }
    String fragment = lines.get(startLine);
    int endColumn = sourcePosition.getEndFilePosition().getColumn();
    int startColumn = sourcePosition.getStartFilePosition().getColumn();
    if (endLine != startLine || endColumn == -1) {
      StringBuilder content =
          new StringBuilder(trimTrailingWhitespace(fragment.substring(startColumn)));
      for (int line = startLine + 1; line < endLine; line++) {
        content.append("\n");
        content.append(trimTrailingWhitespace(lines.get(line)));
      }
      content.append("\n");
      content.append(lines.get(endLine).substring(0, endColumn));
      return "[" + content.toString() + "]";
    }

    return "[" + fragment.substring(startColumn, endColumn) + "]";
  }

  private static String trimTrailingWhitespace(String string) {
    return CharMatcher.whitespace().trimTrailingFrom(string);
  }
}
