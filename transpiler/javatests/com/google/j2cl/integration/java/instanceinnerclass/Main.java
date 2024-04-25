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
package instanceinnerclass;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test instance inner class.
 */
class OuterClass {
  public int fieldInOuter;
  public OuterClass(int f) {
    this.fieldInOuter = f;
  }
  class InnerClass {
    public int fieldInInner = fieldInOuter * 2;
  }
}

public class Main {
  public int fieldInMain;
  public Main(int f) {
    this.fieldInMain = f;
  }

  public int funInMain(int a) {
    return fieldInMain * a;
  }

  /**
   * Basic instance inner class.
   */
  public class A {
    public int fieldInA = fieldInMain * 2;
    public Main enclosingInstance = Main.this;
    public int fun() {
      return fieldInA * 3;
    }
  }

  /**
   * Instance inner class calls outer class's method.
   */
  public class X {
    public int funInX(int a) {
      int result = funInMain(a);
      result += Main.this.funInMain(a);
      return result;
    }
  }

  /**
   * Instance inner class with constructors and this() call.
   */
  public class B {
    public int fieldInB;
    public Main fieldMain;
    public B(int x, Main m) {
      fieldInB = x;
      fieldMain = m;
    }
    public B() {
      this(fieldInMain, Main.this);
    }
    public Main getBOuter() {
      return Main.this;
    }
  }

  /**
   * Two level nested inner class, with calls to outer class's functions.
   */
  public class C {
    public int funInC(int a) {
      return a + 11;
    }
    public class CC {
      public int funInCC(int a) {
        return a + 22;
      }
      public int test(int a) {
        int result = funInMain(a);
        result += Main.this.funInMain(a);
        result += funInC(a);
        result += C.this.funInC(a);
        result += funInCC(a);
        result += this.funInCC(a);
        return result;
      }
      public C fieldOfC = C.this;
      public Main fieldOfMain = Main.this;
    }
  }

  public class W extends X {
    @Override
    public int funInX(int a) {
      return a + 222;
    }

    /**
     * Inner class has different super class as outer class.
     */
    public class W1 extends C {
      public int test(int a) {
        int result = W.super.funInX(a);
        result += W.this.funInX(a);
        return result;
      }
    }

    /**
     * Inner class has the same super class as outer class.
     */
    public class W2 extends X {
      @Override
      public int funInX(int a) {
        return a + 333;
      }

      public int test(int a) {
        int result = W.super.funInX(a); // X.funInX()
        result += W.this.funInX(a); // W.funInX()
        result += funInX(a); // W2.funInX()
        return result;
      }
    }

    /**
     * Inner class has its outer class as its super class.
     */
    public class W3 extends W {
      @Override
      public int funInX(int a) {
        return a + 444;
      }

      public int test(int a) {
        int result = W.super.funInX(a); // X.funInX
        result += W.this.funInX(a); // W.funInX
        result += funInX(a); // funInX
        result += super.funInX(a); // W.funInX
        return result;
      }
    }
  }

  /**
   * Two level nested inner class, with calls to outer class's functions and inherited functions.
   */
  public class Y extends X {
    @Override
    public int funInX(int a) {
      return a + 44;
    }

    public int funInY(int a) {
      return a + 55;
    }

    public class YY extends X {
      public int test(int a) {
        int result = funInMain(a); // this.outer.outer.funInMain()
        result += funInX(a); // this.funInX()
        result += this.funInX(a); // this.funInX()
        result += Y.this.funInX(a); // this.outer.funInX()
        return result;
      }
    }
  }

  /**
   * Test inner class that extends enclosing class
   */
  public class Z {
    public int funInZ(int a) {
      return a + 66;
    }

    public class ZZ extends Z {
      @Override
      public int funInZ(int a) {
        return a + 77;
      }

      public int test(int a) {
        int result = funInZ(a); // this.funInZ(), a + 77
        result += this.funInZ(a); // this.funInZ(), a + 77
        result += Z.this.funInZ(a); // this.outer.funInZ(), a + 66
        return result;
      }
    }
  }

  /**
   * Test inner class whose parent is also an inner class, and shares the same enclosing instance.
   */
  public class Child extends B {
    public int fieldInChild;
    public Child(int x, int y) {
      super(x, new Main(y));
      fieldInChild = fieldInB + fieldMain.fieldInMain; //x + y
    }

