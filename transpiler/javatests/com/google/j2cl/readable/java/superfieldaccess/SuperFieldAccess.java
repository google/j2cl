/*
 * Copyright 2025 Google Inc.
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

public class SuperFieldAccess {

  static class GrandParent {
    public String name = "GrandParent";
  }

  static class Parent extends GrandParent {
    final String name;

    Parent(String from) {
      this.name = "Parent (from " + from + ")";
    }

    public String getParentName() {
      return super.name;
    }
  }

  static class Child extends Parent {
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
}
