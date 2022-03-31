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
package innerclassinitorder;

/**
 * Smoke test for inner classes, copied from GWT.
 */
public class InnerClassInitOrder {
  public int number = 0;

  static class OuterRefFromSuperCtorBase {
    OuterRefFromSuperCtorBase(Object o) {
      o.hashCode();
    }
  }

  class OuterRefFromSuperCtorCall extends OuterRefFromSuperCtorBase {
    OuterRefFromSuperCtorCall() {
      super(
          new Object() {
            @Override
            public int hashCode() {
              number += 100;
              return 0;
            }
          });
    }
  }

  class OuterRefFromThisCtorCall extends OuterRefFromSuperCtorBase {
    public OuterRefFromThisCtorCall(Object object) {
      super(object);
    }

    public OuterRefFromThisCtorCall() {
      this(
          new Object() {
            @Override
            public int hashCode() {
              number += 1000;
              return 0;
            }
          });
    }
  }

  public void testOuterThisFromSuperCall() {
    new OuterRefFromSuperCtorCall();
    assert (number == 100);
  }

  public void testOuterThisFromThisCall() {
    new OuterRefFromThisCtorCall();
    assert (number == 1100);
  }

  class InnerClass {
    {
      callInner();
    }

    void callInner() {
      number += 1;
      class ReallyInnerClass {
        {
          callReallyInner();
        }

        void callReallyInner() {
          number += 10;
        }
      }
      new ReallyInnerClass();
    }
  }

  static class P1<T1> {
    class P2<T2> extends P1<T1> {
      class P3<T3> extends P2<T2> {
        P3() {
          this(1);
        }

        P3(int i) {
          P2.this.super(i);
        }
      }

      P2() {
        this(1);
      }

      P2(int i) {
        super(i);
      }
    }

    final int value;

    P1() {
      this(1);
    }

    P1(int i) {
      value = i;
    }
  }


  /**
   * Used in test {@link #testExtendsNested()}
   */
  private static class ESOuter {
    class ESInner {
      public int value;

      public ESInner() {
        value = 1;
      }

      public ESInner(int value) {
        this.value = value;
      }
    }

    public ESInner newESInner() {
      return new ESInner();
    }
  }

  private static class ESInnerSubclass extends ESOuter.ESInner {
    ESInnerSubclass(ESOuter outer) {
      outer.super();
    }

    ESInnerSubclass(int value, ESOuter outer) {
      outer.super(value);
    }
  }

  /**
   * Used in test {@link #testExtendsNestedWithGenerics()}
   */
  private static class ESWGOuter<T> {
    class ESWGInner {
      public int value;

      public ESWGInner() {
        value = 1;
      }

      public ESWGInner(int value) {
        this.value = value;
      }
    }

    public ESWGInner newESWGInner() {
      return new ESWGInner();
    }
  }

  private static class ESWGInnerSubclass extends ESWGOuter<String>.ESWGInner {
    ESWGInnerSubclass(ESWGOuter<String> outer) {
      outer.super();
    }

    ESWGInnerSubclass(int value, ESWGOuter<String> outer) {
      outer.super(value);
    }
  }

  public void testExtendsNested() {
    ESOuter o = new ESOuter();
    assert (1 == o.new ESInner().value);
    assert (2 == o.new ESInner(2).value);
    assert (1 == new ESInnerSubclass(o).value);
    assert (2 == new ESInnerSubclass(2, o).value);
  }

  /**
   * Test for Issue 7789
   */
  public void testExtendsNestedWithGenerics() {
    ESWGOuter<String> o = new ESWGOuter<String>();
    assert (1 == o.new ESWGInner().value);
    assert (2 == o.new ESWGInner(2).value);
    assert (1 == new ESWGInnerSubclass(o).value);
    assert (2 == new ESWGInnerSubclass(2, o).value);
  }

  public void testInnerClassCtors() {
    P1<?> p1 = new P1<Object>();
    assert (1 == p1.value);
    assert (2 == new P1<Object>(2).value);
    P1<?>.P2<?> p2 = p1.new P2<Object>();
    assert (1 == p2.value);
    assert (2 == p1.new P2<Object>(2).value);
    assert (1 == p2.new P3<Object>().value);
    assert (2 == p2.new P3<Object>(2).value);
  }

