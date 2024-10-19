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
import javax.annotation.Nullable;

/** A benchmark that measures class dispatch performance. */
public final class DynamicClassDispatchBenchmark extends AbstractBenchmark {

  private Object[] array;

  @Nullable
  @Override
  public Object run() {
    int count = 0;
    for (int i = 0; i < array.length; i++) {
      // We use the hashCode to avoid introducing an extra cast that is normally needed by WASM
      // arrays. Note that hashCode dispatch is slower in JS due to the required trampoline.
      count += array[i].hashCode();
    }

    if (count != 43500) {
      throw new AssertionError();
    }

    return null;
  }

  @Override
  public void setupOneTime() {
    array = new Object[1000];
    for (int i = 0; i < array.length; i++) {
      switch (i % 4) {
        case 0:
          array[i] = new Foo();
          break;
        case 1:
          array[i] = new Bar();
          break;
        case 2:
          array[i] = new Baz();
          break;
        case 3:
          array[i] = new Zoo();
          break;
        default: // fall out
      }
    }
  }

  static class Foo {
    int number = Math.random() > 0 ? 42 : 0;

    @Override
    public int hashCode() {
      return number;
    }
  }

  static class Bar {
    int number = Math.random() > 0 ? 43 : 0;

    @Override
    public int hashCode() {
      return number;
    }
  }

  static class Baz {
    int number = Math.random() > 0 ? 44 : 0;

    @Override
    public int hashCode() {
      return number;
    }
  }

  static class Zoo {
    int number = Math.random() > 0 ? 45 : 0;

    @Override
    public int hashCode() {
      return number;
    }
  }
}
