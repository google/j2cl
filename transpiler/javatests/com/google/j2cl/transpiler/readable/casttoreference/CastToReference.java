package com.google.j2cl.transpiler.readable.casttoreference;

public class CastToReference {

  public void testPrimitiveToReference() {
    boolean bool = true;
    byte b = 1;
    char c = 'a';
    short s = 1;
    int i = 1;
    long l = 1L;
    float f = 1.0f;
    double d = 1.0;
    Object o = bool;
    o = b;
    o = c;
    o = s;
    o = i;
    o = l;
    o = f;
    o = d;
    o = (Object) bool;
    o = (Object) b;
    o = (Object) c;
    o = (Object) s;
    o = (Object) i;
    o = (Object) l;
    o = (Object) f;
    o = (Object) d;
  }
}
