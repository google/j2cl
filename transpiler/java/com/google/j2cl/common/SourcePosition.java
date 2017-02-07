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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/**
 * Describes the location of a node in the original source in the form of a range
 * (line,column)-(line,column); where both line and column are zero-based.
 */
@AutoValue
public abstract class SourcePosition implements Comparable<SourcePosition> {

  public abstract FilePosition getStartFilePosition();

  public abstract FilePosition getEndFilePosition();

  public abstract @Nullable String getFilePath();

  // For mappings that should not be displayed in readable output.
  public static final SourcePosition DUMMY =
      newBuilder().setFilePath("UNKNOWN").setStartPosition(0, 0).setEndPosition(0, 0).build();

  public static final SourcePosition UNKNOWN =
      newBuilder().setFilePath("UNKNOWN").setStartPosition(-1, -1).setEndPosition(-1, -1).build();

  @Override
  public int compareTo(SourcePosition o) {
    if (getFilePath() != null) {
      int pathComparisonResult = getFilePath().compareTo(o.getFilePath());
      if (pathComparisonResult != 0) {
        return pathComparisonResult;
      }
    }
    if (getStartFilePosition().getLine() == o.getStartFilePosition().getLine()) {
      return getStartFilePosition().getColumn() - o.getStartFilePosition().getColumn();
    }
    return getStartFilePosition().getLine() - o.getStartFilePosition().getLine();
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_SourcePosition.Builder();
  }

  /** A Builder for SourcePosition. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setStartFilePosition(FilePosition filePosition);

    public abstract Builder setEndFilePosition(FilePosition filePosition);

    public abstract Builder setFilePath(String filePath);

    public Builder setStartPosition(int line, int column) {
      return setStartFilePosition(new FilePosition(line, column));
    }

    public Builder setEndPosition(int line, int column) {
      return setEndFilePosition(new FilePosition(line, column));
    }

    public static Builder from(SourcePosition sourcePosition) {
      checkArgument(sourcePosition != UNKNOWN && sourcePosition != DUMMY);
      return sourcePosition.toBuilder();
    }

    abstract SourcePosition autoBuild();

    public SourcePosition build() {
      return autoBuild();
    }
  }
}
