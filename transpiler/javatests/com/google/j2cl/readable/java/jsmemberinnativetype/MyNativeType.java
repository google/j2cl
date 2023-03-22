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
package jsmemberinnativetype;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "test.foo")
public class MyNativeType {
  static int staticField;

  @JsProperty
  public static native int getStaticField();

  @JsProperty
  public static native void setStaticField(int value);

  public int publicField;
  private int privateField;
  int packageField;
  protected int protectedField;

  @JsProperty
  public native int getPublicField();

  @JsProperty
  public native void setPublicField(int value);

  public native void publicMethod();

  private native void privateMethod();

  native void packageMethod();

  protected native void protectedMethod();

  @JsOverlay
  public final void useFieldsAndMethods() {
    int jsProperties = publicField + privateField + packageField + protectedField + staticField;

    int jsPropertyMethods = getPublicField() + getStaticField();
    setPublicField(1);
    setStaticField(2);

    publicMethod();
    privateMethod();
    packageMethod();
    protectedMethod();
  }
}
