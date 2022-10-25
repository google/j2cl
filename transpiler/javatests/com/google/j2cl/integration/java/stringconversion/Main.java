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
package stringconversion;

import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.getUndefined;

public class Main {
  private static class Person {
    private String firstName;
    private String lastName;

    public Person(String firstName, String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
    }

    @Override
    public String toString() {
      return firstName + " " + lastName;
    }
  }

  @SuppressWarnings("ArrayToString")
  public static void main(String[] args) {
    String locationString = "California, USA";
    Person samPerson = new Person("Sam", "Smith");
    Person nullPerson = null;

    // Object with toString() override.
    String result = samPerson + " is located in " + locationString;
    assertTrue(result.equals("Sam Smith is located in California, USA"));

    // Null Object instance.
    result = nullPerson + " is located in nowhere";
    assertTrue(result.equals("null is located in nowhere"));

    // Two Null String instances.
    String s1 = null;
    String s2 = null;
    String s3 = s1 + s2; // two nullable string instances
    assertTrue((s3.equals("nullnull")));
    s2 += s2; // nullable string compound assignment, plus a nullable string.
    assertTrue((s2.equals("nullnull")));
    s1 += "a"; // nullable string compound assignment, plus a string literal.
    assertTrue((s1.equals("nulla")));

    s1 = null;
    s3 = s1 + s1 + s1 + null + "a";
    assertTrue((s3.equals("nullnullnullnulla")));
    s3 = "a" + s1 + s1 + s1 + null;
    assertTrue((s3.equals("anullnullnullnull")));

    // Char + String
    char c1 = 'F';
    char c2 = 'o';
    int i = c1 + c2;
    assertTrue((c1 + c2 + "o").equals(i + "o"));

    // Boolean boxed type as JS primitive.
    result = new Boolean(true) + " is not " + new Boolean(false);
    assertTrue(result.equals("true is not false"));

    // with integer binary operations
    result = 1 + 2 + "Foo" + 3 + 2;
    assertTrue(result.equals("3Foo32"));

    char[] charArray = new char[] {'f', 'o', 'o'};
    assertFalse("barfoo".equals("bar" + charArray));

    testCharSequenceConcatenation();
    testPrimitiveConcatenation();
    testConcatenationWithUndefined();
  }

  private static class SimpleCharSequence implements CharSequence {
    public String toString() {
      return "some string";
    }

    public int length() {
      throw new UnsupportedOperationException();
    }

    public char charAt(int index) {
      throw new UnsupportedOperationException();
    }

    public CharSequence subSequence(int start, int end) {
      throw new UnsupportedOperationException();
    }
  }

  private static void testCharSequenceConcatenation() {
    StringBuilder stringBuilder = new StringBuilder("foo").append("bar");
    assertTrue((stringBuilder + "baz").equals("foobarbaz"));

    StringBuffer stringBuffer = new StringBuffer("foo").append("bar");
    assertTrue((stringBuffer + "baz").equals("foobarbaz"));

    assertTrue(
        (new SimpleCharSequence() + " from SimpleCharSequence")
            .equals("some string from SimpleCharSequence"));
  }

  private static void testPrimitiveConcatenation() {
    boolean bool = true;
    assertTrue((bool + " is true").equals("true is true"));
    short s = 1;
    assertTrue((s + " is 1").equals("1 is 1"));
    byte b = 1;
    assertTrue((b + " is 1").equals("1 is 1"));
    char c = 'F';
    assertTrue((c + "oo").equals("Foo"));
    int i = 1;
    assertTrue((i + " is 1").equals("1 is 1"));
    long l = 1L;
    assertTrue((l + " is 1").equals("1 is 1"));
    double d = 1.1d;
    assertTrue((d + " is 1.1").equals("1.1 is 1.1"));
    float f = 1.5f;
    assertTrue((f + " is 1.5").equals("1.5 is 1.5"));
  }

  private static void testConcatenationWithUndefined() {
    String s1 = null;
    String s2 = getUndefined(); // undefined in JavaScript
    String s3 = s1 + s2; // two nullable string instances
    assertTrue((s3.equals("nullnull")));
  }
}
