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
package allsimplebridges;

public class Main {

  public static void main(String[] args) {
    /**
     * Test examples are different combinations properties toggling in a 3 class hierarchy (child
     * class, parent class, interface).
     *
     * <ul>
     *   <li>childExtendsParent
     *   <li>childImplementsInterface
     *   <li>childSpecializesGeneric
     *   <li>childOverridesParentMethod
     *   <li>childOverridesInterfaceMethod
     *   <li>parentIsAbstract
     *   <li>parentIsGeneric
     *   <li>parentIsJsType
     *   <li>interfaceHasDefaultMethod
     *   <li>interfaceIsJsType
     * </ul>
     *
     * <p>Some combinations don't work yet in J2CL and are temporarily disabled. They are marked
     * with a TODO.
     *
     * <p>Some combinations are not valid Java or result in duplicate output. These have been
     * omitted.
     */
    Tester1.test();
    Tester2.test();
    Tester3.test();
    Tester4.test();
    Tester5.test();
    Tester6.test();
    Tester7.test();
    Tester8.test();
    Tester17.test();
    Tester18.test();
    Tester19.test();
    Tester20.test();
    Tester21.test();
    Tester22.test();
    Tester23.test();
    Tester24.test();
    Tester113.test();
    Tester114.test();
    Tester115.test();
    Tester116.test();
    Tester117.test();
    Tester118.test();
    Tester119.test();
    Tester120.test();
    // Tester129.test();  // illegal jsinterop
    Tester130.test();
    // Tester131.test();  // illegal jsinterop
    Tester132.test();
    Tester133.test();
    Tester134.test();
    Tester135.test();
    Tester136.test();
    // Tester137.test();  // illegal jsinterop
    Tester138.test();
    // Tester139.test();  // illegal jsinterop
    Tester140.test();
    Tester141.test();
    Tester142.test();
    Tester143.test();
    Tester144.test();
    // Tester145.test();  // illegal jsinterop
    Tester146.test();
    // Tester147.test();  // illegal jsinterop
    Tester148.test();
    Tester149.test();
    Tester150.test();
    Tester151.test();
    Tester152.test();
    // Tester153.test();  // illegal jsinterop
    Tester154.test();
    // Tester155.test();  // illegal jsinterop
    Tester156.test();
    Tester157.test();
    Tester158.test();
    Tester159.test();
    Tester160.test();
    // Tester161.test();  // illegal jsinterop
    Tester162.test();
    Tester165.test();
    Tester166.test();
    // Tester169.test();  // illegal jsinterop
    Tester170.test();
    Tester173.test();
    Tester174.test();
    // Tester177.test();  // illegal jsinterop
    Tester178.test();
    Tester181.test();
    Tester182.test();
    // Tester185.test();  // illegal jsinterop
    Tester186.test();
    Tester189.test();
    Tester190.test();
    // Tester209.test();  // illegal jsinterop
    Tester210.test();
    // Tester211.test();  // illegal jsinterop
    Tester212.test();
    Tester213.test();
    Tester214.test();
    Tester215.test();
    Tester216.test();
    // Tester217.test();  // illegal jsinterop
    Tester218.test();
    // Tester219.test();  // illegal jsinterop
    Tester220.test();
    Tester221.test();
    Tester222.test();
    Tester223.test();
    Tester224.test();
    // Tester241.test();  // illegal jsinterop
    Tester242.test();
    Tester245.test();
    Tester246.test();
    // Tester249.test();  // illegal jsinterop
    Tester250.test();
    Tester253.test();
    Tester254.test();
    Tester257.test();
    Tester261.test();
    Tester273.test();
    Tester277.test();
    Tester369.test();
    Tester373.test();
    Tester385.test();
    Tester389.test();
    Tester393.test();
    Tester397.test();
    Tester401.test();
    Tester405.test();
    Tester409.test();
    Tester413.test();
    Tester417.test();
    Tester421.test();
    Tester425.test();
    Tester429.test();
    Tester433.test();
    Tester437.test();
    Tester441.test();
    Tester445.test();
    Tester465.test();
    Tester469.test();
    Tester473.test();
    Tester477.test();
    Tester497.test();
    Tester501.test();
    Tester505.test();
    Tester509.test();
    Tester513.test();
    Tester514.test();
    Tester515.test();
    Tester516.test();
    Tester609.test();
    Tester610.test();
    Tester641.test();
    Tester642.test();
    Tester643.test();
    Tester644.test();
    Tester673.test();
    Tester674.test();
    Tester769.test();
    Tester897.test();
    Tester929.test();
  }
}
