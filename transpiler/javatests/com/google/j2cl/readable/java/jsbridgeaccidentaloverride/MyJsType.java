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
package jsbridgeaccidentaloverride;

import jsinterop.annotations.JsType;

interface OtherInterface {
  int fun(int a);
}

@JsType
public class MyJsType implements OtherInterface {
  MyJsType() {}
  /**
   * Accidentally exposes non-JsMethod MyInterface.foo(). Thus there should be a bridge method in
   * SubJsType.
   */
  public int foo(int a) {
    return a;
  }

  /**
   * Does not accidentally exposes non-JsMethod MyInterface.bar() because SubJsType exposes it. A
   * bridge method should still be generated in SubJsType.
   */
  public int bar(int a) {
    return a + 1;
  }

  /**
   * Exposes non-JsMethod OtherInterface.fun(). There should be a bridge method in MyJsType. There
   * should not be a bridge method in SubJsType.
   */
  @Override
  public int fun(int a) {
    return a - 1;
  }
}

interface MyInterface {
  int foo(int a);

  int bar(int a);
}

class SubJsType extends MyJsType implements MyInterface {
  // There should be bridge method for MyInterface.foo() because it is accidentally overridden
  // by MyJsType.foo().

  /** There should be a bridge method for MyInterface.bar() because it is exposed by this bar(). */
  @Override
  public int bar(int a) {
    return a + 2;
  }

  // There should no be a bridge method for OtherInterface.fun() because there is already one in
  // MyJsType.
}
