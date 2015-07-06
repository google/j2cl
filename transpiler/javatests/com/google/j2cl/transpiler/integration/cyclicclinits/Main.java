package com.google.j2cl.transpiler.integration.cyclicclinits;

/**
 * Test cyclic $clinits.
 */
public class Main {
  public static void main(String[] args) {
    // call on static method test() triggers Child.$clinit(), which then triggers Parent.$clinit().
    // In Parent.$clinit(), it refers to static field in Child, while this does not trigger
    // Child.$clinit().
    Child.test();
  }
}
