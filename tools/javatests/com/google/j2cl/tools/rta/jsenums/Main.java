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
package com.google.j2cl.tools.rta.jsenums;

import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsMethod;

public class Main {
  @JsEnum
  enum UnusedJsEnum {
    FOO
  }

  @JsEnum
  enum AccessedJsEnum {
    BAR
  }

  @JsEnum
  enum UsedJsEnum {
    BAZ
  }

  @JsEnum
  enum JsEnumWithOverlay {
    Foo;

    String getName() {
      return "Foo";
    }
  }

  @JsEnum
  enum UnusedJsEnumWithOverlay {
    Foo;

    String getName() {
      return "Foo";
    }
  }

  @JsMethod
  static void entryPoint() {
    // avoid polluting unused_[members|types] files
    new Main();

    AccessedJsEnum bar = AccessedJsEnum.BAR;

    var unused = UsedJsEnum.BAZ.ordinal();

    JsEnumWithOverlay.Foo.getName();
  }
}
