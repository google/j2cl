/*
 * Copyright 2015 Google Inc.
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

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

/** Tests JsType with array functionality. */
object JsTypeArrayTest {
  fun testAll() {
    testJsTypeArray_asAField()
    testJsTypeArray_asAParam()
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeAsAField {}

  @JsType
  private class SimpleJsTypeAsAFieldHolder {
    var arrayField: Array<SimpleJsTypeAsAField>? = null
  }

  private fun testJsTypeArray_asAField() {
    val holder = SimpleJsTypeAsAFieldHolder()
    fillArrayField(holder)
    val array = holder.arrayField
    assertEquals(2, array!!.size)
    assertNotNull(array!![0])
  }

  @JsMethod(namespace = "jsinteroptests.JsTypeArrayTestHelper")
  @JvmStatic
  private external fun fillArrayField(holder: SimpleJsTypeAsAFieldHolder)

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeAsAParam {}

  @JsType
  private class SimpleJsTypeAsAParamHolder {
    internal var theParam: Array<SimpleJsTypeAsAParam>? = null

    fun setArrayParam(param: Array<SimpleJsTypeAsAParam>) {
      theParam = param
    }
  }

  private fun testJsTypeArray_asAParam() {
    val holder = SimpleJsTypeAsAParamHolder()
    fillArrayParam(holder)
    val array = holder.theParam
    assertEquals(2, array!!.size)
    assertNotNull(array!![0])
  }

  @JsMethod(namespace = "jsinteroptests.JsTypeArrayTestHelper")
  @JvmStatic
  private external fun fillArrayParam(holder: SimpleJsTypeAsAParamHolder)
}
