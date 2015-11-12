package com.google.j2cl.transpiler.integration.enumspecialfunctions;

/**
 * This class tests the special functions of enum: .values() and .valueOf()
 */
public class Main {
  public static void main(String[] args) {
    //assert Enum.valueOf(Planet.class, "SATURN")
    assert Planet.values().length == 8;
    assert arrayContains(Planet.MERCURY, Planet.values());
    assert arrayContains(Planet.VENUS, Planet.values());
    assert arrayContains(Planet.EARTH, Planet.values());
    assert arrayContains(Planet.MARS, Planet.values());
    assert arrayContains(Planet.JUPITER, Planet.values());
    assert arrayContains(Planet.SATURN, Planet.values());
    assert arrayContains(Planet.URANUS, Planet.values());
    assert arrayContains(Planet.NEPTUNE, Planet.values());

    assert Planet.valueOf("MERCURY") == Planet.MERCURY;
    assert Planet.valueOf("VENUS") == Planet.VENUS;
    assert Planet.valueOf("EARTH") == Planet.EARTH;
    assert Planet.valueOf("MARS") == Planet.MARS;
    assert Planet.valueOf("JUPITER") == Planet.JUPITER;
    assert Planet.valueOf("SATURN") == Planet.SATURN;
    assert Planet.valueOf("URANUS") == Planet.URANUS;
    assert Planet.valueOf("NEPTUNE") == Planet.NEPTUNE;

    try {
      Planet.valueOf("NOTHING");
      assert false : "Should have thrown IllegalArgumentException.";
    } catch (IllegalArgumentException e) {
      // do nothing.
    }

    try {
      Planet.valueOf(null);
      assert false : "Should have thrown IllegalArgumentException.";
    } catch (IllegalArgumentException e) {
      // do nothing.
    }
  }

  private static boolean arrayContains(Object obj, Object[] array) {
    for (Object element : array) {
      if (element == obj) {
        return true;
      }
    }
    return false;
  }
}
