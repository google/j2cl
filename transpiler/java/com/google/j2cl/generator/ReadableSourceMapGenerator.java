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

import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.sourcemaps.SourceInfo;

import java.io.StringWriter;

/**
 * Used for testing.
 */
public class ReadableSourceMapGenerator {
  /**
   * The source location of the ast node to print, input or output.
   */
  public static String generateForJavaType(JavaType javaType) {
    StringWriter stringWriter = new StringWriter();
    StaticFieldAccessGatherer gatherer = new StaticFieldAccessGatherer(stringWriter);
    javaType.accept(gatherer);
    return stringWriter.toString();
  }

  private static class StaticFieldAccessGatherer extends AbstractVisitor {
    StringWriter writer;

    private StaticFieldAccessGatherer(StringWriter writer) {
      this.writer = writer;
    }

    private String formatSourceInfo(SourceInfo sourceInfo) {
      if (sourceInfo == SourceInfo.UNKNOWN_SOURCE_INFO) {
        return "UNKNOWN";
      } else {
        return String.format(
            "l%d c%d - l%d c%d",
            sourceInfo.getStartFilePosition().getLine(),
            sourceInfo.getStartFilePosition().getColumn(),
            sourceInfo.getEndFilePosition().getLine(),
            sourceInfo.getEndFilePosition().getColumn());
      }
    }

    @Override
    public boolean enterStatement(Statement statement) {
      SourceInfo intput = statement.getInputSourceInfo();
      SourceInfo output = statement.getOutputSourceInfo();
      writer.append(String.format("%s ", statement.getClass().getSimpleName()));
      writer.append(formatSourceInfo(intput));
      writer.append(" => ");
      writer.append(formatSourceInfo(output));
      writer.append("\n");
      return true;
    }
  }
}
