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
package com.google.j2cl.transpiler.integration.casttonativetypevariable;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
  public static class Foo<T extends Foo<T>> {
    @SuppressWarnings({"unchecked", "unused"})
    @JsOverlay
    public final T getSelf() {
      Object o = new Object[] {new Object()};
      T[] ts = (T[]) o; // cast to T[].
      return (T) this;
    }
  }

  public static class SubFoo extends Foo<SubFoo> {
    @JsConstructor
    public SubFoo() {}
  }

  public static void main(String... args) {
    SubFoo sf = new SubFoo();
    assert sf.getSelf() == sf;

    testGenericType();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  public static class NativeObject<K, V> {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void testGenericType() {
    Object a = new NativeObject<String, Object>();

    NativeObject e = (NativeObject) a;
    assert e == a;
    NativeObject<String, Object> f = (NativeObject<String, Object>) a;
    assert f == a;
    assert a instanceof NativeObject;

    Object os = new NativeObject[] {e};
    NativeObject[] g = (NativeObject[]) os;
    assert g[0] == e;
    NativeObject<String, Object>[] h = (NativeObject<String, Object>[]) os;
    assert h[0] == e;
    assert os instanceof NativeObject[];
  }
}
