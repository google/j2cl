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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.ast.PrimitiveTypes.INT;
import static com.google.j2cl.transpiler.ast.TypeVariable.createWildcardWithUpperBound;
import static com.google.j2cl.transpiler.passes.FixJavaKotlinCollectionMethodsMismatch.MethodMapping.methodMapping;
import static com.google.j2cl.transpiler.passes.FixJavaKotlinCollectionMethodsMismatch.ParameterMapping.parameterMapping;

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
 */
public class FixJavaKotlinCollectionMethodsMismatch extends NormalizationPass {

  private final TypeDescriptors types = TypeDescriptors.get();

  private final DeclaredTypeDescriptor object = types.javaLangObject;
  private final DeclaredTypeDescriptor collection = types.javaUtilCollection;
  private final DeclaredTypeDescriptor map = types.javaUtilMap;
  private final DeclaredTypeDescriptor list = types.javaUtilList;

  private final DeclaredTypeDescriptor readonlyCollection =
      checkNotNull(types.javaUtilReadonlyCollection);
  private final DeclaredTypeDescriptor readonlyMap = checkNotNull(types.javaUtilReadonlyMap);

  private final TypeVariable collectionElement = typeParameter(collection, 0);
  private final TypeVariable listElement = typeParameter(list, 0);
  private final TypeVariable mapKey = typeParameter(map, 0);
  private final TypeVariable mapValue = typeParameter(map, 1);

  private final TypeDescriptor readonlyCollectionOfElements =
      readonlyCollection.withTypeArguments(ImmutableList.of(collectionElement)).toNonNullable();

  private final TypeDescriptor readonlyMapOfWildcardKeysAndValues =
      readonlyMap
          .withTypeArguments(ImmutableList.of(createWildcardWithUpperBound(mapKey), mapValue))
          .toNonNullable();

  private final ImmutableList<MethodMapping> methodMappings =
      ImmutableList.of(
          methodMapping(
              collection, "addAll", parameterMapping(collection, readonlyCollectionOfElements)),
          methodMapping(collection, "contains", parameterMapping(object, collectionElement)),
          methodMapping(collection, "remove", parameterMapping(object, collectionElement)),
          methodMapping(
              collection,
              "containsAll",
              parameterMapping(collection, readonlyCollectionOfElements)),
          methodMapping(
              collection, "removeAll", parameterMapping(collection, readonlyCollectionOfElements)),
          methodMapping(
              collection, "retainAll", parameterMapping(collection, readonlyCollectionOfElements)),
          methodMapping(map, "containsKey", parameterMapping(object, mapKey)),
          methodMapping(map, "containsValue", parameterMapping(object, mapValue)),
          methodMapping(map, "get", parameterMapping(object, mapKey)),
          // TODO(b/364506629): This one needs return type conversion from V to V?
          // methodMapping(
          //     map,
          //     "getOrDefault",
          //     parameterMapping(object, mapKey),
          //     parameterMapping(mapValue, mapValue)),
          methodMapping(map, "putAll", parameterMapping(map, readonlyMapOfWildcardKeysAndValues)),
          methodMapping(map, "remove", parameterMapping(object, mapKey)),
          methodMapping(
              map, "remove", parameterMapping(object, mapKey), parameterMapping(object, mapValue)),
          methodMapping(
              list,
              "addAll",
              parameterMapping(INT, INT),
              parameterMapping(collection, readonlyCollectionOfElements)),
          methodMapping(list, "indexOf", parameterMapping(object, listElement)),
          methodMapping(list, "lastIndexOf", parameterMapping(object, listElement)));

