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

import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithWasmCustomDescriptorsJsInteropEnabled;
import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithWasmDefaults;

import junit.framework.TestCase;

/** Tests for J2wasm transpilation. */
public final class J2wasmJsInteropRestrictionsCheckerTest extends TestCase {
  public void testNativeJsTypeSucceeds() {
    assertWithInlineMessages(
        "test.MyNative",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class MyNative {
          int primitiveField;
          String stringField;
          MyNative nativeField;
          MyNative(int a, String b, MyNative c) {}
          native MyNative test(int a, String b, MyNative c);
        }
        class MyNonNative {
          @JsMethod
          static native MyNative test(MyNative c);
        }
        class MyNonNative2<T extends MyNative> {
          T field;
          void method(T t) {}
          @JsMethod
          static native <E extends MyNative> void method2(E e);
        }
        class Main {
          void test() {
            // Assignment and casting to null is allowed, even when the null literal is of unknown
            // type.
            MyNative n;
            n = (MyNative) null;
            MyNonNative2<MyNative> r = new MyNonNative2<>();
          }
        }
        """);
  }

  public void testNativeJsTypeInvalidMembersFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        abstract class Buggy {
          C anotherField;
        > Error: Native JsType field 'Buggy.anotherField' cannot be of type 'C'.
          Buggy(C arg) {}
        > Error: Parameter 'arg' in 'Buggy(C arg)' cannot be of type 'C'.
          native void test(Object c);
        > Error: Parameter 'c' in 'void Buggy.test(Object c)' cannot be of type 'Object'.
          native <T> void test2(T c);
        > Error: Parameter 'c' in 'void Buggy.test2(T c)' cannot be of type 'T'.
          native void test3(C c);
        > Error: Parameter 'c' in 'void Buggy.test3(C c)' cannot be of type 'C'.
          abstract void test4(C c);
        > Error: Parameter 'c' in 'void Buggy.test4(C c)' cannot be of type 'C'.
          native C testReturn();
        > Error: Return type of 'C Buggy.testReturn()' cannot be of type 'C'.
          @JsProperty native C getField();
        > Error: Return type of 'C Buggy.getField()' cannot be of type 'C'.
          @JsProperty native void setField(C c);
        > Error: Parameter 'c' in 'void Buggy.setField(C c)' cannot be of type 'C'.
        }
        class C {}
        """);
  }

