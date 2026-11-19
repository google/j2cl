/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.common.visitor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Computes per-type traversal flags for {@link Processor} subclasses by inspecting which visitor or
 * rewriter callbacks are overridden and propagating reachability across the {@link Visitable} type
 * and field graph.
 *
 * <p>The generated visitors use the flags to skip the callbacks that are not overridden and the
 * parts of the AST from which no overridden callback can be reached.
 */
public final class TraversalFlags {

  /** Whether the pre-processing callback of the type or of one of its supertypes is overridden. */
  public static final int PRE_OVERRIDDEN = 1;

  /** Whether the post-processing callback of the type or of one of its supertypes is overridden. */
  public static final int POST_OVERRIDDEN = 2;

  /**
   * Whether an overridden callback might be reached from a node declared with the type, i.e.
   * whether the node needs to be visited.
   */
  public static final int REACHES_OVERRIDE = 4;

  /**
   * Whether an overridden callback might be reached from the visitable fields of a node of the
   * type, i.e. whether any of its fields need to be visited.
   */
  public static final int MEMBERS_REACH_OVERRIDE = 8;

  /**
   * Creates a {@link ClassValue} cache that computes the traversal flags for subclasses of {@code
   * baseClass}, as an array indexed like {@code visitableClasses}.
   *
   * @param prePrefix the name prefix of the pre-processing callbacks, e.g. {@code "enter"}
   * @param postPrefix the name prefix of the post-processing callbacks, e.g. {@code "exit"}
   * @param declaredFieldTargets for each class in {@code visitableClasses}, the indices of the
   *     element types of the visitable fields declared by that class
   */
  public static ClassValue<byte[]> createCache(
      Class<?> baseClass,
      String prePrefix,
      String postPrefix,
      Class<?>[] visitableClasses,
      int[][] declaredFieldTargets) {
    Map<Class<?>, Integer> indexByClass = HashMap.newHashMap(visitableClasses.length);
    for (int i = 0; i < visitableClasses.length; i++) {
      indexByClass.put(visitableClasses[i], i);
    }

    // The graph is the same for all processor classes; only the callbacks they override differ.
    int[][] supertypes = computeSupertypes(visitableClasses);
    int[][] fieldTargets = computeFieldTargets(supertypes, declaredFieldTargets);
    int[][] predecessors = computePredecessors(supertypes, fieldTargets);

    return new ClassValue<>() {
      @Override
      protected byte[] computeValue(Class<?> processorClass) {
        if (!baseClass.isAssignableFrom(processorClass)) {
          throw new IllegalArgumentException(
              processorClass.getName() + " is not a subclass of " + baseClass.getName());
        }
        byte[] overriddenCallbacks =
            findOverriddenCallbacks(processorClass, baseClass, prePrefix, postPrefix, indexByClass);
        return computeFlags(overriddenCallbacks, supertypes, fieldTargets, predecessors);
      }
    };
  }

  /**
   * Returns, for each visitable class, the indices of its visitable supertypes, including itself.
   *
   * <p>The subtype relation is computed from the Java types rather than from the {@code @Visitable}
   * hierarchy since it determines which nodes a field can hold at runtime, e.g. instances of a
   * visitable class whose visitable ancestor is only reachable through a non-visitable class.
   */
  private static int[][] computeSupertypes(Class<?>[] visitableClasses) {
    int[][] supertypes = new int[visitableClasses.length][];
    Arrays.setAll(
        supertypes,
        i ->
            IntStream.range(0, visitableClasses.length)
                .filter(s -> visitableClasses[s].isAssignableFrom(visitableClasses[i]))
                .toArray());
    return supertypes;
  }

  /**
   * Returns, for each visitable class, the indices of the element types of its visitable fields,
   * including the inherited ones.
   */
  private static int[][] computeFieldTargets(int[][] supertypes, int[][] declaredFieldTargets) {
    int[][] fieldTargets = new int[supertypes.length][];
    Arrays.setAll(
        fieldTargets,
        i ->
            Arrays.stream(supertypes[i])
                .flatMap(s -> Arrays.stream(declaredFieldTargets[s]))
                .distinct()
                .toArray());
    return fieldTargets;
  }

