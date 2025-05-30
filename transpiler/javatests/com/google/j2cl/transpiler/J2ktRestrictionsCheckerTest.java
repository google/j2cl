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
    newTranspilerTester("test.Empty", "class Empty {}")
        .addNullMarkPackageInfo("test")
        .assertTranspileSucceeds();
  }

  public void testGenericConstructorFails() {
    newTranspilerTester(
            "test.Main",
            """
            class Main {
              <T> Main(T t) {}
            }
            """)
        .addNullMarkPackageInfo("test")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:3: Constructor 'Main(T t)' cannot declare type variables.");
  }

  public void testMemberVisibilityWarnings() {
    newTranspilerTester(
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
        .addNullMarkPackageInfo("test")
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
    newTranspilerTester(
            "test.Main",
            """
            class Pkg {}
            public class Main extends Pkg {}
            """)
        .addNullMarkPackageInfo("test")
        .assertTranspileSucceeds()
        .assertWarningsWithSourcePosition(
            "Warning:Main.java:3: Type 'Main' (public) should not have wider visibility than its"
                + " super type 'Pkg' (default).");
  }

  public void testInterfaceVisibilityWarnings() {
    newTranspilerTester(
            "test.Main",
            """
            interface Pkg {}
            public interface Main extends Pkg {}
            """)
        .addNullMarkPackageInfo("test")
        .assertTranspileSucceeds()
        .assertWarningsWithSourcePosition(
            "Warning:Main.java:3: Type 'Main' (public) should not have wider visibility than its"
                + " super type 'Pkg' (default).");
  }

  public void testNonNullMarkedErrors() {
    newTranspilerTester("test.Main", "class Main { void m() {} }")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:2: Type 'test.Main' must be directly or indirectly @NullMarked.");

    newTranspilerTester("foo.A", "class A { void m() {} }")
        .addCompilationUnit("bar.B", "class B { void m() {} }")
        .addNullMarkPackageInfo("foo")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:B.java:2: Type 'bar.B' must be directly or indirectly @NullMarked.");

    // Enums are tolerated as not being NullMarked.
    newTranspilerTester("foo.A", "enum A {}").assertTranspileSucceeds();

    // Annotations are tolerated as not being NullMarked.
    newTranspilerTester("foo.A", "@interface A {}").assertTranspileSucceeds();

    // Empty classes are tolerated as not being NullMarked.
    newTranspilerTester("foo.A", "class A {}").assertTranspileSucceeds();

    // But the empty class cannot have a supertype.
    newTranspilerTester("foo.A", "class A {}")
        .addCompilationUnit("foo.B", "class B extends A {}")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:B.java:2: Type 'foo.B' must be directly or indirectly @NullMarked.");

    newTranspilerTester(
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
    newTranspilerTester(
            "test.Main",
            """
            abstract class Main {
              @javaemul.internal.annotations.KtProperty
              abstract int method(int foo);
            }
            """)
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "javaemul.internal.annotations.KtProperty", "public @interface KtProperty {}")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:4: Method 'int Main.method(int foo)' can not be '@KtProperty', as it"
                + " has non-empty parameters.");
  }

  public void testKtPropertyVoidReturnTypeFails() {
    newTranspilerTester(
            "test.Main",
            """
            abstract class Main {
              @javaemul.internal.annotations.KtProperty
              abstract void method();
            }
            """)
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "javaemul.internal.annotations.KtProperty", "public @interface KtProperty {}")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:4: Method 'void Main.method()' can not be '@KtProperty', as it has"
                + " void return type.");
  }

  public void testKtPropertyConstructorFails() {
    newTranspilerTester(
            "test.Main",
            """
            class Main {
              @javaemul.internal.annotations.KtProperty
              Main() {}
            }
            """)
        .addNullMarkPackageInfo("test")
        .addCompilationUnit(
            "javaemul.internal.annotations.KtProperty", "public @interface KtProperty {}")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:4: Constructor 'Main()' can not be '@KtProperty'.");
  }

  public void testSynchronizedMethodInUnsupportedTypeFails() {
    newTranspilerTester(
            "test.Child",
            """
            class Parent {}
            class Child extends Parent {
              synchronized void method() {}
            }
            """)
        .addNullMarkPackageInfo("test")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Child.java:3: Type 'Child' does not support synchronized methods as it does not "
                + "extend 'J2ktMonitor' or is not a direct subclass of 'Object'.");
  }

  public void testUnsupportedSynchronizedStatementFails() {
    newTranspilerTester(
            "test.Main",
            """
            class Main {
              void method() {
                synchronized (this) {}
              }
            }
            """)
        .addNullMarkPackageInfo("test")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Main.java:4: Synchronized statement is valid only on instances of 'Class' or"
                + " 'J2ktMonitor'.");
  }

  public void testExplicitQualifierInAnonymousNewInstance() {
    newTranspilerTester(
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
        .addNullMarkPackageInfo("test")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Outer.java:5: Explicit qualifier in constructor call is not supported.");
  }

  public void testExplicitQualifierInSuperCall() {
    newTranspilerTester(
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
        .addNullMarkPackageInfo("test")
        .assertTranspileFails()
        .assertErrorsWithSourcePosition(
            "Error:Outer.java:7: Explicit qualifier in constructor call is not supported.");
  }

  private TranspilerTester newTranspilerTester(String compilationUnitName, String code) {
    return TranspilerTester.newTesterWithJ2ktDefaults()
        .addCompilationUnit(compilationUnitName, code);
  }
}
