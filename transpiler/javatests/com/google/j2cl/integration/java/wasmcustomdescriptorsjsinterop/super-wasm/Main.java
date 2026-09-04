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
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
import jsinterop.annotations.JsOverlay;
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
    testJsFunction();
    testJsFunctionCapturing();
    testJsFunctionAsObject();
    testJsFunctionIdentity();
    testJsFunctionCrossCasting();
    testParameterizedJsFunction();
    testEntryPoint();
    testMethodWithNativeAndLong();
    testNativeJsTypeConsumer();
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

  private static void testJsFunction() {
    assertEquals(2, MyJsFunction.staticOverlay());

    MyJsFunction lambda = a -> a + 20;
    assertEquals(25, lambda.foo(5));
    assertEquals(1, lambda.myOverlay());
    // At the first border crossing the JavaScript function is created by binding the export bridge,
    assertEquals(25, callFunctionInJs(lambda, 5));
    // and then reused in subsequent calls.
    assertEquals(25, callFunctionAsObjectInJs(lambda, 5));

    MyJsFunction staticRef = Main::staticFooImpl;
    assertEquals(16, staticRef.foo(5));
    assertEquals(1, staticRef.myOverlay());
    assertEquals(16, callFunctionInJs(staticRef, 5));
    assertEquals(16, callFunctionAsObjectInJs(staticRef, 5));

    MyJsFunction instanceRef = new Main()::instanceFooImpl;
    assertEquals(17, instanceRef.foo(5));
    assertEquals(1, instanceRef.myOverlay());
    assertEquals(17, callFunctionInJs(instanceRef, 5));
    assertEquals(17, callFunctionAsObjectInJs(instanceRef, 5));

    MyJsFunctionWithObject objectLambda = o -> o;
    assertEquals(null, objectLambda.foo(null));
    Object obj = new Object();
    assertEquals(obj, objectLambda.foo(obj));
    assertTrue(callFunctionWithObjectInJs(objectLambda, obj) == obj);
    assertEquals("hello", callFunctionWithObjectInJs(objectLambda, "hello"));
    SomeJsType someJsType = new SomeJsType(123);
    assertEquals(someJsType, callFunctionWithObjectInJs(objectLambda, someJsType));
    assertTrue(callFunctionWithObjectInJs(objectLambda, someJsType) == someJsType);

    MyJsFunction jsFunction = getFunctionFromJs();
    assertEquals(42, jsFunction.foo(10));

    MyJsFunction nullJsFunction = getNullFunctionFromJs();
    assertEquals(null, nullJsFunction);

    MyJsFunction undefinedJsFunction = getUndefinedJsFunction();
    assertEquals(null, undefinedJsFunction);

    MyJsFunction jsFunctionAsObject = (MyJsFunction) getFunctionAsObjectFromJs();
    assertEquals(42, jsFunctionAsObject.foo(10));
    assertEquals(37, callFunctionInJs(jsFunctionAsObject, 5));

    MyJsFunctionWithObject jsFunctionWithObject = getFunctionWithObjectFromJs();
    Object obj2 = new Object();
    assertTrue(jsFunctionWithObject.foo(obj2) == obj2);
    assertEquals("hello", jsFunctionWithObject.foo("hello"));
    SomeJsType someJsType2 = new SomeJsType(123);
    assertTrue(jsFunctionWithObject.foo(someJsType2) == someJsType2);

    MyJsFunction concreteInstance = new MyJsFunctionImpl();
    assertEquals(105, concreteInstance.foo(5));
    assertEquals(105, callFunctionInJs(concreteInstance, 5));

    MyJsFunction anonymousInstance =
        new MyJsFunction() {
          @Override
          public int foo(int a) {
            return a + 200;
          }
        };
    assertEquals(205, anonymousInstance.foo(5));
    assertEquals(205, callFunctionInJs(anonymousInstance, 5));

    MyJsFunction constructorInstance = new MyJsFunctionWithConstructorImpl(1000);
    assertEquals(1005, constructorInstance.foo(5));
    assertEquals(1005, callFunctionInJs(constructorInstance, 5));

    MyJsFunction constructorInstanceOtherConstructor = new MyJsFunctionWithConstructorImpl();
    assertEquals(6, constructorInstanceOtherConstructor.foo(5));
    assertEquals(6, callFunctionInJs(constructorInstanceOtherConstructor, 5));

    MyJsFunction innerJsFunction =
        new MyJsFunctionWithConstructorImpl(1000).new InnerJsFunctionImpl();
    assertEquals(1006, innerJsFunction.foo(5));
    assertEquals(1006, callFunctionInJs(innerJsFunction, 5));

    MyJsFunction withSuperConstructorInstance = new MyJsFunctionWithSuperConstructorImpl();
    assertEquals(105, withSuperConstructorInstance.foo(5));
    assertEquals(105, callFunctionInJs(withSuperConstructorInstance, 5));

    MyLongFunction longLambda = l -> l + 1L;
    assertEquals(1234567890123456790L, callLongFunctionInJs(longLambda, 1234567890123456789L));
    assertTrue(testDirectJsFunctionLongFromJs(longLambda));
  }

  private static int classField = 10001;

  private static void testJsFunctionCapturing() {
    int local = 10002;
    MyJsFunction capturingLocal = a -> a + local;
    assertEquals(10007, capturingLocal.foo(5));
    assertEquals(10007, callFunctionInJs(capturingLocal, 5));

    MyJsFunction capturingField = a -> a + classField;
    assertEquals(10006, capturingField.foo(5));
    assertEquals(10006, callFunctionInJs(capturingField, 5));
  }

  /**
   * Tests that the proper export bridge is used when the JavaScript function is created when
   * crossing the border.
   */
  private static void testJsFunctionAsObject() {
    // Assigning to an Object variable does not trigger the instantiation of the JavaScript
    // function that needs the export bridge.
    Object jsFunction = (MyStringFunction) s -> s + "bar";
    // but calling it using a native function by passing it as `Object` does.
    assertEquals("foobar", callFunctionAsObjectInJs(jsFunction, "foo"));

    // Use a second JsFunction with incompatible type conversion needs to make sure that the right
    // bridge is used.
    MyJsFunction intJsFunction = a -> a + 20;
    jsFunction = intJsFunction;
    assertEquals(25, callFunctionAsObjectInJs(jsFunction, 5));
    // The second call uses the already created JavaScript function.
    assertEquals(25, callFunctionInJs(intJsFunction, 5));
  }

  private static void testJsFunctionIdentity() {
    MyJsFunction fromJs = getFunctionFromJs();
    MyJsFunction fromJs2 = passThroughFromJs(fromJs);
    assertTrue(fromJs == fromJs2);
    assertTrue(isSameInJs(fromJs, fromJs2));

    MyJsFunction fromJsAsObject = (MyJsFunction) getFunctionAsObjectFromJs();
    MyJsFunction fromJsAsObject2 = (MyJsFunction) passThroughAsObjectFromJs(fromJsAsObject);
    assertTrue(fromJsAsObject == fromJsAsObject2);
    assertTrue(isSameInJs(fromJsAsObject, fromJsAsObject2));

    MyJsFunction fromWasm = a -> a + 20;
    MyJsFunction fromWasm2 = passThroughFromJs(fromWasm);
    assertTrue(fromWasm == fromWasm2);
    assertTrue(isSameInJs(fromWasm, fromWasm2));
  }

  private static void testJsFunctionCrossCasting() {
    MyJsFunction fromJs = getFunctionFromJs();
    MyJsFunction2 crossCastedFromJs = crossCastFromJs(fromJs);
    assertEquals(42, crossCastedFromJs.bar(10));

    MyJsFunction fromWasm = a -> a + 20;
    MyJsFunction2 crossCastedFromWasm = crossCastFromJs(fromWasm);
    assertEquals(30, crossCastedFromWasm.bar(10));
  }

  static int staticFooImpl(int a) {
    return a + 11;
  }

  int instanceFooImpl(int a) {
    return a + 12;
  }

  static final class MyJsFunctionImpl implements MyJsFunction {
    @Override
    public int foo(int a) {
      return a + 100;
    }
  }

  static final class MyJsFunctionWithConstructorImpl implements MyJsFunction {
    private final int value;

    public MyJsFunctionWithConstructorImpl() {
      this(1);
    }

    public MyJsFunctionWithConstructorImpl(int value) {
      this.value = value;
    }

    @Override
    public int foo(int a) {
      return a + value;
    }

    public final class InnerJsFunctionImpl implements MyJsFunction {
      @Override
      public int foo(int a) {
        return a + MyJsFunctionWithConstructorImpl.this.value + 1;
      }
    }
  }

  static final class MyJsFunctionWithSuperConstructorImpl implements MyJsFunction {
    public MyJsFunctionWithSuperConstructorImpl() {
      super();
    }

    @Override
    public int foo(int a) {
      return a + 100;
    }
  }

  @JsFunction
  interface MyJsFunction {
    int foo(int a);

    @JsOverlay
    default int myOverlay() {
      return 1;
    }

    @JsOverlay
    static int staticOverlay() {
      return 2;
    }
  }

  @JsFunction
  interface MyStringFunction {
    String foo(String s);
  }

  @JsFunction
  interface MyJsFunctionWithObject {
    Object foo(Object o);
  }

  @JsFunction
  interface MyJsFunction2 {
    int bar(int a);
  }

  @JsFunction
  interface MyLongFunction {
    long foo(long l);
  }

  private static void testParameterizedJsFunction() {
    ApplyFunction<String> stringJsFunction = s -> s.toLowerCase();
    assertEquals("hello", stringJsFunction.apply("HELLO"));
    assertEquals("hello", callGeneric(stringJsFunction, "HELLO"));
    assertEquals("hello", callApplyFunctionInJs(stringJsFunction, "HELLO"));

    ApplyFunction<Integer> intJsFunction = i -> i + 1;
    assertEquals(2, intJsFunction.apply(1));
    assertEquals(2, callGeneric(intJsFunction, 1));
    assertEquals((Integer) 2, callApplyFunctionInJs(intJsFunction, 1));

    ApplyFunction<Object> objectJsFunction = (ApplyFunction) stringJsFunction;
    assertThrowsClassCastException(() -> objectJsFunction.apply(new Object()));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Object callGeneric(ApplyFunction af, Object o) {
    return af.apply(o);
  }

  @JsFunction
  interface ApplyFunction<T> {
    T apply(T a);
  }

  @JsMethod(namespace = "functions", name = "getFunction")
  private static native MyJsFunction getFunctionFromJs();

  @JsMethod(namespace = "functions", name = "getNullFunction")
  private static native MyJsFunction getNullFunctionFromJs();

  @JsMethod(namespace = "functions", name = "getUndefinedJsFunction")
  private static native MyJsFunction getUndefinedJsFunction();

  @JsMethod(namespace = "functions", name = "getFunctionAsObject")
  private static native Object getFunctionAsObjectFromJs();

  @JsMethod(namespace = "functions", name = "getFunctionWithObject")
  private static native MyJsFunctionWithObject getFunctionWithObjectFromJs();

  @JsMethod(namespace = "functions", name = "callFunction")
  private static native int callFunctionInJs(MyJsFunction function, int a);

  @JsMethod(namespace = "functions", name = "callFunctionAsObject")
  private static native int callFunctionAsObjectInJs(Object function, int a);

  @JsMethod(namespace = "functions", name = "callFunctionAsObject")
  private static native String callFunctionAsObjectInJs(Object function, String a);

  @JsMethod(namespace = "functions", name = "callFunctionWithObject")
  private static native Object callFunctionWithObjectInJs(
      MyJsFunctionWithObject function, Object a);

  @JsMethod(namespace = "functions", name = "passThrough")
  private static native MyJsFunction passThroughFromJs(MyJsFunction function);

  @JsMethod(namespace = "functions", name = "passThroughAsObject")
  private static native Object passThroughAsObjectFromJs(Object function);

  @JsMethod(namespace = "functions", name = "isSame")
  private static native boolean isSameInJs(MyJsFunction function1, MyJsFunction function2);

  @JsMethod(namespace = "functions", name = "passThrough")
  private static native MyJsFunction2 crossCastFromJs(MyJsFunction function);

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

  public static boolean entryPointWithJsFunction(MyJsFunction fn) {
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

  @JsMethod(namespace = "nativehelper")
  static native long callLongFunctionInJs(MyLongFunction fn, long l);

  @JsMethod(namespace = "nativehelper")
  static native boolean testDirectJsFunctionLongFromJs(MyLongFunction fn);

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

  @JsMethod(namespace = "functions", name = "callApplyFunction")
  private static native <T> T callApplyFunctionInJs(ApplyFunction<T> function, T a);
}