  public void testInnerClassInitialization() {
    new InnerClass();
    assert (number == 1111);
  }

  public void testInnerClassLoop() {
    abstract class AddNumber {
      int num;

      public AddNumber(int i) {
        this.num = i;
      }

      public abstract void act();
    }
    AddNumber[] results = new AddNumber[10];
    for (int i = 0; i < 10; i++) {
      AddNumber ap = new AddNumber(i) {
        @Override
        public void act() {
          number += num;
        }
      };
      results[i] = ap;
    }
    for (AddNumber theAp : results) {
      theAp.act();
    }
    assert (number == 1156);
  }

  public static class Outer {
    public class OuterIsNotSuper {
      public int getValue() {
        return value;
      }
    }

    public class OuterIsSuper extends Outer {
      public OuterIsSuper(int i) {
        super(i);
      }

      @Override
      public int checkDispatch() {
        return 2;
      }

      public int checkDispatchFromSub1() {
        return super.checkDispatch();
      }

      public int checkDispatchFromSub2() {
        return new Outer(1) {
          public int go() {
            return OuterIsSuper.super.checkDispatch();
          }
        }.go();
      }

      public OuterIsNotSuper unqualifiedAlloc() {
        return new OuterIsNotSuper();
      }
    }

    public static class TestQualifiedSuperCall extends OuterIsNotSuper {
      public TestQualifiedSuperCall() {
        new Outer(1).new OuterIsSuper(2).super();
      }
    }

    public class TestUnqualifiedSuperCall extends OuterIsNotSuper {
      public TestUnqualifiedSuperCall() {
        super();
      }
    }

    protected final int value;

    public Outer(int i) {
      value = i;
    }

    public int checkDispatch() {
      return 1;
    }
  }

  private final Outer outer = new Outer(1);

  private final Outer.OuterIsSuper outerIsSuper = outer.new OuterIsSuper(2);

  public void testOuterIsNotSuper() {
    Outer.OuterIsNotSuper x = outerIsSuper.new OuterIsNotSuper();
    assert (2 == x.getValue());
  }

  // new an anonymous class of an inner class with a qualifier
  public void testOuterIsNotSuperAnon() {
    Outer.OuterIsNotSuper x = outerIsSuper.new OuterIsNotSuper() {
    };
    assert (2 == x.getValue());
  }

  public void testQualifiedSuperCall() {
    Outer.TestQualifiedSuperCall x = new Outer.TestQualifiedSuperCall();
    assert (2 == x.getValue());
  }

  public void testQualifiedSuperCallAnon() {
    Outer.TestQualifiedSuperCall x = new Outer.TestQualifiedSuperCall() {};
    assert (2 == x.getValue());
  }

  public void testSuperDispatch() {
    assert (1 == outerIsSuper.checkDispatchFromSub1());
    assert (1 == outerIsSuper.checkDispatchFromSub2());
  }

  public void testUnqualifiedAlloc() {
    Outer.OuterIsNotSuper x = outerIsSuper.unqualifiedAlloc();
    assert (2 == x.getValue());
  }

  public void testUnqualifiedSuperCall() {
    Outer.TestUnqualifiedSuperCall x = outerIsSuper.new TestUnqualifiedSuperCall();
    assert (2 == x.getValue());
  }

  // new an anonymous class of an inner class with a qualifier.
  public void testUnqualifiedSuperCallAnon() {
    Outer.TestUnqualifiedSuperCall x = outerIsSuper.new TestUnqualifiedSuperCall() {
    };
    assert (2 == x.getValue());
  }

  public static void main(String... args) {
    InnerClassInitOrder m = new InnerClassInitOrder();
    m.testOuterThisFromSuperCall();
    m.testOuterThisFromThisCall();

    m.testExtendsNested();
    m.testExtendsNestedWithGenerics();
    m.testInnerClassCtors();
    m.testInnerClassInitialization();
    m.testInnerClassLoop();

    m.testOuterIsNotSuper();
    m.testOuterIsNotSuperAnon();
    m.testQualifiedSuperCall();
    m.testQualifiedSuperCallAnon();
    m.testSuperDispatch();
    m.testUnqualifiedAlloc();
    m.testUnqualifiedSuperCall();
    m.testUnqualifiedSuperCallAnon();
  }
}
