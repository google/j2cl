/*
 * Copyright 2023 Google Inc.
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
package jsconstructor;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import com.google.j2cl.integration.testing.TestUtils;
import jsinterop.annotations.JsConstructor;

public class InnerClassCapture {
  public abstract static class Base {
    public Base() {
      callFromCtor();
    }

    public abstract void callFromCtor();
  }

  public abstract static class BaseJsConstructor {
    @JsConstructor
    public BaseJsConstructor() {
      callFromCtor();
    }

    public abstract void callFromCtor();
  }

  public static class Outer {
    int i = 1;

    public class Inner extends Base {
      @JsConstructor
      public Inner() {
        super();
      }

      public int getEnclosingValue() {
        return i;
      }

      @Override
      public void callFromCtor() {
        // Referencing Outer.i here requires that outer class capture is resolved before we invoke
        // the BaseType ctor as this function will be invoked by that ctor.
        i += 2;
      }
    }

    public class InnerExtendsJsConstructor extends BaseJsConstructor {
      @JsConstructor
      public InnerExtendsJsConstructor() {
        super();
      }

      public int getEnclosingValue() {
        return i;
      }

      @Override
      public void callFromCtor() {
        // Referencing Outer.i here requires that outer class capture is resolved before we invoke
        // the BaseType ctor as this function will be invoked by that ctor.
        if (Outer.this == null) {
          failedInCtor = true;
        } else {
          i += 3;
        }
      }
    }
  }

  private static boolean failedInCtor = false;

  public static void test() {
    Outer outer = new Outer();

    assertEquals(1, outer.i);

    Outer.Inner inner = outer.new Inner();
    assertEquals(3, outer.i);
    assertEquals(3, inner.getEnclosingValue());

    // J2CL cannot properly emulate the JVM semantics of ensuring the outer class binding is
    // resolved before calling the super constructor. This is due to a limitation of ES6 classes
    // where the instance cannot be referenced before the super ctor is called. The binding will be
    // resolved just after the super constructor is called.
    failedInCtor = false;
    Outer.InnerExtendsJsConstructor innerJsConstructor = outer.new InnerExtendsJsConstructor();

    if (TestUtils.isJvm() || TestUtils.isJ2Kt() || TestUtils.isWasm()) {
      assertFalse(failedInCtor);
      assertEquals(6, innerJsConstructor.getEnclosingValue());
    } else {
      assertTrue(failedInCtor);
      assertEquals(3, innerJsConstructor.getEnclosingValue());
    }
  }
}
