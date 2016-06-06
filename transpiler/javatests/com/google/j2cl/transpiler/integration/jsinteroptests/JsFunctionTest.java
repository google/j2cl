package com.google.j2cl.transpiler.integration.jsinteroptests;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class JsFunctionTest extends MyTestCase {
  @JsFunction
  public static interface MyJsFunctionInterface {
    int foo(int a);
  }

  @JsFunction
  public static interface MyJsFunctionIdentityInterface {
    Object identity();
  }

  /**
   * A JsFunction interface.
   */
  @JsFunction
  public static interface MyOtherJsFunctionInterface {
    int bar(int a);
  }

  /**
   * A functional interface annotated by JsFunction that is only referenced by instanceof.
   */
  @JsFunction
  public interface MyJsFunctionWithOnlyInstanceofReference {
    int foo(int a);
  }

  /**
   * A concrete class that implements a JsFunction interface.
   */
  public static class MyJsFunctionInterfaceImpl implements MyJsFunctionInterface {

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
  public void testJsFunctionBasic_js() {
    MyJsFunctionInterface jsFunctionInterface =
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a + 2;
          }
        };
    assertEquals(12, callAsFunction(jsFunctionInterface, 10));
  }

  public void testJsFunctionBasic_java() {
    MyJsFunctionInterface jsFunctionInterface =
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a + 2;
          }
        };
    assertEquals(12, jsFunctionInterface.foo(10));
  }

  public void testJsFunctionBasic_javaAndJs() {
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

  public void testJsFunctionViaFunctionMethods() {
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

  public void testJsFunctionIdentity_js() {
    MyJsFunctionIdentityInterface id =
        new MyJsFunctionIdentityInterface() {
          @Override
          public Object identity() {
            return this;
          }
        };
    assert (id == callAsFunctionNoArgument(id));
  }

  public void testJsFunctionIdentity_java() {
    MyJsFunctionIdentityInterface id =
        new MyJsFunctionIdentityInterface() {
          @Override
          public Object identity() {
            return this;
          }
        };
    assert (id == id.identity());
  }

  public void testJsFunctionAccess() {
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

  public void testJsFunctionCallFromAMember() {
    MyJsFunctionInterfaceImpl impl = new MyJsFunctionInterfaceImpl();
    assertEquals(16, impl.callFoo(10));
  }

  public void testJsFunctionJs2Java() {
    MyJsFunctionInterface intf = createMyJsFunction();
    assertEquals(10, intf.foo(10));
  }

  public void testJsFunctionSuccessiveCalls() {
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

  public void testJsFunctionCallbackPattern() {
    MyClassAcceptsJsFunctionAsCallBack c = new MyClassAcceptsJsFunctionAsCallBack();
    c.setCallBack(createMyJsFunction());
    assertEquals(10, c.triggerCallBack(10));
  }

  public void testJsFunctionReferentialIntegrity() {
    MyJsFunctionIdentityInterface intf = createReferentialFunction();
    assert (intf == intf.identity());
  }

  public void testCast_fromJsFunction() {
    MyJsFunctionInterface c1 = (MyJsFunctionInterface) createFunction();
    assertNotNull(c1);
    MyJsFunctionIdentityInterface c2 = (MyJsFunctionIdentityInterface) createFunction();
    assertNotNull(c2);
    ElementLikeNativeInterface i = (ElementLikeNativeInterface) createFunction();
    assertNotNull(i);
    try {
      MyJsFunctionInterfaceImpl c3 = (MyJsFunctionInterfaceImpl) createFunction();
      assertNotNull(c3);
      fail("ClassCastException should be caught.");
    } catch (ClassCastException cce) {
      // Expected.
    }
  }

  public void testCast_fromJsObject() {
    ElementLikeNativeInterface obj = (ElementLikeNativeInterface) createObject();
    assertNotNull(obj);
    try {
      MyJsFunctionInterface c = (MyJsFunctionInterface) createObject();
      assertNotNull(c);
      fail("ClassCastException should be caught.");
    } catch (ClassCastException cce) {
      // Expected.
    }
    try {
      MyJsFunctionInterfaceImpl c = (MyJsFunctionInterfaceImpl) createObject();
      assertNotNull(c);
      fail("ClassCastException should be caught.");
    } catch (ClassCastException cce) {
      // Expected.
    }
    try {
      MyJsFunctionIdentityInterface c = (MyJsFunctionIdentityInterface) createObject();
      assertNotNull(c);
      fail("ClassCastException should be caught.");
    } catch (ClassCastException cce) {
      // Expected.
    }
  }

  public void testCast_inJava() {
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
    try {
      HTMLElementConcreteNativeJsType c6 = (HTMLElementConcreteNativeJsType) object;
      assertNotNull(c6);
      fail("ClassCastException should be caught.");
    } catch (ClassCastException cce) {
      // Expected.
    }
  }

  public void testCast_crossCastJavaInstance() {
    Object o = new MyJsFunctionInterfaceImpl();
    assertEquals(11, ((MyOtherJsFunctionInterface) o).bar(10));
    assertSame((MyJsFunctionInterface) o, (MyOtherJsFunctionInterface) o);
  }

  public void testInstanceOf_jsFunction() {
    Object object = createFunction();
    assertTrue(object instanceof MyJsFunctionInterface);
    assertTrue(object instanceof MyJsFunctionIdentityInterface);
    assertTrue(object instanceof MyJsFunctionWithOnlyInstanceofReference);
    assertFalse(object instanceof MyJsFunctionInterfaceImpl);
  }

  public void testInstanceOf_jsObject() {
    Object object = createObject();
    assertFalse(object instanceof MyJsFunctionInterface);
    assertFalse(object instanceof MyJsFunctionIdentityInterface);
    assertFalse(object instanceof MyJsFunctionWithOnlyInstanceofReference);
    assertFalse(object instanceof MyJsFunctionInterfaceImpl);
  }

  public void testInstanceOf_javaInstance() {
    Object object = new MyJsFunctionInterfaceImpl();
    assertTrue(object instanceof MyJsFunctionInterface);
    assertTrue(object instanceof MyJsFunctionInterfaceImpl);
    assertTrue(object instanceof MyJsFunctionIdentityInterface);
    assertTrue(object instanceof MyJsFunctionWithOnlyInstanceofReference);
    assertFalse(object instanceof HTMLElementConcreteNativeJsType);
  }

  public void testGetClass_jsFunction() {
    // inline lambda
    MyJsFunctionInterface lambda = a -> a;

    assertEquals(MyJsFunctionInterface.class, lambda.getClass());

    // inner class optimizable to lambda
    MyJsFunctionInterface optimizableInner =
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a;
          }
        };
    assertEquals(MyJsFunctionInterface.class, optimizableInner.getClass());
    // The toString comparison is a bit misleading, because these are native functions
    // toString() returns the JS source code which in this case, same function, same
    // variable names, same JS source.
    assertEquals(optimizableInner.toString(), lambda.toString());
    // inner class optimizable to lambda
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

  // uncomment when Java8 is supported.
  // public void testJsFunctionLambda_JS() {
  //   MyJsFunctionInterface jsFunctionInterface = a -> { return a + 2; };
  //   assertEquals(12, callAsFunction(jsFunctionInterface, 10));
  //   assertEquals(12, callAsCallBackFunction(jsFunctionInterface, 10));
  // }
  //
  // public void testJsFunctionLambda_Java() {
  //   MyJsFunctionInterface jsFunctionInterface = a -> { return a + 2; };
  //   assertEquals(12, jsFunctionInterface.foo(10));
  // }
  //
  // public void testJsFunctionDefaultMethod() {
  //   MyJsFunctionSubInterfaceWithDefaultMethod impl =
  //       new MyJsFunctionSubInterfaceWithDefaultMethod() {
  //       };
  //   assertEquals(10, impl.foo(10));
  //   assertEquals(10, callAsFunction(impl, 10));
  // }

  public void assertJsTypeHasFields(Object obj, String... fields) {
    for (String field : fields) {
      assertTrue("Field '" + field + "' should be exported", hasField(obj, field));
    }
  }

  public void assertJsTypeDoesntHaveFields(Object obj, String... fields) {
    for (String field : fields) {
      assertFalse("Field '" + field + "' should not be exported", hasField(obj, field));
    }
  }

  @JsMethod
  public static native Object callAsFunctionNoArgument(Object fn);

  @JsMethod
  public static native int callAsFunction(Object fn, int arg);

  @JsMethod
  public static native int callWithFunctionApply(Object fn, int arg);

  @JsMethod
  public static native int callWithFunctionCall(Object fn, int arg);

  @JsMethod
  public static native void setField(Object object, String fieldName, int value);

  @JsMethod
  public static native int getField(Object object, String fieldName);

  @JsMethod
  public static native int callIntFunction(Object object, String functionName);

  @JsMethod
  public static native MyJsFunctionInterface createMyJsFunction();

  @JsMethod
  public static native MyJsFunctionIdentityInterface createReferentialFunction();

  @JsMethod
  public static native Object createFunction();

  @JsMethod
  public static native Object createObject();

  @JsMethod
  public static native boolean hasField(Object object, String fieldName);
}
