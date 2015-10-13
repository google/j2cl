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
  }
}
