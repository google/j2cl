/*
 * Copyright 2025 Google Inc.
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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class StreamCollectWildcardProblem {
  interface Foo<T> {
    Foo<T> getThis();
  }

  public static void testCollect(Stream<Foo<?>> list) {
    accept(list.collect(toList()));
  }

  // TODO(b/393561019): Uncomment when fixed
  // public static void testMethodReferenceMapCollect(Stream<Foo<?>> list) {
  //   accept(list.map(Foo::getThis).collect(toList()));
  // }

  // TODO(b/393561019): Uncomment when fixed
  // public static void testLambdaMapCollect(Stream<Foo<?>> list) {
  //   accept(list.map(it -> it.getThis()).collect(toList()));
  // }

  // Currently, this is how users workaround problem with J2KT.
  public static void testCollectWithExplicitTypeArgumentFix(Stream<Foo<?>> list) {
    accept(list.collect(Collectors.<Foo<?>>toList()));
  }

  // Currently, this is how users workaround problem with J2KT.
  public static void testMethodReferenceMapCollectWithExplicitTypeArgumentFix(Stream<Foo<?>> list) {
    accept(list.map(Foo::getThis).collect(Collectors.<Foo<?>>toList()));
  }

  // Currently, this is how users workaround problem with J2KT.
  public static void testLambdaMapCollectWithExplicitTypeArgumentFix(Stream<Foo<?>> list) {
    accept(list.map(it -> it.getThis()).collect(Collectors.<Foo<?>>toList()));
  }

  public static void accept(List<Foo<?>> list) {
    throw new RuntimeException();
  }
}
