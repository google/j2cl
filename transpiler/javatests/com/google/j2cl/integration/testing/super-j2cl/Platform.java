/*
 * Copyright 2025 Google Inc.
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
package com.google.j2cl.integration.testing;

import jsinterop.annotations.JsProperty;

final class Platform {
  @JsProperty // TODO(b/422007932): Remove annotation when the RTA bug is resolved.
  static final int RUNTIME_ENVIRONMENT = Environment.JAVASCRIPT;

  static final boolean IS_J2KT = isJ2kt();

  @JsProperty(
      namespace = "com.google.j2cl.integration.testing.CompilationEnvironment",
      name = "IS_J2KT")
  private static native boolean isJ2kt();

  private Platform() {}
}
