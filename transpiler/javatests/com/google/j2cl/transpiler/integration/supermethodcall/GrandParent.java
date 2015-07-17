package com.google.j2cl.transpiler.integration.supermethodcall;

public class GrandParent {
  public String getNameOne() {
    return "GrandParent";
  }

  public int getGrandParentPassedValueTimesTwo(int value) {
    return value * 2;
  }

  public Object getGrandParentPassedValueWithChangingReturn(GrandParent value) {
    return value;
  }
}
