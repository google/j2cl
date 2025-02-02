/*
 * Copyright 2024 Google Inc.
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

// TODO(b/381046077): Move to `j2kt` when fixed.
@NullMarked
public class GenericClassLiteral {
  public interface Generic<T> {}

  public interface Foo {}

  public void test(Generic<Foo> genericFoo) {
    // J2KT renders invalid method type parameter and inserts invalid cast:
    // {@code accept<Generic<Any>>(genericFoo as GenericFoo<Any>, Generic::class.javaObjectType)}
    accept(genericFoo, Generic.class);
  }

  public <T> void accept(T foo, Class<T> cls) {}
}
