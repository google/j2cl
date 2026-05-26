/*
 * Copyright 2026 Google Inc.
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
import static com.google.common.base.Preconditions.checkState;
import static com.google.j2cl.transpiler.passes.PromoteMutability.ReturnRewriteKind.MUTABLE_ENTRYSET_REWRITE;
import static com.google.j2cl.transpiler.passes.PromoteMutability.ReturnRewriteKind.MUTABLE_REWRITE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;
import javax.annotation.Nullable;

/**
 * The Kotlin backend will typically translate Java JRE collection interface types to their
 * read-only Kotlin equivalents.
 *
 * <p>This pass promotes them to their mutable Kotlin equivalents in situations where using
 * read-only collections would not be legal.
 */
public class PromoteMutability extends AbstractJ2ktNormalizationPass {

  private final TypeDescriptors types = TypeDescriptors.get();

  private final ImmutableSet<MethodDescriptor> mutationMethods;
  private final ImmutableMap<TypeDeclaration, MethodDescriptor> asMutableMethodsByTypeDeclaration;
  private final ImmutableMap<MethodDescriptor, ReturnRewriteKind> overrideReturnMappings;

  public PromoteMutability() {
    ImmutableSet.Builder<MethodDescriptor> mutationMethodsBuilder = ImmutableSet.builder();
    ImmutableMap.Builder<TypeDeclaration, MethodDescriptor> asMutableMethodsBuilder =
        ImmutableMap.builder();

    // Collection
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilCollection,
        "asMutableCollection",
        "add",
        "addAll",
        "remove",
        "removeAll",
        "removeIf",
        "retainAll",
        "clear");

