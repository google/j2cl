/*
 * Copyright 2016 Google Inc.
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
package javaemul.internal;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

/** Simple class to work with native js regular expressions. */
public class NativeRegExp {
  private final WasmExtern instance;

  public NativeRegExp(String regex) {
    this.instance = create(regex.toJsString());
  }

  public NativeRegExp(String regex, String mode) {
    this.instance = create(regex.toJsString(), mode.toJsString());
  }

  public void setLastIndex(int index) {
    setLastIndex(this.instance, index);
  }

  public Match exec(String value) {
    WasmExtern result = exec(this.instance, value.toJsString());
    return result == null ? null : new Match(result);
  }

  public boolean test(String value) {
    return test(this.instance, value.toJsString());
  }

  public WasmExtern toJs() {
    return instance;
  }

  /** Contract of the this.instance returned by {@code RegExp.prototype.exec}. */
  public static class Match {
    private final WasmExtern instance;

    private Match(WasmExtern instance) {
      this.instance = instance;
    }

    public int getIndex() {
      return index(instance);
    }

    public int getLength() {
      return getLength(instance);
    }

    public String getAt(int index) {
      return String.fromJsString(getBufferAt(instance, index));
    }

    @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExpResult.index")
    private static native int index(WasmExtern match);

    @JsMethod(namespace = JsPackage.GLOBAL)
    private static native String.NativeString getBufferAt(WasmExtern o, int i);

    @JsMethod(namespace = JsPackage.GLOBAL)
    private static native int getLength(WasmExtern o);
  }

  @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExp.create")
  private static native WasmExtern create(String.NativeString pattern);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExp.create")
  private static native WasmExtern create(String.NativeString pattern, String.NativeString flags);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExp.setLastIndex")
  private static native void setLastIndex(WasmExtern regExp, int index);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExp.exec")
  private static native WasmExtern exec(WasmExtern regExp, String.NativeString text);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExp.test")
  private static native boolean test(WasmExtern regExp, String.NativeString text);
}
