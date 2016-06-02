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

import com.google.common.base.Strings;
import com.google.debugging.sourcemap.FilePosition;

import org.apache.commons.lang.StringUtils;

/**
 * Builds source and tracks line numbers using a StringBuilder.
 */
class SourceBuilder {
  private static final String INDENT = "  ";
  private StringBuilder sb = new StringBuilder();
  private int currentLine = 0;
  private int currentColumn = 0;
  private int currentIndentation = 0;

  /**
   * Appends some source and returns its resulting location.
   */
  public void append(String source) {
    String indentedSource =
        source.replace(
            System.lineSeparator(),
            System.lineSeparator() + Strings.repeat(INDENT, currentIndentation));

    sb.append(indentedSource);
    currentLine += StringUtils.countMatches(indentedSource, System.lineSeparator());
    currentColumn = sb.length() - sb.lastIndexOf(System.lineSeparator()) - 1;
  }

  public FilePosition getCurrentPosition() {
    return new FilePosition(currentLine, currentColumn);
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
}
