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
package unsafevariance

/**
 * Test for the mismatch in parameters in methods like List.contains, which in java is declared as
 * contains(Object) but in kotlin is declared as contains(e : @UnsafeVariance E)
 */
abstract class StringList : List<String> {
  override fun contains(element: String): Boolean {
    return true
  }

  override fun containsAll(elements: Collection<String>): Boolean {
    return true
  }

  override fun indexOf(element: String): Int {
    return -1
  }

  override fun lastIndexOf(element: String): Int {
    return -1
  }
}

interface StringContainer {
  fun contains(s: String): Boolean
}

// The accidental override here shows the need to expose both contains(Object) and contains(String).
// It turns out that all the methods that are special handled by kotlinc are declared as @JsMethod
// in J2CL.
abstract class StringListAndContainer : List<String>, StringContainer

abstract class StringListAndContainerWithMethods : List<String>, StringContainer {
  override fun contains(element: String): Boolean {
    return true
  }
}

abstract class StringListSub : StringList() {
  override fun contains(element: String): Boolean {
    return true
  }
}

abstract class StringListSubImplContainer : StringList(), StringContainer {
  override fun contains(element: String): Boolean {
    return true
  }
}
