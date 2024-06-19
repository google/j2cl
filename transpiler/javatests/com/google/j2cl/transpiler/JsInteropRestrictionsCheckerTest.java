/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler;

import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithDefaults;

import com.google.j2cl.transpiler.TranspilerTester.TranspileResult;
import junit.framework.TestCase;

/** Tests for JsInteropRestrictionsChecker. */
public class JsInteropRestrictionsCheckerTest extends TestCase {

  public void testSystemGetPropertyFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "class Main {",
            "  public final static String COMPILE_TIME_CONSTANT =\"constant\";",
            "  public static void main(){",
            "    System.getProperty(COMPILE_TIME_CONSTANT);",
            "    String s=\"property\";",
            "    System.getProperty(s);",
            "    System.getProperty(\"pro\"+\"perty\");",
            "    System.getProperty(COMPILE_TIME_CONSTANT,\"default\");}",
            "}")
        .assertErrorsWithSourcePosition(
            "Error:Buggy.java:7: Method 'String System.getProperty(String)' "
                + "can only take a string literal as its first parameter",
            "Error:Buggy.java:9: Method 'String System.getProperty(String)' "
                + "can only take a string literal as its first parameter",
            "Error:Buggy.java:10: Method 'String System.getProperty(String)' "
                + "can only take a string literal as its first parameter",
            "Error:Buggy.java:11: Method 'String System.getProperty(String, String)' "
                + "can only take a string literal as its first parameter");
  }

  public void testSystemGetPropertySucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "class Main {",
            "  public static void main(){",
            "    System.getProperty(\"java.runtime.name\");",
            "    System.getProperty(\"java.runtime.name\",\"default\");}",
            "}")
        .assertNoWarnings();
  }

  public void testCollidingNamePackagePrivateFails() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "  @JsMethod",
            "  void m() {}",
            "  @JsMethod",
            "  void n() {}",
            "}")
        .addCompilationUnit(
            "somePackage.SubBuggy",
            "import jsinterop.annotations.*;",
            "public class SubBuggy extends test.Buggy{",
            "  @JsMethod",
            "  void m() {}",
            "  @JsMethod",
            "  public void n() {}", // public but not overriding due to different packages.
            "}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "'void SubBuggy.m()' and 'void Buggy.m()' cannot both use the same JavaScript name"
                + " 'm'.",
            "'void SubBuggy.n()' and 'void Buggy.n()' cannot both use the same JavaScript name"
                + " 'n'.");
  }

  public void testCollidingNameInInterfaceFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface Foo {",
            "  @JsMethod(name = \"doIt\")",
            "  default void maybeDoIt(Foo foo) {}",
            "}",
            "public class Buggy implements Foo {",
            "  @JsMethod",
            "  void doIt(Foo foo) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.doIt(Foo)' and 'void Foo.maybeDoIt(Foo)' cannot both use the same"
                + " JavaScript name 'doIt'.");
  }

  public void testCollidingAccidentalOverrideConcreteMethodFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface Foo {",
            "  void doIt(Foo foo);",
            "}",
            "@JsType",
            "interface Bar {",
            "  void doIt(Bar bar);",
            "}",
            "class ParentBuggy {",
            "  public void doIt(Foo foo) {}",
            "  public void doIt(Bar bar) {}",
            "}",
            "public class Buggy extends ParentBuggy implements Foo, Bar {",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Bar.doIt(Bar)' and "
                + "'void Foo.doIt(Foo)' cannot both use the same JavaScript name 'doIt'.");
  }

  public void testCollidingAccidentalOverrideAbstractMethodFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface Foo {",
            "  void doIt(Foo foo);",
            "}",
            "@JsType",
            "interface Bar {",
            "  void doIt(Bar bar);",
            "}",
            "abstract class Baz implements Foo, Bar {",
            "  public abstract void doIt(Foo foo);",
            "  public abstract void doIt(Bar bar);",
            "}",
            "public class Buggy {}  // Unrelated class")
        .assertErrorsWithoutSourcePosition(
            "'void Baz.doIt(Bar)' and "
                + "'void Baz.doIt(Foo)' cannot both use the same JavaScript name 'doIt'.");
  }

  public void testCollidingAccidentalOverrideHalfAndHalfFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface Foo {",
            "  void doIt(Foo foo);",
            "}",
            "@JsType",
            "interface Bar {",
            "   void doIt(Bar bar);",
            "}",
            "class ParentParent {",
            "  public void doIt(Bar x) {}",
            "}",
            "@JsType",
            "class Parent extends ParentParent {",
            "  public void doIt(Foo x) {}",
            "}",
            "public class Buggy extends Parent implements Bar {}")
        .assertErrorsWithoutSourcePosition(
            "'void Parent.doIt(Foo)' and "
                + "'void Bar.doIt(Bar)' cannot both use the same JavaScript name 'doIt'.");
  }

  public void testOverrideNoNameSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class Parent {",
            "  @JsMethod(name = \"a\")",
            "  public void ma() {}",
            "  @JsMethod(name = \"b\")",
            "  public void mb() {}",
            "}",
            "@JsType",
            "class Child1 extends Parent {",
            "  public void ma() {}",
            "  public void mb() {}",
            "}",
            "class Child2 extends Parent {",
            "  @JsMethod",
            "  public void ma() {}",
            "  @JsMethod",
            "  public void mb() {}",
            "}",
            "class Child3 extends Parent {",
            "  public void ma() {}",
            "  public void mb() {}",
            "}",
            "@JsType",
            "class Child4 extends Parent {",
            "  @JsIgnore",
            "  public void ma() {}",
            "  @JsIgnore",
            "  public void mb() {}",
            "}",
            "public class Buggy extends Parent {",
            "  Child1 c1;",
            "  Child2 c2;",
            "  Child3 c3;",
            "  Child4 c4;",
            "}")
        .assertNoWarnings();
  }

  public void testCollidingFieldsFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "  @JsProperty",
            "  public static final int show = 0;",
            "  @JsProperty(name = \"show\")",
            "  public static final int display = 0;",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'Buggy.display' and 'Buggy.show' cannot both use the same "
                + "JavaScript name 'show'.");
  }

  public void testJsPropertyNonGetterStyleSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty(name = \"x\") int x();",
            "  @JsProperty(name = \"x\") void x(int x);",
            "}")
        .assertNoWarnings();
  }

  public void testJsPropertyGetterStyleSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public abstract class Buggy {",
            "  @JsProperty static native int getStaticX();",
            "  @JsProperty static native void setStaticX(int x);",
            "  @JsProperty abstract int getX();",
            "  @JsProperty abstract void setX(int x);",
            "  @JsProperty abstract boolean isY();",
            "  @JsProperty abstract void setY(boolean y);",
            "}")
        .assertNoWarnings();
  }

  public void testJsPropertyIncorrectGetterStyleFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public interface Buggy {",
            "  @JsProperty int isX();",
            "  @JsProperty int getY(int x);",
            "  @JsProperty void getZ();",
            "  @JsProperty void setX(int x, int y);",
            "  @JsProperty void setY();",
            "  @JsProperty int setZ(int z);",
            "  @JsProperty static void setStatic() {}",
            "  @JsProperty void setW(int... z);",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsProperty 'int Buggy.isX()' cannot have a non-boolean return.",
            "JsProperty 'int Buggy.getY(int x)' should have a correct setter or getter signature.",
            "JsProperty 'void Buggy.getZ()' should have a correct setter or getter signature.",
            "JsProperty 'void Buggy.setX(int x, int y)' should have a correct "
                + "setter or getter signature.",
            "JsProperty 'void Buggy.setY()' should have a correct setter or getter signature.",
            "JsProperty 'int Buggy.setZ(int z)' should have a correct setter or getter signature.",
            "JsProperty 'void Buggy.setStatic()' should have a correct setter or getter "
                + "signature.",
            "JsProperty 'void Buggy.setW(int... z)' cannot have a vararg parameter.");
  }

  public void testJsPropertyNonGetterStyleFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty boolean hasX();",
            "  @JsProperty int x();",
            "  @JsProperty void x(int x);",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsProperty 'boolean Buggy.hasX()' should either follow Java Bean naming conventions"
                + " or provide a name.",
            "JsProperty 'int Buggy.x()' should either follow Java Bean naming conventions"
                + " or provide a name.",
            "JsProperty 'void Buggy.x(int x)' should either follow Java Bean naming conventions"
                + " or provide a name.");
  }

  public void testCollidingJsPropertiesTwoGettersFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty",
            "  boolean isX();",
            "  @JsProperty",
            "  boolean getX();",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'boolean Buggy.isX()' and 'boolean Buggy.getX()' "
                + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingNativeJsPropertiesSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true)",
            "public class Buggy {",
            "  @JsMethod",
            "  public native int now();",
            "  @JsProperty",
            "  public native Object getNow();",
            "  @JsMethod",
            "  public static native int other();",
            "  @JsProperty",
            "  public static native Object getOther();",
            "  @JsMethod",
            "  public static native int another();",
            "  @JsProperty",
            "  public static Object another;",
            "}")
        .assertNoWarnings();
  }

  public void testCollidingJsPropertiesTwoSettersFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty",
            "  void setX(boolean x);",
            "  @JsProperty",
            "  void setX(int x);",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.setX(int)' and 'void Buggy.setX(boolean)' "
                + "cannot both use the same JavaScript name 'x'.");
  }

  public void testMultipleCollidingJsPropertiesFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty",
            "  void setY(boolean x);",
            "  @JsMethod(name = \"y\")",
            "  void setX(boolean x);",
            "  @JsProperty",
            "  boolean getY();",
            "}")
        .assertErrorsWithoutSourcePosition(
            // TODO(b/277967621): This error message is incorrect, in this case there are multiple
            // members colliding on the same name but the error reporter selects the wrong ones to
            // emit the message.
            "'void Buggy.setY(boolean)' and 'boolean Buggy.getY()' cannot both use the same"
                + " JavaScript name 'y'.");
  }

  public void testCollidingJsMethodAndJsPropertyGetterFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface IBuggy {",
            "  @JsMethod",
            "  boolean x(boolean foo);",
            "  @JsProperty",
            "  int getX();",
            "}",
            "public class Buggy implements IBuggy {",
            "  public boolean x(boolean foo) {return false;}",
            "  public int getX() {return 0;}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'boolean IBuggy.x(boolean)' and 'int IBuggy.getX()' "
                + "cannot both use the same JavaScript name 'x'.",
            "'boolean Buggy.x(boolean)' and 'int Buggy.getX()' "
                + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsMethodAndJsPropertySetterFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface IBuggy {",
            "  @JsMethod",
            "  boolean x(boolean foo);",
            "  @JsProperty",
            "  void setX(int a);",
            "}",
            "public class Buggy implements IBuggy {",
            "  public boolean x(boolean foo) {return false;}",
            "  public void setX(int a) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'boolean IBuggy.x(boolean)' and 'void IBuggy.setX(int)' "
                + "cannot both use the same JavaScript name 'x'.",
            "'boolean Buggy.x(boolean)' and 'void Buggy.setX(int)' "
                + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingPropertyAccessorsFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "  @JsProperty",
            "  public static void setDisplay(int x) {}",
            "  @JsProperty(name = \"display\")",
            "  public static void setDisplay2(int x) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.setDisplay2(int)' and 'void Buggy.setDisplay(int)' cannot both use the "
                + "same JavaScript name 'display'.");
  }

  public void testCollidingMethodsFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "  @JsMethod",
            "  public static void show() {}",
            "  @JsMethod(name = \"show\")",
            "  public static void display() {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.show()' and 'void Buggy.display()' cannot both use the same "
                + "JavaScript name 'show'.");
  }

  public void testCollidingMethodToPropertyAccessorFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "  @JsProperty",
            "  public static void setShow(int x) {}",
            "  @JsMethod",
            "  public static void show() {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.show()' and 'void Buggy.setShow(int)' cannot both use the same "
                + "JavaScript name 'show'.");
  }

  public void testCollidingMethodToFieldFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "  @JsMethod",
            "  public static void show() {}",
            "  @JsProperty",
            "  public static final int show = 0;",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'Buggy.show' and 'void Buggy.show()' cannot both use the same "
                + "JavaScript name 'show'.");
  }

  public void testCollidingMethodToFieldJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class Buggy {",
            "  public void show() {}",
            "  public final int show = 0;",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'Buggy.show' and 'void Buggy.show()' "
                + "cannot both use the same JavaScript name 'show'.");
  }

  public void testCollidingJsMethodWithObjectMethod() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface Buggy {",
            "  @JsMethod(name = \"equals\")",
            "  boolean notEquals(Object o);",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'boolean Buggy.notEquals(Object)' and 'boolean Object.equals(Object)' cannot both use "
                + "the same JavaScript name 'equals'.");
  }

  public void testCollidingMethodToMethodJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class Buggy {",
            "  public void show(int x) {}",
            "  public void show() {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.show()' and "
                + "'void Buggy.show(int)' cannot both use the same JavaScript name 'show'.");
  }

  public void testValidCollidingSubclassMembersSucceeds() {
    assertTranspileSucceeds(
            "test.NonJsTypeSubclass",
            "import jsinterop.annotations.*;",
            "@JsType",
            "class JsTypeParent {",
            "  JsTypeParent() {}",
            "  public int foo = 55;",
            "  public void bar() {}",
            "}",
            "public class NonJsTypeSubclass extends JsTypeParent {",
            "  public int foo = 110;",
            "  public int bar = 110;",
            "  public void foo(int a) {}",
            "  public void bar(int a) {}",
            "}",
            "class PlainJavaParent {",
            "  public int foo = 55;",
            "  public void foo() {}",
            "}",
            "@JsType",
            " class AJsTypeSubclass extends PlainJavaParent {",
            "  public int foo = 110;",
            "}",
            "@JsType",
            "class AnotherJsTypeSubclass extends PlainJavaParent {",
            "  public void foo(int a) {}",
            "}")
        .assertNoWarnings();
  }

  public void testCollidingSubclassMembersJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "class ParentBuggy {",
            "  public int foo = 55;",
            "}",
            "@JsType",
            "public class Buggy extends ParentBuggy {",
            "  public int foo = 110;",
            "}",
            "@JsType",
            "class OtherBuggy extends ParentBuggy {",
            "  public void foo(int a) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'Buggy.foo' and 'ParentBuggy.foo' cannot both use the same "
                + "JavaScript name 'foo'.",
            "'void OtherBuggy.foo(int)' and 'ParentBuggy.foo' cannot both use the same "
                + "JavaScript name 'foo'.");
  }

  public void testCollidingSubclassMethodToMethodInterfaceJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface IBuggy1 {",
            "  void show();",
            "}",
            "@JsType",
            "interface IBuggy2 {",
            "  void show(boolean b);",
            "}",
            "public class Buggy implements IBuggy1 {",
            "  public void show() {}",
            "}",
            "class Buggy2 extends Buggy implements IBuggy2 {",
            "  public void show(boolean b) {}",
            "}",
            "interface IBuggy3 {",
            "@JsMethod(name = \"display\")",
            "  void show(boolean b);",
            "}",
            "class Buggy3 implements IBuggy2, IBuggy3 {",
            "  public void show(boolean b) {}",
            "}",
            "class Main {",
            "  public static void main() {",
            "    Object o;",
            // TODO(b/71911586): This lambda should be rejected but it is not.
            "    o = (IBuggy2 & IBuggy3) (b) -> {};",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy2.show(boolean)' and 'void Buggy.show()' cannot both use the same "
                + "JavaScript name 'show'.",
            "'void Buggy3.show(boolean b)' cannot be assigned JavaScript name 'display' that is "
                + "different from the JavaScript name of a method it overrides "
                + "('void IBuggy2.show(boolean)' with JavaScript name 'show').");
  }

  public void testRenamedSubclassMethodToBridgeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy<T> {",
            "  @JsMethod",
            "  public void show(T t) {}",
            "  @JsMethod(name = \"display\")",
            "  public void show(String s) {}",
            "}",
            "class SubBuggy extends Buggy<String> {",
            "  public void show(String s) {}",
            "}")
        .assertNoWarnings();
  }

  public void testCollidingSubclassMethodToMethodJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "class ParentBuggy {",
            "  public void foo() {}",
            "}",
            "@JsType",
            "public class Buggy extends ParentBuggy {",
            "  public void foo(int a) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.foo(int)' and 'void ParentBuggy.foo()' cannot both use the same "
                + "JavaScript name 'foo'.");
  }

  public void testCollidingJsMethodToJsPropertyFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class ParentBuggy {",
            "  @JsMethod boolean foo() { return false; }",
            "  @JsProperty boolean getBar() { return false; }",
            // TODO(b/35901141): Implement missing restriction check.
            "  @JsProperty @JsMethod boolean getBax() { return false; }",
            "  @JsProperty boolean getBlah() { return false; }",
            "  @JsMethod(name = \"bleh\") boolean getBleh() { return false; }",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  @JsProperty boolean getFoo() { return false; }",
            "  @JsMethod boolean bar() { return false; }",
            "  @JsProperty boolean getBleh() { return false; }",
            "  @JsMethod(name = \"blah\") boolean getBlah() { return false; }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'boolean Buggy.getFoo()' and 'boolean ParentBuggy.foo()' cannot both use the same "
                + "JavaScript name 'foo'.",
            "'boolean Buggy.bar()' and 'boolean ParentBuggy.getBar()' cannot both use the same "
                + "JavaScript name 'bar'.",
            "JsProperty 'boolean Buggy.getBleh()' cannot override "
                + "JsMethod 'boolean ParentBuggy.getBleh()'.",
            "JsMethod 'boolean Buggy.getBlah()' cannot override JsProperty "
                + "'boolean ParentBuggy.getBlah()'.");
  }

  public void testCollidingSubclassMethodToMethodTwoLayerInterfaceJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface IParentBuggy1 {",
            "  void show();",
            "}",
            "interface IBuggy1 extends IParentBuggy1 {",
            "}",
            "@JsType",
            "interface IParentBuggy2 {",
            "  void show(boolean b);",
            "}",
            "interface IBuggy2 extends IParentBuggy2 {",
            "}",
            "public class Buggy implements IBuggy1 {",
            "  public void show() {}",
            "}",
            "class Buggy2 extends Buggy implements IBuggy2 {",
            "  public void show(boolean b) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy2.show(boolean)' and 'void Buggy.show()' cannot both use the same "
                + "JavaScript name 'show'.");
  }

  public void testNonCollidingSyntheticBridgeMethodSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface Comparable<T> {",
            "  int compareTo(T other);",
            "}",
            "@JsType",
            "class Enum<E extends Enum<E>> implements Comparable<E> {",
            "  public int compareTo(E other) {return 0;}",
            "}",
            "public class Buggy {}")
        .assertNoWarnings();
  }

  public void testCollidingSyntheticBridgeMethodSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface Comparable<T> {",
            "  int compareTo(T other);",
            "}",
            "@JsType",
            "class Enum<E extends Enum<E>> implements Comparable<E> {",
            "  public int compareTo(E other) {return 0;}",
            "}",
            "public class Buggy {}")
        .assertNoWarnings();
  }

  public void testSpecializeReturnTypeInImplementorSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface I {",
            "  I m();",
            "}",
            "@JsType",
            "class Buggy implements I {",
            "  public Buggy m() { return null; } ",
            "}")
        .assertNoWarnings();
  }

  public void testSpecializeReturnTypeInSubclassSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "class S {",
            "  public S m() { return null; }",
            "}",
            "@JsType",
            "public class Buggy extends S {",
            "  public Buggy m() { return null; } ",
            "}")
        .assertNoWarnings();
  }

  public void testCollidingTwoLayerSubclassFieldToFieldJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "class ParentParentBuggy {",
            "  protected ParentParentBuggy() {}",
            "  public int foo = 55;",
            "}",
            "class ParentBuggy extends ParentParentBuggy {",
            "  public int foo = 55;",
            "}",
            "@JsType",
            "public class Buggy extends ParentBuggy {",
            "  public int foo = 110;",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'Buggy.foo' and 'ParentParentBuggy.foo' cannot both use the same "
                + "JavaScript name 'foo'.");
  }

  public void testShadowedSuperclassJsMethodFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class ParentBuggy {",
            "  @JsMethod private void foo() {}",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  @JsMethod private void foo() {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.foo()' and 'void ParentBuggy.foo()' cannot both use the same "
                + "JavaScript name 'foo'.");
  }

  public void testRenamedSuperclassJsMethodFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "class ParentBuggy {",
            "  ParentBuggy() {}",
            "  public void foo() {}",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  @JsMethod(name = \"bar\") public void foo() {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.foo()' cannot be assigned JavaScript name 'bar' that is different"
                + " from the JavaScript name of a method it overrides "
                + "('void ParentBuggy.foo()' with JavaScript name 'foo').");
  }

  public void testRenamedSuperInterfaceJsMethodFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface ParentBuggy {",
            "  void foo();",
            "}",
            "public interface Buggy extends ParentBuggy {",
            "  @JsMethod(name = \"bar\") void foo();",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.foo()' cannot be assigned JavaScript name 'bar' that is different"
                + " from the JavaScript name of a method it overrides "
                + "('void ParentBuggy.foo()' with JavaScript name 'foo').");
  }

  // TODO(b/37579830): Finalize checker implementation and enable this test.
  public void disabled_testAccidentallyRenamedSuperInterfaceJsMethodFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface IBuggy {",
            "  void foo();",
            "}",
            "@JsType",
            "class ParentBuggy {",
            "  @JsMethod(name = \"bar\") public void foo() {}",
            "}",
            "public class Buggy extends ParentBuggy implements IBuggy {",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Line 11: 'void EntryPoint.ParentBuggy.foo()' "
                + "(exposed by 'EntryPoint.Buggy') "
                + "cannot be assigned a different JavaScript name than the method it overrides.");
  }

  public void testRenamedSuperclassJsPropertyFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class ParentBuggy {",
            "  @JsProperty public int getFoo() { return 0; }",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  @JsProperty(name = \"bar\") public int getFoo() { return 0; }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'int Buggy.getFoo()' cannot be assigned JavaScript name 'bar' that is different"
                + " from the JavaScript name of a method it overrides "
                + "('int ParentBuggy.getFoo()' with JavaScript name 'foo').");
  }

  public void testJsPropertyDifferentFlavourInSubclassFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "class ParentBuggy {",
            "  ParentBuggy() {}",
            "  @JsProperty public boolean isFoo() { return false; }",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  @JsProperty public boolean getFoo() { return false;}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'boolean Buggy.getFoo()' and 'boolean ParentBuggy.isFoo()' cannot both use the "
                + "same JavaScript name 'foo'.");
  }

  public void testConsistentPropertyTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface IBuggy {",
            "  @JsProperty",
            "  public int getFoo();",
            "  @JsProperty",
            "  public void setFoo(int value);",
            "}",
            "public class Buggy implements IBuggy {",
            "  public int getFoo() {return 0;}",
            "  public void setFoo(int value) {}",
            "}")
        .assertNoWarnings();
  }

  public void testInconsistentGetSetPropertyTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface IBuggy {",
            "  @JsProperty",
            "  public int getFoo();",
            "  @JsProperty",
            "  public void setFoo(Integer value);",
            "}",
            "public class Buggy implements IBuggy {",
            "  public int getFoo() {return 0;}",
            "  public void setFoo(Integer value) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsProperty setter 'void IBuggy.setFoo(Integer)' and getter 'int IBuggy.getFoo()'"
                + " cannot have inconsistent types.",
            "JsProperty setter 'void Buggy.setFoo(Integer)' and getter 'int Buggy.getFoo()'"
                + " cannot have inconsistent types.");
  }

  public void testInconsistentIsSetPropertyTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface IBuggy {",
            "  @JsProperty",
            "  public boolean isFoo();",
            "  @JsProperty",
            "  public void setFoo(Object value);",
            "}",
            "public class Buggy implements IBuggy {",
            "  public boolean isFoo() {return false;}",
            "  public void setFoo(Object value) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsProperty setter 'void IBuggy.setFoo(Object)' and getter 'boolean IBuggy.isFoo()'"
                + " cannot have inconsistent types.",
            "JsProperty setter 'void Buggy.setFoo(Object)' and getter 'boolean Buggy.isFoo()'"
                + " cannot have inconsistent types.");
  }

  public void testJsPropertySuperCallSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType class Super {",
            "  @JsProperty public int getX() { return 5; }",
            "}",
            "@JsType public class Buggy extends Super {",
            "  public int m() { return super.getX(); }",
            "}")
        .assertNoWarnings();
  }

  public void testJsPropertyOnStaticMethodSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType public class Buggy {",
            "  @JsProperty public static int getX() { return 0; }",
            "}")
        .assertNoWarnings();
  }

  public void testJsPropertyCallSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType class Super {",
            "  @JsProperty public int getX() { return 5; }",
            "}",
            "@JsType public class Buggy extends Super {",
            "  public int m() { return getX(); }",
            "}")
        .assertNoWarnings();
  }

  public void testJsPropertyAccidentalSuperCallSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType class Super {",
            "  @JsProperty public int getX() { return 5; }",
            "}",
            "@JsType interface Interface {",
            "  @JsProperty int getX();",
            "}",
            "@JsType public class Buggy extends Super implements Interface {",
            "}")
        .assertNoWarnings();
  }

  public void testJsPropertyOverrideSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType class Super {",
            "  @JsProperty public void setX(int x) {  }",
            "  @JsProperty public int getX() { return 5; }",
            "}",
            "@JsType public class Buggy extends Super {",
            "  @JsProperty public void setX(int x) {  }",
            "}",
            "class OverrideWithoutJsType extends Super {",
            "  @JsProperty public void setX(int x) {  }",
            "}",
            "class OverrideWithoutJsPropertyNorJsType extends Super {",
            "  public void setX(int x) {  }",
            "}",
            "@JsType class OverrideWithoutJsProperty extends Super {",
            "  public void setX(int x) {  }",
            "}")
        .assertNoWarnings();
  }

  public void testMixingJsMethodJsPropertyFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class Super {",
            "  @JsMethod public int getY() { return 5; }",
            "  @JsProperty public void setX(int x) {}",
            "  @JsProperty(name = \"setZ\")  public void setZ(int z) {}",
            "}",
            "public class Buggy extends Super {",
            "  @JsProperty(name = \"getY\") public int getY() { return 6; }",
            "  @JsMethod public void setX(int x) {  }",
            "  @JsMethod public void setZ(int z) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsProperty 'int Buggy.getY()' cannot override JsMethod 'int Super.getY()'.",
            "JsMethod 'void Buggy.setZ(int z)' cannot override JsProperty "
                + "'void Super.setZ(int)'.",
            "JsMethod 'void Buggy.setX(int x)' cannot override JsProperty "
                + "'void Super.setX(int)'.");
  }

  // GWT enforces some restriction on JSNI JsMethods. In J2CL,  JSNI is just a comment and no test
  // should fail for JSNI reasons.
  public void testJsMethodJSNIVarargsSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "  @JsMethod public native void m(int i, int... z) /*-{ return arguments[i]; }-*/;",
            // The next method fails in GWT but should not fail in J2CL.
            "  @JsMethod public native void n(int i, int... z) /*-{ return z[0];}-*/;",
            "}")
        .assertNoWarnings();
  }

  public void testMultiplePrivateConstructorsSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class Buggy {",
            "  private Buggy() {}",
            "  private Buggy(int a) {}",
            "}")
        .assertNoWarnings();
  }

  public void testJsConstructorSubclassSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class Buggy {",
            "  public Buggy() {}",
            "  @JsIgnore",
            "  public Buggy(int a) {",
            "    this();",
            "  }",
            "  public void m() {",
            "    new Buggy() {};",
            "    class LocalBuggy extends Buggy {",
            "      @JsConstructor",
            "      public LocalBuggy() {}",
            "    }",
            "  }",
            "}",
            "class SubBuggy extends Buggy {",
            "  public SubBuggy() { this(1);}",
            "  @JsConstructor",
            "  public SubBuggy(int a) { super();}",
            "}",
            "@JsType",
            "class JsSubBuggy extends Buggy {",
            "  @JsIgnore",
            "  public JsSubBuggy() { this(1);}",
            "  public JsSubBuggy(int a) { super();}",
            "}",
            "@JsType (isNative = true)",
            "class NativeBuggy {",
            "  public NativeBuggy() {}",
            "  public NativeBuggy(int a) {}",
            "}",
            "@JsType (isNative = true)",
            "class NativeSubNativeBuggy extends NativeBuggy {",
            "  public NativeSubNativeBuggy() { super(1); }",
            "  public NativeSubNativeBuggy(int a) { super();}",
            "}",
            "class SubNativeBuggy extends NativeBuggy {",
            "  @JsConstructor",
            "  public SubNativeBuggy() { super(1);}",
            "  public SubNativeBuggy(int a) { this(); }",
            "}",
            "@JsType",
            "class ExplicitSubImplicitSuper extends ImplicitSubNativeSuper {",
            "  public ExplicitSubImplicitSuper() { super(); }",
            "}")
        .addCompilationUnit(
            "test.ImplicitConstructor",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class ImplicitConstructor extends Buggy {",
            "}")
        .addCompilationUnit(
            "test.SubImplicitConstructor",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class SubImplicitConstructor extends ImplicitConstructor {",
            "}")
        .addCompilationUnit(
            "test.ImplicitSubImplicitSuper",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class ImplicitSubImplicitSuper extends ImplicitSubNativeSuper {",
            "}")
        .addCompilationUnit(
            "test.ImplicitSubNativeSuper",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class ImplicitSubNativeSuper extends NativeBuggy {",
            "}")
        .addCompilationUnit(
            "test.ImplicitSubExplicitSuper",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class ImplicitSubExplicitSuper extends ExplicitSubImplicitSuper {",
            "}")
        .assertTranspileSucceeds();
  }

  public void testJsConstructorBadSubclassFails() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "class BuggyJsType {",
            "  public BuggyJsType() {}",
            "  @JsIgnore",
            "  public BuggyJsType(int a) { this(); }",
            "  public void m() {",
            // Error: Anonymous subclass delegating to the wrong constructor.
            "    new BuggyJsType(2) {};",
            "    class LocalBuggy extends BuggyJsType {",
            // Error: Local subclass delegating to the wrong constructor.
            "      @JsConstructor",
            "      public LocalBuggy() { super(3); }",
            "    }",
            "  }",
            "}",
            "public class Buggy extends BuggyJsType {",
            // Error: no JsConstructor.
            "  public Buggy() {}",
            "  public Buggy(int a) { super(a); }",
            "}",
            "class SubBuggyJsType extends BuggyJsType {",
            // Correct: one JsConstructor delegating to the super JsConstructor.
            "  public SubBuggyJsType() { this(1); }",
            "  @JsConstructor",
            "  public SubBuggyJsType(int a) { super(); }",
            "}",
            "class BadImplicitConstructor extends SubBuggyJsType {",
            // Error: Implicit constructor is not a JsConstructor.
            "}",
            "class DelegatingToImplicitConstructor extends ImplicitConstructor {",
            // Error: Non JsConstructor delegating to implicit JsConstructor.
            "  public DelegatingToImplicitConstructor(int a) { }",
            "}",
            "class SubSubBuggyJsType extends SubBuggyJsType {",
            // Error: JsConstructor delegating to a non JsConstructor.
            "  public SubSubBuggyJsType() { this(1);}",
            "  @JsConstructor",
            "  public SubSubBuggyJsType(int a) { super(); }",
            "}",
            "class OtherSubBuggyJsType extends BuggyJsType {",
            // Error: JsConstructor not delegating to super primary constructor.
            "  public OtherSubBuggyJsType() { super();}",
            "  @JsConstructor",
            "  public OtherSubBuggyJsType(int a) { this(); }",
            "}",
            "class AnotherSubBuggyJsType extends BuggyJsType {",
            // Error: Multiple JsConstructors in JsConstructor subclass.
            "  @JsConstructor",
            "  public AnotherSubBuggyJsType() { super();}",
            "  @JsConstructor",
            "  public AnotherSubBuggyJsType(int a) { this(); }",
            "}",
            "class DelegatingConstructorChain extends BuggyJsType {",
            // TODO(b/129550499): The following class should be fine because only the JsConstructor
            // delegates to super. However the normalization pass does not handle those cases
            // correctly.
            "  @JsConstructor",
            "  public DelegatingConstructorChain() { super();}",
            "  public DelegatingConstructorChain(int a) { this(); }",
            "  public DelegatingConstructorChain(int a, int b) { this(a); }",
            "}",
            "@JsType(isNative=true) class NativeType {",
            "  NativeType() { }",
            "  NativeType(int i) { }",
            "}",
            "class SomeClass2 extends NativeType {",
            "}")
        .addCompilationUnit(
            "test.BadImplicitJsConstructor",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class BadImplicitJsConstructor extends SubBuggyJsType {",
            // Error: Implicit constructor delegates to the wrong super constructor.
            "}")
        .addCompilationUnit(
            "test.ImplicitConstructor",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class ImplicitConstructor extends BuggyJsType {",
            // Correct: Implicit constructor delegating to the JsConstructor.
            "}")
        .addCompilationUnit(
            "test.ImplicitJsConstructor",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class ImplicitJsConstructor extends BuggyJsType {",
            // Error: implicit JsConstructor delegating implicitly to a non JsConstructor.
            "}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "JsConstructor '<anonymous> extends BuggyJsType(int)' can only delegate to super "
                + "JsConstructor 'BuggyJsType()'.",
            "JsConstructor 'LocalBuggy()' can only delegate to super JsConstructor "
                + "'BuggyJsType()'.",
            "Class 'Buggy' should have a JsConstructor.",
            "Class 'BadImplicitConstructor' should have a JsConstructor.",
            "Implicit JsConstructor 'BadImplicitJsConstructor()' can only delegate to "
                + "super JsConstructor 'SubBuggyJsType(int)'.",
            "Class 'DelegatingToImplicitConstructor' should have a JsConstructor.",
            "JsConstructor 'SubSubBuggyJsType(int)' can only delegate to super "
                + "JsConstructor 'SubBuggyJsType(int)'.",
            "More than one JsConstructor exists for 'AnotherSubBuggyJsType'.",
            "JsConstructor 'OtherSubBuggyJsType(int)' can be a JsConstructor only if all "
                + "other constructors in the class delegate to it.",
            "Class 'SomeClass2' should have a JsConstructor.",
            "Constructor 'DelegatingConstructorChain(int a, int b)' should delegate to the"
                + " JsConstructor 'DelegatingConstructorChain()'. (b/129550499)");
  }

  public void testMultipleConstructorsNotAllDelegatedToJsConstructorFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class Buggy {",
            "  public Buggy() {}",
            "  private Buggy(int a) {",
            "    new Buggy();",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsConstructor 'Buggy()' can be a JsConstructor only if all other constructors "
                + "in the class delegate to it.");
  }

  public void testMultipleJsConstructorsFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class Buggy {",
            "  public Buggy() {}",
            "  public Buggy(int a) {",
            "    this();",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition("More than one JsConstructor exists for 'Buggy'.");
  }

  public void testNonCollidingAccidentalOverrideSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface Foo {",
            "  void doIt(Object foo);",
            "}",
            "class ParentParent {",
            "  public void doIt(String x) {}",
            "}",
            "@JsType",
            "class Parent extends ParentParent {",
            "  Parent() {}",
            "  public void doIt(Object x) {}",
            "}",
            "public class Buggy extends Parent implements Foo {}")
        .assertNoWarnings();
  }

  public void testJsNameInvalidNamesFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(name = \"a.b.c\") public class Buggy {",
            "   @JsMethod(name = \"34s\") public void m() {}",
            "   @JsProperty(name = \"s^\") public int  m;",
            "   @JsProperty(name = \"\") public int n;",
            "   @JsMethod(name = \"a.b\") static void o() {}",
            "   @JsMethod(name = \"\") static void p() {}",
            "   @JsMethod(name = \"\") void q() {}",
            "   @JsMethod(name = \"\") static native void r();",
            "   @JsMethod(namespace = \"34s\", name = \"\") static native void s();",
            "   @JsProperty(name = \"a.c\") static int t;",
            "   @JsProperty(name = \"\") int u;",
            "   @JsProperty(namespace =\"a.b.c\", name = \"\") int v;",
            "   @JsProperty(namespace = \"34s\", name = \"\") static native int w();",
            "}",
            "@JsType(namespace = JsPackage.GLOBAL, name = \"a.b.d\")",
            "class OtherBuggy { }",
            "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"*\")",
            "class BadGlobalStar { }",
            "@JsType(namespace = JsPackage.GLOBAL, name = \"?\") interface BadGlobalWildcard { }",
            "@JsType(isNative = true, namespace = \"a.b\", name = \"*\") interface BadStar { }",
            "@JsEnum(isNative = true, namespace = \"a.b\", name = \"=\") enum NativeEnum { }",
            "@JsEnum(namespace = \"a.b\", name = \"c.d\") enum MyJsEnum { }")
        .assertErrorsWithoutSourcePosition(
            "'Buggy' has invalid name 'a.b.c'.",
            "'void Buggy.m()' has invalid name '34s'.",
            "'Buggy.m' has invalid name 's^'.",
            "'Buggy.n' cannot have an empty name.",
            "'void Buggy.o()' has invalid name 'a.b'.",
            "'void Buggy.p()' cannot have an empty name.",
            "'void Buggy.q()' cannot have an empty name.",
            "'void Buggy.r()' cannot have an empty name.",
            "'void Buggy.s()' has invalid namespace '34s'.",
            "'Buggy.t' has invalid name 'a.c'.",
            "'Buggy.u' cannot have an empty name.",
            "'Buggy.v' cannot have an empty name.",
            "'int Buggy.w()' has invalid namespace '34s'.",
            "'OtherBuggy' has invalid name 'a.b.d'.",
            "'NativeEnum' has invalid name '='.",
            "'MyJsEnum' has invalid name 'c.d'.",
            "Only native interfaces in the global namespace can be named '*'.",
            "Only native interfaces in the global namespace can be named '?'.",
            "Only native interfaces in the global namespace can be named '*'.");
  }

  public void testJsNameEmptyNamesSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "   @JsMethod(namespace = \"foo.buzz\", name = \"\") static native void buzz();",
            "   @JsProperty(namespace = \"foo.bar\", name = \"\") static native String bar();",
            "}")
        .assertNoWarnings();
  }

  public void testJsNameInvalidNamespacesFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType class Super {",
            "   @JsMethod(name = \"custom\") public void r() {}",
            "}",
            "@JsType(namespace = \"a.b.\") public class Buggy extends Super {",
            "   @JsMethod(namespace = \"34s\") public native static void m();",
            "   @JsMethod(namespace = \"\") public native static void o();",
            "   @JsProperty(namespace = \"\") public int p;",
            "   @JsMethod(namespace = \"a\") public void q() {}",
            "   @JsMethod(namespace = \"b\") public void r() {}",
            "}",
            "@JsType(isNative = true) class NativeClass {",
            "   @JsProperty(namespace = \"s^\") public static int  n;",
            "}",
            "@JsType(namespace = \"<window>\") class JsTypeOnWindow {",
            "   @JsProperty(namespace = \"<window>\") public static int r;",
            "   @JsMethod(namespace = \"<window>\") public static  void s() {}",
            "}",
            "@JsType(namespace = JsPackage.GLOBAL) class InvalidGlobal {",
            "   @JsMethod(namespace = JsPackage.GLOBAL) public static void m() {}",
            "   @JsProperty(namespace = JsPackage.GLOBAL) public static int n;",
            "}",
            "@JsType(namespace = \"\") class NonNativeClass {}",
            "@JsEnum(isNative = true, namespace = \"=\") enum NativeEnum { }",
            "@JsEnum(namespace = \"^\") enum MyJsEnum { }")
        .assertErrorsWithoutSourcePosition(
            "'Buggy' has invalid namespace 'a.b.'.",
            "'NativeEnum' has invalid namespace '='.",
            "'MyJsEnum' has invalid namespace '^'.",
            "'void Buggy.m()' has invalid namespace '34s'.",
            "'void Buggy.o()' cannot have an empty namespace.",
            "Instance member 'Buggy.p' cannot declare a namespace.",
            "Instance member 'void Buggy.q()' cannot declare a namespace.",
            "Instance member 'void Buggy.r()' cannot declare a namespace.",
            "'NativeClass.n' has invalid namespace 's^'.",
            "Non-native member 'JsTypeOnWindow.r' cannot declare a namespace.",
            "Non-native member 'void JsTypeOnWindow.s()' cannot declare a namespace.",
            "Non-native member 'void InvalidGlobal.m()' cannot declare a namespace.",
            "Non-native member 'InvalidGlobal.n' cannot declare a namespace.",
            "'NonNativeClass' cannot have an empty namespace.");
  }

  public void testJsNameGlobalNamespacesSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(namespace = JsPackage.GLOBAL) public class Buggy {",
            "   @JsMethod(namespace = JsPackage.GLOBAL) public static native void m();",
            "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.b\")",
            "   public static native void o();",
            "}",
            "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"a.c\")",
            "class NativeOnGlobalNamespace {",
            "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.d\") static native void o();",
            "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.e\") static native void getP();",
            "   @JsProperty(namespace = JsPackage.GLOBAL, name = \"a.f\") public static int n;",
            "   @JsProperty(namespace = JsPackage.GLOBAL) public static int m;",
            "}",
            "@JsType(isNative = true, namespace = \"<window>\", name = \"a.g\")",
            "class NativeOnWindowNamespace {",
            "   @JsMethod(namespace = \"<window>\", name = \"a.h\") static native void q();",
            "   @JsMethod(namespace = \"<window>\", name = \"a.i\") static native void getR();",
            "   @JsProperty(namespace = \"<window>\", name = \"a.j\") public static int s;",
            "}",
            "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"*\")",
            "interface Star {",
            "}",
            "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"?\")",
            "interface Wildcard {",
            "}")
        .assertNoWarnings();
  }

  public void testNativeJsTypeEmptyNamespaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true, namespace = \"\", name = \"a.c\")",
            "class NativeOnTopLevelNamespace {}")
        .assertNoWarnings();
  }

  public void testSingleJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class Buggy {",
            "  public static void show1() {}",
            "  public void show2() {}",
            "}")
        .assertNoWarnings();
  }

  public void testJsFunctionSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsFunction",
            "interface Function {",
            "  int getFoo();",
            "  @JsOverlay",
            "  static String s = new String();",
            "  @JsOverlay",
            "  default void m() {}",
            "  @JsOverlay",
            "  static void n() {}",
            "}",
            "public final class Buggy implements Function {",
            "  public int getFoo() { return 0; }",
            "  public final void blah() {}",
            "  public void blat() {}",
            "  private void bleh() {}",
            "  static void blet() {",
            "    new Function() {",
            "       public int getFoo() { return 0; }",
            "    }.getFoo();",
            "  }",
            "  {}",
            "  static {}",
            "  String x = new String();",
            "  static int y;",
            "}",
            "@JsFunction",
            "interface Function2 {",
            "  Object getFoo();",
            "}",
            "final class Buggy2 implements Function2 {",
            "  public String getFoo() { return null;}",
            "}",
            "@JsFunction",
            "interface ParametricFunction<T> {",
            "  Object getFoo(T t);",
            "}",
            "final class ImplementationWithBridge implements ParametricFunction<String> {",
            "  public Double getFoo(String s) { return new Double(0); }",
            "}",
            "class Main {",
            "  public static void main() {",
            "    Object o;",
            "    o = (ParametricFunction<?>) (s) -> (Double) null;",
            "  }",
            "}")
        .assertNoWarnings();
  }

  public void testJsFunctionFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "import java.util.List;",
            "@JsFunction",
            "interface Function {",
            "  int getFoo();",
            "}",
            "public final class Buggy implements Function {",
            "  @JsConstructor",
            "  Buggy() { }",
            "  @JsProperty",
            "  public int getFoo() { return 0; }",
            "  @JsMethod",
            "  private void bleh() {}",
            "  @JsMethod",
            "  private native void nativeMethod();",
            "  @JsProperty",
            "  public int prop = 0;",
            "  public String toString() { return \"\"; }",
            "  public boolean equals(Object o) { return false; }",
            "  public int hashCode() { return 0; }",
            "}",
            "@JsFunction",
            "interface InvalidFunction {",
            "  @JsProperty",
            "  int getFoo();",
            "  default void m() {}",
            "  int f = 0;",
            "  static void n() {}",
            "}",
            "class NonFinalJsFunction implements Function {",
            "  public int getFoo() { return 0; }",
            "}",
            "@JsType",
            "final class JsFunctionMarkedAsJsType implements Function {",
            "  public int getFoo() { return 0; }",
            "}",
            "@JsFunction",
            "interface JsFunctionExtendsInterface extends Cloneable {",
            "  void foo();",
            "}",
            "interface InterfaceExtendsJsFunction extends Function {}",
            "class BaseClass { { if (new Object() instanceof Buggy) {} }}",
            "final class JsFunctionExtendingBaseClass extends BaseClass implements Function {",
            "  public int getFoo() { return 0; }",
            "}",
            "final class JsFunctionMultipleInterfaces implements Cloneable, Function {",
            "  public int getFoo() { return 0; }",
            "}",
            "@JsFunction @JsType",
            "interface InvalidJsTypeJsFunction {",
            "  void n();",
            "}",
            "@JsFunction",
            "class InvalidJsFunctionClass {",
            "}",
            "interface Foo {",
            "  int getFoo();",
            "}",
            "final class JsFunctionImplementingDefaultMethod implements Foo, Function {",
            "  public int getFoo() { return 0; }",
            "}",
            "@JsFunction",
            "interface FunctionWithDefaultMethod {",
            "  default int getFoo() { return 0; }",
            "}",
            "abstract class AbstractFunctionImplementation implements Function {",
            "}",
            "@JsFunction",
            "interface MutuallyRecursiveFunctionA {",
            "  void m(MutuallyRecursiveFunctionB f);",
            "}",
            "@JsFunction",
            "interface MutuallyRecursiveFunctionB {",
            "  MutuallyRecursiveFunctionA m();",
            "}",
            "@JsFunction",
            "interface WithoutError {",
            "  void m(IndirectReference f);",
            "}",
            "@JsFunction",
            "interface IndirectReference {",
            "  void m(List<List<IndirectReference>> f);",
            "}",
            "@JsFunction",
            "interface JsFunctionNotInvolvedInCycle<T> {",
            "  void m(T t);",
            "}",
            "@JsFunction",
            "interface IndirectReferenceThroughJsFunction {",
            "  void m(JsFunctionNotInvolvedInCycle<IndirectReferenceThroughJsFunction> f);",
            "}",
            "@JsFunction",
            "interface ArrayReference {",
            "  void m(IndirectReference[] f);",
            "}",
            "@JsFunction",
            "interface TypeVariableReference<T extends TypeVariableReference<T>> {",
            "  void m(T f);",
            "}",
            "@JsFunction",
            "interface TypeVariableWithIntersectionBound<",
            "    T extends Comparable<T> & TypeVariableWithIntersectionBound<T>> {",
            "  void m(T f);",
            "}",
            "@JsFunction interface JsFunctionWithMethodDefinedTypeVariable {",
            "  <T> void m();",
            "}",
            "class Main {",
            "  public static void main() {",
            "    Object o;",
            "    o = (Foo & Function) () -> 0;",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'InvalidJsTypeJsFunction' cannot be both a JsFunction and a JsType at the same time.",
            "JsFunction 'InvalidJsFunctionClass' has to be a functional interface.",
            "JsFunction implementation 'NonFinalJsFunction' must be final.",
            "'JsFunctionMarkedAsJsType' cannot be both a JsFunction implementation and "
                + "a JsType at the same time.",
            "JsFunction 'JsFunctionExtendsInterface' cannot extend other interfaces.",
            "'InterfaceExtendsJsFunction' cannot extend JsFunction 'Function'.",
            "JsFunction implementation 'JsFunctionExtendingBaseClass' cannot extend a class.",
            "JsFunction implementation 'JsFunctionMultipleInterfaces' cannot implement more than"
                + " one interface.",
            "Cannot do instanceof against JsFunction implementation 'Buggy'.",
            "JsFunction interface 'InvalidFunction' cannot declare non-JsOverlay"
                + " member 'void InvalidFunction.m()'.",
            "JsFunction interface 'InvalidFunction' cannot declare non-JsOverlay"
                + " member 'InvalidFunction.f'.",
            "JsFunction interface 'InvalidFunction' cannot declare non-JsOverlay"
                + " member 'void InvalidFunction.n()'.",
            "JsFunction implementation member 'int Buggy.getFoo()' cannot be "
                + "JsMethod nor JsProperty nor JsConstructor.",
            "JsFunction implementation member 'void Buggy.bleh()' cannot be"
                + " JsMethod nor JsProperty nor JsConstructor.",
            "JsFunction implementation method 'void Buggy.nativeMethod()' cannot be native.",
            "JsFunction implementation member 'Buggy.prop' cannot be JsMethod nor JsProperty "
                + "nor JsConstructor.",
            "JsFunction implementation member 'int JsFunctionMarkedAsJsType.getFoo()' cannot be "
                + "JsMethod nor JsProperty nor JsConstructor.",
            "JsFunction implementation method 'String Buggy.toString()' cannot override a "
                + "supertype method.",
            "JsFunction implementation method 'boolean Buggy.equals(Object)' cannot override a "
                + "supertype method.",
            "JsFunction implementation method 'int Buggy.hashCode()' cannot override a supertype "
                + "method.",
            "JsFunction interface member 'int InvalidFunction.getFoo()' cannot be JsMethod "
                + "nor JsProperty nor JsConstructor.",
            "JsFunction interface member 'void InvalidJsTypeJsFunction.n()' cannot be JsMethod "
                + "nor JsProperty nor JsConstructor.",
            "JsFunction implementation 'JsFunctionImplementingDefaultMethod' cannot implement more "
                + "than one interface.",
            "JsFunction 'FunctionWithDefaultMethod' has to be a functional interface.",
            "JsFunction implementation member 'Buggy()' cannot be JsMethod nor JsProperty "
                + "nor JsConstructor.",
            "JsFunction implementation 'AbstractFunctionImplementation' must be final.",
            "JsFunction lambda can only implement the JsFunction interface.",
            "JsFunction 'void MutuallyRecursiveFunctionA.m(MutuallyRecursiveFunctionB f)' cannot "
                + "refer recursively to itself "
                + "(via MutuallyRecursiveFunctionA MutuallyRecursiveFunctionB.m()) (b/153591461).",
            "JsFunction 'MutuallyRecursiveFunctionA MutuallyRecursiveFunctionB.m()' cannot refer "
                + "recursively to itself "
                + "(via void MutuallyRecursiveFunctionA.m(MutuallyRecursiveFunctionB)) "
                + "(b/153591461).",
            "JsFunction 'void IndirectReference.m(List<List<IndirectReference>> f)' cannot refer"
                + " recursively to itself (b/153591461).",
            "JsFunction 'void TypeVariableReference.m(T f)' cannot refer recursively to itself "
                + "(b/153591461).",
            "JsFunction 'void TypeVariableWithIntersectionBound.m(T f)' cannot refer recursively"
                + " to itself (b/153591461).",
            "JsFunction 'void IndirectReferenceThroughJsFunction.m("
                + "JsFunctionNotInvolvedInCycle<IndirectReferenceThroughJsFunction> f)' cannot "
                + "refer recursively to itself (b/153591461).",
            "JsFunction 'void JsFunctionWithMethodDefinedTypeVariable.m()' cannot declare type"
                + " parameters. Type parameters must be declared on the enclosing interface"
                + " instead.");
  }

  public void testNativeJsTypeStaticInitializerSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  static {",
            "    int x = 1;",
            "  }",
            "}",
            "@JsType(isNative = true)",
            "class Buggy2 {",
            "  static {",
            "    Object.class.getName();",
            "  }",
            "}")
        .assertNoWarnings();
  }

  public void testNativeJsTypeInstanceInitializerFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  {",
            "    Object.class.getName();",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Native JsType 'Buggy' cannot have an instance initializer.");
  }

  public void testNativeJsTypeNonEmptyConstructorFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  public Buggy(int n) {",
            "    n++;",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Native JsType constructor 'Buggy(int n)' cannot have non-empty method body.");
  }

  public void testNativeJsTypeImplicitSuperSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy extends Super {",
            "  public Buggy(int n) {}",
            "}")
        .addCompilationUnit(
            "test.Super",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Super {",
            "  public Super() {}",
            "}")
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testNativeJsTypeExplicitSuperSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy extends Super {",
            "  public Buggy(int n) {",
            "    super(n);",
            "  }",
            "}")
        .addCompilationUnit(
            "test.Super",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Super {",
            "  public Super(int x) {}",
            "}")
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testNativeJsTypeExplicitSuperWithEffectSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy extends Super {",
            "  public Buggy(int n) {",
            "    super(n++);",
            "  }",
            "}")
        .addCompilationUnit(
            "test.Super",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Super {",
            "  public Super(int x) {}",
            "}")
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testJsTypeInterfaceInInstanceofFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) interface IBuggy {}",
            "@JsType public class Buggy {",
            "  public Buggy() { if (new Object() instanceof IBuggy) {} }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Cannot do instanceof against native JsType interface 'IBuggy'.");
  }

  public void testNativeJsTypeEnumFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public enum Buggy {",
            "  A,",
            "  B",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Enum 'Buggy' cannot be a native JsType. Use '@JsEnum(isNative = true)' instead.");
  }

  public void testJsEnumFails() {
    assertTranspileFails(
            "test.MyJsEnum",
            "import java.util.function.*;",
            "import jsinterop.annotations.*;",
            "@JsEnum",
            "public enum MyJsEnum {",
            "  A,",
            "  B {},",
            "  C(1);",
            "  {}",
            "  MyJsEnum( ) { }",
            "  MyJsEnum(int x) { }",
            "  static void main() {",
            "    Supplier<String> s = A::name;",
            "    MyJsEnum.values();",
            "    MyJsEnum.valueOf(null);",
            // TODO(b/132736149): make sure the following statement is rejected.
            "    EnumList<MyJsEnum> myJsEnumList = null;",
            "  }",
            "  int value = 5;",
            "  int instanceField;",
            "  @JsOverlay",
            "  public final void anOverlayMethod() {",
            "    super.ordinal();",
            "  }",
            "  public final void aMethod() {",
            "    super.name();",
            "  }",
            "}",
            "@JsEnum @JsType",
            "enum MyJsTypeJsEnum { }",
            "@JsEnum",
            "interface InterfaceWithJsEnum { }",
            "interface MyInterface { }",
            "@JsEnum",
            "enum JsEnumImplementingInterface implements MyInterface { }",
            "@JsEnum",
            "enum JsEnumWithInvalidMembers {",
            "  A;",
            "  @JsMethod static void m() {}",
            "  @JsProperty static int getP() { return 1;}",
            "  static native void n();",
            "  native void o();",
            "  public String toString() { return null; }",
            "}",
            "class AListSubclass<T extends MyJsEnum> {}",
            "interface EnumList<E extends Enum<E>> {}")
        .assertErrorsWithoutSourcePosition(
            "Non-custom-valued JsEnum 'MyJsEnum' cannot have constructor 'MyJsEnum()'.",
            "Non-custom-valued JsEnum 'MyJsEnum' cannot have constructor 'MyJsEnum(int x)'.",
            "Non-custom-valued JsEnum 'MyJsEnum' cannot have a field named 'value'.",
            "JsOverlay 'void MyJsEnum.anOverlayMethod()' can only be declared in a native type"
                + " or @JsFunction interface.",
            "JsEnum constant 'MyJsEnum.B' cannot have a class body.",
            // From (A::name) in MyJsEnum.main()
            "JsEnum 'MyJsEnum' does not support 'String Enum.name()'.",
            // From (super.name) in MyJsEnum.aMethod()
            "JsEnum 'MyJsEnum' does not support 'String Enum.name()'.",
            "JsEnum 'MyJsEnum' does not support 'MyJsEnum[] MyJsEnum.values()'. (b/118228329)",
            "JsEnum 'MyJsEnum' does not support 'MyJsEnum MyJsEnum.valueOf(String)'.",
            "'MyJsTypeJsEnum' cannot be both a JsEnum and a JsType at the same time.",
            "JsEnum 'InterfaceWithJsEnum' has to be an enum type.",
            "JsEnum 'JsEnumImplementingInterface' cannot implement any interface.",
            "JsEnum member 'void JsEnumWithInvalidMembers.m()' cannot be "
                + "JsMethod nor JsProperty nor JsConstructor.",
            "JsEnum member 'int JsEnumWithInvalidMembers.getP()' cannot be "
                + "JsMethod nor JsProperty nor JsConstructor.",
            "JsEnum method 'void JsEnumWithInvalidMembers.n()' cannot be native.",
            "[unusable-by-js] Native 'void JsEnumWithInvalidMembers.n()' is exposed to JavaScript "
                + "without @JsMethod.",
            "JsEnum method 'void JsEnumWithInvalidMembers.o()' cannot be native.",
            "[unusable-by-js] Native 'void JsEnumWithInvalidMembers.o()' is exposed to JavaScript "
                + "without @JsMethod.",
            "JsEnum method 'String JsEnumWithInvalidMembers.toString()' cannot override a"
                + " supertype method.",
            "JsEnum 'MyJsEnum' cannot have instance field 'MyJsEnum.instanceField'.",
            "JsEnum 'MyJsEnum' cannot have an instance initializer.",
            "Type 'AListSubclass' cannot define a type variable with a JsEnum as a bound.",
            "Cannot use 'super' in JsOverlay method 'void MyJsEnum.anOverlayMethod()'.",
            "Cannot use 'super' in JsEnum method 'void MyJsEnum.aMethod()'.");
  }

  public void testJsEnumSucceeds() {
    assertTranspileSucceeds(
            "test.MyJsEnum",
            "import jsinterop.annotations.*;",
            "import java.util.function.*;",
            "@JsEnum",
            "public enum MyJsEnum {",
            "  A,",
            "  B;",
            "  static void main() {",
            "    A.ordinal();",
            "    A.compareTo(B);",
            "    if (((MyJsEnum) A) instanceof MyJsEnum) {}",
            "    Object o = MyJsEnum.class;",
            "    Consumer<MyJsEnum> consumer = c -> c.ordinal();",
            "    Consumer<? super MyJsEnum> consumer2 = c -> c.ordinal();",
            "    Supplier<? extends MyJsEnum> supplier = () -> MyJsEnum.A;",
            "  }",
            "  static int field = 5;",
            "  static { }",
            "  public static int aStaticField;",
            "  public void anInstanceMethod() { }",
            "  public static void aStaticMethod() { }",
            "}",
            "interface List<T> { Object getObject(); }",
            "class AList<T> { }",
            "class AListSubclass extends AList<MyJsEnum> implements List<MyJsEnum> {",
            "    public MyJsEnum getObject() { return null; }",
            "}")
        .assertNoWarnings();
  }

  public void testCustomValuedJsEnumFails() {
    assertTranspileFails(
            "test.CustomValued",
            "import jsinterop.annotations.*;",
            "@JsEnum(hasCustomValue = true)",
            "public enum CustomValued {",
            "  A(1),",
            "  B(false);",
            "  @JsProperty int value;",
            "  CustomValued(int value) { this.value = value; }",
            "  CustomValued(boolean value) { }",
            "  static void main() {",
            "    A.name();",
            "    B.ordinal();",
            "    A.compareTo(B);",
            "    CustomValued.values();",
            "    CustomValued.valueOf(null);",
            "  }",
            "}",
            "@JsEnum(hasCustomValue = true)",
            "enum InvalidCustomValueType {",
            "  A(1L);",
            "  long value;",
            "  InvalidCustomValueType(long value) { this.value = value; }",
            "}",
            "@JsEnum(hasCustomValue = true)",
            "enum InvalidCustomValueType2 {",
            "  A(false);",
            "  boolean value;",
            "  InvalidCustomValueType2(boolean value) { this.value = value; }",
            "}",
            "@JsEnum(hasCustomValue = true)",
            "enum InvalidCustomValueInitializer {",
            "  A(\"  \".length());",
            "  int value = 5;",
            "  InvalidCustomValueInitializer(int value) { this.value = value; }",
            "}",
            "@JsEnum(hasCustomValue = true)",
            "enum CustomValueWithIntegerMinValue {",
            "  A(Integer.MIN_VALUE),",
            "  B(-2147483648),",
            "  C(Integer.MIN_VALUE + 1 - 1);",
            "  int value;",
            "  CustomValueWithIntegerMinValue(int value) { this.value = value; }",
            "}",
            "@JsEnum(hasCustomValue = true)",
            "enum InvalidCustomValueConstructor {",
            "  A(1);",
            "  int value;",
            "  InvalidCustomValueConstructor(int value) { this.value = value + 2; }",
            "}",
            "@JsEnum(hasCustomValue = true)",
            "enum CustomValueMissingConstructor {",
            "  A;",
            "  static int value;",
            "}",
            "@JsEnum(hasCustomValue = true)",
            "enum CustomValueWithJsConstructor {",
            "  A(0);",
            "  int value;",
            "  @JsConstructor CustomValueWithJsConstructor(int value) { this.value = value; }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Custom-valued JsEnum constructor 'CustomValued(boolean value)' should have one "
                + "parameter of the value type and its body should only be the assignment to the "
                + "value field.",
            "Custom-valued JsEnum constructor 'InvalidCustomValueConstructor(int value)' should "
                + "have one parameter of the value type and its body should only be the assignment "
                + "to the value field.",
            "Custom-valued JsEnum value field 'CustomValued.value' cannot be static nor "
                + "JsOverlay nor JsMethod nor JsProperty.",
            "Custom-valued JsEnum value field 'InvalidCustomValueType.value' cannot have the type"
                + " 'long'. The only valid value types are 'int' and 'java.lang.String'."
                + " (b/295240966)",
            "Custom-valued JsEnum value field 'InvalidCustomValueType2.value' cannot have the type"
                + " 'boolean'. The only valid value types are 'int' and 'java.lang.String'."
                + " (b/295240966)",
            "Custom-valued JsEnum value field 'InvalidCustomValueInitializer.value' cannot "
                + "have initializer.",
            "Custom-valued JsEnum 'CustomValueMissingConstructor' should have a constructor.",
            "Custom-valued JsEnum value field 'CustomValueMissingConstructor.value' cannot be "
                + "static nor JsOverlay nor JsMethod nor JsProperty.",
            "Custom-valued JsEnum constant 'InvalidCustomValueInitializer.A' cannot have a "
                + "non-literal value.",
            "Custom-valued JsEnum constant 'CustomValueWithIntegerMinValue.A' cannot be equal to"
                + " Integer.MIN_VALUE.",
            "Custom-valued JsEnum constant 'CustomValueWithIntegerMinValue.B' cannot be equal to"
                + " Integer.MIN_VALUE.",
            "Custom-valued JsEnum constant 'CustomValueWithIntegerMinValue.C' cannot be equal to"
                + " Integer.MIN_VALUE.",
            "JsEnum 'CustomValued' does not support 'String Enum.name()'.",
            "Custom-valued JsEnum 'CustomValued' does not support 'int Enum.ordinal()'.",
            "Custom-valued JsEnum 'CustomValued' does not support "
                + "'int Enum.compareTo(CustomValued)'.",
            "JsEnum 'CustomValued' does not support "
                + "'CustomValued[] CustomValued.values()'. "
                + "(b/118228329)",
            "JsEnum 'CustomValued' does not support "
                + "'CustomValued CustomValued.valueOf(String)'.",
            "JsEnum member 'CustomValueWithJsConstructor(int value)' cannot be "
                + "JsMethod nor JsProperty nor JsConstructor.");
  }

  public void testCustomValuedJsEnumSucceeds() {
    assertTranspileSucceeds(
            "test.CustomValued",
            "import jsinterop.annotations.*;",
            "@JsEnum(hasCustomValue = true)",
            "public enum CustomValued {",
            "  A(1),",
            "  B(-1),",
            "  C(0),",
            "  D(1 + 1);",
            "  final int value;",
            "  CustomValued(int value) { this.value = value;}",
            "}")
        .assertNoWarnings();
  }

  public void testNativeJsEnumFails() {
    assertTranspileFails(
            "test.Native",
            "import jsinterop.annotations.*;",
            "@JsEnum(isNative = true)",
            "public enum Native {",
            "  A,",
            "  B;",
            "  static void main() {",
            "    A.name();",
            "    B.ordinal();",
            "    A.compareTo(B);",
            "    Native.values();",
            "    Native.valueOf(null);",
            "    if (A instanceof Native) { }",
            "    Object o = Native.class;",
            "  }",
            "  static int staticField = 1;",
            "}",
            "@JsType(isNative = true)",
            "interface NativeInterface { }",
            "@JsEnum(isNative = true)",
            "enum NativeJsEnumImplementingNativeInterface implements NativeInterface { }",
            "@JsEnum(isNative = true, hasCustomValue = true)",
            "enum NativeJsEnumDeclaringCustomValueButNoField { }",
            "@JsEnum(isNative = true)",
            "enum NativeJsEnumNotDeclaringCustomValueButWithValueField {",
            "  A,",
            "  B;",
            "  int value = 1;",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsEnum 'Native' does not support 'String Enum.name()'.",
            "Native JsEnum 'Native' does not support 'int Enum.ordinal()'.",
            "JsEnum 'Native' does not support 'Native[] Native.values()'. (b/118228329)",
            "JsEnum 'Native' does not support 'Native Native.valueOf(String)'.",
            "JsEnum 'NativeJsEnumImplementingNativeInterface' cannot implement any interface.",
            "Native JsEnum 'Native' cannot declare non-JsOverlay member 'void Native.main()'.",
            "Native JsEnum 'Native' cannot declare non-JsOverlay member 'Native.staticField'.",
            "Custom-valued JsEnum 'NativeJsEnumDeclaringCustomValueButNoField' does not have a"
                + " field named 'value'.",
            "Non-custom-valued JsEnum 'NativeJsEnumNotDeclaringCustomValueButWithValueField' "
                + "cannot have a field named 'value'.",
            "Cannot do instanceof against native JsEnum 'Native'.",
            "Cannot use native JsEnum literal 'Native.class'.");
  }

  public void testNativeJsEnumSucceeds() {
    assertTranspileSucceeds(
            "test.Native",
            "import jsinterop.annotations.*;",
            "@JsEnum(isNative = true)",
            "public enum Native {",
            "  A,",
            "  B;",
            "  @JsOverlay",
            "  public void anOverlayMethod() { ",
            "    A.compareTo(B);",
            "  }",
            "}",
            "@JsEnum(isNative = true, hasCustomValue = true)",
            "enum NativeJsEnumWithCustomValue {",
            "  A,",
            "  B;",
            "  int value;",
            "}",
            "interface List<T> { Object getObject(); }",
            "class AList<Native> { }",
            "class AListSubclass<T extends Native>",
            "    extends AList<Native> implements List<Native>  {",
            "    public Native getObject() { return null; }",
            "}",
            "interface Consumer<T> {",
            "  void accept(T t);",
            "  static void test() {",
            "    Consumer<Native> consumer = c -> c.toString();",
            "  }",
            "}")
        .assertNoWarnings();
  }

  public void testJsEnumAssignmentExpressionFails() {
    assertTranspileFails(
            "test.Main",
            "import java.io.Serializable;",
            "import jsinterop.annotations.*;",
            "public class Main {",
            "  @JsEnum(isNative = true) enum Native {A, B}",
            "  @JsEnum enum MyJsEnum {A, B}",
            "  @JsEnum(hasCustomValue = true) enum JsEnumWithCustomValue {",
            "    A(1),",
            "    B(2);",
            "    int value;",
            "    JsEnumWithCustomValue(int value) { this.value = value; }",
            "  }",
            "  private static void main() {",
            "    Enum e = Native.A;",
            "    e = MyJsEnum.A;",
            "    e = JsEnumWithCustomValue.A;",
            "    Comparable c = JsEnumWithCustomValue.A;",
            "    Serializable s = JsEnumWithCustomValue.A;",
            "    JsEnumWithCustomValue.A.value = 3;",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsEnum 'Native' cannot be assigned to 'Enum'.",
            "JsEnum 'JsEnumWithCustomValue' cannot be assigned to 'Enum'.",
            "Custom-valued JsEnum 'JsEnumWithCustomValue' cannot be assigned to 'Comparable'.",
            "JsEnum 'MyJsEnum' cannot be assigned to 'Enum'.",
            "Custom-valued JsEnum value field 'JsEnumWithCustomValue.value' cannot be assigned.");
  }

  public void testJsEnumParameterAssignmentFails() {
    assertTranspileFails(
            "test.Main",
            "import java.io.Serializable;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "import jsinterop.annotations.*;",
            "public class Main {",
            "  @JsEnum(isNative = true) enum Native {A, B}",
            "  @JsEnum enum MyJsEnum {A, B}",
            "  @JsEnum(hasCustomValue = true) enum JsEnumWithCustomValue {",
            "    A(1),",
            "    B(2);",
            "    int value;",
            "    JsEnumWithCustomValue(int value) { this.value = value; }",
            "  }",
            "  private static void acceptsEnum(Enum e) {}",
            "  private static void acceptsComparable(Comparable c) {}",
            "  private static void acceptsSerializable(Serializable s) {}",
            "  private static void main() {",
            "    acceptsEnum(Native.A);",
            "    acceptsEnum(MyJsEnum.A);",
            "    acceptsEnum(JsEnumWithCustomValue.A);",
            "    acceptsComparable(JsEnumWithCustomValue.A);",
            "    List<Enum> l1 = null; l1.add(MyJsEnum.A);",
            // TODO(b/114468916): The following should have failed. But for now they will be caught
            // by runtime checks when the erasure cast to Enum occurs.
            "    List<? extends Enum> l2 = new ArrayList<MyJsEnum>();",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsEnum 'Native' cannot be assigned to 'Enum'.",
            "JsEnum 'MyJsEnum' cannot be assigned to 'Enum'.",
            "JsEnum 'MyJsEnum' cannot be assigned to 'Enum'.",
            "JsEnum 'JsEnumWithCustomValue' cannot be assigned to 'Enum'.",
            "Custom-valued JsEnum 'JsEnumWithCustomValue' cannot be assigned to 'Comparable'.");
  }

  public void testJsEnumAssignmentSucceeds() {
    assertTranspileSucceeds(
            "test.Main",
            "import java.io.Serializable;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "import jsinterop.annotations.*;",
            "public class Main {",
            "  @JsEnum(isNative = true)",
            "  public enum Native {",
            "    A,",
            "    B;",
            "  }",
            "  @JsEnum enum MyJsEnum {A, B}",
            "  private static void acceptsObject(Object o) {}",
            "  private static void acceptsSerializable(Serializable s) {}",
            "  private static void acceptsComparable(Comparable c) {}",
            "  private static void acceptsMyJsEnum(MyJsEnum c) {}",
            "  private static void main() {",
            "    {",
            "      Object o = Native.A;",
            "      acceptsObject(Native.A);",
            "      Serializable s = MyJsEnum.A;",
            "      acceptsSerializable(Native.A);",
            "      Comparable c = Native.A;",
            "      acceptsComparable(Native.A);",
            "      List<Native> l1 = null; l1.add(Native.A);",
            "      List<Object> l2 = null; l2.add(Native.A);",
            "      List<? extends Serializable> l3 = new ArrayList<Native>();",
            "    }",
            "    {",
            "      Object o = MyJsEnum.A;",
            "      acceptsObject(MyJsEnum.A);",
            "      Serializable s = MyJsEnum.A;",
            "      acceptsSerializable(MyJsEnum.A);",
            "      Comparable c = MyJsEnum.A;",
            "      acceptsComparable(MyJsEnum.A);",
            "      MyJsEnum myJsEnum = MyJsEnum.A;",
            "      acceptsMyJsEnum(MyJsEnum.A);",
            "      List<MyJsEnum> l1 = null; l1.add(MyJsEnum.A);",
            "      List<Object> l2 = null; l2.add(MyJsEnum.A);",
            "      List<? extends Serializable> l3 = new ArrayList<MyJsEnum>();",
            "    }",
            "  }",
            "}")
        .assertNoWarnings();
  }

  public void testJsEnumArraysFails() {
    assertTranspileFails(
            "Main",
            "import java.util.List;",
            "import java.util.function.Function;",
            "import jsinterop.annotations.*;",
            "public class Main<T> {",
            "  @JsEnum enum MyJsEnum {A, B}",
            "  MyJsEnum[] myJsEnum;",
            "  T[] tArray;",
            "  T t;",
            "  private static void acceptsJsEnumArray(MyJsEnum[] p) {}",
            "  private static void acceptsJsEnumVarargs(MyJsEnum... p) {}",
            "  private static void acceptsJsEnumVarargsArray(MyJsEnum[]... p) {}",
            "  private static MyJsEnum[] returnsJsEnumArray() { return null;}",
            "  private static <T> T[] returnsTArray(T t) { return null;}",
            "  private static void arrays() {",
            "    Object o = new MyJsEnum[1];",
            "    MyJsEnum[] arr = null;",
            "    List<MyJsEnum[]> list = null;",
            "    o = (Function<? extends Object, ? extends Object>) (MyJsEnum[] p1) -> p1;",
            "    o = (Function<? extends Object, ? extends Object>) (MyJsEnum... p2) -> p2;",
            "    o = (MyJsEnum[]) o;",
            "    if (o instanceof MyJsEnum[]) { }",
            "    MyJsEnum e = returnsTArray(MyJsEnum.A)[0];",
            "    e = new Main<MyJsEnum>().tArray[0];",
            "    e = new Main<MyJsEnum[]>().t[0];",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Cannot cast to JsEnum array 'MyJsEnum[]'. (b/118299062)",
            "Cannot do instanceof against JsEnum array 'MyJsEnum[]'. (b/118299062)",
            "Field 'Main<T>.myJsEnum' cannot be of type 'MyJsEnum[]'. (b/118299062)",
            "Parameter 'p' in 'void Main.acceptsJsEnumArray(MyJsEnum[] p)' cannot be of "
                + "type 'MyJsEnum[]'. (b/118299062)",
            "Parameter 'p' in 'void Main.acceptsJsEnumVarargs(MyJsEnum... p)' cannot be of type "
                + "'MyJsEnum[]'. (b/118299062)",
            "Parameter 'p' in 'void Main.acceptsJsEnumVarargsArray(MyJsEnum[]... p)' cannot be of "
                + "type 'MyJsEnum[][]'. (b/118299062)",
            "Return type of 'MyJsEnum[] Main.returnsJsEnumArray()' cannot be of type 'MyJsEnum[]'. "
                + "(b/118299062)",
            "Array creation 'new MyJsEnum[1]' cannot be of type 'MyJsEnum[]'. " + "(b/118299062)",
            "Variable 'arr' cannot be of type 'MyJsEnum[]'. (b/118299062)",
            "Variable 'list' cannot be of type 'List<MyJsEnum[]>'. (b/118299062)",
            "Parameter 'p1' in '<lambda>' cannot be of type 'MyJsEnum[]'. (b/118299062)",
            "Parameter 'p2' in '<lambda>' cannot be of type 'MyJsEnum[]'. (b/118299062)",
            "Returned type in call to method 'MyJsEnum[] Main.returnsTArray(MyJsEnum)' cannot be "
                + "of type 'MyJsEnum[]'. (b/118299062)",
            "Reference to field 'Main<MyJsEnum>.tArray' cannot be of type 'MyJsEnum[]'."
                + " (b/118299062)",
            "Object creation 'new Main.<init>()' cannot be of type 'Main<MyJsEnum[]>'."
                + " (b/118299062)",
            "Reference to field 'Main<MyJsEnum[]>.t' cannot be of type 'MyJsEnum[]'."
                + " (b/118299062)");
  }

  public void testJsEnumArraysSucceeds() {
    assertTranspileSucceeds(
            "test.Main",
            "import java.util.List;",
            "import java.util.function.Function;",
            "import jsinterop.annotations.*;",
            "public class Main {",
            "  @JsEnum enum MyJsEnum {A, B}",
            "  @JsEnum(isNative = true) enum NativeEnum {A, B}",
            "  private static void arrays() {",
            "    Object[] array = null;",
            "    array[0] = MyJsEnum.A;",
            "    List<MyJsEnum> l = null;",
            "    Object[] oarr = l.toArray();",
            "    Object o = new NativeEnum[1];",
            "    NativeEnum[] arr = null;",
            "    List<NativeEnum[]> list = null;",
            "    o = (Function<? extends Object, ? extends Object>) (NativeEnum[] p1) -> p1;",
            "    o = (Function<? extends Object, ? extends Object>) (NativeEnum... p2) -> p2;",
            "    o = (NativeEnum[]) o;",
            "    if (o instanceof NativeEnum[]) { }",
            "  }",
            "}")
        .assertNoWarnings();
  }

  public void testInnerNativeJsTypeFails() {
    assertTranspileFails(
            "EntryPoint",
            "import jsinterop.annotations.*;",
            "public class EntryPoint {",
            "  @JsType(isNative = true)",
            "  public class Buggy {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Non static inner class 'Buggy' cannot be a native JsType.");
  }

  public void testInnerJsTypeSucceeds() {
    assertTranspileSucceeds(
            "EntryPoint",
            "import jsinterop.annotations.*;",
            "public class EntryPoint {",
            "  @JsType",
            "  public static class Buggy {}",
            "}")
        .assertNoWarnings();
  }

  public void testLocalJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy { void m() { @JsType class Local {} } }")
        .assertErrorsWithoutSourcePosition("Local class 'Local' cannot be a JsType.");
  }

  public void testNativeJsTypeImplementsNativeJsTypeSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy implements Super {}")
        .addCompilationUnit(
            "test.Super",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public interface Super {}")
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testNativeJsTypeInterfaceImplementsNativeJsTypeSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public interface Buggy extends Super {}")
        .addCompilationUnit(
            "test.Super",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public interface Super {}")
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testNativeJsTypeExtendsJsTypeFails() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy extends Super {}")
        .addCompilationUnit(
            "test.Super", "import jsinterop.annotations.*;", "@JsType", "public class Super {}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Native JsType 'Buggy' can only extend native JsType classes.");
  }

  public void testNativeJsTypeImplementsJsTypeInterfaceFails() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy implements Interface {}")
        .addCompilationUnit(
            "test.Interface",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public interface Interface {}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Native JsType 'Buggy' can only implement native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceExtendsJsTypeInterfaceFails() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public interface Buggy extends Interface {}")
        .addCompilationUnit(
            "test.Interface",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public interface Interface {}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Native JsType 'Buggy' can only extend native JsType interfaces.");
  }

  public void testNativeJsTypeImplementsNonJsTypeFails() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy implements Interface {}")
        .addCompilationUnit("test.Interface", "public interface Interface {}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Native JsType 'Buggy' can only implement native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceExtendsNonJsTypeFails() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public interface Buggy extends Super {}")
        .addCompilationUnit("test.Super", "public interface Super {}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Native JsType 'Buggy' can only extend native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceDefaultMethodsFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) interface Interface {",
            "  @JsOverlay default void someOtherMethod() {}",
            "}",
            "class OtherClass implements Interface {",
            "  public void someOtherMethod() {}",
            "}",
            "@JsType(isNative=true) public interface Buggy extends Interface {",
            "  default void someMethod() {}",
            "  void someOtherMethod();",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Native JsType method 'void Buggy.someMethod()' should be native, abstract or "
                + "JsOverlay.",
            "Method 'void OtherClass.someOtherMethod()' cannot override a JsOverlay method "
                + "'void Interface.someOtherMethod()'.",
            "Method 'void Buggy.someOtherMethod()' cannot override a JsOverlay method "
                + "'void Interface.someOtherMethod()'.");
  }

  public void testJsOptionalSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit("org.jspecify.annotations.Nullable", "public @interface Nullable {}")
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "import org.jspecify.annotations.*;",
            "public class Buggy<T> {",
            "  @JsConstructor public Buggy(@JsOptional Object a) {}",
            "  @JsMethod public void foo(int a, Object b, @JsOptional String c) {}",
            "  @JsMethod public void bar(int a, @JsOptional Object b, @JsOptional String c) {}",
            "  @JsMethod public void baz(@JsOptional String a, @JsOptional Object b) {}",
            "  @JsMethod public void qux(@JsOptional String c, Object... os) {}",
            "  @JsMethod public void corge(int a, @JsOptional @Nullable T b, Object... c) {}",
            "}",
            "class SubBuggy extends Buggy<String> {",
            "  @JsConstructor",
            "  public SubBuggy() { super(null); } ",
            "  @JsMethod public void corge(int a, @JsOptional String b, Object... c) {}",
            "}",
            "@JsFunction interface Function {",
            "  void m(String a, @JsOptional String b);",
            "}",
            "final class FunctionImpl implements Function {",
            "   public void m(String a, @JsOptional String b) {}",
            "}",
            "class Main {",
            "  public static void main() {",
            "    Object o;",
            "    o = (Function) (String s, @JsOptional String b) -> {};",
            "  }",
            "}")
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testJsOptionalNotJsOptionalOverrideFails() {
    newTesterWithDefaults()
        .addCompilationUnit("org.jspecify.annotations.Nullable", "public @interface Nullable {}")
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "import org.jspecify.annotations.*;",
            "interface Interface {",
            "  @JsMethod void foo(@JsOptional Object o);",
            "  @JsMethod Object bar(@JsOptional Object o);",
            "}",
            "public class Buggy implements Interface {",
            "  @Override",
            "  @JsMethod public void foo(Object o) {}",
            "  @Override",
            "  @JsMethod public String bar(Object o) { return null; }",
            "}",
            "interface I<T> {",
            "  @JsMethod void m(@JsOptional @Nullable T t);",
            "}",
            "class Implementor implements I<Integer> {",
            "  public void m(Integer i) {}",
            "}",
            "@JsFunction interface Function {",
            "  void m(String a, @JsOptional String b);",
            "}",
            "class Main {",
            "  public static void main() {",
            "    Object o;",
            // TODO(b/72319249): This should not pass restriction checks.
            "    o = (Function) (String s, String b) -> {};",
            "  }",
            "}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Method 'void Buggy.foo(Object o)' should declare parameter 'o' as JsOptional.",
            "Method 'String Buggy.bar(Object o)' should declare parameter 'o' as JsOptional.",
            "Method 'void Implementor.m(Integer i)' should declare parameter 'i' as JsOptional.");
  }

  public void testJsOptionalNotAtEndFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "   @JsConstructor",
            "   public Buggy(@JsOptional String a, Object b, @JsOptional String c) {}",
            "   @JsMethod",
            "   public void bar(int a, @JsOptional Object b, String c) {}",
            "   @JsMethod",
            "   public void baz(@JsOptional Object a, String b, Object... c) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsOptional parameter 'a' in method "
                + "'Buggy(String a, Object b, String c)' cannot precede parameters that are not "
                + "JsOptional.",
            "JsOptional parameter 'b' in method "
                + "'void Buggy.bar(int a, Object b, String c)' cannot precede parameters that are"
                + " not JsOptional.",
            "JsOptional parameter 'a' in method "
                + "'void Buggy.baz(Object a, String b, Object... c)' cannot precede parameters that"
                + " are not JsOptional.");
  }

  public void testJsOptionalOnInvalidParametersFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "   @JsConstructor public Buggy(@JsOptional int a) {}",
            "   @JsMethod public void bar(int a, @JsOptional Object b, @JsOptional String... c) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsOptional parameter 'a' in method '" + "Buggy(int a)' cannot be of a primitive type.",
            "JsOptional parameter 'c' in method "
                + "'void Buggy.bar(int a, Object b, String... c)' cannot be a varargs parameter.");
  }

  public void testJsOptionalOnNonNullableParameterFails() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "   @JsMethod public <T> void bar(@JsOptional @JsNonNull T a, @JsOptional @JsNonNull"
                + " String b) {}",
            "}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "JsOptional parameter 'a' in method 'void Buggy.bar(T a, String b)' has to be"
                + " nullable.",
            "JsOptional parameter 'b' in method 'void Buggy.bar(T a, String b)' has to be"
                + " nullable.");
  }

  public void testJsOptionalOnNullableTypeVariableParameterSucceeds() {
    newTesterWithDefaults()
        .addArgs(
            "-experimentalenablejspecifysupportdonotenablewithoutjspecifystaticcheckingoryoumightcauseanoutage")
        .addCompilationUnit(
            "org.jspecify.annotations.NullMarked", "public @interface NullMarked {}")
        .addCompilationUnit("org.jspecify.annotations.Nullable", "public @interface Nullable {}")
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "import org.jspecify.annotations.*;",
            "@NullMarked",
            "public class Buggy {",
            "   @JsMethod public <T extends @Nullable Object> void bar(@JsOptional @Nullable T a,"
                + " @JsOptional @Nullable String b) {}",
            "}")
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testJsOptionalOnNonJsExposedMethodsFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "  public void fun(int a, @JsOptional Object b, @JsOptional String c) {}",
            "  @JsProperty public void setBar(@JsOptional Object o) {}",
            "}",
            "@JsType(isNative = true) class Native {",
            "  @JsOverlay public final void fun( @JsOptional Object a) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsOptional parameter in 'void Buggy.fun(int a, Object b, "
                + "String c)' can only be declared in a JsMethod, a JsConstructor or a JsFunction.",
            "JsOptional parameter in 'void Buggy.setBar(Object o)' can only "
                + "be declared in a JsMethod, a JsConstructor or a JsFunction.",
            "JsOptional parameter in 'void Native.fun(Object a)' can only "
                + "be declared in a JsMethod, a JsConstructor or a JsFunction.");
  }

  public void testJsOverlayOnNativeJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public interface Buggy {",
            "  @JsOverlay Object obj = new Object();",
            "  @JsOverlay default void someOverlayMethod() {};",
            "}")
        .assertNoWarnings();
  }

  public void testJsOverlayOnNativeJsTypeMemberSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) final class FinalType {",
            "  @JsOverlay public void n() { }",
            "}",
            "@JsType(isNative=true) interface NativeInterface {",
            "  @JsOverlay public static Object object = new Object();",
            "  @JsOverlay public static final Object other = new Object();",
            "  @JsOverlay public Object another = new Object();",
            "  @JsOverlay public final Object yetAnother = new Object();",
            "}",
            "@JsType(isNative=true) public class Buggy {",
            "  @JsOverlay public static Object object = new Object();",
            "  @JsOverlay public static final Object other = new Object();",
            "  @JsOverlay public static void m() { }",
            "  @JsOverlay public static void m(int x) { }",
            "  @JsOverlay private static void m(boolean x) { }",
            "  @JsOverlay private void m(String x) { }",
            "  @JsOverlay public final void n() { }",
            "  @JsOverlay public final void n(int x) { }",
            "  @JsOverlay private final void n(boolean x) { }",
            "  @JsOverlay final void o() { }",
            "  @JsOverlay protected final void p() { }",
            "}")
        .assertNoWarnings();
  }

  public void testJsOverlayWithSuperFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) class SuperBuggy {",
            "  public native void m();",
            "}",
            "@JsType(isNative=true) public class Buggy extends SuperBuggy {",
            "  @JsOverlay public final void n() { super.m(); }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Cannot use 'super' in JsOverlay method 'void Buggy.n()'.");
  }

  public void testJsOverlayImplementingInterfaceMethodFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) interface IBuggy {",
            "  void m();",
            "  Object n();",
            "}",
            "@JsType(isNative=true) public class Buggy implements IBuggy {",
            "  @JsOverlay public final void m() { }",
            "  @JsOverlay public final String n() { return null; }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsOverlay method 'void Buggy.m()' cannot override a supertype method.",
            "JsOverlay method 'String Buggy.n()' cannot override a supertype method.");
  }

  public void testJsOverlayOverridingSuperclassMethodFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) class Super {",
            "  public native void m();",
            "  public native Object n();",
            "}",
            "@JsType(isNative=true) public class Buggy extends Super {",
            "  @JsOverlay public final void m() { }",
            "  @JsOverlay public final String n() { return null; }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsOverlay method 'void Buggy.m()' cannot override a supertype method.",
            "JsOverlay method 'String Buggy.n()' cannot override a supertype method.");
  }

  public void testJsOverlayOnNonFinalMethodAndInstanceFieldFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  @JsOverlay public final int f2 = 2;",
            "  @JsOverlay",
            "  public void m() {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsOverlay field 'Buggy.f2' can only be static.",
            "JsOverlay method 'void Buggy.m()' has to be final.");
  }

  public void testJsOverlayWithStaticInitializerSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  @JsOverlay public static final Object f1 = new Object();",
            "  @JsOverlay public static int f2 = 2;",
            "  static { f2 = 3; }",
            "}")
        .assertNoWarnings();
  }

  public void testJsOverlayOnJsMemberFails() {
    // JsOverlay in constructors is checked by JDT.
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) public class Buggy {",
            "  @JsOverlay public Buggy() { }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "The annotation @JsOverlay is disallowed for this location");

    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) public class Buggy {",
            "  @JsProperty @JsOverlay public int a;",
            "  @JsProperty @JsOverlay public static int b;",
            "  @JsMethod @JsOverlay public final void m() { }",
            "  @JsMethod @JsOverlay public static void n() { }",
            "  @JsProperty @JsOverlay public static void setA(String value) { }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsOverlay 'Buggy.a' cannot be nor override a JsProperty or a JsMethod.",
            "JsOverlay 'Buggy.b' cannot be nor override a JsProperty or a JsMethod.",
            "JsOverlay 'void Buggy.m()' cannot be nor override a JsProperty or a JsMethod.",
            "JsOverlay 'void Buggy.n()' cannot be nor override a JsProperty or a JsMethod.",
            "JsOverlay 'void Buggy.setA(String)' cannot be nor override "
                + "a JsProperty or a JsMethod.");
  }

  public void testJsOverlayOnNonNativeJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class Buggy {",
            "  @JsOverlay public static final int F = 2;",
            "  @JsOverlay",
            "  public final void m() {};",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsOverlay 'Buggy.F' can only be declared in a native type "
                + "or @JsFunction interface.",
            "JsOverlay 'void Buggy.m()' can only be declared in a native type "
                + "or @JsFunction interface.");
  }

  public void testJsTypeExtendsNativeJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) class Super {",
            "}",
            "@JsType public class Buggy extends Super {",
            "}")
        .assertNoWarnings();
  }

  public void testJsTypeExtendsNonJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class Super {",
            "}",
            "@JsType public class Buggy extends Super {",
            "}")
        .assertNoWarnings();
  }

  public void testJsTypeImplementsNativeJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) interface Interface {",
            "}",
            "@JsType public class Buggy implements Interface {",
            "}")
        .assertNoWarnings();
  }

  public void testJsTypeImplementsNonJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface Interface {",
            "}",
            "@JsType public class Buggy implements Interface {",
            "}")
        .assertNoWarnings();
  }

  public void testJsTypeInterfaceExtendsNativeJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) interface Interface {",
            "}",
            "@JsType public interface Buggy extends Interface {",
            "}")
        .assertNoWarnings();
  }

  public void testJsTypeInterfaceExtendsNonJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface Interface {",
            "}",
            "@JsType public interface Buggy extends Interface {",
            "}")
        .assertNoWarnings();
  }

  public void testNativeJsTypeExtendsNativeJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) class Super {",
            "  public native int hashCode();",
            "}",
            "@JsType(isNative=true) interface HasHashCode {",
            "  int hashCode();",
            "}",
            "@JsType(isNative=true) public class Buggy extends Super {",
            "  public native String toString();",
            "  public native boolean equals(Object obj);",
            "}",
            "@JsType(isNative=true) class OtherBuggy implements HasHashCode {",
            "  public native String toString(int i);",
            "  public native boolean equals(Object obj);",
            "  public native int hashCode();",
            "}",
            "@JsType(isNative=true) class NativeType {}",
            "interface A { int hashCode(); }",
            "class SomeClass extends NativeType implements A {",
            "  @JsConstructor public SomeClass () {}",
            "  public int hashCode() { return 0; }",
            "}",
            "@JsType(isNative=true) interface NativeInterface {}",
            "class SomeClass3 implements NativeInterface {}")
        .assertNoWarnings();
  }

  public void testNativeJsTypeBadMembersFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) interface Interface {",
            "  @JsIgnore public void n();",
            "}",
            "@JsType(isNative=true) abstract class Buggy {",
            "  public static final int s = 42;",
            "  public static int t = 42;",
            "  public final int f = 42;",
            "  public int g = 42;",
            "  @JsIgnore public Buggy() { }",
            "  @JsIgnore public int x;",
            "  @JsIgnore public native void n();",
            "  public void o() {}",
            "  public native void p() /*-{}-*/;",
            "}",
            "@JsType(isNative=true) class NativeType {}",
            "interface A { @JsMethod(name=\"something\") int hashCode(); }",
            "class SomeClass extends NativeType implements A {",
            "  @JsConstructor public SomeClass() {}",
            "  public int hashCode() { return 0; }",
            "}",
            "@JsType(isNative=true) class NativeTypeWithHashCode {",
            "  public native int hashCode();",
            "}",
            "class SomeClass3 extends NativeTypeWithHashCode implements A {",
            "  @JsConstructor public SomeClass3() {}",
            "}",
            "@JsType(isNative=true) interface NativeInterface {",
            "  public Object foo();",
            "}",
            "@JsType(isNative=true) class NativeTypeWithBridge implements NativeInterface {",
            "  public String foo() { return null; }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Native JsType member 'void Interface.n()' cannot have @JsIgnore.",
            "Native JsType member 'Buggy()' cannot have @JsIgnore.",
            "Native JsType field 'Buggy.f' cannot be final.",
            "Native JsType field 'Buggy.s' cannot be final.",
            "Native JsType member 'Buggy.x' cannot have @JsIgnore.",
            "Native JsType member 'void Buggy.n()' cannot have @JsIgnore.",
            "[unusable-by-js] Native 'void Buggy.n()' is exposed to JavaScript without @JsMethod.",
            "Native JsType method 'void Buggy.o()' should be native, abstract or JsOverlay.",
            "Native JsType field 'Buggy.t' cannot have initializer.",
            "Native JsType field 'Buggy.g' cannot have initializer.",
            "'int SomeClass.hashCode()' cannot be assigned JavaScript name 'something' that is "
                + "different from the JavaScript name of a method it "
                + "overrides ('int Object.hashCode()' with JavaScript name 'hashCode').",
            "'int A.hashCode()' cannot be assigned JavaScript name 'something' that is different "
                + "from the JavaScript name of a method it overrides ('int Object.hashCode()' with "
                + "JavaScript name 'hashCode').",
            "Native JsType method 'String NativeTypeWithBridge.foo()' should be native"
                + ", abstract or JsOverlay.",
            "[unusable-by-js] Native 'void Interface.n()' is exposed to JavaScript without "
                + "@JsMethod.");
  }

  public void testNativeJsTypeImplementingJavaLangObjectMethodsSucceeds() {
    assertTranspileSucceeds(
            "test.NativeType",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) class NativeType {}",
            "interface B { int hashCode(); }",
            "@JsType(isNative=true) class NativeTypeWithHashCode {",
            "  public native int hashCode();",
            "}")
        .assertNoWarnings();
  }

  public void testSubclassOfNativeJsTypeBadMembersFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) class NativeType {",
            "  @JsMethod(name =\"string\")",
            "  public native String toString();",
            "  @JsOverlay",
            "  public final String callToString() { return super.toString(); }",
            "}",
            "class Buggy extends NativeType {",
            "  @JsConstructor Buggy() {}",
            "  public String toString() { return null; }",
            "  @JsMethod(name = \"blah\")",
            "  public int hashCode() { return super.hashCode(); }",
            "}",
            "class SubBuggy extends Buggy {",
            "  @JsConstructor SubBuggy() {}",
            "  public boolean equals(Object obj) { return super.equals(obj); }",
            "  public Object foo(Object obj) { return null; }",
            "}",
            "class SubBuggy2 extends SubBuggy {",
            "  @JsConstructor SubBuggy2() {}",
            "  public String foo(Object obj) { return super.toString(); }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'String NativeType.toString()' cannot be assigned JavaScript name 'string'"
                + " that is different from the JavaScript name of a method it overrides "
                + "('String Object.toString()' with JavaScript name 'toString').",
            "'String Buggy.toString()' cannot be assigned JavaScript name 'string' that is "
                + "different from the JavaScript name of a method it overrides "
                + "('String Object.toString()' with JavaScript name 'toString').",
            "'int Buggy.hashCode()' cannot be assigned JavaScript name 'blah' "
                + "that is different from the JavaScript name of a method it overrides "
                + "('int Object.hashCode()' with JavaScript name 'hashCode').",
            "Cannot use 'super' to call 'int Object.hashCode()' from a subclass of a native class.",
            "Cannot use 'super' to call 'boolean Object.equals(Object)' from a subclass of "
                + "a native class.",
            "Cannot use 'super' to call 'String Buggy.toString()' from a subclass of a "
                + "native class.",
            "Cannot use 'super' in JsOverlay method 'String NativeType.callToString()'.");
  }

  public void testNativeMethodOnJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "public class Buggy {",
            "  @JsMethod",
            "  public native void m();",
            "  @JsProperty",
            "  public native int getM();",
            "}")
        .assertNoWarnings();
  }

  public void testNativeMethodNotJsMethodFails() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "  public native void m();",
            "}")
        .addNativeJsForCompilationUnit("test.Buggy")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "[unusable-by-js] Native 'void Buggy.m()' is exposed to JavaScript without @JsMethod.");
  }

  public void testNativeJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) abstract class Buggy {",
            "  public static native void m();",
            "  protected static native void m(Object o);",
            "  private static native void m(String o);",
            "  public Buggy() { }",
            "  protected Buggy(Object o) { }",
            "  private Buggy(String o) { }",
            "  public native void n();",
            "  protected native void n(Object o);",
            "  private native void n(String o);",
            "  public abstract void o();",
            "  protected abstract void o(Object o);",
            "  abstract void o(String o);",
            "}",
            "@JsType(isNative=true) interface NativeInterface {",
            "  void m();",
            "  void m(Object o);",
            "  void m(String o);",
            "}",
            "@JsType(isNative=true) abstract class NativeClass {",
            "  public native String toString();",
            "  public abstract int hashCode();",
            "}",
            "class NativeSubclass extends NativeClass {",
            "  @JsConstructor public NativeSubclass() {}",
            "  public String toString() { return null; }",
            "  @JsMethod",
            "  public boolean equals(Object obj) { return false; }",
            "  public int hashCode() { return 0; }",
            "}",
            "class SubNativeSubclass extends NativeSubclass {",
            "  @JsConstructor public SubNativeSubclass() {}",
            "  public boolean equals(Object obj) { return false; }",
            "}")
        .assertNoWarnings();
  }

  public void testNativeJsTypeFieldsSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  public static int f1;",
            "  protected static int f2;",
            "  private static int f3;",
            "  public int f4;",
            "  protected int f5;",
            "  private int f6;",
            "}")
        .assertNoWarnings();
  }

  public void testNativeJsTypeDefaultConstructorSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public class Buggy {}",
            "")
        .assertNoWarnings();
  }

  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) class Super {",
            "  public native void m(Object o);",
            "  public native void m(Object[] o);",
            "}",
            "@JsType public class Buggy extends Super {",
            "  public void n(Object o) { }",
            "}")
        .assertNoWarnings();
  }

  public void testClassesExtendingNativeJsTypeInterfaceWithOverlaySucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) interface Super {",
            "  @JsOverlay default void fun() {}",
            "}",
            "@JsType(isNative=true) abstract class Buggy implements Super {",
            "}",
            "class JavaSubclass implements Super {",
            "}")
        .assertNoWarnings();
  }

  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodOverloadsSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) class Super {",
            "  public native void m(Object o);",
            "  public native void m(int o);",
            "}",
            "public class Buggy extends Super {",
            "  @JsConstructor public Buggy() {}",
            "  public void m(Object o) { }",
            "}")
        .assertNoWarnings();
  }

  public void testNonJsTypeWithNativeStaticMethodOverloadsSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "public class Buggy {",
            "  @JsMethod public static native void m(Object o);",
            "  @JsMethod public static native void m(int o);",
            "}")
        .assertNoWarnings();
  }

  public void testNonJsTypeWithNativeInstanceMethodOverloadsSucceeds() {
    assertTranspileSucceeds(
            "Leaf",
            "import jsinterop.annotations.*;",
            "class Top {",
            "  @JsMethod public void m(int o) {}",
            "}",
            "class SubTop extends Top {",
            // Redefines m to be a setter
            "  @JsMethod public native void m(int o);",
            "  @JsProperty public void setM(int m) { }",
            "}",
            "class SubSubTop extends SubTop {",
            //  Adds a getter
            "  @JsProperty public int getM() { return 0; }",
            "}",
            "public class Leaf extends SubSubTop {",
            // makes setter/getter pair native to define a different overload for the
            // JavaScript name
            "  @JsProperty public native void setM(int m);",
            "  @JsProperty public native int getM();",
            "  @JsMethod public void m(int o, Object opt_o) { }",
            "}")
        .assertNoWarnings();
  }

  public void testOneLiveImplementationRuleViolationFails() {
    assertTranspileFails(
            "test.A",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "interface I {",
            "  void m();",
            "}",
            "@JsType",
            "interface J extends I {",
            "  default void m() {}",
            "}",
            "abstract class A implements I, J {",
            "  @JsMethod(name = \"m\") public void x() {}",
            "}",
            "abstract class B implements J, I {",
            "  @JsMethod(name = \"m\") public void y() {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void B.y()' and 'void J.m()' cannot both use the same JavaScript name 'm'.",
            "'void A.x()' and 'void J.m()' cannot both use the same JavaScript name 'm'.");
  }

  public void testOneLiveImplementationRuleComplianceSucceeds() {
    assertTranspileSucceeds(
            "test.A",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "interface I {",
            "  void m();",
            "}",
            "@JsType",
            "interface J extends I {",
            "  default void m() {}",
            "}",
            "class A implements I, J {",
            //  Redirects I.m and J.m to A.x
            "  @JsMethod public native void m();",
            "  @JsMethod(name = \"m\") public void x() {}",
            "}",
            "class B implements J, I {",
            //  Redirects I.m and J.m to B.y
            "  @JsMethod public native void m();",
            "  @JsMethod(name = \"m\") public void y() {}",
            "}")
        .assertNoWarnings();
  }

  public void testNonSingleOverloadImplementationFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class Super {",
            "  @JsMethod public void m(int o) { }",
            "}",
            "public class Buggy extends Super {",
            "  @JsMethod public native void m(Object o);",
            "  @JsMethod public void m(int o, Object opt_o) { }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "'void Buggy.m(int, Object)' and 'void Super.m(int)' cannot both use the "
                + "same JavaScript name 'm'.");
  }

  public void testNonJsTypeExtendsJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType class Super {",
            "  Super() {}",
            "}",
            "public class Buggy extends Super {",
            "}")
        .assertNoWarnings();
  }

  public void testNonJsTypeImplementsJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType interface Interface {",
            "}",
            "public class Buggy implements Interface {",
            "}")
        .assertNoWarnings();
  }

  public void testNonJsTypeInterfaceExtendsJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType interface Interface {",
            "}",
            "public interface Buggy extends Interface {",
            "}")
        .assertNoWarnings();
  }

  public void testNonJsTypeExtendsNativeJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) class Super {",
            "  public native void m();",
            "}",
            "public class Buggy extends Super {",
            "  @JsConstructor Buggy() { }",
            "  public void m() { }",
            "}")
        .assertNoWarnings();
  }

  public void testNonJsTypeImplementsNativeJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true) interface Interface {",
            "}",
            "public class Buggy implements Interface {",
            "}")
        .assertNoWarnings();
  }

  public void testNonJsTypeInterfaceExtendsNativeJsTypeInterfaceSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit("test.Buggy", "public interface Buggy extends Interface {}")
        .addCompilationUnit(
            "test.Interface",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "public interface Interface {}")
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testJsAsyncSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(namespace = JsPackage.GLOBAL)",
            "class Promise {",
            "}",
            "@JsType(namespace = JsPackage.GLOBAL)",
            "interface IThenable {",
            "}",
            "class Buggy {",
            "  @JsAsync",
            "  public Promise a() { return null; }",
            "  @JsAsync",
            "  public IThenable b() { return null; }",
            "}")
        .assertNoWarnings();
  }

  public void testJsAsyncFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "import java.util.List;",
            "@JsType(namespace = JsPackage.GLOBAL)",
            "class Promise {",
            "}",
            "class A {",
            "  @JsAsync",
            "  public <T> List<T> a() { return null; }",
            "  @JsAsync",
            "  public <P extends Promise> P b() { return null; }",
            "  @JsAsync",
            "  public String c() { return null; }",
            "  @JsAsync",
            "  public Promise[] d() { return null; }",
            "  @JsAsync",
            "  public double e() { return 0; }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "JsAsync method 'List<T> A.a()' should return either 'IThenable' or 'Promise' but"
                + " returns 'List<T>'.",
            "JsAsync method 'P A.b()' should return either 'IThenable' or 'Promise' but"
                + " returns 'P'.",
            "JsAsync method 'String A.c()' should return either 'IThenable' or 'Promise' but"
                + " returns 'String'.",
            "JsAsync method 'Promise[] A.d()' should return either 'IThenable' or 'Promise' but"
                + " returns 'Promise[]'.",
            "JsAsync method 'double A.e()' should return either 'IThenable' or 'Promise' but"
                + " returns 'double'.");
  }

  public void testCustomIsInstanceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface InterfaceWithCustomIsInstance {",
            "  public static boolean $isInstance(Object o) { return true; }",
            "}",
            "class ClassWithCustomIsInstance {",
            "  static boolean $isInstance(Object o) { return true; }",
            "}",
            "@JsType(isNative = true)",
            "interface NativeInterfaceWithCustomIsInstance {",
            "  @JsOverlay",
            "  static boolean $isInstance(Object o) { return true; }",
            "}",
            "@JsType(isNative = true)",
            "class NativeClassWithCustomIsInstance {",
            "  @JsOverlay",
            "  protected static boolean $isInstance(Object o) { return true; }",
            "}",
            "@JsType(isNative = true)",
            "class ClassWithNativeIsInstance {",
            "  static native boolean $isInstance(Object o);",
            "}",
            "class Buggy {",
            "  static void main() {",
            "    Object o = null;",
            "    boolean b = o instanceof InterfaceWithCustomIsInstance;",
            "    b = o instanceof ClassWithCustomIsInstance;",
            "    b = o instanceof NativeInterfaceWithCustomIsInstance;",
            "    b = o instanceof NativeClassWithCustomIsInstance;",
            "  }",
            "}")
        .assertNoWarnings();
  }

  public void testCustomIsInstanceFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "interface BadIsInstanceVisibility {",
            "  private static boolean $isInstance(Object o) { return true; }",
            "}",
            "class BadIsInstanceReturnType {",
            "  static void $isInstance(Object o) { }",
            "}",
            "class BadIsInstanceMembership {",
            "  boolean $isInstance(Object o) { return true; }",
            "}",
            "@JsType(isNative = true)",
            "class BadIsInstanceOnNativeType {",
            "  static boolean $isInstance(Object o) { return true; }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Custom '$isInstance' method 'boolean BadIsInstanceVisibility.$isInstance(Object o)'"
                + " has to be static and non private.",
            "Custom '$isInstance' method 'void BadIsInstanceReturnType.$isInstance(Object o)' has"
                + " to return 'boolean'.",
            "Custom '$isInstance' method 'boolean BadIsInstanceMembership.$isInstance(Object o)'"
                + " has to be static and non private.",
            "Native JsType method 'boolean BadIsInstanceOnNativeType.$isInstance(Object o)' should"
                + " be native, abstract or JsOverlay.");
  }

  public void testUnusableByJsSuppressionSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class A {}",
            "@JsType @SuppressWarnings(\"unusable-by-js\")", // SuppressWarnings on the class.
            "class B {",
            "  public A field;",
            "  public A t0(A a, A b) { return null; }",
            "}",
            "@JsType",
            "public class Buggy {",
            "  @SuppressWarnings(\"unusable-by-js\")", // add SuppressWarnings to field.
            "  public A field;",
            "  @SuppressWarnings({\"unusable-by-js\", \"unused\"})", // test multiple warnings.
            "  public A t0(A a, A b) { return null; }", // add SuppressWarnings to the method.
            "  public void t1(",
            "    @SuppressWarnings(\"unusable-by-js\")A a,",
            "    @SuppressWarnings(\"unusable-by-js\")A b",
            "  ) {}", // add SuppressWarnings to parameters.
            "}")
        .assertNoWarnings();
  }

  public void testUsableByJsTypesSucceeds() {
    assertTranspileSucceeds(
            "test.A",
            "import jsinterop.annotations.*;",
            "@JsType public class A {}",
            "@JsType interface I<T> {",
            "  void trigger(T t);",
            "}",
            "@JsFunction interface FI {void foo();}",
            "class List<T> {",
            "  @JsMethod",
            "  public T get() { return null; }",
            "}",
            "@JsEnum(isNative = true) enum NativeEnum { A; }",
            "@JsType class MyJsType implements I<Void> {",
            "  public void f1(boolean a, int b, double c) {}", // primitive types work fine.
            "  public void f2(Boolean a, Double b, String c) {}", // unboxed types work fine.
            "  public void f3(A a) {}", // JsType works fine.
            "  public void f4(I a) {}", // JsType interface works fine.
            "  public void f5(FI a) {}", // JsFunction works fine.
            "  public void f7(Object a) {}", // Java Object works fine.
            "  public void f8(boolean[] a) {}", // array of primitive types work fine.
            "  public void f10(A[] a) {}", // array of JsType works fine.
            "  public void f11(FI[] a) {}", // array of JsFunction works fine.
            "  public void f12(Boolean[] a, Double[] b, String[] c) {}", // array of unboxed types.
            "  public void f13(Object[] a) {}", // Object[] works fine.
            "  public void f14(Object[][] a) {}", // Object[][] works fine.
            "  public long f15(long a) { return 1l; }", // long works fine.
            "  public long f16(long... a) { return 1l; }", // varargs of allowable types works fine.
            // anonymous @JsConstructor class with unusable-by-js captures.
            "  private void f17(Long a) { new A() { { f7(a); } }; }",
            "  public void f18(List<NativeEnum> l) {}", // Type parameterized by native JsEnum
            // succeeds
            "  public void trigger(Void v) { }", // Void succeeds.
            "}",
            "class Outer {",
            "  {",
            "    Long l = 1l;",
            "    class A {",
            "       Long f = l;",
            "    }",
            "  }",
            "}")
        .assertNoWarnings();
  }

  public void testUnusableByNonJsMembersSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class A {}",
            "@JsType public class Buggy {",
            "  private A field;",
            "  private A f1(A a) { return null; }",
            "}")
        .assertNoWarnings();
  }

  public void testUnusableByJsWarns() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class A {}",
            "@JsType interface I {}",
            "class B implements I {}",
            "class C {", // non-jstype class
            "  @JsMethod",
            "  public static void fc1(A a) {}", // JsMethod
            "  @JsMethod",
            "  public native void fc2(A a);", // native method
            "}",
            "class D {", // non-jstype class with JsProperty
            "  @JsProperty",
            "  public static A a;", // JsProperty
            "}",
            "@JsFunction interface FI  { void f(A a); }", // JsFunction method is checked.
            "class List<T> {",
            "  @JsMethod",
            "  public T get() { return null; }",
            "}",
            "@JsEnum enum MyJsEnum { A; }",
            "@JsType public class Buggy {",
            "  public A f;", // exported field
            "  public A f1(A a) { return null; }", // regular class fails.
            "  public A[] f2(A[] a) { return null; }", // array of regular class fails.
            "  public Short f3(Short a) { return (short) 1; }", // Short fails.
            // non-JsType class that implements a JsType interface fails.
            "  public B f4(B a) { return null; }",
            "  public Long f5(Long a) { return 1l; }", // Long fails
            "  public void f6(Long... a) { }", // varargs fails
            "  public void f7(List<MyJsEnum> l) { }", // parameterized by JsEnum fails
            "  public void f8(List<List<MyJsEnum>> l) {}", // parameterized by List<JsEnum> fails
            "  public void f17() { new Object() { @JsMethod void b(Long a){} }; }",
            "}")
        .addFileToZipFile("native.zip", "test/C.native.js")
        .assertTranspileSucceeds()
        .assertWarningsWithoutSourcePosition(
            "[unusable-by-js] Type of parameter 'a' in 'void C.fc1(A a)' is not usable by but "
                + "exposed to JavaScript.",
            "[unusable-by-js] Type of parameter 'a' in 'void C.fc2(A a)' is not usable by but "
                + "exposed to JavaScript.",
            "[unusable-by-js] Type 'A' of field 'D.a' is not usable by but exposed to JavaScript.",
            "[unusable-by-js] Type of parameter 'a' in 'void FI.f(A a)' is not usable by but"
                + " exposed to JavaScript.",
            "[unusable-by-js] Type 'A' of field 'Buggy.f' is not usable by but exposed to "
                + "JavaScript.",
            "[unusable-by-js] Return type of 'A Buggy.f1(A a)' is not usable by but exposed to "
                + "JavaScript.",
            "[unusable-by-js] Type of parameter 'a' in 'A Buggy.f1(A a)' is not usable by but "
                + "exposed to JavaScript.",
            "[unusable-by-js] Return type of 'A[] Buggy.f2(A[] a)' is not usable by but exposed to "
                + "JavaScript.",
            "[unusable-by-js] Type of parameter 'a' in 'A[] Buggy.f2(A[] a)' is not usable"
                + " by but exposed to JavaScript.",
            "[unusable-by-js] Return type of 'Short Buggy.f3(Short a)' is not usable by but"
                + " exposed to JavaScript.",
            "[unusable-by-js] Type of parameter 'a' in 'Short Buggy.f3(Short a)' is not"
                + " usable by but exposed to JavaScript.",
            "[unusable-by-js] Return type of 'B Buggy.f4(B a)' is not usable by but "
                + "exposed to JavaScript.",
            "[unusable-by-js] Type of parameter 'a' in 'B Buggy.f4(B a)' is not usable by but"
                + " exposed to JavaScript.",
            "[unusable-by-js] Return type of 'Long Buggy.f5(Long a)' is not usable by but exposed"
                + " to JavaScript.",
            "[unusable-by-js] Type of parameter 'a' in 'Long Buggy.f5(Long a)' is not usable"
                + " by but exposed to JavaScript.",
            "[unusable-by-js] Type of parameter 'a' in 'void Buggy.f6(Long... a)' is not"
                + " usable by but exposed to JavaScript.",
            "[unusable-by-js] Type of parameter 'l' in 'void Buggy.f7(List<MyJsEnum> l)' is not "
                + "usable by but exposed to JavaScript.",
            "[unusable-by-js] Type of parameter 'l' in 'void Buggy.f8(List<List<MyJsEnum>> l)' is "
                + "not usable by but exposed to JavaScript.",
            "[unusable-by-js] Type of parameter 'a' in 'void <anonymous> extends Object.b(Long a)'"
                + " is not usable by but exposed to JavaScript.")
        .assertLastMessage(
            "Suppress \"[unusable-by-js]\" warnings by adding a "
                + "`@SuppressWarnings(\"unusable-by-js\")` annotation to the "
                + "corresponding member.");
  }

  public void testUnusableByJsAccidentalOverrideSuppressionWarns() {
    assertTranspileSucceeds(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType",
            "interface Foo {",
            "  @SuppressWarnings(\"unusable-by-js\") ",
            "  void doIt(Class foo);",
            "}",
            "class Parent {",
            "  public void doIt(Class x) {}",
            "}",
            "public class Buggy extends Parent implements Foo {}")
        .assertNoWarnings();
    // TODO(b/37579830): This error should be emitted once accidental overrides are handled in
    //  restriction checking.
    //  "Line 10: [unusable-by-js] Type of parameter 'x' in "
    //      + "'void EntryPoint.Parent.doIt(Class)' (exposed by 'EntryPoint.Buggy') is not "
    //      + "usable by but exposed to JavaScript.");
  }

  public void testNullMarkedFails() {
    newTesterWithDefaults()
        // Define the annotation here since we don't have it as a dependency.
        .addCompilationUnit(
            "org.jspecify.annotations.NullMarked", "public @interface NullMarked {}")
        .addCompilationUnit(
            "test.Buggy", "@org.jspecify.annotations.NullMarked", "class NullMarkedType {", "}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "@NullMarked annotation is not supported without enabling static analysis.");
  }

  public void testAutoValueExtendsFails() {
    newTesterWithDefaults()
        .addArgs("-optimizeautovalue")
        // Define the annotation here since we don't have it as a dependency.
        .addCompilationUnit(
            "com.google.auto.value.AutoValue",
            "public @interface AutoValue {",
            "public @interface Builder{}",
            "}")
        .addCompilationUnit(
            "test.Buggy",
            "@com.google.auto.value.AutoValue",
            "class Foo {",
            "  @com.google.auto.value.AutoValue.Builder",
            "  static class Builder {}",
            "}",
            "class CustomFoo extends Foo {",
            "  static class CustomFooBuilder extends Foo.Builder {}",
            "}",
            "class CustomFooBuilder extends Foo.Builder {}",
            // Following are allowed since that's what AutoValue generates.
            "class AutoValue_Foo extends Foo {",
            "  static class Builder extends Foo.Builder {}",
            "}",
            "class $$AutoValue_Foo extends Foo {",
            "  static class Builder extends Foo.Builder {}",
            "}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Extending @AutoValue with CustomFoo is not supported when AutoValue optimization is"
                + " enabled. (Also see https://errorprone.info/bugpattern/ExtendsAutoValue)",
            "Extending @AutoValue with CustomFooBuilder is not supported when AutoValue"
                + " optimization is enabled. (Also see"
                + " https://errorprone.info/bugpattern/ExtendsAutoValue)");
  }

  public void testCorrectLineNumbers() {
    assertTranspileFails(
            "test.Buggy",
            // line 1: package test; // implicit
            // line 2
            "import jsinterop.annotations.*;",
            // line 3
            "@JsType(name = \"invalid.Promise\")",
            // line 4
            "class Promise {",
            // line 5
            "  @JsMethod(name =\"invalid.method\")",
            // line 6
            "  void method() {",
            // line 7
            "    Enum e = MyJsEnum.A;",
            // line 8
            "  }",
            // line 9
            "  @JsProperty(name =\"invalid.field\")",
            // line 10
            "  int field;",
            // line 11
            "  @JsEnum enum MyJsEnum { A }",
            // line 12
            "}")
        .assertErrorsWithSourcePosition(
            "Error:Buggy.java:4: 'Promise' has invalid name 'invalid.Promise'.",
            "Error:Buggy.java:6: 'void Promise.method()' has invalid name 'invalid.method'.",
            "Error:Buggy.java:7: JsEnum 'MyJsEnum' cannot be assigned to 'Enum'.",
            "Error:Buggy.java:9: 'Promise.field' has invalid name 'invalid.field'.");
  }

  private TranspileResult assertTranspileSucceeds(String compilationUnitName, String... code) {
    return newTesterWithDefaults()
        .addCompilationUnit(compilationUnitName, code)
        .assertTranspileSucceeds();
  }

  private TranspileResult assertTranspileFails(String compilationUnitName, String... code) {
    return newTesterWithDefaults()
        .addCompilationUnit(compilationUnitName, code)
        .assertTranspileFails();
  }
}
