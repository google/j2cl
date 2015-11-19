package com.google.j2cl.transpiler.integration.stringconversion;

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
    assert result.equals("Sam Smith is located in California, USA");

    // Null Object instance.
    result = nullPerson + " is located in nowhere";
    assert result.equals("null is located in nowhere");

    // Boxable primitive
    result = 9999 + " is greater than " + 8888;
    assert result.equals("9999 is greater than 8888");

    // Devirtualized primitive
    result = true + " is not " + false;
    assert result.equals("true is not false");
    result = new Boolean(true) + " is not " + new Boolean(false);
    assert result.equals("true is not false");

    // Two Null String instances.
    String s1 = null;
    String s2 = null;
    String s3 = s1 + s2; // two nullable string instances
    assert (s3.equals("nullnull"));
    s2 += s2; // nullable string compound assignment, plus a nullable string.
    assert (s2.equals("nullnull"));
    s1 += "a"; // nullable string compound assignment, plus a string literal.
    assert (s1.equals("nulla"));

    s1 = null;
    s3 = s1 + s1 + s1 + null + "a";
    assert (s3.equals("nullnullnullnulla"));
    s3 = "a" + s1 + s1 + s1 + null;
    assert (s3.equals("anullnullnullnull"));

    // Char + String
    char c1 = 'F';
    char c2 = 'o';
    assert (c1 + c2 + "o").equals("Foo");
  }
}
