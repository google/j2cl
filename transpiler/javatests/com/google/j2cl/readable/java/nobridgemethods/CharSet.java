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
package nobridgemethods;

interface Entry<K> {}

abstract class Map<K> {
  public abstract Entry<K> getCeilingEntry(K key);
}

class TreeMap<K> extends Map<K> {
  static class InnerEntry<K> implements Entry<K> {}

  @Override
  public InnerEntry<K> getCeilingEntry(K key) {
    return new InnerEntry<K>();
  }
}

public class CharSet {
  @SuppressWarnings("unused")
  public static void main(String... args) {
    TreeMap<String> treeMap = new TreeMap<String>() {};
  }
}
