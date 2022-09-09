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
package superfieldaccess;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

/** Test field access using the {@code super} keyword. */
public class Main {
  public static void main(String[] args) {
    testSuperFieldAccess();
  }

  private static void testSuperFieldAccess() {
    class GrandParent {
      public String name = "GrandParent";
    }

    class Parent extends GrandParent {
      final String name;

      Parent(String from) {
        this.name = "Parent (from " + from + ")";
      }

      public String getParentName() {
        return super.name;
      }
    }

    class Child extends Parent {
      final String name;

      Child() {
        this("Child");
      }

      Child(String from) {
        super(from);
        this.name = "Child (from " + from + ")";
      }

      public String getParentName() {
        return super.name;
      }

      public String getGrandParentName() {
        return super.getParentName();
      }

      class Inner extends Parent {
        Inner() {
          super("Inner");
        }

        public String getOuterParentName() {
          return Child.super.name;
        }

        class InnerInner {
          public String getOuterParentNameQualified() {
            return Child.Inner.super.name;
          }
        }
      }

    }

    assertEquals("Parent (from Child)", new Child().getParentName());
    assertEquals("GrandParent", new Child().getGrandParentName());
    assertEquals("Parent (from Child)", new Child().new Inner().getOuterParentName());
    assertEquals(
        "Parent (from Inner)",
        new Child().new Inner().new InnerInner().getOuterParentNameQualified());
  }
}
