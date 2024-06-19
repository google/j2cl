/*
 * Copyright 2016 Google Inc.
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

package com.google.j2cl.jre.java8.util.stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.averagingInt;
import static java.util.stream.Collectors.averagingLong;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.filtering;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.minBy;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.summarizingDouble;
import static java.util.stream.Collectors.summarizingInt;
import static java.util.stream.Collectors.summarizingLong;
import static java.util.stream.Collectors.summingDouble;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.google.j2cl.jre.java.util.EmulTestBase;
import com.google.j2cl.jre.testing.J2ktIncompatible;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Tests {@link java.util.stream.Collectors}.
 *
 * <p>Methods that are presently only tested indirectly:
 *
 * <ul>
 *   <li>reducing: counting, minBy/maxBy use this
 *   <li>toCollection tested by toList (toSet now uses its own impl)
 * </ul>
 */
public class CollectorsTest extends EmulTestBase {

  public void testAveragingDouble() {
    Collector<Double, ?, Double> c = averagingDouble(Double::doubleValue);
    applyItems((4.0 + 8.0) / 2.0, c, 4.0, 8.0);

    assertZeroItemsCollectedAs(0D, c);
    assertSingleItemCollectedAs(5D, c, 5D);
  }

  public void testAveragingInt() {
    Collector<Integer, ?, Double> c = averagingInt(Integer::intValue);
    applyItems((4.0 + 8.0) / 2.0, c, 4, 8);

    assertZeroItemsCollectedAs(0D, c);
    assertSingleItemCollectedAs(5D, c, 5);
  }

  public void testAveragingLong() {
    Collector<Long, ?, Double> c = averagingLong(Long::longValue);
    applyItems((4.0 + 8.0) / 2.0, c, 4L, 8L);

    assertZeroItemsCollectedAs(0D, c);
    assertSingleItemCollectedAs(5D, c, 5L);
  }

  public void testCollectingAndThen() {
    Collector<Object, ?, List<Object>> listIdentityCollector =
        collectingAndThen(toList(), Function.identity());
    // same test as toList():
    // same items (allow dups)
    applyItems(
        Arrays.asList("a", "a"),
        listIdentityCollector,
        "a", "a"
    );

    // ordered
    applyItems(
        Arrays.asList("a", "b"),
        listIdentityCollector,
        "a", "b"
    );
    assertZeroItemsCollectedAs(Collections.emptyList(), listIdentityCollector);
    assertSingleItemCollectedAs(Collections.singletonList("a"), listIdentityCollector, "a");

    Collector<Object, ?, Integer> uglyCount = collectingAndThen(toList(), List::size);
    // (nearly) same test as counting():
    applyItems(2, uglyCount, "1", new Object());

    assertZeroItemsCollectedAs(0, uglyCount);
    assertSingleItemCollectedAs(1, uglyCount, new Object());
  }

  public void testCounting() {
    Collector<Object, ?, Long> c = counting();
    applyItems(2L, c, "1", new Object());

    assertZeroItemsCollectedAs(0L, c);
    assertSingleItemCollectedAs(1L, c, new Object());
  }

  public void testGroupingBy() {
    Collector<String, ?, Map<String, List<String>>> c1 = groupingBy(Function.identity());

    Map<String, List<String>> mapOfLists = new HashMap<>();
    mapOfLists.put("a", Arrays.asList("a", "a"));
    applyItems(mapOfLists, c1, "a", "a");
    mapOfLists.clear();
    mapOfLists.put("a", Collections.singletonList("a"));
    mapOfLists.put("b", Collections.singletonList("b"));
    applyItems(mapOfLists, c1, "a", "b");

    assertZeroItemsCollectedAs(Collections.emptyMap(), c1);
    assertSingleItemCollectedAs(
        Collections.singletonMap("a", Collections.singletonList("a")), c1, "a");

    Collector<String, ?, LinkedHashMap<String, Set<String>>> c2 =
        groupingBy(Function.identity(), LinkedHashMap::new, toSet());

    LinkedHashMap<String, Set<String>> linkedMapOfSets = new LinkedHashMap<>();
    linkedMapOfSets.put("a", Collections.singleton("a"));
    applyItems(linkedMapOfSets, c2, "a", "a");
    linkedMapOfSets.clear();
    linkedMapOfSets.put("a", Collections.singleton("a"));
    linkedMapOfSets.put("b", Collections.singleton("b"));
    applyItems(linkedMapOfSets, c2, "a", "b");

    // check to make sure we actually get the linked results, and that they are ordered how we want
    // them
    LinkedHashMap<String, Set<String>> out = applyItemsWithoutSplitting(c2, "a", "b");
    assertEquals(Arrays.asList("a", "b"), new ArrayList<>(out.keySet()));
    out = applyItemsWithoutSplitting(c2, "b", "a");
    assertEquals(Arrays.asList("b", "a"), new ArrayList<>(out.keySet()));

    assertZeroItemsCollectedAs(new LinkedHashMap<>(), c2);
    linkedMapOfSets.clear();
    linkedMapOfSets.put("a", Collections.singleton("a"));
    assertSingleItemCollectedAs(linkedMapOfSets, c2, "a");
  }

