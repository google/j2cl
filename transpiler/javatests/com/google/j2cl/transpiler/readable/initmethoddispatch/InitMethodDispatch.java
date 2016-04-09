package com.google.j2cl.transpiler.readable.initmethoddispatch;

class InitMethodDispatch<T> {
  int value;

  InitMethodDispatch() {
    this(1);
    // This will translate to a $ctor method that *DOES NOT* dispatch to $init() since doing so
    // would be redundant (in light of the fact that it delegates to this()).
  }

  InitMethodDispatch(int i) {
    value = i;
    // This will translate to a $ctor method that *does* dispatch to $init().
  }
}
