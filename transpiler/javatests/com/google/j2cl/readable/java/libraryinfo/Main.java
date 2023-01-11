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
package libraryinfo;

import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {

  public static String STATIC_FIELD = "STATIC_FIELD";

  public static final String CONSTANT = "STATIC_FIELD";

  @JsProperty public static final String JS_CONSTANT = "STATIC_FIELD";

  @JsType
  interface JsTypeInterface {
    void foo();

    void bar();
  }

  interface FunctionnalInterface {
    void foo();
  }

  interface JsAccessibleFunctionnalInterface {
    @JsMethod
    void accessibleFunctionalInterfaceMethod();
  }

  @JsFunction
  interface Function {
    void apply(String s);
  }

  private static final class FunctionImpl implements Function {
    public void apply(String s) {}
  }

  @JsEnum
  enum MyJsEnum {
    A,
    B,
    C
  }

  enum MyEnum {
    FOO,
    BAR;
  }

  @JsType
  public static class MyJsType {
    Object someField;

    public MyJsType() {
      someField = new Main();
    }
  }

  @JsMethod
  public static void entryPoint() {
    new Main().execute();

    Function jsFunction = new FunctionImpl();
    jsFunction = s -> log(42);
    jsFunction.apply("foo");

    new MyJsType();

    new Object() {
      @JsMethod
      void foo() {}
    };
  }

  private void execute() {
    log(42);
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @JsMethod(namespace = JsPackage.GLOBAL, name = "console.log")
  public static native void log(int o);

  public class Foo {
    void instanceMethod() {}
  }

  @JsMethod
  public static void main() {
    Foo foo = null;
    foo.instanceMethod();

    Class<?> clazz = Foo.class;
  }
}
