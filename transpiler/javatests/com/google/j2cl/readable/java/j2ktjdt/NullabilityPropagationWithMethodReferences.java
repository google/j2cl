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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class NullabilityPropagationWithMethodReferences {
  public static final class Foo {
    public static Foo create() {
      return new Foo();
    }

    public Foo copy() {
      return new Foo();
    }

    public interface Factory {
      Foo create();
    }

    public interface ArrayFactory {
      Foo[] create(int size);
    }
  }

  public static <T extends @Nullable Object> T apply(T t) {
    throw new RuntimeException();
  }

  public static Foo.Factory testConstructor() {
    return apply(Foo::new);
  }

  public static Foo.Factory testStaticMethod() {
    return apply(Foo::create);
  }

  public static Foo.Factory testInstanceMethod(Foo foo) {
    return apply(foo::copy);
  }

  public static Foo.ArrayFactory testNewArray() {
    return apply(Foo[]::new);
  }
}
