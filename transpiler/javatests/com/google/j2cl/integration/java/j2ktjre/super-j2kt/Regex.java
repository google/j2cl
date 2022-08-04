/*
 * Copyright 2022 Google Inc.
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
package j2ktjre;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Regex {

  static void testRegex() {
    testFind();
    testReplace();
  }

  static void testFind() {
    Pattern pattern = Pattern.compile("Hello.World");
    Matcher matcher = pattern.matcher("~~~Hello-World~~~");
    assertTrue(matcher.find());
    assertEquals(3, matcher.start());
  }

  static void testReplace() {
    Pattern pattern = Pattern.compile("cat");
    Matcher matcher = pattern.matcher("one cat two cats in the yard");
    assertEquals("one dog two cats in the yard", matcher.replaceFirst("dog"));
    assertEquals("one dog two dogs in the yard", matcher.replaceAll("dog"));
  }
}
