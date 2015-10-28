package com.google.j2cl.transpiler.integration.arrayobjectcalls;

public class Main {
  @SuppressWarnings({"ArrayEquals", "ArrayHashCode", "ArrayToString"})
  public static void main(String... args) {
    Main[] mains = new Main[1];
    Main[] other = new Main[1];

    assert mains.equals(mains);
    assert !mains.equals(other);
    assert mains.hashCode() == mains.hashCode();
    assert mains.hashCode() != other.hashCode();

    // TODO: uncomment once jre is integrated. Currently it fails because the Arrays.toString()
    // calls Integer.m_toHexString__int(), which is not implemented correctly.
    // assert (mains.toString() != other.toString());

    assert (mains.getClass() == other.getClass());
  }
}
