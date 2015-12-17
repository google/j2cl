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
package com.google.j2cl.sourcemaps;

/**
 * Describes the location of a node in the original source.
 */
public class SourceInfo {
  public static final SourceInfo UNKNOWN_SOURCE_INFO = new SourceInfo(-1, -1, 0);
  private int columnNumber;
  private int lineNumber;
  private int characterLength;

  public SourceInfo(int lineNumber, int columnNumber, int characterLength) {
    this.columnNumber = columnNumber;
    this.lineNumber = lineNumber;
    this.characterLength = characterLength;
  }

  public int getColumn() {
    return columnNumber;
  }

  public int getLine() {
    return lineNumber;
  }

  public int getLength() {
    return characterLength;
  }
}
