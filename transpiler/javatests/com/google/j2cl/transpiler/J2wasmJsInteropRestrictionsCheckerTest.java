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

import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithDefaultsWasm;

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
            "Cannot access member of 'Object' with native JsType 'Buggy'. (b/288128177)");
  }

  @CanIgnoreReturnValue
  private TranspileResult assertTranspileSucceeds(String compilationUnitName, String... code) {
    return newTesterWithDefaultsWasm()
        .addCompilationUnit(compilationUnitName, code)
        .assertTranspileSucceeds();
  }

  private TranspileResult assertTranspileFails(String compilationUnitName, String... code) {
    return newTesterWithDefaultsWasm()
        .addCompilationUnit(compilationUnitName, code)
        .assertTranspileFails();
  }
}
