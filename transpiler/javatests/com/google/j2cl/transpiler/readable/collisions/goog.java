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
package com.google.j2cl.transpiler.readable.collisions;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class goog {}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
class Blah {
  @JsMethod(namespace = JsPackage.GLOBAL, name = "Math.random")
  static native void m();

  @JsProperty(name = "prototype.length")
  static native double getN();
}

class foo {}

class bar {
  {
    int foo, bar, goog, flip, window;
    Blah.m();
    Blah.getN();
    new Blah();
    new goog();
    new foo();

    // Variable colliding with reserved JavaScript names.
    int delete = 1;
    int undefined = 3;
  }
}

class Other<T> {
  {
    int C_T = 3;
    Other<T> o = (Other<T>) null;
  }

  <T> T m() {
    // TODO(b/112669572) : JsCompiler complains about the type of the local variable because
    // it confuses it with the method template (which is named M_T in the js code).
    int M_T = 5;
    Other<T> o = (Other<T>) null;
    return null;
  }
}

class C_T<T extends Number> {
  T m() {
    return null;
  }

  T n() {
    T t = new C_T<T>().m();
    t.byteValue();
    return t;
  }
}
