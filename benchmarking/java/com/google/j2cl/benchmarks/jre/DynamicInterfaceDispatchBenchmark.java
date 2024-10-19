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

/** A benchmark that measures interface dispatch performance. */
public final class DynamicInterfaceDispatchBenchmark extends AbstractBenchmark {

  private Parent[] array;

  @Nullable
  @Override
  public Object run() {
    int count = 0;
    for (int i = 0; i < array.length; i++) {
      // Note that Wasm currently introduces a cast here but since this is an interface and
      // interface dispatch doesn't need the type cast it will be removed by binaryen. So only the
      // dispatch related casts will remain and help us to measure the real cost of the dispatch.
      Parent p = array[i];
      count += p.getNumber();
    }

    if (count != 46000) {
      throw new AssertionError();
    }

    return null;
  }

  @Override
  public void setupOneTime() {
    array = new Parent[1000];
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

  interface Parent {
    int getNumber();
  }

  static class Foo implements Parent {
    int number = Math.random() > 0 ? 42 : 0;

    @Override
    public int getNumber() {
      return number + 1;
    }
  }

  static class Bar implements Parent {
    int number = Math.random() > 0 ? 43 : 0;

    @Override
    public int getNumber() {
      return number + 2;
    }
  }

  static class Baz implements Parent {
    int number = Math.random() > 0 ? 44 : 0;

    @Override
    public int getNumber() {
      return number + 3;
    }
  }

  static class Zoo implements Parent {
    int number = Math.random() > 0 ? 45 : 0;

    @Override
    public int getNumber() {
      return number + 4;
    }
  }
}
