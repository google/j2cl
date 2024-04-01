/*
 * Copyright 2024 Google Inc.
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
package com.google.j2cl.transpiler.backend.wasm;

import static java.util.Comparator.comparingInt;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Assigns a field index in the itable for each interface.
 *
 * <p>Each interfaces implemented in the same class will have a different field indices, but across
 * different parts of the hierarchy indices can be reused. This algorithm heuristically minimizes
 * the size of the itable by trying to assign indices in order of most implemented interfaces. Each
 * index in the itable instance will have the interface vtable for the class and can be used for
 * both dynamic interface dispatch and interface "instanceof" checks.
 *
 * <p>This is a baseline implementation of "packed encoding" based on the algorithm described in
 * section 4.3 of "Efficient type inclusion tests" by Vitek et al (OOPSLA 97). Although the ideas
 * presented in the paper are for performing "instanceof" checks, they generalize to interface
 * dispatch.
 */
public class ItableAllocator<T> {
  private final SetMultimap<T, T> classesByInterface = LinkedHashMultimap.create();
  private final List<Set<T>> classesByFieldindex = new ArrayList<>();

  /**
   * Multiset that keeps track of the field index for an interface.
   *
   * <p>Note that the actual index is the count - 1, since the count of 0 is reserved for interfaces
   * that are not implemented by any class.
   */
  private final Multiset<T> itableIndexByInterface = HashMultiset.create();

  public ItableAllocator(List<T> classes, Function<T, Set<T>> implementedInterfaceByType) {
    // Traverse all classes collecting the interfaces they implement. Actual vtable
    // instances are only required for concrete classes, because they provide the references to the
    // methods that will be invoked on a specific instance, but since we are specializing the
    // the itable types, we need to involve all classes in order to assign correct field indices.
    // Since all dynamic dispatch is performed by obtaining the vtables from an instance, if there
    // are no instances for a type, there is no need for instances of vtables it.
    classes.forEach(
        c -> implementedInterfaceByType.apply(c).forEach(i -> classesByInterface.put(i, c)));

    classesByInterface.keySet().stream()
        .sorted(comparingInt((T t) -> classesByInterface.get(t).size()).reversed())
        .forEach(this::assignFirstNonConflictingFieldIndex);
  }

  /** Returns the number of fields needed in the itable structures. */
  public int getItableSize() {
    return classesByFieldindex.size();
  }

  /**
   * Returns the field index for an interface, or -1 if the interface is not implemented by any
   * class.
   */
  public int getItableFieldIndex(T type) {
    return itableIndexByInterface.count(type) - 1;
  }

  /** Assigns the lowest non conflicting field index to {@code interfaceToAssign}. */
  private void assignFirstNonConflictingFieldIndex(T interfaceToAssign) {
    int fieldIndex = getFirstNonConflictingFieldIndex(interfaceToAssign);
    itableIndexByInterface.setCount(interfaceToAssign, fieldIndex + 1);
    // Add all the concrete implementors for that interface to the assigned fieldIndex, to mark
    // that fieldIndex as already used in all those types.
    addClassesToFieldIndex(fieldIndex, classesByInterface.get(interfaceToAssign));
  }

  private void addClassesToFieldIndex(int fieldIndex, Set<T> classes) {
    if (classesByFieldindex.size() == fieldIndex) {
      classesByFieldindex.add(new HashSet<>());
    }
    classesByFieldindex.get(fieldIndex).addAll(classes);
  }

  /** Finds the lowest non-conflicting field index for {@code interface}. */
  private int getFirstNonConflictingFieldIndex(T interfaceToAssign) {
    // Assign an index by finding the first non conflicting one. Interfaces that are implemented by
    // the same concrete class must have unique field indices but interfaces whose implementers are
    // disjoint can share the same field index.
    int itableSize = classesByFieldindex.size();
    for (int index = 0; index < itableSize; index++) {
      if (Collections.disjoint(
          classesByFieldindex.get(index), classesByInterface.get(interfaceToAssign))) {
        return index;
      }
    }
    // Couldn't find an index that is not conflicting, hence return a new index and implicitly
    // increment the size of the itable.
    return itableSize;
  }
}