  /**
   * Returns, for each visitable class, the indices of the classes from which a node of the class
   * can be visited.
   *
   * <p>A node declared with type {@code t} can be visited from a node declared with type {@code s}
   * if {@code t} is a subtype of {@code s}, since a field of type {@code s} can hold a node of type
   * {@code t} at runtime, or if {@code s} has a visitable field of type {@code t}.
   */
  private static int[][] computePredecessors(int[][] supertypes, int[][] fieldTargets) {
    int n = supertypes.length;
    boolean[][] isPredecessor = new boolean[n][n];
    for (int i = 0; i < n; i++) {
      for (int superIndex : supertypes[i]) {
        isPredecessor[i][superIndex] = true;
      }
      for (int targetIndex : fieldTargets[i]) {
        isPredecessor[targetIndex][i] = true;
      }
    }

    int[][] predecessors = new int[n][];
    Arrays.setAll(
        predecessors, i -> IntStream.range(0, n).filter(p -> isPredecessor[i][p]).toArray());
    return predecessors;
  }

  /**
   * Returns, for each visitable class, the {@link #PRE_OVERRIDDEN} and {@link #POST_OVERRIDDEN}
   * flags of its callbacks that are overridden by {@code processorClass} or one of its superclasses
   * below {@code baseClass}.
   */
  private static byte[] findOverriddenCallbacks(
      Class<?> processorClass,
      Class<?> baseClass,
      String prePrefix,
      String postPrefix,
      Map<Class<?>, Integer> indexByClass) {
    byte[] overriddenCallbacks = new byte[indexByClass.size()];
    for (Class<?> c = processorClass; c != baseClass; c = c.getSuperclass()) {
      for (Method method : c.getDeclaredMethods()) {
        // Callbacks are public instance methods with exactly one parameter, the visited node, so
        // any other method, e.g. an overload with a different signature, is not a callback.
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers)
            || Modifier.isStatic(modifiers)
            || method.getParameterCount() != 1) {
          continue;
        }
        Class<?> parameterType = method.getParameterTypes()[0];
        Integer index = indexByClass.get(parameterType);
        if (index == null) {
          continue;
        }
        String methodName = method.getName();
        if (methodName.equals(prePrefix + parameterType.getSimpleName())) {
          overriddenCallbacks[index] |= PRE_OVERRIDDEN;
        } else if (methodName.equals(postPrefix + parameterType.getSimpleName())) {
          overriddenCallbacks[index] |= POST_OVERRIDDEN;
        }
      }
    }
    return overriddenCallbacks;
  }

  /**
   * Returns the traversal flags of each visitable class given the callbacks that are overridden.
   */
  private static byte[] computeFlags(
      byte[] overriddenCallbacks, int[][] supertypes, int[][] fieldTargets, int[][] predecessors) {
    int n = overriddenCallbacks.length;
    byte[] flags = new byte[n];

    // The default callbacks of a type delegate to the ones of its supertype, so overriding the
    // callback of a type also affects its subtypes.
    int[] worklist = new int[n];
    int worklistSize = 0;
    for (int i = 0; i < n; i++) {
      int callbacks = 0;
      for (int superIndex : supertypes[i]) {
        callbacks |= overriddenCallbacks[superIndex];
      }
      if (callbacks != 0) {
        flags[i] = (byte) (callbacks | REACHES_OVERRIDE);
        worklist[worklistSize++] = i;
      }
    }

    // A node needs to be visited if a node with overridden callbacks can be visited from it. Each
    // type is added to the worklist at most once, when its REACHES_OVERRIDE flag is set.
    for (int next = 0; next < worklistSize; next++) {
      for (int predecessor : predecessors[worklist[next]]) {
        if ((flags[predecessor] & REACHES_OVERRIDE) == 0) {
          flags[predecessor] |= REACHES_OVERRIDE;
          worklist[worklistSize++] = predecessor;
        }
      }
    }

    for (int i = 0; i < n; i++) {
      for (int targetIndex : fieldTargets[i]) {
        if ((flags[targetIndex] & REACHES_OVERRIDE) != 0) {
          flags[i] |= MEMBERS_REACH_OVERRIDE;
          break;
        }
      }
    }
    return flags;
  }

  private TraversalFlags() {}
}
