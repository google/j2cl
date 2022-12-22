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
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.backend.Backend;
import com.google.j2cl.transpiler.backend.BackendOptions;
import com.google.j2cl.transpiler.frontend.Frontend;
import com.google.j2cl.transpiler.frontend.FrontendOptions;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;

/** Configuration for the transpiler. */
@AutoValue
public abstract class J2clTranspilerOptions implements FrontendOptions, BackendOptions {

  public abstract Frontend getFrontend();

  public abstract Backend getBackend();

  public static Builder newBuilder() {
    return new AutoValue_J2clTranspilerOptions.Builder()
        .setOptimizeAutoValue(false)
        .setWasmRemoveAssertStatement(false)
        .setNullMarkedSupported(false);
  }

  @Override
  public ImmutableList<String> getForbiddenAnnotations() {
    return ImmutableList.of(
        getBackend() == Backend.KOTLIN ? "J2ktIncompatible" : "GwtIncompatible");
  }

  /** A Builder for J2clTranspilerOptions. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setSources(List<FileInfo> infos);

    public abstract Builder setNativeSources(List<FileInfo> files);

    public abstract Builder setClasspaths(List<String> entries);

    public abstract Builder setOutput(Output output);

    public abstract Builder setLibraryInfoOutput(@Nullable Path path);

    public abstract Builder setEmitReadableLibraryInfo(boolean b);

    public abstract Builder setEmitReadableSourceMap(boolean b);

    public abstract Builder setGenerateKytheIndexingMetadata(boolean b);

    public abstract Builder setOptimizeAutoValue(boolean b);

    public abstract Builder setFrontend(Frontend frontend);

    public abstract Builder setBackend(Backend backend);

    public abstract Builder setWasmEntryPoints(ImmutableSet<String> wasmEntryPoints);

    public abstract Builder setDefinesForWasm(ImmutableMap<String, String> definesForWasm);

    public abstract Builder setWasmRemoveAssertStatement(boolean wasmRemoveAssertStatement);

    public abstract Builder setNullMarkedSupported(boolean isNullMarkedSupported);

    public abstract Builder setKotlincOptions(ImmutableList<String> kotlincOptions);

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
