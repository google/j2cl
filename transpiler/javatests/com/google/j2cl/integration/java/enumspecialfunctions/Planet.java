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
package enumspecialfunctions;

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

  private final int index;

  Planet(int index) {
    this.index = index;
  }
}
