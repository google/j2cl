package com.google.j2cl.transpiler.readable.devirtualizedcalls;

public class ObjectCalls {
  public void main() {
    Object object = new Object();

    object.equals(object);
    object.hashCode();
    object.toString();
    object.getClass();
  }
}
