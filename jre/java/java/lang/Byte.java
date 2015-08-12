package java.lang;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Byte.html">the
 * official Java API doc</a> for details.
 */
public class Byte extends Number {
  private byte value;

  public Byte(byte value) {
    this.value = value;
  }

  @Override
  public byte byteValue() {
    return value;
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
    // TODO: remove explicit cast after implicit cast is supported.
    return (long) value;
  }

  @Override
  public short shortValue() {
    return value;
  }
}
