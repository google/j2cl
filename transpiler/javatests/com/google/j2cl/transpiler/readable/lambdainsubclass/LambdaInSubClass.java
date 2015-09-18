package com.google.j2cl.transpiler.readable.lambdainsubclass;

interface LambdaInterface {
  int foo(int a);
}

public class LambdaInSubClass extends Parent {
  public void test() {
    LambdaInterface l =
        (i -> {
          // call to outer class's inherited function
          funInParent(); // this.outer.funInParent()
          this.funInParent(); // this.outer.funInParent()
          LambdaInSubClass.this.funInParent(); // this.outer.funInParent()

          // access to outer class's inherited fields
          int a = fieldInParent;
          a = this.fieldInParent;
          a = LambdaInSubClass.this.fieldInParent;
          return a;
        });
  }
}
