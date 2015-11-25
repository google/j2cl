package com.google.j2cl.transpiler.integration.jsinteroptests;

public class Main {
  public static void main(String... args) {
    testJsPropertyTest();
  }

  public static void testJsPropertyTest() {
    JsPropertyTest test = new JsPropertyTest();
    test.testConcreteJsType();
    //    test.testJavaClassImplementingMyJsTypeInterfaceWithProperty();
    //    test.testJsPropertyAccidentalOverrideSuperCall();
    //    test.testJsPropertyBridges();
    //    test.testJsPropertyBridgesSubclass();
    test.testJsPropertyGetX();
    test.testJsPropertyIsX();
    //    test.testJsPropertyRemovedAccidentalOverrideSuperCall();
    test.testNativeJsType();
    //    test.testNativeJsTypeSubclass();
    test.testNativeJsTypeWithConstructor();
    //    test.testNativeJsTypeWithConstructorSubclass();
    test.testProtectedNames();
  }
}