  public void testJoining() {
    Collector<CharSequence, ?, String> c = joining();
    applyItems("ab", c, "a", "b");
    applyItems("a,", c, "a", ",");
    applyItems("", c, "", "");
    assertZeroItemsCollectedAs("", c);
    assertSingleItemCollectedAs("a", c, "a");
    assertSingleItemCollectedAs("", c, "");

    c = joining(",");
    applyItems("a,b", c, "a", "b");
    applyItems("a,,", c, "a", ",");
    applyItems(",", c, "", "");
    assertZeroItemsCollectedAs("", c);
    assertSingleItemCollectedAs("a", c, "a");
    assertSingleItemCollectedAs("", c, "");

    c = joining("-", "{", "}");
    applyItems("{a-b}", c, "a", "b");
    assertZeroItemsCollectedAs("{}", c);
    assertSingleItemCollectedAs("{a}", c, "a");
    assertSingleItemCollectedAs("{}", c, "");
  }

  public void testMapping() {
    Collector<String, ?, List<String>> identityMapping = mapping(Function.identity(), toList());
    // same test as toList():
    // same items (allow dups)
    applyItems(
        Arrays.asList("a", "a"),
        identityMapping,
        "a", "a"
    );

    // ordered
    applyItems(
        Arrays.asList("a", "b"),
        identityMapping,
        "a", "b"
    );
    assertZeroItemsCollectedAs(Collections.emptyList(), identityMapping);
    assertSingleItemCollectedAs(Collections.singletonList("a"), identityMapping, "a");

    Collector<Integer, ?, List<String>> numberMapping = mapping(s -> "#" + s, toList());
    // poke the same tests as list, make sure the mapper is run
    applyItems(
        Arrays.asList("#1", "#2"),
        numberMapping,
        1, 2
    );

    // ordered
    applyItems(
        Arrays.asList("#1", "#2"),
        numberMapping,
        1, 2
    );
    assertZeroItemsCollectedAs(Collections.emptyList(), numberMapping);
    assertSingleItemCollectedAs(Collections.singletonList("#10"), numberMapping, 10);
  }

  public void testFlatMapping() {
    // Since applyItems tests the same inputs multiple times, we need fresh stream instances as they
    // can't be reused
    Collector<Collection<String>, ?, List<String>> flatMapping =
        flatMapping(Collection::stream, toList());
    applyItems(
        Arrays.asList("a", "b"),
        flatMapping,
        Collections.singletonList("a"),
        Collections.singletonList("b"));
    applyItems(
        Arrays.asList("c", "d"), flatMapping, Collections.emptyList(), Arrays.asList("c", "d"));

    // Ensure null is treated like an empty stream.
    Function<Collection<String>, @Nullable Stream<String>> mapEmptyToNull =
        items -> items.isEmpty() ? null : items.stream();
    applyItems(
        Arrays.asList("a", "b", "c"),
        flatMapping(mapEmptyToNull, toList()),
        Collections.emptyList(),
        Arrays.asList("a", "b", "c"));

    // Ensure mapped streams are closed.
    int[] calledCount = {0};
    Stream<String> mapped = Stream.of("x").onClose(() -> calledCount[0]++);
    var ignored = Stream.of(1).collect(flatMapping(x -> mapped, toList()));
    assertEquals(1, calledCount[0]);
  }

