package java.lang;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Long.html">the
 * official Java API doc</a> for details.
 */
public class Long extends Number {
  private long value;

  public Long(long value) {
    this.value = value;
  }

  @Override
  public byte byteValue() {
    return (byte) value;
  }

  @Override
  public double doubleValue() {
    return (double) value;
  }

  @Override
  public float floatValue() {
    return (float) value;
  }

  @Override
  public int intValue() {
    return (int) value;
  }

  @Override
  public long longValue() {
    return value;
  }

  @Override
  public short shortValue() {
    return (short) value;
  }
}
