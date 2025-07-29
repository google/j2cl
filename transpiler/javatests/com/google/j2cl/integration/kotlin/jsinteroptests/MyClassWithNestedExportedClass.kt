/*
 * Copyright 2014 Google Inc.
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
package jsinteroptests

import jsinterop.annotations.JsType

/** Class with a nested exported class. */
class MyClassWithNestedExportedClass {
  /** Exported nested class. */
  @JsType
  class Inner {
    companion object {
      @JvmField val WOO = 99
    }
  }

  /** Exported nested class with namespace. */
  @JsType(namespace = "zoo")
  class InnerWithNamespace {
    companion object {
      @JvmField val WOOZ = 999
    }
  }

  /** Exported nested enum. */
  @JsType
  enum class InnerEnum {
    AA,
    BB,
  }
}
