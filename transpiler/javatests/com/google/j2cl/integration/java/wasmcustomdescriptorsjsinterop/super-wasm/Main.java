/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package wasmcustomdescriptorsjsinterop;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Tests J2WASM custom descriptor jsinterop features. */
public final class Main {
  public static void main(String... args) throws Exception {
    testConstructor();
    testMethod();
    testProperty();
    testInheritedMethod();
    testInterfaceMethod();
  }

  private static void testConstructor() {
    BaseJsType baseJsType = newBaseJsType();

    SomeJsType someJsType = newSomeJsType(123);
    assertTrue(someJsType.field == 123);
  }

  private static void testMethod() {
    SomeJsType someJsType = new SomeJsType(123);
    assertTrue(callGetNumber(someJsType) == 11);
    assertTrue(callGetString(someJsType).equals("str"));
    assertTrue(callPackagePrivateMethod(someJsType).equals("pp"));
  }

  private static void testProperty() {
    SomeJsType someJsType = new SomeJsType(123);

    assertTrue(getField(someJsType) == 123);
    setField(someJsType, 456);
    assertTrue(getField(someJsType) == 456);

    setStaticField(789);
    assertTrue(getStaticField() == 789);

    assertTrue(getReadOnlyField(someJsType) == 111);
    assertTrue(getStaticReadOnlyField() == 222);

    assertTrue(getReadOnlyProperty(someJsType) == 333);
    assertTrue(getStaticReadOnlyProperty() == 444);

    assertTrue(getReadWriteProperty(someJsType) == 0);
    setReadWriteProperty(someJsType, 567);
    assertTrue(getReadWriteProperty(someJsType) == 567);
  }

  private static void testInheritedMethod() {
    SubJsType subJsType = new SubJsType();
    assertTrue(subJsType.field == 12);
    assertTrue(callGetNumber(subJsType) == 22);
    assertTrue(callGetString(subJsType).equals("str"));
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  static class BaseJsType {
    @JsConstructor
    public BaseJsType() {}

    // Non-js method.
    String packagePrivateMethod() {
      return "";
    }
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  static class SomeJsType extends BaseJsType {
    public int field;
    public static int staticField = 0;

    public final int readOnlyField = 111;
    public static final int staticReadOnlyField = 222;

    public SomeJsType(int field) {
      this.field = field;
    }

    public int getNumber() {
      return 11;
    }

    public String getString() {
      return "str";
    }

    @JsProperty
    public int getReadOnlyProperty() {
      return 333;
    }

    @JsProperty
    public static int getStaticReadOnlyProperty() {
      return 444;
    }

    private int ignoredField;

    @JsProperty
    public int getReadWriteProperty() {
      return ignoredField;
    }

    @JsProperty
    public void setReadWriteProperty(int value) {
      ignoredField = value;
    }

    @JsMethod
    String packagePrivateMethod() {
      return "pp";
    }
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  static class SubJsType extends SomeJsType {
    public SubJsType() {
      super(12);
    }

    @Override
    public int getNumber() {
      return 22;
    }
  }

  private static void testInterfaceMethod() {
    JsInterface jsInterface = new JsInterfaceImpl();
    assertTrue(callInterfaceMethod(jsInterface) == 1);

    JsInterfaceGetNumber jsInterfaceGetNumber = new JsInterfaceGetNumberImpl();
    assertTrue(callGetNumber((SomeJsType) jsInterfaceGetNumber) == 22);
    assertTrue(callInterfaceGetNumber(jsInterfaceGetNumber) == 22);

    JsInterfaceRenamedMethod jsInterfaceRenamedMethod = new JsInterfaceRenamedMethodImpl();
    assertTrue(callGetNumber((SomeJsType) jsInterfaceRenamedMethod) == 22);
    // TODO(b/499366074): Test when this case is supported with a bridge method.
    // assertTrue(callInterfaceRenamedMethod(jsInterfaceRenamedMethod) == 22);

    JsInterfaceDefaultMethod jsInterfaceDefaultMethod = new JsInterfaceDefaultMethodImpl();
    assertTrue(callInterfaceDefaultMethod(jsInterfaceDefaultMethod) == 9876);
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  interface JsInterface {
    int interfaceMethod();
  }

  static class JsInterfaceImpl implements JsInterface {
    @Override
    public int interfaceMethod() {
      return 1;
    }
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  interface JsInterfaceGetNumber {
    int getNumber();
  }

  static class JsInterfaceGetNumberImpl extends SubJsType implements JsInterfaceGetNumber {
    @JsConstructor
    public JsInterfaceGetNumberImpl() {}
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  interface JsInterfaceRenamedMethod {
    @JsMethod(name = "renamed")
    int getNumber();
  }

  static class JsInterfaceRenamedMethodImpl extends SubJsType implements JsInterfaceRenamedMethod {
    @JsConstructor
    public JsInterfaceRenamedMethodImpl() {}
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  interface JsInterfaceDefaultMethod {
    default int m() {
      return 9876;
    }
  }

  static class JsInterfaceDefaultMethodImpl implements JsInterfaceDefaultMethod {
    @JsConstructor
    public JsInterfaceDefaultMethodImpl() {}
  }

  @JsMethod(namespace = "nativehelper")
  static native BaseJsType newBaseJsType();

  @JsMethod(namespace = "nativehelper")
  static native SomeJsType newSomeJsType(int value);

  @JsMethod(namespace = "nativehelper")
  static native int callGetNumber(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native String callGetString(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native int getField(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native void setField(SomeJsType someJsType, int value);

  @JsMethod(namespace = "nativehelper")
  static native int getStaticField();

  @JsMethod(namespace = "nativehelper")
  static native void setStaticField(int value);

  @JsMethod(namespace = "nativehelper")
  static native int getReadOnlyField(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native int getStaticReadOnlyField();

  @JsMethod(namespace = "nativehelper")
  static native int getReadOnlyProperty(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native int getStaticReadOnlyProperty();

  @JsMethod(namespace = "nativehelper")
  static native int getReadWriteProperty(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native void setReadWriteProperty(SomeJsType someJsType, int value);

  @JsMethod(namespace = "nativehelper")
  static native String callPackagePrivateMethod(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceMethod(JsInterface jsInterface);

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceGetNumber(JsInterfaceGetNumber jsInterface);

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceRenamedMethod(JsInterfaceRenamedMethod jsInterface);

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceDefaultMethod(JsInterfaceDefaultMethod jsInterface);
}
