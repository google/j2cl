/*
 * Copyright 2019 Google Inc.
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

import com.google.errorprone.annotations.FormatMethod;

/** Exception that signals an error in the compiler code. */
public class InternalCompilerError extends Error {
  @FormatMethod
  public InternalCompilerError(Throwable cause, String format, Object... params) {
    super(String.format(format, params), cause);
  }

  @FormatMethod
  public InternalCompilerError(String format, Object... params) {
    super(String.format(format, params));
  }

  public InternalCompilerError(String message) {
    super(message);
  }

  public InternalCompilerError(Throwable cause, String message) {
    super(message, cause);
  }
}
