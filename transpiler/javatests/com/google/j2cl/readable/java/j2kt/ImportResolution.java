/*
 * Copyright 2024 Google Inc.
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
package j2kt;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ImportResolution {
  static class Parent {
    class String {
      void parentStringMethod() {}

      void testJavaLangString(java.lang.String string) {
        string.trim();
      }

      void testParentString(String string) {
        string.parentStringMethod();
      }

      void testChildString(Child.String string) {
        string.childStringMethod();
      }

      void testSiblingParentString(SiblingParent.String string) {
        string.siblingParentStringMethod();
      }

      void testGenericString(Generic<?>.String string) {
        string.genericMethod();
      }
    }

    void testJavaLangString(java.lang.String string) {
      string.trim();
    }

    void testParentString(String string) {
      string.parentStringMethod();
    }

    void testChildString(Child.String string) {
      string.childStringMethod();
    }

    void testSiblingParentString(SiblingParent.String string) {
      string.siblingParentStringMethod();
    }

    void testGenericString(Generic<?>.String string) {
      string.genericMethod();
    }
  }

  static class Child extends Parent {
    class String {
      void childStringMethod() {}

      void testJavaLang(java.lang.String string) {
        string.trim();
      }

      void testParentString(Parent.String string) {
        string.parentStringMethod();
      }

      void testChildString(String string) {
        string.childStringMethod();
      }

      void testSiblingParentString(SiblingParent.String string) {
        string.siblingParentStringMethod();
      }

      void testGenericString(Generic<?>.String string) {
        string.genericMethod();
      }
    }

    @Override
    void testJavaLangString(java.lang.String string) {
      string.trim();
    }

    @Override
    void testParentString(Parent.String string) {
      string.parentStringMethod();
    }

    @Override
    void testChildString(String string) {
      string.childStringMethod();
    }

    @Override
    void testSiblingParentString(SiblingParent.String string) {
      string.siblingParentStringMethod();
    }

    @Override
    void testGenericString(Generic<?>.String string) {
      string.genericMethod();
    }
  }

  static class GrandChild extends Child {
    @Override
    void testJavaLangString(java.lang.String string) {
      string.trim();
    }

    @Override
    void testParentString(Parent.String string) {
      string.parentStringMethod();
    }

    @Override
    void testChildString(Child.String string) {
      string.childStringMethod();
    }

    @Override
    void testSiblingParentString(SiblingParent.String string) {
      string.siblingParentStringMethod();
    }

    @Override
    void testGenericString(Generic<?>.String string) {
      string.genericMethod();
    }
  }

  static class SiblingParent {
    class String {
      void siblingParentStringMethod() {}
    }

    void testJavaLangString(java.lang.String string) {
      string.trim();
    }

    void testParentString(Parent.String string) {
      string.parentStringMethod();
    }

    void testChildString(Child.String string) {
      string.childStringMethod();
    }

    void testSiblingParentString(String string) {
      string.siblingParentStringMethod();
    }

    void testGenericString(Generic<?>.String string) {
      string.genericMethod();
    }
  }

  static class Generic<Parent> {
    class String {
      void genericMethod() {}
    }

    void testJavaLangString(java.lang.String string) {
      string.trim();
    }

    void testParentString(ImportResolution.Parent.String string) {
      string.parentStringMethod();
    }

    void testChildString(Child.String string) {
      string.childStringMethod();
    }

    void testSiblingParentString(SiblingParent.String string) {
      string.siblingParentStringMethod();
    }

    void testGenericString(String string) {
      string.genericMethod();
    }
  }
}
