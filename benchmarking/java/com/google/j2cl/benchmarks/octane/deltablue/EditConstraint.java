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

/** A unary input constraint used to mark a variable that the client wishes to change. */
class EditConstraint extends UnaryConstraint {

  EditConstraint(Variable v, Strength str) {
    super(v, str);
  }

  /** Edits indicate that a variable is to be changed by imperative code. */
  @Override
  boolean isInput() {
    return true;
  }

  @Override
  void execute() {
    // Edit constraints do nothing
  }
}
