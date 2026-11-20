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
package jsinteroptests;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Tests JsType with array functionality. */
public class JsTypeArrayTest {
  public static void testAll() {
    testJsTypeArray_asAField();
    testJsTypeArray_asAParam();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeAsAField {}

  @JsType
  private static class SimpleJsTypeAsAFieldHolder {
    public SimpleJsTypeAsAField[] arrayField;
  }

  private static void testJsTypeArray_asAField() {
    SimpleJsTypeAsAFieldHolder holder = new SimpleJsTypeAsAFieldHolder();
    fillArrayField(holder);
    SimpleJsTypeAsAField[] array = holder.arrayField;
    assertEquals(2, array.length);
    assertNotNull(array[0]);
  }

  @JsMethod(namespace = "jsinteroptests.JsTypeArrayTestHelper")
  private static native void fillArrayField(SimpleJsTypeAsAFieldHolder holder);

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeAsAParam {}

  @JsType
  private static class SimpleJsTypeAsAParamHolder {
    private SimpleJsTypeAsAParam[] theParam;

    public void setArrayParam(SimpleJsTypeAsAParam[] param) {
      theParam = param;
    }
  }

  private static void testJsTypeArray_asAParam() {
    SimpleJsTypeAsAParamHolder holder = new SimpleJsTypeAsAParamHolder();
    fillArrayParam(holder);
    SimpleJsTypeAsAParam[] array = holder.theParam;
    assertEquals(2, array.length);
    assertNotNull(array[0]);
  }

  @JsMethod(namespace = "jsinteroptests.JsTypeArrayTestHelper")
  private static native void fillArrayParam(SimpleJsTypeAsAParamHolder holder);
}
