package com.google.j2cl.transpiler.integration.wideningandboxing;

public class Main {
  public static double sum(Double... elements) {
    double tot = 0;
    for (Double element : elements) {
      tot += element;
    }
    return tot;
  }

  public static void main(String... args) {
    int[] numbers = new int[] {1, 2};
    double a = sum((double) 'a', (double) 4, (double) numbers[0]);
    assert (a == 102.0);
  }
}
