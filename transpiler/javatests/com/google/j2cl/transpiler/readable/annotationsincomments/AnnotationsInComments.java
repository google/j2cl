package com.google.j2cl.transpiler.readable.annotationsincomments;

import java.util.List;

public class AnnotationsInComments {
  public void foo(List</*@NullableType*/String> a) {}
  public void bar(
      List</*@org.checkerframework.checker.nullness.compatqual.NullableType*/ String> b) {}
}
