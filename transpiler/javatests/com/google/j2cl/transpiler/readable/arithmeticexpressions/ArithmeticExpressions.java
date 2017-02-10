package com.google.j2cl.transpiler.readable.arithmeticexpressions;

public class ArithmeticExpressions {
  public void main() {
    int a = 10;
    int b = a++;
    int c = a--;
    int d = ++a;
    int e = --a;
    int f = -a;
    int g = +a;
    int h = ~a;
    int i = 1 + 1 + 2 - 5;
    int j = (1 + 2) * (3 + 4);
    boolean k = !(1 + 2 + 3 == 4);
    boolean l = (1 + 2 != 4);
    boolean m = Long.MAX_VALUE != 9223372036854776833d;
    getLongArray()[0]++;
    double o = (5 / 2) - 0.0;

    Integer boxI = 3;
    a += boxI;

    // Uncomment once b/35219599 is fixed.
    // (k) |= true;

  }

  static long[] getLongArray() {
    return new long[10];
  }
}
