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
package numberchilddevirtualcalls;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

class NumberChild extends Number {
  private double x;
  private double y;

  public NumberChild(double x, double y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public int intValue() {
    return (int) (x + y);
  }

  @Override
  public long longValue() {
    return (long) (x + y);
  }

  @Override
  public float floatValue() {
    return (float) (x + y);
  }

  @Override
  public double doubleValue() {
    return x + y;
  }

  @Override
  public byte byteValue() {
    return (byte) x;
  }

  // does not override shortValue(), inherited from Number.shortValue().
}

public class Main {
  public static void main(String[] args) {
    Number nc = new NumberChild(2147483647.6, 2.6);
    assertTrue((nc.byteValue() == -1));
    assertTrue((nc.doubleValue() == 2.1474836502E9));
    // assertTrue((nc.floatValue() == 2.14748365E9f)); // does not distinguish float and double.
    assertTrue((nc.intValue() == 2147483647));
    assertTrue((nc.longValue() == 2147483650L));
    assertTrue((nc.shortValue() == -1));

    assertTrue((new NumberChild(3.6, 4.6).byteValue() == 3));
    assertTrue((new NumberChild(3.6, 4.6).shortValue() == 8));
  }
}