  public void testFiltering() {
    Collector<String, ?, List<String>> filtering = filtering(s -> s.equals("a"), toList());
    applyItems(Collections.singletonList("a"), filtering, "a", "b");
    applyItems(Collections.emptyList(), filtering, "c", "d");
    applyItems(Arrays.asList("a", "a"), filtering, "a", "a");
  }

  public void testMaxBy() {
    Collector<String, ?, Optional<String>> c = maxBy(Comparator.naturalOrder());
    applyItems(Optional.of("z"), c, "a", "z");
    applyItems(Optional.of("z"), c, "z", "a");

    assertZeroItemsCollectedAs(Optional.empty(), c);
    assertSingleItemCollectedAs(Optional.of("foo"), c, "foo");
  }

  public void testMinBy() {
    Collector<String, ?, Optional<String>> c = minBy(Comparator.naturalOrder());
    applyItems(Optional.of("a"), c, "a", "z");
    applyItems(Optional.of("a"), c, "z", "a");

    assertZeroItemsCollectedAs(Optional.empty(), c);
    assertSingleItemCollectedAs(Optional.of("foo"), c, "foo");
  }

  public void testPartitioningBy() {
    Collector<Boolean, ?, Map<Boolean, List<Boolean>>> c1 = partitioningBy(Boolean::valueOf);

    Map<Boolean, List<Boolean>> mapOfLists = new HashMap<>();
    mapOfLists.put(true, Collections.singletonList(true));
    mapOfLists.put(false, Collections.singletonList(false));
    applyItems(mapOfLists, c1, true, false);

    mapOfLists.clear();
    mapOfLists.put(false, Collections.emptyList());
    mapOfLists.put(true, Arrays.asList(true, true));
    applyItems(mapOfLists, c1, true, true);

    mapOfLists.clear();
    mapOfLists.put(false, Arrays.asList(false, false));
    mapOfLists.put(true, Collections.emptyList());
    applyItems(mapOfLists, c1, false, false);

    mapOfLists.clear();
    mapOfLists.put(false, Collections.emptyList());
    mapOfLists.put(true, Collections.emptyList());
    assertZeroItemsCollectedAs(mapOfLists, c1);

    mapOfLists.clear();
    mapOfLists.put(false, Collections.emptyList());
    mapOfLists.put(true, Collections.singletonList(true));
    assertSingleItemCollectedAs(mapOfLists, c1, true);

    mapOfLists.clear();
    mapOfLists.put(false, Collections.singletonList(false));
    mapOfLists.put(true, Collections.emptyList());
    assertSingleItemCollectedAs(mapOfLists, c1, false);

    Collector<Boolean, ?, Map<Boolean, Set<Boolean>>> c2 =
        partitioningBy(Boolean::valueOf, toSet());

    Map<Boolean, Set<Boolean>> mapOfSets = new HashMap<>();
    mapOfSets.put(true, Collections.singleton(true));
    mapOfSets.put(false, Collections.singleton(false));
    applyItems(mapOfSets, c2, true, false);

    mapOfSets.clear();
    mapOfSets.put(false, Collections.emptySet());
    mapOfSets.put(true, Collections.singleton(true));
    applyItems(mapOfSets, c2, true, true);

    mapOfSets.clear();
    mapOfSets.put(false, Collections.singleton(false));
    mapOfSets.put(true, Collections.emptySet());
    applyItems(mapOfSets, c2, false, false);
  }

  public void testSummarizingDouble() {
    Collector<Double, ?, DoubleSummaryStatistics> c = summarizingDouble(Double::doubleValue);
    DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
    stats.accept(5.1);
    stats.accept(7);
    BiPredicate<DoubleSummaryStatistics, DoubleSummaryStatistics> equals = (s1, s2) ->
        s1.getSum() == s2.getSum()
            && s1.getAverage() == s2.getAverage()
            && s1.getCount() == s2.getCount()
            && s1.getMin() == s2.getMin()
            && s1.getMax() == s2.getMax();
    applyItems(stats, c, 5.1, 7.0, equals);
    applyItems(stats, c, 7.0, 5.1, equals);//probably unnecessary to run these backward

    assertZeroItemsCollectedAs(new DoubleSummaryStatistics(), c, equals);
    stats = new DoubleSummaryStatistics();
    stats.accept(7.3);
    assertSingleItemCollectedAs(stats, c, 7.3, equals);
  }

