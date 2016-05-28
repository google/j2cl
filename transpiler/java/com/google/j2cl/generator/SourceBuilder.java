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

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.sourcemap.SourcePosition;

import org.apache.commons.lang.StringUtils;

/**
 * Builds source and tracks line numbers using a StringBuilder.
 */
class SourceBuilder {
  static final String INDENT = "  ";
  private StringBuilder sb = new StringBuilder();
  private int lineNumber = 0;
  private int indentationLevel = 0;
  private boolean onNewLine = false;

  /**
   * Appends some source and returns its resulting location.
   */
  public SourcePosition append(String source) {
    // TODO: the code here is quite fragile and buggy, the SourcePosition is only correct if
    // starting on a new line.

    Preconditions.checkArgument(
        !source.startsWith("  "),
        "Let SourceBuilder manage indentation, don't provide spaces at the beginning "
            + "of appended lines.");
    int numNewLines = StringUtils.countMatches(source, System.lineSeparator());
    // For lines with JSDoc we do want to unindent so we just leave it at the same indentation
    // level.
    boolean containsBlock = source.contains("{") && source.contains("}");
    if (!containsBlock) {
      // We unindent immediately if there is a } so that the } is at the outer indentation level
      indentationLevel -= StringUtils.countMatches(source, "}");
    }

    int lastNewLineIndex = 0;
    if (numNewLines > 0) {
      lastNewLineIndex = source.lastIndexOf(System.lineSeparator());
    }

    String indent = onNewLine ? StringUtils.repeat(INDENT, indentationLevel) : "";
    sb.append(indent + source);

    if (!containsBlock) {
      // We want to keep the { at the outer indentation so we indent after it has been written
      // to the buffer.
      indentationLevel += StringUtils.countMatches(source, "{");
    }

    SourcePosition outputLocation =
        new SourcePosition(
            lineNumber,
            indent.length(),
            lineNumber + numNewLines,
            indent.length() + source.length() - lastNewLineIndex);
    lineNumber += numNewLines;
    onNewLine = source.endsWith(System.lineSeparator());
    return outputLocation;
  }

  /**
   * Appends the given string to the output source and then appends a new line character.
   * Returns the location of the string in the output source code without the new line.
   */
  public SourcePosition appendln(String sourceLine) {
    SourcePosition outputLocation = append(sourceLine);
    append(System.lineSeparator());
    return outputLocation;
  }

  public SourcePosition appendln(String template, Object... parameters) {
    return appendln(String.format(template, parameters));
  }

  public void newLine() {
    appendln("");
  }

  public String build() {
    return sb.toString();
  }
}
