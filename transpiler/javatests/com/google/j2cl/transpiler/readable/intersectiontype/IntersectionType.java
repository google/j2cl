package com.google.j2cl.transpiler.readable.intersectiontype;

public class IntersectionType {
  interface Getable {
    int get();
  }
  
  interface Setable {
    void set(int i);
  }
  
  public static <T extends Getable & Setable> void getAndSet(T object) {
    object.set(1);
    object.get();
  }
}
