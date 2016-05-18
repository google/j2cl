package com.google.j2cl.transpiler.integration.jsinteroptests;

public class Main {
  public static void main(String... args) {
    runJsExportTest();
    runJsFunctionTest();
    runJsMethodTest();
    runJsPropertyTest();
    runJsTypeArrayTest();
    runJsTypeBridgeTest();
    runJsTypeTest();
    runNativeJsTypeTest();
    runJsTypeVarargsTest();
  }

  public static void runJsExportTest() {
    JsExportTest test = new JsExportTest();
    test.testClinit_staticField();
    test.testClinit_staticMethod();
    test.testClinit_virtualMethod();
    test.testClinit();
    test.testEnum_enumerations();
    test.testEnum_exportedFields();
    test.testEnum_exportedMethods();
    test.testEnum_notExported();
    test.testEnum_subclassEnumerations();
    test.testEnum_subclassMethodCallFromExportedEnumerations();
    test.testExportClass_correctNamespace();
    test.testExportClass_implicitConstructor();
    test.testExportConstructors();
    test.testExportedField();
    test.testExportedFieldRefInExportedMethod();
    test.testExportedMethod();
    test.testInheritClassNamespace();
    // Not supported in J2CL since the Closure import/export system does not allow exporting types
    // onto the global scope and if we emitted a normal "var $NativeFoo = window.Foo;" style global
    // type alias there would be nothing causing the file that provides Foo to be loaded.
    // test.testInheritClassNamespace_empty();
    test.testInheritClassNamespace_nested();
    test.testInheritClassNamespace_nestedNoExport();
    test.testInheritClassNamespace_noExport();
    test.testInheritClassNamespace_withName();
    test.testInheritPackageNamespace_nestedClass();
    test.testInheritPackageNamespace_nestedEnum();
    test.testInheritPackageNamespace_subpackage();
    test.testInheritPackageNamespace();
    test.testMethodExport_notReferencedFromJava();
    test.testMethodExport();
    test.testMethodExportWithLong();
    test.testNoExport();
  }

  public static void runJsFunctionTest() {
    JsFunctionTest test = new JsFunctionTest();
    test.testCast_crossCastJavaInstance();
    test.testCast_fromJsFunction();
    test.testCast_fromJsObject();
    test.testCast_inJava();
    test.testInstanceOf_javaInstance();
    test.testInstanceOf_jsFunction();
    test.testInstanceOf_jsObject();
    test.testJsFunctionAccess();
    test.testJsFunctionBasic_java();
    test.testJsFunctionBasic_javaAndJs();
    test.testJsFunctionBasic_js();
    test.testJsFunctionCallbackPattern();
    test.testJsFunctionCallFromAMember();
    test.testJsFunctionIdentity_java();
    test.testJsFunctionIdentity_js();
    test.testJsFunctionJs2Java();
    test.testJsFunctionReferentialIntegrity();
    test.testJsFunctionSuccessiveCalls();
    test.testJsFunctionViaFunctionMethods();
  }

  public static void runJsMethodTest() {
    JsMethodTest test = new JsMethodTest();
    test.testNativeJsMethod();
    test.testStaticNativeJsMethod();
    test.testStaticNativeJsPropertyGetter();
    test.testStaticNativeJsPropertySetter();
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
    test.testNativeJsTypeWithConstructorSubclass();
    test.testProtectedNames();
  }

  public static void runJsTypeArrayTest() {
    JsTypeArrayTest test = new JsTypeArrayTest();
    test.testJsType3DimArray_castFromNativeWithACall();
    test.testJsTypeArray_asAField();
    test.testJsTypeArray_asAParam();
    test.testJsTypeArray_instanceOf();
    test.testJsTypeArray_objectArrayInterchangeability();
    test.testJsTypeArray_returnFromNative();
    test.testJsTypeArray_returnFromNativeWithACall();
    test.testObjectArray_castFromNative();
    test.testObjectArray_instanceOf();
  }

  public static void runJsTypeBridgeTest() {
    JsTypeBridgeTest test = new JsTypeBridgeTest();
    test.testBridges();
  }

  public static void runJsTypeTest() {
    JsTypeTest test = new JsTypeTest();
    test.testAbstractJsTypeAccess();
    test.testCasts();
    test.testConcreteJsTypeAccess();
    test.testConcreteJsTypeNoTypeTightenField();
    test.testConcreteJsTypeSubclassAccess();
    test.testEnumeration();
    test.testEnumJsTypeAccess();
    test.testEnumSubclassEnumeration();
    test.testInstanceOf_concreteJsType();
    test.testInstanceOf_extendsJsTypeWithProto();
    test.testInstanceOf_implementsJsType();
    test.testInstanceOf_implementsJsTypeWithPrototype();
    test.testInstanceOf_jsoWithNativeButtonProto();
    test.testInstanceOf_jsoWithoutProto();
    test.testInstanceOf_jsoWithProto();
    test.testInstanceOf_withNameSpace();
    test.testJsMethodWithDifferentVisiblities();
    test.testJsTypeField();
    test.testNamedBridge();
    test.testNativeMethodOverrideNoTypeTightenParam();
    test.testRevealedOverrideJsType();
    test.testSingleJavaConcreteInterface();
    test.testSingleJavaConcreteJsFunction();
  }

  public static void runNativeJsTypeTest() {
    NativeJsTypeTest test = new NativeJsTypeTest();
    test.testClassLiterals();
    test.testToString();
    test.testEquals();
    test.testHashCode();
    test.testNativeJsTypeWithOverlay();
    test.testNativeJsTypeWithStaticIntializer();
  }

  public static void runJsTypeVarargsTest() {
    JsTypeVarargsTest test = new JsTypeVarargsTest();
    // test.testVarargsCall_constructors();
    test.testVarargsCall_fromJavaScript();
    test.testVarargsCall_jsFunction();
    test.testVarargsCall_regularMethods();
    // test.testVarargsCall_superCalls();
    // test.testVarargsCall_sideEffectingInstance();
  }
}
