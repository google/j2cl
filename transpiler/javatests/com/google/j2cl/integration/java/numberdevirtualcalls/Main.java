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
package numberdevirtualcalls;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    Number b = new Byte((byte) 1);
    assertTrue((b.byteValue() == 1));
    assertTrue((b.doubleValue() == 1.0));
    assertTrue((b.floatValue() == 1.0f));
    assertTrue((b.intValue() == 1));
    assertTrue((b.longValue() == 1L));
    assertTrue((b.shortValue() == 1));

    Byte mb = new Byte((byte) 127);
    assertTrue((mb.byteValue() == 127));
    assertTrue((mb.doubleValue() == 127.0));
    assertTrue((mb.floatValue() == 127.0f));
    assertTrue((mb.intValue() == 127));
    assertTrue((mb.longValue() == 127L));
    assertTrue((mb.shortValue() == 127));

    Number d = new Double(1.1);
    assertTrue((d.byteValue() == 1));
    assertTrue((d.doubleValue() == 1.1));
    // assertTrue((d.floatValue() == 1.1f)); // float is emitted as 1.100000023841858
    assertTrue((d.intValue() == 1));
    assertTrue((d.longValue() == 1L));
    assertTrue((d.shortValue() == 1));

    Double md = new Double(1.7976931348623157E308);
    assertTrue((md.byteValue() == -1));
    assertTrue((md.doubleValue() == 1.7976931348623157E308));
    assertTrue((md.intValue() == 2147483647));
    assertTrue((md.longValue() == 9223372036854775807L));
    assertTrue((md.shortValue() == -1));

    Number f = new Float(1.1f);
    assertTrue((f.byteValue() == 1));
    assertTrue((f.doubleValue() - 1.1 < 1e-7));
    assertTrue((f.floatValue() == 1.1f));
    assertTrue((f.intValue() == 1));
    assertTrue((f.longValue() == 1L));
    assertTrue((f.shortValue() == 1));

    Float mf = new Float(3.4028235E38f);
    assertTrue((mf.byteValue() == -1));
    assertTrue((mf.floatValue() == 3.4028234663852886E38f));
    assertTrue((mf.intValue() == 2147483647));
    assertTrue((mf.longValue() == 9223372036854775807L));
    assertTrue((mf.shortValue() == -1));

    Number i = new Integer(1);
    assertTrue((i.byteValue() == 1));
    assertTrue((i.doubleValue() == 1.0));
    assertTrue((i.floatValue() == 1.0f));
    assertTrue((i.intValue() == 1));
    assertTrue((i.longValue() == 1L));
    assertTrue((i.shortValue() == 1));

    Integer mi = new Integer(2147483647);
    assertTrue((mi.byteValue() == -1));
    assertTrue((mi.doubleValue() == 2.147483647E9));
    assertTrue((mi.intValue() == 2147483647));
    assertTrue((mi.longValue() == 2147483647L));
    assertTrue((mi.shortValue() == -1));

    Number l = new Long(1L);
    assertTrue((l.byteValue() == 1));
    assertTrue((l.doubleValue() == 1.0));
    assertTrue((l.floatValue() == 1.0f));
    assertTrue((l.intValue() == 1));
    assertTrue((l.longValue() == 1L));
    assertTrue((l.shortValue() == 1));

    Long ml = new Long(9223372036854775807L);
    assertTrue((ml.byteValue() == -1));
    assertTrue((ml.doubleValue() == 9.223372036854776E18));
    assertTrue((ml.floatValue() == 9.223372E18f));
    assertTrue((ml.intValue() == -1));
    assertTrue((ml.longValue() == 9223372036854775807L));
    assertTrue((ml.shortValue() == -1));

    Number s = new Short((short) 1);
    assertTrue((s.byteValue() == 1));
    assertTrue((s.doubleValue() == 1.0));
    assertTrue((s.floatValue() == 1.0f));
    assertTrue((s.intValue() == 1));
    assertTrue((s.longValue() == 1L));
    assertTrue((s.shortValue() == 1));

    Short ms = new Short((short) 32767);
    assertTrue((ms.byteValue() == -1));
    assertTrue((ms.doubleValue() == 32767.0));
    assertTrue((ms.floatValue() == 32767.0f));
    assertTrue((ms.intValue() == 32767));
    assertTrue((ms.longValue() == 32767L));
    assertTrue((ms.shortValue() == 32767));

    Character c = new Character('a');
    assertTrue((c.charValue() == 'a'));

    Boolean bool = new Boolean(true);
    assertTrue(bool.booleanValue());
  }
}
