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
package jsconstructornested

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsType

class Main {

  var name = "value one"

  val innerJsType = InnerJsType()

  /** The inner JsType in question. */
  @JsType(namespace = "foo", name = "Bar")
  inner class InnerJsType {
    fun getOuterName(): String = name

    fun setOuterName(name: String) {
      this@Main.name = name
    }
  }

  /**
   * A JsInterop calling interface against InnerJsType. We use this to prove that InnerJsType's raw
   * JS constructor does receive an $outer_this parameter.
   */
  @JsType(isNative = true, namespace = "foo", name = "Bar")
  internal class InnerJsTypeFacade internal constructor(outerThis: Main) {
    internal external fun getOuterName(): String

    internal external fun setOuterName(name: String): String
  }
}

/**
 * Test seeks to prove *both* that inner classes annotated with @JsType are getting @JsConstructor
 * implicitly added to their constructor *and* that constructor receives and properly uses an
 * $outer_this parameter.
 */
fun main(vararg args: String) {
  val main = Main()

  // Exercises the class via Java. Prove that the $outer_this is arriving and being used
  // properly but doesn't prove whether it is arriving via the raw JS constructor or if it's
  // actually arriving via a $ctor.
  run {
    val innerJsType = main.innerJsType
    assertTrue("value one".equals(innerJsType.getOuterName()))
    innerJsType.setOuterName("value two")
    assertTrue("value two".equals(innerJsType.getOuterName()))
  }

  // Exercises the class via JsInterop. Proves that the $outer_this is definitely arriving via the
  // raw JS constructor.
  run {
    val innerJsTypeFacade = Main.InnerJsTypeFacade(main)
    assertTrue("value two" == innerJsTypeFacade.getOuterName())
    innerJsTypeFacade.setOuterName("value three")
    assertTrue("value three" == innerJsTypeFacade.getOuterName())
  }
}
