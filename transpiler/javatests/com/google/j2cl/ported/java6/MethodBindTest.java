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
package com.google.j2cl.ported.java6;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests the method binding */
@RunWith(JUnit4.class)
@SuppressWarnings("ClassCanBeStatic")
public class MethodBindTest {

  private abstract static class Abstract implements Go {
    private final class Nested implements Go {
      @Override
      public void go() {
        result = "wrong";
      }

      void nestedTrigger() {
        Abstract.this.go();
      }
    }

    private final class Nested2 extends Abstract {
      @Override
      public void go() {
        result = "wrong";
      }

      void nestedTrigger() {
        Abstract.this.go();
      }
    }

    void trigger() {
      new Nested().nestedTrigger();
    }

    void trigger2() {
      new Nested2().nestedTrigger();
    }
  }

  private final class Concrete extends Abstract {
    @Override
    public void go() {
      result = "right";
    }
  }

  private interface Go {
    void go();
  }

  private static String result;

  @Test
  public void testMethodBinding() {
    result = null;
    new Concrete().trigger();
    assertThat(result).isEqualTo("right");
    result = null;
    new Concrete().trigger2();
    assertThat(result).isEqualTo("right");
  }
}
