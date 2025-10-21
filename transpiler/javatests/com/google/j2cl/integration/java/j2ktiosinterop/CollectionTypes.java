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
package j2ktiosinterop;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class CollectionTypes {

  public static <T extends @Nullable Object> Iterator<T> getIterator() {
    return Collections.<T>emptyList().iterator();
  }

  public static <T extends @Nullable Object> Iterable<T> getIterable() {
    return Collections.emptyList();
  }

  public static <T extends @Nullable Object> Collection<T> getCollection() {
    return Collections.emptyList();
  }

  public static <T extends @Nullable Object> List<T> getList() {
    return Collections.emptyList();
  }

  public static <T extends @Nullable Object> Set<T> getSet() {
    return Collections.emptySet();
  }

  public static <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> getMap() {
    return Collections.emptyMap();
  }

  public static <T extends @Nullable Object> void acceptIterator(Iterator<T> iterator) {}

  public static <T extends @Nullable Object> void acceptIterable(Iterable<T> iterable) {}

  public static <T extends @Nullable Object> void acceptCollection(Collection<T> collection) {}

  public static <T extends @Nullable Object> void acceptList(List<T> list) {}

  public static <T extends @Nullable Object> void acceptSet(Set<T> set) {}

  public static <K extends @Nullable Object, V extends @Nullable Object> void acceptMap(
      Map<K, V> map) {}

  private CollectionTypes() {}
}
