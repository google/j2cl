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
package com.google.j2cl.transpiler.integration.numberobjectcalls;

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
    assert (b.equals(b));
    assert (b.equals(new Byte((byte) 1)));
    assert (d.equals(d));
    assert (d.equals(new Double(1.0)));
    assert (f.equals(f));
    assert (f.equals(new Float(1.0f)));
    assert (i.equals(i));
    assert (i.equals(new Integer(1)));
    assert (l.equals(l));
    assert (l.equals(new Long(1L)));
    assert (s.equals(s));
    assert (s.equals(new Short((short) 1)));
    assert (!l.equals(i));
    assert (!b.equals(d));
    assert (!b.equals(f));
    assert (!b.equals(i));
    assert (!b.equals(l));
    assert (!b.equals(s));
    assert (!d.equals(b));
    assert (!d.equals(f));
    assert (!d.equals(i));
    assert (!d.equals(l));
    assert (!d.equals(s));
    assert (c.equals(c));
    assert (c.equals(new Character('a')));
    assert (!c.equals(new Character('b')));
    assert (bool.equals(bool));
    assert (bool.equals(new Boolean(true)));
    assert (!bool.equals(new Boolean(false)));

    // hashCode
    assert (b.hashCode() == b.hashCode());
    assert (d.hashCode() == d.hashCode());
    assert (f.hashCode() == f.hashCode());
    assert (i.hashCode() == i.hashCode());
    assert (l.hashCode() == l.hashCode());
    assert (s.hashCode() == s.hashCode());
    assert (b.hashCode() == i.hashCode());
    assert (l.hashCode() == i.hashCode());
    // GWT's JRE Long hashcode comes out different here than the JRE
    // TODO(dankurka): investigate
    // assert (new Long(9223372036854775807L).hashCode() == -2147483648);
    assert (c.hashCode() == c.hashCode());
    assert (bool.hashCode() == bool.hashCode());
    assert (bool.hashCode() != new Boolean(false).hashCode());

    // toString
    assert (b.toString().equals("1"));
    // assert (d.toString().equals("1.0")); // d.toString().equals("1")
    // assert (f.toString().equals("1.0")); // f.toString().equals("1")
    assert (i.toString().equals("1"));
    assert (l.toString().equals("1"));
    assert (s.toString().equals("1"));
    assert (bool.toString().equals("true"));

    // getClass
    assert (b.getClass() instanceof Class);
    assert (d.getClass() instanceof Class);
    assert (f.getClass() instanceof Class);
    assert (i.getClass() instanceof Class);
    assert (l.getClass() instanceof Class);
    assert (s.getClass() instanceof Class);
    assert (b.getClass().getName().equals("java.lang.Byte"));
    // assert (d.getClass().getName().equals("java.lang.Double"));
    assert (f.getClass().getName().equals("java.lang.Float"));
    assert (i.getClass().getName().equals("java.lang.Integer"));
    assert (l.getClass().getName().equals("java.lang.Long"));
    assert (s.getClass().getName().equals("java.lang.Short"));
    assert (c.getClass().getName().equals("java.lang.Character"));
    // assert (bool.getClass().getName().equals("java.lang.Boolean"));

    new SubNumber().test();
  }
}
