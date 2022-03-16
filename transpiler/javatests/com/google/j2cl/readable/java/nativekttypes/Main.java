/*
 * Copyright 2022 Google Inc.
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
package nativekttypes;

import jsinterop.annotations.JsConstructor;

public class Main {
  public NativeTopLevel<String> topLevelField;
  public NativeTopLevel.Nested<String> nestedField;
  public NativeTopLevel<String>.Inner<String> innerField;

  public void methodArguments(
      NativeTopLevel<String> foo,
      NativeTopLevel.Nested<String> nested,
      NativeTopLevel<String>.Inner<String> inner) {}

  public void memberAccess() {
    NativeTopLevel<String> foo = new NativeTopLevel<>("foo");
    String fooInstanceMethod = foo.instanceMethod("foo");
    String fooStaticMethod = foo.staticMethod("foo");
    String fooInstanceField = foo.instanceField;
    foo.instanceField = "foo";
    Object fooStaticField = foo.staticField;
    foo.staticField = "foo";

    NativeTopLevel.Nested<String> nested = new NativeTopLevel.Nested<>("foo");
    String nestedInstanceMethod = nested.instanceMethod("foo");
    String nestedStaticMethod = nested.staticMethod("foo");
    String nestedInstanceField = nested.instanceField;
    nested.instanceField = "foo";
    Object nestedStaticField = nested.staticField;
    nested.staticField = "foo";

    NativeTopLevel<String>.Inner<String> inner = foo.new Inner<String>("foo");
  }

  public void typeLiterals() {
    Class<?> c1 = NativeTopLevel.class;
    Class<?> c2 = NativeTopLevel.Nested.class;
    Class<?> c3 = NativeTopLevel.Inner.class;
  }

  public void casts() {
    NativeTopLevel<String> o1 = (NativeTopLevel<String>) null;
    NativeTopLevel.Nested<String> o2 = (NativeTopLevel.Nested<String>) null;
    NativeTopLevel<String>.Inner<String> o3 = (NativeTopLevel<String>.Inner<String>) null;
  }
}

class Bar<V> extends NativeTopLevel<V> implements NativeInterface<V> {
  @JsConstructor
  Bar(V v) {
    super(v);
  }
}
