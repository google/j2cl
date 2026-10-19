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
package jsinteroptests;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Tests native JsType functionality. */
public class NativeJsTypeTest {
  public static void testAll() {
    testBridgesNativeSubclass();
    testAccessNamespaceAsProperty();
  }

  interface InterfaceWithDefaultMethods {
    String method();

    default String defaultMethod() {
      return "default-method";
    }
  }

  private abstract static class AbstractSuperWithPackagePrivateMethod {
    @JsConstructor
    AbstractSuperWithPackagePrivateMethod() {}

    String packagePrivateMethod() {
      return "package-private";
    }
  }

  @JsType(namespace = "woo.NativeJsTypeTest", name = "AbstractJavaSuperclass")
  private abstract static class AbstractSuperClassForJs
      extends AbstractSuperWithPackagePrivateMethod implements InterfaceWithDefaultMethods {
    public AbstractSuperClassForJs() {}

    @Override
    public abstract String packagePrivateMethod();

    @Override
    public abstract String method();
  }

  private static void testBridgesNativeSubclass() {
    AbstractSuperClassForJs nativeSubclass = createBridgesNativeSubclass();
    // Tests that the default method is implemented.
    assertEquals("default-method", nativeSubclass.defaultMethod());

    assertEquals("package-private-override", nativeSubclass.packagePrivateMethod());
    // Tests package private bridge.
    assertEquals(
        "package-private-override",
        ((AbstractSuperWithPackagePrivateMethod) (Object) nativeSubclass).packagePrivateMethod());

    assertEquals("method", nativeSubclass.method());
    // Tests JsMethod bridge.
    assertEquals("method", ((InterfaceWithDefaultMethods) (Object) nativeSubclass).method());
  }

  @JsMethod(namespace = "woo.NativeJsTypeTest.NativeBridgesSubclass", name = "create")
  private static native AbstractSuperClassForJs createBridgesNativeSubclass();

  @JsProperty(namespace = "woo.NativeJsTypeTest.PropertyNamespace", name = "")
  private static native String getPropertyNamespace();

  private static void testAccessNamespaceAsProperty() {
    assertEquals("foo", getPropertyNamespace());
  }
}
