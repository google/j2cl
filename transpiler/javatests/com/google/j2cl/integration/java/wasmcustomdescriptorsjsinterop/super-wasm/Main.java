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

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
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
    testJsSubtyping();
    testJsFunctionWithJsType();
    testEntryPoint();
    testMethodWithNativeAndLong();
    testNativeJsTypeConsumer();
    testJsEnum();
  }

  private static void testMethodWithNativeAndLong() {
    SomeJsType someJsType = new SomeJsType(123);
    MyNativeType nativeType = new MyNativeType(456);
    assertEquals(456L + 789L, callMethodWithNativeAndLong(someJsType, nativeType, 789L));
    assertEquals(456L, callMethodWithNativeAndLong(someJsType, nativeType, null));
  }

  private static void testNativeJsTypeConsumer() {
    NativeJsTypeConsumer consumer = newNativeJsTypeConsumer();
    MyNativeType nativeType = new MyNativeType(456);

    assertEquals(456, consumer.callGetValue(nativeType));

    assertEquals(456, callConsumerGetValue(consumer, nativeType));
    assertEquals(466, callConsumerAdd(consumer, nativeType, 10));
    assertEquals(456, callConsumerGetField(consumer, nativeType));
    assertEquals(456L + 789L, callConsumerCombineWithLong(consumer, nativeType, 789L));
    assertEquals(456L, callConsumerCombineWithLong(consumer, nativeType, null));

    // Also test direct Java calls
    assertEquals(456, consumer.callGetValue(nativeType));
    assertEquals(466, consumer.callAdd(nativeType, 10));
    assertEquals(456, consumer.callGetField(nativeType));
    assertEquals(456L + 789L, consumer.callCombineWithLong(nativeType, 789L));
    assertEquals(456L, consumer.callCombineWithLong(nativeType, null));
  }

  private static void testConstructor() {
    BaseJsType baseJsType = newBaseJsType();

    SomeJsType someJsType = newSomeJsType(123);
    assertTrue(someJsType.field == 123);

    SomeJsType.CapturesOuter capturesOuter = newCapturesOuter(someJsType);
    assertTrue(callGetOuter(capturesOuter) == someJsType);
  }

  private static void testMethod() {
    SomeJsType someJsType = new SomeJsType(123);
    assertTrue(callGetNumber(someJsType) == 11);
    assertTrue(someJsType.getNumber(456) == 456);
    assertTrue(callGetString(someJsType).equals("str"));
    assertTrue(callPackagePrivateMethod(someJsType).equals("pp"));
    assertTrue(callReturnSelf(someJsType) == someJsType);
    assertTrue(callTakesSelf(someJsType, someJsType));
    assertTrue(callGetNumberViaStaticMethod(someJsType) == 11);
    assertTrue(callGetLong(someJsType) == 123456789L);
    assertEquals(1234567890123456789L, callGetPrimitiveLong(someJsType));
    assertEquals(
        3333333333333333333L,
        callAddPrimitiveLong(someJsType, 1111111111111111111L, 2222222222222222222L));
    assertTrue(callGetNativeJsType(someJsType).getNumber() == 929);
    assertEquals(
        11 + 5 + 6, callMethodWithTypeParameters(someJsType, someJsType, "hello", (Double) 6.0));
    assertEquals(3 + 5 + 6, callMethodWithTypeParameters(someJsType, "bye", "hello", (Double) 6.0));
  }

  private static void testProperty() {
    SomeJsType someJsType = new SomeJsType(123);

    assertTrue(getField(someJsType) == 123);
    setField(someJsType, 456);
    assertTrue(getField(someJsType) == 456);

    assertEquals(1234567890123456789L, getLongField(someJsType));
    setLongField(someJsType, 987654321098765432L);
    assertEquals(987654321098765432L, getLongField(someJsType));

    setStaticField(789);
    assertTrue(getStaticField() == 789);

    assertTrue(getReadOnlyField(someJsType) == 111);
    assertTrue(getStaticReadOnlyField() == 222);
    assertTrue(getStaticFieldSameJsNameAsInstanceField() == 666);

    assertTrue(getReadOnlyProperty(someJsType) == 333);
    assertTrue(getStaticReadOnlyProperty() == 444);
    assertTrue(getStaticPropertySameJsNameAsInstanceProperty() == 555);

    assertTrue(getReadWriteProperty(someJsType) == 0);
    setReadWriteProperty(someJsType, 567);
    assertTrue(getReadWriteProperty(someJsType) == 567);
  }

  private static void testInheritedMethod() {
    SubJsType subJsType = new SubJsType();
    assertTrue(subJsType.field == 12);
    assertTrue(callGetNumber(subJsType) == 22);
    assertTrue(callGetString(subJsType).equals("str"));

    AbstractJsTypeImpl abstractJsType = new AbstractJsTypeImpl();
    assertTrue(callAbstractMethod(abstractJsType) == 23);
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
    public long longField = 1234567890123456789L;
    public static int staticField = 0;

    public final int readOnlyField = 111;
    public static final int staticReadOnlyField = 222;

    @JsProperty(name = "readOnlyField")
    public static final int staticFieldSameJsNameAsInstanceField = 666;

    public SomeJsType(int field) {
      this.field = field;
    }

    @JsIgnore
    public int getNumber(int arg) {
      return arg;
    }

    public int getNumber() {
      return 11;
    }

    @JsNonNull
    public String getString() {
      return "str";
    }

    @JsNonNull
    public Long getLong() {
      return 123456789L;
    }

    public long getPrimitiveLong() {
      return 1234567890123456789L;
    }

    public long addPrimitiveLong(long a, long b) {
      return a + b;
    }

    @JsNonNull
    public NativeJsType getNativeJsType() {
      return new NativeJsType(929);
    }

    @JsProperty
    public int getReadOnlyProperty() {
      return 333;
    }

    @JsProperty
    public static int getStaticReadOnlyProperty() {
      return 444;
    }

    @JsProperty(name = "readOnlyProperty")
    public static int getStaticPropertySameJsNameAsInstanceProperty() {
      return 555;
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

    public SomeJsType returnSelf() {
      return this;
    }

    public boolean takesSelf(SomeJsType self) {
      return self == this;
    }

    public long methodWithNativeAndLong(MyNativeType nativeType, Long boxedLong) {
      return nativeType.value + (boxedLong != null ? boxedLong : 0L);
    }

    public static int staticMethod(SomeJsType self) {
      return self.getNumber();
    }

    public <T, U extends String, V extends Double> V withTypeParameters(T t, U u, V v) {
      double result = 0;
      if (t instanceof SomeJsType someType) {
        result += someType.getNumber();
      } else if (t instanceof String s) {
        result += s.length();
      } else if (t instanceof Double d) {
        result += d;
      }
      result += u.length();
      result += v;
      return (V) (Double) result;
    }

    @JsType
    public class CapturesOuter {
      public CapturesOuter() {}

      public SomeJsType getOuter() {
        return SomeJsType.this;
      }
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

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  abstract static class AbstractJsType {
    public abstract int abstractMethod();
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  static class AbstractJsTypeImpl extends AbstractJsType {
    @Override
    public int abstractMethod() {
      return 23;
    }
  }

  private static void testInterfaceMethod() {
    JsInterface jsInterface = new JsInterfaceImpl();
    assertTrue(callInterfaceMethod(jsInterface) == 1);

    JsInterfaceGetNumber jsInterfaceGetNumber = new JsInterfaceGetNumberImpl();
    assertTrue(callGetNumber((SomeJsType) jsInterfaceGetNumber) == 22);
    assertTrue(callInterfaceGetNumber(jsInterfaceGetNumber) == 22);

    JsInterfaceRenamedMethod jsInterfaceRenamedMethod = new JsInterfaceRenamedMethodImpl();
    assertTrue(callInterfaceRenamedMethod(jsInterfaceRenamedMethod) == 23);

    JsInterfaceDefaultMethod jsInterfaceDefaultMethod = new JsInterfaceDefaultMethodImpl();
    assertTrue(callInterfaceDefaultMethod(jsInterfaceDefaultMethod) == 9876);

    assertTrue(callInterfaceStaticMethod() == 123);
    setInterfaceStaticProperty(789);
    assertTrue(callInterfaceStaticProperty() == 789);
    assertTrue(callInterfaceStaticField() == 999);

    JsInterfaceAccidentalImpl jsInterfaceAccidentalImpl = new JsInterfaceAccidentalImpl();
    assertTrue(callAccidentalMethod(jsInterfaceAccidentalImpl) == 2);
    assertTrue(callInterfaceMethod(jsInterfaceAccidentalImpl) == 2);

    JsInterfaceAccidentalDefaultMethodImpl jsInterfaceAccidentalDefaultMethodImpl =
        new JsInterfaceAccidentalDefaultMethodImpl();
    assertTrue(callAccidentalDefaultMethod(jsInterfaceAccidentalDefaultMethodImpl) == 3);
    assertTrue(callInterfaceDefaultMethod(jsInterfaceAccidentalDefaultMethodImpl) == 3);

    assertTrue(callInterfaceMethod(() -> 23) == 23);
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
    int mightBeRenamed();
  }

  static class BaseRenamedMethod {
    public int mightBeRenamed() {
      return 23;
    }
  }

  static class JsInterfaceRenamedMethodImpl extends BaseRenamedMethod
      implements JsInterfaceRenamedMethod {
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

  private static int jsInterfaceStaticProperty = 456;

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  interface JsInterfaceStaticMethod {
    static int staticMethod() {
      return 123;
    }

    @JsProperty
    static int getStaticProperty() {
      return jsInterfaceStaticProperty;
    }

    @JsProperty
    static void setStaticProperty(int value) {
      jsInterfaceStaticProperty = value;
    }

    @JsProperty static int STATIC_FIELD = 999;
  }

  static class NonJsBase {
    public final int interfaceMethod() {
      return 2;
    }

    public final int m() {
      return 3;
    }
  }

  static class JsInterfaceAccidentalImpl extends NonJsBase implements JsInterface {}

  static class JsInterfaceAccidentalDefaultMethodImpl extends NonJsBase
      implements JsInterfaceDefaultMethod {}

  private static void testJsSubtyping() {
    try {
      createJsSubtype();
      fail("JS should throw when constructing a subtype");
    } catch (JsException expected) {
      assertTrue(expected.getMessage().contains("cannot be subtyped"));
    }
  }

  private static void testJsFunctionWithJsType() {
    MyJsFunctionWithObject objectLambda = o -> o;
    SomeJsType someJsType = new SomeJsType(123);
    assertEquals(someJsType, callFunctionWithObjectInJs(objectLambda, someJsType));
    assertTrue(callFunctionWithObjectInJs(objectLambda, someJsType) == someJsType);

    MyJsFunctionWithObject jsFunctionWithObject = getFunctionWithObjectFromJs();
    SomeJsType someJsType2 = new SomeJsType(123);
    assertTrue(jsFunctionWithObject.foo(someJsType2) == someJsType2);

    MyJsFunctionWithJsType jsTypeIdentityLambda = s -> s;
    assertEquals(someJsType, callFunctionWithJsTypeInJs(jsTypeIdentityLambda, someJsType));
    assertTrue(callFunctionWithJsTypeInJs(jsTypeIdentityLambda, someJsType) == someJsType);
    assertTrue(jsTypeIdentityLambda.foo(someJsType) == someJsType);

    MyJsFunctionWithJsType jsTypeLambda = s -> new SomeJsType(s.field + 10);
    assertEquals(133, callFunctionWithJsTypeInJs(jsTypeLambda, someJsType).field);

    MyJsFunctionWithJsType jsFunctionWithJsType = getFunctionWithJsTypeFromJs();
    assertTrue(jsFunctionWithJsType.foo(someJsType2) == someJsType2);
  }


  @JsFunction
  interface MyJsFunctionWithObject {
    Object foo(Object o);
  }

  @JsFunction
  interface MyJsFunctionWithJsType {
    SomeJsType foo(SomeJsType s);
  }

  @JsMethod(namespace = "nativehelper", name = "getFunctionWithObject")
  private static native MyJsFunctionWithObject getFunctionWithObjectFromJs();

  @JsMethod(namespace = "nativehelper", name = "callFunctionWithObject")
  private static native Object callFunctionWithObjectInJs(
      MyJsFunctionWithObject function, Object a);

  @JsMethod(namespace = "nativehelper", name = "getFunctionWithJsType")
  private static native MyJsFunctionWithJsType getFunctionWithJsTypeFromJs();

  @JsMethod(namespace = "nativehelper", name = "callFunctionWithJsType")
  private static native SomeJsType callFunctionWithJsTypeInJs(
      MyJsFunctionWithJsType function, SomeJsType a);

  private static void testEntryPoint() {
    assertTrue(callEntryPointAdd(5, 10) == 15);
    assertTrue(callEntryPointWithJsType() == 11);
    assertTrue(callJsMethodEntryPointWithJsType() == 11);
    assertTrue(callEntryPointWithNullJsFunction());
    assertTrue(callEntryPointWithUndefinedJsFunction());
    assertEquals(
        3333333333333333333L, callEntryPointAddLong(1111111111111111111L, 2222222222222222222L));
    assertTrue(testDirectEntryPointAddLongFromJs());
  }

  public static int entryPointAdd(int a, int b) {
    return a + b;
  }

  public static long entryPointAddLong(long a, long b) {
    return a + b;
  }

  public static int entryPointWithJsType(SomeJsType o) {
    return o.getNumber();
  }

  public static boolean entryPointWithJsFunction(MyJsFunctionWithJsType fn) {
    return fn == null;
  }

  @JsMethod(namespace = "nativehelper")
  static native BaseJsType newBaseJsType();

  @JsMethod(namespace = "nativehelper")
  static native SomeJsType newSomeJsType(int value);

  @JsMethod(namespace = "nativehelper")
  static native int callGetNumber(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native int callGetNumberViaStaticMethod(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native String callGetString(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native Long callGetLong(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native long callGetPrimitiveLong(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native long callAddPrimitiveLong(SomeJsType someJsType, long a, long b);

  @JsMethod(namespace = "nativehelper")
  static native NativeJsType callGetNativeJsType(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native int getField(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native void setField(SomeJsType someJsType, int value);

  @JsMethod(namespace = "nativehelper")
  static native long getLongField(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native void setLongField(SomeJsType someJsType, long value);

  @JsMethod(namespace = "nativehelper")
  static native int getStaticField();

  @JsMethod(namespace = "nativehelper")
  static native void setStaticField(int value);

  @JsMethod(namespace = "nativehelper")
  static native int getReadOnlyField(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native int getStaticReadOnlyField();

  @JsMethod(namespace = "nativehelper")
  static native int getStaticFieldSameJsNameAsInstanceField();

  @JsMethod(namespace = "nativehelper")
  static native int getReadOnlyProperty(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native int getStaticReadOnlyProperty();

  @JsMethod(namespace = "nativehelper")
  static native int getStaticPropertySameJsNameAsInstanceProperty();

  @JsMethod(namespace = "nativehelper")
  static native int getReadWriteProperty(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native void setReadWriteProperty(SomeJsType someJsType, int value);

  @JsMethod(namespace = "nativehelper")
  static native String callPackagePrivateMethod(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native SomeJsType callReturnSelf(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native boolean callTakesSelf(SomeJsType someJsType, SomeJsType arg);

  @JsMethod(namespace = "nativehelper")
  static native <T> int callMethodWithTypeParameters(
      SomeJsType someJsType, T o, String s, Double i);

  @JsMethod(namespace = "nativehelper")
  static native SomeJsType.CapturesOuter newCapturesOuter(SomeJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native SomeJsType callGetOuter(SomeJsType.CapturesOuter capturesOuter);

  @JsMethod(namespace = "nativehelper")
  static native int callAbstractMethod(AbstractJsType someJsType);

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceMethod(JsInterface jsInterface);

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceGetNumber(JsInterfaceGetNumber jsInterface);

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceRenamedMethod(JsInterfaceRenamedMethod jsInterface);

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceDefaultMethod(JsInterfaceDefaultMethod jsInterface);

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceStaticMethod();

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceStaticProperty();

  @JsMethod(namespace = "nativehelper")
  static native void setInterfaceStaticProperty(int value);

  @JsMethod(namespace = "nativehelper")
  static native int callInterfaceStaticField();

  @JsMethod(namespace = "nativehelper")
  static native int callAccidentalMethod(JsInterfaceAccidentalImpl impl);

  @JsMethod(namespace = "nativehelper")
  static native int callAccidentalDefaultMethod(JsInterfaceAccidentalDefaultMethodImpl impl);

  @JsMethod(namespace = "nativehelper")
  static native SomeJsType createJsSubtype();

  @JsMethod(namespace = "nativehelper")
  static native int callEntryPointAdd(int a, int b);

  @JsMethod(namespace = "nativehelper")
  static native int callEntryPointWithJsType();

  @JsMethod(namespace = "nativehelper")
  static native int callJsMethodEntryPointWithJsType();

  @JsMethod(namespace = "nativehelper")
  static native boolean callEntryPointWithNullJsFunction();

  @JsMethod(namespace = "nativehelper")
  static native boolean callEntryPointWithUndefinedJsFunction();

  @JsMethod(namespace = "nativehelper")
  static native long callEntryPointAddLong(long a, long b);

  @JsMethod(namespace = "nativehelper")
  static native boolean testDirectEntryPointAddLongFromJs();

  @JsType(isNative = true, namespace = "nativehelper")
  static class MyNativeType {
    public int value;

    public MyNativeType(int value) {}

    public native int getValue();

    public native int add(int delta);
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  static class NativeJsTypeConsumer {
    @JsConstructor
    public NativeJsTypeConsumer() {}

    public int callGetValue(MyNativeType nativeType) {
      return nativeType.getValue();
    }

    public int callAdd(MyNativeType nativeType, int delta) {
      return nativeType.add(delta);
    }

    public int callGetField(MyNativeType nativeType) {
      return nativeType.value;
    }

    public long callCombineWithLong(MyNativeType nativeType, Long boxedLong) {
      return nativeType.getValue() + (boxedLong != null ? boxedLong : 0L);
    }
  }

  @JsMethod(namespace = "nativehelper")
  static native long callMethodWithNativeAndLong(
      SomeJsType someJsType, MyNativeType nativeType, Long boxedLong);

  @JsMethod(namespace = "nativehelper")
  static native NativeJsTypeConsumer newNativeJsTypeConsumer();

  @JsMethod(namespace = "nativehelper")
  static native int callConsumerGetValue(NativeJsTypeConsumer consumer, MyNativeType nativeType);

  @JsMethod(namespace = "nativehelper")
  static native int callConsumerAdd(
      NativeJsTypeConsumer consumer, MyNativeType nativeType, int delta);

  @JsMethod(namespace = "nativehelper")
  static native int callConsumerGetField(NativeJsTypeConsumer consumer, MyNativeType nativeType);

  @JsMethod(namespace = "nativehelper")
  static native long callConsumerCombineWithLong(
      NativeJsTypeConsumer consumer, MyNativeType nativeType, Long boxedLong);

  @JsType(isNative = true, namespace = "native")
  static class NativeJsType {
    public NativeJsType(int value) {}

    public native int getNumber();
  }

  @JsEnum(namespace = "wasmcustomdescriptorsjsinterop")
  public enum SimpleJsEnum {
    ONE,
    TWO,
    THREE;

    public int getNumber() {
      return 100 + ordinal();
    }
  }

  @JsEnum(namespace = "wasmcustomdescriptorsjsinterop", hasCustomValue = true)
  public enum IntValuedJsEnum {
    MINUS_TEN(-10),
    TEN(10);

    int value;

    IntValuedJsEnum(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  @JsEnum(namespace = "wasmcustomdescriptorsjsinterop", hasCustomValue = true)
  public enum StringValuedJsEnum {
    FOO("foo"),
    BAR("bar");

    String value;

    StringValuedJsEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private static void testJsEnum() {
    // Normal enum behaviors
    assertTrue(SimpleJsEnum.ONE.ordinal() == 0);
    assertTrue(SimpleJsEnum.TWO.ordinal() == 1);
    assertTrue(SimpleJsEnum.THREE.ordinal() == 2);

    assertTrue(SimpleJsEnum.ONE.compareTo(SimpleJsEnum.TWO) < 0);
    assertTrue(SimpleJsEnum.TWO.compareTo(SimpleJsEnum.ONE) > 0);
    assertTrue(SimpleJsEnum.ONE.compareTo(SimpleJsEnum.ONE) == 0);

    assertTrue(SimpleJsEnum.ONE instanceof Enum);
    assertTrue(SimpleJsEnum.ONE instanceof SimpleJsEnum);

    assertTrue(SimpleJsEnum.ONE.getNumber() == 100);
    assertTrue(SimpleJsEnum.TWO.getNumber() == 101);

    assertTrue(getSimpleJsEnumOne() == SimpleJsEnum.ONE);
    assertTrue(getSimpleJsEnumTwo() == SimpleJsEnum.TWO);
    assertTrue(getSimpleJsEnumThree() == SimpleJsEnum.THREE);

    assertTrue(checkJsEnumEquality());
    assertTrue(passThroughJsEnum(SimpleJsEnum.ONE) == SimpleJsEnum.ONE);
    assertTrue(passThroughJsEnum(SimpleJsEnum.TWO) == SimpleJsEnum.TWO);

    assertTrue(getStringValuedJsEnumFoo() == StringValuedJsEnum.FOO);
    assertTrue(getStringValuedJsEnumBar() == StringValuedJsEnum.BAR);

    assertTrue(getIntValuedJsEnumMinusTen() == IntValuedJsEnum.MINUS_TEN);
    assertTrue(getIntValuedJsEnumTen() == IntValuedJsEnum.TEN);

    assertEquals("1-from-js", switchOnSimpleJsEnum(SimpleJsEnum.ONE));
    assertEquals("2-from-js", switchOnSimpleJsEnum(SimpleJsEnum.TWO));
    assertEquals("3-from-js", switchOnSimpleJsEnum(SimpleJsEnum.THREE));
    assertEquals("unknown", switchOnSimpleJsEnum(null));

    assertEquals("-10-from-js", switchOnIntValuedJsEnum(IntValuedJsEnum.MINUS_TEN));
    assertEquals("10-from-js", switchOnIntValuedJsEnum(IntValuedJsEnum.TEN));
    assertEquals("unknown", switchOnIntValuedJsEnum(null));

    assertEquals("foo-from-js", switchOnStringValuedJsEnum(StringValuedJsEnum.FOO));
    assertEquals("bar-from-js", switchOnStringValuedJsEnum(StringValuedJsEnum.BAR));
    assertEquals("unknown", switchOnStringValuedJsEnum(null));
  }

  @JsMethod(namespace = "nativehelper")
  static native SimpleJsEnum getSimpleJsEnumOne();

  @JsMethod(namespace = "nativehelper")
  static native SimpleJsEnum getSimpleJsEnumTwo();

  @JsMethod(namespace = "nativehelper")
  static native SimpleJsEnum getSimpleJsEnumThree();

  @JsMethod(namespace = "nativehelper")
  static native boolean checkJsEnumEquality();

  @JsMethod(namespace = "nativehelper")
  static native SimpleJsEnum passThroughJsEnum(SimpleJsEnum e);

  @JsMethod(namespace = "nativehelper")
  static native StringValuedJsEnum getStringValuedJsEnumFoo();

  @JsMethod(namespace = "nativehelper")
  static native StringValuedJsEnum getStringValuedJsEnumBar();

  @JsMethod(namespace = "nativehelper")
  static native IntValuedJsEnum getIntValuedJsEnumMinusTen();

  @JsMethod(namespace = "nativehelper")
  static native IntValuedJsEnum getIntValuedJsEnumTen();

  @JsMethod(namespace = "nativehelper")
  static native String switchOnSimpleJsEnum(SimpleJsEnum e);

  @JsMethod(namespace = "nativehelper")
  static native String switchOnIntValuedJsEnum(IntValuedJsEnum e);

  @JsMethod(namespace = "nativehelper")
  static native String switchOnStringValuedJsEnum(StringValuedJsEnum e);
}
