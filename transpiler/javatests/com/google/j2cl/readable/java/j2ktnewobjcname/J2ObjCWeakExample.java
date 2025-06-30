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
package j2ktnewobjcname;

import com.google.j2objc.annotations.Weak;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class J2ObjCWeakExample {
  public static final class Foo {}

  @Weak @Nullable private final Foo finalNullableFoo;
  @Weak private final Foo finalNonNullFoo;

  @Weak @Nullable private Foo nullableFooWithoutInitializer;
  @Weak @Nullable private Foo nullableFooWithInitializer = new Foo();

  @Weak private Foo nonNullFooWithoutInitializer;
  @Weak private Foo nonNullFooWithInitializer = new Foo();

  public J2ObjCWeakExample() {
    this.finalNullableFoo = null;
    this.finalNonNullFoo = new Foo();
    this.nonNullFooWithoutInitializer = new Foo();
  }

  public J2ObjCWeakExample(@Nullable Foo nullableFoo, Foo nonNullFoo) {
    this.finalNullableFoo = nullableFoo;
    this.finalNonNullFoo = nullableFoo;
    this.nullableFooWithoutInitializer = nonNullFoo;
    this.nonNullFooWithoutInitializer = nonNullFoo;
  }

  public void setFoo(@Nullable Foo nullableFoo, Foo nonNullFoo) {
    nullableFooWithoutInitializer = nullableFoo;
    nullableFooWithInitializer = nullableFoo;
    nonNullFooWithoutInitializer = nonNullFoo;
    nonNullFooWithInitializer = nonNullFooWithoutInitializer;
  }

  public @Nullable Foo getNullableFoo() {
    return nullableFooWithoutInitializer;
  }

  public Foo getNonNullFoo() {
    return nonNullFooWithoutInitializer;
  }
}
