package com.google.j2cl.transpiler.readable.booleanoperations;

public class BooleanOperations {
  @SuppressWarnings("unused")
  private static void acceptBoolean(boolean b) {
    // does nothing
  }

  @SuppressWarnings({"unused", "cast"})
  public void test() {
    boolean ls = true;
    boolean rs = true;
    boolean r = true;

    // Assignment.
    r = ls == rs;
    r = ls != rs;
    r = ls ^ rs;
    r = ls & rs;
    r = ls | rs;
    r = ls && rs;
    r = ls || rs;
    r = ls = rs;

    // Compound assignment.
    r ^= rs;
    r &= rs;
    r |= rs;

    // Method invocation.
    acceptBoolean(ls == rs);
    acceptBoolean(ls != rs);
    acceptBoolean(ls ^ rs);
    acceptBoolean(ls & rs);
    acceptBoolean(ls | rs);
    acceptBoolean(ls && rs);
    acceptBoolean(ls || rs);
    acceptBoolean(ls = rs);

    // Cast.
    Boolean br;
    br = (Boolean) (ls == rs);
    br = (Boolean) (ls != rs);
    br = (Boolean) (ls ^ rs);
    br = (Boolean) (ls & rs);
    br = (Boolean) (ls | rs);
    br = (Boolean) (ls && rs);
    br = (Boolean) (ls || rs);
    br = (Boolean) (ls = rs);

    // Conditional
    if (ls == rs) {
      r = true;
    }
    if (ls != rs) {
      r = true;
    }
    if (ls ^ rs) {
      r = true;
    }
    if (ls & rs) {
      r = true;
    }
    if (ls | rs) {
      r = true;
    }
    if (ls && rs) {
      r = true;
    }
    if (ls || rs) {
      r = true;
    }

    // Compound assignment with enclosing instance.
    class Outer {
      boolean b;

      class Inner {
        {
          b |= true;
        }
      }
    }
    final Outer finalOuter = new Outer();
    finalOuter.b |= true;

    Outer outer = new Outer();
    outer.b |= (outer = null) == null;

  }
}
