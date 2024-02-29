/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.transpiler.backend;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.EntryPointPattern;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.SourceUtils.FileInfo;
import java.nio.file.Path;
import javax.annotation.Nullable;

/** Configuration for backend. */
public interface BackendOptions {

  Output getOutput();

  @Nullable
  Path getLibraryInfoOutput();

  ImmutableList<FileInfo> getNativeSources();

  @Nullable
  ImmutableMap<String, String> getDefinesForWasm();

  ImmutableList<EntryPointPattern> getWasmEntryPointPatterns();

  boolean getWasmEnableNonNativeJsEnum();

  boolean getOptimizeAutoValue();

  boolean getEmitReadableLibraryInfo();

  boolean getEmitReadableSourceMap();

  boolean getGenerateKytheIndexingMetadata();

  boolean isNullMarkedSupported();
}
