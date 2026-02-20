/*
 * Copyright 2025 Google Inc.
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
package com.google.j2cl.common.bazel.profiler;

import com.google.monitoring.runtime.cpu.JvmProfiler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

class ProfilerImpl implements Profiler {

  private final Path profileOutput;
  private final JvmProfiler profiler;

  public ProfilerImpl(Path profileOutput) {
    this.profileOutput = profileOutput;
    this.profiler = JvmProfiler.getProfiler();
    profiler.startProfiling(Duration.ofSeconds(60));
  }

  @Override
  public void stopProfile() {
    try (OutputStream os = Files.newOutputStream(profileOutput)) {
      profiler.stopProfiling(os);
    } catch (IOException e) {
      throw new AssertionError("Failed to stop profiler", e);
    }
  }
}
