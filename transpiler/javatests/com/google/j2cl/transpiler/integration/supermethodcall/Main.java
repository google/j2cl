package com.google.j2cl.transpiler.integration.supermethodcall;

/**
 * Test non-constructor super method calls.
 */
public class Main {
  public static void main(String[] args) {
    Child child = new Child();
    Parent parent = new Parent();
    GrandParent grandParent = new GrandParent();

    // Given the same inputs the functions return the same values, because the child calls are just
    // delegating to the super calls.
    assert child.getNameTwo() == parent.getNameTwo();
    assert child.getParentPassedValue(15) == parent.getParentPassedValue(15);
    assert child.getParentPassedValueWithChangingReturn(parent)
        == parent.getParentPassedValueWithChangingReturn(parent);
    assert child.getNameOne() == parent.getNameOne();
    assert child.getGrandParentPassedValueTimesTwo(15)
        == parent.getGrandParentPassedValueTimesTwo(15);
    assert child.getGrandParentPassedValueWithChangingReturn(parent)
        == parent.getGrandParentPassedValueWithChangingReturn(parent);
    assert child.getNameOne() == grandParent.getNameOne();
    assert child.getGrandParentPassedValueTimesTwo(15)
        == grandParent.getGrandParentPassedValueTimesTwo(15);
    assert child.getGrandParentPassedValueWithChangingReturn(grandParent)
        == grandParent.getGrandParentPassedValueWithChangingReturn(grandParent);

    // But with different inputs of course the results also differ.
    Parent unexpectedParentInstance = new Parent();
    assert child.getParentPassedValue(99999) != parent.getParentPassedValue(1);
    assert child.getParentPassedValueWithChangingReturn(unexpectedParentInstance)
        != parent.getParentPassedValueWithChangingReturn(parent);
    assert child.getGrandParentPassedValueTimesTwo(99999)
        != parent.getGrandParentPassedValueTimesTwo(1);
    assert child.getGrandParentPassedValueWithChangingReturn(unexpectedParentInstance)
        != parent.getGrandParentPassedValueWithChangingReturn(parent);
    assert child.getGrandParentPassedValueTimesTwo(99999)
        != grandParent.getGrandParentPassedValueTimesTwo(1);
    assert child.getGrandParentPassedValueWithChangingReturn(unexpectedParentInstance)
        != grandParent.getGrandParentPassedValueWithChangingReturn(grandParent);
  }
}
