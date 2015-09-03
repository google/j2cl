package com.google.j2cl.transpiler.integration.objectchilddevirtualcalls;

public class ChildClassOverrides {
  public boolean equals(Object o) {
    return o instanceof ChildClassOverrides;
  }

  public int hashCode() {
    return 100;
  }

  public String toString() {
    return "ChildClassOverrides";
  }

  /**
   * Test object method calls with implicit qualifiers and explicit this.
   */
  public void test() {
    assert (equals(new ChildClassOverrides()));
    assert (!equals(new Object()));
    assert (this.equals(new ChildClassOverrides()));
    assert (!this.equals(new Object()));
    assert (this.hashCode() == 100);
    assert (hashCode() == 100);
    assert (this.toString().equals("ChildClassOverrides"));
    assert (toString().equals("ChildClassOverrides"));
    assert (this.getClass() instanceof Class);
    assert (getClass() instanceof Class);
  }
}
