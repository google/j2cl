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
package j2objcnamenotpassing;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class AccidentalOverride {
  public interface Left<T extends Left<T>> {
    default void name(T t) {}
  }

  public interface Right<T extends Right<T>> {
    default void name(T t) {}
  }

  public interface Bottom<T extends Bottom<T>> extends Left<T>, Right<T> {
    // TODO(b/425618126): Causes compilation error in Kotlin:
    // error: member 'fun name(t: T): Unit' inherits inconsistent '@ObjCName' from:
    // interface Left<T : Left<T>>
    // interface Right<T : Right<T>>
    @Override
    default void name(T t) {}
  }

  public static class A<T extends Left<T>, V extends Right<V>> implements Left<T>, Right<V> {}

  // TODO(b/425618126):  Causes compilation error in Kotlin:
  // error: member 'fun name(t: B): Unit' inherits inconsistent '@ObjCName' from:
  // interface Left<T : Left<T>> : Any
  // interface Right<T : Right<T>> : Any
  // interface Bottom<T : Bottom<T>> : Left<T>, Right<T>
  public static class B extends A<B, B> implements Bottom<B> {}

  public static class C implements Bottom<C> {
    @Override
    public void name(C c) {}
  }
}