  public void testSummarizingInt() {
    Collector<Integer, ?, IntSummaryStatistics> c = summarizingInt(Integer::intValue);
    IntSummaryStatistics stats = new IntSummaryStatistics();
    stats.accept(2);
    stats.accept(10);
    BiPredicate<IntSummaryStatistics, IntSummaryStatistics> equals = (s1, s2) ->
        s1.getSum() == s2.getSum()
            && s1.getAverage() == s2.getAverage()
            && s1.getCount() == s2.getCount()
            && s1.getMin() == s2.getMin()
            && s1.getMax() == s2.getMax();
    applyItems(stats, c, 2, 10, equals);
    applyItems(stats, c, 10, 2, equals);

    assertZeroItemsCollectedAs(new IntSummaryStatistics(), c, equals);
    stats = new IntSummaryStatistics();
    stats.accept(7);
    assertSingleItemCollectedAs(stats, c, 7, equals);
  }

  public void testSummarizingLong() {
    Collector<Long, ?, LongSummaryStatistics> c = summarizingLong(Long::longValue);
    LongSummaryStatistics stats = new LongSummaryStatistics();
    stats.accept(2);
    stats.accept(10);
    BiPredicate<LongSummaryStatistics, LongSummaryStatistics> equals = (s1, s2) ->
        s1.getSum() == s2.getSum()
            && s1.getAverage() == s2.getAverage()
            && s1.getCount() == s2.getCount()
            && s1.getMin() == s2.getMin()
            && s1.getMax() == s2.getMax();
    applyItems(stats, c, 2L, 10L, equals);
    applyItems(stats, c, 10L, 2L, equals);

    assertZeroItemsCollectedAs(new LongSummaryStatistics(), c, equals);
    stats = new LongSummaryStatistics();
    stats.accept(7L);
    assertSingleItemCollectedAs(stats, c, 7L, equals);
  }

  public void testSummingDouble() {
    Collector<Double, ?, Double> c = summingDouble(Double::doubleValue);
    applyItems(4.1 + 8.2, c, 4.1, 8.2);

    assertZeroItemsCollectedAs(0d, c);
    assertSingleItemCollectedAs(7.3, c, 7.3);
  }

  public void testSummingInt() {
    Collector<Integer, ?, Integer> c = summingInt(Integer::intValue);
    applyItems(4 + 8, c, 4, 8);

    assertZeroItemsCollectedAs(0, c);
    assertSingleItemCollectedAs(7, c, 7);
  }

  public void testSummingLong() {
    Collector<Long, ?, Long> c = summingLong(Long::longValue);
    applyItems(4L + 8L, c, 4L, 8L);

    assertZeroItemsCollectedAs(0L, c);
    assertSingleItemCollectedAs(5L, c, 5L);
  }

  public void testList() {
    Collector<String, ?, List<String>> c = toList();

    // same items (allow dups)
    applyItems(
        Arrays.asList("a", "a"),
        c,
        "a", "a"
    );

    // ordered
    applyItems(
        Arrays.asList("a", "b"),
        c,
        "a", "b"
    );
    assertZeroItemsCollectedAs(Collections.emptyList(), c);
    assertSingleItemCollectedAs(Collections.singletonList("a"), c, "a");
  }

