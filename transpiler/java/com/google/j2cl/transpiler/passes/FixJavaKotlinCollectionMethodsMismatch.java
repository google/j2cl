/*
 * Copyright 2024 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.j2cl.transpiler.passes.FixJavaKotlinCollectionMethodsMismatch.MethodMapping.methodMapping;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Rewrites collection methods and method calls where Java method signature is different from
 * Kotlin.
 *
 * <p>TODO(b/364506629): At this moment it handles methods which differ on `Object` parameter, like:
 * {@code contains(Object)} -> {@code contains(T)}.
 */
public class FixJavaKotlinCollectionMethodsMismatch extends NormalizationPass {

  private final DeclaredTypeDescriptor object = TypeDescriptors.get().javaLangObject;
  private final DeclaredTypeDescriptor collection = TypeDescriptors.get().javaUtilCollection;
  private final DeclaredTypeDescriptor map = TypeDescriptors.get().javaUtilMap;
  private final DeclaredTypeDescriptor list = TypeDescriptors.get().javaUtilList;

  private final ImmutableList<MethodMapping> methodMappings =
      ImmutableList.of(
          methodMapping(
              collection.getMethodDescriptor("contains", object),
              collection.getTypeDeclaration().getTypeParameterDescriptors().get(0)),
          methodMapping(
              collection.getMethodDescriptor("remove", object),
              collection.getTypeDeclaration().getTypeParameterDescriptors().get(0)),
          methodMapping(
              map.getMethodDescriptor("containsKey", object),
              map.getTypeDeclaration().getTypeParameterDescriptors().get(0)),
          methodMapping(
              map.getMethodDescriptor("containsValue", object),
              map.getTypeDeclaration().getTypeParameterDescriptors().get(1)),
          methodMapping(
              map.getMethodDescriptor("get", object),
              map.getTypeDeclaration().getTypeParameterDescriptors().get(0)),
          methodMapping(
              map.getMethodDescriptor("remove", object),
              map.getTypeDeclaration().getTypeParameterDescriptors().get(0)),
          methodMapping(
              list.getMethodDescriptor("indexOf", object),
              list.getTypeDeclaration().getTypeParameterDescriptors().get(0)),
          methodMapping(
              list.getMethodDescriptor("lastIndexOf", object),
              list.getTypeDeclaration().getTypeParameterDescriptors().get(0)));

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            MethodMapping methodMapping = findMethodMapping(method.getDescriptor());
            if (methodMapping == null) {
              return method;
            }
            return methodMapping.fixMethodParameters(method);
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodMapping methodMapping = findMethodMapping(methodCall.getTarget());
            if (methodMapping == null) {
              return methodCall;
            }
            return methodMapping.bridgeOrInsertCastIfNeeded(methodCall);
          }
        });
  }

  /** A mapping from Java method to Kotlin method. */
  @AutoValue
  abstract static class MethodMapping {
    static MethodMapping methodMapping(
        MethodDescriptor javaMethodDescriptor, TypeDescriptor... kotlinParameterTypeDescriptors) {
      checkArgument(
          javaMethodDescriptor.getParameterTypeDescriptors().size()
              == kotlinParameterTypeDescriptors.length);
      return new AutoValue_FixJavaKotlinCollectionMethodsMismatch_MethodMapping(
          javaMethodDescriptor, ImmutableList.copyOf(kotlinParameterTypeDescriptors));
    }

    /** The original Java method descriptor. */
    abstract MethodDescriptor getJavaMethodDescriptor();

    /** Kotlin parameter type descriptors. */
    abstract ImmutableList<TypeDescriptor> getKotlinParameterTypeDescriptors();

    final String getBridgeMethodName() {
      return "java_" + getJavaMethodDescriptor().getName();
    }

    final boolean isOverride(MethodDescriptor methodDescriptor) {
      return methodDescriptor
              .getEnclosingTypeDescriptor()
              .isSubtypeOf(getJavaMethodDescriptor().getEnclosingTypeDescriptor())
          && methodDescriptor.isOverride(getJavaMethodDescriptor());
    }

    /**
     * Fixes parameters in {@code method} to match Kotlin, and rewrites the body of the method to
     * introduce local variables with original types.
     */
    final Method fixMethodParameters(Method method) {
      MethodDescriptor methodDescriptor = method.getDescriptor();

      Map<TypeVariable, TypeDescriptor> parameterization =
          methodDescriptor.getEnclosingTypeDescriptor().getParameterization();

      List<TypeDescriptor> parameterTypeDescriptors =
          new ArrayList<>(methodDescriptor.getParameterTypeDescriptors());
      Block body = method.getBody();
      for (int i = 0; i < getJavaMethodDescriptor().getParameterTypeDescriptors().size(); i++) {
        TypeDescriptor javaParameterTypeDescriptor =
            getJavaMethodDescriptor().getParameterTypeDescriptors().get(i);
        TypeDescriptor kotlinParameterTypeDescriptor = getKotlinParameterTypeDescriptors().get(i);
        if (!javaParameterTypeDescriptor.equals(kotlinParameterTypeDescriptor)) {
          TypeDescriptor parameterTypeDescriptor =
              kotlinParameterTypeDescriptor.specializeTypeVariables(parameterization);
          parameterTypeDescriptors.set(i, parameterTypeDescriptor);

          // Create a new parameter with the type expected by kotlin overrides and move the old
          // parameter variable to local declaration to avoid rewriting all uses.
          Variable parameter = method.getParameters().get(i);
          Variable newParameter =
              Variable.Builder.from(parameter).setTypeDescriptor(parameterTypeDescriptor).build();
          method.getParameters().set(i, newParameter);
          Statement declarationStatement =
              VariableDeclarationExpression.newBuilder()
                  .addVariableDeclaration(parameter, newParameter.createReference())
                  .build()
                  .makeStatement(parameter.getSourcePosition());
          body =
              Block.Builder.from(body)
                  .setStatements()
                  .addStatement(declarationStatement)
                  .addStatements(body.getStatements())
                  .build();
        }
      }

      methodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor)
              .updateParameterTypeDescriptors(parameterTypeDescriptors)
              .build();

      return Method.Builder.from(method)
          .setMethodDescriptor(methodDescriptor)
          .setBody(body)
          .setForcedJavaOverride(true)
          .build();
    }

    /**
     * Fixes method call to match Kotlin signature. Ordinary method calls are delegated to "java_"
     * bridge extension functions, and explicit casts are inserted for arguments to super calls.
     */
    MethodCall bridgeOrInsertCastIfNeeded(MethodCall methodCall) {
      MethodDescriptor methodDescriptor = methodCall.getTarget();
      Expression qualifier = methodCall.getQualifier();

      Map<TypeVariable, TypeDescriptor> parameterization =
          methodDescriptor.getEnclosingTypeDescriptor().getParameterization();

      if (qualifier instanceof SuperReference) {
        List<Expression> arguments = methodCall.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
          arguments.set(
              i,
              insertCastIfNeeded(
                  arguments.get(i),
                  getJavaMethodDescriptor().getParameterTypeDescriptors().get(i),
                  getKotlinParameterTypeDescriptors().get(i),
                  parameterization));
        }
        return methodCall;
      } else {
        methodDescriptor =
            methodDescriptor.transform(builder -> builder.setName(getBridgeMethodName()));
        return MethodCall.Builder.from(methodCall).setTarget(methodDescriptor).build();
      }
    }

    static Expression insertCastIfNeeded(
        Expression argument,
        TypeDescriptor javaParameterTypeDescriptor,
        TypeDescriptor kotlinParameterTypeDescriptor,
        Map<TypeVariable, TypeDescriptor> parameterization) {
      if (!javaParameterTypeDescriptor.equals(kotlinParameterTypeDescriptor)) {
        return CastExpression.newBuilder()
            .setExpression(argument)
            .setCastTypeDescriptor(
                kotlinParameterTypeDescriptor.specializeTypeVariables(parameterization))
            .build();
      }
      return argument;
    }
  }

  @Nullable
  MethodMapping findMethodMapping(MethodDescriptor methodDescriptor) {
    return methodMappings.stream()
        .filter(it -> it.isOverride(methodDescriptor))
        .findFirst()
        .orElse(null);
  }
}
