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
package interfacedevirtualize;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test CharSequence Interface on all devirtualized classes that implement it.
 */
public class CharSequenceTest {

  public static void test() {
    final String s = "string";
    CharSequence cs =
        new CharSequence() {
          @Override
          public int length() {
            return s.length();
          }

          @Override
          public char charAt(int i) {
            return s.charAt(i);
          }

          @Override
          public CharSequence subSequence(int i, int j) {
            return s.subSequence(i, j);
          }

          @Override
          public boolean equals(Object c) {
            return s.equals(c);
          }

          @Override
          public int hashCode() {
            return s.hashCode();
          }

          @Override
          public String toString() {
            return s.toString();
          }
        };

    assertTrue(s.length() == 6);
    assertTrue(cs.length() == 6);

    assertTrue(s.charAt(1) == 't');
    assertTrue(cs.charAt(1) == 't');

    assertTrue(s.subSequence(0, 2).equals("st"));
    assertTrue(cs.subSequence(0, 2).equals("st"));

    assertTrue(s.equals("string"));
    assertTrue(cs.equals("string"));

    assertTrue(s.hashCode() == "string".hashCode());
    assertTrue(cs.hashCode() == "string".hashCode());

    assertTrue(s.toString().equals("string"));
    assertTrue(cs.toString().equals("string"));

    assertTrue(s.getClass() == String.class);
    assertTrue(cs.getClass() != String.class);
  }

  public static void testViaSuper() {
    CharSequence cs =
        new CharSequence() {
          @Override
          public int length() {
            return 0;
          }

          @Override
          public char charAt(int i) {
            return 0;
          }

          @Override
          public CharSequence subSequence(int i, int j) {
            return null;
          }

          @Override
          public boolean equals(Object c) {
            return super.equals(c);
          }

          @Override
          public int hashCode() {
            return super.hashCode();
          }

          @Override
          public String toString() {
            return "sub" + super.toString();
          }
        };

    assertTrue(cs.equals(cs));
    assertTrue(!cs.equals("string"));

    assertTrue(cs.hashCode() == System.identityHashCode(cs));
    assertTrue(cs.hashCode() != 0);

    assertTrue(cs.toString().startsWith("sub"));
  }
}

