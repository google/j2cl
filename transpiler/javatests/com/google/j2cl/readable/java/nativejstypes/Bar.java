/*
 * Copyright 2017 Google Inc.
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

/**
 * Native JsType without "namespace" or "name".
 */
@JsType(isNative = true)
public class Bar {
  public int x;
  public int y;
  public static int f;

  public Bar(int x, int y) {};

  public native int product();

  public static native int getStatic();

  @JsType(isNative = true)
  static class Inner {
    public Inner(int n) {}

    public native int square();

    public static native int getInnerStatic();
  }
}

/** Native inner JsType. */
@JsType(isNative = true, name = "Bar.Inner")
class BarInnerWithDotInName {
  public BarInnerWithDotInName(int n) {}

  public native int square();

  public static native int getInnerStatic();
}

@JsType(isNative = true, namespace = "nativejstypes", name = "Bar.Inner")
class BarInnerWithDotInNameAndHasNamespace {
  public BarInnerWithDotInNameAndHasNamespace(int n) {}

  public native int square();

  public static native int getInnerStatic();
}
