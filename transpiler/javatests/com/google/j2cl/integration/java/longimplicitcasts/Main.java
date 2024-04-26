/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package longimplicitcasts;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/** Test implicit cast insertion in long operations. */
@SuppressWarnings({
  "FloatingPointLiteralPrecision",
  "IdentityBinaryExpression",
  "NarrowingCompoundAssignment"
})
public class Main {
  private static class LongHolder {
    // Initialized with not-a-long.
    public long fieldLong = 100;

    public LongHolder() {}

    public LongHolder(long newFieldLong) {
      this.fieldLong = newFieldLong;
    }
  }

  private static class SubLongHolder extends LongHolder {
    private SubLongHolder(int newFieldInt) {
      super(newFieldInt); // To show that coercions occur even in super() params.
    }
  }

  public static void main(String... args) {
    assertTrue(new LongHolder().fieldLong == 100L);

    byte fbyte = 11;
    char fchar = 12;
    short fshort = 13;
    int fint = 14;
    long flong = 15;
    float ffloat = 16;
    double fdouble = 17;

    long tlong = 0;
    assertTrue(tlong == 0L);

    // Direct assignments from smaller types.
    {
      tlong = fbyte;
      assertTrue(tlong == (long) fbyte);
      tlong = fchar;
      assertTrue(tlong == (long) fchar);
      tlong = fshort;
      assertTrue(tlong == (long) fshort);
      tlong = fint;
      assertTrue(tlong == (long) fint);
      tlong = flong;
      assertTrue(tlong == (long) flong);
      // tlong = ffloat; // illegal
      // tlong = fdouble; // illegal
    }

    // Implicit casts to long when performing any assignment binary operation on a long and any
    // non-long type.
    {
      // If implicit casts are not inserted then long library operations will receive invalid
      // parameters and result in a runtime error.
      tlong = fint;
      assertTrue(tlong == 14L);
      tlong += fint;
      assertTrue(tlong == 28L);
      tlong -= fint;
      assertTrue(tlong == 14L);
      tlong *= fint;
      assertTrue(tlong == 196L);
      tlong /= fint;
      assertTrue(tlong == 14L);
      tlong &= fint;
      assertTrue(tlong == 14L);
      tlong |= fint;
      assertTrue(tlong == 14L);
      tlong ^= fint;
      assertTrue(tlong == 0L);
      tlong %= fint;
      assertTrue(tlong == 0L);
      tlong <<= fint; // Does not cast to long on right hand side.
      assertTrue(tlong == 0L);
      tlong >>= fint; // Does not cast to long on right hand side.
      assertTrue(tlong == 0L);
      tlong >>>= fint; // Does not cast to long on right hand side.
      assertTrue(tlong == 0L);
    }

    // Implicit casts to long when performing the PLUS_EQUALS binary operation on a long and any
    // non-long type.
    {
      tlong += fbyte;
      assertTrue(tlong == 11L);
      tlong += fchar;
      assertTrue(tlong == 23L);
      tlong += fshort;
      assertTrue(tlong == 36L);
      tlong += fint;
      assertTrue(tlong == 50L);
      tlong += flong;
      assertTrue(tlong == 65L);
      tlong += ffloat;
      assertTrue(tlong == 81L);
      tlong += fdouble;
      assertTrue(tlong == 98L);
    }

    // Implicit casts to long when performing any non assignment binary operation on a long and any
    // non-long type.
    {
      tlong = flong * fint;
      assertTrue(tlong == 210L);
      tlong = flong / fint;
      assertTrue(tlong == 1L);
      tlong = flong % fint;
      assertTrue(tlong == 1L);
      tlong = flong + fint;
      assertTrue(tlong == 29L);
      tlong = flong - fint;
      assertTrue(tlong == 1L);
      tlong = flong << fint; // Does not cast to long on right hand side.
      assertTrue(tlong == 245760L);
      tlong = flong >> fint; // Does not cast to long on right hand side.
      assertTrue(tlong == 0L);
      tlong = flong >>> fint; // Does not cast to long on right hand side.
      assertTrue(tlong == 0L);
      // tlong = flong < fint; // illegal
      // tlong = flong > fint; // illegal
      // tlong = flong <= fint; // illegal
      // tlong = flong >= fint; // illegal
      // tlong = flong == fint; // illegal
      // tlong = flong != fint; // illegal
      tlong = flong ^ fint;
      assertTrue(tlong == 1L);
      tlong = flong & fint;
      assertTrue(tlong == 14L);
      tlong = flong | fint;
      assertTrue(tlong == 15L);
      // tlong = flong && fint; // illegal
      // tlong = flong || fint; // illegal
    }

    // Implicit casts to long when performing the PLUS binary operation on a long and any non-long
    // type.
    {
      tlong = flong + fbyte;
      assertTrue(tlong == 26L);
      tlong = flong + fchar;
      assertTrue(tlong == 27L);
      tlong = flong + fshort;
      assertTrue(tlong == 28L);
      tlong = flong + fint;
      assertTrue(tlong == 29L);
      tlong = flong + flong;
      assertTrue(tlong == 30L);
      // tlong = flong + ffloat; // illegal
      // tlong = flong + fdouble; // illegal
    }

    // Implicit casts to long when passing into a long parameter slot.
    {
      compareLong(fbyte, (long) fbyte);
      assertTrue(new LongHolder(fbyte).fieldLong == (long) fbyte);
      compareLong(fchar, (long) fchar);
      assertTrue(new LongHolder(fchar).fieldLong == (long) fchar);
      compareLong(fshort, (long) fshort);
      assertTrue(new LongHolder(fshort).fieldLong == (long) fshort);
      compareLong(fint, (long) fint);
      assertTrue(new LongHolder(fint).fieldLong == (long) fint);
      compareLong(flong, (long) flong);
      assertTrue(new LongHolder(flong).fieldLong == (long) flong);
      // compareLong(ffloat, (long)ffloat); // illegal
      // assertTrue(new Container(ffloat).fieldLong == (long) ffloat); // illegal
      // compareLong(fdouble, (long)fdouble); // illegal
      // assertTrue(new Container(fdouble).fieldLong == (long) fdouble); // illegal

      // To show that coercions occur even in super() params.
      assertTrue(new SubLongHolder(fint).fieldLong == (long) fint);
    }

    {
      // If implicit casts away from long are not inserted then long library operations will receive
      // invalid parameters and result in a runtime error.
      tlong = flong << flong;
      assertTrue(tlong == 491520L);
      tlong = flong >> flong;
      assertTrue(tlong == 0L);
      tlong = flong >>> flong;
      assertTrue(tlong == 0L);
      tlong <<= flong;
      assertTrue(tlong == 0L);
      tlong >>= flong;
      assertTrue(tlong == 0L);
      tlong >>>= flong;
      assertTrue(tlong == 0L);
    }

    assertTrue(Long.MAX_VALUE != 9223372036854776833d);

    {
      // Long arrays
      int i = 0;
      long[] longArray = new long[3];
      longArray[i++] += 3;
      assertTrue(i == 1);
    }
  }

  public static void compareLong(long leftValue, long rightValue) {
    assertTrue(leftValue == rightValue);
  }
}
