package com.google.j2cl.transpiler.regression.compiler;

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
