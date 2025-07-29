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
package instanceinitializers

class InstanceInitializersWithPrimary(val ctorProperty: Int = 0, ctorParam: Int) {
  var field = 1

  init {
    field = ctorParam
  }

  var field2 = ctorProperty

  init {
    field = field2
  }
}

class InstanceInitializersNoPrimary {
  var field = 0

  constructor() {
    field = 2
    field2 = 3
  }

  init {
    field = 1
  }

  var field2 = 0

  init {
    field = field2
  }
}
