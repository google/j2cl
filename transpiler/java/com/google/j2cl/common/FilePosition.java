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

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;

/** J2cl's implementation of sourcemap file position. */
@AutoValue
public abstract class FilePosition {

  /** @return the line number of this position. */
  public abstract int getLine();

  /** @return the character index on the line of this position, with the first column being 0. */
  public abstract int getColumn();

  /** @return the byte offset of this position. */
  public abstract int getByteOffset();

  public static Builder newBuilder() {
    return new AutoValue_FilePosition.Builder();
  }

  /** A Builder for FilePosition. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setLine(int line);

    public abstract Builder setColumn(int column);

    public abstract Builder setByteOffset(int byteOffset);

    abstract FilePosition autoBuild();

    public FilePosition build() {
      return autoBuild();
    }
  }

  @Override
  public final boolean equals(Object that) {
    if (!(that instanceof FilePosition)) {
      return false;
    }
    FilePosition thatFilePosition = (FilePosition) that;
    boolean result =
        getLine() == thatFilePosition.getLine() && getColumn() == thatFilePosition.getColumn();
    checkState(
        result == (getByteOffset() == thatFilePosition.getByteOffset()),
        "Line/column position does not match byte offset.");
    return result;
  }

  @Override
  public final int hashCode() {
    return getLine() * 37 + getColumn();
  }

  @Override
  public final String toString() {
    return "(" + getLine() + ":" + getColumn() + ")";
  }
}