    public Child() {
      fieldInChild = fieldInB + fieldMain.fieldInMain; // fieldInMain * 2;
    }
  }

  /**
   * Test inner class whose parent is also an inner class, and shares the same enclosing instance,
   * but the super() call is called by another instance.
   */
  public class Child2 extends B {
    public Child2() {
      new Main(30).super();
    }
    public Main getChild2Outer() {
      return Main.this;
    }
  }

  /**
   * Test inner class without any constructors, and whose parent is also an inner class, 
   * and they share the same enclosing instance.
   */
  public class D extends B {
    public int fieldInD = fieldMain.fieldInMain;
  }

  /**
   * Test inner class whose parent is also an inner class, and does not the same enclosing instance.
   */
  public class AnotherChild extends OuterClass.InnerClass {
    public int fieldInChild;
    public AnotherChild() {
      new OuterClass(10).super();
      fieldInChild = fieldInInner * 3; //10 * 2 * 3 = 60;
    }
  }

  interface InterfaceWithDefault {
    default String m() {
      return "InterfaceWithDefault.m from " + origin();
    }

    String origin();
  }

  public static class SuperClass {
    String s;

    public SuperClass(String s) {
      this.s = s;
    }

    public String m() {
      return "SuperClass.m from " + s;
    }
  }

  public static class EnclosingClass extends SuperClass {
    String s;

    public EnclosingClass(String s) {
      super(s);
      this.s = s;
    }

    public String m() {
      return "EnclosingClass.m from " + s;
    }

    public class InnerClass extends EnclosingClass implements InterfaceWithDefault {
      public InnerClass(String s) {
        super(s);
        this.s = s;
      }

      public String s;

      @Override
      public String origin() {
        return s;
      }

      public String m() {
        return "InnerClass.m";
      }

      public String superM() {
        return super.m();
      }

      public String superMFromOuterClass() {
        return EnclosingClass.super.m();
      }

      public String superDefaultMethodFromInnerClass() {
        return InterfaceWithDefault.super.m();
      }

      public String mFromOuterClass() {
        return EnclosingClass.this.m();
      }
    }
  }

  public static void main(String... args) {
    Main m = new Main(2);
    assertTrue(m.new A().fun() == 12);
    assertTrue(m.new A().enclosingInstance == m);

    Main mm = new Main(20);
    B b = mm.new B();
    B bb = mm.new B(1, m);
    assertTrue(b.fieldInB == 20);
    assertTrue(b.fieldMain == mm);
    assertTrue(bb.fieldInB == 1);
    assertTrue(bb.fieldMain == m);

    C c = m.new C();
    assertTrue(c.new CC().fieldOfC == c);
    assertTrue(c.new CC().fieldOfMain == m);

    assertTrue(m.new D().fieldInD == 2);
    assertTrue(m.new Child().fieldInChild == 4);
    assertTrue(m.new Child(10, 20).fieldInChild == 30);
    assertTrue(m.new AnotherChild().fieldInChild == 60);

    Child2 c2 = m.new Child2();
    assertTrue(c2.getChild2Outer().fieldInMain == 2);
    assertTrue(c2.getBOuter().fieldInMain == 30);

    assertTrue(m.new X().funInX(2) == 8);
    assertTrue(m.new C().new CC().test(8) == 130);
    assertTrue(m.new Y().new YY().test(8) == 132);
    assertTrue(m.new Z().new ZZ().test(8) == 244);

    assertTrue(m.new W().new W1().test(8) == 262);
    assertTrue(m.new W().new W2().test(8) == 603);
    assertTrue(m.new W().new W3().test(8) == 944);

    EnclosingClass outerClass = new EnclosingClass("Outer");
    EnclosingClass.InnerClass innerClass = outerClass.new InnerClass("Super");
    assertEquals(innerClass.m(), "InnerClass.m");
    assertEquals(innerClass.superM(), "EnclosingClass.m from Super");
    assertEquals(innerClass.superMFromOuterClass(), "SuperClass.m from Outer");
    assertEquals(
        innerClass.superDefaultMethodFromInnerClass(), "InterfaceWithDefault.m from Super");
    assertEquals(innerClass.mFromOuterClass(), "EnclosingClass.m from Outer");
  }
}
