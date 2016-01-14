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

import com.google.debugging.sourcemap.FilePosition;

/**
 * Describes the location of a node in the original source.
 */
public class SourceInfo implements Comparable<SourceInfo> {
  public static final SourceInfo UNKNOWN_SOURCE_INFO = new SourceInfo(-1, -1, -1, -1);

  private FilePosition startPosition;
  private FilePosition endPosition;

  public SourceInfo(
      int startLineNumber, int startColumnNumber, int endLineNumber, int endColumnNumber) {
    startPosition = new FilePosition(startLineNumber, startColumnNumber);
    endPosition = new FilePosition(endLineNumber, endColumnNumber);
  }

  public FilePosition getStartFilePosition() {
    return startPosition;
  }

  public FilePosition getEndFilePosition() {
    return endPosition;
  }

  @Override
  public int compareTo(SourceInfo o) {
    if (startPosition.getLine() == o.startPosition.getLine()) {
      return startPosition.getColumn() - o.startPosition.getColumn();
    }
    return startPosition.getLine() - o.startPosition.getLine();
  }
}
