package com.google.j2cl.transpiler.integration.allsimplebridges;

public class Main {

  public static void main(String[] args) {
    /**
     * Test examples are different combinations properties toggling in a 3 class hierarchy (child
     * class, parent class, interface).
     *
     * <ul>
     * <li>childExtendsParent
     * <li>childImplementsInterface
     * <li>childSpecializesGeneric
     * <li>childOverridesParentMethod
     * <li>childOverridesInterfaceMethod
     * <li>parentIsAbstract
     * <li>parentIsGeneric
     * <li>parentIsJsType
     * <li>interfaceHasDefaultMethod
     * <li>interfaceIsJsType
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
    {
      // TODO: fix infinite recursion.
      // Tester113.test();
      // Tester114.test();
    }
    Tester115.test();
    Tester116.test();
    {
      // TODO: fix infinite recursion.
      // Tester117.test();
      // Tester118.test();
    }
    Tester119.test();
    Tester120.test();
    Tester129.test();
    Tester130.test();
    Tester131.test();
    Tester132.test();
    Tester133.test();
    Tester134.test();
    Tester135.test();
    Tester136.test();
    Tester137.test();
    Tester138.test();
    Tester139.test();
    Tester140.test();
    Tester141.test();
    Tester142.test();
    Tester143.test();
    Tester144.test();
    Tester145.test();
    Tester146.test();
    Tester147.test();
    Tester148.test();
    Tester149.test();
    Tester150.test();
    Tester151.test();
    Tester152.test();
    Tester153.test();
    Tester154.test();
    Tester155.test();
    Tester156.test();
    Tester157.test();
    Tester158.test();
    Tester159.test();
    Tester160.test();
    {
      // TODO: fix, assertion fails.
      // Tester161.test();
    }
    Tester162.test();
    Tester165.test();
    Tester166.test();
    {
      // TODO: fix, assertion fails.
      // Tester169.test();
    }
    Tester170.test();
    Tester173.test();
    Tester174.test();
    {
      // TODO: fix, assertion fails.
      // Tester177.test();
    }
    Tester178.test();
    Tester181.test();
    Tester182.test();
    {
      // TODO: fix, assertion fails.
      // Tester185.test();
    }
    Tester186.test();
    Tester189.test();
    Tester190.test();
    {
      // TODO: fix, assertion fails.
      // Tester209.test();
    }
    Tester210.test();
    {
      // TODO: fix, assertion fails.
      // Tester211.test();
    }
    Tester212.test();
    Tester213.test();
    Tester214.test();
    Tester215.test();
    Tester216.test();
    {
      // TODO: fix, assertion fails.
      // Tester217.test();
    }
    Tester218.test();
    {
      // TODO: fix, assertion fails.
      // Tester219.test();
    }
    Tester220.test();
    Tester221.test();
    Tester222.test();
    Tester223.test();
    Tester224.test();
    {
      // TODO: fix, assertion fails.
      // Tester241.test();
    }
    Tester242.test();
    Tester245.test();
    Tester246.test();
    {
      // TODO: fix, assertion fails.
      // Tester249.test();
    }
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
