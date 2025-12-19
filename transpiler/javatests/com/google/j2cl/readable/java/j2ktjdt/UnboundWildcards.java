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
package j2ktjdt;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

// TODO(b/397636459): Uncomment test cases when fixed.
@NullMarked
public class UnboundWildcards {
  interface Foo<T extends @Nullable Object> {}

  public static void test111(Foo<? extends @Nullable Object> extendsNullableObject) {
    acceptTExtendsNullableObject(extendsNullableObject);
  }

  public static void test112(Foo<? extends @Nullable Object> extendsNullableObject) {
    acceptTExtendsObject(extendsNullableObject);
  }

  public static void test113(Foo<? extends @Nullable Object> extendsNullableObject) {
    acceptTExtendsNonNullObject(extendsNullableObject);
  }

  public static void test114(Foo<? extends @Nullable Object> extendsNullableObject) {
    acceptT(extendsNullableObject);
  }

  public static void test121(Foo<? extends @Nullable Object> extendsNullableObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsNullableObject(extendsNullableObject);
  }

  public static void test122(Foo<? extends @Nullable Object> extendsNullableObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsObject(extendsNullableObject);
  }

  public static void test123(Foo<? extends @Nullable Object> extendsNullableObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsNonNullObject(extendsNullableObject);
  }

  public static void test124(Foo<? extends @Nullable Object> extendsNullableObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableT(extendsNullableObject);
  }

  public static void test131(Foo<? extends @Nullable Object> extendsNullableObject) {
    acceptNonNullTExtendsNullableObject(extendsNullableObject);
  }

  public static void test132(Foo<? extends @Nullable Object> extendsNullableObject) {
    acceptNonNullTExtendsObject(extendsNullableObject);
  }

  public static void test133(Foo<? extends @Nullable Object> extendsNullableObject) {
    acceptNonNullTExtendsNonNullObject(extendsNullableObject);
  }

  public static void test134(Foo<? extends @Nullable Object> extendsNullableObject) {
    acceptNonNullT(extendsNullableObject);
  }

  public static void test211(Foo<? extends Object> extendsObject) {
    acceptTExtendsNullableObject(extendsObject);
  }

  public static void test212(Foo<? extends Object> extendsObject) {
    acceptTExtendsObject(extendsObject);
  }

  public static void test213(Foo<? extends Object> extendsObject) {
    acceptTExtendsNonNullObject(extendsObject);
  }

  public static void test214(Foo<? extends Object> extendsObject) {
    acceptT(extendsObject);
  }

  public static void test221(Foo<? extends Object> extendsObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsNullableObject(extendsObject);
  }

  public static void test222(Foo<? extends Object> extendsObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsObject(extendsObject);
  }

  public static void test223(Foo<? extends Object> extendsObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsNonNullObject(extendsObject);
  }

  public static void test224(Foo<? extends Object> extendsObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableT(extendsObject);
  }

  public static void test231(Foo<? extends Object> extendsObject) {
    acceptNonNullTExtendsNullableObject(extendsObject);
  }

  public static void test232(Foo<? extends Object> extendsObject) {
    acceptNonNullTExtendsObject(extendsObject);
  }

  public static void test233(Foo<? extends Object> extendsObject) {
    acceptNonNullTExtendsNonNullObject(extendsObject);
  }

  public static void test234(Foo<? extends Object> extendsObject) {
    acceptNonNullT(extendsObject);
  }

  public static void test311(Foo<? extends @NonNull Object> extendsNonNullObject) {
    acceptTExtendsNullableObject(extendsNonNullObject);
  }

  public static void test312(Foo<? extends @NonNull Object> extendsNonNullObject) {
    acceptTExtendsObject(extendsNonNullObject);
  }

  public static void test313(Foo<? extends @NonNull Object> extendsNonNullObject) {
    acceptTExtendsNonNullObject(extendsNonNullObject);
  }

  public static void test314(Foo<? extends @NonNull Object> extendsNonNullObject) {
    acceptT(extendsNonNullObject);
  }

  public static void test321(Foo<? extends @NonNull Object> extendsNonNullObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsNullableObject(extendsNonNullObject);
  }

  public static void test322(Foo<? extends @NonNull Object> extendsNonNullObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsObject(extendsNonNullObject);
  }

  public static void test323(Foo<? extends @NonNull Object> extendsNonNullObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsNonNullObject(extendsNonNullObject);
  }

  public static void test324(Foo<? extends @NonNull Object> extendsNonNullObject) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableT(extendsNonNullObject);
  }

  public static void test331(Foo<? extends @NonNull Object> extendsNonNullObject) {
    acceptNonNullTExtendsNullableObject(extendsNonNullObject);
  }

  public static void test332(Foo<? extends @NonNull Object> extendsNonNullObject) {
    acceptNonNullTExtendsObject(extendsNonNullObject);
  }

  public static void test333(Foo<? extends @NonNull Object> extendsNonNullObject) {
    acceptNonNullTExtendsNonNullObject(extendsNonNullObject);
  }

  public static void test334(Foo<? extends @NonNull Object> extendsNonNullObject) {
    acceptNonNullT(extendsNonNullObject);
  }

  public static void test411(Foo<?> unbound) {
    acceptTExtendsNullableObject(unbound);
  }

  public static void test412(Foo<?> unbound) {
    acceptTExtendsObject(unbound);
  }

  public static void test413(Foo<?> unbound) {
    acceptTExtendsNonNullObject(unbound);
  }

  public static void test414(Foo<?> unbound) {
    acceptT(unbound);
  }

  public static void test421(Foo<?> unbound) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsNullableObject(unbound);
  }

  public static void test422(Foo<?> unbound) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsObject(unbound);
  }

  public static void test423(Foo<?> unbound) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableTExtendsNonNullObject(unbound);
  }

  public static void test424(Foo<?> unbound) {
    // Incompatible due to nullability differences that cannot be solved with casts in Kotlin.
    // acceptNullableT(unbound);
  }

  public static void test431(Foo<?> unbound) {
    acceptNonNullTExtendsNullableObject(unbound);
  }

  public static void test432(Foo<?> unbound) {
    acceptNonNullTExtendsObject(unbound);
  }

  public static void test433(Foo<?> unbound) {
    acceptNonNullTExtendsNonNullObject(unbound);
  }

  public static void test434(Foo<?> unbound) {
    acceptNonNullT(unbound);
  }

  public static <T extends @Nullable Object> void acceptTExtendsNullableObject(Foo<T> foo) {}

  public static <T extends Object> void acceptTExtendsObject(Foo<T> foo) {}

  public static <T extends @NonNull Object> void acceptTExtendsNonNullObject(Foo<T> foo) {}

  public static <T> void acceptT(Foo<T> foo) {}

  public static <T extends @Nullable Object> void acceptNullableTExtendsNullableObject(
      Foo<@Nullable T> foo) {}

  public static <T extends Object> void acceptNullableTExtendsObject(Foo<@Nullable T> foo) {}

  public static <T extends @NonNull Object> void acceptNullableTExtendsNonNullObject(
      Foo<@Nullable T> foo) {}

  public static <T> void acceptNullableT(Foo<@Nullable T> foo) {}

  public static <T extends @Nullable Object> void acceptNonNullTExtendsNullableObject(
      Foo<@NonNull T> foo) {}

  public static <T extends Object> void acceptNonNullTExtendsObject(Foo<@NonNull T> foo) {}

  public static <T extends @NonNull Object> void acceptNonNullTExtendsNonNullObject(
      Foo<@NonNull T> foo) {}

  public static <T> void acceptNonNullT(Foo<@NonNull T> foo) {}
}
