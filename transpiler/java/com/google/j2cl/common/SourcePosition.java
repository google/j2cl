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
package com.google.j2cl.common;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Describes the location of a node in the original source in the form of a range
 * (line,column)-(line,column); where both line and column are zero-based.
 */
public class SourcePosition implements Comparable<SourcePosition> {

  // For mappings that should not be displayed in readable output.
  public static final SourcePosition DUMMY = new SourcePosition("UNKNOWN", 0, 0, 0, 0);

  public static final SourcePosition UNKNOWN = new SourcePosition("UNKNOWN", -1, -1, -1, -1);

  private final String filePath;
  private final FilePosition startPosition;
  private final FilePosition endPosition;

  public SourcePosition(FilePosition startPosition, FilePosition endPosition) {
    this(null, startPosition, endPosition);
  }

  public SourcePosition(String filePath, FilePosition startPosition, FilePosition endPosition) {
    this.startPosition = checkNotNull(startPosition);
    this.endPosition = checkNotNull(endPosition);
    this.filePath = filePath;
  }

  public SourcePosition(
      String filePath,
      int startLineNumber,
      int startColumnNumber,
      int endLineNumber,
      int endColumnNumber) {
    this(
        filePath,
        new FilePosition(startLineNumber, startColumnNumber),
        new FilePosition(endLineNumber, endColumnNumber));
  }

  public FilePosition getStartFilePosition() {
    return startPosition;
  }

  public FilePosition getEndFilePosition() {
    return endPosition;
  }

  public String getFilePath() {
    return filePath;
  }

  @Override
  public int compareTo(SourcePosition o) {
    if (filePath != null) {
      int nameComparison = filePath.compareTo(o.getFilePath());
      if (nameComparison != 0) {
        return nameComparison;
      }
    }
    if (startPosition.getLine() == o.startPosition.getLine()) {
      return startPosition.getColumn() - o.startPosition.getColumn();
    }
    return startPosition.getLine() - o.startPosition.getLine();
  }
}
