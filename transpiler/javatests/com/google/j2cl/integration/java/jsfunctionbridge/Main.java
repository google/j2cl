/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jsfunctionbridge;

import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.assertUnderlyingTypeEquals;
import static com.google.j2cl.integration.testing.TestUtils.isJavaScript;

import javaemul.internal.annotations.DoNotAutobox;
import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsFunction;

public class Main {
  public static void main(String... args) {
    testBridge();
    testBridge_parameterChecks();
    testBridge_jsEnumBoxing();
    testBridge_nativeReturnType();
  }

  private static final ApplyFunction<String> FOO =
      new ApplyFunction<String>() {
        public String field = "a";

        @Override
        public String apply(String s) {
          return s + field + fun();
        }

        public String fun() {
          return field + "d";
        }
      };

  private static void testBridge() {
    assertTrue(("eaad".equals(callGeneric(FOO, "e"))));
    assertTrue(("eaad".equals(callParametric(FOO, "e"))));
    assertTrue(("eaad".equals(FOO.apply("e"))));
    assertTrue("hello".equals(new Identity().apply("hello")));
  }

  private static void testBridge_parameterChecks() {
    assertThrowsClassCastException(() -> callGeneric(FOO, new Object()));
  }

  private static void testBridge_jsEnumBoxing() {
    // Lambda, method reference and anonymous class implementing a parametric @JsFunction
    // specialized to a non-native JsEnum, so the bridge for apply(Object) unboxes the argument and
    // boxes the result.
    checkApplyFunctionBridge(e -> nextPlainJsEnum(e));
    checkApplyFunctionBridge(Main::nextPlainJsEnum);
    checkApplyFunctionBridge(
        new ApplyFunction<PlainJsEnum>() {
          @Override
          public PlainJsEnum apply(PlainJsEnum e) {
            return nextPlainJsEnum(e);
          }
        });

    // Lambda and anonymous class implementing an Object-returning @JsFunction returning a
    // non-native JsEnum, so the bridge for get() boxes the result.
    checkObjectSupplierBridge(() -> PlainJsEnum.ONE);
    checkObjectSupplierBridge(
        new ObjectSupplier() {
          @Override
          public PlainJsEnum get() {
            return PlainJsEnum.ONE;
          }
        });

    checkBridge_jsEnumTypeVariable(PlainJsEnum.ONE);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void checkApplyFunctionBridge(ApplyFunction<PlainJsEnum> f) {
    assertTrue(f.apply(PlainJsEnum.ONE) == PlainJsEnum.TWO);
    assertUnboxedPlainJsEnum(f.apply(PlainJsEnum.ONE));
    ApplyFunction rawF = f;
    assertTrue(rawF.apply(PlainJsEnum.ONE) == PlainJsEnum.TWO);
    assertBoxedPlainJsEnum(rawF.apply(PlainJsEnum.ONE));
    assertThrowsClassCastException(() -> rawF.apply(OtherPlainJsEnum.UNIT));
  }

  private static void checkObjectSupplierBridge(ObjectSupplier s) {
    assertTrue(s.get() == PlainJsEnum.ONE);
    assertBoxedPlainJsEnum(s.get());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <T extends PlainJsEnum> void checkBridge_jsEnumTypeVariable(T t) {
    // Lambda implementing a parametric @JsFunction specialized to a JsEnum-bounded type variable.
    ApplyFunction<T> f =
        x -> {
          assertUnboxedPlainJsEnum(x);
          return x;
        };
    assertTrue(f.apply(t) == t);
    assertUnboxedPlainJsEnum(f.apply(t));
    ApplyFunction rawF = f;
    assertBoxedPlainJsEnum(rawF.apply(t));
    assertThrowsClassCastException(() -> rawF.apply(OtherPlainJsEnum.UNIT));
  }

  private static void testBridge_nativeReturnType() {
    // Lambda implementing a parametric @JsFunction specialized to a native JsEnum.
    // TODO(b/578707648): Uncomment when the Wasm bridge return conversion is fixed.
    // ApplyFunction<NativeEnum> nativeJsEnumFunction =
    //     e -> e == NativeEnum.ACCEPT ? NativeEnum.CANCEL : e;
    // assertTrue(nativeJsEnumFunction.apply(NativeEnum.ACCEPT) == NativeEnum.CANCEL);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Object callGeneric(ApplyFunction af, Object o) {
    return af.apply(o);
  }

  private static String callParametric(ApplyFunction<String> af, String s) {
    return af.apply(s);
  }

  @JsFunction
  interface ApplyFunction<T> {
    T apply(T element);
  }

  private static final class Identity implements ApplyFunction<Object> {
    @Override
    public Object apply(Object element) {
      return element;
    }
  }

  @JsEnum
  enum PlainJsEnum {
    ONE,
    TWO
  }

  @JsEnum
  enum OtherPlainJsEnum {
    UNIT
  }

  @JsEnum(isNative = true, namespace = "jsfunctionbridge")
  enum NativeEnum {
    ACCEPT,
    CANCEL
  }

  @JsFunction
  interface ObjectSupplier {
    Object get();
  }

  private static PlainJsEnum nextPlainJsEnum(PlainJsEnum e) {
    assertUnboxedPlainJsEnum(e);
    return e == PlainJsEnum.ONE ? PlainJsEnum.TWO : e;
  }

  private static void assertUnboxedPlainJsEnum(@DoNotAutobox Object o) {
    if (isJavaScript()) {
      // JsEnums are only unboxed in JavaScript.
      assertUnderlyingTypeEquals(Double.class, o);
    }
  }

  private static void assertBoxedPlainJsEnum(@DoNotAutobox Object o) {
    if (isJavaScript()) {
      // JsEnums are only boxed in JavaScript.
      assertUnderlyingTypeEquals(PlainJsEnum.class, o);
    }
  }
}
