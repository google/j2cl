package com.google.j2cl.transpiler.readable.circularimportuse;

public class Right {
  Left createLeft() {
    return new Left();
  }
}
