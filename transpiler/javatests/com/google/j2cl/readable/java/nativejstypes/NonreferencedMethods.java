/*
 * Copyright 2024 Google Inc.
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

import jsinterop.annotations.JsType;

/** Native JsType with a private members. */
public class NonreferencedMethods {
  // Public class with private members that are not referenced.
  @JsType(name = "NativeNonreferencedMethods", namespace = "nativejstypes", isNative = true)
  public static class NativePrivateMembersOnPublicClass {
    private int x;

    private static int s;

    private NativePrivateMembersOnPublicClass(int x) {}

    private native int getInstance1();

    private static native int getStatic();
  }

  // Private class with visible members that are not referenced.
  @JsType(name = "NativeNonreferencedMethods", namespace = "nativejstypes", isNative = true)
  private static class NativeMembersOnPrivateClass {
    public static int s;

    public NativeMembersOnPrivateClass(int x) {}

    // Not referenced polymorphic.
    public native int getInstance2();

    public static native int getStatic();
  }
}
