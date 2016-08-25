package com.google.j2cl.transpiler.readable.timing;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * An example that exists to pull in Guava so that JSCompiler's slow type check time of translated
 * Guava code can be examined.
 */
public class Timing {

  public static boolean run() {
    // Use both Guava collections and base libraries, just so the source and BUILD validators don't
    // complain.
    List<String> fooStrings = Lists.newArrayList("foo");
    List<String> barStrings = Lists.newArrayList("bar");
    return Objects.equal(fooStrings, barStrings);
  }
}
