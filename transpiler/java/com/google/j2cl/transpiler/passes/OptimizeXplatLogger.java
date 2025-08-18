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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.ArrayList;
import javax.annotation.Nullable;

/**
 * Rewrites fluent Xplat logger calls to non-polymorphic log method calls and make log messages
 * obfuscatable.
 */
public class OptimizeXplatLogger extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public MethodCall rewriteMethodCall(MethodCall methodCall) {
            String methodName = getWellKnownLoggingApiMethodName(methodCall);
            if (methodName.equals("log")) {
              rewriteLogMethodCallArguments(methodCall);
            }
            return methodCall;
          }
        });
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
   * Replace message parameter in log(message) with Util.$makeLogMessage(message) so they can be
   * intercepted by JsCompiler for obfuscation.
   */
  private static void rewriteLogMethodCallArguments(MethodCall methodCall) {
    var arguments = methodCall.getArguments();
    var messageArg = arguments.getFirst();
    arguments.set(0, RuntimeMethods.createUtilMethodCall("$makeLogMessage", messageArg));
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

    var replacementArguments = new ArrayList<>(methodCall.getArguments());

    // Add the cause to the arguments if present on the chain.
    if (getWellKnownLoggingApiMethodName(qualifierMethodCall).equals("withCause")) {
      // Don't match/rewrite withCause().isEnabled() - only "log" helpers expect a cause and can
      // preserve its side-effects.
      if (loggingApi.equals("isEnabled")) {
        return methodCall;
      }

      if (!(qualifierMethodCall.getQualifier() instanceof MethodCall newQualifier)) {
        return methodCall;
      }

      // To add the cause, need to ensure args argument is added since it is no longer optional.
      if (methodCall.getArguments().size() == 1) {
        replacementArguments.add(TypeDescriptors.get().javaLangObjectArray.getNullValue());
      }
      // Add the cause argument.
      replacementArguments.addAll(qualifierMethodCall.getArguments());

      qualifierMethodCall = newQualifier;
    }

    // Find the replacement method and construct the call.
    var replacementMethodDescriptor =
        getReplacementMethodDescriptor(
            qualifierMethodCall, loggingApi, replacementArguments.size());
    if (replacementMethodDescriptor == null) {
      return methodCall;
    }
    return MethodCall.Builder.from(replacementMethodDescriptor)
        .setQualifier(qualifierMethodCall.getQualifier())
        .setArguments(replacementArguments)
        .build();
  }

  @Nullable
  private static MethodDescriptor getReplacementMethodDescriptor(
      MethodCall qualifierMethodCall, String loggerApi, int argsCount) {
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

    // Make sure to match arguments in size (due to optional parameters).
    return replacementMethodDescriptor.transform(
        builder ->
            builder.setParameterTypeDescriptors(
                builder.getParameterTypeDescriptors().subList(0, argsCount)));
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
