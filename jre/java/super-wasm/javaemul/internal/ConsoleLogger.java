/*
 * Copyright 2021 Google Inc.
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

/**
 * A helper to print log messages to console.
 *
 * <p>Note that, this is not a public API and can change/disappear in any release.
 */
public class ConsoleLogger {
  public static ConsoleLogger createIfSupported() {
    return new ConsoleLogger();
  }

  public void log(String level, String message) {
    log(level.toJsString(), message.toJsString());
  }

  public void log(String level, Throwable t) {
    log(level.toJsString(), t.toString().toJsString());
  }

  @JsMethod(namespace = JsPackage.GLOBAL, name = "ConsoleLogger.log")
  private static native void log(WasmExtern level, WasmExtern message);
}
