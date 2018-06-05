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

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

/** Defines helper functions used for generating cast statements. */
@JsType(namespace = "vmbootstrap")
class Casts {

  // Note: implemented as native to avoid $clinit since Closure chokes while inlining the method.
  public static native Object $to(Object instance, Constructor castType);

  @JsFunction
  private interface IsInstanceFn {
    public boolean execute(Object instance);
  }

  public static Object $toInternal(
      Object instance, IsInstanceFn castTypeIsInstance, Constructor castType) {
    // TODO(goktug) remove isTypeCheck after JsCompiler can remove calls to castTypeIsInstance when
    // the return is unused.
    if (InternalPreconditions.isTypeChecked()) {
      boolean castSucceeds = instance == null || castTypeIsInstance.execute(instance);
      if (!castSucceeds) {
        String message =
            instance.getClass().getName() + " cannot be cast to " + Class.$get(castType).getName();
        InternalPreconditions.checkType(false, message);
      }
    }
    return instance;
  }
}
