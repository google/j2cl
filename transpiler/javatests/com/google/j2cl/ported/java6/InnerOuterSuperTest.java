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

/** Tests qualified outer access. */
@RunWith(JUnit4.class)
public class InnerOuterSuperTest {
  static class Outer {

    class OuterIsNotSuper {
      public int getValue() {
        return value;
      }
    }

    class OuterIsSuper extends Outer {

      public OuterIsSuper(int i) {
        super(i);
      }

      @Override
      public int checkDispatch() {
        return 2;
      }

      public int checkDispatchFromSub1() {
        return super.checkDispatch();
      }

      public int checkDispatchFromSub2() {
        return new Outer(1) {
          public int go() {
            return OuterIsSuper.super.checkDispatch();
          }
        }.go();
      }

      public OuterIsNotSuper unqualifiedAlloc() {
        return new OuterIsNotSuper();
      }
    }

    static class TestQualifiedSuperCall extends OuterIsNotSuper {
      public TestQualifiedSuperCall() {
        new Outer(1).new OuterIsSuper(2).super();
      }
    }

    class TestUnqualifiedSuperCall extends OuterIsNotSuper {
      public TestUnqualifiedSuperCall() {
        super();
      }
    }

    protected final int value;

    public Outer(int i) {
      value = i;
    }

    public int checkDispatch() {
      return 1;
    }
  }

  private final Outer outer = new Outer(1);

  private final Outer.OuterIsSuper outerIsSuper = outer.new OuterIsSuper(2);

  @Test
  public void testOuterIsNotSuper() {
    Outer.OuterIsNotSuper x = outerIsSuper.new OuterIsNotSuper();
    assertThat(x.getValue()).isEqualTo(2);
  }

  @Test
  public void testOuterIsNotSuperAnon() {
    Outer.OuterIsNotSuper x = outerIsSuper.new OuterIsNotSuper() {};
    assertThat(x.getValue()).isEqualTo(2);
  }

  @Test
  public void testQualifiedSuperCall() {
    Outer.TestQualifiedSuperCall x = new Outer.TestQualifiedSuperCall();
    assertThat(x.getValue()).isEqualTo(2);
  }

  @Test
  public void testQualifiedSuperCallAnon() {
    Outer.TestQualifiedSuperCall x = new Outer.TestQualifiedSuperCall() {};
    assertThat(x.getValue()).isEqualTo(2);
  }

  @Test
  public void testSuperDispatch() {
    assertThat(outerIsSuper.checkDispatchFromSub1()).isEqualTo(1);
    assertThat(outerIsSuper.checkDispatchFromSub2()).isEqualTo(1);
  }

  @Test
  public void testUnqualifiedAlloc() {
    Outer.OuterIsNotSuper x = outerIsSuper.unqualifiedAlloc();
    assertThat(x.getValue()).isEqualTo(2);
  }

  @Test
  public void testUnqualifiedSuperCall() {
    Outer.TestUnqualifiedSuperCall x = outerIsSuper.new TestUnqualifiedSuperCall();
    assertThat(x.getValue()).isEqualTo(2);
  }

  @Test
  public void testUnqualifiedSuperCallAnon() {
    Outer.TestUnqualifiedSuperCall x = outerIsSuper.new TestUnqualifiedSuperCall() {};
    assertThat(x.getValue()).isEqualTo(2);
  }
}
