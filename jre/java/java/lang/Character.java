package java.lang;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Character.html">the
 * official Java API doc</a> for details.
 */
public class Character {
  private char value;

  public Character(char value) {
    this.value = value;
  }

  public char charValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Character) && (((Character) o).value == value);
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