package com.google.j2cl.transpiler.readable.equality;

public class Equality {
  public void test() {
    // Primitives
    {
      assert false == false;
      assert 0 != 1;
    }

    // Objects
    {
      assert new Object() != new Object();
    }

    // Null literals
    {
      assert null != new Object();
      assert new Object() != null;
    }
  }
}
