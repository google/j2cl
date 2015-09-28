package com.google.j2cl.transpiler.integration.obviouslydeadcode;

public class Main {

  public static void main(String... args) {
    if (false) {
      // This should not be a compile error in JSComp since it is not a compile error in Java.
    }
  }
}
