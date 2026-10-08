/*
 * Copyright 2026 Google Inc.
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
package nativejstypes;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsType;

/** Intersection types that have native components. */
public class IntersectionTypes {

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  interface NativeInterface {}

  interface NonNativeInterface {}

  // The intersection erases to its first component, which is native, so values are native.
  @SuppressWarnings({"ReferenceEquality", "unused"})
  static <T extends NativeInterface & NonNativeInterface> void testNativeFirst(T t, Object o) {
    boolean b = t == o;
    b = o == (NativeInterface & NonNativeInterface) o;
  }

  // The intersection erases to its first component, which is not native, so values are not native.
  @SuppressWarnings({"ReferenceEquality", "unused"})
  static <T extends NonNativeInterface & NativeInterface> void testNonNativeFirst(T t, Object o) {
    boolean b = t == o;
    b = o == (NonNativeInterface & NativeInterface) o;
  }
}
