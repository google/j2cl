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

/** Abstract superclass for constraints having a single possible output variable. */
abstract class UnaryConstraint extends Constraint {

  private final Variable myOutput;
  private boolean satisfied;

  UnaryConstraint(Variable v, Strength strength) {
    super(strength);
    this.myOutput = v;
    this.satisfied = false;
    this.addConstraint();
  }

  /** Adds this constraint to the constraint graph */
  @Override
  void addToGraph() {
    myOutput.addConstraint(this);
    satisfied = false;
  }

  /** Decides if this constraint can be satisfied and records that decision. */
  @Override
  void chooseMethod(int mark) {
    satisfied = (myOutput.mark != mark) && Strength.stronger(strength, myOutput.walkStrength);
  }

  /** Returns true if this constraint is satisfied in the current solution. */
  @Override
  boolean isSatisfied() {
    return satisfied;
  }

  @Override
  void markInputs(int mark) {
    // has no inputs
  }

  /** Returns the current output variable. */
  @Override
  Variable output() {
    return myOutput;
  }

  /**
   * Calculate the walkabout strength, the stay flag, and, if it is 'stay', the value for the
   * current output of this constraint. Assume this constraint is satisfied.
   */
  @Override
  void recalculate() {
    myOutput.walkStrength = strength;
    myOutput.stay = !isInput();
    if (myOutput.stay) {
      // Stay optimization
      execute();
    }
  }

  /** Records that this constraint is unsatisfied */
  @Override
  void markUnsatisfied() {
    satisfied = false;
  }

  @Override
  boolean inputsKnown(int mark) {
    return true;
  }

  @Override
  void removeFromGraph() {
    if (myOutput != null) {
      myOutput.removeConstraint(this);
    }
    satisfied = false;
  }
}
