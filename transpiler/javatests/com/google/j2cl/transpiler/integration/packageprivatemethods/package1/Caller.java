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
package com.google.j2cl.transpiler.integration.packageprivatemethods.package1;

import com.google.j2cl.transpiler.integration.packageprivatemethods.package2.SubChild;
import com.google.j2cl.transpiler.integration.packageprivatemethods.package2.SubParentPP;
import com.google.j2cl.transpiler.integration.packageprivatemethods.package2.SubParentPublic;

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
    assert (caller.callFun(new Parent()).equals("Parent"));
    assert (caller.callFun(new SubParentPP()).equals("Parent")); // not override
    assert (caller.callFun(new SubParentPublic()).equals("Parent")); // not override
    assert (new SubParentPublic().fun().equals("SubParentPublic"));
    assert (caller.callFun(new Child()).equals("Child")); // does override
    assert (caller.callFun(new SubChild()).equals("SubChild")); // does override
    assert (new SubChild().fun().equals("SubChild"));

    // Test super parent is exposed by child, subparent does not override, subchild does override.
    assert (caller.callBar(new Parent()) == 100);
    assert (caller.callBar(new SubParentPP()) == 100); // not override
    assert (caller.callBar(new SubParentPublic()) == 100); // not override
    assert (caller.callBar(new Child()) == 101); // does override
    assert (caller.callBar(new SubChild()) == 102); // does override

    // Test super parent is exposed by parent, then child, subparent and subchild do override.
    assert (caller.callFoo(new Parent()) == 501);
    assert (caller.callFoo(new SubParentPP()) == 504); // override
    assert (caller.callFoo(new SubParentPublic()) == 505); // override
    assert (caller.callFoo(new Child()) == 502); // override
    assert (caller.callFoo(new SubChild()) == 503); // override
  }
}
