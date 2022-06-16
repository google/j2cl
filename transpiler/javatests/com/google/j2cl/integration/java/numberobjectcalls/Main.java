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
package numberobjectcalls;

import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  @SuppressWarnings("cast")
  public static void main(String... args) {
    testEquals();
    testHashCode();
    testToString();
    testGetClass();
    testNumberSubclass();
  }

  private static final Byte B = new Byte((byte) 1);
  private static final Double D = new Double(1.0);
  private static final Float F = new Float(1.0f);
  private static final Integer I = new Integer(1);
  private static final Long L = new Long(1L);
  private static final Short S = new Short((short) 1);
  private static final Character C = new Character('a');
  private static final Boolean BOOL = new Boolean(true);

  private static void testEquals() {
    // equals
    assertTrue(B.equals(B));
    assertTrue(B.equals(new Byte((byte) 1)));
    assertTrue(D.equals(D));
    assertTrue(D.equals(new Double(1.0)));
    assertTrue(F.equals(F));
    assertTrue(F.equals(new Float(1.0f)));
    assertTrue(I.equals(I));
    assertTrue(I.equals(new Integer(1)));
    assertTrue(L.equals(L));
    assertTrue(L.equals(new Long(1L)));
    assertTrue(S.equals(S));
    assertTrue(S.equals(new Short((short) 1)));
    assertFalse(L.equals(I));
    assertFalse(B.equals(D));
    assertFalse(B.equals(F));
    assertFalse(B.equals(I));
    assertFalse(B.equals(L));
    assertFalse(B.equals(S));
    assertFalse(D.equals(B));
    assertFalse(D.equals(F));
    assertFalse(D.equals(I));
    assertFalse(D.equals(L));
    assertFalse(D.equals(S));
    assertTrue(C.equals(C));
    assertTrue(C.equals(new Character('a')));
    assertFalse(C.equals(new Character('b')));
    assertTrue(BOOL.equals(BOOL));
    assertTrue(BOOL.equals(new Boolean(true)));
    assertFalse(BOOL.equals(new Boolean(false)));
  }

  private static void testHashCode() {
    // hashCode
    assertTrue(B.hashCode() == B.hashCode());
    assertTrue(D.hashCode() == D.hashCode());
    assertTrue(F.hashCode() == F.hashCode());
    assertTrue(I.hashCode() == I.hashCode());
    assertTrue(L.hashCode() == L.hashCode());
    assertTrue(S.hashCode() == S.hashCode());
    assertTrue(B.hashCode() == I.hashCode());
    assertTrue(L.hashCode() == I.hashCode());
    // GWT's JRE Long hashcode comes out different here than the JRE
    // TODO(dankurka): investigate
    // assertTrue((new Long(9223372036854775807L).hashCode() == -2147483648));
    assertTrue(C.hashCode() == C.hashCode());
    assertTrue(BOOL.hashCode() == BOOL.hashCode());
    assertTrue(BOOL.hashCode() != new Boolean(false).hashCode());
  }

  private static void testToString() {
    assertTrue(B.toString().equals("1"));
    // assertTrue((d.toString().equals("1.0"))); // d.toString().equals("1")
    // assertTrue((f.toString().equals("1.0"))); // f.toString().equals("1")
    assertTrue(I.toString().equals("1"));
    assertTrue(L.toString().equals("1"));
    assertTrue(S.toString().equals("1"));
    assertTrue(BOOL.toString().equals("true"));
  }

  private static void testGetClass() {
    assertTrue(B.getClass() instanceof Class);
    assertTrue(D.getClass() instanceof Class);
    assertTrue(F.getClass() instanceof Class);
    assertTrue(I.getClass() instanceof Class);
    assertTrue(L.getClass() instanceof Class);
    assertTrue(S.getClass() instanceof Class);
    assertTrue(B.getClass().getName().equals("java.lang.Byte"));
    assertTrue(D.getClass().getName().equals("java.lang.Double"));
    assertTrue(F.getClass().getName().equals("java.lang.Float"));
    assertTrue(I.getClass().getName().equals("java.lang.Integer"));
    assertTrue(L.getClass().getName().equals("java.lang.Long"));
    assertTrue(S.getClass().getName().equals("java.lang.Short"));
    assertTrue(C.getClass().getName().equals("java.lang.Character"));
    assertTrue(BOOL.getClass().getName().equals("java.lang.Boolean"));
  }

  private static class SubNumber extends Number {
    @Override
    public int intValue() {
      return 0;
    }

    @Override
    public long longValue() {
      return 0;
    }

    @Override
    public float floatValue() {
      return 0;
    }

    @Override
    public double doubleValue() {
      return 0;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof SubNumber;
    }

    @Override
    public int hashCode() {
      return 100;
    }

    @Override
    public String toString() {
      return "SubNumber";
    }
  }

  private static void testNumberSubclass() {
    Number sn1 = new SubNumber();
    Number sn2 = new SubNumber();
    assertTrue(sn1.equals(sn2));
    assertFalse(sn1.equals(new Object()));

    assertTrue(sn1.hashCode() == 100);
    assertTrue(sn2.hashCode() == 100);

    assertTrue(sn1.toString().equals(sn1.toString()));

    assertTrue(sn1.getClass() instanceof Class);
    assertTrue(sn1.getClass().equals(sn2.getClass()));
  }
}
