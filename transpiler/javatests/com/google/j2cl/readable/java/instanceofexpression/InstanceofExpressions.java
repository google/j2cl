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
package instanceofexpression;

import java.io.Serializable;

public class InstanceofExpressions {

  public void testInstanceofClass() {
    Object object = new InstanceofExpressions();
    boolean b;
    b = object instanceof Object;
    b = object instanceof InstanceofExpressions;
    b = object instanceof String;
  }

  public void testInstanceofInterface() {
    Object o = new Object();
    boolean b;
    b = o instanceof Serializable;
    b = new Serializable() {} instanceof Serializable;
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

    Double d = null;
    a = d instanceof Object;
    a = d instanceof Number;
    a = d instanceof Double;
  }

  public void testInstanceOfArray() {
    Object object = new Object();
    Object[] objectArray = new Object[0];
    Object[][] objectDoubleArray = new Object[0][];
    String[] stringArray = new String[0];

    boolean a;
    a = object instanceof Object[];
    a = object instanceof String[][];
    a = object instanceof Object[];
    a = object instanceof String[][];

    a = objectArray instanceof Object[];
    a = objectArray instanceof String[];
    a = objectArray instanceof Object[][];
    a = objectArray instanceof String[][];

    a = objectDoubleArray instanceof Object[];
    a = objectDoubleArray instanceof Object[][];
    a = objectDoubleArray instanceof String[][];

    a = stringArray instanceof Object[];
    a = stringArray instanceof String[];

    a = object instanceof byte[];
    a = object instanceof short[];
    a = object instanceof int[];
    a = object instanceof long[];
    a = object instanceof float[];
    a = object instanceof double[];
    a = object instanceof char[];
    a = object instanceof boolean[];
  }

  public void testPrecedence() {
    boolean b = (false ? "foo" : "bar") instanceof String;
  }
}
