package com.google.j2cl.transpiler.integration.jsinteroptests;

public class Main {
  public static void main(String... args) {
    runJsMethodTest();
    runJsPropertyTest();
    runJsTypeBridgeTest();
  }

  public static void runJsMethodTest() {
    JsMethodTest test = new JsMethodTest();
//    test.testNativeJsMethod();
//    test.testStaticNativeJsMethod();
//    test.testStaticNativeJsPropertyGetter();
//    test.testStaticNativeJsPropertySetter();
  }

  public static void runJsTypeBridgeTest() {
    JsTypeBridgeTest test = new JsTypeBridgeTest();
    test.testBridges();
  }

  public static void runJsPropertyTest() {
    JsPropertyTest test = new JsPropertyTest();
    test.testConcreteJsType();
    test.testJavaClassImplementingMyJsTypeInterfaceWithProperty();
    test.testJsPropertyAccidentalOverrideSuperCall();
    test.testJsPropertyBridges();
    test.testJsPropertyBridgesSubclass();
    test.testJsPropertyGetX();
    test.testJsPropertyIsX();
    test.testJsPropertyRemovedAccidentalOverrideSuperCall();
    test.testNativeJsType();
    test.testNativeJsTypeSubclass();
    test.testNativeJsTypeSubclassNoOverride();
    test.testNativeJsTypeWithConstructor();
    // test.testNativeJsTypeWithConstructorSubclass();
    test.testProtectedNames();
  }
}
