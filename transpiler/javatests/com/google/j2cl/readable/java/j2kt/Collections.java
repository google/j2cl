/*
 * Copyright 2024 Google Inc.
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
package j2kt;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Collections {
  public static <T extends @Nullable Object> void testCollection_generic(Collection<T> collection) {
    collection.contains(Collections.<T>generic());
    collection.remove(Collections.<T>generic());
    collection.containsAll(Collections.<T>genericCollection());
    collection.addAll(Collections.genericCollection());
    collection.removeAll(Collections.<T>genericCollection());
    collection.retainAll(Collections.<T>genericCollection());

    collection.contains(mismatching());
    collection.remove(mismatching());
    collection.containsAll(mismatchingCollection());
    collection.removeAll(mismatchingCollection());
    collection.retainAll(mismatchingCollection());
  }

  public static void testCollection_parameterized(Collection<String> collection) {
    collection.contains(string());
    collection.remove(string());
    collection.containsAll(collectionOfString());
    collection.addAll(collectionOfString());
    collection.removeAll(collectionOfString());
    collection.retainAll(collectionOfString());

    collection.contains(mismatching());
    collection.remove(mismatching());
    collection.containsAll(mismatchingCollection());
    collection.removeAll(mismatchingCollection());
    collection.retainAll(mismatchingCollection());
  }

  public static void testCollection_specialized(CollectionOfString collection) {
    collection.contains(string());
    collection.remove(string());
    collection.containsAll(collectionOfString());
    collection.addAll(collectionOfString());
    collection.removeAll(collectionOfString());
    collection.retainAll(collectionOfString());

    collection.contains(mismatching());
    collection.remove(mismatching());
    collection.containsAll(mismatchingCollection());
    collection.removeAll(mismatchingCollection());
    collection.retainAll(mismatchingCollection());
  }

  public static <T extends @Nullable Object> void testList_generic(List<T> list) {
    list.addAll(0, Collections.genericCollection());
    list.indexOf(Collections.<T>generic());
    list.lastIndexOf(Collections.<T>generic());

    list.indexOf(mismatching());
    list.lastIndexOf(mismatching());
  }

  public static void testList_parameterized(List<String> list) {
    list.addAll(0, collectionOfString());
    list.indexOf(string());
    list.lastIndexOf(string());

    list.indexOf(mismatching());
    list.lastIndexOf(mismatching());
  }

  public static void testList_specialized(ListOfString list) {
    list.addAll(0, collectionOfString());
    list.indexOf(string());
    list.lastIndexOf(string());

    list.indexOf(mismatching());
    list.lastIndexOf(mismatching());
  }

  public static <K extends @Nullable Object, V extends @Nullable Object> void testMap_generic(
      Map<K, V> map) {
    map.get(Collections.<K>generic());
    map.getOrDefault(Collections.<K>generic(), generic());
    map.containsKey(Collections.<K>generic());
    map.containsValue(Collections.<V>generic());
    map.putAll(Collections.genericMap());
    map.remove(Collections.<K>generic());
    map.remove(Collections.<K>generic(), Collections.<V>generic());

    map.get(mismatching());
    map.getOrDefault(mismatching(), generic());
    map.containsKey(mismatching());
    map.containsValue(mismatching());
    map.remove(mismatching());
    map.remove(mismatching(), mismatching());
  }

  public static void testMap_parameterized(Map<String, String> map) {
    map.get(string());
    map.getOrDefault(string(), string());
    map.containsKey(string());
    map.containsValue(string());
    map.putAll(mapOfString());
    map.remove(string());
    map.remove(string(), string());

    map.get(mismatching());
    map.getOrDefault(mismatching(), string());
    map.containsKey(mismatching());
    map.containsValue(mismatching());
    map.remove(mismatching());
    map.remove(mismatching(), mismatching());
  }

  public static void testMap_specialized(MapOfString map) {
    map.get(string());
    map.getOrDefault(string(), string());
    map.containsKey(string());
    map.containsValue(string());
    map.putAll(mapOfString());
    map.remove(string());
    map.remove(string(), string());

    map.get(mismatching());
    map.getOrDefault(mismatching(), string());
    map.containsKey(mismatching());
    map.containsValue(mismatching());
    map.remove(mismatching());
    map.remove(mismatching(), mismatching());
  }

  public static class CustomCollection<T extends @Nullable Object> extends AbstractCollection<T> {
    @Override
    public Iterator<T> iterator() {
      throw new RuntimeException();
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean contains(@Nullable Object o) {
      o = convert(o);
      return super.contains(o);
    }

    @Override
    public boolean remove(@Nullable Object o) {
      o = convert(o);
      return super.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
      c = convertCollection(c);
      return super.addAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      c = convertCollection(c);
      return super.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      c = convertCollection(c);
      return super.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      c = convertCollection(c);
      return super.retainAll(c);
    }

    @Override
    public @Nullable Object[] toArray() {
      return super.toArray();
    }

    @Override
    public <T1 extends @Nullable Object> T1[] toArray(T1[] a) {
      return super.toArray(a);
    }
  }

  public static class CustomCollectionDisambiguatingOverrides<T extends @Nullable Object>
      extends CustomCollection<T> implements Collection<T> {
    // Test that J2KT inserts disambiguating overrides.
  }

  public static class CustomList<T extends @Nullable Object> extends AbstractList<T> {
    @Override
    public T get(int index) {
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
      c = convertCollection(c);
      return super.addAll(index, c);
    }

    @Override
    public int indexOf(@Nullable Object o) {
      o = convert(o);
      return super.indexOf(o);
    }

    @Override
    public int lastIndexOf(@Nullable Object o) {
      o = convert(o);
      return super.lastIndexOf(o);
    }

    @Override
    public @Nullable Object[] toArray() {
      return super.toArray();
    }

    @Override
    public <T1 extends @Nullable Object> T1[] toArray(T1[] a) {
      return super.toArray(a);
    }
  }

  public static class CustomMap<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractMap<K, V> {
    @Override
    public Set<Entry<K, V>> entrySet() {
      throw new RuntimeException();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
      key = convert(key);
      return super.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
      value = convert(value);
      return super.containsValue(value);
    }

    @Override
    public @Nullable V remove(@Nullable Object key) {
      key = convert(key);
      return super.remove(key);
    }

    @Override
    public boolean remove(@Nullable Object key, @Nullable Object value) {
      key = convert(key);
      value = convert(value);
      return super.remove(key, value);
    }

    @Override
    public @Nullable V get(@Nullable Object key) {
      key = convert(key);
      return super.get(key);
    }

    @Override
    public @Nullable V getOrDefault(@Nullable Object key, @Nullable V defaultValue) {
      key = convert(key);
      return super.getOrDefault(key, defaultValue);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
      m = convertMap(m);
      super.putAll(m);
    }
  }

  public abstract static class CustomMapDisambiguatingOverrides<
          K extends @Nullable Object, V extends @Nullable Object>
      extends CustomMap<K, V> implements Map<K, V> {
    // Test that J2KT inserts disambiguating overrides.
  }

  public static class CustomMapReturnTypes<K extends @Nullable Object, V extends @Nullable Object>
      extends CustomMap<K, V> {
    @Override
    public @Nullable V getOrDefault(@Nullable Object key, @Nullable V defaultValue) {
      return defaultValue;
    }
  }

  public static class CollectionOfString extends CustomCollection<String> {
    public boolean containsInteger(Integer integer) {
      return super.remove(integer);
    }

    public boolean removeInteger(Integer integer) {
      return super.remove(integer);
    }
  }

  public static class ListOfString extends CustomList<String> {
    @Override
    public int indexOf(@Nullable Object o) {
      o = convert(o);
      return super.indexOf(o);
    }

    @Override
    public int lastIndexOf(@Nullable Object o) {
      o = convert(o);
      return super.lastIndexOf(o);
    }
  }

  public static class MapOfString extends CustomMap<String, String> {
    @Override
    public boolean containsKey(@Nullable Object key) {
      key = convert(key);
      return super.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
      value = convert(value);
      return super.containsValue(value);
    }

    @Override
    public @Nullable String remove(@Nullable Object key) {
      key = convert(key);
      return super.remove(key);
    }

    @Override
    public boolean remove(@Nullable Object key, @Nullable Object value) {
      key = convert(key);
      value = convert(value);
      return super.remove(key, value);
    }

    @Override
    public @Nullable String get(@Nullable Object key) {
      key = convert(key);
      return super.get(key);
    }

    @Override
    public @Nullable String getOrDefault(@Nullable Object key, @Nullable String defaultValue) {
      key = convert(key);
      return super.getOrDefault(key, defaultValue);
    }
  }

  public abstract static class AbstractCollectionWithToArrayOverride<E extends @Nullable Object>
      implements Collection<E> {

    @Override
    public @Nullable Object[] toArray() {
      return new Object[0];
    }

    @Override
    public <T extends @Nullable Object> T[] toArray(T[] a) {
      return a;
    }
  }

  public interface CollectionInterfaceWithToArrayOverride<E extends @Nullable Object>
      extends List<E> {
    @Override
    @Nullable Object[] toArray();

    @Override
    <T extends @Nullable Object> T[] toArray(T[] a);
  }

  private static <T extends @Nullable Object> T convert(T object) {
    return object;
  }

  private static <T extends @Nullable Object> Collection<T> convertCollection(Collection<T> c) {
    return c;
  }

  private static <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> convertMap(
      Map<K, V> m) {
    return m;
  }

  private static <T extends @Nullable Object> T generic() {
    throw new RuntimeException();
  }

  private static String string() {
    throw new RuntimeException();
  }

  private static Integer mismatching() {
    throw new RuntimeException();
  }

  private static <T extends @Nullable Object> Collection<T> genericCollection() {
    throw new RuntimeException();
  }

  private static Collection<String> collectionOfString() {
    throw new RuntimeException();
  }

  private static Collection<Integer> mismatchingCollection() {
    throw new RuntimeException();
  }

  private static <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> genericMap() {
    throw new RuntimeException();
  }

  private static Map<String, String> mapOfString() {
    throw new RuntimeException();
  }
}
