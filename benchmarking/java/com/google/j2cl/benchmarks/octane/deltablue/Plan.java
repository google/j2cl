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
 * A Plan is an ordered list of constraints to be executed in sequence to resatisfy all currently
 * satisfiable constraints in the face of one or more changing inputs.
 */
class Plan {
  private final List<Constraint> v = new ArrayList<>();

  void addConstraint(Constraint c) {
    v.add(c);
  }

  private int size() {
    return v.size();
  }

  private Constraint constraintAt(int index) {
    return v.get(index);
  }

  void execute() {
    for (int i = 0; i < size(); i++) {
      Constraint c = constraintAt(i);
      c.execute();
    }
  }
}
