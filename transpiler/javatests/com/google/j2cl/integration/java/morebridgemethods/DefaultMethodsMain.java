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
package morebridgemethods;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class DefaultMethodsMain {

  public static final int COLLECTION_ADD = 1;
  public static final int LIST_ADD = 2;

  interface Collection<T> {
    default int add(T elem) {
      assertTrue(this instanceof Collection);
      return COLLECTION_ADD;
    }
  }

  interface List<T> extends Collection<T> {
    default int add(T elem) {
      assertTrue(this instanceof List);
      return LIST_ADD;
    }
  }

  static class SomeOtherCollection<T> implements Collection<T> {}

  static class SomeOtherList<T> extends SomeOtherCollection<T> implements List<T> {}

  // Should inherit List.add  even though the interface is not directly declared.
  static class YetAnotherList<T> extends SomeOtherList<T> implements Collection<T> {}

  public static void test() {
    assertTrue(new YetAnotherList<Object>().add(null) == LIST_ADD);
  }
}
