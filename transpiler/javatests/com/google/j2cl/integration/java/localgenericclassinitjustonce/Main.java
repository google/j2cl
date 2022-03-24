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
package localgenericclassinitjustonce;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * The 'value' field is given a value of 0 in it's initializer, which should run in the $init()
 * function. This value is then replaced with another value in the body of the second P1 constructor
 * method. It's important that constructors that delegate to 'this()' not also call $init() since
 * doing so would mean it is called more than once and values that were set in other constructors
 * (invoked by the 'this()' call) will have been overwritten.
 */
public class Main {

  static class P1<T1> {

    class P2<T2> extends P1<T1> {

      int value = 0;

      P2() {
        this(1);
      }

      P2(int i) {
        value = i;
      }
    }
  }

  public static void main(String[] args) {
    P1<?> p1 = new P1<Object>();
    P1<?>.P2<?> p2 = p1.new P2<Object>();
    assertTrue((1 == p2.value));
    assertTrue((2 == p1.new P2<Object>(2).value));
  }
}
