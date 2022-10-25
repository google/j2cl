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
package strictequality;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.getUndefined;
import static com.google.j2cl.integration.testing.TestUtils.isJavaScript;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;

public class Main {
  public static void main(String... args) {
    testEqualityIsStrict();
    testEqualityIsStrict_regression();
    testBoxedAndDevirtualizedTypes();
  }

  @SuppressWarnings({"EqualsIncompatibleType", "BoxedPrimitiveEquality"})
  private static void testBoxedAndDevirtualizedTypes() {
    // These tests are JavaScript specific.
    if (!isJavaScript()) {
      return;
    }
    assertTrue(new Character((char) 1) != new Character((char) 1));
    assertTrue(Character.valueOf((char) 1) == Character.valueOf((char) 1));

    assertTrue(new Byte((byte) 1) != new Byte((byte) 1));
    assertTrue(Byte.valueOf((byte) 1) == Byte.valueOf((byte) 1));

    assertTrue(new Integer(1) != new Integer(1));
    assertTrue(Integer.valueOf(1) == Integer.valueOf(1));

    // assertTrue(new String("asdf") != new String("asdf")); // can't honor, it's native JS string
    assertTrue(String.valueOf("asdf") == String.valueOf("asdf"));

    // assertTrue(new Boolean(true) != new Boolean(true)); // can't honor, it's native JS boolean
    assertTrue(Boolean.valueOf(true) == Boolean.valueOf(true));

    if (!isJvm()) {
      // assertTrue(new Double(1) != new Double(1)); // can't honor, it's native JS double
      assertTrue(
          Double.valueOf(1) == Double.valueOf(1)); // different from Java, it's native JS double
    }
  }

  private static void testEqualityIsStrict() {
    Object nullObject = null;
    Object emptyString = "";
    Object boxedBooleanFalse = false;
    Object boxedDoubleZero = 0.0d;
    Object emptyArray = new Object[] {};
    Object undefined = getUndefined();

    assertTrue(undefined == null);
    assertTrue(undefined == nullObject);
    assertTrue(undefined != boxedBooleanFalse);
    assertTrue(undefined != emptyString);
    assertTrue(undefined != boxedDoubleZero);
    assertTrue(undefined != emptyArray);

    assertTrue(null == nullObject);
    assertTrue(null != boxedBooleanFalse);
    assertTrue(null != emptyString);
    assertTrue(null != boxedDoubleZero);
    assertTrue(null != emptyArray);

    assertTrue(boxedBooleanFalse != nullObject);
    assertTrue(boxedBooleanFalse != emptyString);
    assertTrue(boxedBooleanFalse != boxedDoubleZero);
    assertTrue(boxedBooleanFalse != emptyArray);

    assertTrue(boxedDoubleZero != nullObject);
    assertTrue(boxedDoubleZero != emptyString);
    assertTrue(boxedDoubleZero != emptyArray);

    assertTrue(emptyArray != nullObject);
    assertTrue(emptyArray != emptyString);

    assertTrue(emptyString != nullObject);
  }

  // Make sure String does not end up compared via '==' (b/33850935).
  @SuppressWarnings({"EqualsIncompatibleType", "BoxedPrimitiveEquality"})
  private static void testEqualityIsStrict_regression() {
    // java.lang.Object.equals should not optimize to '=='
    assertTrue(!new StringBuilder("data").equals("data"));

    // CharSequence comparision should not optimize to '=='
    assertTrue(charSeq1() != charSeq2());
  }

  private static CharSequence charSeq1() {
    return Math.random() > 0 ? "data" : null;
  }

  private static CharSequence charSeq2() {
    return Math.random() > 0 ? new StringBuilder("data") : null;
  }
}
