/*
 * Copyright 2019 Google Inc.
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
package com.google.j2cl.transpiler.readable.jsenum;

import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsNonNull;

public class Main {

  @JsEnum
  enum ComparableJsEnum {
    ZERO,
    ONE,
    TWO;

    public int getValue() {
      return ordinal();
    }
  }

  @JsEnum(hasCustomValue = true)
  enum IntJsEnum {
    TWENTY(20),
    ELEVEN(11);

    private int value;

    public int getValue() {
      return value;
    }

    IntJsEnum(int value) {
      this.value = value;
    }
  }

  @JsEnum(hasCustomValue = true)
  enum StringJsEnum {
    ONE("ONE"),
    THREE("THREE");

    private String value;

    public String getValue() {
      return value;
    }

    StringJsEnum(String value) {
      this.value = value;
    }
  }

  @JsEnum(hasCustomValue = true)
  enum NonNullableStringJsEnum {
    ONE("ONE"),
    THREE("THREE");

    private @JsNonNull String value;

    public String getValue() {
      return value;
    }

    NonNullableStringJsEnum(String value) {
      this.value = value;
    }
  }

  public static void testJsEnumSwitch() {
    ComparableJsEnum comparableJsEnum =
        ComparableJsEnum.ONE.getValue() == 1 ? ComparableJsEnum.TWO : null;
    switch (comparableJsEnum) {
      case TWO:
        break;
      default:
    }

    Comparable comparable = comparableJsEnum;
    comparableJsEnum = (ComparableJsEnum) comparable;

    IntJsEnum intJsEnum = IntJsEnum.ELEVEN.getValue() == 10 ? IntJsEnum.ELEVEN : null;
    switch (intJsEnum) {
      case TWENTY:
        break;
      default:
    }

    Object o = intJsEnum;
    intJsEnum = (IntJsEnum) o;

    StringJsEnum stringJsEnum = StringJsEnum.ONE.getValue() == "10" ? StringJsEnum.THREE : null;
    switch (stringJsEnum) {
      case ONE:
        break;
      default:
    }
  }
}
