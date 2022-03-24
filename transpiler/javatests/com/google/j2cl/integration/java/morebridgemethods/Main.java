/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package morebridgemethods;

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
    TestCase8939.test();

    // From allsimplebridges.
    Tester5.test();

    // Hand rolled.
    TestCaseHand1.test();
    TestCaseHand2.test();

    // Minimal versions of existing tests.
    BridgeJsMethodMain.test();
    BridgeMethodsMain.test();
    MultipleAbstractParentsMain.test();
    DefaultMethodsMain.test();
    OverwrittenTypeVariablesMain.test();
  }
}
