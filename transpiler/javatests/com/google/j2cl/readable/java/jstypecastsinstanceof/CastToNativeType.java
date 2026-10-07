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
package jstypecastsinstanceof;

import javaemul.internal.annotations.UncheckedCast;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class CastToNativeType {
  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsType {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  public static class NativeObject<K, V> {}

  @SuppressWarnings({"unused", "rawtypes", "unchecked"})
  public void test() {
    Object a = new NativeJsType();
    NativeJsType b = (NativeJsType) a;
    NativeJsType[] d = (NativeJsType[]) a;

    NativeObject e = (NativeObject) a;
    NativeObject<String, Object> f = (NativeObject<String, Object>) a;
    NativeObject[] g = (NativeObject[]) a;
    NativeObject<String, Object>[] h = (NativeObject<String, Object>[]) a;
  }

  @SuppressWarnings({"unused", "rawtypes"})
  private void testUncheckedCast() {
    NativeJsType a = uncheckedCast(new Object());
    NativeJsType[] b = uncheckedCast(new Object());
    NativeObject c = uncheckedCast(new Object());
    NativeObject<String, Object> d = uncheckedCast(new Object());
    NativeObject[] e = uncheckedCast(new Object());
    NativeObject<String, Object>[] f = uncheckedCast(new Object());
  }

  @UncheckedCast
  @SuppressWarnings("unchecked")
  private static <T> T uncheckedCast(Object o) {
    return (T) o;
  }

  @Wasm("nop") // instanceof on native types not yet supported in Wasm.
  @SuppressWarnings("unused")
  private void testInstanceOfNative() {
    Object a = new NativeJsType();
    boolean c = a instanceof NativeJsType;
    c = a instanceof NativeJsType[];
    c = a instanceof NativeObject;
    c = a instanceof NativeObject[];
  }

  @SuppressWarnings("unused")
  private void testInstanceOfNonNative() {
    NativeJsType b = new NativeJsType();
    boolean c = b instanceof Object;
    c = b instanceof Comparable<?> i;
  }
}
