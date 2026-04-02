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
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/** Tests for JsInteropRestrictionsChecker. */
public class JsInteropRestrictionsCheckerTest extends TestCase {

  public void testSystemGetPropertyFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        class Main {
          public final static String COMPILE_TIME_CONSTANT ="constant";
          public static void main(){
            System.getProperty(COMPILE_TIME_CONSTANT);
          > Error: Method 'String System.getProperty(String)' can only take a string literal as its first parameter
            String s="property";
            System.getProperty(s);
          > Error: Method 'String System.getProperty(String)' can only take a string literal as its first parameter
            System.getProperty(COMPILE_TIME_CONSTANT,"default");
          > Error: Method 'String System.getProperty(String, String)' can only take a string literal as its first parameter
          }
        }
        """);
  }

  public void testSystemGetPropertySucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            class Main {
              public static void main(){
                System.getProperty("java.runtime.name");
                System.getProperty("java.runtime.name","default");
                System.getProperty("pro"+"perty");
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testCollidingNamePackagePrivateFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
          @JsMethod
          void m() {}
          @JsMethod
          void n() {}
        }
        """,
        "somePackage.SubBuggy",
        """
        import jsinterop.annotations.*;
        public class SubBuggy extends test.Buggy {
          @JsMethod
          void m() {}
        > Error: 'void Buggy.m()' and 'void SubBuggy.m()' cannot both use the same JavaScript name 'm'.
          @JsMethod
          public void n() {} // public but not overriding due to different packages.
        > Error: 'void Buggy.n()' and 'void SubBuggy.n()' cannot both use the same JavaScript name 'n'.
        }
        """);
  }

  public void testCollidingNameInInterfaceFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        interface Foo {
          @JsMethod(name = "doIt")
          default void maybeDoIt(Foo foo) {}
        }
        public class Buggy implements Foo {
          @JsMethod
          void doIt(Foo foo) {}
        > Error: 'void Buggy.doIt(Foo)' and 'void Foo.maybeDoIt(Foo)' cannot both use the same JavaScript name 'doIt'.
        }
        """);
  }

  public void testCollidingAccidentalOverrideConcreteMethodFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        interface Foo {
          void doIt(Foo foo);
        }
        @JsType
        interface Bar {
          void doIt(Bar bar);
        }
        class ParentBuggy {
          public void doIt(Foo foo) {}
          public void doIt(Bar bar) {}
        }
        public class Buggy extends ParentBuggy implements Foo, Bar {
        > Error: 'void Bar.doIt(Bar)' and 'void Foo.doIt(Foo)' cannot both use the same JavaScript name 'doIt'.
        }
        """);
  }

  public void testCollidingAccidentalOverrideAbstractMethodFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        interface Foo {
          void doIt(Foo foo);
        }
        @JsType
        interface Bar {
          void doIt(Bar bar);
        }
        abstract class Baz implements Foo, Bar {
          public abstract void doIt(Foo foo);
          public abstract void doIt(Bar bar);
        > Error: 'void Baz.doIt(Bar)' and 'void Baz.doIt(Foo)' cannot both use the same JavaScript name 'doIt'.
        }
        public class Buggy {}  // Unrelated class
        """);
  }

  public void testCollidingAccidentalOverrideHalfAndHalfFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        interface Bar {
           void doIt(Bar bar);
        }
        class ParentParent {
          public void doIt(Bar x) {}
        }
        @JsType
        class Parent extends ParentParent {
          public void doIt(String x) {}
        }
        public class Buggy extends Parent implements Bar {}
        > Error: 'void Bar.doIt(Bar)' and 'void Parent.doIt(String)' cannot both use the same JavaScript name 'doIt'.
        """);
  }

  public void testOverrideNoNameSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            class Parent {
              @JsMethod(name = "a")
              public void ma() {}
              @JsMethod(name = "b")
              public void mb() {}
            }
            @JsType
            class Child1 extends Parent {
              public void ma() {}
              public void mb() {}
            }
            class Child2 extends Parent {
              @JsMethod
              public void ma() {}
              @JsMethod
              public void mb() {}
            }
            class Child3 extends Parent {
              public void ma() {}
              public void mb() {}
            }
            @JsType
            class Child4 extends Parent {
              @JsIgnore
              public void ma() {}
              @JsIgnore
              public void mb() {}
            }
            public class Buggy extends Parent {
              Child1 c1;
              Child2 c2;
              Child3 c3;
              Child4 c4;
            }
            """)
        .assertNoWarnings();
  }

  public void testCollidingFieldsFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
          @JsProperty
          public static final int show = 0;
          @JsProperty(name = "show")
        > Error: 'Buggy.display' and 'Buggy.show' cannot both use the same JavaScript name 'show'.
          public static final int display = 0;
        }
        """);
  }

  public void testJsPropertyNonGetterStyleSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            public interface Buggy {
              @JsProperty(name = "x") int x();
              @JsProperty(name = "x") void x(int x);
              // Following examples start with "is" but not bean style.
              @JsProperty(name = "issue") int y();
              @JsProperty(name = "issue") void y(int y);
              @JsProperty(name = "z") int issuer();
              @JsProperty(name = "z") void issuer(int i);
              @JsProperty(name = "issuers") int issuers();
              @JsProperty(name = "issuers") void issuers(int i);
            }
            """)
        .assertNoWarnings();
  }

  public void testJsPropertyGetterStyleSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            public abstract class Buggy {
              @JsProperty static native int getStaticX();
              @JsProperty static native void setStaticX(int x);
              @JsProperty abstract int getX();
              @JsProperty abstract void setX(int x);
              @JsProperty abstract boolean isY();
              @JsProperty abstract void setY(boolean y);
              // Getter style with customized name becomes non getter style
              @JsProperty(name = "isEnabled") abstract int isEnabled();
            }
            """)
        .assertNoWarnings();
  }

  public void testJsPropertyIncorrectGetterStyleFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public interface Buggy {
          @JsProperty int isX();
        > Error: JsProperty 'int Buggy.isX()' cannot have a non-boolean return.
          @JsProperty int getY(int x);
        > Error: JsProperty 'int Buggy.getY(int x)' should have a correct property accessor signature.
          @JsProperty void getZ();
        > Error: JsProperty 'void Buggy.getZ()' should have a correct property accessor signature.
          @JsProperty void setX(int x, int y);
        > Error: JsProperty 'void Buggy.setX(int x, int y)' should have a correct property accessor signature.
          @JsProperty void setY();
        > Error: JsProperty 'void Buggy.setY()' should have a correct property accessor signature.
          @JsProperty int setZ(int z);
        > Error: JsProperty 'int Buggy.setZ(int z)' should have a correct property accessor signature.
          @JsProperty static void setStatic() {}
        > Error: JsProperty 'void Buggy.setStatic()' should have a correct property accessor signature.
          @JsProperty void setW(int... z);
        > Error: JsProperty 'void Buggy.setW(int... z)' cannot have a vararg parameter.
        }
        """);
  }

  public void testJsPropertyNonGetterStyleFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        public interface Buggy {
          @JsProperty boolean hasX();
        > Error: JsProperty 'boolean Buggy.hasX()' should either follow Java Bean naming conventions or provide a name.
          @JsProperty int x();
        > Error: JsProperty 'int Buggy.x()' should either follow Java Bean naming conventions or provide a name.
          @JsProperty void x(int x);
        > Error: JsProperty 'void Buggy.x(int x)' should either follow Java Bean naming conventions or provide a name.
        }
        """);
  }

  public void testCollidingJsPropertiesTwoGettersFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        public interface Buggy {
          @JsProperty
          boolean isX();
          @JsProperty
          boolean getX();
        > Error: 'boolean Buggy.getX()' and 'boolean Buggy.isX()' cannot both use the same JavaScript name 'x'.
        }
        """);
  }

  public void testCollidingNativeJsPropertiesSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true)
            public class Buggy {
              @JsMethod
              public native int now();
              @JsProperty
              public native Object getNow();
              @JsMethod
              public static native int other();
              @JsProperty
              public static native Object getOther();
              @JsMethod
              public static native int another();
              @JsProperty
              public static Object another;
            }
            """)
        .assertNoWarnings();
  }

  public void testCollidingJsPropertiesTwoSettersFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        public interface Buggy {
          @JsProperty
          void setX(boolean x);
          @JsProperty
          void setX(int x);
        > Error: 'void Buggy.setX(boolean)' and 'void Buggy.setX(int)' cannot both use the same JavaScript name 'x'.
        }
        """);
  }

  public void testMultipleCollidingJsPropertiesFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        public interface Buggy {
          @JsProperty
          void setY(boolean x);
          @JsMethod(name = "y")
          void setX(boolean x);
          @JsProperty
          boolean getY();
        > Error: 'boolean Buggy.getY()' and 'void Buggy.setY(boolean)' cannot both use the same JavaScript name 'y'.
          // TODO(b/277967621): This error message is incorrect, in this case there are multiple
          // members colliding on the same name but the error reporter selects the wrong ones to
          // emit the message.
        }
        """);
  }

  public void testCollidingJsMethodAndJsPropertyGetterFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        interface IBuggy {
          @JsMethod
          boolean x(boolean foo);
          @JsProperty
          int getX();
        > Error: 'boolean IBuggy.x(boolean)' and 'int IBuggy.getX()' cannot both use the same JavaScript name 'x'.
        }
        public class Buggy implements IBuggy {
          public boolean x(boolean foo) {return false;}
          public int getX() {return 0;}
        > Error: 'boolean Buggy.x(boolean)' and 'int Buggy.getX()' cannot both use the same JavaScript name 'x'.
        }
        """);
  }

  public void testCollidingJsMethodAndJsPropertySetterFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        interface IBuggy {
          @JsMethod
          boolean x(boolean foo);
          @JsProperty
          void setX(int a);
        > Error: 'boolean IBuggy.x(boolean)' and 'void IBuggy.setX(int)' cannot both use the same JavaScript name 'x'.
        }
        public class Buggy implements IBuggy {
          public boolean x(boolean foo) {return false;}
          public void setX(int a) {}
        > Error: 'boolean Buggy.x(boolean)' and 'void Buggy.setX(int)' cannot both use the same JavaScript name 'x'.
        }
        """);
  }

  public void testCollidingPropertyAccessorsFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
          @JsProperty
          public static void setDisplay(int x) {}
          @JsProperty(name = "display")
          public static void setDisplay2(int x) {}
        > Error: 'void Buggy.setDisplay(int)' and 'void Buggy.setDisplay2(int)' cannot both use the same JavaScript name 'display'.
        }
        """);
  }

  public void testCollidingMethodsFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
          @JsMethod
          public static void show() {}
          @JsMethod(name = "show")
          public static void display() {}
        > Error: 'void Buggy.display()' and 'void Buggy.show()' cannot both use the same JavaScript name 'show'.
        }
        """);
  }

  public void testCollidingMethodToPropertyAccessorFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
          @JsProperty
          public static void setShow(int x) {}
          @JsMethod
          public static void show() {}
        > Error: 'void Buggy.setShow(int)' and 'void Buggy.show()' cannot both use the same JavaScript name 'show'.
        }
        """);
  }

  public void testCollidingMethodToFieldFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
          @JsMethod
          public static void show() {}
          @JsProperty
        > Error: 'Buggy.show' and 'void Buggy.show()' cannot both use the same JavaScript name 'show'.
          public static final int show = 0;
        }
        """);
  }

  public void testCollidingMethodToFieldJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        public class Buggy {
          public void show() {}
          public final int show = 0;
        > Error: 'Buggy.show' and 'void Buggy.show()' cannot both use the same JavaScript name 'show'.
        }
        """);
  }

  public void testCollidingJsMethodWithObjectMethod() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        interface Buggy {
          @JsMethod(name = "equals")
          boolean notEquals(Object o);
        > Error: 'boolean Buggy.notEquals(Object)' and 'boolean Object.equals(Object)' cannot both use the same JavaScript name 'equals'.
        }
        """);
  }

  public void testCollidingMethodToMethodJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        public class Buggy {
          public void show(int x) {}
          public void show() {}
        > Error: 'void Buggy.show()' and 'void Buggy.show(int)' cannot both use the same JavaScript name 'show'.
        }
        """);
  }

  public void testValidCollidingSubclassMembersSucceeds() {
    assertTranspileSucceeds(
            "test.NonJsTypeSubclass",
            """
            import jsinterop.annotations.*;
            @JsType
            class JsTypeParent {
              JsTypeParent() {}
              public int foo = 55;
              public void bar() {}
            }
            public class NonJsTypeSubclass extends JsTypeParent {
              public int foo = 110;
              public int bar = 110;
              public void foo(int a) {}
              public void bar(int a) {}
            }
            class PlainJavaParent {
              public int foo = 55;
              public void foo() {}
            }
            @JsType
             class AJsTypeSubclass extends PlainJavaParent {
              public int foo = 110;
            }
            @JsType
            class AnotherJsTypeSubclass extends PlainJavaParent {
              public void foo(int a) {}
            }
            """)
        .assertNoWarnings();
  }

  public void testCollidingSubclassMembersJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        class ParentBuggy {
          public int foo = 55;
        }
        @JsType
        public class Buggy extends ParentBuggy {
          public int foo = 110;
        > Error: 'Buggy.foo' and 'ParentBuggy.foo' cannot both use the same JavaScript name 'foo'.
        }
        @JsType
        class OtherBuggy extends ParentBuggy {
          public void foo(int a) {}
        > Error: 'ParentBuggy.foo' and 'void OtherBuggy.foo(int)' cannot both use the same JavaScript name 'foo'.
        }
        """);
  }

  public void testCollidingSubclassMethodToMethodInterfaceJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        interface IBuggy1 {
          void show();
        }
        @JsType
        interface IBuggy2 {
          void show(boolean b);
        }
        public class Buggy implements IBuggy1 {
          public void show() {}
        }
        class Buggy2 extends Buggy implements IBuggy2 {
          public void show(boolean b) {}
        > Error: 'void Buggy.show()' and 'void Buggy2.show(boolean)' cannot both use the same JavaScript name 'show'.
        }
        interface IBuggy3 {
          @JsMethod(name = "display")
          void show(boolean b);
        }
        class Buggy3 implements IBuggy2, IBuggy3 {
          public void show(boolean b) {}
        > Error: 'void Buggy3.show(boolean b)' cannot be assigned JavaScript name 'display' that is different from the JavaScript name of a method it overrides ('void IBuggy2.show(boolean)' with JavaScript name 'show').
        }
        class Main {
          public static void main() {
            Object o;
            // TODO(b/71911586): This lambda should be rejected but it is not.
            o = (IBuggy2 & IBuggy3) (b) -> {};
          }
        }
        """);
  }

  public void testRenamedSubclassMethodToBridgeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            public class Buggy<T> {
              @JsMethod
              public void show(T t) {}
              @JsMethod(name = "display")
              public void show(String s) {}
            }
            class SubBuggy extends Buggy<String> {
              public void show(String s) {}
            }
            """)
        .assertNoWarnings();
  }

  public void testCollidingSubclassMethodToMethodJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        class ParentBuggy {
          public void foo() {}
        }
        @JsType
        public class Buggy extends ParentBuggy {
          public void foo(int a) {}
        > Error: 'void Buggy.foo(int)' and 'void ParentBuggy.foo()' cannot both use the same JavaScript name 'foo'.
        }
        """);
  }

  public void testCollidingJsMethodToJsPropertyFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        class ParentBuggy {
          @JsMethod boolean foo() { return false; }
          @JsProperty boolean getBar() { return false; }
          // TODO(b/35901141): Implement missing restriction check.
          @JsProperty @JsMethod boolean getBax() { return false; }
          @JsProperty boolean getBlah() { return false; }
          @JsMethod(name = "bleh") boolean getBleh() { return false; }
        }
        public class Buggy extends ParentBuggy {
          @JsProperty boolean getFoo() { return false; }
        > Error: 'boolean Buggy.getFoo()' and 'boolean ParentBuggy.foo()' cannot both use the same JavaScript name 'foo'.
          @JsMethod boolean bar() { return false; }
        > Error: 'boolean Buggy.bar()' and 'boolean ParentBuggy.getBar()' cannot both use the same JavaScript name 'bar'.
          @JsProperty boolean getBleh() { return false; }
        > Error: JsProperty 'boolean Buggy.getBleh()' cannot override JsMethod 'boolean ParentBuggy.getBleh()'.
          @JsMethod(name = "blah") boolean getBlah() { return false; }
        > Error: JsMethod 'boolean Buggy.getBlah()' cannot override JsProperty 'boolean ParentBuggy.getBlah()'.
        }
        """);
  }

  public void testCollidingSubclassMethodToMethodTwoLayerInterfaceJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        interface IParentBuggy1 {
          void show();
        }
        interface IBuggy1 extends IParentBuggy1 {
        }
        @JsType
        interface IParentBuggy2 {
          void show(boolean b);
        }
        interface IBuggy2 extends IParentBuggy2 {
        }
        public class Buggy implements IBuggy1 {
          public void show() {}
        }
        class Buggy2 extends Buggy implements IBuggy2 {
          public void show(boolean b) {}
        > Error: 'void Buggy.show()' and 'void Buggy2.show(boolean)' cannot both use the same JavaScript name 'show'.
        }
        """);
  }

  public void testNonCollidingSyntheticBridgeMethodSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            interface Comparable<T> {
              int compareTo(T other);
            }
            @JsType
            class Enum<E extends Enum<E>> implements Comparable<E> {
              public int compareTo(E other) {return 0;}
            }
            public class Buggy {}
            """)
        .assertNoWarnings();
  }

  public void testCollidingSyntheticBridgeMethodSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            interface Comparable<T> {
              int compareTo(T other);
            }
            @JsType
            class Enum<E extends Enum<E>> implements Comparable<E> {
              public int compareTo(E other) {return 0;}
            }
            public class Buggy {}
            """)
        .assertNoWarnings();
  }

  public void testSpecializeReturnTypeInImplementorSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            interface I {
              I m();
            }
            @JsType
            class Buggy implements I {
              public Buggy m() { return null; }
            }
            """)
        .assertNoWarnings();
  }

  public void testSpecializeReturnTypeInSubclassSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            class S {
              public S m() { return null; }
            }
            @JsType
            public class Buggy extends S {
              public Buggy m() { return null; }
            }
            """)
        .assertNoWarnings();
  }

  public void testCollidingTwoLayerSubclassFieldToFieldJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        class ParentParentBuggy {
          protected ParentParentBuggy() {}
          public int foo = 55;
        }
        class ParentBuggy extends ParentParentBuggy {
          public int foo = 55;
        }
        @JsType
        public class Buggy extends ParentBuggy {
          public int foo = 110;
        > Error: 'Buggy.foo' and 'ParentParentBuggy.foo' cannot both use the same JavaScript name 'foo'.
        }
        """);
  }

  public void testShadowedSuperclassJsMethodFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        class ParentBuggy {
          @JsMethod private void foo() {}
        }
        public class Buggy extends ParentBuggy {
          @JsMethod private void foo() {}
        > Error: 'void Buggy.foo()' and 'void ParentBuggy.foo()' cannot both use the same JavaScript name 'foo'.
        }
        """);
  }

  public void testRenamedSuperclassJsMethodFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        class ParentBuggy {
          ParentBuggy() {}
          public void foo() {}
        }
        public class Buggy extends ParentBuggy {
          @JsMethod(name = "bar") public void foo() {}
        > Error: 'void Buggy.foo()' cannot be assigned JavaScript name 'bar' that is different from the JavaScript name of a method it overrides ('void ParentBuggy.foo()' with JavaScript name 'foo').
        }
        """);
  }

  public void testRenamedSuperInterfaceJsMethodFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        interface ParentBuggy {
          void foo();
        }
        public interface Buggy extends ParentBuggy {
          @JsMethod(name = "bar") void foo();
        > Error: 'void Buggy.foo()' cannot be assigned JavaScript name 'bar' that is different from the JavaScript name of a method it overrides ('void ParentBuggy.foo()' with JavaScript name 'foo').
        }
        """);
  }

  // TODO(b/37579830): Finalize checker implementation and enable this test.
  public void disabled_testAccidentallyRenamedSuperInterfaceJsMethodFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        interface IBuggy {
          void foo();
        }
        @JsType
        class ParentBuggy {
          @JsMethod(name = "bar") public void foo() {}
        }
        public class Buggy extends ParentBuggy implements IBuggy {
        > Error: 'void EntryPoint.ParentBuggy.foo()' (exposed by 'EntryPoint.Buggy') cannot be assigned a different JavaScript name than the method it overrides.
        }
        """);
  }

  public void testRenamedSuperclassJsPropertyFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        class ParentBuggy {
          @JsProperty public int getFoo() { return 0; }
        }
        public class Buggy extends ParentBuggy {
          @JsProperty(name = "bar") public int getFoo() { return 0; }
        > Error: 'int Buggy.getFoo()' cannot be assigned JavaScript name 'bar' that is different from the JavaScript name of a method it overrides ('int ParentBuggy.getFoo()' with JavaScript name 'foo').
        }
        """);
  }

  public void testJsPropertyDifferentFlavourInSubclassFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        class ParentBuggy {
          ParentBuggy() {}
          @JsProperty public boolean isFoo() { return false; }
        }
        public class Buggy extends ParentBuggy {
          @JsProperty public boolean getFoo() { return false;}
        > Error: 'boolean Buggy.getFoo()' and 'boolean ParentBuggy.isFoo()' cannot both use the same JavaScript name 'foo'.
        }
        """);
  }

  public void testConsistentPropertyTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            interface IBuggy {
              @JsProperty
              public int getFoo();
              @JsProperty
              public void setFoo(int value);
            }
            public class Buggy implements IBuggy {
              public int getFoo() {return 0;}
              public void setFoo(int value) {}
            }
            """)
        .assertNoWarnings();
  }

  public void testInconsistentGetSetPropertyTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        interface IBuggy {
          @JsProperty
          public int getFoo();
          @JsProperty
          public void setFoo(Integer value);
        > Error: JsProperty setter 'void IBuggy.setFoo(Integer)' and getter 'int IBuggy.getFoo()' cannot have inconsistent types.
        }
        public class Buggy implements IBuggy {
          public int getFoo() {return 0;}
          public void setFoo(Integer value) {}
        > Error: JsProperty setter 'void Buggy.setFoo(Integer)' and getter 'int Buggy.getFoo()' cannot have inconsistent types.
        }
        """);
  }

  public void testInconsistentIsSetPropertyTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        interface IBuggy {
          @JsProperty
          public boolean isFoo();
          @JsProperty
          public void setFoo(Object value);
        > Error: JsProperty setter 'void IBuggy.setFoo(Object)' and getter 'boolean IBuggy.isFoo()' cannot have inconsistent types.
        }
        public class Buggy implements IBuggy {
          public boolean isFoo() {return false;}
          public void setFoo(Object value) {}
        > Error: JsProperty setter 'void Buggy.setFoo(Object)' and getter 'boolean Buggy.isFoo()' cannot have inconsistent types.
        }
        """);
  }

  public void testJsPropertySuperCallSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType class Super {
              @JsProperty public int getX() { return 5; }
            }
            @JsType public class Buggy extends Super {
              public int m() { return super.getX(); }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsPropertyOnStaticMethodSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType public class Buggy {
              @JsProperty public static int getX() { return 0; }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsPropertyCallSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType class Super {
              @JsProperty public int getX() { return 5; }
            }
            @JsType public class Buggy extends Super {
              public int m() { return getX(); }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsPropertyAccidentalSuperCallSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType class Super {
              @JsProperty public int getX() { return 5; }
            }
            @JsType interface Interface {
              @JsProperty int getX();
            }
            @JsType public class Buggy extends Super implements Interface {
            }
            """)
        .assertNoWarnings();
  }

  public void testJsPropertyOverrideSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType class Super {
              @JsProperty public void setX(int x) {  }
              @JsProperty public int getX() { return 5; }
            }
            @JsType public class Buggy extends Super {
              @JsProperty public void setX(int x) {  }
            }
            class OverrideWithoutJsType extends Super {
              @JsProperty public void setX(int x) {  }
            }
            class OverrideWithoutJsPropertyNorJsType extends Super {
              public void setX(int x) {  }
            }
            @JsType class OverrideWithoutJsProperty extends Super {
              public void setX(int x) {  }
            }
            """)
        .assertNoWarnings();
  }

  public void testMixingJsMethodJsPropertyFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        class Super {
          @JsMethod public int getY() { return 5; }
          @JsProperty public void setX(int x) {}
          @JsProperty(name = "setZ")  public void setZ(int z) {}
        }
        public class Buggy extends Super {
          @JsProperty(name = "getY") public int getY() { return 6; }
          > Error: JsProperty 'int Buggy.getY()' cannot override JsMethod 'int Super.getY()'.
          @JsMethod public void setX(int x) {  }
          > Error: JsMethod 'void Buggy.setX(int x)' cannot override JsProperty 'void Super.setX(int)'.
          @JsMethod public void setZ(int z) {}
          > Error: JsMethod 'void Buggy.setZ(int z)' cannot override JsProperty 'void Super.setZ(int)'.
        }
        """);
  }

  // GWT enforces some restriction on JSNI JsMethods. In J2CL,  JSNI is just a comment and no test
  // should fail for JSNI reasons.
  public void testJsMethodJSNIVarargsSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            public class Buggy {
              @JsMethod public native void m(int i, int... z) /*-{ return arguments[i]; }-*/;
              // The next method fails in GWT but should not fail in J2CL.
              @JsMethod public native void n(int i, int... z) /*-{ return z[0];}-*/;
            }

            """)
        .assertNoWarnings();
  }

  public void testMultiplePrivateConstructorsSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            public class Buggy {
              private Buggy() {}
              private Buggy(int a) {}
            }
            """)
        .assertNoWarnings();
  }

  public void testJsConstructorSubclassSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            public class Buggy {
              public Buggy() {}
              @JsIgnore
              public Buggy(int a) {
                this();
              }
              public void m() {
                new Buggy() {};
                class LocalBuggy extends Buggy {
                  @JsConstructor
                  public LocalBuggy() {}
                }
              }
            }
            class SubBuggy extends Buggy {
              public SubBuggy() { this(1);}
              @JsConstructor
              public SubBuggy(int a) { super();}
            }
            @JsType
            class JsSubBuggy extends Buggy {
              @JsIgnore
              public JsSubBuggy() { this(1);}
              public JsSubBuggy(int a) { super();}
            }
            @JsType (isNative = true)
            class NativeBuggy {
              public NativeBuggy() {}
              public NativeBuggy(int a) {}
            }
            @JsType (isNative = true)
            class NativeSubNativeBuggy extends NativeBuggy {
              public NativeSubNativeBuggy() { super(1); }
              public NativeSubNativeBuggy(int a) { super();}
            }
            class SubNativeBuggy extends NativeBuggy {
              @JsConstructor
              public SubNativeBuggy() { super(1);}
              public SubNativeBuggy(int a) { this(); }
            }
            @JsType
            class ExplicitSubImplicitSuper extends ImplicitSubNativeSuper {
              public ExplicitSubImplicitSuper() { super(); }
            }
            """)
        .addCompilationUnit(
            "test.ImplicitConstructor",
            """
            import jsinterop.annotations.*;
            @JsType
            public class ImplicitConstructor extends Buggy {
            }
            """)
        .addCompilationUnit(
            "test.SubImplicitConstructor",
            """
            import jsinterop.annotations.*;
            @JsType
            public class SubImplicitConstructor extends ImplicitConstructor {
            }
            """)
        .addCompilationUnit(
            "test.ImplicitSubImplicitSuper",
            """
            import jsinterop.annotations.*;
            @JsType
            public class ImplicitSubImplicitSuper extends ImplicitSubNativeSuper {
            }
            """)
        .addCompilationUnit(
            "test.ImplicitSubNativeSuper",
            """
            import jsinterop.annotations.*;
            @JsType
            public class ImplicitSubNativeSuper extends NativeBuggy {
            }
            """)
        .addCompilationUnit(
            "test.ImplicitSubExplicitSuper",
            """
            import jsinterop.annotations.*;
            @JsType
            public class ImplicitSubExplicitSuper extends ExplicitSubImplicitSuper {
            }
            """)
        .assertTranspileSucceeds();
  }

  public void testJsConstructorBadSubclassFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        class BuggyJsType {
          public BuggyJsType() {}
          @JsIgnore
          public BuggyJsType(int a) { this(); }
          public void m() {
            new BuggyJsType(2) {};
          > Error: JsConstructor '<anonymous> extends BuggyJsType(int)' can only delegate to super JsConstructor 'BuggyJsType()'.
            class LocalBuggy extends BuggyJsType {
              @JsConstructor
              public LocalBuggy() { super(3); }
            > Error: JsConstructor 'LocalBuggy()' can only delegate to super JsConstructor 'BuggyJsType()'.
            }
          }
        }
        public class Buggy extends BuggyJsType {
          > Error: Class 'Buggy' should have a JsConstructor.
          public Buggy() {}
          public Buggy(int a) { super(a); }
        }
        class SubBuggyJsType extends BuggyJsType {
          // Correct: one JsConstructor delegating to the super JsConstructor.
          public SubBuggyJsType() { this(1); }
          @JsConstructor
          public SubBuggyJsType(int a) { super(); }
        }
        class BadImplicitConstructor extends SubBuggyJsType {
        > Error: Class 'BadImplicitConstructor' should have a JsConstructor.
        }
        class DelegatingToImplicitConstructor extends ImplicitConstructor {
          public DelegatingToImplicitConstructor(int a) { }
        }
        class SubSubBuggyJsType extends SubBuggyJsType {
          public SubSubBuggyJsType() { this(1);}
          @JsConstructor
          public SubSubBuggyJsType(int a) { super(); }
        > Error: JsConstructor 'SubSubBuggyJsType(int)' can only delegate to super JsConstructor 'SubBuggyJsType(int)'.
        }
        class OtherSubBuggyJsType extends BuggyJsType {
        > Error: JsConstructor 'OtherSubBuggyJsType(int)' can be a JsConstructor only if all other constructors in the class delegate to it.
          public OtherSubBuggyJsType() { super();}
          @JsConstructor
          public OtherSubBuggyJsType(int a) { this(); }
        }
        class AnotherSubBuggyJsType extends BuggyJsType {
        > Error: More than one JsConstructor exists for 'AnotherSubBuggyJsType'.
          @JsConstructor
          public AnotherSubBuggyJsType() { super();}
          @JsConstructor
          public AnotherSubBuggyJsType(int a) { this(); }
        }
        // TODO(b/129550499): The following class should be fine because only the JsConstructor
        // delegates to super. However the normalization pass does not handle those cases
        // correctly.
        class DelegatingConstructorChain extends BuggyJsType {
        > Error: Constructor 'DelegatingConstructorChain(int a, int b)' should delegate to the JsConstructor 'DelegatingConstructorChain()'. (b/129550499)
          @JsConstructor
          public DelegatingConstructorChain() { super();}
          public DelegatingConstructorChain(int a) { this(); }
          public DelegatingConstructorChain(int a, int b) { this(a); }
        }
        @JsType(isNative=true) class NativeType {
          NativeType() { }
          NativeType(int i) { }
        }
        class SomeClass2 extends NativeType {
        > Error: Class 'SomeClass2' should have a JsConstructor.
        }
        """,
        "test.BadImplicitJsConstructor",
        """
        import jsinterop.annotations.*;
        @JsType
        public class BadImplicitJsConstructor extends SubBuggyJsType {
        > Error: Implicit JsConstructor 'BadImplicitJsConstructor()' can only delegate to super JsConstructor 'SubBuggyJsType(int)'.
        }
        """,
        "test.ImplicitConstructor",
        """
        import jsinterop.annotations.*;
        public class ImplicitConstructor extends BuggyJsType {
        > Error: Class 'ImplicitConstructor' should have a JsConstructor.
        }
        """,
        "test.ImplicitJsConstructor",
        """
        import jsinterop.annotations.*;
        @JsType
        public class ImplicitJsConstructor extends BuggyJsType {
          // Correct: Implicit JsConstructor delegating to the JsConstructor.
        }
        """);
  }

  public void testMultipleConstructorsNotAllDelegatedToJsConstructorFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        public class Buggy {
        > Error: JsConstructor 'Buggy()' can be a JsConstructor only if all other constructors in the class delegate to it.
          public Buggy() {}
          private Buggy(int a) {
            new Buggy();
          }
        }
        """);
  }

  public void testMultipleJsConstructorsFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        public class Buggy {
        > Error: More than one JsConstructor exists for 'Buggy'.
          public Buggy() {}
          public Buggy(int a) {
            this();
          }
        }
        """);
  }

  public void testNonCollidingAccidentalOverrideSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            interface Foo {
              void doIt(Object foo);
            }
            class ParentParent {
              public void doIt(String x) {}
            }
            @JsType
            class Parent extends ParentParent {
              Parent() {}
              public void doIt(Object x) {}
            }
            public class Buggy extends Parent implements Foo {}
            """)
        .assertNoWarnings();
  }

  public void testJsNameInvalidNamesFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(name = "a.b.c") public class Buggy {
           > Error: 'Buggy' has invalid name 'a.b.c'.
           @JsMethod(name = "34s") public void m() {}
           > Error: 'void Buggy.m()' has invalid name '34s'.
           @JsProperty(name = "s^") public int  m;
           > Error: 'Buggy.m' has invalid name 's^'.
           @JsProperty(name = "") public int n;
           > Error: 'Buggy.n' cannot have an empty name.
           @JsMethod(name = "a.b") static void o() {}
           > Error: 'void Buggy.o()' has invalid name 'a.b'.
           @JsMethod(name = "") static void p() {}
           > Error: 'void Buggy.p()' cannot have an empty name.
           @JsMethod(name = "") void q() {}
           > Error: 'void Buggy.q()' cannot have an empty name.
           @JsMethod(name = "") static native void r();
           > Error: 'void Buggy.r()' cannot have an empty name.
           @JsMethod(namespace = "34s", name = "") static native void s();
           > Error: 'void Buggy.s()' has invalid namespace '34s'.
           @JsProperty(name = "a.c") static int t;
           > Error: 'Buggy.t' has invalid name 'a.c'.
           @JsProperty(name = "") int u;
           > Error: 'Buggy.u' cannot have an empty name.
           @JsProperty(namespace ="a.b.c", name = "") int v;
           > Error: 'Buggy.v' cannot have an empty name.
           @JsProperty(namespace = "34s", name = "") static native int w();
        > Error: 'int Buggy.w()' has invalid namespace '34s'.
        }
        @JsType(namespace = JsPackage.GLOBAL, name = "a.b.d")
        class OtherBuggy { }
        > Error: 'OtherBuggy' has invalid name 'a.b.d'.
        @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
        class BadGlobalStar { }
        > Error: Only native interfaces in the global namespace can be named '*'.
        @JsType(namespace = JsPackage.GLOBAL, name = "?") interface BadGlobalWildcard { }
        > Error: Only native interfaces in the global namespace can be named '?'.
        @JsType(isNative = true, namespace = "a.b", name = "*") interface BadStar { }
        > Error: Only native interfaces in the global namespace can be named '*'.
        @JsEnum(isNative = true, namespace = "a.b", name = "=") enum NativeEnum { }
        > Error: 'NativeEnum' has invalid name '='.
        @JsEnum(namespace = "a.b", name = "c.d") enum MyJsEnum { }
            > Error: 'MyJsEnum' has invalid name 'c.d'.
        """);
  }

  public void testJsNameEmptyNamesSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            public class Buggy {
               @JsMethod(namespace = "foo.buzz", name = "") static native void buzz();
               @JsProperty(namespace = "foo.bar", name = "") static native String bar();
            }
            """)
        .assertNoWarnings();
  }

  public void testJsNameInvalidNamespacesFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType class Super {
           @JsMethod(name = "custom") public void r() {}
        }
        @JsType(namespace = "a.b.") public class Buggy extends Super {
        > Error: 'Buggy' has invalid namespace 'a.b.'.
           @JsMethod(namespace = "34s") public native static void m();
           > Error: 'void Buggy.m()' has invalid namespace '34s'.
           @JsMethod(namespace = "") public native static void o();
           > Error: 'void Buggy.o()' cannot have an empty namespace.
           @JsProperty(namespace = "") public int p;
           > Error: Instance member 'Buggy.p' cannot declare a namespace.
           @JsMethod(namespace = "a") public void q() {}
           > Error: Instance member 'void Buggy.q()' cannot declare a namespace.
           @JsMethod(namespace = "b") public void r() {}
           > Error: Instance member 'void Buggy.r()' cannot declare a namespace.
        }
        @JsType(isNative = true) class NativeClass {
           @JsProperty(namespace = "s^") public static int  n;
           > Error: 'NativeClass.n' has invalid namespace 's^'.
        }
        @JsType(namespace = "<window>") class JsTypeOnWindow {
           @JsProperty(namespace = "<window>") public static int r;
           > Error: Non-native member 'JsTypeOnWindow.r' cannot declare a namespace.
           @JsMethod(namespace = "<window>") public static  void s() {}
           > Error: Non-native member 'void JsTypeOnWindow.s()' cannot declare a namespace.
        }
        @JsType(namespace = JsPackage.GLOBAL) class InvalidGlobal {
           @JsMethod(namespace = JsPackage.GLOBAL) public static void m() {}
           > Error: Non-native member 'void InvalidGlobal.m()' cannot declare a namespace.
           @JsProperty(namespace = JsPackage.GLOBAL) public static int n;
           > Error: Non-native member 'InvalidGlobal.n' cannot declare a namespace.
        }
        @JsType(namespace = "") class NonNativeClass {}
        > Error: 'NonNativeClass' cannot have an empty namespace.
        @JsEnum(isNative = true, namespace = "=") enum NativeEnum { }
        > Error: 'NativeEnum' has invalid namespace '='.
        @JsEnum(namespace = "^") enum MyJsEnum { }
        > Error: 'MyJsEnum' has invalid namespace '^'.
        """);
  }

  public void testJsNameGlobalNamespacesSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(namespace = JsPackage.GLOBAL) public class Buggy {
               @JsMethod(namespace = JsPackage.GLOBAL) public static native void m();
               @JsMethod(namespace = JsPackage.GLOBAL, name = "a.b")
               public static native void o();
            }
            @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "a.c")
            class NativeOnGlobalNamespace {
               @JsMethod(namespace = JsPackage.GLOBAL, name = "a.d") static native void o();
               @JsMethod(namespace = JsPackage.GLOBAL, name = "a.e") static native void getP();
               @JsProperty(namespace = JsPackage.GLOBAL, name = "a.f") public static int n;
               @JsProperty(namespace = JsPackage.GLOBAL) public static int m;
            }
            @JsType(isNative = true, namespace = "<window>", name = "a.g")
            class NativeOnWindowNamespace {
               @JsMethod(namespace = "<window>", name = "a.h") static native void q();
               @JsMethod(namespace = "<window>", name = "a.i") static native void getR();
               @JsProperty(namespace = "<window>", name = "a.j") public static int s;
            }
            @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
            interface Star {
            }
            @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
            interface Wildcard {
            }
            """)
        .assertNoWarnings();
  }

  public void testNativeJsTypeEmptyNamespaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true, namespace = "", name = "a.c")
            class NativeOnTopLevelNamespace {}
            """)
        .assertNoWarnings();
  }

  public void testSingleJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            public class Buggy {
              public static void show1() {}
              public void show2() {}
            }
            """)
        .assertNoWarnings();
  }

  public void testJsFunctionSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsFunction
            interface Function {
              int getFoo();
              @JsOverlay
              static String s = new String();
              @JsOverlay
              default void m() {}
              @JsOverlay
              static void n() {}
            }
            public final class Buggy implements Function {
              public int getFoo() { return 0; }
              public final void blah() {}
              public void blat() {}
              private void bleh() {}
              static void blet() {
                new Function() {
                   public int getFoo() { return 0; }
                }.getFoo();
              }
              {}
              static {}
              String x = new String();
              static int y;
            }
            @JsFunction
            interface Function2 {
              Object getFoo();
            }
            final class Buggy2 implements Function2 {
              public String getFoo() { return null;}
            }
            @JsFunction
            interface ParametricFunction<T> {
              Object getFoo(T t);
            }
            final class ImplementationWithBridge implements ParametricFunction<String> {
              public Double getFoo(String s) { return new Double(0); }
            }
            class Main {
              public static void main() {
                Object o;
                o = (ParametricFunction<?>) (s) -> (Double) null;
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsFunctionInvalidInterfaceFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsFunction
        interface Function {
          int getFoo();
        }
        @JsFunction
        interface InvalidFunction {
          @JsProperty
          int getFoo();
        > Error: JsFunction interface member 'int InvalidFunction.getFoo()' cannot be JsMethod nor JsProperty nor JsConstructor.
          default void m() {}
        > Error: JsFunction interface 'InvalidFunction' cannot declare non-JsOverlay member 'void InvalidFunction.m()'.
          int f = 0;
        > Error: JsFunction interface 'InvalidFunction' cannot declare non-JsOverlay member 'InvalidFunction.f'.
          static void n() {}
        > Error: JsFunction interface 'InvalidFunction' cannot declare non-JsOverlay member 'void InvalidFunction.n()'.
        }
        @JsFunction
        interface JsFunctionExtendsInterface extends Cloneable {
        > Error: JsFunction 'JsFunctionExtendsInterface' cannot extend other interfaces.
          void foo();
        }
        interface InterfaceExtendsJsFunction extends Function {}
        > Error: 'InterfaceExtendsJsFunction' cannot extend JsFunction 'Function'.
        class BaseClass {}
        final class JsFunctionExtendingBaseClass extends BaseClass implements Function {
        > Error: JsFunction implementation 'JsFunctionExtendingBaseClass' cannot extend a class.
          public int getFoo() { return 0; }
        }
        final class JsFunctionMultipleInterfaces implements Cloneable, Function {
        > Error: JsFunction implementation 'JsFunctionMultipleInterfaces' cannot implement more than one interface.
          public int getFoo() { return 0; }
        }
        @JsFunction @JsType
        interface InvalidJsTypeJsFunction {
        > Error: 'InvalidJsTypeJsFunction' cannot be both a JsFunction and a JsType at the same time.
          void n();
        > Error: JsFunction interface member 'void InvalidJsTypeJsFunction.n()' cannot be JsMethod nor JsProperty nor JsConstructor.
        }
        @JsFunction
        class InvalidJsFunctionClass {
        > Error: JsFunction 'InvalidJsFunctionClass' has to be a functional interface.
        }
        interface Foo {
          int getFoo();
        }
        final class JsFunctionImplementingDefaultMethod implements Foo, Function {
        > Error: JsFunction implementation 'JsFunctionImplementingDefaultMethod' cannot implement more than one interface.
          public int getFoo() { return 0; }
        }
        @JsFunction
        interface FunctionWithDefaultMethod {
        > Error: JsFunction 'FunctionWithDefaultMethod' has to be a functional interface.
          default int getFoo() { return 0; }
        }
        @JsFunction
        interface JsFunctionWithMethodDefinedTypeVariable {
          <T> void m();
        > Error: JsFunction 'void JsFunctionWithMethodDefinedTypeVariable.m()' cannot declare type parameters. Type parameters must be declared on the enclosing interface instead.
        }
        """);
  }

  public void testJsFunctionInvalidImplementationFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsFunction
        interface Function {
          int getFoo();
        }
        public final class Buggy implements Function {
          @JsConstructor
          Buggy() { }
        > Error: JsFunction implementation member 'Buggy()' cannot be JsMethod nor JsProperty nor JsConstructor.
          @JsProperty
          public int getFoo() { return 0; }
        > Error: JsFunction implementation member 'int Buggy.getFoo()' cannot be JsMethod nor JsProperty nor JsConstructor.
          @JsMethod
          private void bleh() {}
        > Error: JsFunction implementation member 'void Buggy.bleh()' cannot be JsMethod nor JsProperty nor JsConstructor.
          @JsMethod
          private native void nativeMethod();
        > Error: JsFunction implementation method 'void Buggy.nativeMethod()' cannot be native.
          @JsProperty
        > Error: JsFunction implementation member 'Buggy.prop' cannot be JsMethod nor JsProperty nor JsConstructor.
          public int prop = 0;
          public String toString() { return ""; }
        > Error: JsFunction implementation method 'String Buggy.toString()' cannot override a supertype method.
          public boolean equals(Object o) { return false; }
        > Error: JsFunction implementation method 'boolean Buggy.equals(Object)' cannot override a supertype method.
          public int hashCode() { return 0; }
        > Error: JsFunction implementation method 'int Buggy.hashCode()' cannot override a supertype method.
        }
        class NonFinalJsFunction implements Function {
        > Error: JsFunction implementation 'NonFinalJsFunction' must be final.
          public int getFoo() { return 0; }
        }
        @JsType
        final class JsFunctionMarkedAsJsType implements Function {
        > Error: 'JsFunctionMarkedAsJsType' cannot be both a JsFunction implementation and a JsType at the same time.
          public int getFoo() { return 0; }
        > Error: JsFunction implementation member 'int JsFunctionMarkedAsJsType.getFoo()' cannot be JsMethod nor JsProperty nor JsConstructor.
        }
        abstract class AbstractFunctionImplementation implements Function {
        > Error: JsFunction implementation 'AbstractFunctionImplementation' must be final.
        }
        interface Foo {
          int getFoo();
        }
        class Main {
          public static void main() {
            Object o = (Foo & Function) () -> 0;
        > Error: JsFunction lambda can only implement the JsFunction interface.
          }
        }
        """);
  }

  public void testJsFunctionRecursiveFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        import java.util.List;
        @JsFunction
        interface MutuallyRecursiveFunctionA {
          void m(MutuallyRecursiveFunctionB f);
        > Error: JsFunction 'void MutuallyRecursiveFunctionA.m(MutuallyRecursiveFunctionB f)' cannot refer recursively to itself (via MutuallyRecursiveFunctionA MutuallyRecursiveFunctionB.m()). (b/153591461)
        }
        @JsFunction
        interface MutuallyRecursiveFunctionB {
          MutuallyRecursiveFunctionA m();
        > Error: JsFunction 'MutuallyRecursiveFunctionA MutuallyRecursiveFunctionB.m()' cannot refer recursively to itself (via void MutuallyRecursiveFunctionA.m(MutuallyRecursiveFunctionB)). (b/153591461)
        }
        @JsFunction
        interface WithoutError {
          void m(IndirectReference f);
        }
        @JsFunction
        interface IndirectReference {
          void m(List<List<IndirectReference>> f);
        > Error: JsFunction 'void IndirectReference.m(List<List<IndirectReference>> f)' cannot refer recursively to itself. (b/153591461)
        }
        @JsFunction
        interface JsFunctionNotInvolvedInCycle<T> {
          void m(T t);
        }
        @JsFunction
        interface IndirectReferenceThroughJsFunction {
          void m(JsFunctionNotInvolvedInCycle<IndirectReferenceThroughJsFunction> f);
        > Error: JsFunction 'void IndirectReferenceThroughJsFunction.m(JsFunctionNotInvolvedInCycle<IndirectReferenceThroughJsFunction> f)' cannot refer recursively to itself. (b/153591461)
        }
        @JsFunction
        interface ArrayReference {
          void m(IndirectReference[] f);
        }
        @JsFunction
        interface TypeVariableReference<T extends TypeVariableReference<T>> {
          void m(T f);
        > Error: JsFunction 'void TypeVariableReference.m(T f)' cannot refer recursively to itself. (b/153591461)
        }
        @JsFunction
        interface TypeVariableWithIntersectionBound<T extends List<T> & TypeVariableWithIntersectionBound<T>> {
          void m(T f);
        > Error: JsFunction 'void TypeVariableWithIntersectionBound.m(T f)' cannot refer recursively to itself. (b/153591461)
        }
        """);
  }

  public void testJsFunctionImplementationInstanceofFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsFunction
        interface Function {
          int getFoo();
        }
        final class Buggy implements Function {
          public int getFoo() { return 0; }
        }
        class Main {
          public static void main() {
            if (new Object() instanceof Buggy) {}
        > Error: Cannot do instanceof against JsFunction implementation 'Buggy'.
          }
        }
        """);
  }

  public void testNativeJsTypeStaticInitializerSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Buggy {
              static {
                int x = 1;
              }
            }
            @JsType(isNative = true)
            class Buggy2 {
              static {
                Object.class.getName();
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testNativeJsTypeInstanceInitializerFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        public class Buggy {
        > Error: Native JsType 'Buggy' cannot have an instance initializer.
          {
            Object.class.getName();
          }
        }
        """);
  }

  public void testNativeJsTypeNonEmptyConstructorFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        public class Buggy {
          public Buggy(int n) {
        > Error: Native JsType constructor 'Buggy(int n)' cannot have non-empty method body.
            n++;
          }
        }
        """);
  }

  public void testNativeJsTypeImplicitSuperSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Buggy extends Super {
              public Buggy(int n) {}
            }
            """)
        .addCompilationUnit(
            "test.Super",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Super {
              public Super() {}
            }
            """)
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testNativeJsTypeExplicitSuperSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Buggy extends Super {
              public Buggy(int n) {
                super(n);
              }
            }
            """)
        .addCompilationUnit(
            "test.Super",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Super {
              public Super(int x) {}
            }
            """)
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testNativeJsTypeExplicitSuperWithEffectSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Buggy extends Super {
              public Buggy(int n) {
                super(n++);
              }
            }
            """)
        .addCompilationUnit(
            "test.Super",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Super {
              public Super(int x) {}
            }
            """)
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testJsTypeInterfaceInInstanceofFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative=true) interface IBuggy {}
        public class Buggy {
          public Buggy() { if (new Object() instanceof IBuggy) {} }
        > Error: Cannot do instanceof against native JsType interface 'IBuggy'.
        }
        """);
  }

  public void testJsTypeInterfaceInPatternFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative=true) interface IBuggy {}
        record R(IBuggy b) {}
        public class Buggy {
          public Buggy() {
            switch (new Object()) {
             case IBuggy b -> {}
        > Error: Cannot pattern match against native JsType interface 'IBuggy'.
             case R(IBuggy b) -> {}
        > Error: Cannot pattern match against native JsType interface 'IBuggy'.
             default -> {}
            }
          }
        }
        """);
  }

  public void testNativeJsTypeEnumFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        public enum Buggy {
        > Error: Enum 'Buggy' cannot be a native JsType. Use '@JsEnum(isNative = true)' instead.
          A,
          B
        }
        """);
  }

  public void testJsEnumIllegalDeclarationsFails() {
    assertWithInlineMessages(
        "test.MyJsEnum",
        """
        import jsinterop.annotations.*;
        @JsEnum @JsType
        enum MyJsTypeJsEnum { }
        > Error: 'MyJsTypeJsEnum' cannot be both a JsEnum and a JsType at the same time.
        @JsEnum
        interface InterfaceWithJsEnum { }
        > Error: JsEnum 'InterfaceWithJsEnum' has to be an enum type.
        interface MyInterface { }
        @JsEnum
        enum JsEnumImplementingInterface implements MyInterface { }
        > Error: JsEnum 'JsEnumImplementingInterface' cannot implement any interface.
        """);
  }

  public void testJsEnumConstructorsAndFieldsFails() {
    assertWithInlineMessages(
        "test.MyJsEnum",
        """
        import jsinterop.annotations.*;
        @JsEnum
        public enum MyJsEnum {
        > Error: Non-custom-valued JsEnum 'MyJsEnum' cannot have a field named 'value'.
          A;
          MyJsEnum() { }
        > Error: Non-custom-valued JsEnum 'MyJsEnum' cannot have constructor 'MyJsEnum()'.
          MyJsEnum(int x) { }
        > Error: Non-custom-valued JsEnum 'MyJsEnum' cannot have constructor 'MyJsEnum(int x)'.
          {}
        > Error: JsEnum 'MyJsEnum' cannot have an instance initializer.
          int instanceField;
        > Error: JsEnum 'MyJsEnum' cannot have instance field 'MyJsEnum.instanceField'.
          int value = 5;
        }
        """);
  }

  public void testJsEnumInvalidMembersFails() {
    assertWithInlineMessages(
        "test.JsEnumWithInvalidMembers",
        """
        import jsinterop.annotations.*;
        @JsEnum
        enum JsEnumWithInvalidMembers {
          A;
          @JsMethod static void m() {}
        > Error: JsEnum member 'void JsEnumWithInvalidMembers.m()' cannot be JsMethod nor JsProperty nor JsConstructor.
          @JsProperty static int getP() { return 1;}
        > Error: JsEnum member 'int JsEnumWithInvalidMembers.getP()' cannot be JsMethod nor JsProperty nor JsConstructor.
          @JsMethod static native void n();
        > Error: JsEnum method 'void JsEnumWithInvalidMembers.n()' cannot be native.
          @JsMethod native void o();
        > Error: JsEnum method 'void JsEnumWithInvalidMembers.o()' cannot be native.
          public String toString() { return null; }
        > Error: JsEnum method 'String JsEnumWithInvalidMembers.toString()' cannot override a supertype method.
        }
        """);
  }

  public void testJsEnumUnsupportedMethodsFails() {
    assertWithInlineMessages(
        "test.MyJsEnum",
        """
        import java.util.function.*;
        import jsinterop.annotations.*;
        @JsEnum
        public enum MyJsEnum {
          A;
          static void main() {
            Supplier<String> s = A::name;
        > Error: JsEnum 'MyJsEnum' does not support 'String Enum.name()'.
            MyJsEnum.values();
        > Error: JsEnum 'MyJsEnum' does not support 'MyJsEnum[] MyJsEnum.values()'. (b/118228329)
            MyJsEnum.valueOf(null);
        > Error: JsEnum 'MyJsEnum' does not support 'MyJsEnum MyJsEnum.valueOf(String)'.
          }
        }
        """);
  }

  public void testJsEnumOverlayAndSuperFails() {
    assertWithInlineMessages(
        "test.MyJsEnum",
        """
        import jsinterop.annotations.*;
        @JsEnum
        public enum MyJsEnum {
          A;
          @JsOverlay
          public final void anOverlayMethod() {
        > Error: JsOverlay 'void MyJsEnum.anOverlayMethod()' can only be declared in a native type or @JsFunction interface.
            super.ordinal();
          }
          public final void aMethod() {
        > Error: Cannot use 'super' in JsEnum method 'void MyJsEnum.aMethod()'.
            super.name();
        > Error: JsEnum 'MyJsEnum' does not support 'String Enum.name()'.
          }
        }
        """);
  }

  public void testJsEnumBoundsAndSpecializationFails() {
    assertWithInlineMessages(
        "test.MyJsEnum",
        """
        import jsinterop.annotations.*;
        @JsEnum enum MyJsEnum { A }
        class AListSubclass<T extends MyJsEnum> {}
        > Error: Type 'AListSubclass' cannot define a type variable with a JsEnum as a bound.
        abstract class WithFunctionReturningEnum {
          abstract Enum f();
        }
        abstract class SpecializingEnumToJsEnum extends WithFunctionReturningEnum {
          abstract MyJsEnum f();
        > Error: Method 'MyJsEnum SpecializingEnumToJsEnum.f()' cannot override method 'Enum WithFunctionReturningEnum.f()' with a JsEnum return type.
        }
        """);
  }

  public void testJsEnumConstantBodyFails() {
    assertWithInlineMessages(
        "test.MyJsEnum",
        """
        import jsinterop.annotations.*;
        @JsEnum
        public enum MyJsEnum {
          A {};
        > Error: JsEnum constant 'MyJsEnum.A' cannot have a class body.
        }
        """);
  }

  public void testJsEnumSucceeds() {
    assertTranspileSucceeds(
            "test.MyJsEnum",
            """
            import jsinterop.annotations.*;
            import java.util.function.*;
            @JsEnum
            public enum MyJsEnum {
              A,
              B;
              static void main() {
                A.ordinal();
                A.compareTo(B);
                if (((MyJsEnum) A) instanceof MyJsEnum) {}
                Object o = MyJsEnum.class;
                Consumer<MyJsEnum> consumer = c -> c.ordinal();
                Consumer<? super MyJsEnum> consumer2 = c -> c.ordinal();
                Supplier<? extends MyJsEnum> supplier = () -> MyJsEnum.A;
              }
              static int field = 5;
              static { }
              public static int aStaticField;
              public void anInstanceMethod() { }
              public static void aStaticMethod() { }
            }
            interface List<T> { Object getObject(); }
            class AList<T> { }
            class AListSubclass extends AList<MyJsEnum> implements List<MyJsEnum> {
                public MyJsEnum getObject() { return null; }
            }
            """)
        .assertNoWarnings();
  }

  public void testCustomValuedJsEnumConstructorsFails() {
    assertWithInlineMessages(
        "test.CustomValued",
        """
        import jsinterop.annotations.*;
        @JsEnum(hasCustomValue = true)
        public enum CustomValued {
          A(1);
          int value;
          CustomValued(int value) { this.value = value + 2; }
        > Error: Custom-valued JsEnum constructor 'CustomValued(int value)' should have one parameter of the value type and its body should only be the assignment to the value field.
        }
        @JsEnum(hasCustomValue = true)
        enum CustomValuedWithInvalidConstructor {
          A(1);
          int value;
          CustomValuedWithInvalidConstructor(int value) { /* Error: body should be assignment */ }
        > Error: Custom-valued JsEnum constructor 'CustomValuedWithInvalidConstructor(int value)' should have one parameter of the value type and its body should only be the assignment to the value field.
        }
        @JsEnum(hasCustomValue = true)
        enum CustomValuedWithExtraConstructor {
          A(1);
          int value;
          CustomValuedWithExtraConstructor(int value) { this.value = value; }
          CustomValuedWithExtraConstructor(boolean value) { /* Error: only one constructor allowed */ }
        > Error: Custom-valued JsEnum constructor 'CustomValuedWithExtraConstructor(boolean value)' should have one parameter of the value type and its body should only be the assignment to the value field.
        }
        @JsEnum(hasCustomValue = true)
        enum CustomValueMissingConstructor {
        > Error: Custom-valued JsEnum 'CustomValueMissingConstructor' should have a constructor.
          A;
          int value;
        }
        """);
  }

  public void testCustomValuedJsEnumValueFieldFails() {
    assertWithInlineMessages(
        "test.CustomValued",
        """
        import jsinterop.annotations.*;
        @JsEnum(hasCustomValue = true)
        public enum CustomValued {
          A(1);
          int value;
          CustomValued(int value) { this.value = value; }
        }
        @JsEnum(hasCustomValue = true)
        enum InvalidCustomValueType {
          A(1L);
          long value;
        > Error: Custom-valued JsEnum value field 'InvalidCustomValueType.value' cannot have the type 'long'. The only valid value types are 'int' and 'java.lang.String'. (b/295240966)
          InvalidCustomValueType(long value) { this.value = value; }
        }
        @JsEnum(hasCustomValue = true)
        enum InvalidCustomValueType2 {
          A(false);
          boolean value;
        > Error: Custom-valued JsEnum value field 'InvalidCustomValueType2.value' cannot have the type 'boolean'. The only valid value types are 'int' and 'java.lang.String'. (b/295240966)
          InvalidCustomValueType2(boolean value) { this.value = value; }
        }
        @JsEnum(hasCustomValue = true)
        enum InvalidCustomValueInitializer {
          A(1);
          int value = 5;
        > Error: Custom-valued JsEnum value field 'InvalidCustomValueInitializer.value' cannot have initializer.
          InvalidCustomValueInitializer(int value) { this.value = value; }
        }
        @JsEnum(hasCustomValue = true)
        enum CustomValueStaticField {
        > Error: Custom-valued JsEnum 'CustomValueStaticField' should have a constructor.
          A;
          static int value;
        > Error: Custom-valued JsEnum value field 'CustomValueStaticField.value' cannot be static nor JsOverlay nor JsMethod nor JsProperty.
        }
        """);
  }

  public void testCustomValuedJsEnumConstantsFails() {
    assertWithInlineMessages(
        "test.CustomValued",
        """
        import jsinterop.annotations.*;
        @JsEnum(hasCustomValue = true)
        public enum CustomValued {
          A("  ".length());
        > Error: Custom-valued JsEnum constant 'CustomValued.A' cannot have a non-literal value.
          int value;
          CustomValued(int value) { this.value = value; }
        }
        @JsEnum(hasCustomValue = true)
        enum CustomValueWithIntegerMinValue {
          A(Integer.MIN_VALUE),
        > Error: Custom-valued JsEnum constant 'CustomValueWithIntegerMinValue.A' cannot be equal to Integer.MIN_VALUE.
          B(-2147483648),
        > Error: Custom-valued JsEnum constant 'CustomValueWithIntegerMinValue.B' cannot be equal to Integer.MIN_VALUE.
          C(Integer.MIN_VALUE + 1 - 1);
        > Error: Custom-valued JsEnum constant 'CustomValueWithIntegerMinValue.C' cannot be equal to Integer.MIN_VALUE.
          int value;
          CustomValueWithIntegerMinValue(int value) { this.value = value; }
        }
        """);
  }

  public void testCustomValuedJsEnumInvalidMembersFails() {
    assertWithInlineMessages(
        "test.CustomValued",
        """
        import jsinterop.annotations.*;
        @JsEnum(hasCustomValue = true)
        public enum CustomValued {
          A(1);
          @JsProperty int value;
        > Error: Custom-valued JsEnum value field 'CustomValued.value' cannot be static nor JsOverlay nor JsMethod nor JsProperty.
          @JsConstructor CustomValued(int value) { this.value = value; }
        > Error: JsEnum member 'CustomValued(int value)' cannot be JsMethod nor JsProperty nor JsConstructor.
          @JsMethod static void m() {}
        > Error: JsEnum member 'void CustomValued.m()' cannot be JsMethod nor JsProperty nor JsConstructor.
        }
        """);
  }

  public void testCustomValuedJsEnumUnsupportedMethodsFails() {
    assertWithInlineMessages(
        "test.CustomValued",
        """
        import jsinterop.annotations.*;
        @JsEnum(hasCustomValue = true)
        public enum CustomValued {
          A(1),
          B(2);
          int value;
          CustomValued(int value) { this.value = value; }
          static void main() {
            A.name();
        > Error: JsEnum 'CustomValued' does not support 'String Enum.name()'.
            B.ordinal();
        > Error: Custom-valued JsEnum 'CustomValued' does not support 'int Enum.ordinal()'.
            A.compareTo(B);
        > Error: Custom-valued JsEnum 'CustomValued' does not support 'int Enum.compareTo(CustomValued)'.
            CustomValued.values();
        > Error: JsEnum 'CustomValued' does not support 'CustomValued[] CustomValued.values()'. (b/118228329)
            CustomValued.valueOf(null);
        > Error: JsEnum 'CustomValued' does not support 'CustomValued CustomValued.valueOf(String)'.
          }
        }
        """);
  }

  public void testCustomValuedJsEnumSucceeds() {
    assertTranspileSucceeds(
            "test.CustomValued",
            """
            import jsinterop.annotations.*;
            @JsEnum(hasCustomValue = true)
            public enum CustomValued {
              A(1),
              B(-1),
              C(0),
              D(1 + 1);
              final int value;
              CustomValued(int value) { this.value = value;}
            }
            """)
        .assertNoWarnings();
  }

  public void testNativeJsEnumUnsupportedMethodsFails() {
    assertWithInlineMessages(
        "test.Native",
        """
        import jsinterop.annotations.*;
        @JsEnum(isNative = true)
        public enum Native {
          A,
          B;
          @JsOverlay
          static void main() {
            A.name();
        > Error: JsEnum 'Native' does not support 'String Enum.name()'.
            B.ordinal();
        > Error: Native JsEnum 'Native' does not support 'int Enum.ordinal()'.
            Native.values();
        > Error: JsEnum 'Native' does not support 'Native[] Native.values()'. (b/118228329)
            Native.valueOf(null);
        > Error: JsEnum 'Native' does not support 'Native Native.valueOf(String)'.
          }
        }
        """);
  }

  public void testNativeJsEnumInvalidMembersFails() {
    assertWithInlineMessages(
        "test.Native",
        """
        import jsinterop.annotations.*;
        @JsEnum(isNative = true)
        public enum Native {
          A;
          static void main() { }
        > Error: Native JsEnum 'Native' cannot declare non-JsOverlay member 'void Native.main()'.
          static int staticField = 1;
        > Error: Native JsEnum 'Native' cannot declare non-JsOverlay member 'Native.staticField'.
          int instanceField;
        > Error: JsEnum 'Native' cannot have instance field 'Native.instanceField'.
        }
        """);
  }

  public void testNativeJsEnumIllegalDeclarationsFails() {
    assertWithInlineMessages(
        "test.Native",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        interface NativeInterface { }
        @JsEnum(isNative = true)
        enum NativeJsEnumImplementingNativeInterface implements NativeInterface { }
        > Error: JsEnum 'NativeJsEnumImplementingNativeInterface' cannot implement any interface.
        @JsEnum(isNative = true, hasCustomValue = true)
        enum NativeJsEnumDeclaringCustomValueButNoField { }
        > Error: Custom-valued JsEnum 'NativeJsEnumDeclaringCustomValueButNoField' does not have a field named 'value'.
        @JsEnum(isNative = true)
        enum NativeJsEnumNotDeclaringCustomValueButWithValueField {
        > Error: Non-custom-valued JsEnum 'NativeJsEnumNotDeclaringCustomValueButWithValueField' cannot have a field named 'value'.
          A,
          B;
          int value = 1;
        }
        """);
  }

  public void testNativeJsEnumInstanceofAndLiteralFails() {
    assertWithInlineMessages(
        "test.Native",
        """
        import jsinterop.annotations.*;
        @JsEnum(isNative = true)
        public enum Native {
          A;
          @JsOverlay
          static void main() {
            if (A instanceof Native) { }
        > Error: Cannot do instanceof against native JsEnum 'Native'.
            Object o = Native.class;
        > Error: Cannot use native JsEnum literal 'Native.class'.
          }
        }
        """);
  }

  public void testNativeJsEnumSucceeds() {
    assertTranspileSucceeds(
            "test.Native",
            """
            import jsinterop.annotations.*;
            @JsEnum(isNative = true)
            public enum Native {
              A,
              B;
              @JsOverlay
              public void anOverlayMethod() {
                A.compareTo(B);
              }
            }
            @JsEnum(isNative = true, hasCustomValue = true)
            enum NativeJsEnumWithCustomValue {
              A,
              B;
              int value;
            }
            interface List<T> { Object getObject(); }
            class AList<Native> { }
            class AListSubclass<T extends Native>
                extends AList<Native> implements List<Native>  {
                public Native getObject() { return null; }
            }
            interface Consumer<T> {
              void accept(T t);
              static void test() {
                Consumer<Native> consumer = c -> c.toString();
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsEnumAssignmentExpressionFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import java.io.Serializable;
        import jsinterop.annotations.*;
        public class Main {
        > Error: Custom-valued JsEnum value field 'JsEnumWithCustomValue.value' cannot be assigned.
          @JsEnum(isNative = true) enum Native {A, B}
          @JsEnum enum MyJsEnum {A, B}
          @JsEnum(hasCustomValue = true) enum JsEnumWithCustomValue {
            A(1),
            B(2);
            int value;
            JsEnumWithCustomValue(int value) { this.value = value; }
          }
          private static void main() {
            Enum e = Native.A;
        > Error: JsEnum 'Native' cannot be assigned to 'Enum'.
            e = MyJsEnum.A;
        > Error: JsEnum 'MyJsEnum' cannot be assigned to 'Enum'.
            e = JsEnumWithCustomValue.A;
        > Error: JsEnum 'JsEnumWithCustomValue' cannot be assigned to 'Enum'.
            Comparable c = JsEnumWithCustomValue.A;
        > Error: Custom-valued JsEnum 'JsEnumWithCustomValue' cannot be assigned to 'Comparable'.
            Serializable s = JsEnumWithCustomValue.A;
            JsEnumWithCustomValue.A.value = 3;
          }
        }
        """);
  }

  public void testJsEnumParameterizationFails() {
    // TODO(b/114468916): Replace with assertTranspileFails when the checks are improved..
    assertTranspileSucceeds(
            "test.MyJsEnum",
            """
            import java.util.*;
            import jsinterop.annotations.*;
            interface EnumList<E extends Enum<E>> {}
            @JsEnum enum MyJsEnum { A }
            class Main {
              // TODO(b/132736149): make sure the parameterization are rejected.
              EnumList<MyJsEnum> myJsEnumList = null;
              // TODO(b/114468916): The following should have failed. But for now they will be caught
              // by runtime checks when the erasure cast to Enum occurs.
              List<? extends Enum> l2 = new ArrayList<MyJsEnum>();
            }
            """)
        .assertNoWarnings();
  }

  public void testJsEnumParameterAssignmentFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import java.io.Serializable;
        import java.util.ArrayList;
        import java.util.List;
        import jsinterop.annotations.*;
        public class Main {
          @JsEnum(isNative = true) enum Native {A, B}
          @JsEnum enum MyJsEnum {A, B}
          @JsEnum(hasCustomValue = true) enum JsEnumWithCustomValue {
            A(1),
            B(2);
            int value;
            JsEnumWithCustomValue(int value) { this.value = value; }
          }
          private static void acceptsEnum(Enum e) {}
          private static void acceptsComparable(Comparable c) {}
          private static void acceptsSerializable(Serializable s) {}
          private static void main() {
            acceptsEnum(Native.A);
        > Error: JsEnum 'Native' cannot be assigned to 'Enum'.
            acceptsEnum(MyJsEnum.A);
        > Error: JsEnum 'MyJsEnum' cannot be assigned to 'Enum'.
            acceptsEnum(JsEnumWithCustomValue.A);
        > Error: JsEnum 'JsEnumWithCustomValue' cannot be assigned to 'Enum'.
            acceptsComparable(JsEnumWithCustomValue.A);
        > Error: Custom-valued JsEnum 'JsEnumWithCustomValue' cannot be assigned to 'Comparable'.
            List<Enum> l1 = null; l1.add(MyJsEnum.A);
        > Error: JsEnum 'MyJsEnum' cannot be assigned to 'Enum'.
          }
        }
        """);
  }

  public void testJsEnumAssignmentSucceeds() {
    assertTranspileSucceeds(
            "test.Main",
            """
            import java.io.Serializable;
            import java.util.ArrayList;
            import java.util.List;
            import jsinterop.annotations.*;
            public class Main {
              @JsEnum(isNative = true)
              public enum Native {
                A,
                B;
              }
              @JsEnum enum MyJsEnum {A, B}
              private static void acceptsObject(Object o) {}
              private static void acceptsSerializable(Serializable s) {}
              private static void acceptsComparable(Comparable c) {}
              private static void acceptsMyJsEnum(MyJsEnum c) {}
              private static void main() {
                {
                  Object o = Native.A;
                  acceptsObject(Native.A);
                  Serializable s = MyJsEnum.A;
                  acceptsSerializable(Native.A);
                  Comparable c = Native.A;
                  acceptsComparable(Native.A);
                  List<Native> l1 = null; l1.add(Native.A);
                  List<Object> l2 = null; l2.add(Native.A);
                  List<? extends Serializable> l3 = new ArrayList<Native>();
                }
                {
                  Object o = MyJsEnum.A;
                  acceptsObject(MyJsEnum.A);
                  Serializable s = MyJsEnum.A;
                  acceptsSerializable(MyJsEnum.A);
                  Comparable c = MyJsEnum.A;
                  acceptsComparable(MyJsEnum.A);
                  MyJsEnum myJsEnum = MyJsEnum.A;
                  acceptsMyJsEnum(MyJsEnum.A);
                  List<MyJsEnum> l1 = null; l1.add(MyJsEnum.A);
                  List<Object> l2 = null; l2.add(MyJsEnum.A);
                  List<? extends Serializable> l3 = new ArrayList<MyJsEnum>();
                }
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsEnumArraysSucceeds() {
    assertTranspileSucceeds(
            "Main",
            """
            import java.util.List;
            import java.util.function.Function;
            import jsinterop.annotations.*;
            public class Main<T> {
              @JsEnum enum MyJsEnum {A, B}
              MyJsEnum[] myJsEnum;
              private static void acceptsJsEnumArray(MyJsEnum[] p) {}
              private static void acceptsJsEnumVarargs(MyJsEnum... p) {}
              private static void acceptsJsEnumVarargsArray(MyJsEnum[]... p) {}
              private static <T> void acceptsTVarargs(T... p) {}
              private static MyJsEnum[] returnsJsEnumArray() { return null; }
              static class ParametricConstructorVarargs<E> {
                ParametricConstructorVarargs(E... p) {}
              }
              private static void arrays() {
                Object[] array = null;
                array[0] = MyJsEnum.A;
                List<MyJsEnum> l = null;
                List<MyJsEnum>[] ll = null;
                Object[] oarr = l.toArray();
                MyJsEnum[] arr = null;
                MyJsEnum[][] arr2 = null;
                acceptsJsEnumArray(new MyJsEnum[0]);
                acceptsJsEnumVarargs(new MyJsEnum[0]);
                acceptsJsEnumVarargsArray(new MyJsEnum[0]);
                // We generally disallow assignment of JsEnum arrays to generic T[]. However, we
                // specifically make an exception for varargs (we change the type of the passed array to
                // Object[] in a desugaring pass, JsEnums are allowed as elements of Object[]).
                acceptsTVarargs(MyJsEnum.A, MyJsEnum.B);
                new ParametricConstructorVarargs<>(MyJsEnum.A, MyJsEnum.B);
                MyJsEnum e = returnsJsEnumArray()[0];
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsEnumArrayAssignmentFails() {
    assertWithInlineMessages(
        "Main",
        """
        import jsinterop.annotations.*;
        public class Main {
          @JsEnum enum MyJsEnum {A, B}
          private static void acceptsObjectArray(Object[] p) {}
          private static void tests() {
            Object[] array = new MyJsEnum[0];
        > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'Object[]'.
            Object[] array2 = new MyJsEnum[0][];
        > Error: JsEnum array 'MyJsEnum[][]' cannot be assigned to 'Object[]'.
            Object[] array3 = (Object[]) new MyJsEnum[0];
        > Error: JsEnum array 'MyJsEnum[]' cannot be cast to 'Object[]'.
            acceptsObjectArray(new MyJsEnum[0]);
        > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'Object[]'.

            Object o = new MyJsEnum[1];
        > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'Object'.
            o = (MyJsEnum[]) o;
        > Error: 'Object' cannot be cast to JsEnum array 'MyJsEnum[]'.
        > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'Object'.
          }
        }
        """);
  }

  public void testJsEnumArrayBoundsAndSpecializationFails() {
    assertWithInlineMessages(
        "Main",
        """
        import java.util.List;
        import java.util.function.Function;
        import jsinterop.annotations.*;
        public class Main<T> {
          @JsEnum enum MyJsEnum {A, B}
          T t;
          T[] tArray;
          T[][] tArrayArray;
          List<T> tList;
          List<T[]> tArrayList;
          private static <T> T returnsT() { return null; }
          private static <T> T[] returnsTArray(T t) { return null; }
          private static <T> T[][] returnsTArrayArray(T t) { return null; }
          private static <T> List<T[]> returnsTArrayList(T t) { return null; }
          private static <T> List<T[][]> returnsTArrayArrayList(T t) { return null; }
          private List<T[]> instanceReturnsTArrayList() { return null; }
          private static <T> void acceptsT(T t) {}
          private static <T> void acceptsTArray(T[] t) {}
          private static <T> void acceptsTArrayArray(T[][] t) {}
          private static <T> void acceptsTVarargs(T... p) {}
          private static void acceptsJsEnumArrayListVarargs(List<MyJsEnum[]>... p) {}
        > Error: Parameter 'p' in 'void Main.acceptsJsEnumArrayListVarargs(List<MyJsEnum[]>... p)' cannot be of type 'List<MyJsEnum[]>[]'.
          private static void tests() {
            Object o = null;
            o = (Function<? extends Object, ? extends Object>) (MyJsEnum[] p1) -> p1;
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'R'.
            o = (Function<? extends Object, ? extends Object>) (MyJsEnum... p2) -> p2;
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'R'.
            acceptsT(new MyJsEnum[0]);
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'T'.
            acceptsTArray(new MyJsEnum[0]);
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'T[]'.
            acceptsTArrayArray(new MyJsEnum[0][]);
          > Error: JsEnum array 'MyJsEnum[][]' cannot be assigned to 'T[][]'.
            Main.<MyJsEnum>acceptsTVarargs(new MyJsEnum[0]);
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'T[]'.
            returnsTArray(MyJsEnum.A);
          > Error: Returned type in call to method 'MyJsEnum[] Main.returnsTArray(MyJsEnum)' cannot be of type 'MyJsEnum[]'.
            returnsTArrayArray(MyJsEnum.A);
          > Error: Returned type in call to method 'MyJsEnum[][] Main.returnsTArrayArray(MyJsEnum)' cannot be of type 'MyJsEnum[][]'.
            returnsTArrayList(MyJsEnum.A);
          > Error: Returned type in call to method 'List<MyJsEnum[]> Main.returnsTArrayList(MyJsEnum)' cannot be of type 'List<MyJsEnum[]>'.
            returnsTArrayArrayList(MyJsEnum.A);
          > Error: Returned type in call to method 'List<MyJsEnum[][]> Main.returnsTArrayArrayList(MyJsEnum)' cannot be of type 'List<MyJsEnum[][]>'.
            new Main<MyJsEnum[]>().instanceReturnsTArrayList();
          > Error: Object creation 'new Main.<init>()' cannot be of type 'Main<MyJsEnum[]>'.
          > Error: Returned type in call to method 'List<MyJsEnum[][]> Main.instanceReturnsTArrayList()' cannot be of type 'List<MyJsEnum[][]>'.
            MyJsEnum e;
            e = Main.<MyJsEnum[]>returnsT()[0];
          > Error: Returned type in call to method 'MyJsEnum[] Main.returnsT()' cannot be of type 'MyJsEnum[]'.
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'T'.
            e = new Main<MyJsEnum[]>().t[0];
          > Error: Object creation 'new Main.<init>()' cannot be of type 'Main<MyJsEnum[]>'.
          > Error: Reference to field 'Main<MyJsEnum[]>.t' cannot be of type 'MyJsEnum[]'.
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'T'.
            e = new Main<MyJsEnum>().tArray[0];
          > Error: Reference to field 'Main<MyJsEnum>.tArray' cannot be of type 'MyJsEnum[]'.
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'T[]'.
            e = new Main<MyJsEnum>().tArrayArray[0][0];
          > Error: Reference to field 'Main<MyJsEnum>.tArrayArray' cannot be of type 'MyJsEnum[][]'.
          > Error: JsEnum array 'MyJsEnum[][]' cannot be assigned to 'T[][]'.
            e = new Main<MyJsEnum[]>().tList.get(0)[0];
          > Error: Object creation 'new Main.<init>()' cannot be of type 'Main<MyJsEnum[]>'.
          > Error: Reference to field 'Main<MyJsEnum[]>.tList' cannot be of type 'List<MyJsEnum[]>'.
          > Error: Returned type in call to method 'MyJsEnum[] List.get(int)' cannot be of type 'MyJsEnum[]'.
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'E'.
            e = new Main<MyJsEnum>().tArrayList.get(0)[0];
          > Error: Reference to field 'Main<MyJsEnum>.tArrayList' cannot be of type 'List<MyJsEnum[]>'.
          > Error: Returned type in call to method 'MyJsEnum[] List.get(int)' cannot be of type 'MyJsEnum[]'.
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'E'.
            List<MyJsEnum[]> list = null;
          > Error: Variable 'list' cannot be of type 'List<MyJsEnum[]>'.
            DerivedWithJsEnumArrayField buggy = new DerivedWithJsEnumArrayField();
            e = buggy.arr[0];
          > Error: Reference to field 'BaseWithTArrayField<MyJsEnum>.arr' cannot be of type 'MyJsEnum[]'.
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'T[]'.
          }
        }
        class Derived extends Main<Main.MyJsEnum[]> {}
        > Error: Supertype of 'Derived' cannot be of type 'Main<MyJsEnum[]>'.
        class BaseWithTArrayParams<T>{
          void acceptsTVarargs(T... p) {}
          void acceptsTArray(T[] p) {}
          void acceptsTArrayArray(T[][] p) {}
          void acceptsTArrayList(List<T[]> p) {}
          void acceptsTArrayListArray(List<T[]>[] p) {}
          T[] returnsTArray() { return null; }
          T[][] returnsTArrayArray() { return null; }
          List<T[]> returnsTArrayList() { return null; }
          List<T[]>[] returnsTArrayListArray() { return null; }
        }
        class DerivedWithJsEnumArrayParams extends BaseWithTArrayParams<Main.MyJsEnum> {
          @Override void acceptsTVarargs(Main.MyJsEnum... p) {}
        > Error: Method 'void DerivedWithJsEnumArrayParams.acceptsTVarargs(MyJsEnum...)' cannot override method 'void BaseWithTArrayParams.acceptsTVarargs(Object...)' with a JsEnum array parameter.
          @Override void acceptsTArray(Main.MyJsEnum[] p) {}
        > Error: Method 'void DerivedWithJsEnumArrayParams.acceptsTArray(MyJsEnum[])' cannot override method 'void BaseWithTArrayParams.acceptsTArray(Object[])' with a JsEnum array parameter.
          @Override void acceptsTArrayArray(Main.MyJsEnum[][] p) {}
        > Error: Method 'void DerivedWithJsEnumArrayParams.acceptsTArrayArray(MyJsEnum[][])' cannot override method 'void BaseWithTArrayParams.acceptsTArrayArray(Object[][])' with a JsEnum array parameter.
          @Override void acceptsTArrayList(List<Main.MyJsEnum[]> p) {}
        > Error: Parameter 'p' in 'void DerivedWithJsEnumArrayParams.acceptsTArrayList(List<MyJsEnum[]> p)' cannot be of type 'List<MyJsEnum[]>'.
        > Error: Method 'void DerivedWithJsEnumArrayParams.acceptsTArrayList(List)' cannot override method 'void BaseWithTArrayParams.acceptsTArrayList(List)' with a JsEnum array parameter.
          @Override void acceptsTArrayListArray(List<Main.MyJsEnum[]>[] p) {}
        > Error: Parameter 'p' in 'void DerivedWithJsEnumArrayParams.acceptsTArrayListArray(List<MyJsEnum[]>[] p)' cannot be of type 'List<MyJsEnum[]>[]'.
        > Error: Method 'void DerivedWithJsEnumArrayParams.acceptsTArrayListArray(List[])' cannot override method 'void BaseWithTArrayParams.acceptsTArrayListArray(List[])' with a JsEnum array parameter.
          @Override Main.MyJsEnum[] returnsTArray() { return null; }
        > Error: Method 'MyJsEnum[] DerivedWithJsEnumArrayParams.returnsTArray()' cannot override method 'T[] BaseWithTArrayParams.returnsTArray()' with a JsEnum array return type.
          @Override Main.MyJsEnum[][] returnsTArrayArray() { return null; }
        > Error: Method 'MyJsEnum[][] DerivedWithJsEnumArrayParams.returnsTArrayArray()' cannot override method 'T[][] BaseWithTArrayParams.returnsTArrayArray()' with a JsEnum array return type.
          @Override List<Main.MyJsEnum[]> returnsTArrayList() { return null; }
        > Error: Return type of 'List<MyJsEnum[]> DerivedWithJsEnumArrayParams.returnsTArrayList()' cannot be of type 'List<MyJsEnum[]>'.
        > Error: Method 'List<MyJsEnum[]> DerivedWithJsEnumArrayParams.returnsTArrayList()' cannot override method 'List<T[]> BaseWithTArrayParams.returnsTArrayList()' with a JsEnum array return type.
          @Override List<Main.MyJsEnum[]>[] returnsTArrayListArray() { return null; }
        > Error: Return type of 'List<MyJsEnum[]>[] DerivedWithJsEnumArrayParams.returnsTArrayListArray()' cannot be of type 'List<MyJsEnum[]>[]'.
        > Error: Method 'List<MyJsEnum[]>[] DerivedWithJsEnumArrayParams.returnsTArrayListArray()' cannot override method 'List<T[]>[] BaseWithTArrayParams.returnsTArrayListArray()' with a JsEnum array return type.
        }
        class DerivedCallingSuperJsEnumVararg extends BaseWithTArrayParams<Main.MyJsEnum> {
          void callsSuperAcceptsTVarargs(Main.MyJsEnum... p) {
            super.acceptsTVarargs(p);
          > Error: JsEnum array 'MyJsEnum[]' cannot be assigned to 'T[]'.
          }
        }
        class BaseWithTArrayField<T>{
          T[] arr;
        }
        class DerivedWithJsEnumArrayField extends BaseWithTArrayField<Main.MyJsEnum> {}
        """);
  }

  public void testJsEnumArrayInstanceofAndMemberAccessFails() {
    assertWithInlineMessages(
        "Main",
        """
        import jsinterop.annotations.*;
        public class Main {
          @JsEnum enum MyJsEnum {A, B}
          private static void tests(Object o) {
            if (o instanceof MyJsEnum[]) { }
        > Error: Cannot do instanceof against JsEnum array 'MyJsEnum[]'.
            MyJsEnum[] arr = new MyJsEnum[0];
            arr.getClass();
        > Error: Cannot access member of 'Object' with JsEnum array 'MyJsEnum[]'.
          }
        }
        """);
  }

  public void testNativeJsEnumArraysSucceeds() {
    assertTranspileSucceeds(
            "test.Main",
            """
            import java.util.List;
            import java.util.function.Function;
            import jsinterop.annotations.*;
            public class Main {
              @JsEnum enum MyJsEnum {A, B}
              @JsEnum(isNative = true) enum NativeEnum {A, B}
              private static void arrays() {
                Object[] array = null;
                array[0] = MyJsEnum.A;
                List<MyJsEnum> l = null;
                Object[] oarr = l.toArray();
                Object o = new NativeEnum[1];
                NativeEnum[] arr = null;
                List<NativeEnum[]> list = null;
                o = (Function<? extends Object, ? extends Object>) (NativeEnum[] p1) -> p1;
                o = (Function<? extends Object, ? extends Object>) (NativeEnum... p2) -> p2;
                o = (NativeEnum[]) o;
                if (o instanceof NativeEnum[]) { }
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsEnumSwitchStatementSucceeds() {
    assertTranspileSucceeds(
            "test.Main",
            """
            import jsinterop.annotations.*;
            public class Main {
              @JsEnum enum NonNativeEnum { A, B }
              @JsEnum(isNative = true) enum NativeEnum { A, B }
              public static void test(NonNativeEnum nonNativeEnum, NativeEnum nativeEnum) {
                switch (nonNativeEnum) {
                  case A:
                  case B:
                    break;
                }
                switch (nonNativeEnum) {
                  case A:
                    break;
                }
                switch (nonNativeEnum) {
                  default:
                    break;
                }
                switch (nativeEnum) {
                  case A:
                  case B:
                    break;
                  default:
                }
                switch (nativeEnum) {
                  case A:
                    break;
                  default:
                }
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsEnumSwitchStatementFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import jsinterop.annotations.*;
        public class Main {
          @JsEnum(isNative = true) enum NativeEnum { A, B }
          public static void test(NativeEnum nativeEnum) {
            switch (nativeEnum) {
        > Error: Switch on native JsEnum 'NativeEnum' should have an explicit default branch. Add a default branch like:
        >   default: // Present for potential version skew with native JsEnums.
        >     ...
              case A:
              case B:
                break;
            }
            switch (nativeEnum) {
        > Error: Switch on native JsEnum 'NativeEnum' should have an explicit default branch. Add a default branch like:
        >   default: // Present for potential version skew with native JsEnums.
        >     ...
              case A:
                break;
            }
            switch (getNativeEnum()) {
        > Error: Switch on native JsEnum 'NativeEnum' should have an explicit default branch. Add a default branch like:
        >   default: // Present for potential version skew with native JsEnums.
        >     ...
              case A:
              case B:
                break;
            }
            switch (getTemplatedNativeEnum()) {
        > Error: Switch on native JsEnum 'NativeEnum' should have an explicit default branch. Add a default branch like:
        >   default: // Present for potential version skew with native JsEnums.
        >     ...
              case A:
              case B:
                break;
            }
          }
          public static NativeEnum getNativeEnum() {
            return NativeEnum.A;
          }
          public static <T extends NativeEnum> T getTemplatedNativeEnum() {
            return (T) NativeEnum.A;
          }
        }
        """);
  }

  public void testJsEnumSwitchExpressionSucceeds() {
    assertTranspileSucceeds(
            "test.Main",
            """
            import jsinterop.annotations.*;
            public class Main {
              @JsEnum enum NonNativeEnum { A, B }
              @JsEnum(isNative = true) enum NativeEnum { A, B }
              public static void test(NonNativeEnum nonNativeEnum, NativeEnum nativeEnum) {
                boolean b;
                b = switch (nonNativeEnum) {
                  case A, B -> true;
                };
                b = switch (nonNativeEnum) {
                  case A -> true;
                  default -> false;
                };
                b = switch (nonNativeEnum) {
                  default -> false;
                };
                b = switch (nativeEnum) {
                  case A, B -> true;
                  default -> false;
                };
                b = switch (nativeEnum) {
                  case A -> true;
                  default -> false;
                };
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsEnumSwitchExpressionFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import jsinterop.annotations.*;
        public class Main {
          @JsEnum(isNative = true) enum NativeEnum { A, B }
          public static void test(NativeEnum nativeEnum) {
            boolean b;
            b = switch (nativeEnum) {
        > Error: Switch on native JsEnum 'NativeEnum' should have an explicit default branch. Add a default branch like:
        >   // Present for potential version skew with native JsEnums.
        >   default -> ...
              case A, B -> true;
            };
            b = switch (getNativeEnum()) {
        > Error: Switch on native JsEnum 'NativeEnum' should have an explicit default branch. Add a default branch like:
        >   // Present for potential version skew with native JsEnums.
        >   default -> ...
              case A, B -> true;
            };
            b = switch (getTemplatedNativeEnum()) {
        > Error: Switch on native JsEnum 'NativeEnum' should have an explicit default branch. Add a default branch like:
        >   // Present for potential version skew with native JsEnums.
        >   default -> ...
              case A, B -> true;
            };
          }
          public static NativeEnum getNativeEnum() {
            return NativeEnum.A;
          }
          public static <T extends NativeEnum> T getTemplatedNativeEnum() {
            return (T) NativeEnum.A;
          }
        }
        """);
  }

  public void testInnerNativeJsTypeFails() {
    assertWithInlineMessages(
        "EntryPoint",
        """
        import jsinterop.annotations.*;
        public class EntryPoint {
          @JsType(isNative = true)
          public class Buggy {}
        > Error: Non static inner class 'Buggy' cannot be a native JsType.
        }
        """);
  }

  public void testInnerJsTypeSucceeds() {
    assertTranspileSucceeds(
            "EntryPoint",
            """
            import jsinterop.annotations.*;
            public class EntryPoint {
              @JsType
              public static class Buggy {}
            }
            """)
        .assertNoWarnings();
  }

  public void testLocalJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
          void m() {
            @JsType
            class Local {}
        > Error: Local class 'Local' cannot be a JsType.
          }
        }
        """);
  }

  public void testRecordJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        public record Buggy() {}
        > Error: Record class 'Buggy' cannot be a JsType. (b/470146353)
        @JsType(isNative = true)
        record NativeBuggy() {}
        > Error: Record class 'NativeBuggy' cannot be a JsType. (b/470146353)
        """);
  }

  public void testNativeJsTypeImplementsNativeJsTypeSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Buggy implements Super {}
            """)
        .addCompilationUnit(
            "test.Super",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public interface Super {}
            """)
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testNativeJsTypeInterfaceImplementsNativeJsTypeSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public interface Buggy extends Super {}
            """)
        .addCompilationUnit(
            "test.Super",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public interface Super {}
            """)
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testNativeJsTypeExtendsJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        public class Buggy extends Super {}
        > Error: Native JsType 'Buggy' can only extend native JsType classes.
        """,
        "test.Super",
        """
        import jsinterop.annotations.*;
        @JsType
        public class Super {}
        """);
  }

  public void testNativeJsTypeImplementsJsTypeInterfaceFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        public class Buggy implements Interface {}
        > Error: Native JsType 'Buggy' can only implement native JsType interfaces.
        """,
        "test.Interface",
        """
        import jsinterop.annotations.*;
        @JsType
        public interface Interface {}
        """);
  }

  public void testNativeJsTypeInterfaceExtendsJsTypeInterfaceFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        public interface Buggy extends Interface {}
        > Error: Native JsType 'Buggy' can only extend native JsType interfaces.
        """,
        "test.Interface",
        """
        import jsinterop.annotations.*;
        @JsType
        public interface Interface {}
        """);
  }

  public void testNativeJsTypeImplementsNonJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        public class Buggy implements Interface {}
        > Error: Native JsType 'Buggy' can only implement native JsType interfaces.
        """,
        "test.Interface",
        "public interface Interface {}");
  }

  public void testNativeJsTypeInterfaceExtendsNonJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        public interface Buggy extends Super {}
        > Error: Native JsType 'Buggy' can only extend native JsType interfaces.
        """,
        "test.Super",
        "public interface Super {}");
  }

  public void testNativeJsTypeInterfaceDefaultMethodsFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative=true) interface Interface {
          @JsOverlay default void someOtherMethod() {}
        }
        class OtherClass implements Interface {
          public void someOtherMethod() {}
        > Error: Method 'void OtherClass.someOtherMethod()' cannot override a JsOverlay method 'void Interface.someOtherMethod()'.
        }
        @JsType(isNative=true) public interface Buggy extends Interface {
          default void someMethod() {}
        > Error: Native JsType method 'void Buggy.someMethod()' should be native, abstract or JsOverlay.
          void someOtherMethod();
        > Error: Method 'void Buggy.someOtherMethod()' cannot override a JsOverlay method 'void Interface.someOtherMethod()'.
        }
        """);
  }

  public void testSealedClassExtendingNativeJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true)
            class NativeBase {}
            sealed class SealedClass extends NativeBase { @JsConstructor SealedClass() {} }
            final class Subtype extends SealedClass { @JsConstructor Subtype() {} }
            """)
        .assertNoWarnings();
  }

  public void testNativeJsTypeSealedTypeFails() {
    assertWithInlineMessages(
        "test.Foo",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        sealed class SealedNativeClass {}
        > Error: Sealed class 'SealedNativeClass' cannot be a native JsType.
        @JsType(isNative = true)
        sealed interface SealedNativeInterface {}
        > Error: Sealed interface 'SealedNativeInterface' cannot be a native JsType.
        @JsType(isNative = true)
        final class NativeSubtype extends SealedNativeClass implements SealedNativeInterface { @JsConstructor NativeSubtype() {} }
        > Error: Native JsType 'NativeSubtype' cannot implement sealed interface 'SealedNativeInterface'.
        > Error: Native JsType 'NativeSubtype' cannot extend sealed class 'SealedNativeClass'.
        @JsType(isNative = true)
        sealed class SealedNativeSubtype extends SealedNativeClass implements SealedNativeInterface {}
        > Error: Sealed class 'SealedNativeSubtype' cannot be a native JsType.
        > Error: Native JsType 'SealedNativeSubtype' cannot implement sealed interface 'SealedNativeInterface'.
        > Error: Native JsType 'SealedNativeSubtype' cannot extend sealed class 'SealedNativeClass'.
        final class FinalSubtype extends SealedNativeSubtype { @JsConstructor FinalSubtype() {} }
        """);
  }

  public void testJsOptionalSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "org.jspecify.annotations.Nullable",
            """
            @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
            public @interface Nullable {}
            """)
        .addCompilationUnit(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            import org.jspecify.annotations.*;
            public class Buggy<T> {
              @JsConstructor public Buggy(@JsOptional Object a) {}
              @JsMethod public void foo(int a, Object b, @JsOptional String c) {}
              @JsMethod public void bar(int a, @JsOptional Object b, @JsOptional String c) {}
              @JsMethod public void baz(@JsOptional String a, @JsOptional Object b) {}
              @JsMethod public void qux(@JsOptional String c, Object... os) {}
              @JsMethod public void corge(int a, @JsOptional @Nullable T b, Object... c) {}
            }
            class SubBuggy extends Buggy<String> {
              @JsConstructor
              public SubBuggy() { super(null); }
              @JsMethod public void corge(int a, @JsOptional String b, Object... c) {}
            }
            @JsFunction interface Function {
              void m(String a, @JsOptional String b);
            }
            final class FunctionImpl implements Function {
               public void m(String a, @JsOptional String b) {}
            }
            class Main {
              public static void main() {
                Object o;
                o = (Function) (String s, @JsOptional String b) -> {};
              }
            }
            """)
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testJsOptionalNotJsOptionalOverrideFails() {
    assertWithInlineMessages(
        "org.jspecify.annotations.Nullable",
        """
        @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
        public @interface Nullable {}
        """,
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        import org.jspecify.annotations.*;
        interface Interface {
          @JsMethod void foo(@JsOptional Object o);
          @JsMethod Object bar(@JsOptional Object o);
        }
        public class Buggy implements Interface {
          @Override
          @JsMethod public void foo(Object o) {}
          > Error: Method 'void Buggy.foo(Object o)' should declare parameter 'o' as JsOptional.
          @Override
          @JsMethod public String bar(Object o) { return null; }
        > Error: Method 'String Buggy.bar(Object o)' should declare parameter 'o' as JsOptional.
        }
        interface I<T> {
          @JsMethod void m(@JsOptional @Nullable T t);
        }
        class Implementor implements I<Integer> {
          public void m(Integer i) {}
        > Error: Method 'void Implementor.m(Integer i)' should declare parameter 'i' as JsOptional.
        }
        @JsFunction interface Function {
          void m(String a, @JsOptional String b);
        }
        class Main {
          public static void main() {
            Object o;
            // TODO(b/72319249): This should not pass restriction checks.
            o = (Function) (String s, String b) -> {};
          }
        }
        """);
  }

  public void testJsOptionalNotAtEndFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
           @JsConstructor
           public Buggy(@JsOptional String a, Object b, @JsOptional String c) {}
        > Error: JsOptional parameter 'a' in method 'Buggy(String a, Object b, String c)' cannot precede parameters that are not JsOptional.
           @JsMethod
           public void bar(int a, @JsOptional Object b, String c) {}
        > Error: JsOptional parameter 'b' in method 'void Buggy.bar(int a, Object b, String c)' cannot precede parameters that are not JsOptional.
           @JsMethod
           public void baz(@JsOptional Object a, String b, Object... c) {}
        > Error: JsOptional parameter 'a' in method 'void Buggy.baz(Object a, String b, Object... c)' cannot precede parameters that are not JsOptional.
        }
        """);
  }

  public void testJsOptionalOnInvalidParametersFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
           @JsConstructor public Buggy(@JsOptional int a) {}
        > Error: JsOptional parameter 'a' in method 'Buggy(int a)' cannot be of a primitive type.
           @JsMethod public void bar(int a, @JsOptional Object b, @JsOptional String... c) {}
        > Error: JsOptional parameter 'c' in method 'void Buggy.bar(int a, Object b, String... c)' cannot be a varargs parameter.
        }
        """);
  }

  public void testJsOptionalOnNonNullableParameterFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
           @JsMethod public <T> void bar(@JsOptional @JsNonNull T a, @JsOptional @JsNonNull String b) {}
           > Error: JsOptional parameter 'b' in method 'void Buggy.bar(T a, String b)' has to be nullable.
           > Error: JsOptional parameter 'a' in method 'void Buggy.bar(T a, String b)' has to be nullable.
        }
        """);
  }

  public void testJsOptionalOnNullableTypeVariableParameterSucceeds() {
    newTesterWithDefaults()
        .addArgs(
            "-experimentalenablejspecifysupportdonotenablewithoutjspecifystaticcheckingoryoumightcauseanoutage")
        .addCompilationUnit(
            "org.jspecify.annotations.NullMarked", "public @interface NullMarked {}")
        .addCompilationUnit(
            "org.jspecify.annotations.Nullable",
            """
            @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
            public @interface Nullable {}
            """)
        .addCompilationUnit(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            import org.jspecify.annotations.*;
            @NullMarked
            public class Buggy {
               @JsMethod public <T extends @Nullable Object> void bar(@JsOptional @Nullable T a, @JsOptional @Nullable String b) {}
            }
            """)
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testJsOptionalOnNonJsExposedMethodsFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        public class Buggy {
          public void fun(int a, @JsOptional Object b, @JsOptional String c) {}
        > Error: JsOptional parameter in 'void Buggy.fun(int a, Object b, String c)' can only be declared in a JsMethod, a JsConstructor or a JsFunction.
          @JsProperty public void setBar(@JsOptional Object o) {}
        > Error: JsOptional parameter in 'void Buggy.setBar(Object o)' can only be declared in a JsMethod, a JsConstructor or a JsFunction.
        }
        @JsType(isNative = true) class Native {
          @JsOverlay public final void fun( @JsOptional Object a) {}
        > Error: JsOptional parameter in 'void Native.fun(Object a)' can only be declared in a JsMethod, a JsConstructor or a JsFunction.
        }
        """);
  }

  public void testJsOverlayOnNativeJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public interface Buggy {
              @JsOverlay Object obj = new Object();
              @JsOverlay default void someOverlayMethod() {};
            }
            """)
        .assertNoWarnings();
  }

  public void testJsOverlayOnNativeJsTypeMemberSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) final class FinalType {
              @JsOverlay public void n() { }
            }
            @JsType(isNative=true) interface NativeInterface {
              @JsOverlay public static Object object = new Object();
              @JsOverlay public static final Object other = new Object();
              @JsOverlay public Object another = new Object();
              @JsOverlay public final Object yetAnother = new Object();
            }
            @JsType(isNative=true) public class Buggy {
              @JsOverlay public static Object object = new Object();
              @JsOverlay public static final Object other = new Object();
              @JsOverlay public static void m() { }
              @JsOverlay public static void m(int x) { }
              @JsOverlay private static void m(boolean x) { }
              @JsOverlay private void m(String x) { }
              @JsOverlay public final void n() { }
              @JsOverlay public final void n(int x) { }
              @JsOverlay private final void n(boolean x) { }
              @JsOverlay final void o() { }
              @JsOverlay protected final void p() { }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsOverlayWithSuperFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative=true) class SuperBuggy {
          public native void m();
        }
        @JsType(isNative=true) public class Buggy extends SuperBuggy {
          @JsOverlay public final void n() { super.m(); }
        > Error: Cannot use 'super' in JsOverlay method 'void Buggy.n()'.
        }
        """);
  }

  public void testJsOverlayImplementingInterfaceMethodFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative=true) interface IBuggy {
          void m();
          Object n();
        }
        @JsType(isNative=true) public class Buggy implements IBuggy {
          @JsOverlay public final void m() { }
        > Error: JsOverlay method 'void Buggy.m()' cannot override a supertype method.
          @JsOverlay public final String n() { return null; }
        > Error: JsOverlay method 'String Buggy.n()' cannot override a supertype method.
        }
        """);
  }

  public void testJsOverlayOverridingSuperclassMethodFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative=true) class Super {
          public native void m();
          public native Object n();
        }
        @JsType(isNative=true) public class Buggy extends Super {
          @JsOverlay public final void m() { }
        > Error: JsOverlay method 'void Buggy.m()' cannot override a supertype method.
          @JsOverlay public final String n() { return null; }
        > Error: JsOverlay method 'String Buggy.n()' cannot override a supertype method.
        }
        """);
  }

  public void testJsOverlayOnNonFinalMethodAndInstanceFieldFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        public class Buggy {
          @JsOverlay public final int f2 = 2;
        > Error: JsOverlay field 'Buggy.f2' can only be static.
          @JsOverlay
          public void m() {}
        > Error: JsOverlay method 'void Buggy.m()' has to be final.
        }
        """);
  }

  public void testJsOverlayWithStaticInitializerSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Buggy {
              @JsOverlay public static final Object f1 = new Object();
              @JsOverlay public static int f2 = 2;
              static { f2 = 3; }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsOverlayOnJsMemberFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative=true) public class Buggy {
          @JsOverlay public Buggy() { }
        > Error: annotation interface not applicable to this kind of declaration
        }
        """);

    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative=true) public class Buggy {
          @JsProperty @JsOverlay public int a;
          > Error: JsOverlay 'Buggy.a' cannot be nor override a JsProperty or a JsMethod.
          @JsProperty @JsOverlay public static int b;
          > Error: JsOverlay 'Buggy.b' cannot be nor override a JsProperty or a JsMethod.
          @JsMethod @JsOverlay public final void m() { }
          > Error: JsOverlay 'void Buggy.m()' cannot be nor override a JsProperty or a JsMethod.
          @JsMethod @JsOverlay public static void n() { }
          > Error: JsOverlay 'void Buggy.n()' cannot be nor override a JsProperty or a JsMethod.
          @JsProperty @JsOverlay public static void setA(String value) { }
          > Error: JsOverlay 'void Buggy.setA(String)' cannot be nor override a JsProperty or a JsMethod.
        }
        """);
  }

  public void testJsOverlayOnNonNativeJsTypeFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType
        public class Buggy {
          @JsOverlay public static final int F = 2;
        > Error: JsOverlay 'Buggy.F' can only be declared in a native type or @JsFunction interface.
          @JsOverlay
          public final void m() {};
        > Error: JsOverlay 'void Buggy.m()' can only be declared in a native type or @JsFunction interface.
        }
        """);
  }

  public void testJsTypeExtendsNativeJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) class Super {
            }
            @JsType public class Buggy extends Super {
            }
            """)
        .assertNoWarnings();
  }

  public void testJsTypeExtendsNonJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            class Super {
            }
            @JsType public class Buggy extends Super {
            }
            """)
        .assertNoWarnings();
  }

  public void testJsTypeImplementsNativeJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) interface Interface {
            }
            @JsType public class Buggy implements Interface {
            }
            """)
        .assertNoWarnings();
  }

  public void testJsTypeImplementsNonJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            interface Interface {
            }
            @JsType public class Buggy implements Interface {
            }
            """)
        .assertNoWarnings();
  }

  public void testJsTypeInterfaceExtendsNativeJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) interface Interface {
            }
            @JsType public interface Buggy extends Interface {
            }
            """)
        .assertNoWarnings();
  }

  public void testJsTypeInterfaceExtendsNonJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            interface Interface {
            }
            @JsType public interface Buggy extends Interface {
            }
            """)
        .assertNoWarnings();
  }

  public void testNativeJsTypeExtendsNativeJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) class Super {
              public native int hashCode();
            }
            @JsType(isNative=true) interface HasHashCode {
              int hashCode();
            }
            @JsType(isNative=true) public class Buggy extends Super {
              public native String toString();
              public native boolean equals(Object obj);
            }
            @JsType(isNative=true) class OtherBuggy implements HasHashCode {
              public native String toString(int i);
              public native boolean equals(Object obj);
              public native int hashCode();
            }
            @JsType(isNative=true) class NativeType {}
            interface A { int hashCode(); }
            class SomeClass extends NativeType implements A {
              @JsConstructor public SomeClass () {}
              public int hashCode() { return 0; }
            }
            @JsType(isNative=true) interface NativeInterface {}
            class SomeClass3 implements NativeInterface {}
            """)
        .assertNoWarnings();
  }

  public void testNativeJsTypeBadMembersFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative=true) interface Interface {
          @JsIgnore public void n();
        > Error: Native JsType member 'void Interface.n()' cannot have @JsIgnore.
        > Error: [unusable-by-js] Native 'void Interface.n()' is exposed to JavaScript without @JsMethod.
        }
        @JsType(isNative=true) abstract class Buggy {
          public static final int s = 42;
        > Error: Native JsType field 'Buggy.s' cannot be final.
          public static int t = 42;
        > Error: Native JsType field 'Buggy.t' cannot have initializer.
          public final int f = 42;
        > Error: Native JsType field 'Buggy.f' cannot be final.
          public int g = 42;
        > Error: Native JsType field 'Buggy.g' cannot have initializer.
          @JsIgnore public Buggy() { }
        > Error: Native JsType member 'Buggy()' cannot have @JsIgnore.
          @JsIgnore public int x;
        > Error: Native JsType member 'Buggy.x' cannot have @JsIgnore.
          @JsIgnore public native void n();
        > Error: Native JsType member 'void Buggy.n()' cannot have @JsIgnore.
        > Error: [unusable-by-js] Native 'void Buggy.n()' is exposed to JavaScript without @JsMethod.
          public void o() {}
        > Error: Native JsType method 'void Buggy.o()' should be native, abstract or JsOverlay.
          public native void p() /*-{}-*/;
        }
        @JsType(isNative=true) class NativeType {}
        interface A { @JsMethod(name="something") int hashCode(); }
        > Error: 'int A.hashCode()' cannot be assigned JavaScript name 'something' that is different from the JavaScript name of a method it overrides ('int Object.hashCode()' with JavaScript name 'hashCode').
        class SomeClass extends NativeType implements A {
          @JsConstructor public SomeClass() {}
          public int hashCode() { return 0; }
        > Error: 'int SomeClass.hashCode()' cannot be assigned JavaScript name 'something' that is different from the JavaScript name of a method it overrides ('int Object.hashCode()' with JavaScript name 'hashCode').
        }
        @JsType(isNative=true) class NativeTypeWithHashCode {
          public native int hashCode();
        }
        class SomeClass3 extends NativeTypeWithHashCode implements A {
          @JsConstructor public SomeClass3() {}
        }
        @JsType(isNative=true) interface NativeInterface {
          public Object foo();
        }
        @JsType(isNative=true) class NativeTypeWithBridge implements NativeInterface {
          public String foo() { return null; }
        > Error: Native JsType method 'String NativeTypeWithBridge.foo()' should be native, abstract or JsOverlay.
        }
        """);
  }

  public void testNativeJsTypeImplementingJavaLangObjectMethodsSucceeds() {
    assertTranspileSucceeds(
            "test.NativeType",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) class NativeType {}
            interface B { int hashCode(); }
            @JsType(isNative=true) class NativeTypeWithHashCode {
              public native int hashCode();
            }
            """)
        .assertNoWarnings();
  }

  public void testSubclassOfNativeJsTypeBadMembersFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative=true) class NativeType {
          @JsMethod(name ="string")
          public native String toString();
          > Error: 'String NativeType.toString()' cannot be assigned JavaScript name 'string' that is different from the JavaScript name of a method it overrides ('String Object.toString()' with JavaScript name 'toString').
          @JsOverlay
          public final String callToString() { return super.toString(); }
          > Error: Cannot use 'super' in JsOverlay method 'String NativeType.callToString()'.
        }
        class Buggy extends NativeType {
          @JsConstructor Buggy() {}
          public String toString() { return null; }
          > Error: 'String Buggy.toString()' cannot be assigned JavaScript name 'string' that is different from the JavaScript name of a method it overrides ('String Object.toString()' with JavaScript name 'toString').
          @JsMethod(name = "blah")
          public int hashCode() { return super.hashCode(); }
          > Error: 'int Buggy.hashCode()' cannot be assigned JavaScript name 'blah' that is different from the JavaScript name of a method it overrides ('int Object.hashCode()' with JavaScript name 'hashCode').
          > Error: Cannot use 'super' to call 'int Object.hashCode()' from a subclass of a native class.
        }
        class SubBuggy extends Buggy {
          @JsConstructor SubBuggy() {}
          public boolean equals(Object obj) { return super.equals(obj); }
          > Error: Cannot use 'super' to call 'boolean Object.equals(Object)' from a subclass of a native class.
          public Object foo(Object obj) { return null; }
        }
        class SubBuggy2 extends SubBuggy {
          @JsConstructor SubBuggy2() {}
          public String foo(Object obj) { return super.toString(); }
          > Error: Cannot use 'super' to call 'String Buggy.toString()' from a subclass of a native class.
        }
        """);
  }

  public void testNativeMethodOnJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            public class Buggy {
              @JsMethod
              public native void m();
              @JsProperty
              public native int getM();
            }
            """)
        .assertNoWarnings();
  }

  public void testNativeMethodNotJsMethodFails() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            public class Buggy {
              public native void m();
            }
            """)
        .addNativeJsForCompilationUnit("test.Buggy", "// empty")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "[unusable-by-js] Native 'void Buggy.m()' is exposed to JavaScript without @JsMethod.");
  }

  public void testNativeJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) abstract class Buggy {
              public static native void m();
              protected static native void m(Object o);
              private static native void m(String o);
              public Buggy() { }
              protected Buggy(Object o) { }
              private Buggy(String o) { }
              public native void n();
              protected native void n(Object o);
              private native void n(String o);
              public abstract void o();
              protected abstract void o(Object o);
              abstract void o(String o);
            }
            @JsType(isNative=true) interface NativeInterface {
              void m();
              void m(Object o);
              void m(String o);
            }
            @JsType(isNative=true) abstract class NativeClass {
              public native String toString();
              public abstract int hashCode();
            }
            class NativeSubclass extends NativeClass {
              @JsConstructor public NativeSubclass() {}
              public String toString() { return null; }
              @JsMethod
              public boolean equals(Object obj) { return false; }
              public int hashCode() { return 0; }
            }
            class SubNativeSubclass extends NativeSubclass {
              @JsConstructor public SubNativeSubclass() {}
              public boolean equals(Object obj) { return false; }
            }
            """)
        .assertNoWarnings();
  }

  public void testNativeJsTypeFieldsSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Buggy {
              public static int f1;
              protected static int f2;
              private static int f3;
              public int f4;
              protected int f5;
              private int f6;
            }
            """)
        .assertNoWarnings();
  }

  public void testNativeJsTypeDefaultConstructorSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public class Buggy {}
            """)
        .assertNoWarnings();
  }

  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) class Super {
              public native void m(Object o);
              public native void m(Object[] o);
            }
            @JsType public class Buggy extends Super {
              public void n(Object o) { }
            }
            """)
        .assertNoWarnings();
  }

  public void testClassesExtendingNativeJsTypeInterfaceWithOverlaySucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) interface Super {
              @JsOverlay default void fun() {}
            }
            @JsType(isNative=true) abstract class Buggy implements Super {
            }
            class JavaSubclass implements Super {
            }
            """)
        .assertNoWarnings();
  }

  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodOverloadsSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) class Super {
              public native void m(Object o);
              public native void m(int o);
            }
            public class Buggy extends Super {
              @JsConstructor public Buggy() {}
              public void m(Object o) { }
            }
            """)
        .assertNoWarnings();
  }

  public void testNonJsTypeWithNativeStaticMethodOverloadsSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            public class Buggy {
              @JsMethod public static native void m(Object o);
              @JsMethod public static native void m(int o);
            }
            """)
        .assertNoWarnings();
  }

  public void testNonJsTypeWithNativeInstanceMethodOverloadsSucceeds() {
    assertTranspileSucceeds(
            "Leaf",
            """
            import jsinterop.annotations.*;
            class Top {
              @JsMethod public void m(int o) {}
            }
            class SubTop extends Top {
              // Redefines m to be a setter
              @JsMethod public native void m(int o);
              @JsProperty public void setM(int m) { }
            }
            class SubSubTop extends SubTop {
              //  Adds a getter
              @JsProperty public int getM() { return 0; }
            }
            public class Leaf extends SubSubTop {
              // makes setter/getter pair native to define a different overload for the
              // JavaScript name
              @JsProperty public native void setM(int m);
              @JsProperty public native int getM();
              @JsMethod public void m(int o, Object opt_o) { }
            }
            """)
        .assertNoWarnings();
  }

  public void testOneLiveImplementationRuleViolationFails() {
    assertWithInlineMessages(
        "test.A",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        interface I {
          void m();
        }
        @JsType
        interface J extends I {
          default void m() {}
        }
        abstract class A implements I, J {
          @JsMethod(name = "m") public void x() {}
        > Error: 'void A.x()' and 'void J.m()' cannot both use the same JavaScript name 'm'.
        }
        abstract class B implements J, I {
          @JsMethod(name = "m") public void y() {}
        > Error: 'void B.y()' and 'void J.m()' cannot both use the same JavaScript name 'm'.
        }
        """);
  }

  public void testOneLiveImplementationRuleComplianceSucceeds() {
    assertTranspileSucceeds(
            "test.A",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            interface I {
              void m();
            }
            @JsType
            interface J extends I {
              default void m() {}
            }
            class A implements I, J {
              //  Redirects I.m and J.m to A.x
              @JsMethod public native void m();
              @JsMethod(name = "m") public void x() {}
            }
            class B implements J, I {
              //  Redirects I.m and J.m to B.y
              @JsMethod public native void m();
              @JsMethod(name = "m") public void y() {}
            }
            """)
        .assertNoWarnings();
  }

  public void testNonSingleOverloadImplementationFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        class Super {
          @JsMethod public void m(int o) { }
        }
        public class Buggy extends Super {
          @JsMethod public native void m(Object o);
          @JsMethod public void m(int o, Object opt_o) { }
        > Error: 'void Buggy.m(int, Object)' and 'void Super.m(int)' cannot both use the same JavaScript name 'm'.
        }
        """);
  }

  public void testNonJsTypeExtendsJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType class Super {
              Super() {}
            }
            public class Buggy extends Super {
            }
            """)
        .assertNoWarnings();
  }

  public void testNonJsTypeImplementsJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType interface Interface {
            }
            public class Buggy implements Interface {
            }
            """)
        .assertNoWarnings();
  }

  public void testNonJsTypeInterfaceExtendsJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType interface Interface {
            }
            public interface Buggy extends Interface {
            }
            """)
        .assertNoWarnings();
  }

  public void testNonJsTypeExtendsNativeJsTypeSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) class Super {
              public native void m();
            }
            public class Buggy extends Super {
              @JsConstructor Buggy() { }
              public void m() { }
            }
            """)
        .assertNoWarnings();
  }

  public void testNonJsTypeImplementsNativeJsTypeInterfaceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(isNative=true) interface Interface {
            }
            public class Buggy implements Interface {
            }
            """)
        .assertNoWarnings();
  }

  public void testNonJsTypeInterfaceExtendsNativeJsTypeInterfaceSucceeds() {
    newTesterWithDefaults()
        .addCompilationUnit("test.Buggy", "public interface Buggy extends Interface {}")
        .addCompilationUnit(
            "test.Interface",
            """
            import jsinterop.annotations.*;
            @JsType(isNative = true)
            public interface Interface {}
            """)
        .assertTranspileSucceeds()
        .assertNoWarnings();
  }

  public void testJsAsyncSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType(namespace = JsPackage.GLOBAL)
            class Promise {
            }
            @JsType(namespace = JsPackage.GLOBAL)
            interface IThenable {
            }
            class Buggy {
              @JsAsync
              public Promise a() { return null; }
              @JsAsync
              public IThenable b() { return null; }
            }
            """)
        .assertNoWarnings();
  }

  public void testJsAsyncFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        import java.util.List;
        @JsType(namespace = JsPackage.GLOBAL)
        class Promise {
        }
        class A {
          @JsAsync
          public <T> List<T> a() { return null; }
        > Error: JsAsync method 'List<T> A.a()' should return either 'IThenable' or 'Promise' but returns 'List<T>'.
          @JsAsync
          public <P extends Promise> P b() { return null; }
        > Error: JsAsync method 'P A.b()' should return either 'IThenable' or 'Promise' but returns 'P'.
          @JsAsync
          public String c() { return null; }
        > Error: JsAsync method 'String A.c()' should return either 'IThenable' or 'Promise' but returns 'String'.
          @JsAsync
          public Promise[] d() { return null; }
        > Error: JsAsync method 'Promise[] A.d()' should return either 'IThenable' or 'Promise' but returns 'Promise[]'.
          @JsAsync
          public double e() { return 0; }
        > Error: JsAsync method 'double A.e()' should return either 'IThenable' or 'Promise' but returns 'double'.
        }
        """);
  }

  public void testCustomIsInstanceSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            interface InterfaceWithCustomIsInstance {
              public static boolean $isInstance(Object o) { return true; }
            }
            class ClassWithCustomIsInstance {
              static boolean $isInstance(Object o) { return true; }
            }
            @JsType(isNative = true)
            interface NativeInterfaceWithCustomIsInstance {
              @JsOverlay
              static boolean $isInstance(Object o) { return true; }
            }
            @JsType(isNative = true)
            class NativeClassWithCustomIsInstance {
              @JsOverlay
              protected static boolean $isInstance(Object o) { return true; }
            }
            @JsType(isNative = true)
            class ClassWithNativeIsInstance {
              static native boolean $isInstance(Object o);
            }
            class Buggy {
              static void main() {
                Object o = null;
                boolean b = o instanceof InterfaceWithCustomIsInstance;
                b = o instanceof ClassWithCustomIsInstance;
                b = o instanceof NativeInterfaceWithCustomIsInstance;
                b = o instanceof NativeClassWithCustomIsInstance;
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testCustomIsInstanceFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        interface BadIsInstanceVisibility {
          private static boolean $isInstance(Object o) { return true; }
          > Error: Custom '$isInstance' method 'boolean BadIsInstanceVisibility.$isInstance(Object o)' has to be non private.
        }
        class BadIsInstanceReturnType {
          static void $isInstance(Object o) { }
          > Error: Custom '$isInstance' method 'void BadIsInstanceReturnType.$isInstance(Object o)' has to return 'boolean'.
        }
        class BadIsInstanceMembership {
          boolean $isInstance(Object o) { return true; }
          > Error: Custom '$isInstance' method 'boolean BadIsInstanceMembership.$isInstance(Object o)' has to be static.
        }
        @JsType(isNative = true)
        class BadIsInstanceOnNativeType {
          static boolean $isInstance(Object o) { return true; }
          > Error: Native JsType method 'boolean BadIsInstanceOnNativeType.$isInstance(Object o)' should be native, abstract or JsOverlay.
        }
        """);
  }

  public void testUnusableByJsSuppressionSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            class A {}
            @JsType @SuppressWarnings("unusable-by-js") // SuppressWarnings on the class.
            class B {
              public A field;
              public A t0(A a, A b) { return null; }
            }
            @JsType
            public class Buggy {
              @SuppressWarnings("unusable-by-js") // add SuppressWarnings to field.
              public A field;
              @SuppressWarnings({"unusable-by-js", "unused"}) // test multiple warnings.
              public A t0(A a, A b) { return null; } // add SuppressWarnings to the method.
              public void t1(
                @SuppressWarnings("unusable-by-js")A a,
                @SuppressWarnings("unusable-by-js")A b
              ) {} // add SuppressWarnings to parameters.
            }
            """)
        .assertNoWarnings();
  }

  public void testUsableByJsTypesSucceeds() {
    assertTranspileSucceeds(
            "test.A",
            """
            import jsinterop.annotations.*;
            @JsType public class A {}
            @JsType interface I<T> {
              void trigger(T t);
            }
            @JsFunction interface FI {void foo();}
            class List<T> {
              @JsMethod
              public T get() { return null; }
            }
            @JsEnum(isNative = true) enum NativeEnum { A; }
            @JsType class MyJsType implements I<Void> {
              public void f1(boolean a, int b, double c) {} // primitive types work fine.
              public void f2(Boolean a, Double b, String c, Long l) {} // unboxed types work fine.
              public void f3(A a) {} // JsType works fine.
              public void f4(I a) {} // JsType interface works fine.
              public void f5(FI a) {} // JsFunction works fine.
              public void f7(Object a) {} // Java Object works fine.
              public void f8(boolean[] a) {} // array of primitive types work fine.
              public void f10(A[] a) {} // array of JsType works fine.
              public void f11(FI[] a) {} // array of JsFunction works fine.
              public void f12(Boolean[] a, Double[] b, String[] c) {} // array of unboxed types.
              public void f13(Object[] a) {} // Object[] works fine.
              public void f14(Object[][] a) {} // Object[][] works fine.
              public long f15(long a) { return 1l; } // long works fine.
              public long f16(long... a) { return 1l; } // varargs of allowable types works fine.
              // anonymous @JsConstructor class with unusable-by-js captures.
              private void f17(Integer a) { new A() { { f7(a); } }; }
              public void f18(List<NativeEnum> l) {} // Type parameterized by native JsEnum
              // succeeds
              public void trigger(Void v) {} // Void succeeds.
            }
            class Outer {
              {
                Integer i = 1;
                class A {
                   Integer f = i;
                }
              }
            }
            """)
        .assertNoWarnings();
  }

  public void testUnusableByNonJsMembersSucceeds() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            class A {}
            @JsType public class Buggy {
              private A field;
              private A f1(A a) { return null; }
            }
            """)
        .assertNoWarnings();
  }

  public void testUnusableByJsWarns() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            class A {}
            @JsType interface I {}
            class B implements I {}
            class C { // non-jstype class
              @JsMethod
              public static void fc1(A a) {} // JsMethod
              @JsMethod
              public native void fc2(A a); // native method
            }
            class D { // non-jstype class with JsProperty
              @JsProperty
              public static A a; // JsProperty
            }
            @JsFunction interface FI  { void f(A a); } // JsFunction method is checked.
            class List<T> {
              @JsMethod
              public T get() { return null; }
            }
            @JsEnum enum MyJsEnum { A; }
            @JsType public class Buggy {
              public A f; // exported field
              public A f1(A a) { return null; } // regular class fails.
              public A[] f2(A[] a) { return null; } // array of regular class fails.
              public Short f3(Short a) { return (short) 1; } // Short fails.
              // non-JsType class that implements a JsType interface fails.
              public B f4(B a) { return null; }
              public void f5(List<MyJsEnum> l) { } // parameterized by JsEnum fails
              public void f6(List<List<MyJsEnum>> l) {} // parameterized by List<JsEnum> fails
              public void f7() { new Object() { @JsMethod void b(A a){} }; }
            }
            """)
        .addFileToZipFile("native.zip", "test/C.native.js", "// empty")
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
            "[unusable-by-js] Type of parameter 'l' in 'void Buggy.f5(List<MyJsEnum> l)' is not "
                + "usable by but exposed to JavaScript.",
            "[unusable-by-js] Type of parameter 'l' in 'void Buggy.f6(List<List<MyJsEnum>> l)' is "
                + "not usable by but exposed to JavaScript.",
            "[unusable-by-js] Type of parameter 'a' in 'void <anonymous> extends Object.b(A a)' is"
                + " not usable by but exposed to JavaScript.")
        .assertLastMessage(
            "Suppress \"[unusable-by-js]\" warnings by adding a "
                + "`@SuppressWarnings(\"unusable-by-js\")` annotation to the "
                + "corresponding member.");
  }

  public void testUnusableByJsAccidentalOverrideSuppressionWarns() {
    assertTranspileSucceeds(
            "test.Buggy",
            """
            import jsinterop.annotations.*;
            @JsType
            interface Foo {
              @SuppressWarnings("unusable-by-js")
              void doIt(Class foo);
            }
            class Parent {
              public void doIt(Class x) {}
            }
            public class Buggy extends Parent implements Foo {}
            """)
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
            "test.Buggy",
            """
            @org.jspecify.annotations.NullMarked
            class NullMarkedType {
            }
            """)
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
            """
            public @interface AutoValue {
            public @interface Builder{}
            }
            """)
        .addCompilationUnit(
            "test.Buggy",
            """
            @com.google.auto.value.AutoValue
            class Foo {
              @com.google.auto.value.AutoValue.Builder
              static class Builder {}
            }
            class CustomFoo extends Foo {
              static class CustomFooBuilder extends Foo.Builder {}
            }
            class CustomFooBuilder extends Foo.Builder {}
            // Following are allowed since that's what AutoValue generates.
            class AutoValue_Foo extends Foo {
              static class Builder extends Foo.Builder {}
            }
            class $$AutoValue_Foo extends Foo {
              static class Builder extends Foo.Builder {}
            }
            """)
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Extending @AutoValue with CustomFoo is not supported when AutoValue optimization is"
                + " enabled. (Also see https://errorprone.info/bugpattern/ExtendsAutoValue)",
            "Extending @AutoValue with CustomFooBuilder is not supported when AutoValue"
                + " optimization is enabled. (Also see"
                + " https://errorprone.info/bugpattern/ExtendsAutoValue)");
  }

  private void assertWithInlineMessages(String... compilationUnitsAndSources) {
    List<String> expectedErrors = new ArrayList<>();
    List<String> expectedWarnings = new ArrayList<>();
    var tester = newTesterWithDefaults();

    for (int i = 0; i < compilationUnitsAndSources.length; i += 2) {
      String compilationUnit = compilationUnitsAndSources[i];
      String source = compilationUnitsAndSources[i + 1];
      String code = parseCompilationUnit(compilationUnit, source, expectedErrors, expectedWarnings);
      tester = tester.addCompilationUnit(compilationUnit, code);
    }

    var result =
        expectedErrors.isEmpty() ? tester.assertTranspileSucceeds() : tester.assertTranspileFails();
    result
        .assertErrorsWithSourcePosition(expectedErrors.toArray(new String[0]))
        .assertWarningsWithSourcePosition(expectedWarnings.toArray(new String[0]));
  }

  private String parseCompilationUnit(
      String compilationUnit,
      String source,
      List<String> expectedErrors,
      List<String> expectedWarnings) {
    String fileName = compilationUnit.substring(compilationUnit.lastIndexOf(".") + 1) + ".java";

    int currentLine = 1;
    if (compilationUnit.contains(".")) {
      currentLine++; // Has package declaration.
    }

    StringBuilder codeBuilder = new StringBuilder();
    List<String> lastMessageList = null;
    for (String line : source.split("\n", -1)) {
      String trimmedLine = line.trim();
      if (trimmedLine.startsWith("> Error:")) {
        expectedErrors.add(formatMessage("Error", fileName, currentLine - 1, trimmedLine));
        lastMessageList = expectedErrors;
      } else if (trimmedLine.startsWith("> Warning:")) {
        expectedWarnings.add(formatMessage("Warning", fileName, currentLine - 1, trimmedLine));
        lastMessageList = expectedWarnings;
      } else if (trimmedLine.startsWith("> ")) {
        if (lastMessageList == null || lastMessageList.isEmpty()) {
          throw new IllegalArgumentException("Unexpected continuation line: " + trimmedLine);
        }
        int lastIndex = lastMessageList.size() - 1;
        String lastMessage = lastMessageList.get(lastIndex);
        lastMessageList.set(lastIndex, lastMessage + "\n" + trimmedLine.substring("> ".length()));
      } else {
        codeBuilder.append(line).append("\n");
        currentLine++;
        lastMessageList = null;
      }
    }
    return codeBuilder.toString();
  }

  private static String formatMessage(String type, String fileName, int lineNumber, String line) {
    String trimmedLine = line.substring(("> " + type + ": ").length());
    return type + ":" + fileName + ":" + lineNumber + ": " + trimmedLine;
  }

  private TranspileResult assertTranspileSucceeds(String compilationUnitName, String code) {
    return newTesterWithDefaults()
        .addCompilationUnit(compilationUnitName, code)
        .assertTranspileSucceeds();
  }
}
