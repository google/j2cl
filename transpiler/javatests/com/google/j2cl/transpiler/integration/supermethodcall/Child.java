package com.google.j2cl.transpiler.integration.supermethodcall;

public class Child extends Parent {
  @Override
  public String getNameOne() {
    return super.getNameOne();
  }

  @Override
  public int getGrandParentPassedValueTimesTwo(int value) {
    return super.getGrandParentPassedValueTimesTwo(value);
  }

  @Override
  public GrandParent getGrandParentPassedValueWithChangingReturn(GrandParent value) {
    return (GrandParent) super.getGrandParentPassedValueWithChangingReturn(value);
  }

  @Override
  public String getNameTwo() {
    return super.getNameTwo();
  }

  @Override
  public int getParentPassedValue(int value) {
    return super.getParentPassedValue(value);
  }

  @Override
  public Parent getParentPassedValueWithChangingReturn(Parent value) {
    return (Parent) super.getParentPassedValueWithChangingReturn(value);
  }
}
