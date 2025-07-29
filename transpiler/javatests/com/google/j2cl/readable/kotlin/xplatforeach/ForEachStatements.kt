/*
 * Copyright 2024 Google Inc.
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
package xplatforeach

import com.google.apps.docs.xplat.collections.JsArrayInteger
import com.google.apps.docs.xplat.collections.SerializedJsArray
import com.google.apps.docs.xplat.collections.SerializedJsMap
import com.google.apps.docs.xplat.collections.UnsafeJsMap
import com.google.apps.docs.xplat.collections.UnsafeJsMapInteger
import com.google.apps.docs.xplat.collections.UnsafeJsSet
import com.google.apps.docs.xplat.structs.SparseArray
import com.google.gwt.corp.collections.ImmutableJsArray
import com.google.gwt.corp.collections.JsArray
import com.google.gwt.corp.collections.UnmodifiableJsArray

fun jsArray(array: JsArray<Throwable>) {
  for (element in array.getIterable()) {
    element.toString()
  }
}

fun jsArrayOfJsEnum(array: JsArray<SomeJsEnum>) {
  for (element in array.getIterable()) {
    element.value.toString()
  }
}

fun jsArrayOfNativeJsEnum(array: JsArray<NativeJsEnum>) {
  for (element in array.getIterable()) {
    element.toString()
  }
}

fun immutableJsArray(array: ImmutableJsArray<Throwable>) {
  for (element in array.getIterable()) {
    element.toString()
  }
}

fun unmodifiableJsArray(array: UnmodifiableJsArray<Throwable>) {
  for (element in array.getIterable()) {
    element.toString()
  }
}

fun jsArrayInteger(array: JsArrayInteger) {
  for (element: Int in array.getIterable()) {
    element.toString()
  }

  for (element: Int? in array.getIterable()) {
    element.toString()
  }
}

fun serializedJsArray(array: SerializedJsArray) {
  for (element in array.getIterable()) {
    element.toString()
  }
}

fun serializedJsMap(map: SerializedJsMap) {
  var keys = ""
  for (key in map.getIterableKeys()) {
    keys += key
  }
}

fun unsafeJsMap(map: UnsafeJsMap<*>) {
  var keys = ""
  for (key in map.getIterableKeys()) {
    keys += key
  }
}

fun unsafeJsMapInteger(map: UnsafeJsMapInteger) {
  var keys = ""
  for (key in map.getIterableKeys()) {
    keys += key
  }
}

fun unsafeJsSet(set: UnsafeJsSet) {
  var keys = ""
  for (key in set.getIterableKeys()) {
    keys += key
  }
}

fun sparseArray(array: SparseArray<*>) {
  var sumOfKeys = 0
  for (key: Int? in array.getIterableKeys()) {
    sumOfKeys += key!!
  }

  for (key: Int in array.getIterableKeys()) {
    sumOfKeys += key
  }
}

fun withoutBlock(array: JsArray<Throwable>, map: SerializedJsMap) {
  for (element: Throwable in array.getIterable()) element.toString()
  for (key: String in map.getIterableKeys()) key.substring(1)
}
