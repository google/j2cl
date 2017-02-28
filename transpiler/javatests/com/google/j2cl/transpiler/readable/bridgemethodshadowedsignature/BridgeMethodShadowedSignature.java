package com.google.j2cl.transpiler.readable.bridgemethodshadowedsignature;

import java.util.function.Consumer;

interface I<I1> {
  String get(Consumer<? super I1> consumer);
}

abstract class B<B1, B2> implements I<B1> {
  public String get(B2 consumer) {
    return "B get B2";
  }
}

class C<C1> extends B<C1, Consumer<? super C1>> implements I<C1> {
  // Needs a bridge from the I.get(Consumer) signature to B.get(Object) implementation.
}

public class BridgeMethodShadowedSignature {}
