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

/** Tests that initialization order for field follow the "quirky" Java semantics. */
@RunWith(JUnit4.class)
public class FieldInitializationOrderTest {
  abstract static class Tester {
    abstract void performTest();
  }

  static class ConstructorCastClasses extends Tester {
    static class Base {
      Base() {
        assertThat(((Child) this).i).isEqualTo(0);
      }
    }

    static class Child extends Base {
      int i = 1;
    }

    @Override
    void performTest() {
      new Child();
    }
  }

  static class InitializerCastClasses extends Tester {
    static class Base {
      {
        assertThat(((Child) this).i).isEqualTo(0);
      }
    }

    static class Child extends Base {
      int i = 1;
    }

    @Override
    void performTest() {
      new Child();
    }
  }

  static class InitializerPolymorphicDispatchClasses extends Tester {
    static class Base {
      {
        m();
      }

      void m() {}
    }

    static class Child extends Base {
      int i = 1;

      @Override
      void m() {
        assertThat(i).isEqualTo(0);
      }
    }

    @Override
    void performTest() {
      // Construct both parent and child to ensure that polymorphic dispatch for m() does not go
      // away
      new Base();
      new Child();
    }
  }

  static class ConstructorPolymorphicDispatchClasses extends Tester {
    static class Base {
      Base() {
        m();
      }

      void m() {}
    }

    static class Child extends Base {
      int i = 1;

      @Override
      void m() {
        assertThat(i).isEqualTo(0);
      }
    }

    @Override
    void performTest() {
      // Construct both parent and child to ensure that polymorphic dispatch for m() does not go
      // away
      new Base();
      new Child();
    }
  }

  static class IncorrectlyOptimizedCondition extends Tester {
    static class Base {
      Base() {
        m();
      }

      void m() {}
    }

    static class Child extends Base {
      String s = "blah";

      @Override
      void m() {
        assertThat(s == null).isTrue();
      }
    }

    @Override
    void performTest() {
      // Construct both parent and child to ensure that polymorphic dispatch for m() does not go
      // away
      new Base();
      new Child();
    }
  }

  @Test
  public void testInitializerCast() {
    new InitializerCastClasses().performTest();
  }

  @Test
  public void testConstructorCast() {
    new ConstructorCastClasses().performTest();
  }

  @Test
  public void testInitializerPolymorphicDispatch() {
    new InitializerPolymorphicDispatchClasses().performTest();
  }

  @Test
  public void testConstructorPolymorphicDispatch() {
    new ConstructorPolymorphicDispatchClasses().performTest();
  }

  @Test
  public void testIncorrectlyOptimizedCondition() {
    new IncorrectlyOptimizedCondition().performTest();
  }
}
