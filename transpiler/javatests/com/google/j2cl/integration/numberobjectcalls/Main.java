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
package com.google.j2cl.integration.numberobjectcalls;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  @SuppressWarnings("cast")
  public static void main(String... args) {
    Byte b = new Byte((byte) 1);
    Double d = new Double(1.0);
    Float f = new Float(1.0f);
    Integer i = new Integer(1);
    Long l = new Long(1L);
    Short s = new Short((short) 1);
    Character c = new Character('a');
    Boolean bool = new Boolean(true);

    // equals
    assertTrue((b.equals(b)));
    assertTrue((b.equals(new Byte((byte) 1))));
    assertTrue((d.equals(d)));
    assertTrue((d.equals(new Double(1.0))));
    assertTrue((f.equals(f)));
    assertTrue((f.equals(new Float(1.0f))));
    assertTrue((i.equals(i)));
    assertTrue((i.equals(new Integer(1))));
    assertTrue((l.equals(l)));
    assertTrue((l.equals(new Long(1L))));
    assertTrue((s.equals(s)));
    assertTrue((s.equals(new Short((short) 1))));
    assertTrue((!l.equals(i)));
    assertTrue((!b.equals(d)));
    assertTrue((!b.equals(f)));
    assertTrue((!b.equals(i)));
    assertTrue((!b.equals(l)));
    assertTrue((!b.equals(s)));
    assertTrue((!d.equals(b)));
    assertTrue((!d.equals(f)));
    assertTrue((!d.equals(i)));
    assertTrue((!d.equals(l)));
    assertTrue((!d.equals(s)));
    assertTrue((c.equals(c)));
    assertTrue((c.equals(new Character('a'))));
    assertTrue((!c.equals(new Character('b'))));
    assertTrue((bool.equals(bool)));
    assertTrue((bool.equals(new Boolean(true))));
    assertTrue((!bool.equals(new Boolean(false))));

    // hashCode
    assertTrue((b.hashCode() == b.hashCode()));
    assertTrue((d.hashCode() == d.hashCode()));
    assertTrue((f.hashCode() == f.hashCode()));
    assertTrue((i.hashCode() == i.hashCode()));
    assertTrue((l.hashCode() == l.hashCode()));
    assertTrue((s.hashCode() == s.hashCode()));
    assertTrue((b.hashCode() == i.hashCode()));
    assertTrue((l.hashCode() == i.hashCode()));
    // GWT's JRE Long hashcode comes out different here than the JRE
    // TODO(dankurka): investigate
    // assertTrue((new Long(9223372036854775807L).hashCode() == -2147483648));
    assertTrue((c.hashCode() == c.hashCode()));
    assertTrue((bool.hashCode() == bool.hashCode()));
    assertTrue((bool.hashCode() != new Boolean(false).hashCode()));

    // toString
    assertTrue((b.toString().equals("1")));
    // assertTrue((d.toString().equals("1.0"))); // d.toString().equals("1")
    // assertTrue((f.toString().equals("1.0"))); // f.toString().equals("1")
    assertTrue((i.toString().equals("1")));
    assertTrue((l.toString().equals("1")));
    assertTrue((s.toString().equals("1")));
    assertTrue((bool.toString().equals("true")));

    // getClass
    assertTrue((b.getClass() instanceof Class));
    assertTrue((d.getClass() instanceof Class));
    assertTrue((f.getClass() instanceof Class));
    assertTrue((i.getClass() instanceof Class));
    assertTrue((l.getClass() instanceof Class));
    assertTrue((s.getClass() instanceof Class));
    assertTrue((b.getClass().getName().equals("java.lang.Byte")));
    assertTrue((d.getClass().getName().equals("java.lang.Double")));
    assertTrue((f.getClass().getName().equals("java.lang.Float")));
    assertTrue((i.getClass().getName().equals("java.lang.Integer")));
    assertTrue((l.getClass().getName().equals("java.lang.Long")));
    assertTrue((s.getClass().getName().equals("java.lang.Short")));
    assertTrue((c.getClass().getName().equals("java.lang.Character")));
    assertTrue((bool.getClass().getName().equals("java.lang.Boolean")));

    new SubNumber().test();
  }
}
