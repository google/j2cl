package com.google.j2cl.transpiler.readable.staticfieldcompoundassignment;

public class StaticFieldCompoundAssignment {
  public static long one = 1;
  public static long foo = one++;
  public long bar = foo++;

  {
    foo++;
  }
}
