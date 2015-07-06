package com.google.j2cl.transpiler.integration.arraycastsinglesideeffect;

public class Main {
  @SuppressWarnings("unused")
  public static void main(String... args) {
    Main main = new Main();

    Object[] object = (Object[]) main.getWithSideEffect();
    assert main.sideEffectCount == 1;

    object = (Object[]) main.getWithSideEffect();
    assert main.sideEffectCount == 2;

    object = (Object[]) main.getWithSideEffect();
    assert main.sideEffectCount == 3;
  }

  private int sideEffectCount = 0;

  private Object getWithSideEffect() {
    sideEffectCount++;
    return new Object[1];
  }
}
