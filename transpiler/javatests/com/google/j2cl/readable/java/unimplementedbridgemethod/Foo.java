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
package unimplementedbridgemethod;

interface I<T> {
  int foo(T t);
  static int staticFoo() {
    return 0;
  }
}

interface J extends I<String> {}

/**
 * No bridge method is needed.
 * 
 * <p>A synthesized abstract method m_foo__java_lang_Object() is generated to implement J.foo()
 * to avoid JSCompiler warning.
 */
abstract class Bar implements J {}

/**
 * No bridge method is needed.
 * 
 * <p>A synthesized abstract method m_foo__java_lang_Object() is generated to implement
 * I.foo(T) to avoid JSCompiler warning.
 */
public abstract class Foo implements I<String> {
}
