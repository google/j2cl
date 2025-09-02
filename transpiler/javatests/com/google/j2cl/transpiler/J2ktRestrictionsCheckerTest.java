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
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit("test.Empty", "class Empty {}")
        .assertTranspileSucceeds();
  }

  public void testGenericConstructorFails() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Main",
            """
            class Main {
              <T> Main(T t) {}
            }
            """)
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:3: Constructor 'Main(T t)' cannot declare type variables.");
  }

  public void testMemberVisibilityWarnings() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Public",
            """
            class Pkg {}
            public class Public {
              public void pkgParam(Pkg pkg) {}
              public Pkg pkgReturnType() { return new Pkg(); }
              public Pkg pkgField;
              static class InnerPkg {
                public InnerPkg() {}
                public Pkg pkgReturnType() { return new Pkg(); }
                public Pkg pkgField;
              }
            }
            """)
        .assertTranspileSucceeds()
        .assertWarningsWithSourcePosition(
            "Warning:Public.java:4: Member 'void Public.pkgParam(Pkg pkg)' (public) should not have"
                + " wider visibility than 'Pkg' (default).",
            "Warning:Public.java:5: Member 'Pkg Public.pkgReturnType()' (public) should not have"
                + " wider visibility than 'Pkg' (default).",
            "Warning:Public.java:6: Member 'Public.pkgField' (public) should not have wider"
                + " visibility than 'Pkg' (default).");
  }

  public void testClassVisibilityWarnings() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Main",
            """
            class Pkg {}
            public class Main extends Pkg {}
            """)
        .assertTranspileSucceeds()
        .assertWarningsWithSourcePosition(
            "Warning:Main.java:3: Type 'Main' (public) should not have wider visibility than its"
                + " super type 'Pkg' (default).");
  }

  public void testInterfaceVisibilityWarnings() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Main",
            """
            interface Pkg {}
            public interface Main extends Pkg {}
            """)
        .assertTranspileSucceeds()
        .assertWarningsWithSourcePosition(
            "Warning:Main.java:3: Type 'Main' (public) should not have wider visibility than its"
                + " super type 'Pkg' (default).");
  }

  public void testNonNullMarkedErrors() {
    newTranspilerTester()
        .addCompilationUnit("test.Main", "class Main { void m() {} }")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:2: Type 'test.Main' must be directly or indirectly @NullMarked.");

    newTranspilerTester()
        .addNullMarkPackageInfo("foo")
        .addCompilationUnit("foo.A", "class A { void m() {} }")
        .addCompilationUnit("bar.B", "class B { void m() {} }")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:B.java:2: Type 'bar.B' must be directly or indirectly @NullMarked.");

    // Enums are tolerated as not being NullMarked.
    newTranspilerTester().addCompilationUnit("foo.A", "enum A {}").assertTranspileSucceeds();

    // Annotations are tolerated as not being NullMarked.
    newTranspilerTester().addCompilationUnit("foo.A", "@interface A {}").assertTranspileSucceeds();

    // Empty classes are tolerated as not being NullMarked.
    newTranspilerTester().addCompilationUnit("foo.A", "class A {}").assertTranspileSucceeds();

    // But the empty class cannot have a supertype.
    newTranspilerTester()
        .addCompilationUnit("foo.A", "class A {}")
        .addCompilationUnit("foo.B", "class B extends A {}")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:B.java:2: Type 'foo.B' must be directly or indirectly @NullMarked.");

    newTranspilerTester()
        .addCompilationUnit(
            "test.Main",
            """
            class Outer {
              @org.jspecify.annotations.NullMarked
              class Inner { void m() {} }
              @org.jspecify.annotations.NullMarked
              static class StaticInner { void m() {} }
            }
            """)
        .addCompilationUnit(
            "org.jspecify.annotations.NullMarked", "public @interface NullMarked {}")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:2: Type 'test.Outer' must be directly or indirectly @NullMarked.");
  }

  public void testKtPropertyNonEmptyParametersFails() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Main",
            """
            abstract class Main {
              @javaemul.internal.annotations.KtProperty
              abstract int method(int foo);
            }
            """)
        .addCompilationUnit(
            "javaemul.internal.annotations.KtProperty", "public @interface KtProperty {}")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:4: Method 'int Main.method(int foo)' can not be '@KtProperty', as it"
                + " has non-empty parameters.");
  }

  public void testKtPropertyVoidReturnTypeFails() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Main",
            """
            abstract class Main {
              @javaemul.internal.annotations.KtProperty
              abstract void method();
            }
            """)
        .addCompilationUnit(
            "javaemul.internal.annotations.KtProperty", "public @interface KtProperty {}")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:4: Method 'void Main.method()' can not be '@KtProperty', as it has"
                + " void return type.");
  }

  public void testKtPropertyConstructorFails() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Main",
            """
            class Main {
              @javaemul.internal.annotations.KtProperty
              Main() {}
            }
            """)
        .addCompilationUnit(
            "javaemul.internal.annotations.KtProperty", "public @interface KtProperty {}")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:4: Constructor 'Main()' can not be '@KtProperty'.");
  }

  public void testSynchronizedMethodInUnsupportedTypeFails() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Child",
            """
            class Parent {}
            class Child extends Parent {
              synchronized void method() {}
            }
            """)
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Child.java:3: Type 'Child' does not support synchronized methods as it does not "
                + "extend 'J2ktMonitor' or is not a direct subclass of 'Object'.");
  }

  public void testUnsupportedSynchronizedStatementFails() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Main",
            """
            class Main {
              void method() {
                synchronized (this) {}
              }
            }
            """)
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:4: Synchronized statement is valid only on instances of 'Class' or"
                + " 'J2ktMonitor'.");
  }

  public void testUnsupportedObjectiveCNameAnnotation() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Main",
            """
            interface Main {
              @com.google.j2objc.annotations.ObjectiveCName("submit:")
              void submit(String s);
            }
            """)
        .addCompilationUnit(
            "com.google.j2objc.annotations.ObjectiveCName",
            """
            public @interface ObjectiveCName {
              String value();
            }
            """)
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:4: Method 'void Main.submit(String s)' is annotated with"
                + " '@ObjectiveCName(\"submit:\")' which can not be translated to Kotlin. The first"
                + " component of Objective C selector must contains at least one uppercase"
                + " character, so it can be split in two parts and translated into two '@ObjCName'"
                + " annotations, on function and its first parameter. Consider renaming to"
                + " '@ObjectiveCName(\"submitWith:\")' or removing the annotation. Reference bug:"
                + " https://youtrack.jetbrains.com/issue/KT-80557");
  }

  public void testExplicitQualifierInAnonymousNewInstanceFails() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Outer",
            """
            class Outer {
              class Inner {
                void test(Outer outer) {
                  outer.new Inner() {};
                }
              }
            }
            """)
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Outer.java:5: Explicit qualifier in constructor call is not supported.");
  }

  public void testExplicitQualifierInSuperCallFails() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Outer",
            """
            class Outer {
              class Inner {}
            }
            class InnerSubclass extends Outer.Inner {
              InnerSubclass(Outer outer) {
                outer.super();
              }
            }
            """)
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Outer.java:7: Explicit qualifier in constructor call is not supported.");
  }

  public void testFieldShadowingFails() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
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
            """)
        .addCompilationUnit(
            "test.Child",
            """
            public class Child extends Parent {
              public int publicShadowedByPublic;
              public int publicShadowedByPrivateShadowedByPublic;
              protected int protectedShadowedByProtected;
              public int protectedShadowedByPublic;
              int packagePrivateShadowedByPackagePrivate;
              public int packagePrivateShadowedByPublic;
            }
            """)
        .addCompilationUnit(
            "test.GrandChild",
            """
            public class GrandChild extends Child {
              public int publicShadowedByPrivateShadowedByPublic;
            }
            """)
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Child.java:3: Field 'Child.publicShadowedByPublic' cannot shadow a super type"
                + " field.",
            "Error:Child.java:4: Field 'Child.publicShadowedByPrivateShadowedByPublic' cannot"
                + " shadow a super type field.",
            "Error:Child.java:5: Field 'Child.protectedShadowedByProtected' cannot shadow a super"
                + " type field.",
            "Error:Child.java:6: Field 'Child.protectedShadowedByPublic' cannot shadow a super type"
                + " field.",
            "Error:Child.java:7: Field 'Child.packagePrivateShadowedByPackagePrivate' cannot shadow"
                + " a super type field.",
            "Error:Child.java:8: Field 'Child.packagePrivateShadowedByPublic' cannot shadow a super"
                + " type field.",
            "Error:GrandChild.java:3: Field 'GrandChild.publicShadowedByPrivateShadowedByPublic'"
                + " cannot shadow a super type field.");
  }

  public void testFieldShadowingSucceeds() {
    newTranspilerTester()
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "test.Parent",
            """
            public class Parent {
              private int privateField;
              int packagePrivateInDifferentPackage;
              public static int staticField;
            }
            """)
        .addCompilationUnit(
            "test.Child",
            """
            public interface Child extends Parent {
              int interfaceField = 1;
            }
            """)
        .addCompilationUnit(
            "test.Child",
            """
            public class Child extends Parent {
              public int privateField;
            }
            """)
        .addNullMarkPackageInfo("other")
        .addCompilationUnit(
            "other.Child",
            """
            public class Child extends test.Parent {
              int packagePrivateInOtherPackage;
              public static int staticField;
            }
            """)
        .addCompilationUnit(
            "test.ParentInterface",
            """
            public interface ParentInterface {
              int interfaceField = 0;
            }
            """)
        .addCompilationUnit(
            "test.ChildInterface",
            """
            public interface ChildInterface extends ParentInterface {
              int interfaceField = 1;
            }
            """)
        .assertTranspileSucceeds();
  }

  private TranspilerTester newTranspilerTester() {
    return TranspilerTester.newTesterWithJ2ktDefaults();
  }
}
