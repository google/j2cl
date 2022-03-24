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
package cascadedstaticinit;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test cascaded static initializers run only once.
 */
public class Main {
  public static void main(String... args) {
    // Bar's initializer defines it to be 5;
    assertTrue(getBar() == 5);
    // Foo's initializer defines it to be bar * 5;
    assertTrue(getFoo() == 25);

    // If you change Bar then Foo won't update because it's initializer only runs once.
    setBar(10);
    assertTrue(getBar() == 10);
    assertTrue(getFoo() == 25);
    assertTrue(getFoo() == 25);
    assertTrue(getFoo() == 25);
  }

  public static int getBar() {
    // Since this is a static function in Main it will attempt to run Main's $clinit and the getter
    // for the static ValueHolder.bar property will attempt to run it's own $clinit.
    return ValueHolder.bar;
  }

  public static int getFoo() {
    // Since this is a static function in Main it will attempt to run Main's $clinit and the getter
    // for the static ValueHolder.foo property will attempt to run it's own $clinit.
    return ValueHolder.foo;
  }

  public static void setBar(int value) {
    // Since this is a static function in Main it will attempt to run Main's $clinit and the setter
    // for the static ValueHolder.bar property will attempt to run it's own $clinit.
    ValueHolder.bar = value;
  }
}
