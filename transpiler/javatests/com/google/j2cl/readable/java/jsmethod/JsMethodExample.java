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
package jsmethod;

import java.util.ArrayList;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

public class JsMethodExample {

  // Regression readable for b/70040143.
  // The following declaration indirectly triggers the code in MethodDescriptor that fails a
  // precondition check when building the constructor of a raw type.
  // To reproduce the failing state the method needs:
  //    1. be a @JsMethod
  //    2. return a type variable that is bounded by a generic class that has a constructor.
  // This would better be handled in a unit test.
  @JsMethod
  public native <T extends ArrayList<String>> T testMethod();

  abstract static class Base<T> {
    @JsMethod
    public void m(T t) {}
  }

  interface I {
    @JsMethod(name = "mString")
    void m(String s);
  }

  // Regression test for b/124227197
  static class Sub extends Base<String> implements I {
    // This should not be a JsMethod.
    public void m(String s) {}
  }

  @JsType
  static class SubJsType extends Base<String> {
    // This should not be a JsMethod.
    public void m(String s) {}
  }

  @JsType
  static class SubJsTypeWithRenamedJsMethod extends Base<String> {
    // This should not be a JsMethod.
    @JsMethod(name = "renamedM")
    public void m(String s) {}
  }

  interface InterfaceWithMethod {
    void m();

    void n();
  }

  interface InterfaceExposingJsMethods extends InterfaceWithMethod {
    @JsMethod
    default void m() {}

    @JsMethod
    void n();
  }

  static class SuperClassWithFinalMethod {
    public final void n() {}
  }

  // This class inherits implementations from supertypes and needs to exposes both the Java
  // contracts and js contracts.
  static class ExposesOverrideableJsMethod extends SuperClassWithFinalMethod
      implements InterfaceExposingJsMethods {}
}
