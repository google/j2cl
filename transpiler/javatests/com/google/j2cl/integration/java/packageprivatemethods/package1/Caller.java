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
package packageprivatemethods.package1;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import packageprivatemethods.package2.SubChild;
import packageprivatemethods.package2.SubParentPP;
import packageprivatemethods.package2.SubParentPublic;

public class Caller {
  public String callFun(SuperParent p) {
    return p.fun();
  }

  public int callBar(SuperParent p) {
    return p.bar(100);
  }

  public int callFoo(SuperParent p) {
    return p.foo(200, 300);
  }

  public void test() {
    Caller caller = new Caller();
    // Test parent is exposed by child, subparent does not override, subchild does override.
    assertTrue((caller.callFun(new Parent()).equals("Parent")));
    assertTrue((caller.callFun(new SubParentPP()).equals("Parent"))); // not override
    assertTrue((caller.callFun(new SubParentPublic()).equals("Parent"))); // not override
    assertTrue((new SubParentPublic().fun().equals("SubParentPublic")));
    assertTrue((caller.callFun(new Child()).equals("Child"))); // does override
    assertTrue((caller.callFun(new SubChild()).equals("SubChild"))); // does override
    assertTrue((new SubChild().fun().equals("SubChild")));

    // Test super parent is exposed by child, subparent does not override, subchild does override.
    assertTrue((caller.callBar(new Parent()) == 100));
    assertTrue((caller.callBar(new SubParentPP()) == 100)); // not override
    assertTrue((caller.callBar(new SubParentPublic()) == 100)); // not override
    assertTrue((caller.callBar(new Child()) == 101)); // does override
    assertTrue((caller.callBar(new SubChild()) == 102)); // does override

    // Test super parent is exposed by parent, then child, subparent and subchild do override.
    assertTrue((caller.callFoo(new Parent()) == 501));
    assertTrue((caller.callFoo(new SubParentPP()) == 504)); // override
    assertTrue((caller.callFoo(new SubParentPublic()) == 505)); // override
    assertTrue((caller.callFoo(new Child()) == 502)); // override
    assertTrue((caller.callFoo(new SubChild()) == 503)); // override
  }
}
