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
package jstype;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType
public class SomeJsType<T> {
  public int publicField;
  private int privateField;
  int packageField;
  protected int protectedField;

  public void publicMethod() {};

  private void privateMethod() {};

  void packageMethod() {};

  protected void protectedMethod() {};

  public void useFieldsAndMethods() {
    @SuppressWarnings("unused")
    int value = publicField + privateField + packageField + protectedField;
    publicMethod();
    privateMethod();
    packageMethod();
    protectedMethod();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface Star {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface Wildcard {}

  @Wasm("nop") // TODO(b/262009761): Casts between Object and JsTypes not supported in Wasm.
  private Wildcard testStarAndWildCard(Star s, Wildcard w) {
    Object object = new Object();

    Star star = (Star) (Object) 3.0;
    return (Wildcard) star;
  }
}
