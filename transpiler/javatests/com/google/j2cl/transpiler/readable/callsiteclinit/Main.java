package com.google.j2cl.transpiler.readable.callsiteclinit;

// We should be inserting a Yo.clinit() in the constructor for Main, the clinit of Main and in
// Main.asdf.
public class Main {
  public static class Yo {
    public static String a = "asdf";
  }

  public static class Yar {
    public static String b = "qwerty";
  }

  public static class Yas {
    public static String c = "string";
  }

  private static String staticField = Yo.a;

  private static String staticWithBlock;

  static {
    String temp = Yas.c;
    {
      // Test inner block.
      staticWithBlock = Yar.b;
    }
  }

  private String instanceField = Yo.a;
  private String instanceWithBlock;

  {
    instanceWithBlock = Yar.b;
  }

  public void asdf() {
    String insideMethod = Yo.a;
  }
}
