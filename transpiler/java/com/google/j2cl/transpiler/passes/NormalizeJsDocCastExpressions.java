/*
 * Copyright 2016 Google Inc.
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Rewrites casts to {@code @type {Foo<?>}} where {@code ? extends Bar} as casts to {@code @type
 * {Foo<Bar>}} to avoid "unknown type" errors; and replaces out of scope type variables with
 * wildcards.
 */
public class NormalizeJsDocCastExpressions extends NormalizationPass {
  private final SetMultimap<Member, TypeVariable> typeVariablesByMember = HashMultimap.create();

  @Override
  public void applyTo(CompilationUnit compilationUnit) {

    // Collect all type variables that are in context in a given member.
    compilationUnit.accept(
        new AbstractVisitor() {
          private final Deque<Set<TypeVariable>> typeVariablesInContext = new ArrayDeque<>();

          {
            typeVariablesInContext.add(new HashSet<>());
          }

          @Override
          public boolean enterType(Type type) {
            TypeDeclaration typeDeclaration = type.getDeclaration();
            Set<TypeVariable> typeVariables =
                new HashSet<>(typeDeclaration.getTypeParameterDescriptors());
            if (typeDeclaration.isCapturingEnclosingInstance()) {
              typeVariables.addAll(typeVariablesInContext.peek());
            }
            typeVariablesInContext.push(typeVariables);
            return true;
          }

          @Override
          public void exitType(Type type) {
            typeVariablesInContext.pop();
          }

          @Override
          public boolean enterMember(Member member) {
            pushEnclosingTypeVariables(member);
            return true;
          }

          private void pushEnclosingTypeVariables(Member member) {
            Set<TypeVariable> enclosingTypeVariables = new HashSet<>();
            if (!member.isStatic()) {
              // Add type variables from enclosing context.
              enclosingTypeVariables.addAll(typeVariablesInContext.peek());
            }
            typeVariablesInContext.push(enclosingTypeVariables);
          }

          @Override
          public boolean enterMethod(Method method) {
            pushEnclosingTypeVariables(method);
            typeVariablesInContext
                .peek()
                .addAll(method.getDescriptor().getTypeParameterTypeDescriptors());
            return true;
          }

          @Override
          public void exitMember(Member member) {
            typeVariablesByMember.putAll(member, typeVariablesInContext.pop());
          }
        });

    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteJsDocCastExpression(JsDocCastExpression jsDocCastExpression) {
            TypeDescriptor castTypeDescriptor = jsDocCastExpression.getTypeDescriptor();
            if (jsDocCastExpression.getTypeDescriptor() instanceof DeclaredTypeDescriptor) {

              DeclaredTypeDescriptor castDeclaredTypeDescriptor =
                  (DeclaredTypeDescriptor) jsDocCastExpression.getTypeDescriptor();

              // If it is a parameterized type first replace wildcards with their bound top
              // avoid "unknown type" errors.
              if (castDeclaredTypeDescriptor.hasTypeArguments()) {
                castTypeDescriptor =
                    DeclaredTypeDescriptor.Builder.from(castDeclaredTypeDescriptor)
                        .setTypeArgumentDescriptors(
                            castDeclaredTypeDescriptor.getTypeArgumentDescriptors().stream()
                                .map(NormalizeJsDocCastExpressions::replaceWildcardWithBound)
                                .collect(toImmutableList()))
                        .build();
              }
            }

            // Replace out of bounds type variables that might have been left by the frontend
            // if the inferrence was not needed for Java compilation.
            return JsDocCastExpression.Builder.from(jsDocCastExpression)
                .setCastType(
                    castTypeDescriptor.specializeTypeVariables(
                        typeVariable ->
                            replaceOutofScopeTypeVariable(getCurrentMember(), typeVariable)))
                .build();
          }
        });
  }

  /** Replaces wildcards with their bound. */
  private static TypeDescriptor replaceWildcardWithBound(TypeDescriptor typeDescriptor) {
    if (!(typeDescriptor instanceof TypeVariable)) {
      return typeDescriptor;
    }
    TypeVariable typeVariable = (TypeVariable) typeDescriptor;
    return typeVariable.isWildcardOrCapture() ? typeDescriptor.toRawTypeDescriptor() : typeVariable;
  }

  /** Replaces out of scope variables by a wilcard. */
  private TypeDescriptor replaceOutofScopeTypeVariable(Member member, TypeVariable typeVariable) {
    return typeVariable.isWildcardOrCapture()
            || typeVariablesByMember.containsEntry(member, typeVariable)
        ? typeVariable
        : TypeVariable.createWildcard();
  }
}
