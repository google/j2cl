// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package javaemul.internal;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Provides devirtualized Object methods */
@SuppressWarnings("unusable-by-js")
@JsType(namespace = "vmbootstrap")
class Objects {

  static boolean equals(Object obj, Object other) {
    // Objects: use the custom 'equals' if it exists.
    if (((ObjectLike) obj).hasEquals()) {
      return obj.equals(other);
    }
    assertEqualsAndHashCodePresentIfExpected(obj);

    // Boxed Types: overrides 'equals' but doesn't need special casing as
    // fallback covers them.

    // Array Types: doesn't override 'equals'.

    // Fallback to default j.l.Object#equals behavior (Equality.$same) except we
    // already know 'obj' is not null.
    return is(obj, other);
  }

  @JsMethod(name = "Object.is", namespace = JsPackage.GLOBAL)
  private static native boolean is(Object a, Object b);

  static int hashCode(Object obj) {
    // Objects: use the custom 'hashCode' if it exists.
    if (((ObjectLike) obj).hasHashCode()) {
      return obj.hashCode();
    }
    assertEqualsAndHashCodePresentIfExpected(obj);

    // Boxed Types: overrides 'hashCode'  but doesn't need special casing as
    // fallback covers them.

    // Array Types: doesn't override 'hashCode' so fall back cover them.

    // The fallback to default j.l.Object#hashCode behavior.
    return HashCodes.getIdentityHashCode(obj);
  }

  static String toString(Object obj) {
    return obj.toString();
  }

  static Class<?> getClass(Object obj) {
    // We special case 'getClass' for all types as they all corresspond to
    // different classes.
    switch (JsUtils.typeOf(obj)) {
      case "number":
        return Double.class;
      case "boolean":
        return Boolean.class;
      case "string":
        return String.class;
      case "function":
        return JavaScriptFunction.class;
    }

    if (obj instanceof JavaLangObject) {
      JavaLangObject jlObject = (JavaLangObject) obj;
      return jlObject.getClass();
    } else if (obj instanceof JavaScriptObject[]) {
      // Note that JavaScriptObject[] is top level type for all arrays including primitives so
      // we can handle them in one shot.
      JavaScriptObject[] array = (JavaScriptObject[]) obj;
      return arrayGetClass(array);
    } else if (obj != null) {
      // Do not need to check existence of 'getClass' since j.l.Object#getClass
      // is final and all native types map to a single special class.
      return JavaScriptObject.class;
    }

    // Explicitly throw TypeError instead of relying on null-dereference since JsCompiler
    // sometimes optimizes that away. Alternatively we can throw Java NPE that is backed by
    // TypeError but it adds too much boilerplate which messes up size reports
    return throwTypeError();
  }

  @JsMethod
  private static native Class<?> throwTypeError();

  @JsMethod
  private static native void assertEqualsAndHashCodePresentIfExpected(Object value);

  @JsType(isNative = true, name = "*", namespace = JsPackage.GLOBAL)
  private interface ObjectLike {
    // Expose methods as properties so we can do if check for their existence above.

    @JsProperty(name = "hashCode")
    boolean hasHashCode();

    @JsProperty(name = "equals")
    boolean hasEquals();
  }

  @JsMethod(name = "$getClass", namespace = "vmbootstrap.Arrays")
  private static native Class<?> arrayGetClass(Object[] a);

  @JsType(isNative = true, name = "Object$impl", namespace = "java.lang")
  private static class JavaLangObject {}

  @JsFunction
  private interface JavaScriptFunction {
    void fn();
  }

  @JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
  private static class JavaScriptObject {}
}
