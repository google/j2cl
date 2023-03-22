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
package nativejstypes;

import javaemul.internal.annotations.Wasm;

public class Main {
  public static int testNativeJsTypeWithNamespace() {
    Foo foo = new Foo();
    foo.x = 50;
    foo.y = 5;
    return foo.sum();
  }

  public static int testNativeJsTypeWithoutNamespace() {
    Bar bar = new Bar(6, 7);
    bar.x = 50;
    bar.y = 5;
    Bar.f = 10;
    return bar.product();
  }

  public static void testInnerNativeJsType() {
    Bar.Inner unusedBarInner = new Bar.Inner(1);
    BarInnerWithDotInName unusedBarInnerWithDotInName = new BarInnerWithDotInName(2);
  }

  public static void testGlobalNativeJsType() {
    Headers header = new Headers();
    header.append("Content-Type", "text/xml");
  }

  @SuppressWarnings("unused")
  public static void testNativeTypeClassLiteral() {
    Object o1 = Bar.class;
    o1 = Bar[][].class;
  }

  @Wasm("nop") // Not supported in WASM.
  public static void testNativeTypeObjectMethods() {
    Bar bar = new Bar(6, 7);
    bar.toString();
    bar.hashCode();
    bar.equals(new Object());
  }
}
