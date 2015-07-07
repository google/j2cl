package com.google.j2cl.transpiler.integration.blockscoping;

public class Main {
  @SuppressWarnings("unused")
  public static void main(String... args) {
    // If 'i' is not block scoped then Closure compile will fail and if the 'i' created in the
    // 'for' header is not available in the 'for' body then runtime will fail.
    for (int i = 11; i < 12; i++) {
      assert i == 11;
    }

    // If 'i' is not block scoped then Closure compile will fail.
    boolean firstTime = true;
    while (firstTime) {
      int i = 22;
      assert i == 22;
      firstTime = false;
    }

    // If 'i' is not block scoped then Closure compile will fail.
    if (!firstTime) {
      int i = 33;
      assert i == 33;
    }

    // If 'i' is not block scoped then Closure compile will fail.
    {
      int i = 44;
      assert i == 44;
    }
  }
}
