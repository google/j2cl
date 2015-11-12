package com.google.j2cl.transpiler.integration.enumspecialfunctions;

public enum Planet {
  MERCURY(1),
  VENUS(2),
  EARTH(3),
  MARS(4),
  JUPITER(5),
  SATURN(6),
  URANUS(7),
  NEPTUNE(8);

  public static final Planet extraPlanet = Planet.MERCURY;

  private int index;

  Planet(int index) {
    this.index = index;
  }
}