  public void testMap() {
    Collector<String, ?, Map<String, String>> c = toMap(Function.identity(), Function.identity());

    // two distinct items
    Map<String, String> map = new HashMap<>();
    map.put("a", "a");
    map.put("b", "b");
    applyItems(map, c, "a", "b");

    // inline applyItems and test each to confirm failure for duplicates
    try {
      applyItemsWithoutSplitting(c, "a", "a");
      fail("expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      applyItemsWithSplitting(c, "a", "a");
      fail("expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }

    assertZeroItemsCollectedAs(Collections.emptyMap(), c);
    assertSingleItemCollectedAs(Collections.singletonMap("a", "a"), c, "a");

    List<String> seen = new ArrayList<>();
    c = toMap(Function.identity(), Function.identity(), (s, s2) -> {
      seen.add("first: " + s);
      seen.add("second: " + s2);
      return s + "," + s2;
    });
    map = new HashMap<>();
    map.put("a", "a,a");
    applyItems(map, c, "a", "a");
    assertEquals(Arrays.asList("first: a", "second: a", "first: a", "second: a"), seen);
  }

  @SuppressWarnings({
    "JdkCollectors", // test of a JDK Collector implementation
    "UnnecessaryCast", // for nullness purposes: b/326255614
  })
  public void testMapRemovalFromMergeFunction() {
    /*
     * J2KT requires us to declare a BinaryOperator<@Nullable Integer> because we return null. (This
     * makes our parameter types nullable, too, even though toMap will never pass a null argument.)
     * We later cast it to BinaryOperator<Integer>, which is currently the type required by the J2KT
     * signature for toMap. That signature should perhaps change, similar to how the signature for
     * ImmutableMap.toImmutableMap should perhaps change, as discussed in
     * https://github.com/google/guava/issues/6824.
     */
    BinaryOperator<@Nullable Integer> mergeFunction =
        (a, b) -> {
          int result = requireNonNull(a) + requireNonNull(b);
          return result == 0 ? null : result;
        };
    Map<String, Integer> actual =
        Stream.<Map.Entry<String, Integer>>of(
                new SimpleImmutableEntry<>("a", 1),
                new SimpleImmutableEntry<>("b", 2),
                new SimpleImmutableEntry<>("b", -2),
                new SimpleImmutableEntry<>("c", 3),
                new SimpleImmutableEntry<>("c", -3),
                new SimpleImmutableEntry<>("c", 7))
            .collect(
                toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (BinaryOperator<Integer>) mergeFunction));
    Map<String, Integer> expected = new HashMap<>();
    expected.put("a", 1);
    expected.put("c", 7);
    assertEquals(expected, actual);
  }

  @SuppressWarnings("JdkCollectors") // test of a JDK Collector implementation
  public void testNullFromKeyFunction() {
    Map<@Nullable Object, Integer> actual = Stream.of(1).collect(toMap(e -> null, e -> e));
    Map<@Nullable Object, Integer> expected = new HashMap<>();
    expected.put(null, 1);
    assertEquals(expected, actual);
  }

  @SuppressWarnings("JdkCollectors") // test of a JDK Collector implementation
  public void testNullFromValueFunction() {
    try {
      Stream.of(1).collect(toMap(e -> e, e -> null));
      fail();
    } catch (NullPointerException expected) {
    }
  }

  @SuppressWarnings({
    "JdkCollectors", // test of a JDK Collector implementation
    "UnnecessaryCast", // for nullness purposes: b/326255614
  })
  public void testNullFromValueFunctionWhenMerging() {
    /*
     * J2KT's toMap is annotated to forbid a valueFunction that returns null, so the only way to
     * return null is to cast away the nullness in the function type.
     *
     * (I'm surprised that J2KT doesn't object to the `e -> null` function in
     * testNullFromValueFunction() above....)
     */
    Function<Map.Entry<?, @Nullable String>, @Nullable String> valueFunction = Map.Entry::getValue;
    try {
      Stream.<Map.Entry<String, @Nullable String>>of(
              new SimpleImmutableEntry<>("a", "x"),
              new SimpleImmutableEntry<>("a", "y"),
              new SimpleImmutableEntry<>("a", null))
          .collect(
              toMap(
                  Map.Entry::getKey,
                  (Function<Map.Entry<?, @Nullable String>, String>) valueFunction,
                  (a, b) -> a + b));
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testSet() {
    Collector<String, ?, Set<String>> c = toSet();

    // same items (no dups)
    applyItems(
        Collections.singleton("a"),
        c,
        "a", "a"
    );

    // different items
    applyItems(
        new HashSet<>(Arrays.asList("a", "b")),
        c,
        "a", "b"
    );

    assertZeroItemsCollectedAs(Collections.emptySet(), c);
    assertSingleItemCollectedAs(Collections.singleton("a"), c, "a");
  }

  @J2ktIncompatible // Not emulated in J2KT
  public void testToUnmodifiableList() {
    applyItems(List.of("a", "b"), toUnmodifiableList(), "a", "b");
    assertUnmodifiableCollection(applyItemsWithSplitting(toUnmodifiableList(), "a", "b"), "a", "z");
    try {
      Stream.of("a").map(ignore -> null).collect(toUnmodifiableList());
      fail("Expected NPE");
    } catch (NullPointerException expected) {
      // expected
    }
  }

  @J2ktIncompatible // Not emulated in J2KT
  public void testToUnmodifiableMap() {
    // verify simple cases copy all values and results are unmodifiable
    applyItems(
        Map.of("a", 0, "b", 1),
        toUnmodifiableMap(Function.identity(), k -> k.charAt(0) - 'a'),
        "a",
        "b");
    Map<String, Integer> tested =
        applyItemsWithSplitting(
            toUnmodifiableMap(Function.identity(), k -> k.charAt(0) - 'a'), "a", "b");
    assertUnmodifiableMap(tested, "a", 0, "z", 100);

    // verify merge works with only one key (but this is just passing through to the toMap func
    // anyway...)
    applyItems(
        Map.of("a", 2),
        toUnmodifiableMap(Function.identity(), ignore -> 1, Integer::sum),
        "a",
        "a");

    // verify nulls blow up for both keys and values
    try {
      Stream.of("a").collect(toUnmodifiableMap(obj -> null, Function.identity()));
      fail("Expected NPE");
    } catch (NullPointerException expected) {
      // expected
    }
    try {
      Stream.of("a").collect(toUnmodifiableMap(Function.identity(), obj -> null));
      fail("Expected NPE");
    } catch (NullPointerException expected) {
      // expected
    }
  }

  @J2ktIncompatible // Not emulated in J2KT
  public void testToUnmodifiableSet() { // Not emulated in J2KT
    applyItems(Set.of("a", "b"), toUnmodifiableSet(), "a", "b");
    assertUnmodifiableCollection(applyItemsWithSplitting(toUnmodifiableList(), "a", "b"), "a", "z");
    // verify nulls fail
    try {
      Stream.of("a").map(ignore -> null).collect(toUnmodifiableSet());
      fail("Expected NPE");
    } catch (NullPointerException expected) {
      // expected
    }
  }

  /**
   * This method attempts to apply a collector to items as a stream might do, so that we can simply
   * verify the output. Taken from the Collector class's javadoc.
   */
  private static <
          T extends @Nullable Object, A extends @Nullable Object, R extends @Nullable Object>
      void applyItems(
          R expected, Collector<T, A, R> collector, T t1, T t2, BiPredicate<R, R> equals) {
    assertTrue(
        "failed without splitting",
        equals.test(expected, applyItemsWithoutSplitting(collector, t1, t2)));
    assertTrue(
        "failed with splitting", equals.test(expected, applyItemsWithSplitting(collector, t1, t2)));
  }

  /**
   * This method attempts to apply a collector to items as a stream might do, so that we can simply
   * verify the output. Taken from the Collector class's javadoc.
   */
  private static <
          T extends @Nullable Object, A extends @Nullable Object, R extends @Nullable Object>
      void applyItems(R expected, Collector<T, A, R> collector, T t1, T t2) {
    applyItems(expected, collector, t1, t2, Object::equals);
  }

  /** Helper for applyItems. */
  private static <
          T extends @Nullable Object, A extends @Nullable Object, R extends @Nullable Object>
      R applyItemsWithoutSplitting(Collector<T, A, R> collector, T t1, T t2) {
    Supplier<A> supplier = collector.supplier();
    BiConsumer<A, T> accumulator = collector.accumulator();
    // unused in this impl
    BinaryOperator<A> combiner = collector.combiner();
    Function<A, R> finisher = collector.finisher();

    A a1 = supplier.get();

    accumulator.accept(a1, t1);
    accumulator.accept(a1, t2);

    // result without splitting
    R r1 = finisher.apply(a1);
    return r1;
  }

  /** Helper for applyItems. */
  private static <
          T extends @Nullable Object, A extends @Nullable Object, R extends @Nullable Object>
      R applyItemsWithSplitting(Collector<T, A, R> collector, T t1, T t2) {
    Supplier<A> supplier = collector.supplier();
    BiConsumer<A, T> accumulator = collector.accumulator();
    // actually used in this impl
    BinaryOperator<A> combiner = collector.combiner();
    Function<A, R> finisher = collector.finisher();

    A a2 = supplier.get();
    accumulator.accept(a2, t1);
    A a3 = supplier.get();
    accumulator.accept(a3, t2);

    // result with splitting
    R r2 = finisher.apply(combiner.apply(a2, a3));
    return r2;
  }

  private static <
          T extends @Nullable Object, A extends @Nullable Object, R extends @Nullable Object>
      void assertZeroItemsCollectedAs(R expected, Collector<T, A, R> collector) {
    assertZeroItemsCollectedAs(expected, collector, Object::equals);
  }

  private static <
          T extends @Nullable Object, A extends @Nullable Object, R extends @Nullable Object>
      void assertZeroItemsCollectedAs(
          R expected, Collector<T, A, R> collector, BiPredicate<R, R> equals) {
    Supplier<A> supplier = collector.supplier();
    // unused in this impl
    BiConsumer<A, T> accumulator = collector.accumulator();
    // shouldn't really be used, just handy to poke the internals quick
    BinaryOperator<A> combiner = collector.combiner();
    Function<A, R> finisher = collector.finisher();

    R actual = finisher.apply(supplier.get());
    assertTrue(equals, expected, actual);
    // doesn't actually ever happen, just internal checks
    actual = finisher.apply(combiner.apply(supplier.get(), supplier.get()));
    assertTrue(equals, expected, actual);
  }

  private static <
          T extends @Nullable Object, A extends @Nullable Object, R extends @Nullable Object>
      void assertSingleItemCollectedAs(R expected, Collector<T, A, R> collector, T item) {
    assertSingleItemCollectedAs(expected, collector, item, Object::equals);
  }

  private static <
          T extends @Nullable Object, A extends @Nullable Object, R extends @Nullable Object>
      void assertSingleItemCollectedAs(
          R expected, Collector<T, A, R> collector, T item, BiPredicate<R, R> equals) {
    Supplier<A> supplier = collector.supplier();
    BiConsumer<A, T> accumulator = collector.accumulator();
    // shouldn't really be used, just handy to poke the internals quick
    BinaryOperator<A> combiner = collector.combiner();
    Function<A, R> finisher = collector.finisher();

    A a1 = supplier.get();

    accumulator.accept(a1, item);

    R actual = finisher.apply(a1);
    // normal test
    assertTrue(equals, expected, actual);
    // these shouldn't really be used, just handy to poke the internals quick
    actual = finisher.apply(combiner.apply(a1, supplier.get()));
    assertTrue(equals, expected, actual);
    actual = finisher.apply(combiner.apply(supplier.get(), a1));
    assertTrue(equals, expected, actual);
  }

  private static <T extends @Nullable Object, U extends @Nullable Object> void assertTrue(
      BiPredicate<T, U> predicate, T expected, U actual) {
    assertTrue("expected= " + expected + ", actual=" + actual, predicate.test(expected, actual));
  }

  private static <T> void assertUnmodifiableCollection(
      Collection<T> c, T existingSample, T newSample) {
    try {
      c.remove(existingSample);
      fail();
    } catch (UnsupportedOperationException exoected) {
      // expected
    }
    try {
      c.add(newSample);
      fail();
    } catch (UnsupportedOperationException exoected) {
      // expected
    }
    Iterator<T> itr = c.iterator();
    itr.next();
    try {
      itr.remove();
      fail();
    } catch (UnsupportedOperationException exoected) {
      // expected
    }
  }

  private <K, V> void assertUnmodifiableMap(
      Map<K, V> a, K existingKey, V existingValue, K newKey, V newValue) {
    assertUnmodifiableCollection(a.keySet(), existingKey, newKey);
    assertUnmodifiableCollection(a.values(), existingValue, newValue);

    try {
      a.put(newKey, newValue);
      fail();
    } catch (UnsupportedOperationException exoected) {
      // expected
    }
    try {
      a.remove(existingKey);
      fail();
    } catch (UnsupportedOperationException exoected) {
      // expected
    }
  }
}
