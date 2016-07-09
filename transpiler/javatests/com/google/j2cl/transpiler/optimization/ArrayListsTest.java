package com.google.j2cl.transpiler.optimization;

import static com.google.j2cl.transpiler.optimization.OptimizationTestUtil.assertFunctionMatches;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

@RunWith(JUnit4.class)
public class ArrayListsTest {

  private ArrayList<String> arrayListField = new ArrayList<String>();

  @JsType(isNative = true, name = "ArrayList", namespace = "java.util")
  interface HasArrayListMethods {
    @JsProperty(name = "get")
    public Object getMethod();

    @JsProperty(name = "set")
    public Object setMethod();
  }

  @Test
  public void arrayListGetChecksAreRemoved() {
    Object getMethod = ((HasArrayListMethods) arrayListField).getMethod();
    assertFunctionMatches(getMethod, "return this.<obf>[<obf>];");
  }

  @Test
  public void arrayListSetChecksAreRemoved() {
    HasArrayListMethods instance = (HasArrayListMethods) arrayListField;
    assertFunctionMatches(
        instance.setMethod(),
        "var <obf>=this.<obf>(<obf>); this.<obf>[<obf>]=<obf>; return <obf>;");
  }
}
