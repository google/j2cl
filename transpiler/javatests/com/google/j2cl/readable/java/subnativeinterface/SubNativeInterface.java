/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package subnativeinterface;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "test.foo")
interface MyNativeInterface {
  int getValue();

  @JsMethod(name = "renamed")
  int mightBeRenamed();

  @JsProperty
  int getProp();

  @JsProperty
  void setProp(int value);
}

@JsType(isNative = true, namespace = "test.foo")
interface MyNativeGenericInterface<T> {
  T echo(T t);
}

public class SubNativeInterface implements MyNativeInterface {
  private int prop;

  @Override
  public int getValue() {
    return 1;
  }

  @Override
  public int mightBeRenamed() {
    return 2;
  }

  @Override
  public int getProp() {
    return prop;
  }

  @Override
  public void setProp(int value) {
    prop = value;
  }

  public static String test() {
    MyNativeInterface nativeInterface = new SubNativeInterface();
    nativeInterface.setProp(nativeInterface.getValue() + nativeInterface.mightBeRenamed());
    MyNativeGenericInterface<String> lambda = s -> s + nativeInterface.getProp();
    return lambda.echo("");
  }
}

class SubSubNativeInterface extends SubNativeInterface {
  @Override
  public int getValue() {
    return 3;
  }
}

abstract class AbstractSubNativeInterface implements MyNativeInterface {
  @Override
  public int mightBeRenamed() {
    return 4;
  }
}

class StringEcho implements MyNativeGenericInterface<String> {
  @Override
  public String echo(String s) {
    return s;
  }
}

abstract class AbstractStringEcho implements MyNativeGenericInterface<String> {}

class ConcreteStringEcho extends AbstractStringEcho {
  @Override
  public String echo(String s) {
    return s;
  }
}

class GenericEcho<T> implements MyNativeGenericInterface<T> {
  @Override
  public T echo(T t) {
    return t;
  }
}

class NonJsBase {
  public int getValue() {
    return 5;
  }

  public final int mightBeRenamed() {
    return 6;
  }

  public int getProp() {
    return 7;
  }

  public void setProp(int value) {}
}

class AccidentalSubNativeInterface extends NonJsBase implements MyNativeInterface {}

class NonJsStringEcho {
  public String echo(String s) {
    return s;
  }
}

class AccidentalStringEcho extends NonJsStringEcho implements MyNativeGenericInterface<String> {}

interface JavaSubNativeInterface extends MyNativeInterface {
  @Override
  default int getValue() {
    return 8;
  }
}

class JavaSubNativeInterfaceImpl implements JavaSubNativeInterface {
  @Override
  public int mightBeRenamed() {
    return 9;
  }

  @Override
  public int getProp() {
    return 10;
  }

  @Override
  public void setProp(int value) {}
}
