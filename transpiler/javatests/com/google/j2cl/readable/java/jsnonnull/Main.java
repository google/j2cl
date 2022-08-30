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
package jsnonnull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;

public class Main {
  @JsNonNull private String f1 = "Hello";
  private String f2 = null;
  private @JsNonNull List<@JsNonNull String> f4 = new ArrayList<>();
  private List<@JsNonNull String> f5 = new ArrayList<>();
  private @JsNonNull List<String> f6 = new ArrayList<>();
  private List<String> f7 = null;
  private @JsNonNull String @JsNonNull [] f8 = {};
  // Nonnullable array of nullable strings.
  private String @JsNonNull [] f9 = {};
  // Nullable array of non-nullable strings.
  private @JsNonNull String[] f10 = {};
  // Conversion from generic type parameter.
  private List<String> f12 = new ArrayList<>();

  @JsConstructor
  public Main(@JsNonNull String a) {}

  @JsNonNull
  public String m1(@JsNonNull String a, @JsNonNull List<@JsNonNull Double> b, String c) {
    return "";
  }

  public String m2(String a, @JsNonNull List<@JsNonNull Double> b) {
    return "";
  }

  @JsMethod
  public String m3(@JsNonNull String a, @JsNonNull String... args) {
    return null;
  }

  public void m4(@JsNonNull MyFunction f) {}

  @JsFunction
  interface MyFunction {
    @JsNonNull
    String x(@JsNonNull String a);
  }
  // Should implement Comparator<string>
  public static class StringComparator implements Comparator<@JsNonNull String> {
    @Override
    public int compare(@JsNonNull String a, @JsNonNull String b) {
      return 0;
    }
  }

  // Should implement Comparator<?string>
  public static class NullableStringComparator implements Comparator<String> {
    @Override
    public int compare(String a, String b) {
      return 0;
    }
  }

  public static class NonNullableStringSubNullableComparator extends NullableStringComparator {
    // Tests that an override can strengthen the type of a parameter to non-nullable without
    // triggering a warning in jscompiler.
    @Override
    public int compare(@JsNonNull String a, @JsNonNull String b) {
      return 0;
    }
  }

  interface NonNullableTemplatedReturn<T> {
    @JsNonNull
    T get();

    @JsNonNull
    T[] getArray();
  }

  interface NonNullableTemplate<T extends @JsNonNull Object> {
    T get();

    T[] getArray();
  }
}
