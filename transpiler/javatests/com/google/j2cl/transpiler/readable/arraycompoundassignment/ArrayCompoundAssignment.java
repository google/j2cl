package com.google.j2cl.transpiler.readable.arraycompoundassignment;

public class ArrayCompoundAssignment {
  public void main() {
    int[] ints = new int[100];

    ints[0] += 1;
    ints[0] -= 1;
    ints[0] *= 1;
    ints[0] /= 1;
    ints[0] &= 1;
    ints[0] ^= 1;
    ints[0] |= 1;
    ints[0] %= 1;
    ints[0] <<= 1;
    ints[0] >>= 1;
    ints[0] >>>= 1;
  }
}
