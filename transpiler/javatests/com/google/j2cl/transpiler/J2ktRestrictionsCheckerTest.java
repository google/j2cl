/*
 * Copyright 2022 Google Inc.
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

import junit.framework.TestCase;

/** Tests for J2ktRestrictionsChecker. */
@SuppressWarnings("CheckReturnValue")
public class J2ktRestrictionsCheckerTest extends TestCase {
  public void testEmptyClass() {
    assertWithInlineMessages(
        "test.Empty",
        """
        class Empty {}
        """);
  }

  public void testGenericConstructorFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        class Main {
          <T> Main(T t) {}
        > Error: Constructor 'Main(T t)' cannot declare type variables.
        }
        """);
  }

  public void testMemberVisibilityWarnings() {
    assertWithInlineMessages(
        "test.Public",
        """
        class Pkg {}
        public class Public {
          public void pkgParam(Pkg pkg) {}
        > Warning: Member 'void Public.pkgParam(Pkg pkg)' (public) should not have wider visibility than 'Pkg' (default).
          public Pkg pkgReturnType() { return new Pkg(); }
        > Warning: Member 'Pkg Public.pkgReturnType()' (public) should not have wider visibility than 'Pkg' (default).
          public Pkg pkgField;
        > Warning: Member 'Public.pkgField' (public) should not have wider visibility than 'Pkg' (default).
          static class InnerPkg {
            public InnerPkg() {}
            public Pkg pkgReturnType() { return new Pkg(); }
            public Pkg pkgField;
          }
        }
        """);
  }

  public void testClassVisibilityWarnings() {
    assertWithInlineMessages(
        "test.Main",
        """
        class Pkg {}
        public class Main extends Pkg {}
        > Warning: Type 'Main' (public) should not have wider visibility than its super type 'Pkg' (default).
        """);
  }

  public void testAnonymousClassVisibilitySucceeds() {
    assertWithInlineMessages(
        "test.Main",
        """
        class Pkg {}
        public class Main {
          Pkg m() {
            return new Pkg() {};
          }
        }
        """);
  }

  public void testInterfaceVisibilityWarnings() {
    assertWithInlineMessages(
        "test.Main",
        """
        interface Pkg {}
        public interface Main extends Pkg {}
        > Warning: Type 'Main' (public) should not have wider visibility than its super type 'Pkg' (default).
        """);
  }

  public void testNonNullMarkedErrors() {
    newTranspilerTester()
        .assertWithInlineMessages(
            "test.Main",
            """
            class Main { void m() {} }
            > Error: Type 'test.Main' must be directly or indirectly @NullMarked.
            """);

    newTranspilerTester()
        .addNullMarkPackageInfo("foo")
        .assertWithInlineMessages(
            "foo.A",
            "class A { void m() {} }",
            "bar.B",
            """
            class B { void m() {} }
            > Error: Type 'bar.B' must be directly or indirectly @NullMarked.
            """);

    // Enums are tolerated as not being NullMarked.
    newTranspilerTester().assertWithInlineMessages("foo.A", "enum A {}");

    // Annotations are tolerated as not being NullMarked.
    newTranspilerTester().assertWithInlineMessages("foo.A", "@interface A {}");

    // Empty classes are tolerated as not being NullMarked.
    newTranspilerTester().assertWithInlineMessages("foo.A", "class A {}");

    // But the empty class cannot have a supertype.
    newTranspilerTester()
        .assertWithInlineMessages(
            "foo.A",
            "class A {}",
            "foo.B",
            """
            class B extends A {}
            > Error: Type 'foo.B' must be directly or indirectly @NullMarked.
            """);

    newTranspilerTester()
        .assertWithInlineMessages(
            "test.Main",
            """
            class Outer {
            > Error: Type 'test.Outer' must be directly or indirectly @NullMarked.
              @org.jspecify.annotations.NullMarked
              class Inner { void m() {} }
              @org.jspecify.annotations.NullMarked
              static class StaticInner { void m() {} }
            }
            """,
            "org.jspecify.annotations.NullMarked",
            "public @interface NullMarked {}");
  }

  public void testKtPropertyNonEmptyParametersFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        abstract class Main {
          @javaemul.internal.annotations.KtProperty
          abstract int method(int foo);
        > Error: Method 'int Main.method(int foo)' can not be '@KtProperty', as it has non-empty parameters.
        }
        """,
        "javaemul.internal.annotations.KtProperty",
        "public @interface KtProperty {}");
  }

  public void testKtPropertyVoidReturnTypeFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        abstract class Main {
          @javaemul.internal.annotations.KtProperty
          abstract void method();
        > Error: Method 'void Main.method()' can not be '@KtProperty', as it has void return type.
        }
        """,
        "javaemul.internal.annotations.KtProperty",
        "public @interface KtProperty {}");
  }

  public void testKtPropertyConstructorFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        class Main {
          @javaemul.internal.annotations.KtProperty
          Main() {}
        > Error: Constructor 'Main()' can not be '@KtProperty'.
        }
        """,
        "javaemul.internal.annotations.KtProperty",
        "public @interface KtProperty {}");
  }

  public void testSynchronizedMethodInUnsupportedTypeFails() {
    assertWithInlineMessages(
        "test.Child",
        """
        class Parent {}
        class Child extends Parent {
        > Error: Type 'Child' does not support synchronized methods as it does not extend 'J2ktMonitor' or is not a direct subclass of 'Object'.
          synchronized void method() {}
        }
        """);
  }

  public void testUnsupportedSynchronizedStatementFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        class Main {
          void method() {
            synchronized (this) {}
        > Error: Synchronized statement is valid only on instances of 'Class' or 'J2ktMonitor'.
          }
        }
        """);
  }

  public void testExplicitQualifierInAnonymousNewInstanceFails() {
    assertWithInlineMessages(
        "test.Outer",
        """
        class Outer {
          class Inner {
            void test(Outer outer) {
              outer.new Inner() {};
        > Error: Explicit qualifier in constructor call is not supported.
            }
          }
        }
        """);
  }

  public void testExplicitQualifierInSuperCallFails() {
    assertWithInlineMessages(
        "test.Outer",
        """
        class Outer {
          class Inner {}
        }
        class InnerSubclass extends Outer.Inner {
          InnerSubclass(Outer outer) {
            outer.super();
        > Error: Explicit qualifier in constructor call is not supported.
          }
        }
        """);
  }

  public void testFieldShadowingFails() {
    assertWithInlineMessages(
        "test.Parent",
        """
        public class Parent {
          public int publicShadowedByPublic;
          public int publicShadowedByPrivateShadowedByPublic;
          protected int protectedShadowedByProtected;
          protected int protectedShadowedByPublic;
          int packagePrivateShadowedByPackagePrivate;
          int packagePrivateShadowedByPublic;
        }
        """,
        "test.Child",
        """
        public class Child extends Parent {
          public int publicShadowedByPublic;
        > Error: Field 'Child.publicShadowedByPublic' cannot shadow a super type field.
          public int publicShadowedByPrivateShadowedByPublic;
        > Error: Field 'Child.publicShadowedByPrivateShadowedByPublic' cannot shadow a super type field.
          protected int protectedShadowedByProtected;
        > Error: Field 'Child.protectedShadowedByProtected' cannot shadow a super type field.
          public int protectedShadowedByPublic;
        > Error: Field 'Child.protectedShadowedByPublic' cannot shadow a super type field.
          int packagePrivateShadowedByPackagePrivate;
        > Error: Field 'Child.packagePrivateShadowedByPackagePrivate' cannot shadow a super type field.
          public int packagePrivateShadowedByPublic;
        > Error: Field 'Child.packagePrivateShadowedByPublic' cannot shadow a super type field.
        }
        """,
        "test.GrandChild",
        """
        public class GrandChild extends Child {
          public int publicShadowedByPrivateShadowedByPublic;
        > Error: Field 'GrandChild.publicShadowedByPrivateShadowedByPublic' cannot shadow a super type field.
        }
        """);
  }

  public void testFieldShadowingSucceeds() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addNullMarkPackageInfo("other")
        .assertWithInlineMessages(
            "test.Parent",
            """
            public class Parent {
              private int privateField;
              int packagePrivateInDifferentPackage;
              public static int staticField;
            }
            """,
            "test.Child",
            """
            public interface Child extends Parent {
              int interfaceField = 1;
            }
            """,
            "test.Child",
            """
            public class Child extends Parent {
              public int privateField;
            }
            """,
            "other.Child",
            """
            public class Child extends test.Parent {
              int packagePrivateInOtherPackage;
              public static int staticField;
            }
            """,
            "test.ParentInterface",
            """
            public interface ParentInterface {
              int interfaceField = 0;
            }
            """,
            "test.ChildInterface",
            """
            public interface ChildInterface extends ParentInterface {
              int interfaceField = 1;
            }
            """);
  }

  public void testJ2ObjCPropertyAndJsTypeFails() {
    assertWithInlineMessages(
        "com.google.j2objc.annotations.Property",
        """
        public @interface Property {}
        """,
        "jsinterop.annotations.JsType",
        """
        public @interface JsType {}
        """,
        "test.Main",
        """
        @jsinterop.annotations.JsType
        @com.google.j2objc.annotations.Property
        class Main {
          public int getFoo() {
        > Error: Method 'int Main.getFoo()' is marked @Property for J2ObjC but exposed to JS without a @JsProperty.
            return 1;
          };
        }
        """);
  }

  public void testJ2ObjCPropertyAndJsMethodFails() {
    assertWithInlineMessages(
        "com.google.j2objc.annotations.Property",
        """
        public @interface Property {}
        """,
        "jsinterop.annotations.JsMethod",
        """
        public @interface JsMethod {}
        """,
        "test.Main",
        """
        @com.google.j2objc.annotations.Property
        class Main {
          @jsinterop.annotations.JsMethod
          public int getFoo() {
        > Error: Method 'int Main.getFoo()' is marked @Property for J2ObjC but exposed to JS without a @JsProperty.
            return 1;
          };
        }
        """);
  }

  public void testJ2ObjCPropertyAndJsPropertySucceeds() {
    assertWithInlineMessages(
        "com.google.j2objc.annotations.Property",
        """
        public @interface Property {}
        """,
        "jsinterop.annotations.JsProperty",
        """
        public @interface JsProperty {}
        """,
        "test.Main",
        """
        @com.google.j2objc.annotations.Property
        class Main {
          @jsinterop.annotations.JsProperty
          public int getFoo() {
            return 1;
          };
        }
        """);
  }

  public void testJsTypeOnRecordFails() {
    assertWithInlineMessages(
        "jsinterop.annotations.JsType",
        """
        public @interface JsType {}
        """,
        "test.Main",
        """
        @jsinterop.annotations.JsType
        record TopLevel(int x) {}
        > Error: Record class 'TopLevel' cannot be a JsType. (b/470146353)
        """);

    assertWithInlineMessages(
        "jsinterop.annotations.JsType",
        """
        public @interface JsType {}
        """,
        "test.Main2",
        """
        class Outer {
          @jsinterop.annotations.JsType
          record Nested(int x) {}
        > Error: Record class 'Nested' cannot be a JsType. (b/470146353)
        }
        """);
  }

  private void assertWithInlineMessages(String... compilationUnitsAndSources) {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .assertWithInlineMessages(compilationUnitsAndSources);
  }

  private TranspilerTester newTranspilerTester() {
    return TranspilerTester.newTesterWithJ2ktDefaults();
  }
}
