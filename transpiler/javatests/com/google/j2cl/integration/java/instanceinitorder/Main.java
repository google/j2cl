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
package instanceinitorder;

import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;
import static com.google.j2cl.integration.testing.Asserts.assertNull;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test instance initialization order.
 */
public class Main {
  public static int initStep;

  public static void main(String... args) {
    initStep = 1;
    Child c = new Child();
    assertTrue(Main.initStep == 7);

    // The values in these fields are written to by the super constructor, but they are overwritten
    // by the constructor due to having an initializer.
    assertTrue(c.doubleNegativeZero == -0.0);
    assertTrue(c.floatNegativeZero == -0.0f);
    assertTrue(c.field1 == 0);
    assertTrue(c.field6 == 0);
    assertNotNull(c.field2);
    assertTrue(c.field3 == 0);
    assertFalse(c.field4);
    assertNull(c.field5);

    // Fields with no initializer will see the values written by the super constructor.
    assertTrue(c.uninitializedField1 == 1);
    assertTrue(c.uninitializedField2);
    assertTrue(c.uninitializedField3 == 'a');
    assertTrue(c.uninitializedField4 == 1);
    assertTrue(c.uninitializedField5 == 1);
    assertTrue(c.uninitializedField6 == 1L);
    assertTrue(c.uninitializedField7 == 1.0);
    assertTrue(c.uninitializedField8 == 1.0f);
    assertNotNull(c.uninitializedField9);
  }

  static class Child extends Parent {
    public int uninitializedField1;
    public boolean uninitializedField2;
    public char uninitializedField3;
    public byte uninitializedField4;
    public short uninitializedField5;
    public long uninitializedField6;
    public double uninitializedField7;
    public float uninitializedField8;
    public Object uninitializedField9;

    public double doubleNegativeZero = -0.0;
    public float floatNegativeZero = -0.0f;

    public int field1 = this.initializeField1();
    public Object field2 = new Object();
    public int field3 = 0;
    public boolean field4 = false;
    public Object field5 = null;

    {
      assertTrue(initStep++ == 3); // #3
    }

    public int field6 = initializeField6();

    {
      assertTrue(initStep++ == 5); // #5
    }

    public Child() {
      assertTrue(initStep++ == 6); // #6
    }

    public int initializeField1() {
      assertTrue(initStep++ == 2); // #2
      return 0;
    }

    public int initializeField6() {
      assertTrue(initStep++ == 4); // #4
      return 0;
    }

    @Override
    void assignFromParentConstructor() {
      assertTrue(initStep++ == 1); // #1
      // Assign values to each field that are different from the default to test which assigment is
      // done last.
      uninitializedField1 = 1;
      uninitializedField2 = true;
      uninitializedField3 = 'a';
      uninitializedField4 = 1;
      uninitializedField5 = 1;
      uninitializedField6 = 1L;
      uninitializedField7 = 1.0;
      uninitializedField8 = 1.0f;
      uninitializedField9 = new Object();

      doubleNegativeZero = -1.0;
      floatNegativeZero = -1.0f;

      field1 = 10;
      field2 = null;
      field3 = 1;
      field4 = true;
      field5 = new Object();
      field6 = 10;
    }
  }

  static class Parent {
    Parent() {
      assignFromParentConstructor();
    }

    void assignFromParentConstructor() {}
  }
}
