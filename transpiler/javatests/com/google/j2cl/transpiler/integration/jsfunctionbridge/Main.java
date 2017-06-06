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
package com.google.j2cl.transpiler.integration.jsfunctionbridge;

import jsinterop.annotations.JsFunction;

public class Main {
  public static void main(String... args) {
    test();
  }

  public static void test() {
    ApplyFunction<String> foo =
        new ApplyFunction<String>() {
          public String field = "a";

          @Override
          public String apply(String s) {
            return s + field + fun();
          }

          public String fun() {
            return field + "d";
          }
        };
    assert ("eaad".equals(callGeneric(foo, "e")));
    try {
      callGeneric(foo, new Object());
      assert false;
    } catch (ClassCastException e) {
      // expected.
    }
    assert ("eaad".equals(callParametric(foo, "e")));
    assert ("eaad".equals(foo.apply("e")));
    assert "hello".equals(new Identity().apply("hello"));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Object callGeneric(ApplyFunction af, Object o) {
    return af.apply(o);
  }

  public static String callParametric(ApplyFunction<String> af, String s) {
    return af.apply(s);
  }

  @JsFunction
  interface ApplyFunction<T> {
    T apply(T element);
  }

  private static final class Identity implements ApplyFunction<Object> {
    @Override
    public Object apply(Object element) {
      return element;
    }
  }
}
