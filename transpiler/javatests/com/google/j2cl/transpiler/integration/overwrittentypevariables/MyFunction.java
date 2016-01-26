package com.google.j2cl.transpiler.integration.overwrittentypevariables;

public interface MyFunction<F, T> {
  T apply(F input);
}
