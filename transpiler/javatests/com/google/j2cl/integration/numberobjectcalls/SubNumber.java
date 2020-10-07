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

public class SubNumber extends Number {
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

  public void test() {
    SubNumber sn = new SubNumber();
    assertTrue((this.equals(sn)));
    assertTrue((equals(sn)));
    assertTrue((!equals(new Object())));

    assertTrue((this.hashCode() == 100));
    assertTrue((hashCode() == 100));

    assertTrue((toString().equals(this.toString())));

    assertTrue((getClass() instanceof Class));
    assertTrue((getClass().equals(this.getClass())));
  }
}
