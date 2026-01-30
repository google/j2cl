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

package com.google.j2cl.jre.java.util.stream;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThrows;

import com.google.j2cl.jre.java.util.EmulTestBase;
import com.google.j2cl.jre.testing.J2ktIncompatible;
import com.google.j2cl.jre.testing.TestUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** Tests {@link Stream}. */
public class StreamTest extends EmulTestBase {

  public void testEmptyStream() {
    Stream<Object> empty = Stream.empty();
    assertEquals(0, empty.count());
    assertThrows(IllegalStateException.class, () -> empty.count());

    assertEquals(0, Stream.empty().limit(2).collect(Collectors.toList()).size());
    assertEquals(0, Stream.empty().count());
    assertEquals(0, Stream.empty().limit(2).count());

    assertFalse(Stream.empty().findFirst().isPresent());
    assertFalse(Stream.empty().findAny().isPresent());
    assertFalse(Stream.<String>empty().max(Comparator.naturalOrder()).isPresent());
    assertFalse(Stream.<String>empty().min(Comparator.naturalOrder()).isPresent());
    assertTrue(Stream.empty().allMatch(item -> true));
    assertFalse(Stream.empty().anyMatch(item -> true));
    assertTrue(Stream.empty().noneMatch(item -> false));
    assertFalse(Stream.empty().iterator().hasNext());
    assertFalse(Stream.empty().spliterator().tryAdvance(a -> fail("should not advance")));
    Stream.empty().spliterator().forEachRemaining(a -> fail("should not advance"));
    assertEquals(new Object[0], Stream.empty().toArray());
    assertEquals(new Object[0], Stream.empty().toArray(Object[]::new));
  }

  public void testStreamOfOne() {
    Supplier<Stream<String>> one = () -> Stream.of("");
    assertEquals(Collections.singletonList(""), one.get().collect(Collectors.toList()));
    assertEquals(1L, one.get().count());
    assertEquals("", one.get().findFirst().get());
    assertEquals("", one.get().findAny().get());
  }

  public void testBuilder() {
    Supplier<Stream<String>> s = () -> Stream.<String>builder().add("1").add("3").add("2").build();

    Optional<String> max = s.get().filter(str -> !str.equals("3")).max(Comparator.naturalOrder());
    assertTrue(max.isPresent());
    assertEquals("2", max.get());

    max = s.get().max(Comparator.reverseOrder());
    assertTrue(max.isPresent());
    assertEquals("1", max.get());

    Stream.Builder<Object> builder = Stream.builder();
    Stream<Object> built = builder.build();
    assertEquals(0, built.count());
    assertThrows(IllegalStateException.class, () -> builder.build());
    assertThrows(IllegalStateException.class, () -> builder.add("asdf"));
  }

  public void testConcat() {
    Supplier<Stream<String>> adbc = () -> Stream.concat(Stream.of("a", "d"), Stream.of("b", "c"));

    assertEquals(new String[] {"a", "d", "b", "c"}, adbc.get().toArray(String[]::new));
    assertEquals(new String[] {"a", "b", "c", "d"}, adbc.get().sorted().toArray(String[]::new));

    List<String> closed = new ArrayList<>();
    Stream<String> first = Stream.of("first").onClose(() -> closed.add("first"));
    Stream<String> second = Stream.of("second").onClose(() -> closed.add("second"));

    Stream<String> concat = Stream.concat(first, second);

    // read everything, make sure we saw it all and didn't close automatically
    String collectedAll = concat.collect(Collectors.joining());
    assertEquals("firstsecond", collectedAll);
    assertEquals(0, closed.size());

    concat.close();
    assertEquals(Arrays.asList("first", "second"), closed);
  }

