/*
 * Copyright 2015 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.Type;
import java.util.List;

/** Synthesizes instance initialization method $init and adds calls to them in each constructor. */
public class ImplementInstanceInitialization extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    if (type.getInstanceInitializerBlocks().isEmpty()) {
      return;
    }
    implementInitMethod(type);
    insertInitCalls(type);
  }

  /** Implements the instance initialization method. */
  private void implementInitMethod(Type type) {
    checkArgument(!type.isInterface());
    List<Statement> statements =
        type.getInstanceInitializerBlocks().stream()
            .flatMap(initializerBlock -> initializerBlock.getBlock().getStatements().stream())
            .collect(toImmutableList());

    type.addMember(
        Method.newBuilder()
            .setMethodDescriptor(type.getTypeDescriptor().getInitMethodDescriptor())
            .addStatements(statements)
            .setSourcePosition(type.getSourcePosition())
            .build());

    type.getMembers().removeIf(member -> member.isInitializerBlock() && !member.isStatic());
  }

  /** Inserts init calls in each constructor */
  private void insertInitCalls(Type type) {
    for (Method constructor : type.getConstructors()) {
      if (AstUtils.hasThisCall(constructor)) {
        // A constructor with this() call does not need $init call.
        continue;
      }
      synthesizeInstanceInitCall(constructor);
    }
  }

  private static void synthesizeInstanceInitCall(Method constructor) {
    DeclaredTypeDescriptor enclosingTypeDescriptor =
        constructor.getDescriptor().getEnclosingTypeDescriptor();
    MethodDescriptor initMethodDescriptor = enclosingTypeDescriptor.getInitMethodDescriptor();

    SourcePosition sourcePosition = constructor.getBody().getSourcePosition();

    List<Statement> constructorStatements = constructor.getBody().getStatements();
    // If the constructor has a super() call, insert $init call after it. Otherwise, insert
    // to the top of the method body.
    int insertIndex =
        constructorStatements.indexOf(AstUtils.getConstructorInvocationStatement(constructor)) + 1;

    constructorStatements.add(
        insertIndex,
        MethodCall.Builder.from(initMethodDescriptor)
            .setDefaultInstanceQualifier()
            .build()
            .makeStatement(sourcePosition));
  }
}
