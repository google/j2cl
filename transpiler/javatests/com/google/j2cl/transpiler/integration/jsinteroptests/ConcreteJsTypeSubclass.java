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
package com.google.j2cl.transpiler.integration.jsinteroptests;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;

/**
 * This concrete test class is not annotated as a @JsType but its parent is.
 */
class ConcreteJsTypeSubclass extends ConcreteJsType {
  @JsConstructor
  public ConcreteJsTypeSubclass() {}

  /**
   * Overrides an exported method in the parent class and so will also itself be exported.
   */
  @Override
  public int publicMethod() {
    return 20;
  }

  /*
   * The following members will not be exported since this class is not an @JsType and these members
   * do not override already exported members.
   */

  public int publicSubclassMethod() {
    return super.publicMethod();
  }

  public static void publicStaticSubclassMethod() {}

  private void privateSubclassMethod() {}

  protected void protectedSubclassMethod() {}

  void packageSubclassMethod() {}

  public int publicSubclassField = 20;

  public static int publicStaticSubclassField = 20;

  private int privateSubclassField = 20;

  protected int protectedSubclassField = 20;

  int packageSubclassField = 20;

  @JsMethod
  public static native boolean hasPublicMethod(Object obj);

  @JsMethod
  public static native boolean hasPublicSubclassMethod(Object obj);

  @JsMethod
  public static native boolean hasPublicStaticSubclassMethod(Object obj);

  @JsMethod
  public static native boolean hasPrivateSubclassMethod(Object obj);

  @JsMethod
  public static native boolean hasProtectedSubclassMethod(Object obj);

  @JsMethod
  public static native boolean hasPackageSubclassMethod(Object obj);

  @JsMethod
  public static native boolean hasPublicSubclassField(Object obj);

  @JsMethod
  public static native boolean hasPublicStaticSubclassField(Object obj);

  @JsMethod
  public static native boolean hasPrivateSubclassField(Object obj);

  @JsMethod
  public static native boolean hasProtectedSubclassField(Object obj);

  @JsMethod
  public static native boolean hasPackageSubclassField(Object obj);
}
