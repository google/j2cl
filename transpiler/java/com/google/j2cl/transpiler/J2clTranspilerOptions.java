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
package com.google.j2cl.transpiler;

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.backend.Backend;
import com.google.j2cl.transpiler.frontend.Frontend;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;

/** Frontend options, which is initialized by a Flag instance that is already parsed. */
@AutoValue
public abstract class J2clTranspilerOptions {

  public abstract ImmutableList<FileInfo> getSources();

  public abstract ImmutableList<FileInfo> getNativeSources();

  public abstract ImmutableList<String> getClasspaths();

  public abstract Path getOutput();

  @Nullable
  public abstract Path getLibraryInfoOutput();

  public abstract boolean getEmitReadableLibraryInfo();

  public abstract boolean getEmitReadableSourceMap();

  public abstract boolean getGenerateKytheIndexingMetadata();

  public abstract boolean getExperimentalOptimizeAutovalue();

  public abstract Frontend getFrontend();

  public abstract Backend getBackend();

  public abstract boolean getWasmRemoveAssertStatement();

  @Nullable
  public abstract ImmutableSet<String> getWasmEntryPoints();

  @Nullable
  public abstract ImmutableMap<String, String> getDefinesForWasm();

  public static Builder newBuilder() {
    return new AutoValue_J2clTranspilerOptions.Builder()
        .setExperimentalOptimizeAutovalue(false)
        .setWasmRemoveAssertStatement(false);
  }

  /** A Builder for J2clTranspilerOptions. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setSources(List<FileInfo> infos);

    public abstract Builder setNativeSources(List<FileInfo> files);

    public abstract Builder setClasspaths(List<String> entries);

    public abstract Builder setOutput(Path path);

    public abstract Builder setLibraryInfoOutput(@Nullable Path path);

    public abstract Builder setEmitReadableLibraryInfo(boolean b);

    public abstract Builder setEmitReadableSourceMap(boolean b);

    public abstract Builder setGenerateKytheIndexingMetadata(boolean b);

    public abstract Builder setExperimentalOptimizeAutovalue(boolean b);

    public abstract Builder setFrontend(Frontend frontend);

    public abstract Builder setBackend(Backend backend);

    public abstract Builder setWasmEntryPoints(ImmutableSet<String> wasmEntryPoints);

    public abstract Builder setDefinesForWasm(ImmutableMap<String, String> definesForWasm);

    public abstract Builder setWasmRemoveAssertStatement(boolean wasmRemoveAssertStatement);

    abstract J2clTranspilerOptions autoBuild();

    public J2clTranspilerOptions build() {
      J2clTranspilerOptions options = autoBuild();
      checkState(
          !options.getEmitReadableSourceMap() || !options.getGenerateKytheIndexingMetadata());
      checkState(!options.getEmitReadableLibraryInfo() || options.getLibraryInfoOutput() != null);
      return options;
    }
  }
}
