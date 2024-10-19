/*
 * Copyright 2022 Google Inc.
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
package com.google.j2cl.benchmarks.jre;

import com.google.j2cl.benchmarking.framework.AbstractBenchmark;

/** A benchmark to test out instanceof/cast performance with multiple levels. */
public final class InstanceOfDeepBenchmark extends AbstractBenchmark {

  private Object[] array;
  private Object fake;

  @Override
  public Object run() {
    int count = 0;
    for (int i = 0; i < array.length; i++) {
      Object o = array[i];
      if (i % 2 == 0 ? o instanceof FooParent : o instanceof BarParent) {
        count++;
      }
    }

    if (count != array.length) {
      throw new AssertionError();
    }

    // Ensure runtime doesn't do tricks around no use of Bar or Foo.
    return fake instanceof Foo | fake instanceof Bar | fake instanceof Baz | fake instanceof Zoo;
  }

  @Override
  public void setupOneTime() {
    array = new Object[1000];
    for (int i = 0; i < array.length; i++) {
      array[i] = i % 2 == 0 ? new Foo() : new Bar();
    }
    fake = Math.random() > 0 ? new Baz() : new Zoo();
  }

  static class FooParent {}

  static class Foo extends FooParent {}

  static class Baz extends FooParent {}

  static class BarParent {}

  static class Bar extends BarParent {}

  static class Zoo extends BarParent {}
}
