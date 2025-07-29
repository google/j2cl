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
package properties

class Properties<T>(
  val inConstructorReadOnly: Int,
  var inConstructorReadWrite: Int,
  normalParam: Int
) {
  val readOnly = 1
  var readWrite = 2
  var parametricProperty: T? = null
  val complexPropertyInitializer = if (normalParam > 0) normalParam else throw AssertionError()

  val readOnlyCustomGetter: Int
    get() = 1

  var readWriteCustomGetter: Int = 1
    get() = field + 1

  var readWriteCustomSetter: Int = 1
    set(value) {
      field = 3
    }

  var customGetterSetterAndBackingField: Int = 1
    get() = field - 1
    set(value) {
      field = value + 1
    }

  var withPrivateDSetter: Int = 1
    public get() = field - 1
    private set(value) {
      field = value + 1
    }
}

val readOnly = 1
var readWrite = 2

val readOnlyCustomGetter: Int
  get() = 1

var readWriteCustomGetter: Int = 1
  get() = field + 1

var readWriteCustomSetter: Int = 1
  set(value) {
    field = 3
  }

var customGetterSetterAndBackingField: Int = 1
  get() = field - 1
  set(value) {
    field = value + 1
  }

var withPrivateDSetter: Int = 1
  public get() = field - 1
  private set(value) {
    field = value + 1
  }

fun <E : String> main() {
  Properties<E>(1, 2, 3).parametricProperty
  Properties<Int>(1, 2, 3).parametricProperty
}

interface Holder<T> {
  var content: T
}

interface NumberHolder<T : Number> {
  var content: T
}

open class Base {
  open var content: Int = 1
}

class Implementor : Base(), Holder<Int>, NumberHolder<Int> {}

class OverridingImplementor : Base(), Holder<Int>, NumberHolder<Int> {
  override var content: Int = 2
}
