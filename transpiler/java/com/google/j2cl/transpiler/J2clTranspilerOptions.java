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
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.EntryPointPattern;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.backend.Backend;
import com.google.j2cl.transpiler.backend.BackendOptions;
import com.google.j2cl.transpiler.frontend.Frontend;
import com.google.j2cl.transpiler.frontend.common.FrontendOptions;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** Configuration for the transpiler. */
@AutoValue
public abstract class J2clTranspilerOptions implements FrontendOptions, BackendOptions {

  public abstract Frontend getFrontend();

  public abstract Backend getBackend();

  public static Builder newBuilder() {
    return new AutoValue_J2clTranspilerOptions.Builder()
        .setOptimizeAutoValue(false)
        .setNullMarkedSupported(false)
        .setEnableWasmCustomDescriptors(false)
        .setEnableWasmCustomDescriptorsJsInterop(false);
  }

  @Override
  @Nullable
  public abstract ImmutableList<EntryPointPattern> getWasmEntryPointPatterns();

  @Override
  public abstract ImmutableList<String> getForbiddenAnnotations();

  /** A Builder for J2clTranspilerOptions. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setSources(List<FileInfo> infos);

    public abstract Builder setNativeSources(List<FileInfo> files);

    public abstract Builder setClasspaths(List<String> entries);

    public abstract Builder setSystem(String jdkSystem);

    public abstract Builder setOutput(Output output);

    public abstract Builder setTargetLabel(String targetLabel);

    public abstract Builder setLibraryInfoOutput(@Nullable Path path);

    public abstract Builder setEmitReadableLibraryInfo(boolean b);

    public abstract Builder setEmitReadableSourceMap(boolean b);

    public abstract Builder setSourceMappingPathPrefix(String value);

    public abstract Builder setGenerateKytheIndexingMetadata(boolean b);

    public abstract Builder setOptimizeAutoValue(boolean b);

    public abstract Builder setFrontend(Frontend frontend);

    public abstract Builder setBackend(Backend backend);

    public Builder setWasmEntryPointStrings(List<String> wasmEntryPoints) {
      return setWasmEntryPointPatterns(
          wasmEntryPoints.stream().map(EntryPointPattern::from).collect(toImmutableList()));
    }

    abstract Builder setWasmEntryPointPatterns(List<EntryPointPattern> entryPointSpecs);

    public abstract Builder setDefinesForWasm(Map<String, String> definesForWasm);

    public abstract Builder setEnableWasmCustomDescriptors(boolean enableWasmCustomDescriptors);

    public abstract Builder setEnableWasmCustomDescriptorsJsInterop(
        boolean enableWasmCustomDescriptorsJsInterop);

    public abstract Builder setNullMarkedSupported(boolean isNullMarkedSupported);

    public abstract Builder setJavacOptions(List<String> javacOptions);

    public abstract Builder setKotlincOptions(List<String> kotlincOptions);

    public abstract Builder setEnableKlibs(boolean enableKlibs);

    public abstract Builder setDependencyKlibs(List<String> dependencyKlibs);

    public abstract Builder setForbiddenAnnotations(List<String> forbiddenAnnotations);

    public abstract Builder setObjCNamePrefix(String objCNamePrefix);

    abstract J2clTranspilerOptions autoBuild();

    public J2clTranspilerOptions build(Problems problems) {
      J2clTranspilerOptions options = autoBuild();

      // Validate the entry point syntax.
      for (EntryPointPattern entryPointPattern : options.getWasmEntryPointPatterns()) {
        if (!entryPointPattern.isValid()) {
          problems.error(
              "Invalid entry point syntax in '%s'.",
              entryPointPattern.getEntryPointPatternString());
        }
      }
      problems.abortIfHasErrors();

      checkState(
          !options.getEmitReadableSourceMap() || !options.getGenerateKytheIndexingMetadata());
      checkState(!options.getEmitReadableLibraryInfo() || options.getLibraryInfoOutput() != null);
      return options;
    }
  }
}
