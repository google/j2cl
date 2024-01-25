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

public class ImportResolution {
  static class Parent {
    class String {
      void local() {}

      void testJavaLang(java.lang.String string) {
        string.trim();
      }

      void testLocal(String string) {
        string.local();
      }
    }

    void testJavaLang(java.lang.String string) {
      string.trim();
    }

    void testLocal(String string) {
      string.local();
    }
  }

  static class Child extends Parent {
    @Override
    void testJavaLang(java.lang.String string) {
      string.trim();
    }

    @Override
    void testLocal(String string) {
      string.local();
    }
  }

  static class GrandChild extends Child {
    @Override
    void testJavaLang(java.lang.String string) {
      string.trim();
    }

    @Override
    void testLocal(String string) {
      string.local();
    }
  }
}
