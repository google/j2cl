package com.google.j2cl.transpiler.integration.staticnestedclass;

public class Main {
  public static class ParentThis {
    public static class StaticNestedClass {
      public int field = 1;
    }
  }

  public static class ParentThat {
    public static class StaticNestedClass {
      public int field = 2;
    }
  }

  public static void main(String... args) {
    ParentThis.StaticNestedClass thisClass = new ParentThis.StaticNestedClass();
    assert thisClass instanceof ParentThis.StaticNestedClass;
    assert thisClass.field == 1;

    ParentThat.StaticNestedClass thatClass = new ParentThat.StaticNestedClass();
    assert thatClass instanceof ParentThat.StaticNestedClass;
    assert thatClass.field == 2;
  }
}
