package com.google.j2cl.transpiler.integration.longimplicitcasts;

/** Test implicit cast insertion in long operations. */
@SuppressWarnings({
  "FloatingPointLiteralPrecision",
  "IdentityBinaryExpression",
  "NarrowingCompoundAssignment"
})
public class Main {
  // Initialized with not-a-long.
  public long fieldLong = 100;

  public void compareLong(long leftValue, long rightValue) {
    assert leftValue == rightValue;
  }

  public Main() {}

  public Main(long newFieldLong) {
    this.fieldLong = newFieldLong;
  }

  private static class SubMain extends Main {
    private SubMain(int newFieldInt) {
      super(newFieldInt); // To show that coercions occur even in super() params.
    }
  }

  public static void main(String... args) {
    Main main = new Main();
    main.main();
  }

  public void main() {
    assert fieldLong == 100L;

    byte fbyte = 11;
    char fchar = 12;
    short fshort = 13;
    int fint = 14;
    long flong = 15;
    float ffloat = 16;
    double fdouble = 17;

    long tlong = 0;
    assert tlong == 0L;

    // Direct assignments from smaller types.
    {
      tlong = fbyte;
      assert tlong == (long) fbyte;
      tlong = fchar;
      assert tlong == (long) fchar;
      tlong = fshort;
      assert tlong == (long) fshort;
      tlong = fint;
      assert tlong == (long) fint;
      tlong = flong;
      assert tlong == (long) flong;
      // tlong = ffloat; // illegal
      // tlong = fdouble; // illegal
    }

    // Implicit casts to long when performing any assignment binary operation on a long and any
    // non-long type.
    {
      // If implicit casts are not inserted then long library operations will receive invalid
      // parameters and result in a runtime error.
      tlong = fint;
      assert tlong == 14L;
      tlong += fint;
      assert tlong == 28L;
      tlong -= fint;
      assert tlong == 14L;
      tlong *= fint;
      assert tlong == 196L;
      tlong /= fint;
      assert tlong == 14L;
      tlong &= fint;
      assert tlong == 14L;
      tlong |= fint;
      assert tlong == 14L;
      tlong ^= fint;
      assert tlong == 0L;
      tlong %= fint;
      assert tlong == 0L;
      tlong <<= fint; // Does not cast to long on right hand side.
      assert tlong == 0L;
      tlong >>= fint; // Does not cast to long on right hand side.
      assert tlong == 0L;
      tlong >>>= fint; // Does not cast to long on right hand side.
      assert tlong == 0L;
    }

    // Implicit casts to long when performing the PLUS_EQUALS binary operation on a long and any
    // non-long type.
    {
      tlong += fbyte;
      assert tlong == 11L;
      tlong += fchar;
      assert tlong == 23L;
      tlong += fshort;
      assert tlong == 36L;
      tlong += fint;
      assert tlong == 50L;
      tlong += flong;
      assert tlong == 65L;
      tlong += ffloat;
      assert tlong == 81L;
      tlong += fdouble;
      assert tlong == 98L;
    }

    // Implicit casts to long when performing any non assignment binary operation on a long and any
    // non-long type.
    {
      tlong = flong * fint;
      assert tlong == 210L;
      tlong = flong / fint;
      assert tlong == 1L;
      tlong = flong % fint;
      assert tlong == 1L;
      tlong = flong + fint;
      assert tlong == 29L;
      tlong = flong - fint;
      assert tlong == 1L;
      tlong = flong << fint; // Does not cast to long on right hand side.
      assert tlong == 245760L;
      tlong = flong >> fint; // Does not cast to long on right hand side.
      assert tlong == 0L;
      tlong = flong >>> fint; // Does not cast to long on right hand side.
      assert tlong == 0L;
      // tlong = flong < fint; // illegal
      // tlong = flong > fint; // illegal
      // tlong = flong <= fint; // illegal
      // tlong = flong >= fint; // illegal
      // tlong = flong == fint; // illegal
      // tlong = flong != fint; // illegal
      tlong = flong ^ fint;
      assert tlong == 1L;
      tlong = flong & fint;
      assert tlong == 14L;
      tlong = flong | fint;
      assert tlong == 15L;
      // tlong = flong && fint; // illegal
      // tlong = flong || fint; // illegal
    }

    // Implicit casts to long when performing the PLUS binary operation on a long and any non-long
    // type.
    {
      tlong = flong + fbyte;
      assert tlong == 26L;
      tlong = flong + fchar;
      assert tlong == 27L;
      tlong = flong + fshort;
      assert tlong == 28L;
      tlong = flong + fint;
      assert tlong == 29L;
      tlong = flong + flong;
      assert tlong == 30L;
      // tlong = flong + ffloat; // illegal
      // tlong = flong + fdouble; // illegal
    }

    // Implicit casts to long when passing into a long parameter slot.
    {
      compareLong(fbyte, (long) fbyte);
      assert new Main(fbyte).fieldLong == (long) fbyte;
      compareLong(fchar, (long) fchar);
      assert new Main(fchar).fieldLong == (long) fchar;
      compareLong(fshort, (long) fshort);
      assert new Main(fshort).fieldLong == (long) fshort;
      compareLong(fint, (long) fint);
      assert new Main(fint).fieldLong == (long) fint;
      compareLong(flong, (long) flong);
      assert new Main(flong).fieldLong == (long) flong;
      // compareLong(ffloat, (long)ffloat); // illegal
      // assert new Main(ffloat).fieldLong == (long) ffloat; // illegal
      // compareLong(fdouble, (long)fdouble); // illegal
      // assert new Main(fdouble).fieldLong == (long) fdouble; // illegal

      // To show that coercions occur even in super() params.
      assert new SubMain(fint).fieldLong == (long) fint;
    }

    {
      // If implicit casts away from long are not inserted then long library operations will receive
      // invalid parameters and result in a runtime error.
      tlong = flong << flong;
      assert tlong == 491520L;
      tlong = flong >> flong;
      assert tlong == 0L;
      tlong = flong >>> flong;
      assert tlong == 0L;
      tlong <<= flong;
      assert tlong == 0L;
      tlong >>= flong;
      assert tlong == 0L;
      tlong >>>= flong;
      assert tlong == 0L;
    }

    assert Long.MAX_VALUE != 9223372036854776833d;

    {
      // Long arrays
      int i = 0;
      long[] longArray = new long[3];
      longArray[i++] += 3;
      assert i == 1;
    }
  }
}
