package com.google.j2cl.transpiler.readable.instancequalifieronstaticfield;

public class InstanceQualifiers {
  public static int staticField = 100;
  public static int sideEffectCount = 0;

  public static int getStaticValue() {
    return staticField;
  }

  public static InstanceQualifiers getStaticInstanceQualifiers() {
    return null;
  }

  public static void main(String... args) {
    InstanceQualifiers main = new InstanceQualifiers();
    int i = 0;

    {
      staticField = 100;
    }

    {
      main.staticField = 100;
      main.getStaticInstanceQualifiers().staticField = 300;
    }

    {
      i = main.staticField;
      i = main.getStaticValue();
      i = main.getStaticInstanceQualifiers().staticField;
    }

    {
      main.staticField += 100;
      main.getStaticInstanceQualifiers().staticField += 100;
    }

    {
      main.getStaticInstanceQualifiers().getStaticInstanceQualifiers().staticField +=
          main.getStaticInstanceQualifiers().getStaticInstanceQualifiers().getStaticValue();
    }
  }
}
