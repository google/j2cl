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

import com.google.j2objc.annotations.WeakOuter;

public final class J2ObjCWeakOuterExample {
  interface Supplier<T> {
    T get();
  }

  private final String string = "foo";

  @WeakOuter private final Supplier<String> lambdaSupplierField = () -> string;

  @WeakOuter
  private final Supplier<String> anonymousClassSupplierField =
      new Supplier<String>() {
        @Override
        public String get() {
          return string;
        }
      };

  private void testLambdaSupplierVariable() {
    @WeakOuter Supplier<String> unused = () -> string;
  }

  private void testAnonymousClassSupplierVariable() {
    @WeakOuter
    Supplier<String> unused =
        new Supplier<String>() {
          @Override
          public String get() {
            return string;
          }
        };
  }

  @WeakOuter
  private class InnerSupplier implements Supplier<String> {
    @Override
    public String get() {
      return string;
    }
  }
}
