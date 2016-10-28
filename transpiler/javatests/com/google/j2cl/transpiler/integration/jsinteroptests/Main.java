package com.google.j2cl.transpiler.integration.jsinteroptests;

public class Main {
  public static void main(String... args) {
    JsExportTest.testAll();
    JsFunctionTest.testAll();
    JsMethodTest.testAll();
    JsPropertyTest.testAll();
    JsTypeArrayTest.testAll();
    JsTypeBridgeTest.testAll();
    JsTypeTest.testAll();
    NativeJsTypeTest.testAll();
    JsTypeVarargsTest.testAll();
    JsTypeObjectMethodsTest.testAl();
  }
}
