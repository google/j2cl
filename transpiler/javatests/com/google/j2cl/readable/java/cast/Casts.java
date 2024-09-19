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
package cast;

import java.io.Serializable;

public class Casts {

  public void testCastToClass() {
    Object o = new Object();
    Casts c = (Casts) o;
  }

  public void testCasToInterface() {
    Object o = new Object();
    Serializable s = (Serializable) o;
  }

  public void testCastToBoxedType() {
    Object o = new Integer(1);
    Byte b = (Byte) o;
    Double d = (Double) o;
    Float f = (Float) o;
    Integer i = (Integer) o;
    Long l = (Long) o;
    Short s = (Short) o;
    Number n = (Number) o;
    Character c = (Character) o;
    Boolean bool = (Boolean) o;
  }

  public void testCastToArray() {
    Object foo = (Object[]) null;
    Object bar = (Object[][]) null;
  }

  private void functionForInstanceofTest() {}

  public void testCastObjectAfterInstanceOf() {
    Object o = new Object();
    if (o instanceof Casts) {
      Casts c = (Casts) o;
    }
    if (o instanceof Casts) {
      Casts cAvoidsCastsTo = (Casts) o;
      o = new Object();
      Casts cNotAvoidCastsTo = (Casts) o;
    }
    if (o instanceof Casts) {
      Casts cAvoidsCastsTo = (Casts) o;
      o = new Foo();
      Casts cNotAvoidCastsTo = (Casts) o;
    }
    if (o instanceof Casts) {
      Casts cAvoidsCastsTo = (Casts) o;
      o = new Object();
      Casts cAvoidCastsTo = (Casts) o;
    }
    if (o instanceof Casts) {
      Casts cAvoidsCastsTo = (Casts) o;
      functionForInstanceofTest();
      Casts cNotAvoidCastsTo = (Casts) o;
    }
  }

  /** Class for testing purposes */
  private class Foo {
    public Object field = new Object();

    public Object method() {
      return new Object();
    }
  }

  public void testCastFieldAfterInstanceOf() {
    Foo foo = new Foo();
    if (foo.field instanceof Casts) {
      Casts c = (Casts) foo.field;
    }
    if (foo.field instanceof Casts) {
      Casts cAvoidsCastsTo = (Casts) foo.field;
      foo.field = new Object();
      Casts cNotAvoidCastsTo = (Casts) foo.field;
    }
    if (foo.field instanceof Casts) {
      Casts cAvoidsCastsTo = (Casts) foo.field;
      foo = new Foo();
      Casts cNotAvoidCastsTo = (Casts) foo.field;
    }
    if (foo.field instanceof Casts) {
      Casts cAvoidsCastsTo = (Casts) foo.field;
      functionForInstanceofTest();
      Casts cNotAvoidCastsTo = (Casts) foo.field;
    }
  }

  public void testCaseMethodAfterInstanceOf() {
    Foo foo = new Foo();
    if (foo.method() instanceof Casts) {
      Casts cNotAvoidCastsTo = (Casts) foo.method();
    }
  }

  public void testPrecedence() {
    Object foo = "foo";
    Object bar = "bar";
    Integer notString = 123;
    String s1 = (String) (false ? foo : bar);
    String s2 = (String) ("foo" + notString);
  }
}
