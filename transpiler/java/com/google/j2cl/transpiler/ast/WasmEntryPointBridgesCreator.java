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
import com.google.common.collect.Streams;
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

    return generateBridge(methodDescriptor, sourcePosition, /* isEntryPoint= */ true);
  }

  /**
   * Generates a bridge method, intended to be exported, that defers to the specified method.
   *
   * @param isEntryPoint true if the bridge is an entry point, which is exported by a clause at the
   *     method declaration. If false, the bridge should be exported by other means, such as by
   *     custom descriptors {@code configureAll}.
   */
  public static Method generateBridge(
      MethodDescriptor methodDescriptor, SourcePosition sourcePosition, boolean isEntryPoint) {
    MethodDescriptor bridgeMethodDescriptor =
        createExportBridgeDescriptor(methodDescriptor, isEntryPoint);
    List<Variable> parameters =
        AstUtils.createParameterVariables(bridgeMethodDescriptor.getParameterTypeDescriptors());

    ImmutableList<Expression> arguments =
        Streams.zip(
                parameters.stream(),
                methodDescriptor.getParameterTypeDescriptors().stream(),
                WasmEntryPointBridgesCreator::convertArgumentIfNeeded)
            .collect(toImmutableList());

    TypeDescriptor returnType = methodDescriptor.getReturnTypeDescriptor();

    return Method.newBuilder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setWasmExportName(isEntryPoint ? methodDescriptor.getName() : null)
        .setParameters(parameters)
        .addStatements(
            convertReturnIfNeeded(
                AstUtils.createForwardingStatement(
                    sourcePosition,
                    /* qualifier= */ methodDescriptor.isStatic()
                        ? null
                        : new ThisReference(methodDescriptor.getEnclosingTypeDescriptor()),
                    methodDescriptor,
                    /* isStaticDispatch= */ methodDescriptor.isStatic(),
                    arguments,
                    returnType),
                returnType))
        .setJsDocDescription(isEntryPoint ? "Wasm entry point forwarding method." : null)
        .setSourcePosition(sourcePosition)
        .build();
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

  /** Creates the argument expression, containing any necessary conversions. */
  private static Expression convertArgumentIfNeeded(
      Variable parameter, TypeDescriptor targetArgumentDescriptor) {
    Expression reference = parameter.createReference();
    if (TypeDescriptors.isJavaLangString(targetArgumentDescriptor)) {
      return RuntimeMethods.createStringFromJsStringMethodCall(reference);
    }
    return reference;
  }

  private static Statement convertReturnIfNeeded(
      Statement statement, TypeDescriptor targetReturnTypeDescriptor) {
    if (TypeDescriptors.isJavaLangString(targetReturnTypeDescriptor)) {
      ReturnStatement returnStatement = (ReturnStatement) statement;
      return ReturnStatement.Builder.from(returnStatement)
          .setExpression(
              RuntimeMethods.createJsStringFromStringMethodCall(returnStatement.getExpression()))
          .build();
    }
    return statement;
  }

  private static MethodDescriptor createExportBridgeDescriptor(
      MethodDescriptor descriptor, boolean isEntryPoint) {
    MethodDescriptor.Builder builder =
        MethodDescriptor.Builder.from(descriptor)
            // TODO(b/487374903): Should no longer need to change the name once the origin is more
            // specific. The "$" prefix added here may also not be needed.
            .setName(
                (isEntryPoint ? "" : "$")
                    + descriptor.getName()
                    + (isEntryPoint ? "__$export" : "__$js_export"))
            .setReturnTypeDescriptor(
                replaceStringWithNativeString(descriptor.getReturnTypeDescriptor()))
            .updateParameterTypeDescriptors(
                descriptor.getParameterTypeDescriptors().stream()
                    .map(WasmEntryPointBridgesCreator::replaceStringWithNativeString)
                    .collect(toImmutableList()));
    if (!isEntryPoint) {
      // For JsInterop exports, copy the JsInfo and set an origin that can be referenced later to
      // build configuration data.
      builder
          .setOriginalJsInfo(
              descriptor.getJsInfo().toBuilder().setJsName(descriptor.getSimpleJsName()).build())
          .setOrigin(getExportBridgeOrigin(descriptor));
    }
    return builder.build();
  }

  private static MethodDescriptor.MethodOrigin getExportBridgeOrigin(MethodDescriptor descriptor) {
    return descriptor.getOrigin() == MethodDescriptor.MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR
        ? MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_CONSTRUCTOR_EXPORT
        : MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_EXPORT;
  }

  private static TypeDescriptor replaceStringWithNativeString(TypeDescriptor typeDescriptor) {
    if (TypeDescriptors.isJavaLangString(typeDescriptor)) {
      return TypeDescriptors.getNativeStringType().toNullable(typeDescriptor.isNullable());
    }
    return typeDescriptor;
  }
}
