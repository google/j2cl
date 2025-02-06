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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NullabilityInferenceWithLocalVariables {
  public static String[] testArray() {
    String local = "";
    return new String[] {local, ""};
  }

  public static Foo<String> testConstructor() {
    String local = "";
    return new Foo(local, "");
  }

  public static Foo<String> testMethod() {
    String local = "";
    return Foo.create(local, "");
  }

  public static class Foo<T extends @Nullable Object> {
    Foo(T t1, T t2) {}

    public static <T extends @Nullable Object> Foo<T> create(T t1, T t2) {
      throw new RuntimeException();
    }
  }
}
