package com.google.j2cl.transpiler.readable.casttoprimitive;

public class CastToPrimitive {
  public void test() {
    byte b = 1;
    char c = 1;
    short s = 1;
    int i = 1;
    long l = 1L;
    float f = 1.1f;
    double d = 1.1;

    b = (byte) b;
    c = (char) b;
    s = (short) b;
    i = (int) b;
    l = (long) b;
    f = (float) b;
    d = (double) b;

    b = (byte) c;
    c = (char) c;
    s = (short) c;
    i = (int) c;
    l = (long) c;
    f = (float) c;
    d = (double) c;

    b = (byte) s;
    c = (char) s;
    s = (short) s;
    i = (int) s;
    l = (long) s;
    f = (float) s;
    d = (double) s;

    b = (byte) i;
    c = (char) i;
    s = (short) i;
    i = (int) i;
    l = (long) i;
    f = (float) i;
    d = (double) i;

    b = (byte) l;
    c = (char) l;
    s = (short) l;
    i = (int) l;
    l = (long) l;
    f = (float) l;
    d = (double) l;

    b = (byte) f;
    c = (char) f;
    s = (short) f;
    i = (int) f;
    l = (long) f;
    f = (float) f;
    d = (double) f;

    b = (byte) d;
    c = (char) d;
    s = (short) d;
    i = (int) d;
    l = (long) d;
    f = (float) d;
    d = (double) d;
  }

  public void testReferenceToPrimitive() {
    Object o = new Object();
    boolean bool = (boolean) o;
    byte b = (byte) o;
    char c = (char) o;
    short s = (short) o;
    int i = (int) o;
    long l = (long) o;
    float f = (float) o;
    double d = (double) o;
  }

  public void testUnboxAndWiden() {
    Byte boxedByte = Byte.valueOf((byte) 0);

    // char c = (char) boxedByte; // illegal
    short s = (short) boxedByte;
    int i = (int) boxedByte;
    long l = (long) boxedByte;
    float f = (float) boxedByte;
    double d = (double) boxedByte;
  }
}
