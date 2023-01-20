/*
 * Copyright 2017 Google Inc.
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
package typewildcards;

interface Function<I, O> {
  O apply(I i);
}

interface List<I> {}

class GenericType<T> {
  T get() {
    return null;
  }
}

class Bar {}

class RecursiveType<T extends RecursiveType<T>> {
  public RecursiveType(RecursiveType<?> wildcardParameter) {}
}

interface DeepRecursiveType<T extends GenericType<? super T>> {}

class RecursiveSubtype extends RecursiveType<RecursiveSubtype> {
  public RecursiveSubtype(RecursiveType<?> wildcardParameter) {
    super(wildcardParameter);
  }
}

public class TypeWildCards {
  public Object unbounded(GenericType<?> g) {
    return g.get();
  }

  public Bar upperBound(GenericType<? extends Bar> g) {
    return g.get();
  }

  public Object lowerBound(GenericType<? super Bar> g) {
    return g.get();
  }

  public void unboundedRecursive(RecursiveType<?> g) {}

  public void upperBoundRecursive(GenericType<? extends RecursiveType<?>> g) {}

  public void lowerBoundRecursive(GenericType<? super RecursiveType<?>> g) {}

  public void deepRecursiveType(DeepRecursiveType<?> t) {}

  public void test() {
    unbounded(new GenericType<Bar>());
    upperBound(new GenericType<Bar>());
    lowerBound(new GenericType<Bar>());
  }

  private interface X {
    void m();
  }

  private interface Y {
    void n();
  }

  private static class A implements X {
    int f;

    @Override
    public void m() {}
  }

  public static <T extends A> void testBoundedTypeMemberAccess(T t) {
    int i = t.f;
    t.m();
  }

  public static <T extends A & Y> void testIntersectionBoundedTypeMemberAccess(T t) {
    int i = t.f;
    t.m();
    t.n();
  }

  public interface IntegerSupplier {
    Integer get();
  }

  public interface HasKey {
    String getKey();
  }

  public abstract class Element implements HasKey, IntegerSupplier {}

  public abstract class OtherElement implements IntegerSupplier, HasKey {}

  public abstract class SubOtherElement extends OtherElement implements HasKey {}

  private static <F, V> List<V> transform(List<F> from, Function<? super F, ? extends V> function) {
    return null;
  }

  private static <E> List<E> concat(List<? extends E> a, List<? extends E> b) {
    return null;
  }

  public void testInferredGenericIntersection() {
    List<Element> elements = null;
    List<SubOtherElement> otherElements = null;
    // This is a rather complicated way to make sure the inference ends with a wildcard extending
    // multiple bounds. This is type of code that  exposes b/67858399.
    List<Integer> integers =
        transform(
            concat(elements, otherElements),
            a -> {
              a.getKey();
              return a.get();
            });
  }

  class Foo extends GenericType<Foo> {}

  private static void takesRecursiveGeneric(GenericType<Foo> foo) {}

  public void testRecursiveGeneric() {
    takesRecursiveGeneric(new Foo());
  }

  interface RecursiveInterface<T extends RecursiveInterface<T, C>, C> {
    T m();
  }

  public static <C> C testInferredIntersectionWithTypeVariable(
      RecursiveInterface<? extends C, C> ri) {
    // This creates an intersection type "C & RecursiveInterface" by using C in both parameters.
    // That type surfaces as the bound of a capture due to unifying the return C with ri.m() which
    // adds the type equation C = ? extends C and makes C = RecursiveInterface<C, C> through the
    // type variable T = C & RecursiveInterface<C, C>.
    return ri.m();
  }

  static class MultipleGenerics<A, B, C> {}

  static <D> MultipleGenerics<D, String, List<D>> createMultipleGenerics(List<D> foo) {
    return new MultipleGenerics<>();
  }

  static List<?> listWithWildcard = null;
  static MultipleGenerics<?, String, ?> valMultipleGenerics =
      createMultipleGenerics(listWithWildcard);
}
