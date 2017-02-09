package com.google.j2cl.transpiler.integration.gwtincompatiblesrcjar;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.io.Files;
import java.text.SimpleDateFormat;

public class Main {
  public static void main(String... args) {
    compatible();
  }

  public static void compatible() {
    SomeEnum a = SomeEnum.COMPATIBLE;
  }

  @GwtIncompatible
  public static void incompatible() {
    SomeEnum a = SomeEnum.INCOMPATIBLE;
  }

  enum SomeEnum {
    COMPATIBLE {
      @Override
      void method() {}
    },

    @GwtIncompatible
    INCOMPATIBLE {
      @Override
      void method() {
        new SimpleDateFormat();
      }
    };

    abstract void method();
  }

  @GwtIncompatible
  public static void incompatibleMissingInDep() {
    Files.toByteArray(null);
  }
}
