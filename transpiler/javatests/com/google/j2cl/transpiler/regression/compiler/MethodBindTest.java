package com.google.j2cl.transpiler.regression.compiler;

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
