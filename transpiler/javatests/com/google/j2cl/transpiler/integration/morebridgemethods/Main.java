package com.google.j2cl.transpiler.integration.morebridgemethods;

/**
 * This is a large but not *too-large* list of bridge method construction scenarios. All of these
 * different failure modes were discovered when rewriting bridge method construction and it is
 * believed that together they provide coverage for almost all bridge method problems.
 */
public class Main {

  public static void main(String... args) {
    // From regression/mediumbridgeshards.
    TestCase3371.test();
    TestCase718.test();
    TestCase10015.test();
    TestCase10020.test();
    TestCase10092.test();
    TestCase3783.test();
    TestCase8166.test();
    TestCase10332.test();
    TestCase615.test();
    TestCase6104.test();
    TestCase9366.test();
    TestCase5814.test();
    TestCase6674.test();
    TestCase9226.test();
    TestCase55.test();
    TestCase3352.test();
    TestCase4787.test();
    TestCase1596.test();
    TestCase2123.test();
    TestCase4182.test();
    TestCase5828.test();
    TestCase6.test();
    TestCase8435.test();

    // From allsimplebridges.
    Tester5.test();

    // Hand rolled.
    TestCaseHand1.test();
    TestCaseHand2.test();

    // Minimal versions of existing tests.
    // BridgeJsMethodMain.test();
    BridgeMethodsMain.test();
    MultipleAbstractParentsMain.test();
    DefaultMethodsMain.test();
    OverwrittenTypeVariablesMain.test();
  }
}
