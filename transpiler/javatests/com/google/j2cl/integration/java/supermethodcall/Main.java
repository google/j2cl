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
package supermethodcall;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

/**
 * Test non-constructor super method calls.
 */
public class Main {
  public static void main(String[] args) {
    testSuperMethodCalls();
    testSuperMethodCalls_returnSpecialization();
    testSuperMethodCalls_parameterSpecialization();
    testSuperMethodCalls_defaultMethods();
  }

  private static void testSuperMethodCalls() {
    class GrandParent {
      public String getName() {
        return "GrandParent";
      }
    }

    class Parent extends GrandParent {
      public String getName() {
        return "Parent";
      }

      public String getParentName() {
        return super.getName();
      }
    }

    class Child extends Parent {
      @Override
      public String getName() {
        return "Child";
      }

      @Override
      public String getParentName() {
        return super.getName();
      }

      public String getGrandParentName() {
        return super.getParentName();
      }

      class Inner {
        public String getOuterParentName() {
          return Child.super.getName();
        }
      }
    }
    assertEquals("Parent", new Child().getParentName());
    assertEquals("GrandParent", new Child().getGrandParentName());
    assertEquals("Parent", new Child().new Inner().getOuterParentName());
  }

  private static void testSuperMethodCalls_returnSpecialization() {
    class GrandParent {
      public CharSequence getName() {
        return "GrandParent";
      }
    }

    class Parent extends GrandParent {
      @Override
      public String getName() {
        return "Parent";
      }

      public String getParentName() {
        return (String) super.getName();
      }
    }

    class Child extends Parent {
      public String getName() {
        return "Child";
      }

      public String getParentName() {
        return super.getName();
      }

      public String getGrandParentName() {
        return super.getParentName();
      }
    }

    assertEquals("Parent", new Child().getParentName());
    assertEquals("GrandParent", new Child().getGrandParentName());
  }

  private static void testSuperMethodCalls_parameterSpecialization() {
    class GrandParent<T> {
      public T getName(T object) {
        return (T) "GrandParent";
      }
    }

    class Parent<T extends CharSequence> extends GrandParent<T> {
      @Override
      public T getName(T object) {
        return (T) "Parent";
      }

      public T getParentName(T object) {
        // Dispatches to an override.
        return super.getName(object);
      }
    }

    class Child extends Parent<String> {
      // Parameter Specialization
      public String getName(String object) {
        return "Child";
      }

      public String getParentName(String object) {
        return super.getName(object);
      }

      public String getGrandParentName(String object) {
        return super.getParentName(object);
      }
    }

    assertEquals("Parent", new Child().getParentName(null));
    assertEquals("GrandParent", new Child().getGrandParentName(null));
  }

  interface GrandParentInterface {
    default String getName() {
      return "GrandParent";
    }

    default String getDiamondName() {
      return "Diamond";
    }
  }

  interface Parent1Interface extends GrandParentInterface {
    default String getName() {
      return "Parent1";
    }

    default String getParentName() {
      return GrandParentInterface.super.getName();
    }
  }

  interface Parent2Interface extends GrandParentInterface {
    default String getName() {
      return "Parent2";
    }

    default String getParentName() {
      return GrandParentInterface.super.getName();
    }
  }

  interface ChildInterface extends Parent1Interface, Parent2Interface {
    default String getName() {
      return "Child";
    }

    default String getParentName() {
      return Parent1Interface.super.getName() + "+" + Parent2Interface.super.getName();
    }

    default String getGrandParentName() {
      return Parent1Interface.super.getParentName() + "+" + Parent2Interface.super.getParentName();
    }
  }

  interface GrandChildInterface extends ChildInterface {
    default String getDiamondName() {
      return ChildInterface.super.getDiamondName();
    }
  }

  private static void testSuperMethodCalls_defaultMethods() {
    assertEquals("GrandParent", new GrandParentInterface() {}.getName());
    assertEquals("GrandParent", new Parent1Interface() {}.getParentName());
    assertEquals("Parent1", new Parent1Interface() {}.getName());
    assertEquals("GrandParent", new Parent2Interface() {}.getParentName());
    assertEquals("Parent2", new Parent2Interface() {}.getName());
    assertEquals("GrandParent+GrandParent", new ChildInterface() {}.getGrandParentName());
    assertEquals("Parent1+Parent2", new ChildInterface() {}.getParentName());
    assertEquals("Child", new ChildInterface() {}.getName());
    assertEquals("Diamond", new GrandChildInterface() {}.getDiamondName());
  }
}
