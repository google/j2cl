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

/** Abstract superclass for constraints having two possible output variables. */
abstract class BinaryConstraint extends Constraint {

  Variable v1;
  Variable v2;
  int direction;

  BinaryConstraint(Variable var1, Variable var2, Strength strength) {
    super(strength);
    this.v1 = var1;
    this.v2 = var2;
    this.direction = Direction.NONE;
    // for the java version we have to move the call to addConstraint()
    // to the child classes since we need to call the parent constructor
    // first and this would cause the benchmark to fail
    // this.addConstraint();
  }

  /**
   * Decides if this constraint can be satisfied and which way it should flow based on the relative
   * strength of the variables related, and record that decision.
   */
  @Override
  void chooseMethod(int mark) {
    if (v1.mark == mark) {
      direction =
          (v2.mark != mark && Strength.stronger(strength, v2.walkStrength))
              ? Direction.FORWARD
              : Direction.NONE;
    }
    if (v2.mark == mark) {
      direction =
          (v1.mark != mark && Strength.stronger(strength, v1.walkStrength))
              ? Direction.BACKWARD
              : Direction.NONE;
    }
    if (Strength.weaker(v1.walkStrength, v2.walkStrength)) {
      direction =
          Strength.stronger(strength, v1.walkStrength) ? Direction.BACKWARD : Direction.NONE;
    } else {
      direction =
          Strength.stronger(strength, v2.walkStrength) ? Direction.FORWARD : Direction.BACKWARD;
    }
  }

  /** Add this constraint to the constraint graph */
  @Override
  void addToGraph() {
    v1.addConstraint(this);
    v2.addConstraint(this);
    direction = Direction.NONE;
  }

  /** Answer true if this constraint is satisfied in the current solution. */
  @Override
  boolean isSatisfied() {
    return direction != Direction.NONE;
  }

  /** Mark the input variable with the given mark. */
  @Override
  void markInputs(int mark) {
    input().mark = mark;
  }

  /** Returns the current input variable */
  Variable input() {
    return (direction == Direction.FORWARD) ? v1 : v2;
  }

  /** Returns the current output variable */
  @Override
  Variable output() {
    return (direction == Direction.FORWARD) ? v2 : v1;
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
    out.stay = ihn.stay;
    if (out.stay) {
      execute();
    }
  }

  /** Record the fact that this constraint is unsatisfied. */
  @Override
  void markUnsatisfied() {
    direction = Direction.NONE;
  }

  @Override
  boolean inputsKnown(int mark) {
    Variable i = input();
    return i.mark == mark || i.stay || i.determinedBy == null;
  }

  @Override
  void removeFromGraph() {
    if (v1 != null) {
      v1.removeConstraint(this);
    }
    if (v2 != null) {
      v2.removeConstraint(this);
    }
    direction = Direction.NONE;
  }
}
