/*
 * Copyright 2022 Google Inc.
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
package staticfieldaccesslevels

var a = 0
private var b = false
internal var c = 0

public class StaticFieldAccessLevels {
  companion object {
    var d = 0
    private var e = false
    protected var f: Any? = null
    internal var g = 0
  }
}



fun test(): Int {
  return if (b) a else c
}

