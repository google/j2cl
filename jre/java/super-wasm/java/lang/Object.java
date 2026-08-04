/*
 * Copyright 2015 Google Inc.
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
package java.lang;

import javaemul.internal.HashCodes;
import javaemul.internal.JsFunctionAdaptor;
import javaemul.internal.JsObject;
import javaemul.internal.WasmAny;
import javaemul.internal.WasmExtern;
import jsinterop.annotations.JsMethod;

/**
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Object.html">the official Java API
 * doc</a> for details.
 */
public class Object {

  public int $systemIdentityHashCode; // only used in wasm to store identity hash code.

  public boolean equals(Object that) {
    return this == that;
  }

  public int hashCode() {
    return HashCodes.getObjectIdentityHashCode(this);
  }

  public String toString() {
    return getClass().getName() + "@" + Integer.toHexString(hashCode());
  }

  public final Class<?> getClass() {
    return $getClassImpl();
  }

  // Stub method. Replaced by the compiler.
  public Class<?> $getClassImpl() {
    return null;
  }

  static WasmExtern toJs(Object obj) {
    return switch (obj) {
      case null -> null;
      case String s -> (WasmExtern) String.toJsString(s);
      case Double d -> (WasmExtern) Double.toJs(d);
      case Boolean b -> (WasmExtern) Boolean.toJs(b);
      case Long l -> (WasmExtern) Long.toJs(l);
      case JsFunctionAdaptor f -> JsFunctionAdaptor.toJs(f);
      case JsObject o -> o.getExtern();
      default -> WasmExtern.convertToExtern(obj);
    };
  }

  static Object fromJs(WasmExtern extern) {
    if (extern == null) {
      return null;
    }
    return switch (typeOf(extern)) {
      case 0 -> null;
      case 1 -> String.fromJs(extern);
      case 2 -> Boolean.fromJs((Boolean.NativeBoolean) extern);
      case 3 -> Double.fromJs((Double.NativeNumber) extern);
      case 4 -> Long.fromJs((Long.NativeLong) extern);
      case 5 -> JsFunctionAdaptor.fromJs(extern);
      default -> {
        WasmAny any = WasmExtern.convertToAny(extern);
        if (any instanceof Object) {
          yield WasmExtern.convertToJava(any);
        }
        yield new JsObject(extern);
      }
    };
  }

  @JsMethod(namespace = "j2wasm.JsInteropRuntime")
  private static native int typeOf(WasmExtern obj);
}
