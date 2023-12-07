/*
 * Copyright 2023 Google Inc.
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
package jsenum;

import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;

class NativeEnums {

  @JsEnum(isNative = true, namespace = "test")
  enum NativeEnum {
    @JsProperty(name = "OK")
    ACCEPT,
    CANCEL
  }

  @JsEnum(isNative = true, namespace = "test", name = "NativeEnum", hasCustomValue = true)
  enum StringNativeEnum {
    OK,
    CANCEL;

    private String value;

    @JsOverlay
    public String getValue() {
      return value;
    }
  }

  @JsEnum(isNative = true, namespace = "test", name = "NativeEnumOfNumber", hasCustomValue = true)
  enum NumberNativeEnum {
    ONE,
    TWO;

    int value;
  }

  static boolean nativeClinitCalled = false;

  @JsEnum(isNative = true, hasCustomValue = true, namespace = "test", name = "NativeEnum")
  enum NativeEnumWithClinit {
    OK;

    static {
      nativeClinitCalled = true;
    }

    String value;

    @JsOverlay
    String getValue() {
      return value;
    }
  }

  private NativeEnums() {}
}
