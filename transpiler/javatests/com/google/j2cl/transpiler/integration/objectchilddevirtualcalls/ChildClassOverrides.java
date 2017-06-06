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
package com.google.j2cl.transpiler.integration.objectchilddevirtualcalls;

public class ChildClassOverrides {
  public boolean equals(Object o) {
    return o instanceof ChildClassOverrides;
  }

  public int hashCode() {
    return 100;
  }

  public String toString() {
    return "ChildClassOverrides";
  }

  /**
   * Test object method calls with implicit qualifiers and explicit this.
   */
  public void test() {
    assert (equals(new ChildClassOverrides()));
    assert (!equals(new Object()));
    assert (this.equals(new ChildClassOverrides()));
    assert (!this.equals(new Object()));
    assert (this.hashCode() == 100);
    assert (hashCode() == 100);
    assert (this.toString().equals("ChildClassOverrides"));
    assert (toString().equals("ChildClassOverrides"));
    assert (this.getClass() instanceof Class);
    assert (getClass() instanceof Class);
  }
}
