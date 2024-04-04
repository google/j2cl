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
package javaemul.internal;

import javaemul.internal.ThrowableUtils.JsObject;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(namespace = "vmbootstrap")
class Exceptions {

  /**
   * A try with resource block uses safeClose to close resources that have been opened. If an
   * exception occurred during the resource initialization or in the try block, it is passed in here
   * so that we can add suppressed exceptions to it if the resource fails to close.
   */
  public static Throwable safeClose(
      @SuppressWarnings("unusable-by-js") AutoCloseable resource, Throwable currentException) {
    if (resource == null) {
      return currentException;
    }
    try {
      resource.close();
    } catch (Throwable t) {
      if (currentException == null) {
        return t;
      }
      currentException.addSuppressed(t);
    }
    return currentException;
  }

  @JsMethod(name = "Error", namespace = JsPackage.GLOBAL)
  public static native JsObject createJsError(String errorMessage);

  public static Throwable toJava(JsObject e) {
    return Throwable.of(e);
  }

  public static JsObject toJs(Throwable t) {
    return t.getBackingJsObject();
  }

  // TODO(goktug): Remove this method and super-source when we can throw directly from Wasm.
  @JsMethod(name = "throwException", namespace = "j2wasm.ExceptionUtils")
  public static native void throwJsError(JsObject object);
}
