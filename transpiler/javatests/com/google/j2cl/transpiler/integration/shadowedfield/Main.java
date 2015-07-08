package com.google.j2cl.transpiler.integration.shadowedfield;

/**
 * Test shadowed field.
 */
public class Main {
  public static void main(String[] args) {
    Child instance = new Child();

    // Value starts uninitialized.
    assert instance.foo == 0;

    ((Parent) instance).foo = 10;
    // Changing the foo field value in the ParentClass has no effect on the field when read out of
    // the ChildClass, because it's shadowed.
    assert instance.foo == 0;
    // But explicitly reading the field from the ParentClass will show the value change.
    assert ((Parent) instance).foo == 10;
    // Changing the value in the shadowing field is visible when also reading from that shadowing
    // field.
    instance.foo = 20;
    assert instance.foo == 20;
  }
}
