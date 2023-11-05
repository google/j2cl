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

/** Placeholders for Wasm. TODO(b/288145698): Native JsEnum is not supported. */
class NativeEnums {

  enum NativeEnum {
    ACCEPT,
    CANCEL
  }

  enum StringNativeEnum {
    OK,
    CANCEL;

    private String value;

    public String getValue() {
      return value;
    }
  }

  enum NumberNativeEnum {
    ONE,
    TWO;

    int value;
  }

  static boolean nativeClinitCalled = false;

  enum NativeEnumWithClinit {
    OK;

    String value;

    String getValue() {
      return value;
    }
  }

  private NativeEnums() {}
}
