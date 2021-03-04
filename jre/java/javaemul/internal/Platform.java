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

/**
 * Platform specific utilities. This class will eventually replace most of the functionalities in
 * JsUtils.
 */
public final class Platform {

  @JsMethod(namespace = "<window>")
  public static native boolean isNaN(double x);

  @JsMethod(namespace = "<window>")
  public static native boolean isFinite(double x);

  private Platform() {}
}
