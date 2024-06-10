/*
 * Copyright 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler;

import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithWasmDefaults;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.transpiler.TranspilerTester.TranspileResult;
import junit.framework.TestCase;

/** Tests for J2wasm transpilation. */
public final class J2wasmJsInteropRestrictionsCheckerTest extends TestCase {
  public void testNativeJsTypeSucceeds() {
    assertTranspileSucceeds(
        "test.MyNative",
        "import jsinterop.annotations.*;",
        "@JsType(isNative = true)",
        "class MyNative {",
        "  int primitiveField;",
        "  String stringField;",
        "  C nativeField;",
        "  MyNative(int a, String b, C c) {}",
        "  native C test(int a, String b, C c);",
        // TODO(b/290267878): Uncomment when this case is correct.
        // "  native <T extends MyNative> void test2(T t);",
        "}",
        "class MyNonNative {",
        "  @JsMethod",
        "  static native C test(C c);",
        "}",
        "@JsType(isNative = true)",
        "class C {}");
  }

  public void testNativeJsTypeInvalidMembersFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "abstract class Buggy {",
            "  C anotherField;",
            "  Buggy(C arg) {}",
            "  native void test(Object c);",
            "  native <T> void test2(T c);",
            "  native void test3(C c);",
            "  abstract void test4(C c);",
            "  native C testReturn();",
            "  @JsProperty native C getField();",
            "  @JsProperty native void setField(C c);",
            "}",
            "class C {}")
        .assertErrorsWithoutSourcePosition(
            "Native JsType field 'Buggy.anotherField' cannot be of type 'C'.",
            "Parameter 'arg' in 'Buggy(C arg)' cannot be of type 'C'.",
            "Parameter 'c' in 'void Buggy.test(Object c)' cannot be of type 'Object'.",
            "Parameter 'c' in 'void Buggy.test2(T c)' cannot be of type 'T'.",
            "Parameter 'c' in 'void Buggy.test3(C c)' cannot be of type 'C'.",
            "Parameter 'c' in 'void Buggy.test4(C c)' cannot be of type 'C'.",
            "Return type of 'C Buggy.testReturn()' cannot be of type 'C'.",
            "Return type of 'C Buggy.getField()' cannot be of type 'C'.",
            "Parameter 'c' in 'void Buggy.setField(C c)' cannot be of type 'C'.");
  }

  public void testNativeMemberFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "class Main {",
            "  @JsMethod",
            "  static native void test(Object c);",
            "  @JsMethod",
            "  static native <T> void test2(T c);",
            "  @JsMethod",
            "  static native void test3(C c);",
            "  @JsMethod",
            "  static native C test4();",
            "}",
            "class C {}")
        .assertErrorsWithoutSourcePosition(
            "Parameter 'c' in 'void Main.test(Object c)' cannot be of type 'Object'.",
            "Parameter 'c' in 'void Main.test2(T c)' cannot be of type 'T'.",
            "Parameter 'c' in 'void Main.test3(C c)' cannot be of type 'C'.",
            "Return type of 'C Main.test4()' cannot be of type 'C'.");
  }

  public void testNativeJsTypeInvalidAssignmentsFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "class Buggy {}",
            "@JsType(isNative = true)",
            "class AlsoBuggy {}",
            "class Main {",
            "  void test() {",
            "    Object obj = new Buggy();",
            "    passArgument(new AlsoBuggy());",
            "    Object obj2 = (Object) new Buggy();",
            "    Buggy b = (Buggy) new Object();",
            "    new Buggy().equals(null);",
            "  }",
            "  void passArgument(Object arg) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Native JsType 'Buggy' cannot be assigned to 'Object'. (b/262009761)",
            "Native JsType 'AlsoBuggy' cannot be assigned to 'Object'. (b/262009761)",
            "Native JsType 'Buggy' cannot be cast to 'Object'. (b/262009761)",
            "Native JsType 'Object' cannot be cast to 'Buggy'. (b/262009761)",
            "Cannot access member of 'Object' with Native JsType 'Buggy'. (b/262009761)");
  }

  public void testNonnativeTypeExtendNativeJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "class Buggy {}",
            "class Subclass extends Buggy {}")
        .assertErrorsWithoutSourcePosition(
            "Non-native type 'Subclass' cannot extend native JsType 'Buggy'.");
  }

  public void testInstanceOfNativeJsTypeFails() {
    assertTranspileFails(
            "test.Buggy",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "class Buggy {}",
            "@JsType(isNative = true)",
            "interface BuggyInterface {}",
            "class Main {",
            "  void test(Object b) {",
            "    if (b instanceof Buggy) {}",
            "    if (b instanceof BuggyInterface) {}",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Cannot do instanceof against native JsType 'Buggy'.",
            "Cannot do instanceof against native JsType interface 'BuggyInterface'.");
  }

  public void testNativeJsTypeArrayFails() {
    assertTranspileFails(
            "test.Main",
            "import java.util.List;",
            "import java.util.function.Function;",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "class MyNativeType {}",
            "public class Main<T> {",
            "  MyNativeType[] myNativeType;",
            "  private static void acceptsNativeTypeArray(MyNativeType[] p) {}",
            "  private static void acceptsNativeTypeVarargs(MyNativeType... p) {}",
            "  private static void acceptsNativeTypeVarargsArray(MyNativeType[]... p) {}",
            "  private static MyNativeType[] returnsNativeTypeArray() { return null; }",
            "  private static <T> T[] returnsTArray() { return null; }",
            "  private static <T> T returnsT() { return null; }",
            "  T t;",
            "  private static void arrays() {",
            "    Object o = new MyNativeType[1];",
            "    MyNativeType[] arr = null;",
            "    List<MyNativeType[]> list = null;",
            "    o = (MyNativeType[]) o;",
            "    if (o instanceof MyNativeType[]) {}",
            "    MyNativeType e = Main.<MyNativeType>returnsTArray()[0];",
            "    e = Main.<MyNativeType[]>returnsT()[0];",
            "    e = new Main<MyNativeType[]>().t[0];",
            "  }",
            "  private static <T extends MyNativeType> void createsTArray() {",
            "     T[] arrGeneric = null;",
            "  }",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Field 'Main<T>.myNativeType' cannot be of type 'MyNativeType[]'. (b/261079024)",
            "Parameter 'p' in 'void Main.acceptsNativeTypeArray(MyNativeType[] p)' cannot be of"
                + " type 'MyNativeType[]'. (b/261079024)",
            "Parameter 'p' in 'void Main.acceptsNativeTypeVarargs(MyNativeType... p)' cannot be of"
                + " type 'MyNativeType[]'. (b/261079024)",
            "Parameter 'p' in 'void Main.acceptsNativeTypeVarargsArray(MyNativeType[]... p)' cannot"
                + " be of type 'MyNativeType[][]'. (b/261079024)",
            "Return type of 'MyNativeType[] Main.returnsNativeTypeArray()' cannot be of type"
                + " 'MyNativeType[]'. (b/261079024)",
            "Array creation 'new MyNativeType[1]' cannot be of type 'MyNativeType[]'."
                + " (b/261079024)",
            "Variable 'arr' cannot be of type 'MyNativeType[]'. (b/261079024)",
            "Variable 'list' cannot be of type 'List<MyNativeType[]>'. (b/261079024)",
            "Cannot cast to Native type array 'MyNativeType[]'. (b/261079024)",
            "Cannot do instanceof against Native type array 'MyNativeType[]'. (b/261079024)",
            "Returned type in call to method 'MyNativeType[] Main.returnsTArray()'"
                + " cannot be of type 'MyNativeType[]'. (b/261079024)",
            "Returned type in call to method 'MyNativeType[] Main.returnsT()' cannot be of type"
                + " 'MyNativeType[]'. (b/261079024)",
            "Object creation 'new Main.<init>()' cannot be of type 'Main<MyNativeType[]>'."
                + " (b/261079024)",
            "Reference to field 'Main<MyNativeType[]>.t' cannot be of type 'MyNativeType[]'."
                + " (b/261079024)",
            "Variable 'arrGeneric' cannot be of type 'T[]'. (b/261079024)");
  }

  public void testNativeJsTypeArgumentFails() {
    assertTranspileFails(
            "test.Main",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "import java.util.function.Function;",
            "import jsinterop.annotations.*;",
            "@JsType(isNative = true)",
            "class MyNativeType {}",
            "public class Main<T> {",
            "  List<MyNativeType> myNativeType;",
            "  List<T> tList;",
            "  T t;",
            "  private static void acceptsNativeTypeList(List<MyNativeType> p) {}",
            "  private static void acceptsNativeTypeVarargsList(List<MyNativeType>... p) {}",
            "  private static List<MyNativeType> returnsNativeTypeList() { return null; }",
            "  private static <T> List<T> returnsTList() { return null; }",
            "  private static <T> T returnsT() { return null; }",
            "  private static <T> void acceptsT(T t) {}",
            "  private static void arrays() {",
            "    Object o = new ArrayList<MyNativeType>();",
            "    List<MyNativeType> arr = null;",
            "    o = (List<MyNativeType>) o;",
            "    MyNativeType e = Main.<MyNativeType>returnsTList().get(0);",
            "    e = Main.<MyNativeType>returnsT();",
            "    acceptsT(new MyNativeType());",
            "    e = new Main<MyNativeType>().tList.get(0);",
            "    e = new Main<List<MyNativeType>>().t.get(0);",
            "  }",
            "  static class Buggy extends Main<MyNativeType> {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Field 'Main<T>.myNativeType' cannot be of type 'List<MyNativeType>'. (b/290992813)",
            "Parameter 'p' in 'void Main.acceptsNativeTypeList(List<MyNativeType> p)' cannot be of"
                + " type 'List<MyNativeType>'. (b/290992813)",
            "Return type of 'List<MyNativeType> Main.returnsNativeTypeList()' cannot be of type"
                + " 'List<MyNativeType>'. (b/290992813)",
            "Object creation 'new ArrayList.<init>()' cannot be of type 'ArrayList<MyNativeType>'."
                + " (b/290992813)",
            "Variable 'arr' cannot be of type 'List<MyNativeType>'. (b/290992813)",
            "Cannot cast to type with Native type argument 'List<MyNativeType>'. (b/290992813)",
            "Returned type in call to method 'List<MyNativeType> Main.returnsTList()'"
                + " cannot be of type 'List<MyNativeType>'. (b/290992813)",
            "Returned type in call to method 'MyNativeType Main.returnsT()' cannot be of type"
                + " 'MyNativeType'. (b/290992813)",
            "Native JsType 'MyNativeType' cannot be assigned to 'T'. (b/262009761)",
            "Object creation 'new Main.<init>()' cannot be of type 'Main<MyNativeType>'."
                + " (b/290992813)",
            "Object creation 'new Main.<init>()' cannot be of type 'Main<List<MyNativeType>>'."
                + " (b/290992813)",
            "Returned type in call to method 'MyNativeType List.get(int)' cannot be of type"
                + " 'MyNativeType'. (b/290992813)",
            "Reference to field 'Main<MyNativeType>.tList' cannot be of type 'List<MyNativeType>'."
                + " (b/290992813)",
            "Reference to field 'Main<List<MyNativeType>>.t' cannot be of type"
                + " 'List<MyNativeType>'. (b/290992813)",
            "Supertype of 'Buggy' cannot be of type 'Main<MyNativeType>'. (b/290992813)");
  }

  @CanIgnoreReturnValue
  private TranspileResult assertTranspileSucceeds(String compilationUnitName, String... code) {
    return newTesterWithWasmDefaults()
        .addCompilationUnit(compilationUnitName, code)
        .assertTranspileSucceeds();
  }

  private TranspileResult assertTranspileFails(String compilationUnitName, String... code) {
    return newTesterWithWasmDefaults()
        .addCompilationUnit(compilationUnitName, code)
        .assertTranspileFails();
  }
}
