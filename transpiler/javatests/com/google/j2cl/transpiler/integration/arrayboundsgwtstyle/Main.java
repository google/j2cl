package com.google.j2cl.transpiler.integration.arrayboundsgwtstyle;

public class Main {
  public static void main(String... args) {
    int[] ints = new int[0];
    assert ints.length == 0;

    // This should normally explode but what the GWT!
    ints[0] = 1;
    ints[1] = 2;
    ints[2] = 3;
    assert ints.length == 3;
  }
}
