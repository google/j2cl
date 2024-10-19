/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.benchmarks.octane.deltablue;

/**
 * Relates two variables by the linear scaling relationship: "v2 = (v1 * scale) + offset". Either v1
 * or v2 may be changed to maintain this relationship but the scale factor and offset are considered
 * read-only.
 */
class ScaleConstraint extends BinaryConstraint {

  private final Variable scale;
  private final Variable offset;

  ScaleConstraint(Variable src, Variable scale, Variable offset, Variable dest, Strength strength) {
    super(src, dest, strength);
    this.direction = Direction.NONE;
    this.scale = scale;
    this.offset = offset;
    // this line needed to be added see comment in BinaryConstraint's constructor
    this.addConstraint();
  }

  /** Adds this constraint to the constraint graph. */
  @Override
  void addToGraph() {
    super.addToGraph();
    scale.addConstraint(this);
    offset.addConstraint(this);
  }

  @Override
  void removeFromGraph() {
    super.removeFromGraph();
    if (scale != null) {
      scale.removeConstraint(this);
    }
    if (offset != null) {
      offset.removeConstraint(this);
    }
  }

  @Override
  void markInputs(int mark) {
    super.markInputs(mark);
    scale.mark = offset.mark = mark;
  }

  /** Enforce this constraint. Assume that it is satisfied. */
  @Override
  void execute() {
    if (direction == Direction.FORWARD) {
      v2.value = v1.value * scale.value + offset.value;
    } else {
      v1.value = (v2.value - offset.value) / scale.value;
    }
  }

  /**
   * Calculate the walkabout strength, the stay flag, and, if it is 'stay', the value for the
   * current output of this constraint. Assume this constraint is satisfied.
   */
  @Override
  void recalculate() {
    Variable ihn = input();
    Variable out = output();
    out.walkStrength = Strength.weakestOf(strength, ihn.walkStrength);
    out.stay = ihn.stay && scale.stay && offset.stay;
    if (out.stay) {
      execute();
    }
  }
}
