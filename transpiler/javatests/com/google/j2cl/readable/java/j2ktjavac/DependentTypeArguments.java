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
package j2ktjavac;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DependentTypeArguments {
  // J2KT with javac frontend renders `BiFunction` argument as:
  //
  // BiFunction { m1: TestF, m2: TestF ->
  //   return@BiFunction mergeAll<Any?, TestV, TestF>(m1, m2, mergeFunction)
  // }
  //
  // and should be:
  //
  // BiFunction { m1: TestF, m2: TestF ->
  //   return@BiFunction mergeAll<TestK, TestV, TestF>(m1, m2, mergeFunction)
  // }
  public static <
          TestK extends @Nullable Object,
          TestV extends @Nullable Object,
          TestF extends Function<TestK, TestV>>
      Collector<?, TestF> testImplicitTypeArguments(
          final TestF function, final BiFunction<TestV, TestV, TestV> mergeFunction) {
    return Collector.of(function, (m1, m2) -> mergeAll(m1, m2, mergeFunction));
  }

  public static <
          TestK extends @Nullable Object,
          TestV extends @Nullable Object,
          TestF extends Function<TestK, TestV>>
      Collector<?, TestF> testExplicitTypeArguments(
          final TestF function, final BiFunction<TestV, TestV, TestV> mergeFunction) {
    return Collector.of(
        function,
        (m1, m2) -> DependentTypeArguments.<TestK, TestV, TestF>mergeAll(m1, m2, mergeFunction));
  }

  public interface Function<FunIn extends @Nullable Object, FunOut extends @Nullable Object> {
    FunOut apply(FunIn in);
  }

  public interface BiFunction<
      BiIn1 extends @Nullable Object,
      BiIn2 extends @Nullable Object,
      BiOut extends @Nullable Object> {
    BiOut apply(BiIn1 in1, BiIn2 in2);
  }

  public interface Collector<CollAcum extends @Nullable Object, CollRes extends @Nullable Object> {
    static <OfRes extends @Nullable Object> Collector<OfRes, OfRes> of(
        OfRes supplier, BiFunction<OfRes, OfRes, OfRes> combiner) {
      throw new RuntimeException();
    }
  }

  private static <
          MergeK extends @Nullable Object,
          MergeV extends @Nullable Object,
          MergeF extends Function<MergeK, MergeV>>
      MergeF mergeAll(MergeF m1, MergeF m2, BiFunction<MergeV, MergeV, MergeV> mergeFunction) {
    throw new RuntimeException();
  }
}
