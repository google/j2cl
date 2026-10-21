/*
 * Copyright 2015 Google Inc.
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
package jsinteroptests;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

import java.util.Arrays;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.Nullable;

/** Tests JsMethod functionality. */
public class JsMethodTest {
  public static void testAll() {
    testLambdaRequiringJsMethodBridge();
    testJsOptionalJsVarargsLambda();
  }

  interface NullSupplier {
    default Object get() {
      return null;
    }
  }

  interface JsSupplier extends NullSupplier {
    @JsMethod
    @Override
    Object get();
  }

  private static void testLambdaRequiringJsMethodBridge() {
    JsSupplier aSupplier = () -> "Hello";
    NullSupplier aliasToSupplier = aSupplier;
    assertEquals("Hello", aSupplier.get());
    assertEquals("Hello", aliasToSupplier.get());
  }

  interface FunctionalInterfaceWithJsVarargsAndJsOptionalJsMethod<T extends Number> {
    @JsMethod
    double sum(@JsOptional @Nullable T first, T... rest);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface NativeSum {
    @JsMethod
    double sum();
  }

  private static void testJsOptionalJsVarargsLambda() {
    FunctionalInterfaceWithJsVarargsAndJsOptionalJsMethod<Double> f =
        (first, rest) ->
            (first != null ? first : 0)
                + Arrays.stream(rest).mapToDouble(Double::doubleValue).sum();

    // Call lambda with no parameters.
    assertEquals(0, ((NativeSum) f).sum());

    // Call lambda with wrong type of array. This is still OK since the method is a JsVarargs hence
    // the array is not passed directly in JavaScript but recreated by the JsVarargs prologue.
    FunctionalInterfaceWithJsVarargsAndJsOptionalJsMethod rawF = f;
    assertEquals(6, rawF.sum(1.0d, new Number[] {2.0d, 3.0d}));
  }
}
