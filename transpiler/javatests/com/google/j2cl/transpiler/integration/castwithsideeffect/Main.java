package com.google.j2cl.transpiler.integration.castwithsideeffect;

public class Main {
  public int count = 0;

  public Object test() {
    count++;
    return new Main();
  }

  public static void main(String... args) {
    Main m = new Main();
    Main mm = (Main) m.test();
    assert (m.count == 1);
  }
}
