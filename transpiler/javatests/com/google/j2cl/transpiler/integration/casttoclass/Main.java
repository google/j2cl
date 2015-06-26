package com.google.j2cl.transpiler.integration.casttoclass;

/**
 * Test cast to class type.
 */
public class Main {
  public static void main(String[] args) {
    Object o = new Main();
    Main m = (Main) o;
    assert m != null;
    // TODO: add wrong cast after try{} catch{} is in.
  }
}
