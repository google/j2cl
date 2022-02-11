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
package com.google.j2cl.ported.java6;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import jsinterop.annotations.JsMethod;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ArraysTest {
  @JsMethod
  private static native Object createJsArray(int length);

  @Test
  public void testObjectArray_empty() {
    Object nativeArray = createJsArray(0);
    assertThat(nativeArray instanceof Object[]).isTrue();
    assertThat(nativeArray instanceof Object[][]).isFalse();
    assertThat(nativeArray instanceof int[]).isFalse();
    assertThat(nativeArray instanceof List[]).isFalse();

    Object objectArray = new Object[0];
    assertThat(objectArray instanceof Object[]).isTrue();
    assertThat(objectArray instanceof Object[][]).isFalse();
    assertThat(objectArray instanceof int[]).isFalse();
    assertThat(objectArray instanceof List[]).isFalse();
    assertThat(objectArray.getClass()).isEqualTo(Object[].class);
  }

  @Test
  public void testObjectArray_nonEmpty() {
    // Native array is an object array
    Object nativeArray = createJsArray(10);
    assertThat(nativeArray instanceof Object[]).isTrue();
    assertThat(nativeArray instanceof Object[][]).isFalse();
    assertThat(nativeArray instanceof int[]).isFalse();
    assertThat(nativeArray instanceof List[]).isFalse();
    assertThat(nativeArray.getClass()).isEqualTo(Object[].class);

    Object objectArray = new Object[10];
    assertThat(objectArray instanceof Object[]).isTrue();
    assertThat(objectArray instanceof Object[][]).isFalse();
    assertThat(objectArray instanceof int[]).isFalse();
    assertThat(objectArray instanceof List[]).isFalse();
    assertThat(objectArray.getClass()).isEqualTo(Object[].class);
  }

  @Test
  public void testObjectObjectArray() {
    Object array = new Object[10][];
    assertThat(array instanceof Object[]).isTrue();
    assertThat(array instanceof Object[][]).isTrue();
    assertThat(array instanceof int[]).isFalse();
    assertThat(array instanceof List[]).isFalse();
    assertThat(array.getClass()).isEqualTo(Object[][].class);

    Object[] objectArray = (Object[]) array;
    objectArray[0] = new Object[0];
    objectArray[1] = new List<?>[1];
    objectArray[2] = new Double[1];

    try {
      objectArray[3] = new int[0];
      fail("Should have thrown ArrayStoreException");
    } catch (ArrayStoreException expected) {
    }

    try {
      objectArray[4] = new Object();
      fail("Should have thrown ArrayStoreException");
    } catch (ArrayStoreException expected) {
    }
  }
}
