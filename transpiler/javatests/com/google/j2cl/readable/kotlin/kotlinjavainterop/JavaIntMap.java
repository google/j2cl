/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package kotlinjavainterop;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JavaIntMap implements Map<Integer, Integer> {
  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean containsKey(Object key) {
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    return false;
  }

  @Override
  public Integer get(Object key) {
    return 0;
  }

  @Override
  public Integer put(Integer key, Integer value) {
    return 0;
  }

  @Override
  public Integer remove(Object key) {
    return 0;
  }

  @Override
  public void putAll(Map<? extends Integer, ? extends Integer> m) {}

  @Override
  public void clear() {}

  @Override
  @SuppressWarnings("JdkImmutableCollections")
  public Set<Integer> keySet() {
    return Set.of();
  }

  @Override
  @SuppressWarnings("JdkImmutableCollections")
  public Collection<Integer> values() {
    return List.of();
  }

  @Override
  @SuppressWarnings("JdkImmutableCollections")
  public Set<Entry<Integer, Integer>> entrySet() {
    return Set.of();
  }
}
