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
package anonymousclass;

import jsinterop.annotations.JsConstructor;

abstract class SomeClass {
  public abstract String foo();

  SomeClass(int i) {}
}

abstract class SomeClassWithStaticMembers {
  public abstract String foo();

  SomeClassWithStaticMembers(int i) {}

  static void staticMethod() {}
}

public class AnonymousClass {
  int i;
  Object o;

  public AnonymousClass(Object a) {
    o =
        new SomeClass(0) {
          Object outer = AnonymousClass.this;
          Object other = a;

          @Override
          public String foo() {
            return "" + i;
          }
        };
  }

  public void main() {
    SomeClass instance =
        new SomeClass(i) {
          Object object = this;
          Object outer = AnonymousClass.this;

          public String foo() {
            return "a";
          }
        };

    // Test for: https://youtrack.jetbrains.com/issue/KT-54349
    SomeClassWithStaticMembers instanceWithStaticMembers =
        new SomeClassWithStaticMembers(i) {
          Object object = this;
          Object outer = AnonymousClass.this;

          public String foo() {
            return "a";
          }
        };
  }
}

interface SomeInterface {
  SomeClass implicitlyStatic =
      new SomeClass(1) {
        public String foo() {
          return "a";
        }
      };
}

// Test case for b/321755877
class JsConstructorClass {
  @JsConstructor
  JsConstructorClass(Object o) {}

  JsConstructorClass() {
    this(trueVar ? new Object() {} : null);
  }

  static boolean trueVar = true;
}

class JsConstructorSubclass extends JsConstructorClass {
  @JsConstructor
  JsConstructorSubclass() {
    super(trueVar ? new Object() {} : null);
  }
}
