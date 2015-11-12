package com.google.j2cl.transpiler.integration.trywithresource;

/**
 * Test cases that follow the outline in JLS 14.20.3.1.
 */
public class Main {
  public static void main(String... args) {
    TryWithResourceSingleResourceTests.run();
    TryWithResourceMultipleResourcesTests.run();
  }
}
