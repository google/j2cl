package com.google.j2cl.transpiler.integration.unusedmultiexpressionreturn;

/**
 * Unused multi expression return values.
 * <p>
 * If we do not strip the return in this scenario then JSComp will flag the code as suspicious.
 */
public class Main {
  @SuppressWarnings("unused")
  private static int field = 100;

  public static void main(String[] args) {
    @SuppressWarnings("unused")
    int variable = 100;
    int[] arrayVariable = new int[] {100};

    // If these are normalized to returning a value that is ignored then JSComp will reject.
    variable++;
    variable--;
    arrayVariable[0]++;
    arrayVariable[0]--;
    Main.field++;
    Main.field--;
  }
}
