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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.j2cl.common.FilePosition;
import com.google.j2cl.common.SourcePosition;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;

/**
 * Builds source and tracks line numbers using a StringBuilder.
 */
class SourceBuilder {
  private static final String INDENT = "  ";
  private StringBuilder sb = new StringBuilder();
  private int currentLine = 0;
  private int currentColumn = 0;
  private int currentIndentation = 0;
  private final SortedMap<SourcePosition, SourcePosition> javaSourceInfoByOutputSourceInfo =
      new TreeMap<>();
  private boolean finished = false;

  public void emitWithMapping(Optional<SourcePosition> javaSourcePosition, Runnable codeEmitter) {
    if (!javaSourcePosition.isPresent()) {
      codeEmitter.run();
    } else {
      emitWithMapping(javaSourcePosition.get(), codeEmitter);
    }
  }

  public void emitWithMapping(SourcePosition javaSourcePosition, Runnable codeEmitter) {
    checkNotNull(javaSourcePosition);
    FilePosition startPosition = getCurrentPosition();
    codeEmitter.run();
    javaSourceInfoByOutputSourceInfo.put(
        SourcePosition.newBuilder()
            .setStartFilePosition(startPosition)
            .setEndFilePosition(getCurrentPosition())
            .build(),
        javaSourcePosition);
  }

  /**
   * Give the SourceMap file construction library enough information to be able to generate all of
   * the required empty group elements between the last mapping and the end of the file.
   */
  private void emitEOF() {
    // TODO(stalcup): switch to generator.setFileLength() when that becomes possible.
    // Emit eof marker
    if (sb.length() != 0) {
      emitWithMapping(
          SourcePosition.newBuilder()
              .setStartPosition(0, 0)
              .setEndPosition(0, 0)
              .build(),
          () -> append(" "));
    }
    finished = true;
  }

  public SortedMap<SourcePosition, SourcePosition> getMappings() {
    return javaSourceInfoByOutputSourceInfo;
  }

  public void append(String source) {
    checkState(!finished);
    String indentedSource =
        source.replace(
            System.lineSeparator(),
            System.lineSeparator() + Strings.repeat(INDENT, currentIndentation));

    sb.append(indentedSource);
    currentLine += StringUtils.countMatches(indentedSource, System.lineSeparator());
    currentColumn = sb.length() - sb.lastIndexOf(System.lineSeparator()) - 1;
  }

  public void appendLines(String... lines) {
    boolean firstLine = true;
    for (String line : lines) {
      if (!firstLine) {
        newLine();
      }
      append(line);
      firstLine = false;
    }
  }

  public void appendln(String line) {
    append(line);
    newLine();
  }

  public void newLine() {
    append(System.lineSeparator());
  }

  public void newLines(int numberOfLines) {
    for (int i = 0; i < numberOfLines; i++) {
      newLine();
    }
  }

  public void indent() {
    currentIndentation++;
  }

  public void unindent() {
    currentIndentation--;
  }

  public String build() {
    emitEOF();
    return sb.toString();
  }

  public void openBrace() {
    append("{");
    indent();
  }

  public void closeBrace() {
    unindent();
    newLine();
    append("}");
  }

  private FilePosition getCurrentPosition() {
    return new FilePosition(currentLine, currentColumn);
  }
}
