package com.google.j2cl.transpiler.integration.alllongoperations;

public class Main {
  public static void main(String... args) {
    testInfixOperations();
    testPrefixOperations();
    testPostfixOperations();
    testAssignmentOperators();
    testOverAndUnderflow();
    testLiteralFormats();
    testExtendedOperands();
    testInitialization();
  }

  public static void testAssignmentOperators() {
    long a = 0L;

    a += 1L;
    assert a == 1L;

    a -= 2L;
    assert a == -1L;

    a *= 30L;
    assert a == -30L;

    a /= -10L;
    assert a == 3L;

    a &= 2L;
    assert a == 2L;

    a |= 4L;
    assert a == 6L;

    a ^= 3L;
    assert a == 5L;

    a %= 2L;
    assert a == 1L;

    a <<= 3L;
    assert a == 8L;

    a >>= 1L;
    assert a == 4L;

    a >>>= 1L;
    assert a == 2L;
  }

  public static void testExtendedOperands() {
    long a = 1234L;

    assert 1254L == a + 5L + 5L + 5L + 5L;
  }

  public static void testInfixOperations() {
    long a = 1234L;
    long b = 9876L;
    long c = -55L;

    assert 12186984L == (a * b);
    assert 8L == (b / a);
    assert 4L == (b % a);
    assert 11110L == (a + b);
    assert -8642L == (a - b);
    assert 9872L == (a << 3); // right operand is not a long
    assert -7L == (c >> 3); // right operand is not a long
    assert 2305843009213693945L == (c >>> 3); // right operand is not a long
    assert a < b;
    assert b > a;
    assert b <= b;
    assert a <= b;
    assert b >= b;
    assert b >= a;
    assert 1234L == a;
    assert a != b;
    assert 8774L == (a ^ b);
    assert 1168L == (a & b);
    assert 9942L == (a | b);
  }

  public static void testLiteralFormats() {
    // Small longs get a fromInt() fast path.
    // ************************************************************
    long a = 0x0011eeffL;
    assert 1175295L == a; // Hex

    long b = 01234567L;
    assert 342391L == b; // Octal

    long c = 0b00011110L;
    assert 30L == c; // Binary

    // Huge longs fall back on fromString() construction.
    // ************************************************************
    long e = 0x20000000000001L;
    assert 9007199254740993L == e; // Hex

    long f = 0400000000000000001L;
    assert 9007199254740993L == f; // Octal

    long g = 0b100000000000000000000000000000000000000000000000000001L;
    assert 9007199254740993L == g; // Binary
  }

  @SuppressWarnings("ConstantOverflow")
  public static void testOverAndUnderflow() {
    long a = 999999999999999999L * 1000L;
    assert 3875820019684211736L == a;

    long b = 8999999999999999999L + 8999999999999999999L;
    assert -446744073709551618L == b;

    long c = 999999999999999999L * -1000L;
    assert -3875820019684211736L == c;

    long d = -8999999999999999999L - 8999999999999999999L;
    assert 446744073709551618L == d;
  }

  public static void testPostfixOperations() {
    long a = 100L;

    assert 100L == a++;
    assert 101L == a--;
  }

  public static void testPrefixOperations() {
    long a = 100L;

    assert 101L == ++a;
    assert 100L == --a;
    assert 100L == +a;
    assert -100L == -a;
    assert -101L == ~a;
  }

  private static long fieldLong = 100;

  private static long getReturnInitializerLong() {
    return 100;
  }

  public static void testInitialization() {
    long localVariableLong = 100;

    assert localVariableLong == fieldLong;
    assert fieldLong == getReturnInitializerLong();
  }
}
