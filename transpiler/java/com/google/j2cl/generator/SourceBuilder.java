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

import com.google.j2cl.ast.sourcemap.SourceInfo;

import org.apache.commons.lang.StringUtils;

/**
 * Builds source and tracks line numbers using a StringBuilder.
 */
class SourceBuilder {
  private StringBuilder sb = new StringBuilder();
  private int lineNumber = 0;

  /**
   * Appends some source and returns its resulting location.
   */
  public SourceInfo append(String source) {
    sb.append(source);
    int numNewLines = StringUtils.countMatches(source, System.lineSeparator());
    int lastNewLineIndex = 0;
    if (numNewLines > 0) {
      lastNewLineIndex = source.lastIndexOf(System.lineSeparator());
    }
    SourceInfo outputLocation =
        new SourceInfo(lineNumber, 0, lineNumber + numNewLines, source.length() - lastNewLineIndex);
    lineNumber += numNewLines;
    return outputLocation;
  }

  /**
   * Appends the given string to the output source and then appends a new line character.
   * Returns the location of the string in the output source code without the new line.
   */
  public SourceInfo appendln(String sourceLine) {
    SourceInfo outputLocation = append(sourceLine);
    append("\n");
    return outputLocation;
  }

  public SourceInfo appendln(String template, Object... parameters) {
    return appendln(String.format(template, parameters));
  }

  public void newLine() {
    appendln("");
  }

  public String build() {
    return sb.toString();
  }
}
