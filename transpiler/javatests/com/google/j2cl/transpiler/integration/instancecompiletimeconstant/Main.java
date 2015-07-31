package com.google.j2cl.transpiler.integration.instancecompiletimeconstant;

public class Main {
  private static class Parent {
    public final String parentCompileTimeConstantString = "987";
    public final byte parentCompileTimeConstantByte = 123;
    public final short parentCompileTimeConstantShort = 123;
    public final int parentCompileTimeConstantInt = 123;
    public final long parentCompileTimeConstantLong = 123L;
    public String parentRegularString = "987";
    public int parentRegularInt = 123;

    public Parent() {
      checkFieldsInitialized();
    }

    /**
     * If this check() is called then the control flow was main -> Parent.constructor() ->
     * Parent.checkFieldsInitialized() and as a result both instance fields A and B should already
     * be initialized.
     */
    public void checkFieldsInitialized() {
      assert parentCompileTimeConstantString != null;
      assert parentCompileTimeConstantByte != 0;
      assert parentCompileTimeConstantShort != 0;
      assert parentCompileTimeConstantInt != 0;
      assert parentCompileTimeConstantLong != 0L;

      // Parent's non compile time constant fields *have* been initialized because of the time at
      // which check was called!
      assert parentRegularString != null;
      assert parentRegularInt != 0;
    }
  }

  private static class Child extends Parent {
    public final float childCompileTimeConstantFloat = 123;
    public final double childCompileTimeConstantDouble = 123;
    public final char childCompileTimeConstantChar = 123;
    public final boolean childCompileTimeConstantBoolean = true;
    public String childRegularString = "987";
    public int childRegularInt = 123;

    public Child() {
      super();
    }

    /**
     * If this check() is called then the control flow was main -> Child.constructor()->
     * Parent.constructor() -> Child.checkFieldsInitialized() and as a result both of Parent
     * instance fields A and B should already be initialized BUT only final Child instance fields
     * should be initialized.
     */
    @Override
    public void checkFieldsInitialized() {
      super.checkFieldsInitialized();

      assert childCompileTimeConstantFloat != 0;
      assert childCompileTimeConstantDouble != 0;
      assert childCompileTimeConstantChar != 0;
      assert childCompileTimeConstantBoolean != false;

      // Child's non compile time constant fields have not been initialized!
      assert childRegularString == null;
      assert childRegularInt == 0;
    }
  }

  @SuppressWarnings("unused")
  public static void main(String... args) {
    Child child = new Child();
  }
}
