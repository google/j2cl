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

import com.google.j2cl.ast.sourcemap.SourcePosition;

import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.Map.Entry;

/**
 * Generates a readable version of the sourcemap.
 */
public class ReadableSourceMapGenerator {

  public static String generate(
      SourceMapBuilder sourceMapBuilder,
      Path javaSourceFile,
      String javaScriptImplementationFilecontents) {
    StringBuilder sb = new StringBuilder();
    for (Entry<SourcePosition, Pair<String, SourcePosition>> entry :
        sourceMapBuilder.getMappings().entrySet()) {
      SourcePosition javaSourcePosition = entry.getValue().getRight();
      SourcePosition javaScriptSourcePosition = entry.getKey();
      sb.append(String.format("%s ", entry.getValue().getLeft()));
      sb.append(formatSourceInfo(javaSourcePosition));
      sb.append(" => ");
      sb.append(formatSourceInfo(javaScriptSourcePosition));
      sb.append("\n");
    }

    return sb.toString();
  }

  private static String formatSourceInfo(SourcePosition sourcePosition) {
    if (sourcePosition == SourcePosition.UNKNOWN) {
      return "UNKNOWN";
    } else {
      return String.format(
          "l%d c%d - l%d c%d",
          sourcePosition.getStartFilePosition().getLine(),
          sourcePosition.getStartFilePosition().getColumn(),
          sourcePosition.getEndFilePosition().getLine(),
          sourcePosition.getEndFilePosition().getColumn());
    }
  }
}
