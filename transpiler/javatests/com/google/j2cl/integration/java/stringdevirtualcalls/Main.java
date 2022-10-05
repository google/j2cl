/*
 * Copyright 2015 Google Inc.
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
package stringdevirtualcalls;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Verifies that String methods on the String class execute properly. It is effectively a test that
 * String method devirtualization is occurring and that the implementation being routed to is
 * proper.
 */
public class Main {
  public static void main(String... args) {
    testEquals();
    testHashCode();
    testToString();
    testGetClass();
    testComparableMethods();
    testCharSequenceMethods();
    testStringMethods();
  }

  private static void testEquals() {
    String string1 = "string1";
    String string2 = "string2";

    assertTrue(string1.equals(string1));
    assertTrue(!string1.equals(string2));
    assertTrue(!string1.equals(new Object()));
  }

  private static void testHashCode() {
    String string1 = "string1";
    String string2 = "string2";

    assertTrue(string1.hashCode() != -1);
    assertTrue(string1.hashCode() == string1.hashCode());
    assertTrue(string1.hashCode() != string2.hashCode());
  }

  private static void testToString() {
    String string1 = "string1";
    String string2 = "string2";

    assertTrue(string1.toString() != null);
    assertTrue(string1.toString() == string1);
    assertTrue(string1.toString() != string2.toString());
  }

  private static void testGetClass() {
    String string1 = "string1";
    String string2 = "string2";

    assertTrue(string1.getClass() instanceof Class);
    assertTrue(string1.getClass() == string2.getClass());
  }

  @SuppressWarnings("SelfComparison")
  private static void testComparableMethods() {
    String string1 = "string1";
    String string2 = "string2";

    assertTrue(string1.compareTo(string2) == -1);
    assertTrue(string1.compareTo(string1) == 0);
    assertTrue(string2.compareTo(string1) == 1);
    assertTrue(string2.compareTo(string2) == 0);
  }

  private static void testCharSequenceMethods() {
    String string1 = "string1";
    String string2 = "";

    assertTrue(string1.length() == 7);
    assertTrue(string2.length() == 0);

    assertTrue(string1.charAt(0) == 's');
    assertTrue(string1.charAt(1) == 't');
    assertTrue(string1.charAt(2) == 'r');
    assertTrue(string1.charAt(3) == 'i');
    assertTrue(string1.charAt(4) == 'n');
    assertTrue(string1.charAt(5) == 'g');
    assertTrue(string1.charAt(6) == '1');

    assertTrue(string1.subSequence(0, 2).equals("st"));
  }

  @SuppressWarnings("SubstringOfZero")
  private static void testStringMethods() {
    String string1 = "string1";

    String whitespaceString = string1 + " ";
    assertTrue(whitespaceString.trim().equals(string1));

    whitespaceString = "  " + string1 + " ";
    assertTrue(whitespaceString.trim().equals(string1));

    // substring (start)
    assertTrue(string1.substring(0).equals("string1"));
    assertTrue(string1.substring(1).equals("tring1"));
    assertTrue(string1.substring(2).equals("ring1"));
    assertTrue(string1.substring(3).equals("ing1"));
    assertTrue(string1.substring(4).equals("ng1"));
    assertTrue(string1.substring(5).equals("g1"));
    assertTrue(string1.substring(6).equals("1"));
    assertTrue(string1.substring(7).equals(""));

    // substring (start, end)
    assertTrue(string1.substring(0, 2).equals("st"));
    assertTrue(string1.substring(1, 5).equals("trin"));
  }
}
