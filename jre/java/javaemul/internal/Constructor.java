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

import javaemul.internal.annotations.HasNoSideEffects;
import javaemul.internal.annotations.UncheckedCast;
import jsinterop.annotations.JsFunction;
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

  /** A function that supplies a value. */
  @JsFunction
  public interface Supplier<T> {
    T get();
  }

  @HasNoSideEffects
  @UncheckedCast
  @JsOverlay
  public final <T> T cache(String key, Supplier<T> supplier) {
    if (hasOwnProperty(this.prototype, key)) {
      return JsUtils.getProperty(this.prototype, key);
    } else {
      T t = supplier.get();
      JsUtils.setProperty(this.prototype, key, t);
      return t;
    }
  }

  @JsMethod(name = "Object.prototype.hasOwnProperty.call", namespace = JsPackage.GLOBAL)
  private static native boolean hasOwnProperty(Object obj, String name);

  @JsOverlay
  public final Constructor getSuperConstructor() {
    Constructor parentCtor = Constructor.of(getPrototypeOf(this.prototype));
    return parentCtor == globalObjectCtor ? null : parentCtor;
  }

  @JsMethod(name = "Object.getPrototypeOf", namespace = JsPackage.GLOBAL)
  private static native Object getPrototypeOf(Object obj);

  @JsOverlay
  public final boolean isInterface() {
    return Util.$extractClassType(this) == Util.TYPE_INTERFACE;
  }

  @JsOverlay
  public final boolean isEnum() {
    return Util.$extractClassType(this) == Util.TYPE_ENUM;
  }

  @JsOverlay
  public final boolean isPrimitive() {
    return Util.$extractClassType(this) == Util.TYPE_PRIMITIVE;
  }

  @JsOverlay
  public final String getPrimitiveShortName() {
    return Util.$extractPrimitiveShortName(this);
  }

  @JsOverlay
  public final String getClassName() {
    return Util.$extractClassName(this);
  }

  @JsOverlay
  public final boolean isPrimitiveType() {
    return Util.$isPrimitiveType(this);
  }

  @JsOverlay
  public final Constructor getBoxedConstructor() {
    return Util.$getBoxedConstructor(this);
  }

  @JsOverlay
  public final Constructor getPrimitiveConstructor() {
    return Util.$getPrimitiveConstructor(this);
  }

  @JsType(isNative = true, namespace = "nativebootstrap")
  private static class Util {
    public static int TYPE_ENUM;
    public static int TYPE_INTERFACE;
    public static int TYPE_PRIMITIVE;

    public static native String $extractClassName(Constructor ctor);

    public static native String $extractPrimitiveShortName(Constructor ctor);

    public static native int $extractClassType(Constructor ctor);

    public static native boolean $isPrimitiveType(Constructor ctor);

    public static native Constructor $getPrimitiveConstructor(Constructor ctor);

    public static native Constructor $getBoxedConstructor(Constructor ctor);
  }
}