  public void testNativeMemberFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import jsinterop.annotations.*;
        class Main {
          @JsMethod
          static native void test(Object c);
        > Error: Parameter 'c' in 'void Main.test(Object c)' cannot be of type 'Object'.
          @JsMethod
          static native <T> void test2(T c);
        > Error: Parameter 'c' in 'void Main.test2(T c)' cannot be of type 'T'.
          @JsMethod
          static native void test3(C c);
        > Error: Parameter 'c' in 'void Main.test3(C c)' cannot be of type 'C'.
          @JsMethod
          static native C test4();
        > Error: Return type of 'C Main.test4()' cannot be of type 'C'.
        }
        class C {}
        """);
  }

  public void testNativeJsTypeEqualityFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class Native {}
        class Main {
          void test() {
            Native n = new Native();
            Object o = new Object();
            boolean b = n == o;
        > Error: Native JsType 'Native' cannot be compared with non-native type.
            b = o != n;
        > Error: Native JsType 'Native' cannot be compared with non-native type.

            Native[] arr = new Native[1];
            b = arr == o;
        > Error: Native JsType 'Native[]' cannot be compared with non-native type.
            b = o != arr;
        > Error: Native JsType 'Native[]' cannot be compared with non-native type.
          }
        }
        """);
  }

  public void testNativeTypeStringConcatenationFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class Native {}
        class Main {
          void test(Native n) {
            String s1 = "" + n;
        > Error: Native JsType 'Native' cannot be assigned to 'Object'. (b/262009761)
            s1 += n;
        > Error: Native JsType 'Native' cannot be assigned to 'Object'. (b/262009761)
          }
        }
        """);
  }

  public void testNativeJsTypeInvalidAssignmentsFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class Buggy {}
        @JsType(isNative = true)
        class AlsoBuggy {}
        class Main {
          void test() {
            Object obj = new Buggy();
        > Error: Native JsType 'Buggy' cannot be assigned to 'Object'. (b/262009761)
            obj = new Buggy[1];
        > Error: Native JsType 'Buggy[]' cannot be assigned to 'Object'. (b/262009761)
            passArgument(new AlsoBuggy());
        > Error: Native JsType 'AlsoBuggy' cannot be assigned to 'Object'. (b/262009761)
            passArgument(new AlsoBuggy[1]);
        > Error: Native JsType 'AlsoBuggy[]' cannot be assigned to 'Object'. (b/262009761)
            Object obj2 = (Object) new Buggy();
        > Error: Native JsType 'Buggy' cannot be cast to 'Object'. (b/262009761)
            Buggy b = (Buggy) new Object();
        > Error: 'Object' cannot be cast to Native JsType 'Buggy'. (b/262009761)
            Buggy[] bArr = (Buggy[]) new Object();
        > Error: 'Object' cannot be cast to Native JsType 'Buggy[]'. (b/262009761)
            new Buggy().equals(null);
        > Error: Cannot access member of 'Object' with Native JsType 'Buggy'. (b/262009761)
            new Buggy[1].equals(null);
        > Error: Cannot access member of 'Object' with Native JsType 'Buggy[]'. (b/262009761)
          }
          void passArgument(Object arg) {}
        }
        """);
  }

  public void testNonnativeTypeExtendNativeJsTypeFails() {
    assertWithInlineMessages(
        "test.Subclass",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class Buggy {}
        class Subclass extends Buggy {}
        > Error: Non-native type 'Subclass' cannot extend native JsType 'Buggy'.
        """);
  }

  public void testInstanceOfNativeJsTypeFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class Buggy {}
        @JsType(isNative = true)
        interface BuggyInterface {}
        class Main {
          void test(Object b) {
            if (b instanceof Buggy) {}
        > Error: Cannot do instanceof against native JsType 'Buggy'.
            if (b instanceof BuggyInterface) {}
        > Error: Cannot do instanceof against native JsType interface 'BuggyInterface'.
            if (b instanceof Buggy[]) {}
        > Error: Cannot do instanceof against native JsType 'Buggy[]'.
          }
        }
        """);
  }

  public void testNativeJsTypePatternMatchFails() {
    assertWithInlineMessages(
        "test.Buggy",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class Buggy {}
        @JsType(isNative = true)
        interface BuggyInterface {}

        // TODO(b/466508694): Decide if we can allow these or give a better error message if not.
        record R(BuggyInterface b) {}
        > Error: Native JsType 'BuggyInterface' cannot be assigned to 'Object'. (b/262009761)
        class Main {
          public Main() {
            switch (new Object()) {
             case BuggyInterface b -> {}
        > Error: Cannot pattern match against native JsType interface 'BuggyInterface'.
             case R(BuggyInterface b) -> {}
        > Error: Cannot pattern match against native JsType interface 'BuggyInterface'.
             case R(Object o) -> {}
             case Buggy b -> {}
        > Error: Cannot pattern match against native JsType 'Buggy'.
             case Buggy[] b -> {}
        > Error: Cannot pattern match against native JsType 'Buggy[]'.
             default -> {}
            }
          }
        }
        """);
  }

  public void testNativeJsTypeArraySucceeds() {
    assertWithInlineMessages(
        "test.Main",
        """
        import java.util.List;
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class MyNativeType {}
        public class Main {
          MyNativeType[] myNativeType;
          private static void acceptsNativeTypeArray(MyNativeType[] p) {}
          private static void acceptsNativeTypeVarargs(MyNativeType... p) {}
          private static void acceptsNativeTypeVarargsArray(MyNativeType[]... p) {}
          private static MyNativeType[] returnsNativeTypeArray() { return null; }
          @JsMethod
          private static native MyNativeType[][] nativeBoundary(MyNativeType[] p);
          private static void arrays() {
            MyNativeType[] arr = new MyNativeType[1];
            MyNativeType[][] arr2d = new MyNativeType[1][1];
          }
          private static <T extends MyNativeType> void createsTArray() {
            T[] arrGeneric = null;
          }
        }
        """);
  }

  public void testNativeJsTypeArgumentFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import java.util.ArrayList;
        import java.util.List;
        import java.util.function.Function;
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class MyNativeType {}
        public class Main<T> {
        > Error: Type Main<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
          List<MyNativeType> myNativeType;
        > Error: Type List<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
          List<T> tList;
          T t;
          private static void acceptsNativeTypeList(List<MyNativeType> p) {}
        > Error: Type List<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
          private static void acceptsNativeTypeVarargsList(List<MyNativeType>... p) {}
        > Error: Type List<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
          private static List<MyNativeType> returnsNativeTypeList() { return null; }
        > Error: Type List<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
          private static <T> List<T> returnsTList() { return null; }
          private static <T> T returnsT() { return null; }
          private static <T> void acceptsT(T t) {}
          private static void arrays() {
            Object o = new ArrayList<MyNativeType>();
        > Error: Type ArrayList<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
            List<MyNativeType> arr = null;
        > Error: Type List<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
            o = (List<MyNativeType>) o;
            MyNativeType e = Main.<MyNativeType>returnsTList().get(0);
        > Error: Method List<MyNativeType> Main.returnsTList() cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
        > Error: Type List<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
            e = Main.<MyNativeType>returnsT();
        > Error: Method MyNativeType Main.returnsT() cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
            acceptsT(new MyNativeType());
        > Error: Method void Main.acceptsT(MyNativeType) cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
        > Error: Native JsType 'MyNativeType' cannot be assigned to 'T'. (b/262009761)
            e = new Main<MyNativeType>().tList.get(0);
        > Error: Type Main<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
        > Error: Type List<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
            e = new Main<List<MyNativeType>>().t.get(0);
        > Error: Type List<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
          }
          static class Buggy extends Main<MyNativeType> {}
        > Error: Type Main<MyNativeType> cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
        }
        """);
  }

  public void testNativeJsTypeArrayArgumentFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import java.util.List;
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class MyNativeType {}
        public class Main<T> {
          private static <T> T[] returnsTArray() { return null; }
          private static <T> T returnsT() { return null; }
          T t;
          public void test() {
            List<MyNativeType[]> list = null;
        > Error: Type List<MyNativeType[]> cannot be parameterized with native JsType 'MyNativeType[]'. (b/290992813)
            MyNativeType e = Main.<MyNativeType>returnsTArray()[0];
        > Error: Method MyNativeType[] Main.returnsTArray() cannot be parameterized with native JsType 'MyNativeType'. (b/290992813)
        > Error: Native JsType 'MyNativeType[]' cannot be assigned to 'T[]'. (b/262009761)
            e = Main.<MyNativeType[]>returnsT()[0];
        > Error: Method MyNativeType[] Main.returnsT() cannot be parameterized with native JsType 'MyNativeType[]'. (b/290992813)
        > Error: Native JsType 'MyNativeType[]' cannot be assigned to 'T'. (b/262009761)
            e = new Main<MyNativeType[]>().t[0];
        > Error: Type Main<MyNativeType[]> cannot be parameterized with native JsType 'MyNativeType[]'. (b/290992813)
        > Error: Native JsType 'MyNativeType[]' cannot be assigned to 'T'. (b/262009761)
          }
        }
        """);
  }

  public void testExportedTypePassedToNativeMethodSucceeds() {
    newTesterWithWasmCustomDescriptorsJsInteropEnabled()
        .addCompilationUnit(
            "test.MyNative",
            """
            import jsinterop.annotations.*;
            @JsType
            class MyJsType {
              public void m() {}
            }
            class Main {
              @JsMethod
              static native void acceptJsType(MyJsType jsType);

              @JsMethod
              static native MyJsType returnJsType();

              private static void test() {
                acceptJsType(new MyJsType());
                MyJsType jsType = returnJsType();
              }
            }
            """)
        .assertTranspileSucceeds();
  }

  private void assertWithInlineMessages(String... compilationUnitsAndSources) {
    newTesterWithWasmDefaults().assertWithInlineMessages(compilationUnitsAndSources);
  }
}
