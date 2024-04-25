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
package implementsgenericinterface;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

interface GenericInterface<T> {
  T foo(T t);
}

class InterfaceImpl implements GenericInterface<InterfaceImpl> {
  @Override
  public InterfaceImpl foo(InterfaceImpl t) {
    return t;
  }
}

class InterfaceGenericImpl<T> implements GenericInterface<T> {
  @Override
  public T foo(T t) {
    return t;
  }
}

public class Main {
  public static void main(String... args) {
    InterfaceImpl i = new InterfaceImpl();
    Object o = i.foo(i);
    assertTrue(o == i);

    InterfaceGenericImpl<InterfaceImpl> gi = new InterfaceGenericImpl<>();
    Object oo = gi.foo(i);
    assertTrue(oo == i);
  }
}
