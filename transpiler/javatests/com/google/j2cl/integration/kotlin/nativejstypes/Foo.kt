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
package nativejstypes

import jsinterop.annotations.JsType

/** Native JsType with "namespace" and "name". */
@JsType(namespace = "com.acme", name = "MyFoo", isNative = true)
class Foo {
  @JvmField var x: Int = 0
  @JvmField var y: Int = 0

  external fun sum(): Int
}
