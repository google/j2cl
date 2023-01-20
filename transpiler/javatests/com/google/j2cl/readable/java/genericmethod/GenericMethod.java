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
package genericmethod;

import java.util.ArrayList;
import java.util.List;
import javaemul.internal.annotations.UncheckedCast;

public class GenericMethod<T> {
  public <T, S> void foo(T f, S s) {} // two type parameters, no bounds.

  public void fun(Object o) {}

  public <T extends Exception> void fun(T t) {} // type parameter with bounds.

  public <T extends Error> void fun(T t) { // type parameter with different bounds.
    new GenericMethod<T>() { // inherit method T
      public void fun2(T t) {} // inherit method T

      public <T> void fun2(T t) {} // redefine T
    };

    class LocalClass<T> extends GenericMethod<T> {
      public void fun2(T t) {}

      public <T extends Number> void fun2(T t) {}
    }
    new LocalClass<T>();
  }

  public <T> GenericMethod<T> bar() {
    return null;
  } // return parameterized type.

  public <T> T[] fun(T[] array) { // generic array type
    return array;
  }

  public <T> T checked() {
    return null;
  }

  @UncheckedCast
  public <T> T unchecked() {
    return null;
  }

  public void test() {
    GenericMethod<Number> g = new GenericMethod<>();
    g.foo(g, g); // call generic method without diamond.
    g.<Error, Exception>foo(new Error(), new Exception()); // call generic method with diamond.

    g.fun(new Object());
    g.fun(new Exception());
    g.fun(new Error());
    g.fun(new String[] {"asdf"});

    String s = checked();
    s = unchecked();

    this.<Content>checked().getProp();
    this.<Content>unchecked().getProp();
  }

  static class SuperContainer<C extends Container<?>> {
    C get() {
      return null;
    }
  }

  static class Container<CT extends Content> {
    CT get() {
      return null;
    }
  }

  static class Content {
    String getProp() {
      return null;
    }
  }

  public static void acceptsContent(Content content) {}

  public static void acceptsString(String string) {}

  public static void testErasureCast_wildcard() {
    List<Container<?>> list = new ArrayList<>();
    Content content = list.get(0).get();
    acceptsString(content.getProp());
    acceptsContent(content);

    List<SuperContainer<? extends Container<? extends Content>>> nestedWildcardList =
        new ArrayList<>();
    Content nestedContent = nestedWildcardList.get(0).get().get();
    acceptsString(nestedContent.getProp());
    acceptsContent(nestedContent);

    List<SuperContainer<Container<? extends Content>>> deepWildcardList = new ArrayList<>();
    Content deepContent = deepWildcardList.get(0).get().get();
    acceptsString(deepContent.getProp());
    acceptsContent(deepContent);
  }

  public static <CT extends Container<C>, C extends Content> void testErasureCast_typeVariable() {
    List<Container<C>> list = new ArrayList<>();
    Content content = list.get(0).get();
    acceptsString(content.getProp());
    acceptsContent(content);

    List<SuperContainer<CT>> nestedTypeVariableList = new ArrayList<>();
    Content nestedContent = nestedTypeVariableList.get(0).get().get();
    acceptsString(nestedContent.getProp());
    acceptsContent(nestedContent);

    List<SuperContainer<Container<C>>> deepTypeVariableList = new ArrayList<>();
    Content deepContent = deepTypeVariableList.get(0).get().get();
    acceptsString(deepContent.getProp());
    acceptsContent(deepContent);
  }

  interface Consumer<V> {
    void accept(V v);
  }

  interface Supplier<V> {
    V get();
  }

  static <V> void testLowerWildcardBound(Consumer<? super V> consumer, V v) {
    consumer.accept(v);
  }

  static <V> V testUpperWildcardBound(Supplier<? extends V> supplier) {
    return supplier.get();
  }

  static <T extends Number> void methodWithBoundTypeVariable() {}

  static void testMethodCallsWithCaptureTypeArguments() {
    methodWithBoundTypeVariable();
  }
}
