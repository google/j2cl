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

import java.nio.file.Path;

/** Profiler facade for the Bazel actions. */
public interface Profiler {

  static Profiler create(Path workdir, Path profileOutput) {
    if (profileOutput == null) {
      return new Profiler() {
        @Override
        public void stopProfile() {}
      };
    }
    try {
      Class<?> clazz = Class.forName("com.google.j2cl.common.bazel.profiler.ProfilerImpl");
      return (Profiler)
          clazz.getDeclaredConstructor(Path.class).newInstance(workdir.resolve(profileOutput));
    } catch (ReflectiveOperationException e) {
      throw new LinkageError("Failed to start profiler", e);
    }
  }

  public void stopProfile();
}
