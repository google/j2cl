package com.google.j2cl.transpiler.integration.supermethodcall;

public class Parent extends GrandParent {
  public String getNameTwo() {
    return "Parent";
  }

  public int getParentPassedValue(int value) {
    return value;
  }

  public Object getParentPassedValueWithChangingReturn(Parent value) {
    return value;
  }
}
