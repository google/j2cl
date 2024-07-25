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
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Assigns a field index in the itable for each interface.
 *
 * <p>Each interface inheritance chain implemented in the same class will have a distinct field
 * index, but across different parts of the hierarchy indices can be reused. This algorithm
 * heuristically minimizes the size of the itable by trying to assign indices in order of interfaces
 * with most implementors. Each index in the itable instance will have the interface vtable (for the
 * most specific interface in the inheritance chain) for the class and can be used for both dynamic
 * interface dispatch and interface "instanceof" checks.
 *
 * <p>This is a baseline implementation of "packed encoding" based on the algorithm described in
 * section 4.3 of "Efficient type inclusion tests" by Vitek et al (OOPSLA 97). Although the ideas
 * presented in the paper are for performing "instanceof" checks, they generalize to interface
 * dispatch.
 */
public class ItableAllocator<T> {
  private final SetMultimap<T, T> classesByInterface = LinkedHashMultimap.create();
  private final List<Set<T>> classesByFieldindex = new ArrayList<>();
  private final List<Set<T>> interfacesByFieldIndex = new ArrayList<>();

  private final SetMultimap<T, T> implementedInterfacesByClass = LinkedHashMultimap.create();
  private final Map<T, T> superInterfaceByInterface = new HashMap<>();

  /**
   * Multiset that keeps track of the field index for an interface.
   *
   * <p>Note that the actual index is the count - 1, since the count of 0 is reserved for interfaces
   * that are not implemented by any class.
   */
  private final Multiset<T> itableIndexByInterface = HashMultiset.create();

  public ItableAllocator(
      List<T> classes,
      Function<T, Set<T>> implementedInterfaceByType,
      Function<T, T> superInterfaceByType) {
    // Traverse all classes collecting the interfaces they implement. Actual vtable
    // instances are only required for concrete classes, because they provide the references to the
    // methods that will be invoked on a specific instance, but since we are specializing the
    // the itable types, we need to involve all classes in order to assign correct field indices.
    // Since all dynamic dispatch is performed by obtaining the vtables from an instance, if there
    // are no instances for a type, there is no need for instances of vtables it.
    classes.forEach(
        c ->
            implementedInterfaceByType
                .apply(c)
                .forEach(
                    i -> {
                      classesByInterface.put(i, c);
                      implementedInterfacesByClass.put(c, i);
                    }));

    classesByInterface
        .keySet()
        .forEach(i -> superInterfaceByInterface.put(i, superInterfaceByType.apply(i)));

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
    addClassesToFieldIndex(fieldIndex, interfaceToAssign);
  }

  private void addClassesToFieldIndex(int fieldIndex, T interfaceToAdd) {
    if (classesByFieldindex.size() == fieldIndex) {
      classesByFieldindex.add(new HashSet<>());
      interfacesByFieldIndex.add(new HashSet<>());
    }
    classesByFieldindex.get(fieldIndex).addAll(classesByInterface.get(interfaceToAdd));
    interfacesByFieldIndex.get(fieldIndex).add(interfaceToAdd);
  }

  /** Finds the lowest non-conflicting field index for {@code interface}. */
  private int getFirstNonConflictingFieldIndex(T interfaceToAssign) {
    // Assign an index.
    // Interfaces whose implementers are disjoint can share the same index. Implementers are
    // considered disjoint if no implementors implement both of the interfaces, and if they do, one
    // of the interfaces extends the other.
    // For example, `Iterable`, `Collection`, and `List` may be assigned the same index. `List` and
    // `Set` (also a child of `Collection` but doesn't extend each other) may only share the same
    // index if there is no class that implements both.
    int itableSize = classesByFieldindex.size();
    for (int index = 0; index < itableSize; index++) {
      Set<T> alreadyAssignedInterfaces = interfacesByFieldIndex.get(index);
      // Perform an intersection on the classes who have interfaces assigned to this index and the
      // classes who implement the interface to assign. This is the set of classes that possibly
      // conflict.
      // Among these classes, check if there are any interfaces that:
      // - Are already assigned to this index, AND:
      // - Are not part of the same inheritance chain.
      // If there are no such interfaces, then the index can be assigned to this interface.
      if (Sets.intersection(
              classesByFieldindex.get(index), classesByInterface.get(interfaceToAssign))
          .stream()
          .flatMap(c -> implementedInterfacesByClass.get(c).stream())
          .distinct()
          .filter(alreadyAssignedInterfaces::contains)
          .allMatch(i -> sharesInheritanceChain(interfaceToAssign, i))) {
        return index;
      }
    }
    // Couldn't find an index that is not conflicting, hence return a new index and implicitly
    // increment the size of the itable.
    return itableSize;
  }

  /**
   * Determines if the given 2 interfaces are part of the same inheritance chain. Returns {@code
   * true} if one is a superinterface of the other.
   */
  private boolean sharesInheritanceChain(T interface1, T interface2) {
    return interface1.equals(interface2)
        || isSubinterfaceOf(interface1, interface2)
        || isSubinterfaceOf(interface2, interface1);
  }

  private boolean isSubinterfaceOf(T maybeSubinterface, T superinterface) {
    T maybeSuperinterface = superInterfaceByInterface.get(maybeSubinterface);
    if (maybeSuperinterface == null) {
      return false;
    }
    return maybeSuperinterface.equals(superinterface)
        || isSubinterfaceOf(maybeSuperinterface, superinterface);
  }
}
