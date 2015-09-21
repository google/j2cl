package com.google.j2cl.transpiler.readable.circularimportuse;

public class Left {
  Right createRight() {
    return new Right();
  }
}
