package com.google.j2cl.transpiler.readable.devirtualizedcalls;

public class ObjectCalls {
  @SuppressWarnings({"ArrayEquals", "ArrayHashCode", "ArrayToString"})
  public void main() {
    Object object = new Object();

    object.equals(object);
    object.hashCode();
    object.toString();
    object.getClass();

    ObjectCalls[] objectCalls = new ObjectCalls[1];
    objectCalls.equals(objectCalls);
    objectCalls.hashCode();
    objectCalls.toString();
    objectCalls.getClass();
  }

  // test object calls in the same declaring class with implicit qualifiers and this qualifier.
  public void test() {
    equals(new Object());
    hashCode();
    toString();
    getClass();

    this.equals(new Object());
    this.hashCode();
    this.toString();
    this.getClass();
  }
}
