/*
 * Copyright 2025 Google Inc.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import javax.annotation.Nullable;

/** Rewrites fluent Xplat logger calls to non-polymorphic log method calls. */
public class OptimizeXplatLogger extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public MethodCall rewriteMethodCall(MethodCall methodCall) {
            String methodName = getWellKnownLoggingApiMethodName(methodCall);
            return switch (methodName) {
              case "log", "isEnabled" -> getReplacementLogCall(methodName, methodCall);
              default -> methodCall;
            };
          }
        });
  }

  /**
   * Replace calls with the pattern logger.at<Level>().withCause(cause).log(...) with
   * logger.log_at<Level>(..., cause) where withCause is optional. And 'isEnabled' is also handled
   * similarly if there is no cause in the chain.
   */
  private static MethodCall getReplacementLogCall(String loggingApi, MethodCall methodCall) {
    Expression qualifier = methodCall.getQualifier();
    if (!(qualifier instanceof MethodCall qualifierMethodCall)) {
      return methodCall;
    }

    // Extract the cause from the chain if present.
    Expression cause = null;
    if (getWellKnownLoggingApiMethodName(qualifierMethodCall).equals("withCause")) {
      // Don't match/rewrite withCause().isEnabled() - only "log" helpers expect a cause and can
      // preserve its effects.
      if (loggingApi.equals("isEnabled")) {
        return methodCall;
      }

      if (!(qualifierMethodCall.getQualifier() instanceof MethodCall newQualifier)) {
        return methodCall;
      }

      cause = qualifierMethodCall.getArguments().getFirst();
      qualifierMethodCall = newQualifier;
    }

    // Find the replacement method and construct the call.

    var replacementMethodDescriptor =
        getReplacementMethodDescriptor(qualifierMethodCall, loggingApi);
    if (replacementMethodDescriptor == null) {
      return methodCall;
    }

    var replacementArguments = getReplacementArguments(methodCall, cause);

    // Make sure to match arguments in size (due to optional parameters).
    replacementMethodDescriptor =
        replacementMethodDescriptor.transform(
            builder ->
                builder.setParameterTypeDescriptors(
                    builder.getParameterTypeDescriptors().subList(0, replacementArguments.size())));

    return MethodCall.Builder.from(replacementMethodDescriptor)
        .setQualifier(qualifierMethodCall.getQualifier())
        .setArguments(replacementArguments)
        .build();
  }

  @Nullable
  private static MethodDescriptor getReplacementMethodDescriptor(
      MethodCall qualifierMethodCall, String loggerApi) {
    var target = qualifierMethodCall.getTarget();
    if (!isMemberOf(target, "com.google.apps.xplat.logging.XLogger")
        || !target.getName().startsWith("at")) {
      return null;
    }

    // Avoid re-writing calls to old Xplat logger by checking a well-known method in new one.
    var enclosingType = target.getEnclosingTypeDescriptor();
    if (enclosingType.getMethodDescriptorByName("log_atInfo") == null) {
      // TODO(b/435512074): Remove this once the old logger is removed.
      return null;
    }

    // By convention, replacement method should be provided as <loggerApi>_<atMethodName>.
    var replacementMethodName = loggerApi + "_" + target.getName();
    var replacementMethodDescriptor =
        enclosingType.getMethodDescriptorByName(replacementMethodName);
    checkNotNull(replacementMethodDescriptor, "Replacement not found: %s", replacementMethodName);
    return replacementMethodDescriptor;
  }

  private static ImmutableList<Expression> getReplacementArguments(
      MethodCall methodCall, Expression cause) {
    var builder = new ImmutableList.Builder<Expression>();
    builder.addAll(methodCall.getArguments());

    if (cause != null) {
      // Having a cause implies that this is 'log' (vs 'isEnabled' which was filtered out earlier).
      checkState(methodCall.getTarget().getName().equals("log"));

      // To add the cause, need to ensure args argument is added as it was optional.
      if (methodCall.getArguments().size() == 1) {
        builder.add(TypeDescriptors.get().javaLangObjectArray.getNullValue());
      }

      builder.add(cause);
    }

    return builder.build();
  }

  private static String getWellKnownLoggingApiMethodName(MethodCall methodCall) {
    if (!isMemberOf(methodCall.getTarget(), "com.google.apps.xplat.logging.LoggingApi")) {
      return "<unknown>";
    }

    return methodCall.getTarget().getName();
  }

  private static boolean isMemberOf(MethodDescriptor memberDescriptor, String qualifiedSourceName) {
    return memberDescriptor
        .getEnclosingTypeDescriptor()
        .getQualifiedSourceName()
        .equals(qualifiedSourceName);
  }
}
