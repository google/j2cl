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
 * A Java implementation of the DeltaBlue constraint-solving algorithm, as described in: "The
 * DeltaBlue Algorithm: An Incremental Constraint Hierarchy Solver" by Bjorn N. Freeman-Benson and
 * John Maloney.
 *
 * <p>Beware: this benchmark is written in a grotesque style where the constraint model is built by
 * side-effects from constructors. We've kept it this way to avoid deviating too much from the
 * original implementation.
 */
public class DeltaBlue {
  static Planner planner;

  /**
   * This is the standard DeltaBlue benchmark. A long chain of equality constraints is constructed
   * with a stay constraint on one end. An edit constraint is then added to the opposite end and the
   * time is measured for adding and removing this constraint, and extracting and executing a
   * constraint satisfaction plan. There are two cases. In case 1, the added constraint is stronger
   * than the stay constraint and values must propagate down the entire length of the chain. In case
   * 2, the added constraint is weaker than the stay constraint so it cannot be accomodated. The
   * cost in this case is, of course, very low. Typical situations lie somewhere between these two
   * extremes.
   */
  public static void chainTest(int n) {
    planner = new Planner();
    Variable prev = null;
    Variable first = null;
    Variable last = null;
    // Build chain of n equality constraints
    for (int i = 0; i <= n; i++) {
      String name = "v" + i;
      Variable v = new Variable(name);
      if (prev != null) {
        new EqualityConstraint(prev, v, Strength.REQUIRED);
      }
      if (i == 0) {
        first = v;
      }
      if (i == n) {
        last = v;
      }
      prev = v;
    }
    new StayConstraint(last, Strength.STRONG_DEFAULT);
    Constraint edit = new EditConstraint(first, Strength.PREFERRED);
    List<Constraint> edits = new ArrayList<>();
    edits.add(edit);
    Plan plan = planner.extractPlanFromConstraints(edits);
    for (int i = 0; i < 100; i++) {
      first.value = i;
      plan.execute();
      if (last.value != i) {
        throw new AssertionError("Chain test failed.");
      }
    }
  }

  /**
   * This test constructs a two sets of variables related to each other by a simple linear
   * transformation (scale and offset). The time is measured to change a variable on either side of
   * the mapping and to change the scale and offset factors.
   */
  public static void projectionTest(int n) {
    planner = new Planner();
    Variable scale = new Variable("scale", 10);
    Variable offset = new Variable("offset", 1000);
    Variable src = null;
    Variable dst = null;
    List<Variable> dests = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      src = new Variable("src" + i, i);
      dst = new Variable("dst" + i, i);
      dests.add(dst);
      new StayConstraint(src, Strength.NORMAL);
      new ScaleConstraint(src, scale, offset, dst, Strength.REQUIRED);
    }

    change(src, 17);
    if (dst.value != 1170) {
      throw new AssertionError("Projection 1 failed");
    }

    change(dst, 1050);
    if (src.value != 5) {
      throw new AssertionError("Projection 2 failed");
    }

    change(scale, 5);
    for (int i = 0; i < n - 1; i++) {
      if (dests.get(i).value != i * 5 + 1000) {
        throw new AssertionError("Projection 3 failed");
      }
    }
    change(offset, 2000);
    for (int i = 0; i < n - 1; i++) {
      if (dests.get(i).value != i * 5 + 2000) {
        throw new AssertionError("Projection 4 failed");
      }
    }
  }

  private static void change(Variable v, int newValue) {
    EditConstraint edit = new EditConstraint(v, Strength.PREFERRED);
    List<Constraint> edits = new ArrayList<>();
    edits.add(edit);
    Plan plan = planner.extractPlanFromConstraints(edits);
    for (int i = 0; i < 10; i++) {
      v.value = newValue;
      plan.execute();
    }
    edit.destroyConstraint();
  }

  private DeltaBlue() {}
}
