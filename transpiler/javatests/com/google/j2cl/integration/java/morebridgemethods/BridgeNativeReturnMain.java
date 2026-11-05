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
package morebridgemethods;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class BridgeNativeReturnMain {
  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  public static class NativeObject {}

  public static class Container<T> {
    public T get() {
      return null;
    }
  }

  public static class NativeObjectContainer extends Container<NativeObject> {
    public final NativeObject value = new NativeObject();

    // Specializes the return type of Container.get() to a native type.
    @Override
    public NativeObject get() {
      return value;
    }
  }

  @SuppressWarnings("rawtypes")
  public static Object callGetByContainer(Container c) {
    return c.get();
  }

  public static void test() {
    NativeObjectContainer c = new NativeObjectContainer();
    Container<NativeObject> container = c;
    assertTrue(container.get() == c.value);
    assertTrue(callGetByContainer(c) == c.value);
  }
}
