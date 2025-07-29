/*
 * Copyright 2023 Google Inc.
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
package nativeinjection

import jsinterop.annotations.JsMethod

/**
 * The class has several special properties; directory doesn't follow package name and also
 * directory has java in directy name. The Kotlin compiler will package the class file in a
 * directory following the package statement of this class. The native file will not be repackaged
 * next to this file and it needs to use fqn in order to match.
 */
object NativeClassSuper {
  @JvmStatic
  @Suppress("unusable-by-js")
  // TODO(b/307651466): Remove the JsMethod annotation when `unusable-by-js` suppression is
  //   supported by Kotlin frontend.
  @JsMethod
  external fun nativeStaticMethod(): String
}
