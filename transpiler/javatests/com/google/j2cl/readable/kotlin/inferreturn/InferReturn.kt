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
package inferreturn

import java.util.ArrayList

/** Tests that Closure type inference based on return doesn't result in warnings.  */
class InferReturn<T>

fun <T> inferGeneric(foo: T): InferReturn<T> {
  return InferReturn()
}

fun tightenType(foo: InferReturn<String?>?): InferReturn<InferReturn<String?>>? {
  return if (foo != null) {
    // Without a cast to fix it, JSCompiler will infer the type of this return statement to be
    // ?Foo<!Foo<?string>>, which does not match the return type, ?Foo<?Foo<?string>>.
    inferGeneric(foo)
  } else null
}

fun main() {
  val list: ArrayList<Any?>? = newArrayList("foo")
  // list will be tightened to ArrayList<String> hence OTI would complain below without a cast.
  acceptsArrayListOfObject(list)
  val array: Array<Any?>? = newArray("foo")
  // same here
  acceptsArrayOfObject(array)
}

fun <V> newArrayList(foo: V): ArrayList<V>? {
  return ArrayList()
}

fun <V> newArray(foo: V): Array<V?>? {
  return arrayOfNulls<Any>(0) as Array<V?>
}

fun acceptsArrayListOfObject(foo: ArrayList<Any?>?) {
  // empty
}

fun acceptsArrayOfObject(foo: Array<Any?>?) {
  // empty
}
