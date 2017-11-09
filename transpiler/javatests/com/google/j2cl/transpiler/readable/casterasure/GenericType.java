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
package com.google.j2cl.transpiler.readable.casterasure;

import java.util.ArrayList;

/**
 * Tests that a cast is inserted due to erasures.
 */
public class GenericType<T> {
  T field;

  T method() {
    return null;
  }

  public static void addErasureCast() {
    String str = new GenericType<String>().field;
    str = new GenericType<String>().method();
  }

  public static <T> GenericType<T> inferGeneric(T foo) {
    return new GenericType<>();
  }

  public static GenericType<GenericType<String>> tightenType(GenericType<String> foo) {
    if (foo != null) {
      // Without a cast to fix it, JSCompiler will infer the type of this return statement to be
      // ?Foo<!Foo<?string>>, which does not match the return type, ?Foo<?Foo<?string>>.
      return inferGeneric(foo);
    }
    return null;
  }


  public static void main() {
    ArrayList<Object> list = newArrayList("foo");
    // list will be tightened to ArrayList<String> hence OTI would complain below without a cast.
    acceptsArrayListOfObject(list);
  }

  public static <V> ArrayList<V> newArrayList(V foo) {
    return new ArrayList<>();
  }

  public static void acceptsArrayListOfObject(ArrayList<Object> foo) {
    // empty
  }
}
