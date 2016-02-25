package com.google.j2cl.transpiler.integration.jsbridgemethodaccidentaloverride;

public interface OtherInterface {
  int fun(int a);
  @Override
  String toString();
}
