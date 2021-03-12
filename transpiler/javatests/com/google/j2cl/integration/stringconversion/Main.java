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
package com.google.j2cl.integration.stringconversion;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

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

    // Boxable primitive
    result = 9999 + " is greater than " + 8888;
    assertTrue(result.equals("9999 is greater than 8888"));

    // Devirtualized primitive
    result = true + " is not " + false;
    assertTrue(result.equals("true is not false"));
    result = new Boolean(true) + " is not " + new Boolean(false);
    assertTrue(result.equals("true is not false"));

    // Two Null String instances.
    String s1 = null;
    String s2 = (new String[0])[0]; // undefined
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

    // with integer binary operations
    result = 1 + 2 + "Foo" + 3 + 2;
    assertTrue(result.equals("3Foo32"));
  }
}
