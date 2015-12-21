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

import com.google.j2cl.sourcemaps.SourceInfo;

import org.apache.commons.lang.StringUtils;

/**
 * Builds source and tracks line numbers using a StringBuilder.
 */
class SourceBuilder {
  private StringBuilder sb = new StringBuilder();
  private int lineNumber = 1; // lines start at 1

  public SourceBuilder() {}

  /**
   * Returns the location of the string in the output source code.
   */
  public SourceInfo append(String source) {
    sb.append(source);
    SourceInfo outputLocation = new SourceInfo(lineNumber, 0, source.length());
    int numNewLines = StringUtils.countMatches(source, System.lineSeparator());
    lineNumber += numNewLines;
    return outputLocation;
  }

  /**
   * Returns the location of the string in the output source code.
   */
  public SourceInfo appendln(String sourceLine) {
    return append(sourceLine + "\n");
  }

  public String build() {
    return sb.toString();
  }
}
