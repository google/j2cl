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

import java.util.ArrayList;
import java.util.List;

/**
 * A constrained variable. In addition to its value, it maintain the structure of the constraint
 * graph, the current dataflow graph, and various parameters of interest to the DeltaBlue
 * incremental constraint solver.
 */
class Variable {

  int mark;
  String name;
  int value;
  List<Constraint> constraints;
  Constraint determinedBy;
  Strength walkStrength;
  boolean stay;

  Variable(String name) {
    this(name, 0);
  }

  Variable(String name, int initialValue) {
    this.value = initialValue;
    this.constraints = new ArrayList<>();
    this.determinedBy = null;
    this.walkStrength = Strength.WEAKEST;
    this.stay = true;
    this.name = name;
  }

  /** Add the given constraint to the set of all constraints that refer this variable. */
  void addConstraint(Constraint c) {
    constraints.add(c);
  }

  /** Removes all traces of c from this variable. */
  void removeConstraint(Constraint c) {
    doRemoveConstraint(c);
    if (determinedBy == c) {
      determinedBy = null;
    }
  }

  private void doRemoveConstraint(Constraint c) {
    List<Constraint> newConstraints = new ArrayList<>(constraints.size());
    for (Constraint value : constraints) {
      if (value != c) {
        newConstraints.add(value);
      }
    }
    constraints = newConstraints;
  }
}
