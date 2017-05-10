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
package com.google.j2cl.transpiler.integration.stringdevirtualcalls;

/**
 * Verifies that String methods on the String class execute properly. It is effectively a test that
 * String method devirtualization is occurring and that the implementation being routed to is
 * proper.
 */
public class Main {
  public static void main(String... args) {
    testJavaLangObjectMethods();
    testComparableMethods();
    testCharSequenceMethods();
    testStringMethods();
  }

  private static void testJavaLangObjectMethods() {
    String string1 = "string1";
    String string2 = "string2";

    // Equals
    assert string1.equals(string1);
    assert !string1.equals(string2);
    assert !string1.equals(new Object());

    // HashCode
    assert string1.hashCode() != -1;
    assert string1.hashCode() == string1.hashCode();
    assert string1.hashCode() != string2.hashCode();

    // ToString
    assert string1.toString() != null;
    assert string1.toString() == string1;
    assert string1.toString() != string2.toString();

    // GetClass
    assert string1.getClass() instanceof Class;
    assert string1.getClass() == string2.getClass();
  }
  
  private static void testComparableMethods() {
    String string1 = "string1";
    String string2 = "string2";

    assert string1.compareTo(string2) == -1;
    assert string1.compareTo(string1) == 0;
    assert string2.compareTo(string1) == 1;
    assert string2.compareTo(string2) == 0;
  }

  private static void testCharSequenceMethods() {
    String string1 = "string1";
    String string2 = "";

    assert string1.length() == 7;
    assert string2.length() == 0;

    assert string1.charAt(0) == 's';
    assert string1.charAt(1) == 't';
    assert string1.charAt(2) == 'r';
    assert string1.charAt(3) == 'i';
    assert string1.charAt(4) == 'n';
    assert string1.charAt(5) == 'g';
    assert string1.charAt(6) == '1';

    assert string1.subSequence(0, 2).equals("st");

    // toString() already tested.
  }

  private static void testStringMethods() {
    String string1 = "string1";

    String whitespaceString = string1 + " ";
    assert whitespaceString.trim().equals(string1);

    whitespaceString = "  " + string1 + " ";
    assert whitespaceString.trim().equals(string1);

    // substring (start)
    assert string1.substring(0).equals("string1");
    assert string1.substring(1).equals("tring1");
    assert string1.substring(2).equals("ring1");
    assert string1.substring(3).equals("ing1");
    assert string1.substring(4).equals("ng1");
    assert string1.substring(5).equals("g1");
    assert string1.substring(6).equals("1");
    assert string1.substring(7).equals("");

    // substring (start, end)
    assert string1.substring(0, 2).equals("st");
    assert string1.substring(1, 5).equals("trin");
  }
}
