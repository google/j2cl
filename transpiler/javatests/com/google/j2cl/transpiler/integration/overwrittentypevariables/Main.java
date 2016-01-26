package com.google.j2cl.transpiler.integration.overwrittentypevariables;

enum A {
  FIRST,
  SECOND;
}

public class Main {
  public static void main(String... args) {
    HashFunction<A> hashFunction = HashFunctions.enumHashFunction();
    assert hashFunction.apply(A.FIRST).equals("0");
    HashFunction<Object> f = HashFunctions.hashFunction();
    assert f.apply(new Object()).equals("a");
  }
}
