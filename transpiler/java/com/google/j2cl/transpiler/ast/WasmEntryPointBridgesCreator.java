/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.EntryPointPattern;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Generates forwarding methods for Wasm entry points. The forwarding methods are then exported
 * (instead of the original entry points). The forwarding methods perform necessary conversions
 * between {@code java.lang.String} and Wasm strings.
 */
// TODO(b/482402363) This helper class can be removed after updating test infra and moving the
// traversal logic into BazelJ2wasmExportsGenerator. Currently, this common logic is shared between
// the external pipeline and the test-specific one so that the test is valid.
public class WasmEntryPointBridgesCreator {
  private final ImmutableList<EntryPointPattern> entryPointPatterns;
  private final Set<EntryPointPattern> unmatchedEntryPointPatterns;
  private final Set<String> exportedMethodNames;
  private final Problems problems;

  public WasmEntryPointBridgesCreator(
      ImmutableList<EntryPointPattern> entryPointPatterns, Problems problems) {
    this.entryPointPatterns = entryPointPatterns;
    this.unmatchedEntryPointPatterns = new HashSet<>(entryPointPatterns);
    this.exportedMethodNames = new HashSet<>();
    this.problems = problems;
  }

  public void validateEntryPoints(Library library) {
    library
        .streamTypes()
        .forEach(
            type -> {
              for (Method method : type.getMethods()) {
                // isEntryPoint also checks that the regular expressions used to select entrypoints
                // are consistent.
                var unused = isEntryPoint(method.getDescriptor());
              }
            });

    checkForUnusedPatterns();
  }

  public ImmutableList<Method> generateBridges(List<MethodDescriptor> methodDescriptors) {
    ImmutableList<Method> bridges =
        methodDescriptors.stream()
            .map(this::generateBridge)
            .filter(notNull())
            .collect(toImmutableList());
    checkForUnusedPatterns();
    return bridges;
  }

  private void checkForUnusedPatterns() {
    for (EntryPointPattern unmatchedEntryPointPattern : unmatchedEntryPointPatterns) {
      problems.error(
          "No public static method matched the entry point string '%s'.",
          unmatchedEntryPointPattern.getEntryPointPatternString());
    }
  }

  @Nullable
  private Method generateBridge(MethodDescriptor methodDescriptor) {
    return generateBridge(methodDescriptor, SourcePosition.NONE);
  }

  @Nullable
  private Method generateBridge(MethodDescriptor methodDescriptor, SourcePosition sourcePosition) {
    if (!isEntryPoint(methodDescriptor)) {
      return null;
    }

    return WasmExportBridgesUtils.generateBridge(
        methodDescriptor, sourcePosition, MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_ENTRY_POINT);
  }

  private boolean isEntryPoint(MethodDescriptor methodDescriptor) {
    if (!methodDescriptor.isStatic()
        || !methodDescriptor.getVisibility().isPublic()
        || methodDescriptor.isSynthetic()) {
      return false;
    }

    // Only the first pattern matching the method will be added, so there will be log error if
    // patterns are duplicate
    for (EntryPointPattern entryPointPattern : entryPointPatterns) {
      if (entryPointPattern.matchesMethod(
          methodDescriptor.getEnclosingTypeDescriptor().getQualifiedSourceName(),
          methodDescriptor.getName())) {
        unmatchedEntryPointPatterns.remove(entryPointPattern);

        if (!exportedMethodNames.add(methodDescriptor.getName())) {
          problems.error(
              "More than one method are exported with the same name '%s'.",
              methodDescriptor.getName());
        }

        return true;
      }
    }

    return false;
  }
}
