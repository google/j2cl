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
package jsignore;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType
public class JsIgnoreExample {

  public static void exportedFunction() {}

  @JsIgnore
  public static void notExportedFunction() {}

  public static int exportedField = 10;

  @JsIgnore
  public static int notExportedField = 20;

  public static final Object CONSTNAME = new Object();
}
