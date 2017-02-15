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
package com.google.j2cl.transpiler.integration;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests for JsInteropRestrictionsChecker.
 */
public class JsInteropRestrictionsCheckerTest extends IntegrationTestCase {

  //  // TODO: eventually test this for default methods in Java 8.
  //  public void testCollidingAccidentalOverrideConcreteMethodFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static interface Foo {",
  //        "  void doIt(Foo foo);",
  //        "}",
  //        "@JsType",
  //        "public static interface Bar {",
  //        "  void doIt(Bar bar);",
  //        "}",
  //        "public static class ParentBuggy {",
  //        "  public void doIt(Foo foo) {}",
  //        "  public void doIt(Bar bar) {}",
  //        "}",
  //        "public static class Buggy extends ParentBuggy implements Foo, Bar {",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 14: 'void EntryPoint.ParentBuggy.doIt(EntryPoint.Bar)' "
  //            + "(exposed by 'EntryPoint.Buggy') and "
  //            + "'void EntryPoint.ParentBuggy.doIt(EntryPoint.Foo)' "
  //            + "(exposed by 'EntryPoint.Buggy') "
  //            + "cannot both use the same JavaScript name 'doIt'.");
  //  }

  public void testCollidingAccidentalOverrideAbstractMethodFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
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
        .assertCompileFails(
            "'void Baz.doIt(Bar)' and "
                + "'void Baz.doIt(Foo)' cannot both use the same JavaScript name 'doIt'.");
  }

  //  public void testCollidingAccidentalOverrideHalfAndHalfFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "public static interface Foo {",
  //        "  void doIt(Foo foo);",
  //        "}",
  //        "@JsType",
  //        "public static interface Bar {",
  //        "   void doIt(Bar bar);",
  //        "}",
  //        "public static class ParentParent {",
  //        "  public void doIt(Bar x) {}",
  //        "}",
  //        "@JsType",
  //        "public static class Parent extends ParentParent {",
  //        "  public void doIt(Foo x) {}",
  //        "}",
  //        "public static class Buggy extends Parent implements Bar {}");
  //
  //    assertBuggyFails(
  //        "Line 12: 'void EntryPoint.ParentParent.doIt(EntryPoint.Bar)' "
  //            + "(exposed by 'EntryPoint.Buggy') and "
  //            + "'void EntryPoint.Parent.doIt(EntryPoint.Foo)' "
  //            + "cannot both use the same JavaScript name 'doIt'.");
  //  }
  //
  //  public void testCollidingFieldExportsFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //       "public static class Buggy {",
  //        "  @JsProperty",
  //        "  public static final int show = 0;",
  //        "  @JsProperty(name = \"show\")",
  //        "  public static final int display = 0;",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 8: 'int EntryPoint.Buggy.display' cannot be exported because the global "
  //            + "name 'test.EntryPoint.Buggy.show' is already taken by "
  //            + "'int EntryPoint.Buggy.show'.");
  //  }

  public void testJsPropertyNonGetterStyleSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty(name = \"x\") int x();",
            "  @JsProperty(name = \"x\") void x(int x);",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsPropertyGetterStyleSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "public abstract class Buggy {",
            "  @JsProperty static native int getStaticX();",
            "  @JsProperty static native void setStaticX(int x);",
            "  @JsProperty abstract int getX();",
            "  @JsProperty abstract void setX(int x);",
            "  @JsProperty abstract boolean isY();",
            "  @JsProperty abstract void setY(boolean y);",
            "}")
        .assertCompileSucceeds();
  }

  //  public void testJsPropertyIncorrectGetterStyleFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "public interface Buggy {",
  //        "  @JsProperty int isX();",
  //        "  @JsProperty int getY(int x);",
  //        "  @JsProperty void getZ();",
  //        "  @JsProperty void setX(int x, int y);",
  //        "  @JsProperty void setY();",
  //        "  @JsProperty int setZ(int z);",
  //        "  @JsProperty static void setStatic(){}",
  //        "  @JsProperty void setW(int... z);",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 6: JsProperty 'int EntryPoint.Buggy.isX()' cannot have a non-boolean return.",
  //        "Line 7: JsProperty 'int EntryPoint.Buggy.getY(int)' should have a correct setter "
  //            + "or getter signature.",
  //        "Line 8: JsProperty 'void EntryPoint.Buggy.getZ()' should have a correct setter "
  //            + "or getter signature.",
  //        "Line 9: JsProperty 'void EntryPoint.Buggy.setX(int, int)' should have a correct "
  //            + "setter or getter signature.",
  //        "Line 10: JsProperty 'void EntryPoint.Buggy.setY()' should have a correct setter "
  //            + "or getter signature.",
  //        "Line 11: JsProperty 'int EntryPoint.Buggy.setZ(int)' should have a correct setter "
  //            + "or getter signature.",
  //        "Line 12: JsProperty 'void EntryPoint.Buggy.setStatic()' should have a correct setter "
  //            + "or getter signature.",
  //        "Line 13: JsProperty 'void EntryPoint.Buggy.setW(int[])' cannot have a vararg "
  //            + "parameter.");
  //  }
  //
  //  public void testJsPropertyNonGetterStyleFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public interface Buggy {",
  //        "  @JsProperty boolean hasX();",
  //        "  @JsProperty int x();",
  //        "  @JsProperty void x(int x);",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 7: JsProperty 'boolean EntryPoint.Buggy.hasX()' should either follow Java Bean "
  //        + "naming conventions or provide a name.",
  //        "Line 8: JsProperty 'int EntryPoint.Buggy.x()' should either follow Java Bean "
  //        + "naming conventions or provide a name.",
  //        "Line 9: JsProperty 'void EntryPoint.Buggy.x(int)' should either follow Java Bean "
  //        + "naming conventions or provide a name.");
  //  }

  public void testCollidingJsPropertiesTwoGettersFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty",
            "  boolean isX();",
            "  @JsProperty",
            "  boolean getX();",
            "}")
        .assertCompileFails(
            "'boolean Buggy.getX()' and 'boolean Buggy.isX()' "
                + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsPropertiesTwoSettersFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty",
            "  void setX(boolean x);",
            "  @JsProperty",
            "  void setX(int x);",
            "}")
        .assertCompileFails(
            "'void Buggy.setX(int)' and "
                + "'void Buggy.setX(boolean)' cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsMethodAndJsPropertyGetterFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
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
        .assertCompileFails(
            "'int IBuggy.getX()' and 'boolean IBuggy.x(boolean)' "
                + "cannot both use the same JavaScript name 'x'.",
            "'int Buggy.getX()' and 'boolean Buggy.x(boolean)' "
                + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsMethodAndJsPropertySetterFails() throws Exception {
    TranspileResult result =
        compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "interface IBuggy {",
            "  @JsMethod",
            "  boolean x(boolean foo);",
            "  @JsProperty",
            "  void setX(int a);",
            "}",
            "public class Buggy implements IBuggy {",
            "  public boolean x(boolean foo) {return false;}",
            "  public void setX(int a) {}",
            "}");

    result.assertCompileFails(
        "'void IBuggy.setX(int)' and 'boolean IBuggy.x(boolean)' "
            + "cannot both use the same JavaScript name 'x'.",
        "'void Buggy.setX(int)' and 'boolean Buggy.x(boolean)' "
            + "cannot both use the same JavaScript name 'x'.");
  }

  //  // TODO(rluble): enable when static property definitions are implemented.
  //  public void __disabled__testCollidingPropertyAccessorExportsFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "public static class Buggy {",
  //        "  @JsProperty",
  //        "  public static void setDisplay(int x) {}",
  //        "  @JsProperty(name = \"display\")",
  //        "  public static void setDisplay2(int x) {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 8: 'void EntryPoint.Buggy.setDisplay2(int)' cannot be exported because the "
  //            + "global name 'test.EntryPoint.Buggy.display' is already taken "
  //            + "by 'void EntryPoint.Buggy.setDisplay(int)'.");
  //  }
  //
  //  public void testCollidingMethodExportsFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetClassDecl(
  //        "public static class Buggy {",
  //        "  @JsMethod",
  //        "  public static void show() {}",
  //        "  @JsMethod(name = \"show\")",
  //        "  public static void display() {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 8: 'void EntryPoint.Buggy.display()' cannot be exported because the global name "
  //            + "'test.EntryPoint.Buggy.show' is already taken "
  //            + "by 'void EntryPoint.Buggy.show()'.");
  //  }
  //
  //  // TODO(rluble): enable when static property definitions are implemented.
  //  public void __disabled__testCollidingMethodToPropertyAccessorExportsFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "public static class Buggy {",
  //        "  @JsProperty",
  //        "  public static void setShow(int x) {}",
  //        "  @JsMethod",
  //        "  public static void show() {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 9: 'void EntryPoint.Buggy.show()' cannot be exported because the global name "
  //            + "'test.EntryPoint.Buggy.show' is already taken by "
  //            + "'void EntryPoint.Buggy.setShow(int)'.");
  //  }
  //
  //  public void testCollidingMethodToFieldExportsFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "public static class Buggy {",
  //        "  @JsMethod",
  //        "  public static void show() {}",
  //        "  @JsProperty",
  //        "  public static final int show = 0;",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 7: 'void EntryPoint.Buggy.show()' cannot be exported because the global name "
  //            + "'test.EntryPoint.Buggy.show' is already taken by "
  //            + "'int EntryPoint.Buggy.show'.");
  //  }

  public void testCollidingMethodToFieldJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  public void show() {}",
            "  public final int show = 0;",
            "}")
        .assertCompileFails(
            "'int Buggy.show' and 'void Buggy.show()' "
                + "cannot both use the same JavaScript name 'show'.");
  }

  public void testCollidingMethodToMethodJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  public void show(int x) {}",
            "  public void show() {}",
            "}")
        .assertCompileFails(
            "'void Buggy.show()' and 'void Buggy.show(int)' "
                + "cannot both use the same JavaScript name 'show'.");
  }

  //  public void testCollidingSubclassExportedFieldToFieldJsTypeSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class ParentBuggy {",
  //        "  public int foo = 55;",
  //        "}",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  public int foo = 110;",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testCollidingSubclassExportedFieldToMethodJsTypeSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class ParentBuggy {",
  //        "  public int foo = 55;",
  //        "}",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  public void foo(int a) {}",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testCollidingSubclassExportedMethodToMethodJsTypeSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class ParentBuggy {",
  //        "  public void foo() {}",
  //        "}",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  public void foo(int a) {}",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testCollidingSubclassFieldToExportedFieldJsTypeSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "public static class ParentBuggy {",
  //        "  public int foo = 55;",
  //        "}",
  //        "@JsType",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  public int foo = 110;",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testCollidingSubclassFieldToExportedMethodJsTypeSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "public static class ParentBuggy {",
  //        "  public int foo = 55;",
  //        "}",
  //        "@JsType",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  public void foo(int a) {}",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testCollidingSubclassFieldToFieldJsTypeFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class ParentBuggy {",
  //        "  public int foo = 55;",
  //        "}",
  //        "@JsType",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  public int foo = 110;",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 10: 'int EntryPoint.Buggy.foo' and 'int EntryPoint.ParentBuggy.foo' cannot both "
  //            + "use the same JavaScript name 'foo'.");
  //  }
  //
  //  public void testCollidingSubclassFieldToMethodJsTypeFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class ParentBuggy {",
  //        "  public int foo = 55;",
  //        "}",
  //        "@JsType",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  public void foo(int a) {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 10: 'void EntryPoint.Buggy.foo(int)' and 'int EntryPoint.ParentBuggy.foo' cannot "
  //            + "both use the same JavaScript name 'foo'.");
  //  }
  //
  //  public void testCollidingSubclassMethodToExportedMethodJsTypeSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "public static class ParentBuggy {",
  //        "  public void foo() {}",
  //        "}",
  //        "@JsType",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  public void foo(int a) {}",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testCollidingSubclassMethodToMethodInterfaceJsTypeFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public interface IBuggy1 {",
  //        "  void show();",
  //        "}",
  //        "@JsType",
  //        "public interface IBuggy2 {",
  //        "  void show(boolean b);",
  //        "}",
  //        "public static class Buggy implements IBuggy1 {",
  //        "  public void show() {}",
  //        "}",
  //        "public static class Buggy2 extends Buggy implements IBuggy2 {",
  //        "  public void show(boolean b) {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 16: 'void EntryPoint.Buggy2.show(boolean)' and 'void EntryPoint.Buggy.show()' "
  //            + "cannot both use the same JavaScript name 'show'.");
  //  }
  //
  //  public void testCollidingSubclassMethodToMethodJsTypeFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class ParentBuggy {",
  //        "  public void foo() {}",
  //        "}",
  //        "@JsType",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  public void foo(int a) {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 10: 'void EntryPoint.Buggy.foo(int)' and 'void EntryPoint.ParentBuggy.foo()' "
  //            + "cannot both use the same JavaScript name 'foo'.");
  //  }
  //
  //  public void testCollidingSubclassMethodToMethodTwoLayerInterfaceJsTypeFails()
  //        throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public interface IParentBuggy1 {",
  //        "  void show();",
  //        "}",
  //        "public interface IBuggy1 extends IParentBuggy1 {",
  //        "}",
  //        "@JsType",
  //        "public interface IParentBuggy2 {",
  //        "  void show(boolean b);",
  //        "}",
  //        "public interface IBuggy2 extends IParentBuggy2 {",
  //        "}",
  //        "public static class Buggy implements IBuggy1 {",
  //        "  public void show() {}",
  //        "}",
  //        "public static class Buggy2 extends Buggy implements IBuggy2 {",
  //        "  public void show(boolean b) {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 20: 'void EntryPoint.Buggy2.show(boolean)' and 'void EntryPoint.Buggy.show()' "
  //            + "cannot both use the same JavaScript name 'show'.");
  //  }

  public void testNonCollidingSyntheticBridgeMethodSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "interface Comparable<T> {",
            "  int compareTo(T other);",
            "}",
            "@JsType",
            "class Enum<E extends Enum<E>> implements Comparable<E> {",
            "  public int compareTo(E other) {return 0;}",
            "}",
            "public class Buggy {}")
        .assertCompileSucceeds();
  }

  public void testCollidingSyntheticBridgeMethodSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "interface Comparable<T> {",
            "  int compareTo(T other);",
            "}",
            "@JsType",
            "class Enum<E extends Enum<E>> implements Comparable<E> {",
            "  public int compareTo(E other) {return 0;}",
            "}",
            "public class Buggy {}")
        .assertCompileSucceeds();
  }

  public void testSpecializeReturnTypeInImplementorSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "interface I {",
            "  I m();",
            "}",
            "@JsType",
            "class Buggy implements I {",
            "  public Buggy m() { return null; } ",
            "}")
        .assertCompileSucceeds();
  }

  public void testSpecializeReturnTypeInSubclassSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "class S {",
            "  public S m() { return null; }",
            "}",
            "@JsType",
            "public class Buggy extends S {",
            "  public Buggy m() { return null; } ",
            "}")
        .assertCompileSucceeds();
  }

  //  public void testCollidingTwoLayerSubclassFieldToFieldJsTypeFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class ParentParentBuggy {",
  //        "  public int foo = 55;",
  //        "}",
  //        "public static class ParentBuggy extends ParentParentBuggy {",
  //        "  public int foo = 55;",
  //        "}",
  //        "@JsType",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  public int foo = 110;",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 13: 'int EntryPoint.Buggy.foo' and 'int EntryPoint.ParentParentBuggy.foo' cannot "
  //            + "both use the same JavaScript name 'foo'.");
  //  }
  //
  //  public void testShadowedSuperclassJsMethodFails() {
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetClassDecl(
  //        "public static class ParentBuggy {",
  //        "  @JsMethod private void foo() {}",
  //        "}",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  @JsMethod private void foo() {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 8: 'void EntryPoint.Buggy.foo()' and 'void EntryPoint.ParentBuggy.foo()' cannot "
  //            + "both use the same JavaScript name 'foo'.");
  //  }
  //
  //  public void testRenamedSuperclassJsMethodFails() {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class ParentBuggy {",
  //        "  public void foo() {}",
  //        "}",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  @JsMethod(name = \"bar\") public void foo() {}",
  //        "}");
  //
  //    assertBuggyFails("Line 10: 'void EntryPoint.Buggy.foo()' cannot be assigned a different "
  //        + "JavaScript name than the method it overrides.");
  //  }
  //
  //  public void testRenamedSuperInterfaceJsMethodFails() {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public interface ParentBuggy {",
  //        "  void foo();",
  //        "}",
  //        "public interface Buggy extends ParentBuggy {",
  //        "  @JsMethod(name = \"bar\") void foo();",
  //        "}");
  //
  //    assertBuggyFails("Line 10: 'void EntryPoint.Buggy.foo()' cannot be assigned a different "
  //        + "JavaScript name than the method it overrides.");
  //  }
  //
  //  public void testAccidentallyRenamedSuperInterfaceJsMethodFails() {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public interface IBuggy {",
  //        "  void foo();",
  //        "}",
  //        "@JsType",
  //        "public static class ParentBuggy {",
  //        "  @JsMethod(name = \"bar\") public void foo() {}",
  //        "}",
  //        "public static class Buggy extends ParentBuggy implements IBuggy {",
  //        "}");
  //
  //    assertBuggyFails("Line 11: 'void EntryPoint.ParentBuggy.foo()' "
  //        + "(exposed by 'EntryPoint.Buggy') "
  //        + "cannot be assigned a different JavaScript name than the method it overrides.");
  //  }
  //
  //  public void testRenamedSuperclassJsPropertyFails() {
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "public static class ParentBuggy {",
  //        "  @JsProperty public int getFoo() { return 0; }",
  //        "}",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  @JsProperty(name = \"bar\") public int getFoo() { return 0; }",
  //        "}");
  //
  //    assertBuggyFails("Line 8: 'int EntryPoint.Buggy.getFoo()' "
  //        + "cannot be assigned a different JavaScript name than the method it overrides.");
  //  }
  //
  //  public void testJsPropertyDifferentFlavourInSubclassFails() {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class ParentBuggy {",
  //        "  @JsProperty public boolean isFoo() { return false; }",
  //        "}",
  //        "public static class Buggy extends ParentBuggy {",
  //        "  @JsProperty public boolean getFoo() { return false;}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 10: 'boolean EntryPoint.Buggy.getFoo()' and 'boolean EntryPoint.ParentBuggy"
  //            + ".isFoo()' cannot both use the same JavaScript name 'foo'.");
  //  }

  public void testConsistentPropertyTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
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
        .assertCompileSucceeds();
  }

  //  public void testInconsistentGetSetPropertyTypeFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static interface IBuggy {",
  //        "  @JsProperty",
  //        "  public int getFoo();",
  //        "  @JsProperty",
  //        "  public void setFoo(Integer value);",
  //        "}",
  //        "public static class Buggy implements IBuggy {",
  //        "  public int getFoo() {return 0;}",
  //        "  public void setFoo(Integer value) {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 10: JsProperty setter 'void EntryPoint.IBuggy.setFoo(Integer)' and "
  //            + "getter 'int EntryPoint.IBuggy.getFoo()' cannot have inconsistent types.",
  //        "Line 14: JsProperty setter 'void EntryPoint.Buggy.setFoo(Integer)' and "
  //            + "getter 'int EntryPoint.Buggy.getFoo()' cannot have inconsistent types.");
  //  }
  //
  //  public void testInconsistentIsSetPropertyTypeFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static interface IBuggy {",
  //        "  @JsProperty",
  //        "  public boolean isFoo();",
  //        "  @JsProperty",
  //        "  public void setFoo(Object value);",
  //        "}",
  //        "public static class Buggy implements IBuggy {",
  //        "  public boolean isFoo() {return false;}",
  //        "  public void setFoo(Object value) {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 10: JsProperty setter 'void EntryPoint.IBuggy.setFoo(Object)' and "
  //            + "getter 'boolean EntryPoint.IBuggy.isFoo()' cannot have inconsistent types.",
  //        "Line 14: JsProperty setter 'void EntryPoint.Buggy.setFoo(Object)' and "
  //            + "getter 'boolean EntryPoint.Buggy.isFoo()' cannot have inconsistent types.");
  //  }
  //
  //  public void testJsPropertySuperCallFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "@JsType public static class Super {",
  //        "  @JsProperty public int getX() { return 5; }",
  //        "}",
  //        "@JsType public static class Buggy extends Super {",
  //        "  public int m() { return super.getX(); }",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 9: Cannot call property accessor 'int EntryPoint.Super.getX()' via super.");
  //  }
  //
  //  public void testJsPropertyOnStaticMethodFails() {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "@JsType public static class Buggy {",
  //        "  @JsProperty public static int getX() { return 0; }",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 6: Static property accessor 'int EntryPoint.Buggy.getX()' can only be native.");
  //  }
  //
  //  public void testJsPropertyCallSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "@JsType public static class Super {",
  //        "  @JsProperty public int getX() { return 5; }",
  //        "}",
  //        "@JsType public static class Buggy extends Super {",
  //        "  public int m() { return getX(); }",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testJsPropertyAccidentalSuperCallSucceeds()
  //      throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "@JsType public static class Super {",
  //        "  @JsProperty public int getX() { return 5; }",
  //        "}",
  //        "@JsType public interface Interface {",
  //        "  @JsProperty int getX();",
  //        "}",
  //
  //        "@JsType public static class Buggy extends Super implements Interface {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testJsPropertyOverrideSucceeds()
  //      throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "@JsType public static class Super {",
  //        "  @JsProperty public void setX(int x) {  }",
  //        "  @JsProperty public int getX() { return 5; }",
  //        "}",
  //
  //        "@JsType public static class Buggy extends Super {",
  //        "  @JsProperty public void setX(int x) {  }",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testMixingJsMethodJsPropertyFails()
  //      throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "public static class Super {",
  //        "  @JsMethod public int getY() { return 5; }",
  //        "  @JsProperty public void setZ(int z) {}",
  //        "}",
  //
  //        "public static class Buggy extends Super {",
  //        "  @JsProperty(name = \"getY\") public int getY() { return 6; }",
  //        "  @JsMethod(name = \"z\") public void setZ(int z) {}",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 10: 'int EntryPoint.Buggy.getY()' and 'int EntryPoint.Super.getY()' cannot "
  //            + "both use the same JavaScript name 'getY'.",
  //        "Line 11: 'void EntryPoint.Buggy.setZ(int)' and 'void EntryPoint.Super.setZ(int)' "
  //           + "cannot both use the same JavaScript name 'z'.");
  //  }
  //
  //  public void testJsMethodJSNIVarargsWithNoReferenceSucceeds()
  //      throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetClassDecl(
  //        "public static class Buggy {",
  //        "  @JsMethod public native void m(int i, int... z) /*-{ return arguments[i]; }-*/;",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testJsMethodJSNIVarargsWithReferenceFails() {
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetClassDecl(
  //        "public static class Buggy {",
  //        "  @JsMethod public native void m(int i, int... z) /*-{ return z[0];}-*/;",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 5: Cannot access vararg parameter 'z' from JSNI in JsMethod "
  //            + "'void EntryPoint.Buggy.m(int, int[])'. Use 'arguments' instead.");
  //  }

  public void testMultiplePrivateConstructorsExportSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  private Buggy() {}",
            "  private Buggy(int a) {}",
            "}")
        .assertCompileSucceeds();
  }

  public void testMultiplePublicConstructorsAllDelegatesToJsConstructorSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsIgnore;",
            "@JsType",
            "public class Buggy {",
            "  public Buggy() {}",
            "  @JsIgnore",
            "  public Buggy(int a) {",
            "    this();",
            "  }",
            "}")
        .assertCompileSucceeds();
  }

  //  public void testMultipleConstructorsNotAllDelegatedToJsConstructorFails()
  //      throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class Buggy {",
  //        "  public Buggy() {}",
  //        "  private Buggy(int a) {",
  //        "    new Buggy();",
  //        "  }",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 6: Constructor 'EntryPoint.Buggy.EntryPoint$Buggy()' can be a JsConstructor only "
  //            + "if all constructors in the class are delegating to it.");
  //  }
  //
  //  public void testMultiplePublicConstructorsExportFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static class Buggy {",
  //        "  public Buggy() {}",
  //        "  public Buggy(int a) {",
  //        "    this();",
  //        "  }",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 5: More than one JsConstructor exists for EntryPoint.Buggy.",
  //        "Line 7: 'EntryPoint.Buggy.EntryPoint$Buggy(int)' cannot be exported because the "
  //            + "global name 'test.EntryPoint.Buggy' is already taken by "
  //            + "'EntryPoint.Buggy.EntryPoint$Buggy()'.");
  //  }

  public void testNonCollidingAccidentalOverrideSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "interface Foo {",
            "  void doIt(Object foo);",
            "}",
            "class ParentParent {",
            "  public void doIt(String x) {}",
            "}",
            "@JsType",
            "class Parent extends ParentParent {",
            "  public void doIt(Object x) {}",
            "}",
            "public class Buggy extends Parent implements Foo {}")
        .assertCompileSucceeds();
  }

  public void testJsNameInvalidNamesFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsPackage;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType(name = \"a.b.c\") public class Buggy {",
            "   @JsMethod(name = \"34s\") public void m() {}",
            "   @JsProperty(name = \"s^\") public int  m;",
            "   @JsProperty(name = \"\") public int n;",
            "   @JsProperty(namespace = JsPackage.GLOBAL, name = \"a.b.c.d\") public static int o;",
            "}",
            "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"*\")",
            "class BadGlobalStar {",
            "}",
            "@JsType(namespace = JsPackage.GLOBAL, name = \"?\") interface BadGlobalWildcard {",
            "}",
            "@JsType(isNative = true, namespace = \"a.b\", name = \"*\") interface BadStar {",
            "}")
        .assertCompileFails(
            "'Buggy' has invalid name 'a.b.c'.",
            "'void Buggy.m()' has invalid name '34s'.",
            "'int Buggy.m' has invalid name 's^'.",
            "'int Buggy.n' cannot have an empty name.",
            "'int Buggy.o' has invalid name 'a.b.c.d'.",
            "Only native interfaces in the global namespace can be named '*'.",
            "Only native interfaces in the global namespace can be named '?'.",
            "Only native interfaces in the global namespace can be named '*'.");
  }

  public void testJsNameInvalidNamespacesFails() throws Exception {
    TranspileResult result =
        compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType(namespace = \"a.b.\") public class Buggy {",
            "   @JsMethod(namespace = \"34s\") public static void m() {}",
            "   @JsProperty(namespace = \"s^\") public static int  n;",
            "   @JsMethod(namespace = \"\") public static void o() {}",
            "   @JsProperty(namespace = \"\") public int p;",
            "   @JsMethod(namespace = \"a\") public void q() {}",
            "}");

    result.assertCompileFails(
        "'Buggy' has invalid namespace 'a.b.'.",
        "'void Buggy.m()' has invalid namespace '34s'.",
        "'int Buggy.n' has invalid namespace 's^'.",
        "'void Buggy.o()' cannot have an empty namespace.",
        "Instance member 'int Buggy.p' cannot declare a namespace.",
        "Instance member 'void Buggy.q()' cannot declare a namespace.");
  }

  public void testJsNameGlobalNamespacesSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "import jsinterop.annotations.JsPackage;",
            "@JsType(namespace = JsPackage.GLOBAL) public class Buggy {",
            "   @JsMethod(namespace = JsPackage.GLOBAL)",
            "   public static void m() {}",
            "   @JsProperty(namespace = JsPackage.GLOBAL)",
            "   public static int  n;",
            "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.b\")",
            "   public native static int o();",
            "}")
        .assertCompileSucceeds();
  }

  public void testSingleJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  public static void show1() {}",
            "  public void show2() {}",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsFunctionSucceeds() throws Exception {
    compile(
            source(
                "Function",
                "import jsinterop.annotations.JsFunction;",
                "import jsinterop.annotations.JsOverlay;",
                "@JsFunction",
                "public interface Function {",
                "  int getFoo();",
                "  @JsOverlay",
                "  static String s = new String();",
                "  @JsOverlay",
                "  default void m() {}",
                "  @JsOverlay",
                "  static void n() {}",
                "}"),
            source(
                "Buggy",
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
                "  String x = new String();",
                "  static int y;",
                "}"))
        .assertCompileSucceeds();
  }

  public void testJsFunctionOnClassFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsFunction;",
            "@JsFunction",
            "public class Buggy {",
            "}")
        .assertCompileFails("JsFunction 'Buggy' has to be a functional interface.");
  }

  public void testJsFunctionExtendsInterfaceFails() throws Exception {
    compile(
            source("AnotherInterface", "public interface AnotherInterface {}"),
            source(
                "Buggy",
                "import jsinterop.annotations.JsFunction;",
                "@JsFunction",
                "public interface Buggy extends AnotherInterface {",
                "  void foo();",
                "}"))
        .assertCompileFails("JsFunction 'Buggy' cannot extend other interfaces.");
  }

  public void testJsFunctionExtendedByInterfaceFails() throws Exception {
    compile(
            source("Buggy", "public interface Buggy extends MyJsFunctionInterface {}"),
            source(
                "MyJsFunctionInterface",
                "import jsinterop.annotations.JsFunction;",
                "@JsFunction public interface MyJsFunctionInterface {",
                " int foo(int x);",
                "}"))
        .assertCompileFails("'Buggy' cannot extend JsFunction 'MyJsFunctionInterface'.");
  }

  public void testJsFunctionMarkedAsJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsFunction;",
            "@JsFunction @JsType",
            "public interface Buggy {",
            "  void foo();",
            "}")
        .assertCompileFails("'Buggy' cannot be both a JsFunction and a JsType at the same time.");
  }

  public void testJsFunctionImplementationWithSingleInterfaceSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "public class Buggy implements MyJsFunctionInterface {",
                "  public int foo(int x) { return 0; }",
                "}"),
            source(
                "MyJsFunctionInterface",
                "import jsinterop.annotations.JsFunction;",
                "@JsFunction public interface MyJsFunctionInterface {",
                " int foo(int x);",
                "}"))
        .assertCompileSucceeds();
  }

  public void testJsFunctionImplementationWithMultipleSuperInterfacesFails() throws Exception {
    compile(
            source("AnotherInterface", "public interface AnotherInterface {}"),
            source(
                "Buggy",
                "public class Buggy implements MyJsFunctionInterface, AnotherInterface {",
                "  public int foo(int x) { return 0; }",
                "  public int bar(int x) { return 0; }",
                "}"),
            source(
                "MyJsFunctionInterface",
                "import jsinterop.annotations.JsFunction;",
                "@JsFunction public interface MyJsFunctionInterface {",
                " int foo(int x);",
                "}"))
        .assertCompileFails(
            "JsFunction implementation 'Buggy' cannot implement more than one interface.");
  }

  public void testJsFunctionImplementationWithSuperClassFails() throws Exception {
    compile(
            source("BaseClass", "public class BaseClass {}"),
            source(
                "Buggy",
                "public class Buggy extends BaseClass implements MyJsFunctionInterface {",
                "  public int foo(int x) { return 0; }",
                "}"),
            source(
                "MyJsFunctionInterface",
                "import jsinterop.annotations.JsFunction;",
                "@JsFunction public interface MyJsFunctionInterface {",
                " int foo(int x);",
                "}"))
        .assertCompileFails("JsFunction implementation 'Buggy' cannot extend a class.");
  }

  public void testJsFunctionImplementationWithSubclassesFails() throws Exception {
    compile(
            source(
                "BaseClass",
                "public class BaseClass implements MyJsFunctionInterface {",
                "  public int foo(int x) { return 0; }",
                "}"),
            source("Buggy", "public class Buggy extends BaseClass  {", "}"),
            source(
                "MyJsFunctionInterface",
                "import jsinterop.annotations.JsFunction;",
                "@JsFunction public interface MyJsFunctionInterface {",
                " int foo(int x);",
                "}"))
        .assertCompileFails("'Buggy' cannot extend JsFunction implementation 'BaseClass'.");
  }

  public void testJsFunctionImplementationMarkedAsJsTypeFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType",
                "public class Buggy implements MyJsFunctionInterface {",
                "  public int foo(int x) { return 0; }",
                "}"),
            source(
                "MyJsFunctionInterface",
                "import jsinterop.annotations.JsFunction;",
                "@JsFunction public interface MyJsFunctionInterface {",
                " int foo(int x);",
                "}"))
        .assertCompileFails(
            "'Buggy' cannot be both a JsFunction implementation and a "
                + "JsType at the same time.");
  }

  public void testNativeJsTypeStaticInitializerSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy {",
                "  static {",
                "    int x = 1;",
                "  }",
                "}"),
            source(
                "Buggy2",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy2 {",
                "  static {",
                "    Object.class.getName();",
                "  }",
                "}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeInstanceInitializerFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy {",
                "  {",
                "    Object.class.getName();",
                "  }",
                "}"),
            source(
                "Buggy2",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy2 {",
                "  {",
                "    int x = 1;",
                "  }",
                "}"))
        .assertCompileFails(
            "Native JsType 'Buggy' cannot have initializer.",
            "Native JsType 'Buggy2' cannot have initializer.");
  }

  public void testNativeJsTypeNonEmptyConstructorFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  public Buggy(int n) {",
            "    n++;",
            "  }",
            "}")
        .assertCompileFails(
            "Native JsType constructor 'Buggy.Buggy(int)' cannot have non-empty method body.");
  }

  public void testNativeJsTypeImplicitSuperSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {",
                "  public Buggy(int n) {}",
                "}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Super {",
                "  public Super() {}",
                "}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeExplicitSuperSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {",
                "  public Buggy(int n) {",
                "    super(n);",
                "  }",
                "}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Super {",
                "  public Super(int x) {}",
                "}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeExplicitSuperWithEffectSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {",
                "  public Buggy(int n) {",
                "    super(n++);",
                "  }",
                "}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Super {",
                "  public Super(int x) {}",
                "}"))
        .assertCompileSucceeds();
  }

  //  public void testJsTypeInterfaceInInstanceofFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType(isNative=true) public interface IBuggy {}",
  //        "@JsType public static class Buggy {",
  //        "  public Buggy() { if (new Object() instanceof IBuggy) {} }",
  //        "}");
  //
  //    assertBuggyFails("Line 6: Cannot do instanceof against native JsType interface "
  //        + "'EntryPoint.IBuggy'.");
  //  }
  //
  public void testNativeJsTypeEnumFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public enum Buggy {",
            "  A,",
            "  B",
            "}")
        .assertCompileFails("Enum 'Buggy' cannot be a native JsType.");
  }

  public void testInnerNativeJsTypeFails() throws Exception {
    compile(
            "EntryPoint",
            "import jsinterop.annotations.JsType;",
            "public class EntryPoint {",
            "  @JsType(isNative = true)",
            "  public class Buggy {}",
            "}")
        .assertCompileFails("Non static inner class 'Buggy' cannot be a native JsType.");
  }

  public void testInnerJsTypeSucceeds() throws Exception {
    compile(
            "EntryPoint",
            "import jsinterop.annotations.JsType;",
            "public class EntryPoint {",
            "  @JsType",
            "  public static class Buggy {}",
            "}")
        .assertCompileSucceeds();
  }

  public void testLocalJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "public class Buggy { void m() { @JsType class Local {} } }")
        .assertCompileFails("Local class '$1Local' cannot be a JsType.");
  }

  public void testNativeJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Super {}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeImplementsNativeJsTypeSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy implements Super {}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Super {}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeInterfaceImplementsNativeJsTypeSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Buggy extends Super {}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Super {}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeExtendsJsTypeFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType",
                "public class Super {}"))
        .assertCompileFails("Native JsType 'Buggy' can only extend native JsType classes.");
  }

  public void testNativeJsTypeImplementsJsTypeInterfaceFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy implements Interface {}"),
            source(
                "Interface",
                "import jsinterop.annotations.JsType;",
                "@JsType",
                "public interface Interface {}"))
        .assertCompileFails("Native JsType 'Buggy' can only implement native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceExtendsJsTypeInterfaceFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Buggy extends Interface {}"),
            source(
                "Interface",
                "import jsinterop.annotations.JsType;",
                "@JsType",
                "public interface Interface {}"))
        .assertCompileFails("Native JsType 'Buggy' can only extend native JsType interfaces.");
  }

  public void testNativeJsTypeImplementsNonJsTypeFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy implements Interface {}"),
            source("Interface", "public interface Interface {}"))
        .assertCompileFails("Native JsType 'Buggy' can only implement native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceExtendsNonJsTypeFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Buggy extends Super {}"),
            source("Super", "public interface Super {}"))
        .assertCompileFails("Native JsType 'Buggy' can only extend native JsType interfaces.");
  }

  //  public void testNativeJsTypeInterfaceDefenderMethodsFails() {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsOverlay");
  //    addSnippetClassDecl(
  //        "@JsType(isNative=true) public interface Interface {",
  //        "  @JsOverlay default void someOtherMethod(){}",
  //        "}",
  //        "public static class OtherClass implements Interface {",
  //        "  public void someOtherMethod() {}",
  //        "}",
  //        "@JsType(isNative=true) public interface Buggy extends Interface {",
  //        "  default void someMethod(){}",
  //        "  void someOtherMethod();",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 9: Method 'void EntryPoint.OtherClass.someOtherMethod()' cannot override a "
  //            + "JsOverlay method 'void EntryPoint.Interface.someOtherMethod()'.",
  //        "Line 12: Native JsType method 'void EntryPoint.Buggy.someMethod()' should be native "
  //            + "or abstract.",
  //        "Line 13: Method 'void EntryPoint.Buggy.someOtherMethod()' cannot override a JsOverlay"
  //            + " method 'void EntryPoint.Interface.someOtherMethod()'.");
  //  }
  //
  public void testJsOverlayOnNativeJsTypeInterfaceSucceds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public interface Buggy {",
            "  @JsOverlay Object obj = new Object();",
            "  // TODO: uncomment once default method is supported.",
            "  // @JsOverlay default void someOverlayMethod(){};",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsOverlayOnNativeJsTypeMemberSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  @JsOverlay public static Object object = new Object();",
            "  @JsOverlay",
            "  public static void m() {}",
            "  @JsOverlay",
            "  public static void m(int x) {}",
            "  @JsOverlay",
            "  private static void m(boolean x) {}",
            "  @JsOverlay",
            "  public final void n() {}",
            "  @JsOverlay",
            "  public final void n(int x) {}",
            "  @JsOverlay",
            "  private final void n(boolean x) {}",
            "  @JsOverlay",
            "  final void o() {}",
            "  @JsOverlay",
            "  protected final void p() {}",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsOverlayImplementingInterfaceMethodFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsOverlay;",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy implements IBuggy {",
                "  @JsOverlay",
                "  public void m() {}",
                "}"),
            source(
                "IBuggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface IBuggy {",
                "  void m();",
                "}"))
        .assertCompileFails(
            "JsOverlay method 'void Buggy.m()' cannot override a supertype method.");
  }

  public void testJsOverlayOverridingSuperclassMethodFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsOverlay;",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {",
                "  @JsOverlay",
                "  public void m() {}",
                "}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Super {",
                "  public native void m();",
                "}"))
        .assertCompileFails(
            "JsOverlay method 'void Buggy.m()' cannot override a supertype method.");
  }

  public void testJsOverlayOnNonFinalMethodAndInstanceFieldFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  @JsOverlay public final int f2 = 2;",
            "  @JsOverlay",
            "  public void m() {}",
            "}")
        .assertCompileFails(
            "JsOverlay field 'int Buggy.f2' can only be static.",
            "JsOverlay method 'void Buggy.m()' cannot be non-final nor native.");
  }

  public void testJsOverlayWithStaticInitializerSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  @JsOverlay public static final Object f1 = new Object();",
            "  @JsOverlay public static int f2 = 2;",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsOverlayOnNativeMethodFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  @JsOverlay",
            "  public static final native void m1();",
            "  @JsOverlay",
            "  public final native void m2();",
            "}")
        .assertCompileFails(
            "JsOverlay method 'void Buggy.m1()' cannot be non-final nor native.",
            "JsOverlay method 'void Buggy.m2()' cannot be non-final nor native.");
  }

  //      // Not applicable to J2cl
  //      public void testJsOverlayOnJsoMethodSucceeds() throws Exception {
  //        addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
  //        addSnippetImport("jsinterop.annotations.JsOverlay");
  //        addSnippetClassDecl(
  //            "public static class Buggy extends JavaScriptObject {",
  //            "  protected Buggy() { }",
  //            "  @JsOverlay public final void m() { }",
  //            "}");
  //
  //        assertBuggySucceeds();
  //      }
  //
  //      // Not applicable to J2cl
  //      public void testImplicitJsOverlayOnJsoMethodSucceeds() throws Exception {
  //        addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
  //        addSnippetImport("jsinterop.annotations.JsOverlay");
  //        addSnippetClassDecl(
  //            "public static class Buggy extends JavaScriptObject {",
  //            "  protected Buggy() { }",
  //            "  public final void m() { }",
  //            "}");
  //
  //        assertBuggySucceeds();
  //      }

  public void testJsOverlayOnNonNativeJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  @JsOverlay public static final int F = 2;",
            "  @JsOverlay",
            "  public final void m() {};",
            "}")
        .assertCompileFails(
            "JsOverlay 'int Buggy.F' can only be declared in a native type "
                + "or @JsFunction interface.",
            "JsOverlay 'void Buggy.m()' can only be declared in a native type "
                + "or @JsFunction interface.");
  }

  //  public void testJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType(isNative=true) public static class Super {",
  //        "}",
  //        "@JsType public static class Buggy extends Super {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testJsTypeExtendsNonJsTypeSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "public static class Super {",
  //        "}",
  //        "@JsType public static class Buggy extends Super {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testJsTypeImplementsNativeJsTypeInterfaceSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType(isNative=true) public interface Interface {",
  //        "}",
  //        "@JsType public static class Buggy implements Interface {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testJsTypeImplementsNonJsTypeInterfaceSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "public interface Interface {",
  //        "}",
  //        "@JsType public static class Buggy implements Interface {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testJsTypeIntefaceExtendsNativeJsTypeInterfaceSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType(isNative=true) public interface Interface {",
  //        "}",
  //        "@JsType public interface Buggy extends Interface {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testJsTypeInterfaceExtendsNonJsTypeInterfaceSucceeds()
  //      throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "public interface Interface {",
  //        "}",
  //        "@JsType public interface Buggy extends Interface {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //

  public void testNativeJsTypeBadMembersFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsIgnore;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  public static final int S = 42;",
            "  public int f = 42;",
            "  @JsIgnore",
            "  public Buggy() {}",
            "  @JsIgnore public int x;",
            "  @JsIgnore",
            "  public native void n();",
            "  public void o() {}",
            "  public native void p() /*-{}-*/;",
            "}")
        .assertCompileFails(
            "Native JsType field 'int Buggy.S' cannot have initializer.",
            "Native JsType field 'int Buggy.f' cannot have initializer.",
            "Native JsType member 'Buggy.Buggy()' cannot have @JsIgnore.",
            "Native JsType member 'int Buggy.x' cannot have @JsIgnore.",
            "Native JsType member 'void Buggy.n()' cannot have @JsIgnore.",
            "Native JsType method 'void Buggy.o()' should be native or abstract.");
  }

  public void testNativeMethodOnJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  @JsMethod",
            "  public native void m();",
            "}")
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public abstract class Buggy {",
            "  public static native void m();",
            "  protected static native void m(Object o);",
            "  private static native void m(String o);",
            "  public Buggy() {}",
            "  protected Buggy(Object o) {}",
            "  private Buggy(String o) {}",
            "  public native void n();",
            "  protected native void n(Object o);",
            "  private native void n(String o);",
            "  public abstract void o();",
            "  protected abstract void o(Object o);",
            "  abstract void o(String o);",
            "}",
            "@JsType(isNative = true)",
            "interface NativeFunctionalInterface {",
            "  void f();",
            " }",
            "class Main {",
            "  static void main() {",
            "    NativeFunctionalInterface i = () -> {};",
            "  }",
            "}")
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeFieldsSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  public static int f1;",
            "  protected static int f2;",
            "  private static int f3;",
            "  public int f4;",
            "  protected int f5;",
            "  private int f6;",
            "}")
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeDefaultConstructorSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {}",
            "")
        .assertCompileSucceeds();
  }

  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) class Super {",
            "  public native void m(Object o);",
            "  public native void m(Object[] o);",
            "}",
            "@JsType public class Buggy extends Super {",
            "  public void n(Object o) { }",
            "}")
        .assertCompileSucceeds();
  }

  //  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodOverloadsFails() {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType(isNative=true) public static class Super {",
  //        "  public native void m(Object o);",
  //        "  public native void m(int o);",
  //        "}",
  //        "public static class Buggy extends Super {",
  //        "  public void m(Object o) { }",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 9: 'void EntryPoint.Buggy.m(Object)' and 'void EntryPoint.Super.m(int)' "
  //            + "cannot both use the same JavaScript name 'm'.");
  //  }

  public void testNonJsTypeWithNativeStaticMethodOverloadsSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "public class Buggy {",
            "  @JsMethod public static native void m(Object o);",
            "  @JsMethod public static native void m(int o);",
            "}")
        .assertCompileSucceeds();
  }

  //  public void testNonJsTypeWithNativeInstanceMethodOverloadsFails() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetClassDecl(
  //        "public static class Buggy {",
  //        "  @JsMethod public native void m(Object o);",
  //        "  @JsMethod public void m(int o) { }",
  //        "}");
  //
  //    assertBuggyFails(
  //        "Line 6: 'void EntryPoint.Buggy.m(int)' and 'void EntryPoint.Buggy.m(Object)' "
  //            + "cannot both use the same JavaScript name 'm'.");
  //  }
  //
  //  public void testNonJsTypeExtendsJsTypeSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType public static class Super {",
  //        "}",
  //        "public static class Buggy extends Super {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testNonJsTypeImplementsJsTypeInterfaceSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType public interface Interface {",
  //        "}",
  //        "public static class Buggy implements Interface {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testNonJsTypeInterfaceExtendsJsTypeInterfaceSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType public interface Interface {",
  //        "}",
  //        "public interface Buggy extends Interface {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testNonJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType(isNative=true) public static class Super {",
  //        "  public native void m();",
  //        "}",
  //        "public static class Buggy extends Super {",
  //        "  public void m() { }",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testNonJsTypeImplementsNativeJsTypeInterfaceSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType(isNative=true) public interface Interface {",
  //        "}",
  //        "public static class Buggy implements Interface {",
  //        "}");
  //
  //    assertBuggySucceeds();
  //  }

  public void testNonJsTypeInterfaceExtendsNativeJsTypeInterfaceSucceeds() throws Exception {
    compile(
            source("Buggy", "public interface Buggy extends Interface {}"),
            source(
                "Interface",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Interface {}"))
        .assertCompileSucceeds();
  }

  //  public void testUnusableByJsSuppressionSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl("public static class A {}");
  //    addSnippetClassDecl(
  //        "@JsType @SuppressWarnings(\"unusable-by-js\")", // SuppressWarnings on the class.
  //        "public static class B {",
  //        "  public A field;",
  //        "  public A t0(A a, A b) { return null; }",
  //        "}");
  //    addSnippetClassDecl(
  //    "@JsType",
  //    "public static class Buggy {",
  //    "  @SuppressWarnings(\"unusable-by-js\") public A field;", // add SuppressWarnings to field.
  //    "  @SuppressWarnings({\"unusable-by-js\", \"unused\"})", // test multiple warnings.
  //    "  public A t0(A a, A b) { return null; }", // add SuppressWarnings to the method.
  //    "  public void t1(",
  //    "    @SuppressWarnings(\"unusable-by-js\")A a,",
  //    "    @SuppressWarnings(\"unusable-by-js\")A b",
  //    "  ) {}", // add SuppressWarnings to parameters.
  //    "}");
  //
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testUsableByJsTypesSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsExport");
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsFunction");
  //    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
  //    addSnippetClassDecl(
  //        "@JsType public static class A {}",
  //        "@JsType public static interface I {}",
  //        "@JsFunction public static interface FI {void foo();}",
  //        "public static class C extends JavaScriptObject {protected C(){}}",
  //        "@JsType public static class Buggy {",
  //        "  public void f1(boolean a, int b, double c) {}", // primitive types work fine.
  //        "  public void f2(Boolean a, Double b, String c) {}", // unboxed types work fine.
  //        "  public void f3(A a) {}", // JsType works fine.
  //        "  public void f4(I a) {}", // JsType interface works fine.
  //        "  public void f5(FI a) {}", // JsFunction works fine.
  //        "  public void f6(C a) {}", // JavaScriptObject works fine.
  //        "  public void f7(Object a) {}", // Java Object works fine.
  //        "  public void f8(boolean[] a) {}", // array of primitive types work fine.
  //        "  public void f9(Boolean[] a, Double[] b, String[] c) {}", // array of unboxed types.
  //        "  public void f10(A[] a) {}", // array of JsType works fine.
  //        "  public void f11(FI[] a) {}", // array of JsFunction works fine.
  //        "  public void f12(C[][] a) {}", // array of JavaScriptObject works fine.
  //        "  public void f13(Object[] a) {}", // Object[] works fine.
  //        "  public void f14(Object[][] a) {}", // Object[][] works fine.
  //        "}");
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testUnusableByJsNotExportedMembersSucceeds() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "public static class A {}",
  //        "@JsType public static class Buggy {",
  //        "  private A field;", // private field.
  //        "  private A f1(A a) { return null; }", // private method.
  //        "}");
  //    assertBuggySucceeds();
  //  }
  //
  //  public void testUnusuableByJsWarns() throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsFunction");
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetImport("jsinterop.annotations.JsMethod");
  //    addSnippetImport("jsinterop.annotations.JsProperty");
  //    addSnippetClassDecl(
  //        "public static class A {}",
  //        "@JsType public static interface I {}",
  //        "public static class B implements I {}",
  //        "public static class C {", // non-jstype class with JsMethod
  //        "  @JsMethod",
  //        "  public static void fc1(A a) {}", // JsMethod
  //        "}",
  //        "public static class D {", // non-jstype class with JsProperty
  //        "  @JsProperty",
  //        "  public static A a;", // JsProperty
  //        "}",
  //    "@JsFunction public static interface FI  { void f(A a); }", // JsFunction method is checked.
  //        "@JsType public static class Buggy {",
  //        "  public A f;", // exported field
  //        "  public A f1(A a) { return null; }", // regular class fails.
  //        "  public A[] f2(A[] a) { return null; }", // array of regular class fails.
  //        "  public long f3(long a) { return 1l; }", // long fails.
  //        // non-JsType class that implements a JsType interface fails.
  //        "  public B f4(B a) { return null; }",
  //        "}");
  //
  //    assertBuggySucceeds(
  //        "Line 12: [unusable-by-js] Type of parameter 'a' in "
  //          + "'void EntryPoint.C.fc1(EntryPoint.A)' is not usable by but exposed to JavaScript.",
  //        "Line 16: [unusable-by-js] Type of 'EntryPoint.A EntryPoint.D.a' is not usable by but "
  //            + "exposed to JavaScript.",
  //        "Line 18: [unusable-by-js] Type of parameter 'a' in 'void "
  //            + "EntryPoint.FI.f(EntryPoint.A)' is not usable by but exposed to JavaScript.",
  //        "Line 20: [unusable-by-js] Type of 'EntryPoint.A EntryPoint.Buggy.f' is not usable by "
  //            + "but exposed to JavaScript.",
  //        "Line 21: [unusable-by-js] Return type of 'EntryPoint.A EntryPoint.Buggy.f1(EntryPoint"
  //            + ".A)' is not usable by but exposed to JavaScript.",
  //        "Line 21: [unusable-by-js] Type of parameter 'a' in "
  //            + "'EntryPoint.A EntryPoint.Buggy.f1(EntryPoint.A)' is not usable by but "
  //            + "exposed to JavaScript.",
  //        "Line 22: [unusable-by-js] Return type of "
  //            + "'EntryPoint.A[] EntryPoint.Buggy.f2(EntryPoint.A[])' is not usable by but "
  //            + "exposed to JavaScript.",
  //        "Line 22: [unusable-by-js] Type of parameter 'a' in "
  //            + "'EntryPoint.A[] EntryPoint.Buggy.f2(EntryPoint.A[])' is not usable by but "
  //            + "exposed to JavaScript.",
  //        "Line 23: [unusable-by-js] Return type of 'long EntryPoint.Buggy.f3(long)' is not "
  //            + "usable by but exposed to JavaScript.",
  //        "Line 23: [unusable-by-js] Type of parameter 'a' in "
  //            + "'long EntryPoint.Buggy.f3(long)' is not usable by but exposed to JavaScript.",
  //        "Line 24: [unusable-by-js] Return type of 'EntryPoint.B EntryPoint.Buggy.f4(EntryPoint"
  //            + ".B)' is not usable by but exposed to JavaScript.",
  //        "Line 24: [unusable-by-js] Type of parameter 'a' in "
  //            + "'EntryPoint.B EntryPoint.Buggy.f4(EntryPoint.B)' is not usable by but "
  //            + "exposed to JavaScript.");
  //  }
  //
  //  public void testUnusableByJsAccidentalOverrideSuppressionWarns()
  //      throws Exception {
  //    addSnippetImport("jsinterop.annotations.JsType");
  //    addSnippetClassDecl(
  //        "@JsType",
  //        "public static interface Foo {",
  //        "  @SuppressWarnings(\"unusable-by-js\") ",
  //        "  void doIt(Class foo);",
  //        "}",
  //        "public static class Parent {",
  //        "  public void doIt(Class x) {}",
  //        "}",
  //        "public static class Buggy extends Parent implements Foo {}");
  //
  //    assertBuggySucceeds(
  //        "Line 10: [unusable-by-js] Type of parameter 'x' in "
  //            + "'void EntryPoint.Parent.doIt(Class)' (exposed by 'EntryPoint.Buggy') is not "
  //            + "usable by but exposed to JavaScript.");
  //  }
  //
  //  private static final MockJavaResource jsFunctionInterface = new MockJavaResource(
  //      "test.MyJsFunctionInterface") {
  //    @Override
  //    public CharSequence getContent() {
  //      StringBuilder code = new StringBuilder();
  //      code.append("package test;\n");
  //      code.append("import jsinterop.annotations.JsFunction;\n");
  //      code.append("@JsFunction public interface MyJsFunctionInterface {\n");
  //      code.append("  int foo(int x);\n");
  //      code.append("}\n");
  //      return code;
  //    }
  //  };

  public void testJsOptionalSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsConstructor;",
            "import jsinterop.annotations.JsFunction;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsOptional;",
            "public class Buggy<T> {",
            "  @JsConstructor public Buggy(@JsOptional Object a) {}",
            "  @JsMethod public void foo(int a, Object b, @JsOptional String c) {}",
            "  @JsMethod public void bar(int a, @JsOptional Object b, @JsOptional String c) {}",
            "  @JsMethod public void baz(@JsOptional String a, @JsOptional Object b) {}",
            "  @JsMethod public void qux(@JsOptional String c, Object... os) {}",
            "  @JsMethod public void corge(int a, @JsOptional T b, String... c) {}",
            "}",
            "class SubBuggy extends Buggy<String> {",
            "  public SubBuggy() { super(null); } ",
            "  @JsMethod public void bar(int a, @JsOptional String b, String... c) {}",
            "}",
            "@JsFunction interface Function {",
            "  void m(String a, @JsOptional String b);",
            "}",
            "class FunctionImpl implements Function {",
            "   public void m(String a, @JsOptional String b) {}",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsOptionalNotAtEndFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsConstructor;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsOptional;",
            "public class Buggy {",
            "   @JsConstructor",
            "   public Buggy(@JsOptional String a, Object b, @JsOptional String c) {}",
            "   @JsMethod",
            "   public void bar(int a, @JsOptional Object b, String c) {}",
            "   @JsMethod",
            "   public void baz(@JsOptional Object b, String c, Object... os) {}",
            "}")
        .assertCompileFails(
            "JsOptional parameter 'a' in method "
                + "'Buggy.Buggy(String, Object, String)' cannot precede parameters that are not "
                + "JsOptional.",
            "JsOptional parameter 'b' in method "
                + "'void Buggy.bar(int, Object, String)' cannot precede parameters that are not "
                + "JsOptional.",
            "JsOptional parameter 'b' in method "
                + "'void Buggy.baz(Object, String, Object[])' cannot precede parameters that are"
                + " not JsOptional.");
  }

  public void testJsOptionalOnInvalidParametersFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsConstructor;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsOptional;",
            "public class Buggy {",
            "   @JsConstructor public Buggy(@JsOptional int a) {}",
            "   @JsMethod public void bar(int a, @JsOptional Object b, @JsOptional String... c) {}",
            "}")
        .assertCompileFails(
            "JsOptional parameter 'a' in method '"
                + "Buggy.Buggy(int)' cannot be of a primitive type.",
            "JsOptional parameter 'c' in method "
                + "'void Buggy.bar(int, Object, String[])' cannot be a varargs parameter.");
  }

  public void testJsOptionalOnNonJsExposedMethodsFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsProperty;",
            "import jsinterop.annotations.JsOptional;",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsOverlay;",
            "public class Buggy {",
            "  public void fun(int a, @JsOptional Object b, @JsOptional String c) {}",
            "  @JsProperty public void bar(@JsOptional Object o) {}",
            "}",
            "@JsType(isNative = true) class Native {",
            "  @JsOverlay public final void fun( @JsOptional Object a) {}",
            "}")
        .assertCompileFails(
            "JsOptional parameter in 'void Buggy.fun(int, Object, "
                + "String)' can only be declared in a JsMethod, a JsConstructor or a JsFunction.",
            "JsOptional parameter in 'void Buggy.bar(Object)' can only "
                + "be declared in a JsMethod, a JsConstructor or a JsFunction.",
            "JsOptional parameter in 'void Native.fun(Object)' can only "
                + "be declared in a JsMethod, a JsConstructor or a JsFunction.");
  }

  private TranspileResult compile(String mainClass, String... source) throws IOException {
    return compile(source(mainClass, source));
  }

  private TranspileResult compile(Source... sources) throws IOException {
    File tempDir = Files.createTempDirectory("interop_checker").toFile();

    File inputDir = new File(tempDir, "input");
    inputDir.mkdir();
    File outputDir = new File(tempDir, "output");
    outputDir.mkdir();
    File packageDir = new File(inputDir, "test");
    packageDir.mkdir();

    for (Source source : sources) {
      Files.write(
          new File(packageDir, source.mainClass + ".java").toPath(),
          source.content,
          Charset.forName("UTF-8"));
    }
    return transpile(getTranspilerArgs(inputDir, outputDir), outputDir);
  }

  private String[] getTranspilerArgs(File inputDir, File outputDir) {
    List<String> argList = new ArrayList<>();

    argList.add("-sourcepath");
    argList.add(inputDir.getAbsolutePath());

    // Output dir
    argList.add("-d");
    argList.add(outputDir.getAbsolutePath());

    // Input source
    List<File> sourceFiles = sourceFiles(inputDir);
    assertFalse(sourceFiles.isEmpty());
    for (File sourceFile : sourceFiles) {
      argList.add(sourceFile.getPath());
    }

    argList.addAll(Arrays.asList("-source", "1.8", "-encoding", "UTF-8", "-cp", JRE_PATH));

    return Iterables.toArray(argList, String.class);
  }

  private static List<File> sourceFiles(File directory) {
    try {
      return Files.walk(directory.toPath())
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".srcjar"))
          .map(Path::toFile)
          .collect(Collectors.toList());
    } catch (IOException e) {
      return null;
    }
  }

  private static class Source {
    String mainClass;
    List<String> content;

    Source(String mainClass, String... code) {
      this.mainClass = mainClass;
      this.content = Lists.newArrayList(code);
      this.content.add(0, "package test;");
    }
  }

  private static Source source(String mainClass, String... code) {
    return new Source(mainClass, code);
  }
}
