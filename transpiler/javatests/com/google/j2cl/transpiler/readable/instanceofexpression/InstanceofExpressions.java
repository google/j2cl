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
package com.google.j2cl.transpiler.readable.instanceofexpression;

import java.io.Serializable;

public class InstanceofExpressions {

  public void testInstanceofClass() {
    Object object = new InstanceofExpressions();
    assert object instanceof InstanceofExpressions;
    assert object instanceof Object;
    assert !(object instanceof String);
  }

  public boolean testInstanceofInterface() {
    Object o = new Object();
    return o instanceof Serializable;
  }

  public void testInstanceofBoxedType() {
    Object b = new Integer(1);

    boolean a = b instanceof Byte;
    a = b instanceof Double;
    a = b instanceof Float;
    a = b instanceof Integer;
    a = b instanceof Long;
    a = b instanceof Short;
    a = b instanceof Number;
    a = b instanceof Character;
    a = b instanceof Boolean;
  }

  public void testInstanceOfArray() {
    Object object = new Object();

    boolean a = object instanceof Object[];
    boolean b = object instanceof Object[][];
  }
}
