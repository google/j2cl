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
package com.google.j2cl.transpiler.backend.closure;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourcePosition;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Generates a readable version of the sourcemap. */
public final class ReadableSourceMapGenerator {
  /** The source location of the ast node to print, input or output. */
  public static String generate(
      Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition,
      String javaScriptImplementationFileContents,
      NativeJavaScriptFile nativeJavaScriptFile,
      String j2clUnitFilePath,
      Problems problems) {

    Map<String, List<String>> sourceLinesByFileName =
        buildSourceLinesByFileName(nativeJavaScriptFile, j2clUnitFilePath, problems);

    StringBuilder sb = new StringBuilder();

    List<Entry<SourcePosition, SourcePosition>> entries =
        new ArrayList<>(javaSourcePositionByOutputSourcePosition.entrySet());

    List<String> javaScriptSourceLines =
        Arrays.asList(javaScriptImplementationFileContents.split("\n"));
    for (Entry<SourcePosition, SourcePosition> entry : entries) {
      SourcePosition javaSourcePosition = checkNotNull(entry.getValue());
      SourcePosition javaScriptSourcePosition = checkNotNull(entry.getKey());
      List<String> javaSourceLines = sourceLinesByFileName.get(javaSourcePosition.getFileName());
      if (javaSourceLines == null) {
        // ReadableSourceMapGenerator doesn't work over multiple files so inlining is not handled.
        continue;
      }

      boolean hasName = javaSourcePosition.getName() != null;

      sb.append(extract(javaSourcePosition, javaSourceLines, hasName))
          .append(" => ")
          .append(extract(javaScriptSourcePosition, javaScriptSourceLines, hasName).trim());

      if (hasName) {
        sb.append(" \"").append(javaSourcePosition.getName()).append("\"");
      }

      sb.append("\n");
    }
    return sb.toString();
  }

  private static ImmutableMap<String, List<String>> buildSourceLinesByFileName(
      NativeJavaScriptFile nativeJavaScriptFile, String j2clUnitFilePath, Problems problems) {
    ImmutableMap.Builder<String, List<String>> contentsByFileNameBuilder = ImmutableMap.builder();

    if (nativeJavaScriptFile != null) {
      String nativeJavaScriptFileFileName = nativeJavaScriptFile.getRelativeFilePath();
      contentsByFileNameBuilder.put(
          new File(nativeJavaScriptFileFileName).getName(),
          Splitter.on('\n').splitToList(nativeJavaScriptFile.getContent()));
    }
    try {
      contentsByFileNameBuilder.put(
          new File(j2clUnitFilePath).getName(),
          java.nio.file.Files.readAllLines(Paths.get(j2clUnitFilePath)));
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_OPEN_FILE, e.toString());
    }
    return contentsByFileNameBuilder.build();
  }

  private static String extract(
      SourcePosition sourcePosition, List<String> lines, boolean condense) {
    int startLine = sourcePosition.getStartFilePosition().getLine();
    int endLine = sourcePosition.getEndFilePosition().getLine();
    String fragment = lines.get(startLine);
    int endColumn = sourcePosition.getEndFilePosition().getColumn();
    int startColumn = sourcePosition.getStartFilePosition().getColumn();
    if (endLine != startLine || endColumn == -1) {
      StringBuilder content =
          new StringBuilder(trimTrailingWhitespace(fragment.substring(startColumn)));
      if (condense && startLine + 3 < endLine) {
        content
            .append("\n")
            .append(trimTrailingWhitespace(lines.get(startLine + 1)))
            .append("\n...")
            .append(trimTrailingWhitespace(lines.get(endLine - 1)));
      } else {
        for (int line = startLine + 1; line < endLine; line++) {
          content.append("\n").append(trimTrailingWhitespace(lines.get(line)));
        }
      }
      content
          .append("\n")
          .append(trimTrailingWhitespace(lines.get(endLine).substring(0, endColumn)));
      return "[" + content + "]";
    }

    return "[" + fragment.substring(startColumn, endColumn) + "]";
  }

  private static String trimTrailingWhitespace(String string) {
    return CharMatcher.whitespace().trimTrailingFrom(string);
  }

  private ReadableSourceMapGenerator() {}
}
