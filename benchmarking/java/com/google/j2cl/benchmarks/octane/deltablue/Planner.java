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

/** The DeltaBlue planner */
class Planner {

  private int currentMark = 0;

  /**
   * Attempt to satisfy the given constraint and, if successful, incrementally update the dataflow
   * graph. Details: If satifying the constraint is successful, it may override a weaker constraint
   * on its output. The algorithm attempts to resatisfy that constraint using some other method.
   * This process is repeated until either a) it reaches a variable that was not previously
   * determined by any constraint or b) it reaches a constraint that is too weak to be satisfied
   * using any of its methods. The variables of constraints that have been processed are marked with
   * a unique mark value so that we know where we've been. This allows the algorithm to avoid
   * getting into an infinite loop even if the constraint graph has an inadvertent cycle.
   */
  void incrementalAdd(Constraint c) {
    int mark = newMark();
    Constraint overridden = c.satisfy(mark);
    while (overridden != null) {
      overridden = overridden.satisfy(mark);
    }
  }

  /**
   * Entry point for retracting a constraint. Remove the given constraint and incrementally update
   * the dataflow graph. Details: Retracting the given constraint may allow some currently
   * unsatisfiable downstream constraint to be satisfied. We therefore collect a list of unsatisfied
   * downstream constraints and attempt to satisfy each one in turn. This list is traversed by
   * constraint strength, strongest first, as a heuristic for avoiding unnecessarily adding and then
   * overriding weak constraints. Assume: c is satisfied.
   */
  void incrementalRemove(Constraint c) {
    Variable out = c.output();
    c.markUnsatisfied();
    c.removeFromGraph();
    List<Constraint> unsatisfied = removePropagateFrom(out);
    Strength strength = Strength.REQUIRED;
    do {
      for (Constraint u : unsatisfied) {
        if (u.strength.equals(strength)) {
          incrementalAdd(u);
        }
      }
      strength = strength.nextWeaker();
    } while (strength != Strength.WEAKEST);
  }

  /** Select a previously unused mark value. */
  private int newMark() {
    return ++currentMark;
  }

  /**
   * Extract a plan for resatisfaction starting from the given source constraints, usually a set of
   * input constraints. This method assumes that stay optimization is desired; the plan will contain
   * only constraints whose output variables are not stay. Constraints that do no computation, such
   * as stay and edit constraints, are not included in the plan. Details: The outputs of a
   * constraint are marked when it is added to the plan under construction. A constraint may be
   * appended to the plan when all its input variables are known. A variable is known if either a)
   * the variable is marked (indicating that has been computed by a constraint appearing earlier in
   * the plan), b) the variable is 'stay' (i.e. it is a constant at plan execution time), or c) the
   * variable is not determined by any constraint. The last provision is for past states of history
   * variables, which are not stay but which are also not computed by any constraint. Assume:
   * sources are all satisfied.
   */
  private Plan makePlan(List<Constraint> sources) {
    int mark = newMark();
    Plan plan = new Plan();
    while (!sources.isEmpty()) {
      Constraint c = sources.remove(sources.size() - 1);
      if (c.output().mark != mark && c.inputsKnown(mark)) {
        plan.addConstraint(c);
        c.output().mark = mark;
        addConstraintsConsumingTo(c.output(), sources);
      }
    }
    return plan;
  }

  /**
   * Extract a plan for resatisfying starting from the output of the given constraints, usually a
   * set of input constraints.
   */
  Plan extractPlanFromConstraints(List<Constraint> constraints) {
    List<Constraint> sources = new ArrayList<>();
    for (Constraint c : constraints) {
      if (c.isInput() && c.isSatisfied()) {
        // not in plan already and eligible for inclusion
        sources.add(c);
      }
    }
    return makePlan(sources);
  }

  /**
   * Recompute the walkabout strengths and stay flags of all variables downstream of the given
   * constraint and recompute the actual values of all variables whose stay flag is true. If a cycle
   * is detected, remove the given constraint and answer false. Otherwise, answer true. Details:
   * Cycles are detected when a marked variable is encountered downstream of the given constraint.
   * The sender is assumed to have marked the inputs of the given constraint with the given mark.
   * Thus, encountering a marked node downstream of the output constraint means that there is a path
   * from the constraint's output to one of its inputs.
   */
  boolean addPropagate(Constraint c, int mark) {
    List<Constraint> todo = new ArrayList<>();
    todo.add(c);
    while (!todo.isEmpty()) {
      Constraint d = todo.remove(todo.size() - 1);
      if (d.output().mark == mark) {
        incrementalRemove(c);
        return false;
      }
      d.recalculate();
      addConstraintsConsumingTo(d.output(), todo);
    }
    return true;
  }

  /**
   * Update the walkabout strengths and stay flags of all variables downstream of the given
   * constraint. Answer a collection of unsatisfied constraints sorted in order of decreasing
   * strength.
   */
  private List<Constraint> removePropagateFrom(Variable out) {
    out.determinedBy = null;
    out.walkStrength = Strength.WEAKEST;
    out.stay = true;
    List<Constraint> unsatisfied = new ArrayList<>();
    List<Variable> todo = new ArrayList<>();
    todo.add(out);
    while (!todo.isEmpty()) {
      Variable v = todo.remove(todo.size() - 1);
      for (Constraint c : v.constraints) {
        if (!c.isSatisfied()) {
          unsatisfied.add(c);
        }
      }
      Constraint determining = v.determinedBy;
      for (Constraint next : v.constraints) {
        if (!next.equals(determining) && next.isSatisfied()) {
          next.recalculate();
          todo.add(next.output());
        }
      }
    }
    return unsatisfied;
  }

  private void addConstraintsConsumingTo(Variable v, List<Constraint> coll) {
    Constraint determining = v.determinedBy;
    List<Constraint> cc = v.constraints;
    for (Constraint c : cc) {
      if (c != determining && c.isSatisfied()) {
        coll.add(c);
      }
    }
  }
}
