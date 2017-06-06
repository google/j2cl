/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.integration.localclasswithcaptures;

/**
 * Test local classes with captured variables.
 */
public class Main {
  public int funInMain(int a, int b) {
    return a + b + 11;
  }

  public int foo(int a) {
    return a + 4;
  }

  public int testSimple(final int p) {
    final int localVar = 1;
    class InnerClass {
      public int fun() {
        class InnerInnerClass {
          public int bar() {
            return localVar + p;
          }
        }
        return new InnerInnerClass().bar(); // localVar + p
      }
    }
    return new InnerClass().fun() + p; // localVar + p + p
  }

  public int testFunctionCall(final int p) {
    final int localVar = 1;
    class InnerClass {
      public int fun() {
        class InnerInnerClass {
          public int bar() {
            return funInMain(localVar, p) + Main.this.funInMain(localVar, p);
          }
        }
        return new InnerInnerClass().bar(); // (localVar + p + 11) * 2
      }
    }
    return new InnerClass().fun(); // (localVar + p + 11) * 2
  }

  public int testConstructor(final int p) {
    final int localVar = 1;
    // local class with non-default constructor and this() call
    class LocalClassWithConstructor {
      public int field;

      public LocalClassWithConstructor(int a, int b) {
        field = localVar + a + b;
      }

      public LocalClassWithConstructor(int a) {
        this(a, p);
      }
    }
    int first = new LocalClassWithConstructor(1, 2).field; // 1 + 1 + 2 = 4
    int second = new LocalClassWithConstructor(3).field; // 1 + 3 + p;
    return first + second; // 8 + p;
  }

  public int testClassWithSameName() {
    final int a = 10;
    class InnerClass {
      public int test(int b) {
        return a + b;
      }
    }
    return new InnerClass().test(20); // 30
  }

  public int testClassWithParent() {
    final int a = 10;
    class ParentClass {
      int fun(int p) {
        return a + p;
      }
    }

    // local classes with/without explicit super() whose parent is also a local class
    class ChildClass extends ParentClass {}

    class AnotherChild extends ParentClass {
      public AnotherChild() {
        super();
      }
    }

    return new ChildClass().fun(20) + new AnotherChild().fun(100); // 30 + 110 = 140;
  }

  public int testFunctionCallWithOverriding() {
    class SubMain extends Main {
      @Override
      public int foo(int a) {
        return a + 5;
      }

      public int test(int a) {
        int result = funInMain(a, a); // a+a+11
        result += Main.this.funInMain(a, a); // a+a+11
        result += foo(a); // a+5
        result += this.foo(a); // a+5
        result += Main.this.foo(a); // it should be a+4, not a + 5.
        return result;
      }
    }

    return new SubMain().test(9);
  }

  public void testIndirectCapture() {
    int local = 3;
    class ClassCapturingLocal {
      int returnLocal() {
        return local;
      }
    }

    // TODO(b/33438153): uncomment the code that follows when captures are implemented properly.
    // class ClassIndirectlyCapturingLocal {
    //  int returnInderctCapture() {
    //    return new ClassCapturingLocal().returnLocal();
    //  }
    // }
    // assert new ClassIndirectlyCapturingLocal().returnInderctCapture() == 3;
  }

  public static void main(String[] args) {
    Main m = new Main();
    assert m.testSimple(100) == 201;
    assert m.testConstructor(100) == 108;
    assert m.testClassWithSameName() == 30;
    assert m.testClassWithParent() == 140;
    assert m.testFunctionCallWithOverriding() == 99;
    m.testIndirectCapture();
  }
}
