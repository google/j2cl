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
package jsfunction;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;
import static com.google.j2cl.integration.testing.Asserts.assertSame;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsArrayStoreException;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testJsFunction();
    testSpecializedJsFunction();
    testParameterizedJsFunctionMethod();
    testCast_crossCastJavaInstance();
    testCast_fromJsFunction();
    testCast_fromJsObject();
    testCast_inJava();
    testInstanceField();
    testInstanceOf_javaInstance();
    testInstanceOf_javaInstance_jsFunction();
    testInstanceOf_javaInstance_nativeType();
    testInstanceOf_jsFunction();
    testInstanceOf_jsObject();
    testJsFunctionAccess();
    testJsFunctionBasic_java();
    testJsFunctionBasic_javaAndJs();
    testJsFunctionBasic_js();
    testJsFunctionCallbackPattern();
    testJsFunctionCallFromAMember();
    testJsFunctionIdentity_java();
    testJsFunctionIdentity_js();
    // TODO(b/63941038): enable this test
    // testJsFunctionIdentity_ctor();
    testJsFunctionJs2Java();
    testJsFunctionProperty();
    testJsFunctionReferentialIntegrity();
    testJsFunctionSuccessiveCalls();
    testJsFunctionViaFunctionMethods();
    testGetClass();
    testJsFunctionOptimization();
    testJsFunctionWithVarArgs();
    testJsFunctionLambda();
    testJsFunctionArray();
    testJsFunctionWithNativeType();
    testJsFunctionWithJsType();
  }

  @JsFunction
  interface Function {
    boolean invoke();

    @JsOverlay int f = 1;

    @JsOverlay
    default int fun() {
      return f + (invoke() ? 1 : 2);
    }
  }

  @JsFunction
  interface FunctionWithStaticOverlay {
    boolean invoke();

    @JsOverlay
    static int fun() {
      return 4;
    }
  }

  @JsFunction
  interface FunctionWithStaticField {
    boolean invoke();

    @JsOverlay int f = 1;
  }

  private static void testJsFunction() {
    assertTrue(((Function) (() -> true)).fun() == 2);
    assertTrue(((Function) (() -> false)).fun() == 3);
    assertTrue(FunctionWithStaticOverlay.fun() == 4);
    assertTrue(FunctionWithStaticField.f == 1);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void testSpecializedJsFunction() {
    Consumer<String> stringConsumer =
        s -> {
          Object unused = s.substring(2);
        };
    Consumer rawConsumer = stringConsumer;

    assertThrowsClassCastException(() -> rawConsumer.accept(new Object()), String.class);
  }

  @JsFunction
  interface Consumer<T> {
    void accept(T t);
  }

  @JsFunction
  interface ParameterizedInterface<T> {
    T f(T t);
  }

  static <T> T identity(T t) {
    return t;
  }

  static <T> T nullFn(T t) {
    return null;
  }

  private static void testParameterizedJsFunctionMethod() {
    class A {
      String m() {
        return "HelloA";
      }
    }

    class B extends A {
      String m() {
        return "HelloB";
      }
    }

    ParameterizedInterface<B> parameterInterfaceFn;
    // Use a random to make the compiler not know which method is actually passed so that
    // whatever optimization is made on the call to B::m is based solely on types.
    if (Math.random() < -1) {
      parameterInterfaceFn = Main::nullFn;
    } else {
      parameterInterfaceFn = Main::identity;
    }
    new A();
    assertEquals("HelloB", parameterInterfaceFn.f(new B()).m());
  }

  @JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
  public static class NativeRegExp {
    public NativeRegExp(String regEx) {}

    // TODO(b/528427081): Wasm does not yet support Java array conversions on the JS interop
    // boundary.
    @Wasm("nop")
    public native String[] exec(String s);

    public native boolean test(String s);
  }

  @JsFunction
  public interface MyJsFunctionInterface {
    int foo(int a);
  }

  @JsFunction
  public interface MyJsFunctionIdentityInterface {
    Object identity();
  }

  /** A JsFunction interface. */
  @JsFunction
  public interface MyOtherJsFunctionInterface {
    int bar(int a);
  }

  /** A functional interface annotated by JsFunction that is only referenced by instanceof. */
  @JsFunction
  public interface MyJsFunctionWithOnlyInstanceofReference {
    int foo(int a);
  }

  /** A concrete class that implements a JsFunction interface. */
  public static final class MyJsFunctionInterfaceImpl implements MyJsFunctionInterface {

    public int publicField = 10;

    public int callFoo(int a) {
      // to prevent optimizations from inlining function foo.
      return 5 + foo(Math.random() > -1 ? a : -a);
    }

    @Override
    public int foo(int a) {
      return a + 1;
    }
  }

  @JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
  static class HTMLElementConcreteNativeJsType {}

  /**
   * A class that has a field of JsFunction type, and a method that accepts JsFunction parameter.
   */
  public static class MyClassAcceptsJsFunctionAsCallBack {

    private MyJsFunctionInterface callBack;

    public void setCallBack(MyJsFunctionInterface callBack) {
      this.callBack = callBack;
    }

    public int triggerCallBack(int a) {
      return callBack.foo(a);
    }
  }

  // separate java call and js calls into two tests to see if it works correctly.
  private static void testJsFunctionBasic_js() {
    MyJsFunctionInterface jsFunctionInterface =
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a + 2;
          }
        };
    assertEquals(12, callAsFunction(jsFunctionInterface, 10));
  }

  private static void testJsFunctionBasic_java() {
    MyJsFunctionInterface jsFunctionInterface =
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a + 2;
          }
        };
    assertEquals(12, jsFunctionInterface.foo(10));
  }

  private static void testJsFunctionBasic_javaAndJs() {
    MyJsFunctionInterface jsFunctionInterface =
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a + 2;
          }
        };
    assertEquals(12, jsFunctionInterface.foo(10));
    assertEquals(13, callAsFunction(jsFunctionInterface, 11));
  }

  private static void testJsFunctionViaFunctionMethods() {
    MyJsFunctionInterface jsFunctionInterface =
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a + 2;
          }
        };
    assertEquals(12, callWithFunctionApply(jsFunctionInterface, 10));
    assertEquals(12, callWithFunctionCall(jsFunctionInterface, 10));
  }

  private static void testJsFunctionIdentity_js() {
    MyJsFunctionIdentityInterface id =
        new MyJsFunctionIdentityInterface() {
          @Override
          public Object identity() {
            return this;
          }
        };
    assertEquals(id, callAsFunctionNoArgument(id));
  }

  private static void testJsFunctionIdentity_java() {
    MyJsFunctionIdentityInterface id =
        new MyJsFunctionIdentityInterface() {
          @Override
          public Object identity() {
            return this;
          }
        };
    assertTrue((id == id.identity()));
  }

  private static final class MyJsFunctionIdentityInConstructor
      implements MyJsFunctionIdentityInterface {

    public MyJsFunctionIdentityInterface storedThis;

    public MyJsFunctionIdentityInConstructor() {
      storedThis = (MyJsFunctionIdentityInterface) this;
    }

    @Override
    public Object identity() {
      return this;
    }
  }

  private static void testJsFunctionIdentity_ctor() {
    MyJsFunctionIdentityInConstructor id = new MyJsFunctionIdentityInConstructor();
    assertTrue((id.storedThis == id.identity()));
  }

  private static void testJsFunctionAccess() {
    MyJsFunctionInterface intf =
        new MyJsFunctionInterface() {
          public int publicField;

          @Override
          public int foo(int a) {
            return a;
          }
        };
    assertJsTypeDoesntHaveFields(intf, "foo");
    assertJsTypeDoesntHaveFields(intf, "publicField");
  }

  private static void testJsFunctionCallFromAMember() {
    MyJsFunctionInterfaceImpl impl = new MyJsFunctionInterfaceImpl();
    assertEquals(16, impl.callFoo(10));
  }

  private static void testJsFunctionJs2Java() {
    MyJsFunctionInterface intf = createMyJsFunction();
    assertEquals(10, intf.foo(10));
  }

  private static void testJsFunctionSuccessiveCalls() {
    assertEquals(
        12,
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a + 2;
          }
        }.foo(10));
    assertEquals(10, createMyJsFunction().foo(10));
  }

  private static void testJsFunctionCallbackPattern() {
    MyClassAcceptsJsFunctionAsCallBack c = new MyClassAcceptsJsFunctionAsCallBack();
    c.setCallBack(createMyJsFunction());
    assertEquals(10, c.triggerCallBack(10));
  }

  private static void testJsFunctionReferentialIntegrity() {
    MyJsFunctionIdentityInterface intf = createReferentialFunction();
    assertEquals(intf, intf.identity());
  }

  private static void testCast_fromJsFunction() {
    MyJsFunctionInterface c1 = (MyJsFunctionInterface) createFunction();
    assertNotNull(c1);
    MyJsFunctionIdentityInterface c2 = (MyJsFunctionIdentityInterface) createFunction();
    assertNotNull(c2);
    ElementLikeNativeInterface i = (ElementLikeNativeInterface) createFunction();
    assertNotNull(i);
    assertThrowsClassCastException(
        () -> {
          MyJsFunctionInterfaceImpl unused = (MyJsFunctionInterfaceImpl) createFunction();
        });
  }

  private static void testCast_fromJsObject() {
    ElementLikeNativeInterface obj = (ElementLikeNativeInterface) createObject();
    assertNotNull(obj);
    assertThrowsClassCastException(
        () -> {
          MyJsFunctionInterface unused = (MyJsFunctionInterface) createObject();
        });
    assertThrowsClassCastException(
        () -> {
          MyJsFunctionInterfaceImpl unused = (MyJsFunctionInterfaceImpl) createObject();
        });
    assertThrowsClassCastException(
        () -> {
          MyJsFunctionIdentityInterface unused = (MyJsFunctionIdentityInterface) createObject();
        });
  }

  private static void testCast_inJava() {
    Object object = new MyJsFunctionInterfaceImpl();
    MyJsFunctionInterface c1 = (MyJsFunctionInterface) object;
    assertNotNull(c1);
    MyJsFunctionInterfaceImpl c2 = (MyJsFunctionInterfaceImpl) c1;
    assertEquals(10, c2.publicField);
    MyJsFunctionInterfaceImpl c3 = (MyJsFunctionInterfaceImpl) object;
    assertNotNull(c3);
    MyJsFunctionIdentityInterface c4 = (MyJsFunctionIdentityInterface) object;
    assertNotNull(c4);
    ElementLikeNativeInterface c5 = (ElementLikeNativeInterface) object;
    assertNotNull(c5);
    assertThrowsClassCastException(
        () -> {
          HTMLElementConcreteNativeJsType unused = (HTMLElementConcreteNativeJsType) object;
        });
  }

  private static void testCast_crossCastJavaInstance() {
    Object o = new MyJsFunctionInterfaceImpl();
    assertEquals(11, ((MyOtherJsFunctionInterface) o).bar(10));
    assertSame((MyJsFunctionInterface) o, (MyOtherJsFunctionInterface) o);
  }

  // TODO(b/548108681): Wasm does not support instanceof against JsFunction interfaces yet.
  @Wasm("nop")
  private static void testInstanceOf_jsFunction() {
    Object object = createFunction();
    assertTrue(object instanceof MyJsFunctionInterface);
    assertTrue(object instanceof MyJsFunctionIdentityInterface);
    assertTrue(object instanceof MyJsFunctionWithOnlyInstanceofReference);
  }

  private static void testInstanceOf_jsObject() {
    Object object = createObject();
    assertFalse(object instanceof MyJsFunctionInterface);
    assertFalse(object instanceof MyJsFunctionIdentityInterface);
    assertFalse(object instanceof MyJsFunctionWithOnlyInstanceofReference);
  }

  private static void testInstanceOf_javaInstance() {
    Object object = new MyJsFunctionInterfaceImpl();
    assertTrue(object instanceof MyJsFunctionInterface);
    Object nullObject = null;
    assertFalse(nullObject instanceof MyJsFunctionInterface);
  }

  // TODO(b/548108681): Wasm does not support instanceof against JsFunction interfaces yet.
  @Wasm("nop")
  private static void testInstanceOf_javaInstance_jsFunction() {
    Object object = new MyJsFunctionInterfaceImpl();
    assertTrue(object instanceof MyJsFunctionIdentityInterface);
    assertTrue(object instanceof MyJsFunctionWithOnlyInstanceofReference);
  }

  // TODO(b/540455028): Cannot do instanceof against native JsType in Wasm.
  @Wasm("nop")
  private static void testInstanceOf_javaInstance_nativeType() {
    Object object = new MyJsFunctionInterfaceImpl();
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
  }

  // TODO(b/548108681): getClass on JsFunction instances currently calls JsFunctionAdapter.getClass
  @Wasm("nop")
  private static void testGetClass() {
    MyJsFunctionInterface jsfunctionImplementation =
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a;
          }
        };
    assertEquals(MyJsFunctionInterface.class, jsfunctionImplementation.getClass());
    assertEquals(MyJsFunctionInterface.class, ((Object) jsfunctionImplementation).getClass());
    assertEquals(MyJsFunctionInterface.class, createMyJsFunction().getClass());
    assertEquals(MyJsFunctionInterface.class, ((Object) createMyJsFunction()).getClass());
  }

  // TODO(b/548108681): getClass on JsFunction instances currently calls JsFunctionAdapter.getClass
  // TODO(b/528427081): Wasm does not yet support Java array conversions on the JS interop boundary
  // (NativeRegExp uses arrays).
  @Wasm("nop")
  private static void testJsFunctionOptimization() {
    MyJsFunctionInterface lambda = a -> a;

    // inner class optimizable to lambda
    MyJsFunctionInterface optimizableInner =
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a;
          }
        };
    assertEquals(MyJsFunctionInterface.class, optimizableInner.getClass());

    // Look at the structure of the two functions to make sure they are plain functions. They should
    // look something like
    //
    //     "function <fn>( /** type */ <par>) { return <par>; }"
    //
    NativeRegExp functionRegExp =
        new NativeRegExp(
            "function [\\w$]*\\(\\s*(?:\\/\\*.*\\*\\/)?\\s*([\\w$]+)\\)\\s*{\\s*return \\1;\\s*}");
    //
    //  or "(/** type */ <par>)=>{ return <par>;}"
    //
    NativeRegExp arrowRegExp =
        new NativeRegExp(
            "\\(\\s*(?:\\/\\*.*\\*\\/)?\\s*([\\w$]+)\\)\\s*=>\\s*{\\s*return \\1;\\s*}");
    //
    //  or "<par>=><par>"
    //
    NativeRegExp es6ArrowRegExp =
        new NativeRegExp("\\s*(?:\\/\\*.*\\*\\/)?\\s*([\\w$]+)\\s*=>\\s*\\1\\s*");

    assertTrue(
        functionRegExp.exec(optimizableInner.toString()) != null
            || arrowRegExp.exec(optimizableInner.toString()) != null
            || es6ArrowRegExp.exec(optimizableInner.toString()) != null);
    assertTrue(
        functionRegExp.exec(lambda.toString()) != null
            || arrowRegExp.exec(lambda.toString()) != null
            || es6ArrowRegExp.exec(lambda.toString()) != null);

    // inner class not optimizable to lambda
    MyJsFunctionInterface unoptimizableInner =
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return id(a);
          }

          private int id(int a) {
            return a;
          }
        };
    assertEquals(MyJsFunctionInterface.class, unoptimizableInner.getClass());
  }

  private static void testInstanceField() {
    MyJsFunctionInterface jsfunctionImplementation =
        new MyJsFunctionInterface() {
          String hello = new Object().getClass().getName();

          @Override
          public int foo(int a) {
            return hello.length() + a;
          }
        };
    assertEquals(Object.class.getName().length() + 4, jsfunctionImplementation.foo(4));
  }

  @JsFunction
  interface JsFunctionInterface {
    Object m();
  }

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  private static native JsFunctionInterface createFunctionThatReturnsThis();

  private static void testJsFunctionProperty() {
    class JsFuncionProperty {
      @JsProperty public JsFunctionInterface func = createFunctionThatReturnsThis();

      @JsProperty
      public JsFunctionInterface getF() {
        return createFunctionThatReturnsThis();
      }
    }

    JsFunctionInterface[] array = new JsFunctionInterface[] {createFunctionThatReturnsThis()};
    JsFuncionProperty instance = new JsFuncionProperty();
    JsFunctionInterface funcInVar;

    // Field
    assertTrue(instance != instance.func.m());
    // Assert that "this" is bound to the same object regardless of whether the calls is made
    // directly or from variable.
    funcInVar = instance.func;
    assertSame(funcInVar.m(), instance.func.m());

    // Getter
    assertTrue(instance != instance.getF().m());
    // Assert that "this" is bound to the same object regardless of whether the calls is made
    // directly or from variable.
    funcInVar = instance.getF();
    assertSame(funcInVar.m(), instance.getF().m());

    // Array Access
    assertTrue(array != array[0].m());
    // Assert that "this" is bound to the same object regardless of whether the calls is made
    // directly or from variable.
    funcInVar = array[0];
    assertSame(funcInVar.m(), array[0].m());

    // Parenthesized
    assertTrue(instance != (instance.func).m());
    // Assert that "this" is bound to the same object regardless of whether the calls is made
    // directly or from variable.
    funcInVar = instance.func;
    assertSame(funcInVar.m(), (instance.func).m());

    // Conditional expression
    // Currently there is no way to write it in Java without parenthesis but the parenthesis might
    // be dropped in the future.
    assertTrue(instance != ((instance != null) ? instance.func : instance.func).m());
    // Assert that "this" is bound to the same object regardless of whether the calls is made
    // directly or from variable.
    funcInVar = (instance != null) ? instance.func : instance.func;
    assertSame(funcInVar.m(), ((instance != null) ? instance.func : instance.func).m());
  }

  @JsFunction
  interface JsFunctionWithVarargs {
    int f(int n, int... numbers);
  }

  static final class JsFunctionWithVarargsOptimizable implements JsFunctionWithVarargs {
    @Override
    public int f(int n, int... numbers) {
      return numbers[n];
    }
  }

  static final class JsFunctionWithVarargsNonOptimizable implements JsFunctionWithVarargs {
    @Override
    public int f(int n, int... numbers) {
      return accum = numbers[n];
    }

    int accum = 0;
  }

  static class JsFunctionWithVarargsTestSuper {
    int m() {
      return 5;
    }
  }

  static class JsFunctionWithVarargsTestSub extends JsFunctionWithVarargsTestSuper {
    int instanceField = 5;

    @Override
    int m() {
      return 3;
    }

    void test() {
      // Access through super
      assertEquals(8, ((JsFunctionWithVarargs) (n, numbers) -> numbers[n] + super.m()).f(1, 1, 3));
      // Access through this (instanceField)
      assertEquals(
          8, ((JsFunctionWithVarargs) (n, numbers) -> numbers[n] + instanceField).f(1, 1, 3));
    }
  }

  private static void testJsFunctionWithVarArgs() {
    assertEquals(3, ((JsFunctionWithVarargs) new JsFunctionWithVarargsOptimizable()).f(1, 1, 3));
    assertEquals(3, ((JsFunctionWithVarargs) new JsFunctionWithVarargsNonOptimizable()).f(1, 1, 3));
    assertEquals(3, ((JsFunctionWithVarargs) (n, numbers) -> numbers[n]).f(1, 1, 3));
    assertEquals(3, ((JsFunctionWithVarargs) (int n, int... numbers) -> numbers[n]).f(1, 1, 3));
    assertEquals(3, ((JsFunctionWithVarargs) (int n, int[] numbers) -> numbers[n]).f(1, 1, 3));

    new JsFunctionWithVarargsTestSub().test();
  }

  @JsFunction
  interface JsFunctionWithNativeType {
    NativeRegExp f(NativeRegExp regExp);
  }

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native NativeRegExp callAsFunctionWithNativeType(
      JsFunctionWithNativeType fn, NativeRegExp arg);

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper", name = "createFunction")
  public static native JsFunctionWithNativeType createJsFunctionWithNativeType();

  private static void testJsFunctionWithNativeType() {
    NativeRegExp regExp = new NativeRegExp("a");
    assertTrue(
        ((JsFunctionWithNativeType)
                    (a -> {
                      // Make a simple call to ensure the argument isn't just blindly being passed
                      // and that proper conversions are taking place.
                      a.test("a");
                      return a;
                    }))
                .f(regExp)
            == regExp);

    JsFunctionWithNativeType fn = a -> a;
    assertTrue(callAsFunctionWithNativeType(fn, regExp) == regExp);

    JsFunctionWithNativeType fnFromJs = createJsFunctionWithNativeType();
    assertTrue(fnFromJs.f(regExp) == regExp);
  }

  @JsType
  public static class SomeJsType {
    // TODO(b/549970784): Currently not considered an exported type in Wasm without this. Revisit if
    // this type ends up being exported.
    @JsConstructor
    public SomeJsType() {}
  }

  @JsFunction
  interface JsFunctionWithJsType {
    SomeJsType f(SomeJsType jsType);
  }

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native SomeJsType callAsFunctionWithJsType(JsFunctionWithJsType fn, SomeJsType arg);

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native JsFunctionWithJsType createJsFunctionWithJsType();

  private static void testJsFunctionWithJsType() {
    SomeJsType jsType = new SomeJsType();
    assertTrue(((JsFunctionWithJsType) (a -> a)).f(jsType) == jsType);

    JsFunctionWithJsType fn = a -> a;
    assertTrue(callAsFunctionWithJsType(fn, jsType) == jsType);

    JsFunctionWithJsType fnFromJs = createJsFunctionWithJsType();
    assertTrue(fnFromJs.f(jsType) == jsType);
  }

  private static void testJsFunctionLambda() {
    MyJsFunctionInterface jsFunctionInterface = a -> a + 2;
    assertEquals(12, callAsFunction(jsFunctionInterface, 10));
    assertEquals(12, jsFunctionInterface.foo(10));
  }

  private static void testJsFunctionArray() {
    MyJsFunctionInterface[] functionArray = new MyJsFunctionInterface[1];
    functionArray[0] = a -> a + 2;

    assertThrowsArrayStoreException(
        () -> {
          Object[] temp = functionArray;
          // Storing anything other than a function throws.
          temp[0] = new Integer(1);
        });

    MyJsFunctionInterface[][] function2dArray = new MyJsFunctionInterface[1][];
    function2dArray[0] = functionArray;

    assertThrowsArrayStoreException(
        () -> {
          Object[][] temp = function2dArray;
          // Trying to store an integer array as a JsFunction array throws.
          temp[0] = new Integer[1];
        });

    assertThrowsClassCastException(
        () -> {
          // Casting an integer array to a JsFunction array throws.
          Object o = new Integer[1];
          Object temp = (JsFunctionInterface[]) o;
        });
  }

  private static void assertJsTypeDoesntHaveFields(Object obj, String... fields) {
    for (String field : fields) {
      assertFalse("Field '" + field + "' should not be exported", hasField(obj, field));
    }
  }

  @JsType(isNative = true, namespace = "test.foo")
  interface ElementLikeNativeInterface {
    @JsProperty
    String getTagName();
  }

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native Object callAsFunctionNoArgument(Object fn);

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native int callAsFunction(Object fn, int arg);

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native int callWithFunctionApply(Object fn, int arg);

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native int callWithFunctionCall(Object fn, int arg);

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native MyJsFunctionInterface createMyJsFunction();

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native MyJsFunctionIdentityInterface createReferentialFunction();

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native Object createFunction();

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native Object createObject();

  @JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
  public static native boolean hasField(Object object, String fieldName);
}