  public void testIterate() {
    assertEquals(new Integer[] {0, 1, 2, 3, 4}, Stream.iterate(0, i -> i + 1).limit(5).toArray());
    assertEquals(
        new Integer[] {10, 11, 12, 13, 14},
        Stream.iterate(0, i -> i + 1).skip(10).limit(5).toArray(Integer[]::new));

    // Check that the function is called the correct number of times
    int[] calledCount = {0};
    Integer[] array =
        Stream.iterate(
                0,
                val -> {
                  calledCount[0]++;
                  return val + 1;
                })
            .limit(5)
            .toArray(Integer[]::new);
    // Verify that the function was called for each value after the seed
    assertEquals(array.length - 1, calledCount[0]);
    // Sanity check the values returned
    assertEquals(new Integer[] {0, 1, 2, 3, 4}, array);
  }

  public void testIterate_predicate() {
    // Check that base case works
    assertEquals(
        new Integer[] {1, 2, 3, 4},
        Stream.iterate(1, x -> x < 5, x -> x + 1).toArray(Integer[]::new));

    // Check that negative to positive works
    assertEquals(
        new Integer[] {-2, -1, 0, 1, 2},
        Stream.iterate(-2, x -> x <= 2, x -> x + 1).toArray(Integer[]::new));

    // Check the initial element is not included if the predicate is x -> false
    assertEquals(new Integer[0], Stream.iterate(1, x -> false, x -> x + 1).toArray());

    // Check non incrementing sequence with limit 0
    assertEquals(
        new Integer[0], Stream.iterate(1, x -> x < 5, x -> x).limit(0).toArray(Integer[]::new));

    // Check decreasing sequence
    assertEquals(
        new Integer[] {8, 4, 2, 1},
        Stream.iterate(8, x -> x > 0, x -> x / 2).toArray(Integer[]::new));

    // Test with zero increment sequence
    assertEquals(
        new Integer[] {1, 1, 1, 1},
        Stream.iterate(1, x -> x < 5, x -> x).limit(4).toArray(Integer[]::new));
  }

  public void testGenerate() {
    // infinite, but if you limit it is already too short to skip much
    assertEquals(
        new Integer[] {},
        Stream.generate(makeGenerator()).limit(4).skip(5).toArray(Integer[]::new));

    assertEquals(
        new Integer[] {10, 11, 12, 13, 14},
        Stream.generate(makeGenerator()).skip(10).limit(5).toArray(Integer[]::new));
  }

  private Supplier<Integer> makeGenerator() {
    return new Supplier<Integer>() {
      int next = 0;

      @Override
      public Integer get() {
        return next++;
      }
    };
  }

  public void testSpliterator() {
    final String[] values = new String[] {"a", "b", "c"};

    Spliterator<String> spliterator = Stream.of(values).spliterator();
    assertEquals(3, spliterator.estimateSize());
    assertEquals(3, spliterator.getExactSizeIfKnown());

    List<String> actualValues = new ArrayList<>();
    while (spliterator.tryAdvance(actualValues::add)) {
      // work is all done in the condition
    }

    assertEquals(asList(values), actualValues);
  }

  public void testIterator() {
    final String[] values = new String[] {"a", "b", "c"};

    List<String> actualValues = new ArrayList<String>();
    Iterator<String> iterator = Stream.of(values).iterator();
    while (iterator.hasNext()) {
      actualValues.add(iterator.next());
    }
    assertEquals(asList(values), actualValues);
  }

  public void testForEach() {
    final String[] values = new String[] {"a", "b", "c"};

    List<String> actualValues = new ArrayList<>();
    Stream.of(values).forEach(actualValues::add);
    assertEquals(asList(values), actualValues);
  }

  // toArray
  public void testToArray() {
    assertEquals(new Object[] {"a", "b"}, asList("a", "b").stream().toArray());
    assertEquals(new String[] {"a", "b"}, asList("a", "b").stream().toArray(String[]::new));
  }

  // reduce
  public void testReduce() {
    String reduced = Stream.of("a", "b", "c").reduce("", String::concat);
    assertEquals("abc", reduced);

    reduced = Stream.<String>of().reduce("initial", String::concat);
    assertEquals("initial", reduced);

    Optional<String> maybe = Stream.of("a", "b", "c").reduce(String::concat);
    assertTrue(maybe.isPresent());
    assertEquals("abc", maybe.get());
    maybe = Stream.<String>of().reduce(String::concat);
    assertFalse(maybe.isPresent());

    reduced = Stream.of("a", "b", "c").reduce("", String::concat, String::concat);
    assertEquals("abc", reduced);
  }

