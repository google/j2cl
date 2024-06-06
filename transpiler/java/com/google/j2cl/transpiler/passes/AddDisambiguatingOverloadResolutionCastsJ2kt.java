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
package com.google.j2cl.transpiler.passes;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.function.Predicate.isEqual;

import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import java.util.stream.Stream;

/**
 * Adds casts to invocation arguments where Java/Kotlin would resolve the target to different
 * methods.
 *
 * <p>For Java overload resolution is defined in JLS §15.12.2 and for Kotlin in the Kotlin
 * Specification §11.4.2. To summarize: Kotlin always prefers the most specific overload it can
 * utilize, whereas Java resolves overloads in three phases (first phase to match wins). In
 * particular, Java will first consider any methods without permitting boxing/unboxing, widening, or
 * varargs. Then it will consider only disallowing varargs. Finally, it will consider all options.
 *
 * <p>An example of this mismatch would occur for a contract of the form:
 *
 * <pre>{@code
 * void m(Object o) { ... }
 * void m(String s, Object... o) { ... }
 * }</pre>
 *
 * Given a caller {@code m("foo")}, Java would resolve this to the first method as it matched in its
 * first overload resolution phase. However, the equivalent Kotlin code would resolve to the latter
 * method as it's a more specific overload.
 *
 * <p>Without any handling the behavior would change in J2KT output as the target method would be
 * different between Java and Kotlin. To avoid this we'll insert casts to the parameter types that
 * of the target method that Java resolved to. For the previous example this would become: {@code
 * m((Object) "m")}.
 */
public class AddDisambiguatingOverloadResolutionCastsJ2kt extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteInvocation(Invocation invocation) {
            if (!hasAmbiguousOverload(invocation)) {
              return invocation;
            }
            // If it's a potentially ambiguous call, explicitly cast every argument to the
            // parameter's type.
            return Invocation.Builder.from(invocation)
                .setArguments(invocation)
                .setArguments(
                    Streams.zip(
                            invocation.getTarget().getParameterTypeDescriptors().stream(),
                            invocation.getArguments().stream(),
                            AddDisambiguatingOverloadResolutionCastsJ2kt
                                ::castArgumentToParameterType)
                        .collect(toImmutableList()))
                .build();
          }
        });
  }

  private static Expression castArgumentToParameterType(
      TypeDescriptor parameterTypeDescriptor, Expression argument) {
    if (!parameterTypeDescriptor.isDenotable()) {
      return argument;
    }
    return CastExpression.newBuilder()
        .setCastTypeDescriptor(parameterTypeDescriptor)
        .setExpression(argument)
        .build();
  }

  private static boolean hasAmbiguousOverload(Invocation invocation) {
    MethodDescriptor target = invocation.getTarget();
    // Exit early if there are no arguments or if the target method already accepts varargs. Java
    // always considers variable arity last so it shouldn't be ambiguous if it was chosen.
    if (target.isVarargs() || invocation.getArguments().isEmpty()) {
      return false;
    }

    var enclosingType = target.getEnclosingTypeDescriptor();
    var candidateMethods =
        target.isInstanceMember()
            ? enclosingType.getPolymorphicMethods()
            : enclosingType.getDeclaredMethodDescriptors();

    // We're not worried about matching ourselves here as the existing target doesn't have varargs
    // and we require a matching one to have varargs.
    return candidateMethods.stream()
        .anyMatch(candidateMethod -> isVarargAndMatches(candidateMethod, invocation));
  }

  /**
   * Returns true if the given method descriptor has varargs and could be an overload for the given
   * invocation.
   *
   * <p>We're somewhat lenient here (ex. not checking visibility) as the failure mode is only that
   * we introduce additional casts.
   */
  private static boolean isVarargAndMatches(
      MethodDescriptor methodDescriptor, Invocation invocation) {
    // We only care about overloads that accept varargs so it must have the same name.
    if (!methodDescriptor.isVarargs()
        || !methodDescriptor.getName().equals(invocation.getTarget().getName())) {
      return false;
    }

    int numArguments = invocation.getArguments().size();
    int numNonVarArgParams = methodDescriptor.getParameterDescriptors().size() - 1;
    // The number of arguments must at least consume all the non-vararg params.
    if (numArguments < numNonVarArgParams) {
      return false;
    }

    final TypeDescriptor varArgComponentType =
        ((ArrayTypeDescriptor)
                methodDescriptor.getParameterTypeDescriptors().get(numNonVarArgParams))
            .getComponentTypeDescriptor();
    // A stream of parameter types that has an equal length to the number of arguments. The trailing
    // vararg parameter type will be repeated as needed.
    Stream<TypeDescriptor> flattenedParameterTypes =
        Stream.concat(
            methodDescriptor.getParameterTypeDescriptors().stream().limit(numNonVarArgParams),
            Stream.generate(() -> varArgComponentType).limit(numArguments - numNonVarArgParams));

    // Check that all arguments are assignable to their corresponding parameter types.
    return Streams.zip(
            flattenedParameterTypes,
            invocation.getArguments().stream(),
            (parameterTypeDescriptor, argument) ->
                argument.getTypeDescriptor().isAssignableTo(parameterTypeDescriptor))
        .allMatch(isEqual(true));
  }
}
