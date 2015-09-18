package com.google.j2cl.transpiler.readable.mixednestings;

interface MyInterface {
  int fun(int a);
}

/**
 * Test lambda having access to local variables and arguments when placed in mixed scopes.
 * Local class -> local class -> anonymous -> lambda -> lambda -> anonymous
 */
public class MixedNestings {
  public void mm() {}

  class A {
    public void aa() {}

    public void a() {
      class B {
        public void bb() {}

        public int b() {
          MyInterface i =
              new MyInterface() {

                @Override
                public int fun(int a) {
                  MyInterface ii =
                      n -> {
                        mm();
                        aa();
                        bb();
                        MyInterface iii =
                            m -> {
                              mm();
                              aa();
                              bb();
                              return new MyInterface() {
                                @Override
                                public int fun(int b) {
                                  return b;
                                }
                              }.fun(100);
                            };
                        return iii.fun(200);
                      };
                  return ii.fun(300);
                }
              };
          return i.fun(400);
        }
      }
      new B().b();
    }
  }

  public void test() {
    new A().a();
  }
}
