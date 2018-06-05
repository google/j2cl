/*
 * Copyright 2018 Google Inc.
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

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Abstract a JavaScript constructor function. */
@JsType(isNative = true)
public class Constructor {

  @JsOverlay
  public static Constructor of(Object obj) {
    return JsUtils.getProperty(obj, "constructor");
  }

  @JsProperty(name = "Object", namespace = JsPackage.GLOBAL)
  private static Constructor globalObjectCtor;

  private Object prototype;

  @JsOverlay
  public final Constructor getSuperConstructor() {
    Constructor parentCtor = Constructor.of(getPrototypeOf(this.prototype));
    return parentCtor == globalObjectCtor ? null : parentCtor;
  }

  @JsMethod(name = "Object.getPrototypeOf", namespace = JsPackage.GLOBAL)
  private static native Object getPrototypeOf(Object obj);
}
