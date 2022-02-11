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
package com.google.j2cl.ported.java8;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.google.j2cl.ported.java8.Java8Test.TestLambdaClass.TestLambdaInnerClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Java8Test {

  int local = 42;

  abstract static class SameClass {

    public int method1() {
      return 10;
    }

    public abstract int method2();
  }

  interface Lambda<T> {

    T run(int a, int b);
  }

  interface Lambda2<String> {

    boolean run(String a, String b);
  }

  interface Lambda3<String> {

    boolean run(String a);
  }

  static class AcceptsLambda<T> {

    public T accept(Lambda<T> foo) {
      return foo.run(10, 20);
    }

    public boolean accept2(Lambda2<String> foo) {
      return foo.run("a", "b");
    }

    public boolean accept3(Lambda3<String> foo) {
      return foo.run("hello");
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  class Pojo {

    private final int x;
    private final int y;

    public Pojo(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public int fooInstance(int a, int b) {
      return a + b + x + y;
    }
  }

  interface DefaultInterface {

    void method1();

    default int method2() {
      return 42;
    }

    default int redeclaredAsAbstract() {
      return 88;
    }

    default Integer addInts(int x, int y) {
      return x + y;
    }

    default String print() {
      return "DefaultInterface";
    }
  }

  interface DefaultInterface2 {

    void method3();

    default int method4() {
      return 23;
    }

    default int redeclaredAsAbstract() {
      return 77;
    }
  }

  interface DefaultInterfaceSubType extends DefaultInterface {

    @Override
    default int method2() {
      return 43;
    }

    @Override
    default String print() {
      return "DefaultInterfaceSubType " + DefaultInterface.super.print();
    }
  }

  abstract static class DualImplementorSuper implements DefaultInterface {

    @Override
    public void method1() {}

    @Override
    public abstract int redeclaredAsAbstract();
  }

  static class DualImplementorBoth extends VirtualUpRef
      implements DefaultInterface, DefaultInterface2 {

    @Override
    public void method1() {}

    @Override
    public void method3() {}
  }

  static class DualImplementor extends DualImplementorSuper implements DefaultInterface2 {

    @Override
    public void method3() {}

    @Override
    public int redeclaredAsAbstract() {
      return DefaultInterface2.super.redeclaredAsAbstract();
    }
  }

  // this doesn't implement DefaultInterface, but will provide implementation in subclasses
  static class VirtualUpRef {

    public int method2() {
      return 99;
    }

    public int redeclaredAsAbstract() {
      return 44;
    }
  }

  class Inner {

    int local = 22;

    public void run() {
      assertThat(
              (Object)
                  new AcceptsLambda<Integer>()
                      .accept((a, b) -> Java8Test.this.local + local + a + b)
                      .intValue())
          .isEqualTo(94);
    }
  }

  static class Static {

    static int staticField;

    static {
      staticField = 99;
    }

    static Integer staticMethod(int x, int y) {
      return x + y + staticField;
    }
  }

  static class ClinitCalled extends RuntimeException {

    static void throwException() {
      throw new ClinitCalled();
    }
  }

  static class StaticFailIfClinitRuns {

    static {
      ClinitCalled.throwException();
    }

    public static Integer staticMethod(int x, int y) {
      return null;
    }
  }

  static class DefaultInterfaceImpl implements DefaultInterface {

    @Override
    public void method1() {}
  }

  static class DefaultInterfaceImpl2 implements DefaultInterface {

    @Override
    public void method1() {}

    @Override
    public int method2() {
      return 100;
    }
  }

  static class DefaultInterfaceImplVirtualUpRef extends VirtualUpRef implements DefaultInterface {

    @Override
    public void method1() {}
  }

  static class DefaultInterfaceImplVirtualUpRefTwoInterfaces extends VirtualUpRef
      implements DefaultInterfaceSubType {

    @Override
    public void method1() {}

    @Override
    public String print() {
      return "DefaultInterfaceImplVirtualUpRefTwoInterfaces";
    }
  }

  @Test
  public void testLambdaNoCapture() {
    assertThat((Object) new AcceptsLambda<Integer>().accept((a, b) -> a + b).intValue())
        .isEqualTo(30);
  }

  @Test
  public void testLambdaCaptureLocal() {
    int x = 10;
    assertThat((Object) new AcceptsLambda<Integer>().accept((a, b) -> x + a + b).intValue())
        .isEqualTo(40);
  }

  @Test
  public void testLambdaCaptureLocalWithInnerClass() {
    int x = 10;
    Lambda<Integer> l =
        (a, b) ->
            new Lambda<Integer>() {
              @Override
              public Integer run(int a, int b) {
                int t = x;
                return t + a + b;
              }
            }.run(a, b);
    assertThat((Object) new AcceptsLambda<Integer>().accept(l).intValue()).isEqualTo(40);
  }

  @Test
  public void testLambdaCaptureLocalAndField() {
    int x = 10;
    assertThat((Object) new AcceptsLambda<Integer>().accept((a, b) -> x + local + a + b).intValue())
        .isEqualTo(82);
  }

  @Test
  public void testLambdaCaptureLocalAndFieldWithInnerClass() {
    int x = 10;
    Lambda<Integer> l =
        (a, b) ->
            new Lambda<Integer>() {
              @Override
              public Integer run(int j, int k) {
                int t = x;
                int s = local;
                return t + s + a + b;
              }
            }.run(a, b);
    assertThat((Object) new AcceptsLambda<Integer>().accept(l).intValue()).isEqualTo(82);
  }

  @Test
  public void testCompileLambdaCaptureOuterInnerField() throws Exception {
    new Inner().run();
  }

  @Test
  public void testStaticReferenceBinding() throws Exception {
    assertThat((Object) new AcceptsLambda<Integer>().accept(Static::staticMethod).intValue())
        .isEqualTo(129);
    // if this next line runs a clinit, it fails
    @SuppressWarnings("unchecked")
    Lambda l = dummyMethodToMakeCheckStyleHappy(StaticFailIfClinitRuns::staticMethod);
    try {
      // but now it should fail
      l.run(1, 2);
      fail("Clinit should have run for the first time");
    } catch (ClinitCalled expected) {
      // TODO(b/36781579): the code here should be catching ExceptionInInitializer and checking
      // the cause, but J2CL does not wrap the exception.
      // success, it was supposed to throw!
    }
  }

  private static Lambda<Integer> dummyMethodToMakeCheckStyleHappy(Lambda<Integer> l) {
    return l;
  }

  @Test
  public void testInstanceReferenceBinding() throws Exception {
    Pojo instance1 = new Pojo(1, 2);
    Pojo instance2 = new Pojo(3, 4);
    assertThat((Object) new AcceptsLambda<Integer>().accept(instance1::fooInstance).intValue())
        .isEqualTo(33);
    assertThat((Object) new AcceptsLambda<Integer>().accept(instance2::fooInstance).intValue())
        .isEqualTo(37);
  }

  @Test
  public void testImplicitQualifierReferenceBinding() throws Exception {
    assertThat(new AcceptsLambda<String>().accept2(String::equalsIgnoreCase)).isFalse();
    assertThat(new AcceptsLambda<String>().accept3("hello world"::contains)).isTrue();
  }

  @Test
  public void testConstructorReferenceBinding() {
    assertThat((Object) new AcceptsLambda<Pojo>().accept(Pojo::new).fooInstance(0, 0))
        .isEqualTo(30);
  }

  @Test
  public void testStaticInterfaceMethod() {
    assertThat((Object) (int) Static.staticMethod(0, 0)).isEqualTo(99);
  }

  interface ArrayProducer {

    Element[][][] create(int i);
  }

  interface ArrayProduceBoxedParameter {

    Element[][][] create(Integer i);
  }

  static class Element {}

  @Test
  public void testArrayConstructorReference() {
    ArrayProducer ctor = Element[][][]::new;
    Element[][][] array = ctor.create(100);
    assertThat((Object) array.length).isEqualTo(100);
  }

  @Test
  public void testArrayConstructorReferenceBoxed() {
    ArrayProduceBoxedParameter ctor = Element[][][]::new;
    Element[][][] array = ctor.create(100);
    assertThat((Object) array.length).isEqualTo(100);
  }

  interface ThreeArgs {
    int foo(int x, int y, int z);
  }

  interface ThreeVarArgs {
    int foo(int x, int y, int... z);
  }

  public static int addMany(int x, int y, int... nums) {
    int sum = x + y;
    for (int num : nums) {
      sum += num;
    }
    return sum;
  }

  @Test
  public void testVarArgsReferenceBinding() {
    ThreeArgs t = Java8Test::addMany;
    assertThat((Object) t.foo(1, 2, 3)).isEqualTo(6);
  }

  @Test
  public void testVarArgsPassthroughReferenceBinding() {
    ThreeVarArgs t = Java8Test::addMany;
    assertThat((Object) t.foo(1, 2, 3)).isEqualTo(6);
  }

  @Test
  public void testVarArgsPassthroughReferenceBindingProvidedArray() {
    ThreeVarArgs t = Java8Test::addMany;
    assertThat((Object) t.foo(1, 2, new int[] {3})).isEqualTo(6);
  }

  interface I {

    int foo(Integer i);
  }

  @Test
  public void testSuperReferenceExpression() {
    class Y {

      int foo(Integer i) {
        return 42;
      }
    }

    class X extends Y {

      @Override
      int foo(Integer i) {
        return 23;
      }

      int goo() {
        I i = super::foo;
        return i.foo(0);
      }
    }

    assertThat((Object) new X().goo()).isEqualTo(42);
  }

  static class X2 {

    protected int field;

    void foo() {
      int local;
      class Y extends X2 {

        class Z extends X2 {

          void f() {
            Ctor c = X2::new;
            X2 x = c.makeX(123456);
            assertThat((Object) x.field).isEqualTo(123456);
            c = Y::new;
            x = c.makeX(987654);
            x = new Y(987654);
            assertThat((Object) x.field).isEqualTo(987655);
            c = Z::new;
            x = c.makeX(456789);
            x = new Z(456789);
            assertThat((Object) x.field).isEqualTo(456791);
          }

          private Z(int z) {
            super(z + 2);
          }

          Z() {}
        }

        private Y(int y) {
          super(y + 1);
        }

        private Y() {}
      }
      new Y().new Z().f();
    }

    private X2(int x) {
      this.field = x;
    }

    X2() {}
  }

  @Test
  public void testSuperReferenceExpressionWithVarArgs() {
    class Base {

      int foo(Object... objects) {
        return 0;
      }
    }

    class X extends Base {

      @Override
      int foo(Object... objects) {
        throw new AssertionError();
      }

      void goo() {
        I i = super::foo;
        i.foo(10);
      }
    }
    new X().goo();
  }

  interface Ctor {

    X2 makeX(int x);
  }

  @Test
  public void testPrivateConstructorReference() {
    new X2().foo();
  }

  @Test
  public void testDefaultInterfaceMethod() {
    assertThat((Object) new DefaultInterfaceImpl().method2()).isEqualTo(42);
  }

  @Test
  public void testDefaultInterfaceMethodVirtualUpRef() {
    assertThat((Object) new DefaultInterfaceImplVirtualUpRef().method2()).isEqualTo(99);
    assertThat((Object) new DefaultInterfaceImplVirtualUpRefTwoInterfaces().method2())
        .isEqualTo(99);
    assertThat((Object) new com.google.j2cl.ported.java8.package3.SimpleC().m())
        .isEqualTo("SimpleB");
    assertThat((Object) new com.google.j2cl.ported.java8.package1.SimpleD().m())
        .isEqualTo("SimpleASimpleB");
  }

  @Test
  public void testDefaultInterfaceMethodMultiple() {
    assertThat((Object) new DualImplementor().method2()).isEqualTo(42);
    assertThat((Object) new DualImplementor().method4()).isEqualTo(23);
    assertThat((Object) new DualImplementor().redeclaredAsAbstract()).isEqualTo(77);
    assertThat((Object) new DualImplementorBoth().redeclaredAsAbstract()).isEqualTo(44);
    DefaultInterfaceImplVirtualUpRefTwoInterfaces instanceImplementInterfaceSubType =
        new DefaultInterfaceImplVirtualUpRefTwoInterfaces();
    DefaultInterfaceSubType interfaceSubType1 = instanceImplementInterfaceSubType;
    assertThat((Object) instanceImplementInterfaceSubType.print())
        .isEqualTo("DefaultInterfaceImplVirtualUpRefTwoInterfaces");
    assertThat((Object) interfaceSubType1.print())
        .isEqualTo("DefaultInterfaceImplVirtualUpRefTwoInterfaces");
    DefaultInterfaceSubType interfaceSubType2 =
        new DefaultInterfaceSubType() {
          @Override
          public void method1() {}
        };
    assertThat((Object) interfaceSubType2.print())
        .isEqualTo("DefaultInterfaceSubType DefaultInterface");
    DefaultInterfaceSubType interfaceSubType3 = () -> {};
    assertThat((Object) interfaceSubType3.print())
        .isEqualTo("DefaultInterfaceSubType DefaultInterface");
  }

  @Test
  public void testDefenderMethodByInterfaceInstance() {
    DefaultInterfaceImpl2 interfaceImpl2 = new DefaultInterfaceImpl2();
    DefaultInterface interface1 = interfaceImpl2;
    assertThat((Object) interfaceImpl2.method2()).isEqualTo(100);
    assertThat((Object) interface1.method2()).isEqualTo(100);
  }

  @Test
  public void testDefaultMethodReference() {
    DefaultInterfaceImplVirtualUpRef x = new DefaultInterfaceImplVirtualUpRef();
    assertThat((Object) (int) new AcceptsLambda<Integer>().accept(x::addInts)).isEqualTo(30);
  }

  interface InterfaceWithTwoDefenderMethods {

    default String foo() {
      return "interface.foo";
    }

    default String bar() {
      return this.foo() + " " + foo();
    }
  }

  static class ClassImplementOneDefenderMethod implements InterfaceWithTwoDefenderMethods {

    @Override
    public String foo() {
      return "class.foo";
    }
  }

  @Test
  public void testThisRefInDefenderMethod() {
    ClassImplementOneDefenderMethod c = new ClassImplementOneDefenderMethod();
    InterfaceWithTwoDefenderMethods i1 = c;
    InterfaceWithTwoDefenderMethods i2 = new InterfaceWithTwoDefenderMethods() {};
    assertThat((Object) c.bar()).isEqualTo("class.foo class.foo");
    assertThat((Object) i1.bar()).isEqualTo("class.foo class.foo");
    assertThat((Object) i2.bar()).isEqualTo("interface.foo interface.foo");
  }

  interface InterfaceImplementOneDefenderMethod extends InterfaceWithTwoDefenderMethods {

    @Override
    default String foo() {
      return "interface1.foo";
    }
  }

  interface InterfaceImplementZeroDefenderMethod extends InterfaceWithTwoDefenderMethods {}

  static class ClassImplementsTwoInterfaces
      implements InterfaceImplementOneDefenderMethod, InterfaceImplementZeroDefenderMethod {}

  @Test
  public void testClassImplementsTwoInterfacesWithSameDefenderMethod() {
    ClassImplementsTwoInterfaces c = new ClassImplementsTwoInterfaces();
    assertThat((Object) c.foo()).isEqualTo("interface1.foo");
  }

  abstract static class AbstractClass implements InterfaceWithTwoDefenderMethods {}

  static class Child1 extends AbstractClass {

    @Override
    public String foo() {
      return super.foo() + " child1.foo";
    }
  }

  static class Child2 extends AbstractClass {}

  @Test
  public void testAbstractClassImplementsInterface() {
    Child1 child1 = new Child1();
    Child2 child2 = new Child2();
    assertThat((Object) child1.foo()).isEqualTo("interface.foo child1.foo");
    assertThat((Object) child2.foo()).isEqualTo("interface.foo");
  }

  interface InterfaceI {

    default String print() {
      return "interface1";
    }
  }

  interface InterfaceII {

    default String print() {
      return "interface2";
    }
  }

  static class ClassI {

    public String print() {
      return "class1";
    }
  }

  static class ClassII extends ClassI implements InterfaceI, InterfaceII {

    @Override
    public String print() {
      return super.print() + " " + InterfaceI.super.print() + " " + InterfaceII.super.print();
    }
  }

  @Test
  public void testSuperRefInDefenderMethod() {
    ClassII c = new ClassII();
    assertThat((Object) c.print()).isEqualTo("class1 interface1 interface2");
  }

  interface II {

    default String fun() {
      return "fun() in i: " + this.foo();
    }

    default String foo() {
      return "foo() in i.\n";
    }
  }

  interface JJ extends II {

    @Override
    default String fun() {
      return "fun() in j: " + this.foo() + II.super.fun();
    }

    @Override
    default String foo() {
      return "foo() in j.\n";
    }
  }

  static class AA {

    public String fun() {
      return "fun() in a: " + this.foo();
    }

    public String foo() {
      return "foo() in a.\n";
    }
  }

  static class BB extends AA implements JJ {

    @Override
    public String fun() {
      return "fun() in b: " + this.foo() + super.fun() + JJ.super.fun();
    }

    @Override
    public String foo() {
      return "foo() in b.\n";
    }
  }

  static class CC extends BB implements JJ {

    @Override
    public String fun() {
      return "fun() in c: " + super.fun();
    }
  }

  @Test
  public void testSuperThisRefsInDefenderMethod() {
    CC c = new CC();
    II i1 = c;
    JJ j1 = c;
    BB b = new BB();
    II i2 = b;
    JJ j2 = b;
    JJ j3 = new JJ() {};
    II i3 = j3;
    II i4 = new II() {};
    String cFun =
        "fun() in c: fun() in b: foo() in b.\n"
            + "fun() in a: foo() in b.\n"
            + "fun() in j: foo() in b.\n"
            + "fun() in i: foo() in b.\n";
    String bFun =
        "fun() in b: foo() in b.\n"
            + "fun() in a: foo() in b.\n"
            + "fun() in j: foo() in b.\n"
            + "fun() in i: foo() in b.\n";
    String jFun = "fun() in j: foo() in j.\n" + "fun() in i: foo() in j.\n";
    String iFun = "fun() in i: foo() in i.\n";
    assertThat((Object) c.fun()).isEqualTo(cFun);
    assertThat((Object) i1.fun()).isEqualTo(cFun);
    assertThat((Object) j1.fun()).isEqualTo(cFun);
    assertThat((Object) b.fun()).isEqualTo(bFun);
    assertThat((Object) i2.fun()).isEqualTo(bFun);
    assertThat((Object) j2.fun()).isEqualTo(bFun);
    assertThat((Object) j3.fun()).isEqualTo(jFun);
    assertThat((Object) i3.fun()).isEqualTo(jFun);
    assertThat((Object) i4.fun()).isEqualTo(iFun);
  }

  interface OuterInterface {

    default String m() {
      return "I.m;" + new InnerClass().n();
    }

    default String n() {
      return "I.n;" + this.m();
    }

    class InnerClass {

      public String n() {
        return "A.n;" + m();
      }

      public String m() {
        return "A.m;";
      }
    }
  }

  class OuterClass {

    public String m() {
      return "B.m;";
    }

    public String n1() {
      OuterInterface i = new OuterInterface() {};
      return "B.n1;" + i.n() + OuterClass.this.m();
    }

    public String n2() {
      OuterInterface i =
          new OuterInterface() {
            @Override
            public String n() {
              return this.m() + OuterClass.this.m();
            }
          };
      return "B.n2;" + i.n() + OuterClass.this.m();
    }
  }

  @Test
  public void testNestedInterfaceClass() {
    OuterClass outerClass = new OuterClass();
    assertThat((Object) outerClass.n1()).isEqualTo("B.n1;I.n;I.m;A.n;A.m;B.m;");
    assertThat((Object) outerClass.n2()).isEqualTo("B.n2;I.m;A.n;A.m;B.m;B.m;");
  }

  static class EmptyA {}

  interface EmptyI {}

  interface EmptyJ {}

  static class EmptyB extends EmptyA implements EmptyI {}

  static class EmptyC extends EmptyA implements EmptyI, EmptyJ {}

  @Test
  public void testBaseIntersectionCast() {
    EmptyA localB = new EmptyB();
    EmptyA localC = new EmptyC();
    EmptyB b2BI = (EmptyB & EmptyI) localB;
    EmptyC c2CIJ = (EmptyC & EmptyI & EmptyJ) localC;
    EmptyI ii1 = (EmptyB & EmptyI) localB;
    EmptyI ii2 = (EmptyC & EmptyI) localC;
    EmptyI ii3 = (EmptyC & EmptyJ) localC;
    EmptyI ii4 = (EmptyC & EmptyI & EmptyJ) localC;
    EmptyJ jj1 = (EmptyC & EmptyI & EmptyJ) localC;
    EmptyJ jj2 = (EmptyC & EmptyI) localC;
    EmptyJ jj3 = (EmptyC & EmptyJ) localC;
    EmptyJ jj4 = (EmptyI & EmptyJ) localC;

    assertThrows(
        ClassCastException.class,
        () -> {
          EmptyC unused = (EmptyC & EmptyI & EmptyJ) localB;
        });
    assertThrows(
        ClassCastException.class,
        () -> {
          EmptyB unused = (EmptyB & EmptyI) localC;
        });
    assertThrows(
        ClassCastException.class,
        () -> {
          EmptyJ unused = (EmptyB & EmptyJ) localB;
        });
  }

  interface SimpleI {

    int fun();
  }

  interface SimpleK {}

  @Test
  public void testIntersectionCastWithLambdaExpr() {
    SimpleI simpleI1 = (SimpleI & EmptyI) () -> 11;
    assertThat((Object) simpleI1.fun()).isEqualTo(11);
    SimpleI simpleI2 = (EmptyI & SimpleI) () -> 22;
    assertThat((Object) simpleI2.fun()).isEqualTo(22);
    EmptyI emptyI = (EmptyI & SimpleI) () -> 33;
    assertThat((Object) ((SimpleI & SimpleK) () -> 55).fun()).isEqualTo(55);
  }

  static class SimpleA {

    public int bar() {
      return 11;
    }
  }

  static class SimpleB extends SimpleA implements SimpleI {

    @Override
    public int fun() {
      return 22;
    }
  }

  static class SimpleC extends SimpleA implements SimpleI {

    @Override
    public int fun() {
      return 33;
    }

    @Override
    public int bar() {
      return 44;
    }
  }

  @Test
  public void testIntersectionCastPolymorphism() {
    SimpleA bb = new SimpleB();
    assertThat((Object) ((SimpleB & SimpleI) bb).fun()).isEqualTo(22);
    assertThat(((SimpleB & SimpleI) bb).bar()).isEqualTo(11);
    SimpleA cc = new SimpleC();
    assertThat((Object) ((SimpleC & SimpleI) cc).fun()).isEqualTo(33);
    assertThat(((SimpleC & SimpleI) cc).bar()).isEqualTo(44);
    assertThat((Object) ((SimpleA & SimpleI) cc).fun()).isEqualTo(33);
    SimpleI ii = (SimpleC & SimpleI) cc;
    assertThat((Object) ii.fun()).isEqualTo(33);
  }

  interface ClickHandler {

    int onClick(int a);
  }

  private static int addClickHandler(ClickHandler clickHandler) {
    return clickHandler.onClick(1);
  }

  private int addClickHandler(int a) {
    return addClickHandler(
        x -> {
          int temp = a;
          return temp;
        });
  }

  @Test
  public void testLambdaCaptureParameter() {
    assertThat((Object) addClickHandler(2)).isEqualTo(2);
  }

  interface TestLambdaInner {

    void f();
  }

  interface TestLambdaOuter {

    void accept(TestLambdaInner t);
  }

  public void testLambda_call(TestLambdaOuter a) {
    a.accept(() -> {});
  }

  @Test
  public void testLambdaNestingCaptureLocal() {
    int[] success = new int[] {0};
    testLambda_call(
        sam1 -> {
          testLambda_call(
              sam2 -> {
                success[0] = 10;
              });
        });
    assertThat((Object) success[0]).isEqualTo(10);
  }

  @Test
  public void testLambdaNestingInAnonymousCaptureLocal() {
    int[] x = new int[] {42};
    new Runnable() {
      @Override
      public void run() {
        Lambda<Integer> l = (a, b) -> x[0] = x[0] + a + b;
        l.run(1, 2);
      }
    }.run();
    assertThat((Object) x[0]).isEqualTo(45);
  }

  @Test
  public void testLambdaNestingInMultipleMixedAnonymousCaptureLocal() {
    // checks that lambda has access to local variable and arguments when placed in mixed scopes
    // Local Class -> Local Class -> Local Anonymous -> lambda -> Local Anonymous
    class A {

      int a() {
        int[] x = new int[] {42};
        class B {

          void b() {
            I i =
                new I() {
                  @Override
                  public int foo(Integer arg) {
                    Runnable r =
                        () -> {
                          new Runnable() {
                            @Override
                            public void run() {
                              Lambda<Integer> l = (a, b) -> x[0] = x[0] + a + b + arg;
                              l.run(1, 2);
                            }
                          }.run();
                        };
                    r.run();
                    return x[0];
                  }
                };
            i.foo(1);
          }
        }
        B b = new B();
        b.b();
        return x[0];
      }
    }
    A a = new A();
    assertThat((Object) a.a()).isEqualTo(46);
  }

  @Test
  public void testLambdaNestingInMultipleMixedAnonymousCaptureLocal_withInterference() {
    // checks that lambda has access to NEAREST local variable and arguments when placed in mixed
    // scopes Local Class -> Local Class -> Local Anonymous -> lambda -> Local Anonymous
    class A {

      int a() {
        int[] x = new int[] {42};
        class B {

          int b() {
            int[] x = new int[] {22};
            I i =
                new I() {
                  @Override
                  public int foo(Integer arg) {
                    Runnable r =
                        () -> {
                          new Runnable() {
                            @Override
                            public void run() {
                              Lambda<Integer> l = (a, b) -> x[0] = x[0] + a + b + arg;
                              l.run(1, 2);
                            }
                          }.run();
                        };
                    r.run();
                    return x[0];
                  }
                };
            return i.foo(1);
          }
        }
        B b = new B();
        return b.b();
      }
    }
    A a = new A();
    assertThat((Object) a.a()).isEqualTo(26);
  }

  @Test
  public void testLambdaNestingInMultipleMixedAnonymousCaptureLocalAndField() {
    // checks that lambda has access to local variable, field and arguments when placed in mixed
    // scopes - Local Class -> Local Class -> Local Anonymous -> lambda -> Local Anonymous
    class A {

      int fA = 1;

      int a() {
        int[] x = new int[] {42};
        class B {

          int fB = 2;

          int b() {
            I i =
                new I() {
                  int fI = 3;

                  @Override
                  public int foo(Integer arg) {
                    Runnable r =
                        () -> {
                          new Runnable() {
                            @Override
                            public void run() {
                              Lambda<Integer> l =
                                  (a, b) -> x[0] = x[0] + a + b + arg + fA + fB + fI;
                              l.run(1, 2);
                            }
                          }.run();
                        };
                    r.run();
                    return x[0];
                  }
                };
            return i.foo(1);
          }
        }
        B b = new B();
        return b.b();
      }
    }
    A a = new A();
    assertThat((Object) a.a()).isEqualTo(52);
  }

  @Test
  public void testLambdaNestingInMultipleAnonymousCaptureLocal() {
    // checks that lambda has access to local variable and arguments when placed in local anonymous
    // class with multile nesting
    int[] x = new int[] {42};
    int result =
        new I() {
          @Override
          public int foo(Integer i1) {
            return new I() {
              @Override
              public int foo(Integer i2) {
                return new I() {
                  @Override
                  public int foo(Integer i3) {
                    Lambda<Integer> l = (a, b) -> x[0] = x[0] + a + b + i1 + i2 + i3;
                    return l.run(1, 2);
                  }
                }.foo(3);
              }
            }.foo(2);
          }
        }.foo(1);
    assertThat((Object) x[0]).isEqualTo(51);
  }

  static class TestLambdaClassA {

    int[] f = new int[] {42};

    class B {

      void m() {
        Runnable r = () -> f[0] = f[0] + 1;
        r.run();
      }
    }

    int a() {
      B b = new B();
      b.m();
      return f[0];
    }
  }

  @Test
  public void testLambdaNestingCaptureField_InnerClassCapturingOuterClassVariable() {
    TestLambdaClassA a = new TestLambdaClassA();
    assertThat((Object) a.a()).isEqualTo(43);
  }

  @Test
  public void testInnerClassCaptureLocalFromOuterLambda() {
    int[] x = new int[] {42};
    Lambda<Integer> l =
        (a, b) -> {
          int[] x1 = new int[] {32};
          Lambda<Integer> r =
              (rA, rB) -> {
                int[] x2 = new int[] {22};
                I i =
                    new I() {
                      @Override
                      public int foo(Integer arg) {
                        x1[0] = x1[0] + 1;
                        x[0] = x[0] + 1;
                        return x2[0] = x2[0] + rA + rB + a + b;
                      }
                    };
                return i.foo(1);
              };
          return r.run(3, 4) + x1[0];
        };

    // x1[0](32) + 1 + x2[0](22) + rA(3) + rB(4) + a(1) + b(2)
    assertThat((Object) l.run(1, 2).intValue()).isEqualTo(65);
    assertThat((Object) x[0]).isEqualTo(43);
  }

  static class TestLambdaClass {

    public int[] s = new int[] {0};

    public void call(TestLambdaOuter a) {
      a.accept(() -> {});
    }

    class TestLambdaInnerClass {

      public int[] s = new int[] {0};

      public int test() {
        int[] s = new int[] {0};
        TestLambdaClass.this.call(
            sam0 ->
                TestLambdaClass.this.call(
                    sam1 -> {
                      TestLambdaClass.this.call(
                          sam2 -> {
                            TestLambdaClass.this.s[0] = 10;
                            this.s[0] = 20;
                            s[0] = 30;
                          });
                    }));
        return s[0];
      }
    }
  }

  @Test
  public void testLambdaNestingCaptureField() {
    TestLambdaClass a = new TestLambdaClass();
    a.call(
        sam1 -> {
          a.call(
              sam2 -> {
                a.s[0] = 20;
              });
        });
    assertThat((Object) a.s[0]).isEqualTo(20);
  }

  @Test
  public void testLambdaMultipleNestingCaptureFieldAndLocal() {
    TestLambdaClass a = new TestLambdaClass();
    TestLambdaClass b = new TestLambdaClass();
    int[] s = new int[] {0};
    b.call(
        sam0 ->
            a.call(
                sam1 -> {
                  a.call(
                      sam2 -> {
                        a.s[0] = 20;
                        b.s[0] = 30;
                        s[0] = 40;
                      });
                }));
    assertThat((Object) a.s[0]).isEqualTo(20);
    assertThat((Object) b.s[0]).isEqualTo(30);
    assertThat((Object) s[0]).isEqualTo(40);
  }

  @Test
  public void testLambdaMultipleNestingCaptureFieldAndLocalInnerClass() {
    TestLambdaClass a = new TestLambdaClass();
    TestLambdaInnerClass b = a.new TestLambdaInnerClass();
    int result = b.test();
    assertThat((Object) a.s[0]).isEqualTo(10);
    assertThat((Object) b.s[0]).isEqualTo(20);
    assertThat((Object) result).isEqualTo(30);
  }

  static class TestMFA {

    public static String getId() {
      return "A";
    }

    public int getIdx() {
      return 1;
    }
  }

  static class TestMFB {

    public static String getId() {
      return "B";
    }

    public int getIdx() {
      return 2;
    }
  }

  interface Function<T> {

    T apply();
  }

  private static String f(Function<String> arg) {
    return arg.apply();
  }

  private static int g(Function<Integer> arg) {
    return arg.apply().intValue();
  }

  @Test
  public void testMethodRefWithSameName() {
    assertThat((Object) f(TestMFA::getId)).isEqualTo("A");
    assertThat((Object) f(TestMFB::getId)).isEqualTo("B");
    TestMFA a = new TestMFA();
    TestMFB b = new TestMFB();
    assertThat((Object) g(a::getIdx)).isEqualTo(1);
    assertThat((Object) g(b::getIdx)).isEqualTo(2);
  }

  // Test particular scenarios involving multiple path to inherit defaults.
  interface ITop {

    default String m() {
      return "ITop.m()";
    }
  }

  interface IRight extends ITop {

    @Override
    default String m() {
      return "IRight.m()";
    }
  }

  interface ILeft extends ITop {}

  @Test
  public void testMultipleDefaults_fromInterfaces_left() {
    class A implements ILeft, IRight {}

    assertThat((Object) new A().m()).isEqualTo("IRight.m()");
  }

  @Test
  public void testMultipleDefaults_fromInterfaces_right() {
    class A implements IRight, ILeft {}

    assertThat((Object) new A().m()).isEqualTo("IRight.m()");
  }

  @Test
  public void testMultipleDefaults_superclass_left() {
    class A implements ITop {}

    class B extends A implements ILeft, IRight {}

    assertThat((Object) new B().m()).isEqualTo("IRight.m()");
  }

  @Test
  public void testMultipleDefaults_superclass_right() {
    class A implements ITop {}

    class B extends A implements IRight, ILeft {}

    assertThat((Object) new B().m()).isEqualTo("IRight.m()");
  }

  static class DefaultTrumpsOverSyntheticAbstractStub {

    interface SuperInterface {

      String m();
    }

    interface SubInterface extends SuperInterface {

      @Override
      default String m() {
        return "SubInterface.m()";
      }
    }
  }

  @Test
  public void testMultipleDefaults_defaultShadowsOverSyntheticAbstractStub() {
    abstract class A implements DefaultTrumpsOverSyntheticAbstractStub.SuperInterface {}

    class B extends A implements DefaultTrumpsOverSyntheticAbstractStub.SubInterface {}

    assertThat((Object) new B().m()).isEqualTo("SubInterface.m()");
  }

  static class DefaultTrumpsOverDefaultOnSuperAbstract {

    interface SuperInterface {

      default String m() {
        return "SuperInterface.m()";
      }
    }

    interface SubInterface extends SuperInterface {

      @Override
      default String m() {
        return "SubInterface.m()";
      }
    }
  }

  @Test
  public void testMultipleDefaults_defaultShadowsOverDefaultOnSuperAbstract() {
    abstract class A implements DefaultTrumpsOverDefaultOnSuperAbstract.SuperInterface {}

    class B extends A implements DefaultTrumpsOverDefaultOnSuperAbstract.SubInterface {}

    assertThat((Object) new B().m()).isEqualTo("SubInterface.m()");
  }

  interface InterfaceWithThisReference {

    default String n() {
      return "default n";
    }

    default String callNUnqualified() {
      class Super implements InterfaceWithThisReference {

        @Override
        public String n() {
          return "super n";
        }
      }
      return new Super() {
        @Override
        public String callNUnqualified() {
          return "Object " + n();
        }
      }.callNUnqualified();
    }

    default String callNWithThis() {
      class Super implements InterfaceWithThisReference {

        @Override
        public String n() {
          return "super n";
        }
      }
      return new Super() {
        @Override
        public String callNWithThis() {
          return "Object " + this.n();
        }
      }.callNWithThis();
    }

    default String callNWithInterfaceThis() {
      class Super implements InterfaceWithThisReference {

        @Override
        public String n() {
          return "super n";
        }
      }
      return new Super() {
        @Override
        public String callNWithInterfaceThis() {
          // In this method this has interface Test as its type, but it refers to outer n();
          return "Object " + InterfaceWithThisReference.this.n();
        }
      }.callNWithInterfaceThis();
    }

    default String callNWithSuper() {
      class Super implements InterfaceWithThisReference {

        @Override
        public String n() {
          return "super n";
        }
      }
      return new Super() {
        @Override
        public String callNWithSuper() {
          // In this method this has interface Test as its type.
          return "Object " + super.n();
        }
      }.callNWithSuper();
    }

    default String callNWithInterfaceSuper() {
      return new InterfaceWithThisReference() {
        @Override
        public String n() {
          return "this n";
        }

        @Override
        public String callNWithInterfaceSuper() {
          // In this method this has interface Test as its type and refers to default n();
          return "Object " + InterfaceWithThisReference.super.n();
        }
      }.callNWithInterfaceSuper();
    }
  }

  @Test
  public void testInterfaceThis() {
    class A implements InterfaceWithThisReference {

      @Override
      public String n() {
        return "n";
      }
    }
    assertThat((Object) new A().callNUnqualified()).isEqualTo("Object super n");
    assertThat((Object) new A().callNWithThis()).isEqualTo("Object super n");
    assertThat((Object) new A().callNWithInterfaceThis()).isEqualTo("Object n");
    assertThat((Object) new A().callNWithSuper()).isEqualTo("Object super n");
    assertThat((Object) new A().callNWithInterfaceSuper()).isEqualTo("Object default n");
  }

  private static List<String> initializationOrder;

  private static int get(String s) {
    initializationOrder.add(s);
    return 1;
  }

  interface A1 {

    int FA1 = get("A1");

    default void a1() {}
  }

  interface A2 {

    int FA2 = get("A2");

    default void a2() {}
  }

  interface A3 {

    int FA3 = get("A3");

    default void a3() {}
  }

  interface B1 extends A1 {

    int FB1 = get("B1");

    default void b1() {}
  }

  interface B2 extends A2 {

    int FB2 = get("B2");

    default void b2() {}
  }

  interface B3 extends A3 {

    int FB3 = get("B3");
  }

  static class C implements B1, A2 {

    static {
      get("C");
    }
  }

  static class D extends C implements B2, B3 {

    static {
      get("D");
    }
  }

  @Test
  public void testInterfaceWithDefaultMethodsInitialization() {
    initializationOrder = new ArrayList<String>();
    new D();
    assertContentsInOrder(initializationOrder, "A1", "B1", "A2", "C", "B2", "A3", "D");
  }

  /** Regression test for issue 9214. */
  interface P<T> {

    boolean apply(T obj);
  }

  static class B {

    public boolean getTrue() {
      return true;
    }
  }

  private static <T> String getClassName(T obj) {
    return obj.getClass().getSimpleName();
  }

  @Test
  @SuppressWarnings("BoxedPrimitiveConstructor")
  public void testMethodReference_generics() {
    P<B> p = B::getTrue;
    assertThat(p.apply(new B())).isTrue();
    // The next two method references must result in two different lambda implementations due
    // to generics, see bug # 9333.
    MyFunction1<B, String> f1 = Java8Test::getClassName;
    MyFunction1<Double, String> f2 = Java8Test::getClassName;

    assertThat((Object) f1.apply(new B())).isEqualTo(B.class.getSimpleName());
    assertThat((Object) f2.apply(new Double(2))).isEqualTo(Double.class.getSimpleName());
  }

  public interface WithDefaultMethodAndStaticInitializer {

    SomeClass someClass = new SomeClass("1");
    SomeClass someClass2 = new SomeClass("2");

    default SomeClass getGetSomeClass() {
      return someClass;
    }
  }

  public static class ImplementsWithDefaultMethodAndStaticInitializer
      implements WithDefaultMethodAndStaticInitializer {

    static {
      SomeClass.initializationOrder.add("3");
    }

    public static SomeClass someClass = new SomeClass("4");
  }

  public static class SomeClass {

    public static List<String> initializationOrder;

    SomeClass(String s) {
      initializationOrder.add(s);
    }
  }

  @Test
  public void testDefaultMethod_staticInitializer() {
    SomeClass.initializationOrder = new ArrayList<String>();
    Object object = ImplementsWithDefaultMethodAndStaticInitializer.someClass;
    assertContentsInOrder(SomeClass.initializationOrder, "1", "2", "3", "4");
  }

  private static void assertContentsInOrder(Iterable<String> contents, String... elements) {
    assertThat((Object) contents.toString()).isEqualTo(Arrays.asList(elements).toString());
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface NativeJsTypeInterfaceWithStaticInitializationAndFieldAccess {

    @SuppressWarnings("BoxedPrimitiveConstructor")
    @JsOverlay
    Object object = new Integer(3);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface NativeJsTypeInterfaceWithStaticInitializationAndStaticOverlayMethod {

    @SuppressWarnings("BoxedPrimitiveConstructor")
    @JsOverlay
    Object object = new Integer(4);

    @JsOverlay
    static Object getObject() {
      return object;
    }
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface NativeJsTypeInterfaceWithStaticInitializationAndInstanceOverlayMethod {

    @SuppressWarnings("BoxedPrimitiveConstructor")
    @JsOverlay
    Object object = new Integer(5);

    int getA();

    @JsOverlay
    default Object getObject() {
      return ((int) object) + this.getA();
    }
  }

  @JsMethod
  private static native NativeJsTypeInterfaceWithStaticInitializationAndInstanceOverlayMethod
      createNativeJsTypeInterfaceWithStaticInitializationAndInstanceOverlayMethod();

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface NativeJsTypeInterfaceWithStaticInitialization {

    @SuppressWarnings("BoxedPrimitiveConstructor")
    @JsOverlay
    Object object = new Integer(6);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface NativeJsTypeInterfaceWithComplexStaticInitialization {

    @JsOverlay
    Object object = (Integer) (((int) NativeJsTypeInterfaceWithStaticInitialization.object) + 1);
  }

  static class JavaTypeImplementingNativeJsTypeInterceWithDefaultMethod
      implements NativeJsTypeInterfaceWithStaticInitializationAndInstanceOverlayMethod {

    @Override
    public int getA() {
      return 4;
    }
  }

  @Test
  public void testNativeJsTypeWithStaticInitializer() {
    assertThat(NativeJsTypeInterfaceWithStaticInitializationAndFieldAccess.object).isEqualTo(3);
    assertThat(NativeJsTypeInterfaceWithStaticInitializationAndStaticOverlayMethod.getObject())
        .isEqualTo(4);
    assertThat(
            createNativeJsTypeInterfaceWithStaticInitializationAndInstanceOverlayMethod()
                .getObject())
        .isEqualTo(6);
    assertThat(NativeJsTypeInterfaceWithComplexStaticInitialization.object).isEqualTo(7);
    assertThat(new JavaTypeImplementingNativeJsTypeInterceWithDefaultMethod().getObject())
        .isEqualTo(9);
  }

  @JsFunction
  interface VarargsFunction {

    String f(int i, String... args);
  }

  @JsMethod
  private static native String callFromJSNI(VarargsFunction f);

  @Test
  public void testJsVarargsLambda() {
    VarargsFunction function = (i, args) -> args[i];
    assertThat((Object) function.f(1, "a", "b", "c")).isSameInstanceAs("b");
    assertThat((Object) callFromJSNI(function)).isSameInstanceAs("c");
    String[] pars = new String[] {"a", "b", "c"};
    assertThat((Object) function.f(0, pars)).isSameInstanceAs("a");
  }

  private static <T> T m(T s) {
    return s;
  }

  static class Some<T> {

    T s;
    MyFunction2<T, T, T> combine;

    Some(T s, MyFunction2<T, T, T> combine) {
      this.s = s;
      this.combine = combine;
    }

    public T m(T s2) {
      return combine.apply(s, s2);
    }

    public T m1() {
      return s;
    }
  }

  @FunctionalInterface
  interface MyFunction1<T, U> {

    U apply(T t);
  }

  @FunctionalInterface
  interface MyFunction2<T, U, V> {

    V apply(T t, U u);
  }

  @Test
  public void testMethodReference_implementedInSuperclass() {
    MyFunction1<StringBuilder, String> toString = StringBuilder::toString;
    assertThat((Object) toString.apply(new StringBuilder("Hello"))).isEqualTo("Hello");
  }

  static MyFunction2<String, String, String> concat = (s, t) -> s + t;

  @Test
  public void testMethodReference_genericTypeParameters() {
    testMethodReference_genericTypeParameters(
        new Some<String>("Hell", concat), "Hell", "o", concat);
  }

  private static <T> void testMethodReference_genericTypeParameters(
      Some<T> some, T t1, T t2, MyFunction2<T, T, T> combine) {
    T t1t2 = combine.apply(t1, t2);

    // Test all 4 flavours of methodReference
    // 1. Static method
    assertThat(((MyFunction1<T, T>) Java8Test::m).apply(t1t2)).isEqualTo(t1t2);
    // 2. Qualified instance method
    assertThat(((MyFunction1<T, T>) some::m).apply(t2)).isEqualTo(t1t2);
    // 3. Unqualified instance method
    assertThat(((MyFunction1<Some<T>, T>) Some<T>::m1).apply(some)).isEqualTo(t1);
    assertThat(
            (Object)
                ((MyFunction1<Some<String>, String>) Some<String>::m1)
                    .apply(new Some<>("Hello", concat)))
        .isEqualTo("Hello");
    // 4. Constructor reference.
    assertThat(
            ((MyFunction2<T, MyFunction2<T, T, T>, Some<T>>) Some<T>::new)
                .apply(t1t2, combine)
                .m1())
        .isEqualTo(t1t2);
  }

  static MyFunction2<Integer, Integer, Integer> addInteger = (s, t) -> s + t;

  @FunctionalInterface
  interface MyIntFunction1 {

    int apply(int t);
  }

  @FunctionalInterface
  interface MyIntFunction2 {

    int apply(int t, int u);
  }

  @FunctionalInterface
  interface MyIntFuncToSomeIntegeFunction2 {

    SomeInteger apply(int t, MyFunction2<Integer, Integer, Integer> u);
  }

  @FunctionalInterface
  interface MySomeIntegerFunction1 {

    int apply(SomeInteger t);
  }

  @FunctionalInterface
  interface MySomeIntegerIntFunction2 {

    int apply(SomeInteger t, int u);
  }

  static MyIntFunction2 addint = (s, t) -> s + t;

  static class SomeInteger {

    int s;
    MyFunction2<Integer, Integer, Integer> combine;

    SomeInteger(int s, MyFunction2<Integer, Integer, Integer> combine) {
      this.s = s;
      this.combine = combine;
    }

    public int m(int s2) {
      return combine.apply(s, s2);
    }

    public int m1() {
      return s;
    }
  }

  @Test
  public void testMethodReference_autoboxing() {
    SomeInteger some = new SomeInteger(3, addInteger);

    // Test all 4 flavours of methodReference autoboxing parameters.
    // 1. Static method
    assertThat((Object) ((MyFunction1<Integer, Integer>) Java8Test::m).apply(5))
        .isEqualTo((Integer) 5);
    // 2. Qualified instance method
    assertThat((Object) ((MyFunction1<Integer, Integer>) some::m).apply(2)).isEqualTo((Integer) 5);
    // 3. Unqualified instance method
    assertThat((Object) ((MyFunction1<SomeInteger, Integer>) SomeInteger::m1).apply(some))
        .isEqualTo((Integer) 3);
    assertThat(
            (Object) ((MyFunction2<SomeInteger, Integer, Integer>) SomeInteger::m).apply(some, 2))
        .isEqualTo((Integer) 5);
    assertThat(
            (Object)
                ((MyFunction1<SomeInteger, Integer>) SomeInteger::m1)
                    .apply(new SomeInteger(5, addInteger)))
        .isEqualTo((Integer) 5);
    // 4. Constructor reference.
    assertThat(
            (Object)
                ((MyFunction2<Integer, MyFunction2<Integer, Integer, Integer>, SomeInteger>)
                        SomeInteger::new)
                    .apply(5, addInteger)
                    .m1())
        .isEqualTo(5);

    // Test all 4 flavours of methodReference (interface unboxed)
    // 1. Static method
    assertThat((Object) ((MyIntFunction1) Java8Test::m).apply(5)).isEqualTo(5);
    // 2. Qualified instance method
    assertThat((Object) ((MyIntFunction1) some::m).apply(2)).isEqualTo(5);
    // 3. Unqualified instance method
    assertThat((Object) ((MySomeIntegerFunction1) SomeInteger::m1).apply(some)).isEqualTo(3);
    // The next expression was the one that triggered bug #9346 where decisions on whether to
    // box/unbox were decided incorrectly due to differring number of parameters in the method
    // reference and the functional interface method.
    assertThat((Object) ((MySomeIntegerIntFunction2) SomeInteger::m).apply(some, 2)).isEqualTo(5);
    assertThat(
            (Object)
                ((MySomeIntegerFunction1) SomeInteger::m1).apply(new SomeInteger(5, addInteger)))
        .isEqualTo(5);
    // 4. Constructor reference.
    assertThat(
            (Object) ((MyIntFuncToSomeIntegeFunction2) SomeInteger::new).apply(5, addInteger).m1())
        .isEqualTo(5);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private static class NativeClassWithJsOverlay {

    @JsOverlay
    public static String m(String s) {
      MyFunction1<String, String> id = (a) -> a;
      return id.apply(s);
    }
  }

  @Test
  public void testNativeJsOverlay_lambda() {
    assertThat((Object) NativeClassWithJsOverlay.m("Hello")).isSameInstanceAs("Hello");
  }

  interface IntefaceWithDefaultMethodAndLambda {

    boolean f();

    default BooleanPredicate fAsPredicate() {
      // This lambda will be defined as an instance method in the enclosing class, which is an
      // interface. In this case the methdod will be devirtualized.
      return () -> this.f();
    }
  }

  interface BooleanPredicate {

    boolean apply();
  }

  @Test
  public void testLambdaCapturingThis_onDefaultMethod() {
    assertThat(
            new IntefaceWithDefaultMethodAndLambda() {
              @Override
              public boolean f() {
                return true;
              }
            }.fAsPredicate().apply())
        .isTrue();
  }

  @JsFunction
  interface JsFunctionInterface {

    Double m();

    @JsOverlay
    default Double callM() {
      return this.m();
    }
  }

  @JsMethod
  private static native JsFunctionInterface createNative();

  @Test
  public void testJsFunction_withOverlay() {
    JsFunctionInterface f =
        new JsFunctionInterface() {
          @Override
          @SuppressWarnings("BoxedPrimitiveConstructor")
          public Double m() {
            return new Double(2.0);
          }
        };
    assertThat((Object) f.callM().intValue()).isEqualTo(2);
    assertThat((Object) createNative().callM().intValue()).isEqualTo(5);
  }

  interface FunctionalExpressionBridgesI<T> {

    T apply(T t);

    FunctionalExpressionBridgesI<T> m(T t);
  }

  @FunctionalInterface
  interface FunctionalExpressionBridgesJ<T extends Comparable<T>>
      extends FunctionalExpressionBridgesI<T> {

    @Override
    T apply(T t);

    // Overrides I.m() and specializes return type
    @Override
    default FunctionalExpressionBridgesJ<T> m(T t) {
      return this;
    }
  }

  public static String identity(String s) {
    return s;
  }

  @Test
  public void testFunctionalExpressionBridges() {
    FunctionalExpressionBridgesJ<String> ann =
        new FunctionalExpressionBridgesJ<String>() {
          @Override
          public String apply(String string) {
            return string;
          }
        };

    assertBridgeDispatchIsCorrect(ann);
    assertBridgeDispatchIsCorrect((String s) -> s + "");
    assertBridgeDispatchIsCorrect(Java8Test::identity);
  }

  private static void assertBridgeDispatchIsCorrect(
      FunctionalExpressionBridgesJ<String> functionalExpression) {
    assertThat((Object) functionalExpression.m(null).apply("Hello")).isEqualTo("Hello");
    assertThat((Object) functionalExpression.apply("Hello")).isEqualTo("Hello");
    assertThat(((FunctionalExpressionBridgesI<String>) functionalExpression).apply("Hello"))
        .isEqualTo("Hello");
  }

  static class ClassWithAVeryLoooooooooooooooooooooooooooooooooooongName {

    public static String m() {
      return null;
    }
  }

  // Regression test for bug: #9426.
  @Test
  public void testCorrectNaming() {
    Function<String> f = ClassWithAVeryLoooooooooooooooooooooooooooooooooooongName::m;
    assertThat(f).isNotNull();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface InterfaceWithOverlay {

    @JsProperty
    int getLength();

    @JsOverlay
    default int len() {
      return this.getLength();
    }
  }

  @JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
  abstract static class SubclassImplementingInterfaceWithOverlay implements InterfaceWithOverlay {}

  // Regression test for bug: #9440
  @Test
  public void testInterfaceWithOverlayAndNativeSubclass() {
    SubclassImplementingInterfaceWithOverlay object =
        (SubclassImplementingInterfaceWithOverlay) (Object) new int[] {1, 2, 3};
    assertThat((Object) object.len()).isEqualTo(3);
  }

  interface Producer<T> {

    T get();
  }

  @SuppressWarnings("unchecked")
  private static Producer<Object> createInnerClassProducer() {
    class InnerClass {}
    return (Producer) InnerClass::new;
  }

  @Test
  public void testLocalClassConstructorReferenceInStaticMethod() {
    assertThat(createInnerClassProducer().get() != null).isTrue();
  }

  // NOTE: DO NOT reorder the following classes, bug  #9453 is only reproducible in certain
  // orderings.
  interface SubSubSuperDefaultMethodDevirtualizationOrder
      extends SubSuperDefaultMethodDevirtualizationOrder {

    @Override
    default String m() {
      return SubSuperDefaultMethodDevirtualizationOrder.super.m();
    }
  }

  interface SubSuperDefaultMethodDevirtualizationOrder
      extends SuperSuperDefaultMethodDevirtualizationOrder {

    @Override
    default String m() {
      return SuperSuperDefaultMethodDevirtualizationOrder.super.m();
    }
  }

  interface SuperSuperDefaultMethodDevirtualizationOrder {

    default String m() {
      return "Hi";
    }
  }

  // Regression test for bug #9453.
  @Test
  public void testDefaultMethodDevirtualizationOrder() {
    assertThat((Object) new SubSubSuperDefaultMethodDevirtualizationOrder() {}.m()).isEqualTo("Hi");
  }

  private static String first(String... strings) {
    return strings[0];
  }

  // Regresion test for https://github.com/gwtproject/gwt/issues/9497
  @Test
  public void testVarargsFunctionalConversion() {
    java.util.function.Function<String[], String> function = Java8Test::first;
    assertThat((Object) function.apply(new String[] {"Hello", "GoodBye"})).isEqualTo("Hello");
  }
}
