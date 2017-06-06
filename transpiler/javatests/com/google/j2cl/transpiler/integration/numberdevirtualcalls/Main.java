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
package com.google.j2cl.transpiler.integration.numberdevirtualcalls;

public class Main {
  public static void main(String[] args) {
    Number b = new Byte((byte) 1);
    assert (b.byteValue() == 1);
    assert (b.doubleValue() == 1.0);
    assert (b.floatValue() == 1.0f);
    assert (b.intValue() == 1);
    assert (b.longValue() == 1L);
    assert (b.shortValue() == 1);

    Byte mb = new Byte((byte) 127);
    assert (mb.byteValue() == 127);
    assert (mb.doubleValue() == 127.0);
    assert (mb.floatValue() == 127.0f);
    assert (mb.intValue() == 127);
    assert (mb.longValue() == 127L);
    assert (mb.shortValue() == 127);

    Number d = new Double(1.1);
    assert (d.byteValue() == 1);
    assert (d.doubleValue() == 1.1);
    // assert (d.floatValue() == 1.1f); // float is emitted as 1.100000023841858
    assert (d.intValue() == 1);
    assert (d.longValue() == 1L);
    assert (d.shortValue() == 1);

    Double md = new Double(1.7976931348623157E308);
    assert (md.byteValue() == -1);
    assert (md.doubleValue() == 1.7976931348623157E308);
    assert (md.intValue() == 2147483647);
    assert (md.longValue() == 9223372036854775807L);
    assert (md.shortValue() == -1);

    Number f = new Float(1.1f);
    assert (f.byteValue() == 1);
    assert (f.doubleValue() - 1.1 < 1e-7);
    assert (f.floatValue() == 1.1f);
    assert (f.intValue() == 1);
    assert (f.longValue() == 1L);
    assert (f.shortValue() == 1);

    Float mf = new Float(3.4028235E38f);
    assert (mf.byteValue() == -1);
    assert (mf.floatValue() == 3.4028234663852886E38f);
    assert (mf.intValue() == 2147483647);
    assert (mf.longValue() == 9223372036854775807L);
    assert (mf.shortValue() == -1);

    Number i = new Integer(1);
    assert (i.byteValue() == 1);
    assert (i.doubleValue() == 1.0);
    assert (i.floatValue() == 1.0f);
    assert (i.intValue() == 1);
    assert (i.longValue() == 1L);
    assert (i.shortValue() == 1);

    Integer mi = new Integer(2147483647);
    assert (mi.byteValue() == -1);
    assert (mi.doubleValue() == 2.147483647E9);
    assert (mi.intValue() == 2147483647);
    assert (mi.longValue() == 2147483647L);
    assert (mi.shortValue() == -1);

    Number l = new Long(1L);
    assert (l.byteValue() == 1);
    assert (l.doubleValue() == 1.0);
    assert (l.floatValue() == 1.0f);
    assert (l.intValue() == 1);
    assert (l.longValue() == 1L);
    assert (l.shortValue() == 1);

    Long ml = new Long(9223372036854775807L);
    assert (ml.byteValue() == -1);
    assert (ml.doubleValue() == 9.223372036854776E18);
    assert (ml.floatValue() == 9.223372E18f);
    assert (ml.intValue() == -1);
    assert (ml.longValue() == 9223372036854775807L);
    assert (ml.shortValue() == -1);

    Number s = new Short((short) 1);
    assert (s.byteValue() == 1);
    assert (s.doubleValue() == 1.0);
    assert (s.floatValue() == 1.0f);
    assert (s.intValue() == 1);
    assert (s.longValue() == 1L);
    assert (s.shortValue() == 1);

    Short ms = new Short((short) 32767);
    assert (ms.byteValue() == -1);
    assert (ms.doubleValue() == 32767.0);
    assert (ms.floatValue() == 32767.0f);
    assert (ms.intValue() == 32767);
    assert (ms.longValue() == 32767L);
    assert (ms.shortValue() == 32767);

    Character c = new Character('a');
    assert (c.charValue() == 'a');

    Boolean bool = new Boolean(true);
    assert bool.booleanValue();
  }
}