    // List
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilList,
        "asMutableList",
        "add",
        "addAll",
        "set",
        "sort",
        "remove",
        "removeFirst",
        "removeLast",
        "replaceAll",
        "addFirst",
        "addLast");

    // Set
    addMutatingMethods(
        asMutableMethodsBuilder, mutationMethodsBuilder, types.javaUtilSet, "asMutableSet");

    // Map
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilMap,
        "asMutableMap",
        "put",
        "putAll",
        "putIfAbsent",
        "remove",
        "replace",
        "replaceAll",
        "clear",
        "compute",
        "computeIfAbsent",
        "computeIfPresent",
        "merge");

    // Iterator
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilIterator,
        "asMutableIterator",
        "remove");

    // ListIterator
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilListIterator,
        "asMutableListIterator",
        "add",
        "set");

    // Map.Entry
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilMapEntry,
        "asMutableEntry",
        "setValue");

    this.mutationMethods = mutationMethodsBuilder.build();
    this.asMutableMethodsByTypeDeclaration = asMutableMethodsBuilder.buildOrThrow();

    ImmutableMap.Builder<MethodDescriptor, ReturnRewriteKind> returnMappingsBuilder =
        ImmutableMap.builder();

    addReturnRewrite(returnMappingsBuilder, types.javaLangIterable, "iterator", MUTABLE_REWRITE);
    addReturnRewrite(returnMappingsBuilder, types.javaUtilList, "subList", MUTABLE_REWRITE);
    addReturnRewrite(returnMappingsBuilder, types.javaUtilList, "listIterator", MUTABLE_REWRITE);
    addReturnRewrite(returnMappingsBuilder, types.javaUtilMap, "keySet", MUTABLE_REWRITE);
    addReturnRewrite(returnMappingsBuilder, types.javaUtilMap, "values", MUTABLE_REWRITE);
    addReturnRewrite(
        returnMappingsBuilder, types.javaUtilMap, "entrySet", MUTABLE_ENTRYSET_REWRITE);

    this.overrideReturnMappings = returnMappingsBuilder.buildOrThrow();
  }

  /**
   * Registers the converter method to obtain a mutable view of a collection type, and the methods
   * that mutate that collection type.
   *
   * <p>For a given collection type (e.g., {@code Collection}), this associates the type's
   * declaration with its corresponding {@code asMutable...} converter method (e.g., {@code
   * asMutableCollection}) in {@code asMutableMethodsBuilder}.
   *
   * <p>It also registers all declared methods with the specified names (including all overloads) as
   * mutating methods in {@code mutationMethodsBuilder}. Calls to these mutating methods on
   * read-only collection types will trigger mutability promotion by inserting a call to the
   * registered converter method.
   */
  private static void addMutatingMethods(
      ImmutableMap.Builder<TypeDeclaration, MethodDescriptor> asMutableMethodsBuilder,
      ImmutableSet.Builder<MethodDescriptor> mutationMethodsBuilder,
      DeclaredTypeDescriptor type,
      String asMutableMethodName,
      String... mutatingMethodNames) {
    MethodDescriptor asMutableMethod = type.getMethodDescriptorByName(asMutableMethodName);
    asMutableMethodsBuilder.put(type.getTypeDeclaration(), asMutableMethod);
    ImmutableSet<String> names = ImmutableSet.copyOf(mutatingMethodNames);

    for (MethodDescriptor method : type.getTypeDeclaration().getDeclaredMethodDescriptors()) {
      if (names.contains(method.getName())) {
        mutationMethodsBuilder.add(method.getDeclarationDescriptor());
      }
    }
  }

  /**
   * Registers a return rewrite mapping for all declared methods with the specified name (i.e. all
   * overloads with that name) in the enclosing type.
   *
   * <p>These mappings are used to rewrite return types of overridden methods to ensure they return
   * mutable types when implemented in mutable collection types (e.g., {@code subList} in {@code
   * MutableList} must return {@code MutableList}) when they would otherwise return read-only
   * collection types.
   */
  private static void addReturnRewrite(
      ImmutableMap.Builder<MethodDescriptor, ReturnRewriteKind> builder,
      DeclaredTypeDescriptor enclosingType,
      String methodName,
      ReturnRewriteKind kind) {
    for (MethodDescriptor method :
        enclosingType.getTypeDeclaration().getDeclaredMethodDescriptors()) {
      if (method.getName().equals(methodName)) {
        builder.put(method.getDeclarationDescriptor(), kind);
      }
    }
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    rewriteMutationCalls(compilationUnit);
    rewriteLowerBoundedCollectionExpressions(compilationUnit);
    rewriteOverrideReturnStatements(compilationUnit);
  }

  private void rewriteMutationCalls(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor targetMethod = methodCall.getTarget();
            if (!isCollectionMutationMethod(targetMethod)) {
              return methodCall;
            }

            TypeDescriptor qualifierType = methodCall.getQualifier().getTypeDescriptor();
            MethodDescriptor asMutableMethod = getAsMutableMethodIfReadonly(qualifierType);
            if (asMutableMethod == null) {
              // Qualifier type was already mutable, no need to insert asMutable() call.
              return methodCall;
            }

            MethodCall asMutableCall =
                MethodCall.builderFrom(asMutableMethod)
                    .setQualifier(methodCall.getQualifier())
                    .build();

            return methodCall.toBuilder().setQualifier(asMutableCall).build();
          }
        });
  }

  /**
   * Returns whether a collection type that would normally become readonly in Kotlin has to be
   * mutable due to the use of lower-bounded type parameters (the "List<in T> is not legal in
   * Kotlin" problem)
   */
  private boolean isMutableDueToVariance(TypeDescriptor typeDescriptor) {
    if (typeDescriptor == null) {
      return false;
    }

    if (typeDescriptor instanceof DeclaredTypeDescriptor declaredTypeDescriptor) {
      if (!asMutableMethodsByTypeDeclaration.containsKey(
          declaredTypeDescriptor.getTypeDeclaration())) {
        // Not a base collection interface (non-collection type or a collection subtype which is
        // always mutable), no need to worry about variance-based mutability
        return false;
      }

      ImmutableList<TypeDescriptor> typeArguments =
          declaredTypeDescriptor.getTypeArgumentDescriptors();
      if (typeArguments.isEmpty()) {
        return false;
      }
      // TODO - b/502503059: Annotate type parameters with @KtOut and remove index-based logic.
      // In Kotlin, all type parameters of the readonly collection interfaces have declaration-site
      // covariance (except the first ("Key") type parameter of Map). That means they cannot be
      // instantiated with a lower type-bound (=contravariant). So the Kotlin backend will never
      // render collection interface types with lower bounds as readonly, but always as mutable.
      int startIndex = declaredTypeDescriptor.isSameBaseType(types.javaUtilMap) ? 1 : 0;
      for (int i = startIndex; i < typeArguments.size(); i++) {
        TypeDescriptor arg = typeArguments.get(i);
        if (arg instanceof TypeVariable typeVariable
            && typeVariable.getLowerBoundTypeDescriptor() != null) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns whether the given method is a mutation method. i.e. cannot be called on readonly
   * collection types without first calling a converter function like asMutableList.
   */
  private boolean isCollectionMutationMethod(MethodDescriptor methodDescriptor) {
    if (mutationMethods.contains(methodDescriptor.getDeclarationDescriptor())) {
      return true;
    }
    return methodDescriptor.getJavaOverriddenMethodDescriptors().stream()
        .map(MethodDescriptor::getDeclarationDescriptor)
        .anyMatch(mutationMethods::contains);
  }

  private void rewriteLowerBoundedCollectionExpressions(CompilationUnit compilationUnit) {

    // java.util.List will generally be translated as kotlin.collection.List. But contravariant
    // lists must be MutableList in Kotlin, so the Kotlin transpiler backend will render those
    // as mutable.
    //
    // This pass ensure that code like the following results in legal Kotlin code:
    // List<Number> l1;
    // List<? super Integer> l2 = l1;
    //
    // Namely
    // val l1: List<Number>
    // val l2: MutableList<in Int> = l1.asMutableList()
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {

                if (!isMutableDueToVariance(declaredTypeDescriptor)) {
                  return expression;
                }

                TypeDescriptor typeDescriptor = expression.getTypeDescriptor();
                if (isMutableDueToVariance(typeDescriptor)) {
                  return expression;
                }

                MethodDescriptor asMutableMethod = getAsMutableMethodIfReadonly(typeDescriptor);
                if (asMutableMethod == null) {
                  return expression;
                }

                return MethodCall.builderFrom(asMutableMethod).setQualifier(expression).build();
              }
            }));
  }

  private void rewriteOverrideReturnStatements(CompilationUnit compilationUnit) {

    // `java.util.List` will generally be translated as `kotlin.collection.List`. `implements List`
    // however will become `: MutableList` because all Java List implementations override mutations.
    //
    // Implementing MutableList forces us to rewrite the return values of some method overrides to
    // also be mutable, for example `subList` of `MutableList` must return `MutableList`.
    // Other collection interfaces also have such methods.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteReturnStatement(ReturnStatement returnStatement) {
            // Method_Like_ because we could be in a lambda implementing a collection interface.
            MethodLike methodLike = (MethodLike) getParent(MethodLike.class::isInstance);

            Expression expression = returnStatement.getExpression();
            if (expression == null) {
              return returnStatement;
            }

            MethodDescriptor methodDescriptor = methodLike.getDescriptor();
            ReturnRewriteKind kind = getReturnRewriteKind(methodDescriptor);
            if (kind == null) {
              return returnStatement;
            }

            if (kind == MUTABLE_ENTRYSET_REWRITE) {
              expression = castEntrySetToMutableEntrySet(expression);
            }

            MethodDescriptor asMutableMethod =
                getAsMutableMethodIfReadonly(expression.getTypeDescriptor());
            if (asMutableMethod != null) {
              expression = MethodCall.builderFrom(asMutableMethod).setQualifier(expression).build();
            }

            return ReturnStatement.builder()
                .setSourcePosition(returnStatement.getSourcePosition())
                .setExpression(expression)
                .build();
          }
        });
  }

  /**
   * Cast {@code Set<Entry<K,V>>} to {@code Set<MutableEntry<K,V>>}. Does not cast the outer Set
   * type but preserves it.
   */
  private Expression castEntrySetToMutableEntrySet(Expression expression) {
    TypeDescriptor expressionType = expression.getTypeDescriptor();

    checkState(
        expressionType.isAssignableTo(types.javaUtilSet), "Expected Set, got: %s", expressionType);

    if (!(expressionType instanceof DeclaredTypeDescriptor declaredExpressionType)) {
      return expression;
    }

    // Look for a supertype where the Entry type is a type parameter. So
    // MyCustomSet<Entry<K,V>> but not MyCustomEntrySet<K,V>. We want to insert a cast to
    // MyCustomSet<MutableEntry<K,V>>. We can't do that with MyCustomEntrySet<K,V>.
    DeclaredTypeDescriptor targetType =
        declaredExpressionType.getAllSuperTypesIncludingSelf().stream()
            .filter(t -> t.isAssignableTo(types.javaUtilSet))
            .filter(t -> t.getTypeArgumentDescriptors().size() == 1)
            .filter(
                t -> t.getTypeArgumentDescriptors().get(0).isSameBaseType(types.javaUtilMapEntry))
            .findFirst()
            .orElse(null);

    checkNotNull(
        targetType,
        "Expected to find at least Set<Entry<K,V>> as a supertype of entrySet() return"
            + " expression.");

    ImmutableList<TypeDescriptor> typeArguments = targetType.getTypeArgumentDescriptors();
    TypeDescriptor typeArgument = typeArguments.get(0);
    DeclaredTypeDescriptor entryType = (DeclaredTypeDescriptor) typeArgument;

    TypeDescriptor mutableEntryType =
        types
            .javaUtilMutableMapMutableEntry
            .withTypeArguments(entryType.getTypeArgumentDescriptors())
            .toNullable(entryType.isNullable());

    DeclaredTypeDescriptor newExpressionType =
        targetType.withTypeArguments(ImmutableList.of(mutableEntryType));

    return CastExpression.builder()
        .setExpression(expression)
        .setCastTypeDescriptor(newExpressionType)
        .build();
  }

  @Nullable
  private ReturnRewriteKind getReturnRewriteKind(MethodDescriptor methodDescriptor) {
    if (!methodDescriptor.isPolymorphic()) {
      return null;
    }
    ReturnRewriteKind kind =
        overrideReturnMappings.get(methodDescriptor.getDeclarationDescriptor());
    if (kind != null) {
      return kind;
    }
    for (MethodDescriptor overridden : methodDescriptor.getJavaOverriddenMethodDescriptors()) {
      kind = overrideReturnMappings.get(overridden.getDeclarationDescriptor());
      if (kind != null) {
        return kind;
      }
    }
    return null;
  }

  /**
   * Returns the asMutable() method for a type if it is a collection base interface that will be
   * translated as a readonly Kotlin type, null otherwise.
   */
  @Nullable
  private MethodDescriptor getAsMutableMethodIfReadonly(TypeDescriptor type) {
    while (type instanceof TypeVariable typeVariable) {
      // Treat `T extends List<...>` like `List<...>`. Other type variables are not considered
      // readonly.
      type = typeVariable.getUpperBoundTypeDescriptor();
      if (type == null) {
        return null;
      }
    }
    if (isMutableDueToVariance(type)) {
      // The type is not readonly due to contravariance (List<in T> does not exist in Kotlin so
      // the transpiler backend will render the type as mutable).
      return null;
    }
    if (type instanceof DeclaredTypeDescriptor declaredTypeDescriptor) {
      MethodDescriptor asMutableMethod =
          asMutableMethodsByTypeDeclaration.get(declaredTypeDescriptor.getTypeDeclaration());
      if (asMutableMethod != null) {
        return asMutableMethod.specializeTypeVariables(
            declaredTypeDescriptor.getParameterization());
      }
    }
    return null;
  }

  enum ReturnRewriteKind {
    MUTABLE_REWRITE,
    MUTABLE_ENTRYSET_REWRITE
  }
}
