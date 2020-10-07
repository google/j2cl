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
package inferreturn;

import java.util.ArrayList;

/** Tests that Closure type inference based on return doesn't result in warnings. */
public class InferReturn<T> {

  public static <T> InferReturn<T> inferGeneric(T foo) {
    return new InferReturn<>();
  }

  public static InferReturn<InferReturn<String>> tightenType(InferReturn<String> foo) {
    if (foo != null) {
      // Without a cast to fix it, JSCompiler will infer the type of this return statement to be
      // ?Foo<!Foo<?string>>, which does not match the return type, ?Foo<?Foo<?string>>.
      return inferGeneric(foo);
    }
    return null;
  }

  public static void main() {
    ArrayList<Object> list = newArrayList("foo");
    // list will be tightened to ArrayList<String> hence OTI would complain below without a cast.
    acceptsArrayListOfObject(list);

    Object[] array = newArray("foo");
    // same here
    acceptsArrayOfObject(array);
  }

  public static <V> ArrayList<V> newArrayList(V foo) {
    return new ArrayList<>();
  }

  public static <V> V[] newArray(V foo) {
    return (V[]) new Object[0];
  }

  public static void acceptsArrayListOfObject(ArrayList<Object> foo) {
    // empty
  }

  public static void acceptsArrayOfObject(Object[] foo) {
    // empty
  }
}
