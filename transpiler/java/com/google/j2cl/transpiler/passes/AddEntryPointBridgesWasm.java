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
package com.google.j2cl.transpiler.passes;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.j2cl.common.EntryPointPattern;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates forwarding methods for Wasm entry points. The forwarding methods are then exported
 * (instead of the original entry points). The forwarding methods perform necessary conversions
 * between {@code java.lang.String} and Wasm strings.
 */
public class AddEntryPointBridgesWasm extends LibraryNormalizationPass {

  private final ImmutableList<EntryPointPattern> entryPointPatterns;
  private final Set<EntryPointPattern> unmatchedEntryPointPatterns;
  private final Set<String> exportedMethodNames;

  public AddEntryPointBridgesWasm(ImmutableList<EntryPointPattern> entryPointPatterns) {
    this.entryPointPatterns = entryPointPatterns;
    this.unmatchedEntryPointPatterns = new HashSet<>(entryPointPatterns);
    this.exportedMethodNames = new HashSet<>();
  }

  @Override
  public void applyTo(Library library) {
    library
        .streamTypes()
        .forEach(
            type -> {
              for (Method method : type.getMethods()) {
                MethodDescriptor methodDescriptor = method.getDescriptor();
                if (!isEntryPoint(methodDescriptor)) {
                  continue;
                }

                Method exportBridgeMethod = createExportBridgeMethod(method);
                if (!exportedMethodNames.add(exportBridgeMethod.getWasmExportName())) {
                  getProblems()
                      .error(
                          "More than one method are exported with the same name '%s'.",
                          exportBridgeMethod.getWasmExportName());
                }
                type.addMember(exportBridgeMethod);
              }
            });

    for (EntryPointPattern unmatchedEntryPointPattern : unmatchedEntryPointPatterns) {
      getProblems()
          .error(
              "No public static method matched the entry point string '%s'.",
              unmatchedEntryPointPattern.getEntryPointPatternString());
    }
  }

  private static Method createExportBridgeMethod(Method targetMethod) {
    MethodDescriptor targetMethodDescriptor = targetMethod.getDescriptor();
    MethodDescriptor bridgeMethodDescriptor =
        createExportBridgeDescriptor(targetMethod.getDescriptor());

    List<Variable> parameters =
        AstUtils.createParameterVariables(bridgeMethodDescriptor.getParameterTypeDescriptors());

    ImmutableList<Expression> arguments =
        Streams.zip(
                parameters.stream(),
                targetMethodDescriptor.getParameterTypeDescriptors().stream(),
                AddEntryPointBridgesWasm::convertArgumentIfNeeded)
            .collect(toImmutableList());

    TypeDescriptor returnType = targetMethodDescriptor.getReturnTypeDescriptor();

    return Method.newBuilder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setWasmExportName(targetMethodDescriptor.getName())
        .setParameters(parameters)
        .addStatements(
            convertReturnIfNeeded(
                AstUtils.createForwardingStatement(
                    targetMethod.getSourcePosition(),
                    /* qualifier= */ null,
                    targetMethodDescriptor,
                    /* isStaticDispatch= */ true,
                    arguments,
                    returnType),
                returnType))
        .setJsDocDescription("Wasm entry point forwarding method.")
        .setSourcePosition(targetMethod.getSourcePosition())
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

  private static MethodDescriptor createExportBridgeDescriptor(MethodDescriptor descriptor) {
    return MethodDescriptor.Builder.from(descriptor)
        .setName(descriptor.getName() + "__$export")
        .setReturnTypeDescriptor(
            replaceStringWithNativeString(descriptor.getReturnTypeDescriptor()))
        .updateParameterTypeDescriptors(
            descriptor.getParameterTypeDescriptors().stream()
                .map(x -> replaceStringWithNativeString(x))
                .collect(toImmutableList()))
        .build();
  }

  private static TypeDescriptor replaceStringWithNativeString(TypeDescriptor typeDescriptor) {
    if (TypeDescriptors.isJavaLangString(typeDescriptor)) {
      return TypeDescriptors.getNativeStringType().toNullable(typeDescriptor.isNullable());
    }
    return typeDescriptor;
  }
}