  public void testCollect() {
    final String[] values = new String[] {"a", "b", "c"};

    String collectedString =
        Stream.of(values)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString();
    assertEquals("abc", collectedString);

    List<String> collectedList = Stream.of(values).collect(Collectors.toList());
    assertEquals(asList(values), collectedList);
  }

  public void testFilter() {
    // unconsumed stream never runs filter
    boolean[] data = {false};
    Stream.of(1, 2, 3).filter(i -> data[0] |= true);
    assertFalse(data[0]);

    // Nothing's *defined* to care about the Spliterator characteristics, but the implementation
    // can't actually know the size before executing, so we check the characteristics explicitly.
    assertFalse(
        Stream.of("a", "b", "c", "d", "c")
            .filter(a -> a.equals("a"))
            .spliterator()
            .hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED));

    // one result
    assertEquals(
        Collections.singletonList("a"),
        Stream.of("a", "b", "c", "d", "c").filter(a -> a.equals("a")).collect(Collectors.toList()));
    // zero results
    assertEquals(
        Collections.emptyList(),
        Stream.of("a", "b", "c", "d", "c").filter(a -> false).collect(Collectors.toList()));
    // two results
    assertEquals(
        asList("c3", "c5"),
        Stream.of("a1", "b2", "c3", "d4", "c5")
            .filter(a -> a.startsWith("c"))
            .collect(Collectors.toList()));
    // all
    assertEquals(
        asList("a", "b", "c", "d", "c"),
        Stream.of("a", "b", "c", "d", "c").filter(a -> true).collect(Collectors.toList()));
  }

  public void testMap() {
    // unconsumed stream never runs map
    boolean[] data = {false};
    Stream.of(1, 2, 3).map(i -> data[0] |= true);
    assertFalse(data[0]);

    assertEquals(
        asList("#1", "#2", "#3"),
        Stream.of(1, 2, 3).map(i -> "#" + i).collect(Collectors.toList()));
  }

  public void testPeek() {
    // unconsumed stream never peeks
    boolean[] data = {false};
    Stream.of(1, 2, 3).peek(i -> data[0] |= true);
    assertFalse(data[0]);

    // make sure we saw it all in order
    List<String> items = asList("a", "b", "c");
    List<String> peeked = new ArrayList<>();
    items.stream()
        .peek(peeked::add)
        .forEach(
            item -> {
              // deliberately do nothing, just run
            });
    assertEquals(items, peeked);
  }

  // same impl, no parallel in browser
  public void testFindFirstOrAny() {
    Optional<String> any = Stream.of("a", "b").findAny();
    assertTrue(any.isPresent());
    assertEquals("a", any.get());
  }

  public void testAnyMatch() {
    // all
    assertTrue(Stream.of("a", "b").anyMatch(s -> true));
    // some
    assertTrue(Stream.of("a", "b").anyMatch(s -> s.equals("a")));
    // none
    assertFalse(Stream.of("a", "b").anyMatch(s -> false));

    // With null values.
    // all
    // TODO(b/315476228): Remove explicit type here and below
    assertTrue(Stream.<@Nullable String>of(null, "a", "b").anyMatch(s -> true));
    // some
    assertTrue(Stream.<@Nullable String>of(null, "a", "b").anyMatch(s -> s == null));
    // none
    assertFalse(Stream.<@Nullable String>of(null, "a", "b").anyMatch(s -> false));
  }

  public void testAllMatch() {
    // all
    assertTrue(Stream.of("a", "b").allMatch(s -> true));
    // some
    assertFalse(Stream.of("a", "b").allMatch(s -> s.equals("a")));
    // none
    assertFalse(Stream.of("a", "b").allMatch(s -> false));

    // With null values.
    // all
    assertTrue(Stream.<@Nullable String>of(null, "a", "b").allMatch(s -> true));
    // some
    assertFalse(Stream.<@Nullable String>of(null, "a", "b").allMatch(s -> s != null));
    // none
    assertFalse(Stream.<@Nullable String>of(null, "a", "b").allMatch(s -> false));
  }

  public void testNoneMatch() {
    // all
    assertFalse(Stream.of("a", "b").noneMatch(s -> true));
    // some
    assertFalse(Stream.of("a", "b").noneMatch(s -> s.equals("a")));
    // none
    assertTrue(Stream.of("a", "b").noneMatch(s -> false));

    // With null values.
    // all
    assertFalse(Stream.<@Nullable String>of(null, "a", "b").noneMatch(s -> true));
    // some
    assertFalse(Stream.<@Nullable String>of(null, "a", "b").noneMatch(s -> s == null));
    // none
    assertTrue(Stream.<@Nullable String>of(null, "a", "b").noneMatch(s -> false));
  }

  public void testFlatMap() {
    assertEquals(0, Stream.<Stream<String>>empty().flatMap(Function.identity()).count());
    assertEquals(0, Stream.of(Stream.<String>empty()).flatMap(Function.identity()).count());
    assertEquals(0, Stream.of(Stream.of()).flatMap(Function.identity()).count());
    assertEquals(1, Stream.of(Stream.of("")).flatMap(Function.identity()).count());

    Stream<Stream<String>> strings = Stream.of(Stream.of("a", "b"), Stream.empty(), Stream.of("c"));

    assertEquals(
        asList("a", "b", "c"), strings.flatMap(Function.identity()).collect(Collectors.toList()));
  }

  public void testMapToPrimitives() {
    Supplier<Stream<String>> s = () -> Stream.of("1", "2", "10");

    assertEquals(new int[] {1, 2, 10}, s.get().mapToInt(Integer::parseInt).toArray());

    assertEquals(new long[] {1, 2, 10}, s.get().mapToLong(Long::parseLong).toArray());

    assertEquals(new double[] {1, 2, 10}, s.get().mapToDouble(Double::parseDouble).toArray());
  }

  public void testFlatMapToPrimitives() {
    assertEquals(0, Stream.<IntStream>empty().flatMapToInt(Function.identity()).count());
    assertEquals(0, Stream.of(IntStream.empty()).flatMapToInt(Function.identity()).count());
    assertEquals(0, Stream.of(IntStream.of()).flatMapToInt(Function.identity()).count());
    assertEquals(1, Stream.of(IntStream.of(0)).flatMapToInt(Function.identity()).count());

    Stream<IntStream> intStreams =
        Stream.of(IntStream.of(1, 2), IntStream.empty(), IntStream.of(5));
    assertEquals(new int[] {1, 2, 5}, intStreams.flatMapToInt(Function.identity()).toArray());

    Stream<LongStream> longStreams =
        Stream.of(LongStream.of(1, 2), LongStream.empty(), LongStream.of(5));
    assertEquals(new long[] {1, 2, 5}, longStreams.flatMapToLong(Function.identity()).toArray());

    Stream<DoubleStream> doubleStreams =
        Stream.of(DoubleStream.of(1, 2), DoubleStream.empty(), DoubleStream.of(5));
    assertEquals(
        new double[] {1, 2, 5}, doubleStreams.flatMapToDouble(Function.identity()).toArray());
  }

  public void testDistinct() {
    List<String> distinct =
        asList("a", "b", "c", "b").stream().distinct().collect(Collectors.toList());
    assertEquals(3, distinct.size());
    assertTrue(distinct.contains("a"));
    assertTrue(distinct.contains("b"));
    assertTrue(distinct.contains("c"));
  }

  public void testSorted() {
    List<String> sorted = asList("c", "a", "b").stream().sorted().collect(Collectors.toList());
    List<String> reversed =
        asList("c", "a", "b").stream()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());

    assertEquals(asList("a", "b", "c"), sorted);
    assertEquals(asList("c", "b", "a"), reversed);
  }

  public void testMinMax() {
    Supplier<Stream<String>> stream = () -> Stream.of("b", "c", "d", "a");

    assertEquals("a", stream.get().min(Comparator.naturalOrder()).orElse(null));
    assertEquals("d", stream.get().min(Comparator.reverseOrder()).orElse(null));
    assertEquals("a", stream.get().max(Comparator.reverseOrder()).orElse(null));
    assertEquals("d", stream.get().max(Comparator.naturalOrder()).orElse(null));

    assertFalse(stream.get().filter(a -> false).max(Comparator.naturalOrder()).isPresent());
    assertFalse(stream.get().filter(a -> false).min(Comparator.naturalOrder()).isPresent());
  }

  public void testCountLimitSkip() {
    Supplier<Stream<String>> stream = () -> asList("a", "b", "c", "d").stream();

    assertEquals(4, stream.get().count());

    assertEquals(4, stream.get().limit(4).count());
    assertEquals(4, stream.get().limit(5).count());
    assertEquals(3, stream.get().limit(3).count());

    assertEquals(3, stream.get().skip(1).limit(3).count());

    assertEquals(2, stream.get().limit(3).skip(1).count());

    assertEquals(1, stream.get().skip(3).count());

    assertEquals(asList("c", "d"), stream.get().skip(2).limit(3).collect(Collectors.toList()));
    assertEquals(
        Collections.singletonList("c"), stream.get().skip(2).limit(1).collect(Collectors.toList()));

    assertEquals(Collections.singletonList("d"), stream.get().skip(3).collect(Collectors.toList()));
    assertEquals(Collections.emptyList(), stream.get().skip(5).collect(Collectors.toList()));

    assertEquals(asList("a", "b"), stream.get().limit(2).collect(Collectors.toList()));

    assertEquals(
        Collections.singletonList("b"), stream.get().limit(2).skip(1).collect(Collectors.toList()));
  }

  // This frustrating test was written first on the JVM stream to discover the basic behavior before
  // trying to implement it in GWT. As far as I can tell, none of this is clearly described in
  // javadoc. Also note that it is *not* required to use the returned stream from calling onClose
  public void testCloseQuirks() {
    // all subclasses use the same close()/onClose(...) impl, just test once with Stream.empty()

    Stream<Object> s = Stream.of(1);
    s.close();
    // allow multiple close
    s.close();

    // Add a handler, close, and attempt to re-close - handler only runs the one time
    int[] calledCount = {0};
    s = Stream.empty();
    s.onClose(() -> calledCount[0]++);
    // shouldn't have been called yet
    assertEquals(0, calledCount[0]);
    s.close();
    // called once
    assertEquals(1, calledCount[0]);
    s.close();
    // not called again on subsequent closes
    assertEquals(1, calledCount[0]);

    if (TestUtils.getJdkVersion() >= 11) {
      var s2 = s;
      assertThrows(IllegalStateException.class, () -> s2.onClose(() -> calledCount[0]++));
      return;
    }

    // Add a handler after close, and re-close, the handler will only go off after the _second_
    // close
    calledCount[0] = 0;
    s = Stream.of(1);
    s.close();
    s = s.onClose(() -> calledCount[0]++);
    // shouldn't have been called yet
    assertEquals(0, calledCount[0]);
    s.close();
    // frustratingly, the JVM apparently permits each handler when added to let the stream be closed
    // _again_
    assertEquals(1, calledCount[0]);

    // Adding yet another runnable and closing again demonstrates this - only the new one is run,
    // the old ones are not
    s = s.onClose(() -> calledCount[0]++);
    s.close();
    assertEquals(2, calledCount[0]);

    // Add two handlers, ensure both are called, and neither called the second time the stream is
    // closed
    calledCount[0] = 0;
    s = Stream.empty();
    s = s.onClose(() -> calledCount[0]++);
    s = s.onClose(() -> calledCount[0]++);
    s.close();
    assertEquals(2, calledCount[0]);
    s.close();
    assertEquals(2, calledCount[0]);
  }

  public void testClose() {
    // terminate stream before closing, confirm that handler is called
    Stream<Object> s = Stream.of("a", "b", "c");
    int[] calledCount = {0};
    s = s.onClose(() -> calledCount[0]++);

    long count = s.count();
    assertEquals(3, count);
    assertEquals(0, calledCount[0]);

    s.close();
    assertEquals(1, calledCount[0]);

    // terminate stream after closing, confirm that handler is called, and terminating fails
    s = Stream.of("a", "b", "c");
    calledCount[0] = 0;
    s = s.onClose(() -> calledCount[0]++);

    s.close();
    assertEquals(1, calledCount[0]);

    var s2 = s;
    assertThrows(IllegalStateException.class, () -> s2.count());
    assertEquals(1, calledCount[0]);
  }

  public void testCloseException() {
    // Try a single exception, confirm we catch it
    Stream<Object> s = Stream.of(1, 2, 3);

    RuntimeException a = new RuntimeException("a");
    s.onClose(
        () -> {
          throw a;
        });
    RuntimeException e = assertThrows(RuntimeException.class, () -> s.close());
    assertSame(a, e);
    assertEquals(0, e.getSuppressed().length);

    // Throw an exception in two of the three handlers, confirm both arrive and the third was called
    // correctly
    Stream<Object> s2 = Stream.of(1, 2, 3);

    RuntimeException a2 = new RuntimeException("a");
    IllegalStateException b = new IllegalStateException("b");
    int[] calledCount = {0};
    s2.onClose(
            () -> {
              throw a2;
            })
        .onClose(
            () -> {
              throw b;
            })
        .onClose(
            () -> {
              throw a2;
            })
        .onClose(
            () -> {
              throw b;
            })
        .onClose(() -> calledCount[0]++);

    RuntimeException e2 = assertThrows(RuntimeException.class, () -> s2.close());
    assertSame(a2, e2);
    assertEquals(new Throwable[] {b, b}, e2.getSuppressed());
    assertEquals(1, calledCount[0]);

    // Throw the same exception instance twice, ensure it only arrives once
    Stream<Object> s3 = Stream.of(1, 2, 3);

    RuntimeException t = new RuntimeException("a");
    s3.onClose(
            () -> {
              throw t;
            })
        .onClose(
            () -> {
              throw t;
            });

    RuntimeException e3 = assertThrows(RuntimeException.class, () -> s3.close());
    assertSame(t, e3);
    assertEquals(0, e3.getSuppressed().length);
  }

  public void testOfNullable() {
    assertEquals(0, Stream.ofNullable(null).count());
    assertEquals(new String[] {"abc"}, Stream.ofNullable("abc").toArray());
  }

  @J2ktIncompatible // Not emulated.
  public void testTakeWhile() {
    assertEquals(new Integer[] {1, 2}, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray());
    assertEquals(0, Stream.of(1, 2, 3, 4, 5, 1).takeWhile(i -> i > 2).count());

    assertEquals(
        new Integer[] {0, 1, 2, 3, 4},
        Stream.iterate(0, i -> i + 1).takeWhile(i -> i < 5).toArray());
    assertEquals(0, Stream.<Integer>empty().takeWhile(n -> n < 4).count());
    assertEquals(0, Stream.of(5, 6, 7).takeWhile(n -> n < 5).count());
    assertEquals(new Integer[] {1, 2, 3}, Stream.of(1, 2, 3).takeWhile(n -> n < 4).toArray());

    // pass an infinite stream to takeWhile, ensure it handles it
    assertEquals(
        new Integer[] {0, 1, 2, 3, 4},
        Stream.iterate(0, i -> i + 1).takeWhile(i -> true).limit(5).toArray());
  }

  @J2ktIncompatible // Not emulated.
  public void testDropWhile() {
    assertEquals(new Integer[] {3, 4, 5}, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray());
    assertEquals(
        new Integer[] {1, 2, 3, 4, 5}, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i > 2).toArray());

    // pass an infinite stream to dropWhile, ensure it handles it
    assertEquals(
        new Integer[] {5, 6, 7, 8, 9},
        Stream.iterate(0, i -> i + 1).dropWhile(i -> i < 5).limit(5).toArray());
  }
}