  private static TypeVariable typeParameter(DeclaredTypeDescriptor typeDescriptor, int index) {
    return typeDescriptor.getTypeDeclaration().getTypeParameterDescriptors().get(index);
  }

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
            MethodMapping methodMapping =
                findMethodMapping(methodCall.getTarget().getDeclarationDescriptor());
            if (methodMapping == null) {
              return methodCall;
            }
            return methodMapping.bridgeOrInsertCastIfNeeded(methodCall);
          }
        });
  }

  /** A mapping from Java parameter signature to Kotlin parameter type. */
  @AutoValue
  abstract static class ParameterMapping {
    static ParameterMapping parameterMapping(
        TypeDescriptor javaRawTypeDescriptor, TypeDescriptor kotlinTypeDescriptor) {
      return new AutoValue_FixJavaKotlinCollectionMethodsMismatch_ParameterMapping(
          javaRawTypeDescriptor, kotlinTypeDescriptor);
    }

    abstract TypeDescriptor getJavaSignatureTypeDescriptor();

    abstract TypeDescriptor getKotlinTypeDescriptor();
  }

  /** A mapping from Java method to Kotlin method. */
  @AutoValue
  abstract static class MethodMapping {
    static MethodMapping methodMapping(
        DeclaredTypeDescriptor enclosingTypeDescriptor,
        String methodName,
        ParameterMapping... parameterTypeMappings) {
      ImmutableList<ParameterMapping> parameterTypeMappingsList =
          ImmutableList.copyOf(parameterTypeMappings);
      ImmutableList<TypeDescriptor> parameterSignatureTypeDescriptors =
          parameterTypeMappingsList.stream()
              .map(ParameterMapping::getJavaSignatureTypeDescriptor)
              .collect(toImmutableList());
      MethodDescriptor javaMethodDescriptor =
          enclosingTypeDescriptor.getMethodDescriptor(
              methodName, parameterSignatureTypeDescriptors.toArray(new TypeDescriptor[] {}));
      ImmutableList<TypeDescriptor> kotlinParameterTypeDescriptors =
          parameterTypeMappingsList.stream()
              .map(ParameterMapping::getKotlinTypeDescriptor)
              .collect(toImmutableList());
      MethodDescriptor kotlinMethodDescriptor =
          MethodDescriptor.Builder.from(javaMethodDescriptor)
              .updateParameterTypeDescriptors(kotlinParameterTypeDescriptors)
              .build();
      return new AutoValue_FixJavaKotlinCollectionMethodsMismatch_MethodMapping(
          javaMethodDescriptor, kotlinMethodDescriptor);
    }

    abstract MethodDescriptor getJavaMethodDescriptor();

    abstract MethodDescriptor getKotlinMethodDescriptor();

    final String getBridgeMethodName() {
      return "java_" + getJavaMethodDescriptor().getName();
    }

    final boolean isOrOverrides(MethodDescriptor methodDescriptor) {
      return methodDescriptor.equals(getJavaMethodDescriptor())
          || (methodDescriptor
                  .getEnclosingTypeDescriptor()
                  .isSubtypeOf(getJavaMethodDescriptor().getEnclosingTypeDescriptor())
              && methodDescriptor.isOverride(getJavaMethodDescriptor()));
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
        TypeDescriptor javaDeclarationParameterTypeDescriptor =
            getJavaMethodDescriptor().getParameterTypeDescriptors().get(i);
        TypeDescriptor kotlinDeclarationParameterTypeDescriptor =
            getKotlinMethodDescriptor().getParameterTypeDescriptors().get(i);
        if (!javaDeclarationParameterTypeDescriptor.equals(
            kotlinDeclarationParameterTypeDescriptor)) {
          TypeDescriptor kotlinParameterTypeDescriptor =
              kotlinDeclarationParameterTypeDescriptor.specializeTypeVariables(parameterization);
          parameterTypeDescriptors.set(i, kotlinParameterTypeDescriptor);

          // Create a new parameter with the type expected by kotlin overrides and move the old
          // parameter variable to local declaration to avoid rewriting all uses.
          Variable parameter = method.getParameters().get(i);
          TypeDescriptor parameterTypeDescriptor = parameter.getTypeDescriptor();

          Variable kotlinParameter =
              Variable.Builder.from(parameter)
                  .setTypeDescriptor(kotlinParameterTypeDescriptor)
                  .build();
          method.getParameters().set(i, kotlinParameter);

          Expression initializer = kotlinParameter.createReference();

          // Insert cast from read-only to mutable if necessary.
          if (!kotlinParameterTypeDescriptor.isAssignableTo(parameterTypeDescriptor)) {
            initializer =
                CastExpression.newBuilder()
                    .setExpression(initializer)
                    .setCastTypeDescriptor(parameterTypeDescriptor)
                    .build();
          }

          Statement declarationStatement =
              VariableDeclarationExpression.newBuilder()
                  .addVariableDeclaration(parameter, initializer)
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
      ImmutableList<TypeDescriptor> javaParameterTypeDescriptors =
          getJavaMethodDescriptor().getParameterTypeDescriptors();
      ImmutableList<TypeDescriptor> kotlinParameterTypeDescriptors =
          getKotlinMethodDescriptor().getParameterTypeDescriptors();
      MethodDescriptor methodDescriptor = methodCall.getTarget();
      Expression qualifier = methodCall.getQualifier();

      Map<TypeVariable, TypeDescriptor> parameterization =
          methodDescriptor.getEnclosingTypeDescriptor().getParameterization();

      if (qualifier instanceof SuperReference) {
        if (javaParameterTypeDescriptors.equals(kotlinParameterTypeDescriptors)) {
          return methodCall;
        }

        List<Expression> arguments = methodCall.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
          arguments.set(
              i,
              insertCastIfNeeded(
                  arguments.get(i),
                  javaParameterTypeDescriptors.get(i),
                  kotlinParameterTypeDescriptors.get(i),
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
      // TODO(b/380235439): Use generic-aware isAssignableTo() when it's implemented.
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
        .filter(it -> it.isOrOverrides(methodDescriptor))
        .findFirst()
        .orElse(null);
  }
}
