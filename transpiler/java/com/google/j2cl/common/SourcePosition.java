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

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import java.io.File;
import javax.annotation.Nullable;

/**
 * Describes the location of a node in the original source in the form of a range
 * (line,column)-(line,column); where both line and column are zero-based.
 */
@AutoValue
public abstract class SourcePosition implements Comparable<SourcePosition> {

  public static final SourcePosition NONE =
      newBuilder()
          .setStartFilePosition(FilePosition.NONE)
          .setEndFilePosition(FilePosition.NONE)
          .build();

  public abstract FilePosition getStartFilePosition();

  public abstract FilePosition getEndFilePosition();

  @Nullable
  public abstract String getFilePath();

  @Nullable
  public abstract String getName();

  @Nullable
  public abstract String getPackageRelativePath();

  @Override
  public int compareTo(SourcePosition o) {
    if (getFilePath() != null) {
      int pathComparisonResult = getFilePath().compareTo(o.getFilePath());
      if (pathComparisonResult != 0) {
        return pathComparisonResult;
      }
    }
    return getStartFilePosition().compareTo(o.getStartFilePosition());
  }

  @Memoized
  @Nullable
  public String getFileName() {
    String filePath = getFilePath();
    return filePath == null ? filePath : new File(filePath).getName();
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

    public abstract Builder setPackageRelativePath(String packageRelativePath);

    public abstract Builder setName(String name);

    abstract SourcePosition autoBuild();

    public SourcePosition build() {
      SourcePosition sourcePosition = autoBuild();
      checkState(
          sourcePosition.getStartFilePosition().compareTo(sourcePosition.getEndFilePosition())
              <= 0);
      return sourcePosition;
    }

    public static Builder from(SourcePosition sourcePosition) {
      return sourcePosition.toBuilder();
    }
  }
}
