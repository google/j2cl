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
package interfacedevirtualize;

public class InterfaceDevirtualize {
  public <T> int compare0(Comparable<T> c, T t) {
    return c.compareTo(t);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public int compare1(Comparable c, Object o) {
    return c.compareTo(o);
  }

  public int compare2(Comparable<Double> c, Double d) {
    return c.compareTo(d);
  }

  public int compare3(Double d1, Double d2) {
    return d1.compareTo(d2);
  }

  public int compare2(Comparable<Boolean> c, Boolean d) {
    return c.compareTo(d);
  }

  public int compare3(Boolean d1, Boolean d2) {
    return d1.compareTo(d2);
  }

  public int compare2(Comparable<Integer> c, Integer d) {
    return c.compareTo(d);
  }

  public int compare3(Integer d1, Integer d2) {
    return d1.compareTo(d2);
  }
}
