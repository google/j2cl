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
package multipleconstructors

class PrimaryWithParentPrimary(var id: Int = 1, var flag: Boolean = true) :
  ParentWithPrimary(flag) {
  constructor(id: Int) : this(id, id == 0)
  constructor(flag: Boolean) : this(-1, flag)
}

class PrimaryWithParentNoPrimary(var id: Int = 1, var flag: Boolean = true) :
  ParentWithoutPrimary(id, flag) {
  constructor(id: Int) : this(id, id == 0)
  constructor(flag: Boolean) : this(0, flag)
}

class NoPrimaryWithParentNoPrimary : ParentWithoutPrimary {
  private var id = 0
  private var flag = false

  constructor(id: Int) : super(id) {
    this.id = id
    flag = id == 0
  }

  constructor(flag: Boolean) : this(-1, flag)

  constructor(id: Int, flag: Boolean) : super(id, flag) {
    this.id = id
    this.flag = flag
  }
}

class NoPrimaryWithParentPrimary : ParentWithPrimary {
  constructor(id: Int) : super(id == 0)
  constructor(flag: Boolean) : super(flag, "secondary")
}

open class ParentWithPrimary(f: Boolean) {
  constructor(f: Boolean, s: String) : this(f)
}

open class ParentWithoutPrimary {
  constructor(id: Int)
  constructor(flag: Boolean)
  constructor(id: Int, flag: Boolean)
}
