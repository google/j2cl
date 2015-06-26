package com.google.j2cl.transpiler.readable.instanceinitializers;

public class InstanceInitializers {
  public int field = 1; // #1

  {
    field = 2; // #2
  }
}
