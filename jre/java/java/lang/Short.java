package java.lang;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Short.html">the
 * official Java API doc</a> for details.
 */
public class Short extends Number {
  private short value;

  public Short(short value) {
    this.value = value;
  }

  @Override
  public byte byteValue() {
    return (byte) value;
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public float floatValue() {
    return value;
  }

  @Override
  public int intValue() {
    return value;
  }

  @Override
  public long longValue() {
    return (long) value;
  }

  @Override
  public short shortValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Short) && (((Short) o).value == value);
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public String toString() {
    return "" + value;
  }
}
