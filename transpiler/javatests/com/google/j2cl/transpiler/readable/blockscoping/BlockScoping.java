package com.google.j2cl.transpiler.readable.blockscoping;

public class BlockScoping {
  @SuppressWarnings("unused")
  public void main() {
    {
      int i = 0;
    }
    {
      int i = 1;
    }
  }
}
