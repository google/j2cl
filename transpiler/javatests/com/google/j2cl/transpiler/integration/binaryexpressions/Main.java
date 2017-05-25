package com.google.j2cl.transpiler.integration.binaryexpressions;

/** Test basic binary operations. This test does not aim to test primitive overflow and Coercion. */
@SuppressWarnings({"NarrowingCompoundAssignment", "ReferenceEquality"})
public class Main {
  public static void main(String[] args) {
    int a = 6;
    int b = 3;
    int c = -1;
    assert a * b == 18;
    assert a / b == 2;
    assert a % b == 0;
    assert a + b == 9;
    assert a - b == 3;
    assert a << 1 == 12;
    assert c >> 16 == -1;
    assert c >>> 16 == 65535;
    assert a > b;
    assert a >= b;
    assert b < a;
    assert b <= a;
    assert a != b;
    assert (a ^ b) == 5;
    assert (a & b) == 2;
    assert (a | b) == 7;
    assert (a > b) && (a == 6);
    assert (a < b) || (a == 6);
    assert (-1 >>> 0) == -1;

    assert a + b + a - b == 12;
    assert (a + b) * (a - b) == 27;

    int i = 1;
    i += 1L;
    assert i == 2;

    int d = 10;
    assert d == 10;
    d ^= d;
    assert d == 0;
    d += 15;
    assert d == 15;
    d -= 5;
    assert d == 10;
    d *= 2;
    assert d == 20;
    d /= 4;
    assert d == 5;
    d &= 3;
    assert d == 1;
    d |= 2;
    assert d == 3;
    d %= 2;
    assert d == 1;
    d <<= 3;
    assert d == 8;
    d >>= 3;
    assert d == 1;
    d = -1;
    d >>>= 16;
    assert d == 65535;
    d = -1;
    d >>>= 0;
    assert d == -1;

    boolean bool = true;
    bool &= false;
    assert ("" + bool).equals("false");

    String s = null;
    assert s + s == "nullnull";

    String[] stringArray = new String[1];
    assert stringArray[0] + stringArray[0] == "nullnull";

    assert ((5 / 2) - 0.0) == 2.0;

    // Compound assignment with enclosing instance.
    class Outer {
      boolean b;

      class Inner {
        {
          b |= true;
        }
      }
    }

    final Outer finalOuter = new Outer();
    finalOuter.b |= true;
    assert finalOuter.b;

    Outer outer = new Outer();
    Outer copy = outer;
    outer.b |= (outer = null) == null;
    assert copy.b;

    outer = new Outer();
    outer.new Inner();
    assert outer.b;
  }
}
