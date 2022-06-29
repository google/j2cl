/*
 * Copyright 2022 Google Inc.
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
package javaemul.internal.ktstubs;

import javaemul.internal.InternalPreconditions;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * Temporary implementation of kotlin.jvm.internal.Intrinsics needed at runtime. This class will be
 * remoe once we transpile kotlin stdlib.
 */
@JsType(namespace = "kotlin.jvm.internal")
public final class Intrinsics {
  private Intrinsics() {}

  @JsIgnore
  public static void checkNotNull(Object object, String message) {
    InternalPreconditions.checkNotNull(object, message);
  }
}
