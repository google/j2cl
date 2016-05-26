package com.google.j2cl.transpiler.optimization;

import static com.google.j2cl.transpiler.optimization.OptimizationTestUtil.assertFunctionMatches;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;

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

  @JsProperty
  private native Object getModifyArray();

  @Test
  public void arrayStoreChecksAreRemoved() {
    assertFunctionMatches(getModifyArray(), "this.<obf>[0]='ABC';");
  }
}
