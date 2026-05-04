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
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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

  public static <L extends List<String>, M extends L> void testMutability(
      Iterator<String> iterator,
      ListIterator<String> listIterator,
      Collection<String> collection,
      List<String> list,
      L genericList,
      M genericList2,
      Set<String> set,
      Map<String, String> map,
      Map.Entry<String, String> mapEntry) {
    iterator.remove();

    listIterator.add("foo");
    listIterator.set("foo");
    listIterator.remove();

    collection.add("foo");
    collection.addAll(list);
    collection.remove("foo");
    collection.removeAll(list);
    collection.removeIf(x -> true);
    collection.retainAll(list);
    collection.clear();

    list.add(0, "foo");
    list.addAll(list);
    list.set(0, "foo");
    list.sort(null);
    list.remove(0);
    list.replaceAll(x -> x);

    genericList.add(0, "foo");
    genericList.set(0, "foo");
    genericList.remove(0);
    genericList.sort(null);

    genericList2.add(0, "foo");
    genericList2.set(0, "foo");
    genericList2.remove(0);
    genericList2.sort(null);

    set.add("foo");
    set.addAll(list);
    set.remove("foo");
    set.removeAll(list);
    set.removeIf(x -> true);
    set.retainAll(list);
    set.clear();

    map.put("foo", "bar");
    map.putAll(map);
    map.putIfAbsent("foo", "bar");
    map.remove("foo");
    map.replace("foo", "bar");
    map.replaceAll((k, v) -> v);
    map.clear();
    map.compute("foo", (k, v) -> v);
    map.computeIfAbsent("foo", k -> "bar");
    map.computeIfPresent("foo", (k, v) -> v);
    map.merge("foo", "bar", (v1, v2) -> v1);

    mapEntry.setValue("bar");
  }

  public static void testMutability_subtypes(
      CustomCollection<String> collection, CustomList<String> list, CustomMap<String, String> map) {
    collection.add("foo");
    collection.addAll(list);
    collection.remove("foo");
    collection.removeAll(list);
    collection.removeIf(x -> true);
    collection.retainAll(list);
    collection.clear();

    list.add(0, "foo");
    list.addAll(list);
    list.set(0, "foo");
    list.sort(null);
    list.remove(0);
    list.replaceAll(x -> x);

    map.put("foo", "bar");
    map.putAll(map);
    map.putIfAbsent("foo", "bar");
    map.remove("foo");
    map.replace("foo", "bar");
    map.replaceAll((k, v) -> v);
    map.clear();
    map.compute("foo", (k, v) -> v);
    map.computeIfAbsent("foo", k -> "bar");
    map.computeIfPresent("foo", (k, v) -> v);
    map.merge("foo", "bar", (v1, v2) -> v1);
  }

  public static void testLowerBoundAssignment(
      Collection<String> collection,
      List<String> list,
      Set<String> set,
      Map<String, String> map,
      Map.Entry<String, String> mapEntry,
      CustomCollection<String> customCollection) {
    Collection<? super String> lowerC = collection;
    List<? super String> lowerL = list;
    Set<? super String> lowerS = set;
    Map<? super String, ? super String> lowerM = map;
    Map.Entry<String, ? super String> lowerMe = mapEntry;

    Collection<? super String> lowerC2 = customCollection;
    Collection<? super String> lowerC3 = lowerC;

    lowerC = collection;
    lowerL = list;
    lowerS = set;
    lowerM = map;
    lowerMe = mapEntry;

    passLowerBoundCollection(collection);
    passLowerBoundList(list);
    passLowerBoundSet(set);
    passLowerBoundMap(map);
    passLowerBoundMapEntry(mapEntry);

    List<List<? super String>> nestedList = new java.util.ArrayList<>();
    nestedList.add(new java.util.ArrayList<String>());
    nestedList.add(new java.util.ArrayList<Object>());

    List<Map<String, ? super String>> nestedMap = new java.util.ArrayList<>();
    Map<String, String> innerMap = new java.util.HashMap<>();
    nestedMap.add(innerMap);

    testNestedLowerBounds(l -> {}, nestedList, nestedMap);
  }

  private static void testNestedLowerBounds(
      java.util.function.Consumer<List<? super String>> consumer,
      List<List<? super String>> nestedList,
      List<Map<String, ? super String>> nestedMap) {}

  private static void passLowerBoundCollection(Collection<? super String> collection) {}

  private static void passLowerBoundList(List<? super String> list) {}

  private static void passLowerBoundSet(Set<? super String> set) {}

  private static void passLowerBoundMap(Map<? super String, ? super String> map) {}

  private static void passLowerBoundMapEntry(Map.Entry<String, ? super String> mapEntry) {}

  private static Collection<? super String> returnLowerBoundCollection(
      Collection<String> collection) {
    return collection;
  }

  private static List<? super String> returnLowerBoundList(List<String> list) {
    return list;
  }

  private static Set<? super String> returnLowerBoundSet(Set<String> set) {
    return set;
  }

  private static Map<? super String, ? super String> returnLowerBoundMap(Map<String, String> map) {
    return map;
  }

  private static Map.Entry<String, ? super String> returnLowerBoundMapEntry(
      Map.Entry<String, String> mapEntry) {
    return mapEntry;
  }

  public static class CustomCollection<T extends @Nullable Object> extends AbstractCollection<T> {
    @Override
    public Iterator<T> iterator() {
      return java.util.Collections.emptyIterator();
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
    public ListIterator<T> listIterator() {
      return java.util.Collections.emptyListIterator();
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
      return java.util.Collections.emptyList();
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

  public static class CustomListWithSuperCalls<T extends @Nullable Object> extends ArrayList<T> {
    @Override
    public T get(int index) {
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public Iterator<T> iterator() {
      return super.iterator();
    }

    @Override
    public ListIterator<T> listIterator() {
      return super.listIterator();
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
      return super.subList(fromIndex, toIndex);
    }
  }

  public static class CustomMap<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractMap<K, V> {
    @Override
    public Set<Entry<K, V>> entrySet() {
      return java.util.Collections.emptySet();
    }

    @Override
    public Set<K> keySet() {
      return java.util.Collections.emptySet();
    }

    @Override
    public Collection<V> values() {
      return java.util.Collections.emptyList();
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

  public static class CustomMapWithSuperCalls<
          K extends @Nullable Object, V extends @Nullable Object>
      extends HashMap<K, V> {
    @Override
    public Set<Entry<K, V>> entrySet() {
      return super.entrySet();
    }

    @Override
    public Set<K> keySet() {
      return super.keySet();
    }

    @Override
    public Collection<V> values() {
      return super.values();
    }
  }

  public static class CustomMapSetValues<K extends @Nullable Object, V extends @Nullable Object>
      extends CustomMap<K, V> {
    @Override
    public Set<V> values() {
      return java.util.Collections.emptySet();
    }
  }

  public static class CustomMapWithNestedLambda<
          K extends @Nullable Object, V extends @Nullable Object>
      extends CustomMap<K, V> {
    @Override
    public Collection<V> values() {
      Runnable r =
          () -> {
            Collection<V> ignored = (Collection<V>) java.util.Collections.emptySet();
            return;
          };
      java.util.function.Supplier<Collection<V>> s =
          () -> (Collection<V>) java.util.Collections.emptySet();
      return (Collection<V>) java.util.Collections.emptyList();
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

  public static class SpecificEntrySet extends AbstractSet<Map.Entry<String, Integer>> {
    @Override
    public Iterator<Map.Entry<String, Integer>> iterator() {
      return null;
    }

    @Override
    public int size() {
      return 0;
    }
  }

  public static class CustomMapWithSpecificEntrySet extends AbstractMap<String, Integer> {
    @Override
    public Set<Map.Entry<String, Integer>> entrySet() {
      return new SpecificEntrySet();
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

  private static void testCustomMapWithCustomSetEntry(
      CustomMapWithCustomSetEntry<String, String> map) {
    Set<Map.Entry<String, String>> entrySet = map.entrySet();
    Map<String, String> regularMap = map;
    entrySet = regularMap.entrySet();
  }

  public interface CustomMapWithCustomSetEntry<
          K extends @Nullable Object, V extends @Nullable Object>
      extends Map<K, V> {
    @Override
    AbstractSet<Entry<K, V>> entrySet();
  }

  public interface CustomIterable<E extends @Nullable Object> extends Iterable<E> {
    @Override
    Iterator<E> iterator();
  }

  public interface CustomListWithSubList extends List<List<String>> {
    @Override
    List<List<String>> subList(int fromIndex, int toIndex);
  }

  public static <L extends List<String>> void testTypeVariableMutation(L list) {
    list.add("foo");
  }

  public static <L extends List<? super String>> void testTypeVariableMutationWithLowerBound(
      L list) {
    list.add("foo");
  }

  public static void testWildcardMutation(List<? extends List<String>> lists) {
    lists.get(0).add("foo");
  }

  public static void testIterableLambda() {
    List<String> list = new ArrayList<>();
    Iterable<String> it = () -> list.iterator();
  }

  public static void testCustomIterableLambda() {
    List<String> list = new ArrayList<>();
    CustomIterable<String> it = () -> list.iterator();
  }
}
