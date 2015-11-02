package com.google.j2cl.transpiler.integration.instancequalifieronstaticfield;

public class Main {
  public static int staticField = 100;
  public static int sideEffectCount = 0;
  public static Main staticMain;

  public static int getStaticValue() {
    return staticField;
  }

  public static Main getStaticMain() {
    return staticMain;
  }

  public Main getWithSideEffects() {
    sideEffectCount++;
    return this;
  }

  public static void main(String... args) {
    Main main = new Main();
    staticMain = main;
    int i = 0;

    // Simplest.
    {
      staticField = 50;
      assert staticField == 50;
    }

    // Simple inline.
    {
      main.staticField = 100;
      assert main.staticField == 100;
      assert main.getStaticValue() == 100;
    }

    // Right hand side assignment.
    {
      i = main.staticField;
      assert i == 100;
      i = main.getStaticValue();
      assert i == 100;
    }

    // Left hand side, simple assignment and side effects
    {
      {
        main.getWithSideEffects().staticField = 200;
        assert staticField == 200;
        assert sideEffectCount == 1;
      }
      {
        main.getWithSideEffects().getStaticMain().staticField = 300;
        assert staticField == 300;
        assert sideEffectCount == 2;
      }
    }

    // Left hand side, compound assignment and side effects
    {
      {
        main.getWithSideEffects().staticField += 100;
        assert staticField == 400;
        assert sideEffectCount == 3;
      }
      {
        main.getWithSideEffects().getStaticMain().staticField += 100;
        assert staticField == 500;
        assert sideEffectCount == 4;
      }
    }

    // Stress
    {
      main.getWithSideEffects()
              .getStaticMain()
              .getWithSideEffects()
              .getStaticMain()
              .getWithSideEffects()
              .getStaticMain()
              .staticField +=
          main.getWithSideEffects()
              .getStaticMain()
              .getWithSideEffects()
              .getStaticMain()
              .getWithSideEffects()
              .getStaticMain()
              .staticField;
      assert staticField == 1000;
      assert sideEffectCount == 10;
    }
  }
}
