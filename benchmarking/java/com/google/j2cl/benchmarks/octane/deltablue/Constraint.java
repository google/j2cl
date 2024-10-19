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

import javax.annotation.Nullable;

/**
 * An abstract class representing a system-maintainable relationship (or "constraint") between a set
 * of variables. A constraint supplies a strength instance variable; concrete subclasses provide a
 * means of storing the constrained variables and other information required to represent a
 * constraint.
 */
abstract class Constraint {

  Strength strength;

  Constraint(Strength strength) {
    this.strength = strength;
  }

  /** Activate this constraint and attempt to satisfy it. */
  void addConstraint() {
    addToGraph();
    DeltaBlue.planner.incrementalAdd(this);
  }

  abstract void addToGraph();

  /**
   * Attempt to find a way to enforce this constraint. If successful, record the solution, perhaps
   * modifying the current dataflow graph. Answer the constraint that this constraint overrides, if
   * there is one, or nil, if there isn't. Assume: I am not already satisfied.
   */
  @Nullable
  Constraint satisfy(int mark) {
    chooseMethod(mark);
    if (!isSatisfied()) {
      if (strength == Strength.REQUIRED) {
        // removed console and added RuntimeException
        // Console.log("Could not satisfy a required constraint!");
        throw new AssertionError("Could not satisfy a required constraint!");
      }
      return null;
    }
    markInputs(mark);
    Variable out = output();
    Constraint overridden = out.determinedBy;
    if (overridden != null) {
      overridden.markUnsatisfied();
    }
    out.determinedBy = this;
    if (!DeltaBlue.planner.addPropagate(this, mark)) {
      // removed console and added RuntimeException
      // Console.log("Cycle encountered");
      throw new AssertionError("Cycle encountered");
    }

    out.mark = mark;
    return overridden;
  }

  void destroyConstraint() {
    if (isSatisfied()) {
      DeltaBlue.planner.incrementalRemove(this);
    } else {
      removeFromGraph();
    }
  }

  /**
   * Normal constraints are not input constraints. An input constraint is one that depends on
   * external state, such as the mouse, the keybord, a clock, or some arbitraty piece of imperative
   * code.
   */
  boolean isInput() {
    return false;
  }

  abstract void removeFromGraph();

  abstract void markInputs(int mark);

  abstract boolean isSatisfied();

  abstract void chooseMethod(int mark);

  abstract Variable output();

  abstract void markUnsatisfied();

  abstract void execute();

  abstract void recalculate();

  abstract boolean inputsKnown(int mark);
}
