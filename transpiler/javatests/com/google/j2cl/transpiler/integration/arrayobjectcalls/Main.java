package com.google.j2cl.transpiler.integration.arrayobjectcalls;

@SuppressWarnings({
  "ArrayEquals",
  "ArrayHashCode",
  "ArrayToString",
  "SelfEquals",
  "IdentityBinaryExpression",
  "ReferenceEquality"
})
public class Main {
  public static void main(String... args) {
    Main[] mains = new Main[1];
    Main[] other = new Main[1];

    assert mains.equals(mains);
    assert !mains.equals(other);
    assert mains.hashCode() == mains.hashCode();
    assert mains.hashCode() != other.hashCode();
    assert mains.toString() != other.toString();
    assert mains.getClass() == other.getClass();
  }
}
