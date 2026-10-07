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
        @JsFunction
        interface MyJsFunction {
          void run();
        }
        @JsType(isNative = true)
        class MyNative {
          int primitiveField;
          String stringField;
          Double doubleField;
          Object objectField;
          Boolean booleanField;
          Long longField;
          MyNative nativeField;
          MyJsFunction jsFunctionField;
          MyNative(int a, String b, Double d, Object o, Boolean bool, Long l, MyNative c, MyJsFunction f) {}
          native MyNative test(int a, String b, Double d, Object o, Boolean bool, Long l, MyNative c, MyJsFunction f);
        }
        class MyNonNative {
          @JsMethod
          static native MyNative test(MyNative c);
          @JsMethod
          static native MyJsFunction testJsFunction(MyJsFunction f);
        }
        class MyNonNative2<T extends MyNative> {
          T field;
          void method(T t) {}
          @JsMethod
          static native <E extends MyNative> void method2(E e);
        }
        class MyNonNative3 {
          @JsMethod
          static native <T> T method1(T t);
          @JsMethod
          static native <T extends String> T method2(T t);
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
          native <T extends C> void test2(T c);
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
          static native <T extends C> void test2(T c);
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

  public void testNativeJsTypeEqualitySucceeds() {
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
            b = o != n;

            Native[] arr = new Native[1];
            b = arr == o;
            b = o != arr;
          }
        }
        """);
  }

  public void testNativeTypeStringConcatenationSucceeds() {
    assertWithInlineMessages(
        "test.Main",
        """
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class Native {}
        class Main {
          void test(Native n) {
            String s1 = "" + n;
            s1 += n;
          }
        }
        """);
  }

  public void testNativeJsTypeAssignmentsSucceeds() {
    assertWithInlineMessages(
        "test.Main",
        """
        import java.io.Serializable;
        import jsinterop.annotations.*;
        @JsType(isNative = true)
        class Buggy {}
        @JsType(isNative = true)
        class AlsoBuggy {}
        @JsType(isNative = true)
        interface NativeInterface {}
        interface NonNativeInterface {}
        class Main {
          void test() {
            Object obj = new Buggy();
            obj = new Buggy[1];
            Cloneable cloneable = new Buggy[1];
            Serializable serializable = new Buggy[1];
            passArgument(new AlsoBuggy());
            passArgument(new AlsoBuggy[1]);
            Object obj2 = (Object) new Buggy();
            Buggy b = (Buggy) new Object();
            Buggy[] bArr = (Buggy[]) new Object();
            NonNativeInterface nonNativeIntf = (NonNativeInterface) new Buggy();
            NativeInterface nativeIntf = (NativeInterface) new Main();
            Main m = (Main) nativeIntf;
            new Buggy().equals(null);
            new Buggy[1].equals(null);
          }
          void passArgument(Object obj) {}
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

        record R1(Buggy b) {}
        record R2(BuggyInterface b) {}
        class Main {
          public Main() {
            switch (new Object()) {
             case Buggy b -> {}
        > Error: Cannot pattern match against native JsType 'Buggy'.
             case Buggy[] b -> {}
        > Error: Cannot pattern match against native JsType 'Buggy[]'.
             case BuggyInterface b -> {}
        > Error: Cannot pattern match against native JsType interface 'BuggyInterface'.
             case R1(Buggy b) -> {}
        > Error: Cannot pattern match against native JsType 'Buggy'.
             case R2(BuggyInterface b) -> {}
        > Error: Cannot pattern match against native JsType interface 'BuggyInterface'.
             case R1(Object o) -> {}
             case R2(Object o) -> {}
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

  public void testNativeJsTypeArgumentSucceeds() {
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
          List<MyNativeType> myNativeType;
          List<T> tList;
          T t;
          private static void acceptsNativeTypeList(List<MyNativeType> p) {}
          private static void acceptsNativeTypeVarargsList(List<MyNativeType>... p) {}
          private static List<MyNativeType> returnsNativeTypeList() { return null; }
          private static <T> List<T> returnsTList() { return null; }
          private static <T> T returnsT() { return null; }
          private static <T> void acceptsT(T t) {}
          private static void arrays() {
            Object o = new ArrayList<MyNativeType>();
            List<MyNativeType> arr = null;
            o = (List<MyNativeType>) o;
            MyNativeType e = Main.<MyNativeType>returnsTList().get(0);
            e = Main.<MyNativeType>returnsT();
            acceptsT(new MyNativeType());
            e = new Main<MyNativeType>().tList.get(0);
            e = new Main<List<MyNativeType>>().t.get(0);
          }
          static class Buggy extends Main<MyNativeType> {}
        }
        """);
  }

  public void testNativeJsTypeArrayArgumentSucceeds() {
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
            MyNativeType e = Main.<MyNativeType>returnsTArray()[0];
            e = Main.<MyNativeType[]>returnsT()[0];
            e = new Main<MyNativeType[]>().t[0];
          }
        }
        """);
  }

  public void testExportedTypePassedToNativeMethodFails() {
    assertWithInlineMessages(
        "test.Main",
        """
        import jsinterop.annotations.*;
        @JsType
        class MyJsType {
          public void m() {}
        }
        class Main {
          @JsMethod
          static native void acceptJsType(MyJsType jsType);
        > Error: Parameter 'jsType' in 'void Main.acceptJsType(MyJsType jsType)' cannot be of type 'MyJsType'.
          @JsMethod
          static native MyJsType returnJsType();
        > Error: Return type of 'MyJsType Main.returnJsType()' cannot be of type 'MyJsType'.
        }
        """);
  }

  public void testExportedTypePassedToNativeMethodWithCustomDescriptorsSucceeds() {
    newTesterWithWasmCustomDescriptorsJsInteropEnabled()
        .addCompilationUnit(
            "test.MyNative",
            """
            import jsinterop.annotations.*;
            class MyType {}
            interface MyInterface {}
            class Main {
              @JsMethod
              static native void acceptType(MyType type);

              @JsMethod
              static native MyType returnType();

              @JsMethod
              static native void acceptInterface(MyInterface intf);

              @JsMethod
              static native MyInterface returnInterface();

              private static void test() {
                acceptType(new MyType());
                MyType type = returnType();
                acceptInterface(new MyInterface() {});
                MyInterface intf = returnInterface();
              }
            }
            """)
        .assertTranspileSucceeds();
  }

  private void assertWithInlineMessages(String... compilationUnitsAndSources) {
    newTesterWithWasmDefaults().assertWithInlineMessages(compilationUnitsAndSources);
  }
}
