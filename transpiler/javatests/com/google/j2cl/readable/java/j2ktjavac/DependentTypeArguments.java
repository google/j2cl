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

/** Tests covering complex cases from {@code java.util.stream.Collectors}. */
@NullMarked
public class DependentTypeArguments {
  public static <
          TestK extends @Nullable Object,
          TestV extends @Nullable Object,
          TestF extends Function<TestK, TestV>>
      Collector<?, TestF> test1_implicitTypeArguments(
          final TestF function, final BiFunction<TestV, TestV, TestV> mergeFunction) {
    return Collector.of(function, (m1, m2) -> mergeAll(m1, m2, mergeFunction));
  }

  public static <
          TestK extends @Nullable Object,
          TestV extends @Nullable Object,
          TestF extends Function<TestK, TestV>>
      Collector<?, TestF> test1_explicitTypeArguments(
          final TestF function, final BiFunction<TestV, TestV, TestV> mergeFunction) {
    return Collector.of(
        function,
        (m1, m2) -> DependentTypeArguments.<TestK, TestV, TestF>mergeAll(m1, m2, mergeFunction));
  }

  // J2KT with javac frontend renders `BiFunction` argument as:
  //
  // BiFunction { m1: Function<TestK, Supplier<TestE>>, m2: Function<TestK, Supplier<TestE>> ->
  //   return@BiFunction DependentTypeArguments.mergeAll<Any?, Supplier<TestE>, Function<TestK,
  // Supplier<TestE>>>(
  //     m1, m2,
  //     BiFunction { arg0: Supplier<TestE>, arg1: Supplier<TestE> ->
  //      return@BiFunction DependentTypeArguments.addAll<TestE, Supplier<TestE>>(arg0, arg1)
  //     },
  //   }
  // }
  //
  // and should be:
  //
  // BiFunction { m1: Function<TestK, Supplier<TestE>>, m2: Function<TestK, Supplier<TestT>> ->
  //   return@BiFunction DependentTypeArguments.mergeAll<TestK, Supplier<TestE>, Function<TestK,
  // Supplier<TestE>>>(
  //     m1, m2,
  //     BiFunction { arg0: Supplier<TestE>, arg1: Supplier<TestE> ->
  //      return@BiFunction DependentTypeArguments.addAll<TestE, Supplier<TestE>>(arg0, arg1)
  //     },
  //   }
  // }
  private static <
          TestE extends @Nullable Object,
          TestK extends @Nullable Object,
          TestV extends @Nullable Object,
          TestF extends Function<TestK, TestV>>
      Collector<?, TestF> test2_implicitTypeArguments(
          Function<TestK, Supplier<TestE>> acum, TestF map) {
    return Collector.of(
        acum, (m1, m2) -> mergeAll(m1, m2, DependentTypeArguments::addAll), unused -> map);
  }

  private static <
          TestE extends @Nullable Object,
          TestK extends @Nullable Object,
          TestV extends @Nullable Object,
          TestF extends Function<TestK, TestV>>
      Collector<?, TestF> test2_explicitTypeArguments(
          Function<TestK, Supplier<TestE>> acum, TestF map) {
    return Collector.of(
        acum,
        (m1, m2) ->
            DependentTypeArguments
                .<TestK, Supplier<TestE>, Function<TestK, Supplier<TestE>>>mergeAll(
                    m1, m2, DependentTypeArguments::addAll),
        unused -> map);
  }

  public interface Supplier<SupOut extends @Nullable Object> {
    SupOut get();
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

  public interface BiConsumer<BiIn1 extends @Nullable Object, BiIn2 extends @Nullable Object> {
    void accept(BiIn1 in1, BiIn2 in2);
  }

  public interface Collector<CollAcum extends @Nullable Object, CollRes extends @Nullable Object> {
    static <OfRes extends @Nullable Object> Collector<OfRes, OfRes> of(
        OfRes start, BiFunction<OfRes, OfRes, OfRes> combiner) {
      throw new RuntimeException();
    }

    static <OfAcum extends @Nullable Object, OfRes extends @Nullable Object>
        Collector<OfAcum, OfRes> of(
            OfAcum start,
            BiFunction<OfAcum, OfAcum, OfAcum> combiner,
            Function<OfAcum, OfRes> finisher) {
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

  private static <AddElem extends @Nullable Object, AddSup extends Supplier<AddElem>> AddSup addAll(
      AddSup supplier, Supplier<AddElem> sup2) {
    throw new RuntimeException();
  }
}
