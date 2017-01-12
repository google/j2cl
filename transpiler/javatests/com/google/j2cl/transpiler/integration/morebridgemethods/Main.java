package com.google.j2cl.transpiler.integration.morebridgemethods;

/**
 * This is a large but not *too-large* list of bridge method construction scenarios. All of these
 * different failure modes were discovered when rewriting bridge method construction and it is
 * believed that together they provide coverage for almost all bridge method problems.
 */
public class Main {

  public static void main(String... args) {
    /**
     * Be warned, the existing implementation of bridge method construction contains some spooky
     * behavior. If you enable BridgeJsMethodMain.test() then TestCase6.test() will suddenly break.
     * Do not be surprised when this happens. And right now BridgeJsMethodMain.test() is broken but
     * the test from which it is extracted ('bridgejsmethod') currently passes when run by itself.
     */

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
    // TestCase6674.test();  // fails right now
    TestCase9226.test();
    // TestCase55.test();  // fails right now
    TestCase3352.test();
    TestCase4787.test();
    // TestCase1596.test();  // fails right now
    TestCase2123.test();
    TestCase4182.test();
    TestCase5828.test();
    TestCase6.test();
    // TestCase8435.test();  // fails right now

    // From allsimplebridges.
    Tester5.test();

    // Hand rolled.
    TestCaseHand1.test();
    // TestCaseHand2.test();  // fails right now

    // Minimal versions of existing tests.
    // BridgeJsMethodMain.test();  // fails right now
    BridgeMethodsMain.test();
    MultipleAbstractParentsMain.test();
    DefaultMethodsMain.test();
    OverwrittenTypeVariablesMain.test();
  }
}
