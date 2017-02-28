package com.google.j2cl.transpiler.optimization;

import static com.google.j2cl.transpiler.optimization.OptimizationTestUtil.assertFunctionMatches;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ArraysTest {

  private static class TestObject {}

  private Object[] arrayField = new TestObject[3];

  @JsMethod
  public void modifyArray() {
    arrayField[0] = "ABC";
  }

  @Test
  public void arrayStoreChecksAreRemoved() {
    assertFunctionMatches(((MethodsAsProperties) this).getModifyArray(), "this.<obf>[0]='ABC';");
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface MethodsAsProperties {
    @JsProperty
    Object getModifyArray();
  }
}
