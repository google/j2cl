/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.common;

/** J2cl's implementation of sourcemap file position. */
public final class FilePosition {
  private final int line;
  private final int column;

  public FilePosition(int line, int column) {
    this.line = line;
    this.column = column;
  }

  /** @return the line number of this position. */
  public int getLine() {
    return line;
  }

  /** @return the character index on the line of this position, with the first column being 0. */
  public int getColumn() {
    return column;
  }

  @Override
  public boolean equals(Object that) {
    if (!(that instanceof FilePosition)) {
      return false;
    }
    FilePosition thatFilePosition = (FilePosition) that;
    return line == thatFilePosition.line && column == thatFilePosition.column;
  }

  @Override
  public int hashCode() {
    return line * 37 + column;
  }

  @Override
  public String toString() {
    return "(" + getLine() + ":" + getColumn() + ")";
  }
}
