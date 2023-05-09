/*
 * Copyright 2022 Google Inc.
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
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adds super forwarding stubs for cases in which default methods introduce ambiguity in kotlin. */
public class AddDisambiguatingSuperMethodForwardingStubs extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    if (!type.isClass() || type.isNative()) {
      return;
    }

    // Index relevant default methods by their signature, since method have to have the same
    // signature (considering their parameterized version) to be overrides.
    Map<String, MethodDescriptor> defaultMethodBySignature = new LinkedHashMap<>();
    for (DeclaredTypeDescriptor ifce : type.getTypeDescriptor().getInterfaceTypeDescriptors()) {
      // Consider only the default methods that are in an interface that is explicitly implemented
      // by this class (or a super interface of those) but not the interfaces that are declared
      // in the superclasses.
      // A conflict in this type can only be due to an interface that is implemented by the type.
      ifce.getPolymorphicMethods().stream()
          .filter(MethodDescriptor::isDefaultMethod)
          .forEach(m -> defaultMethodBySignature.putIfAbsent(m.getSignature(), m));
    }

    // Index the all the declared methods in these classes and super classes by their signature.
    // But keep only the closest to the type in question.
    Map<String, MethodDescriptor> classMethodsBySignature = new HashMap<>();
    // Collect all the methods defined in the class and super classes.
    addClassMethodsBySignature(type.getTypeDescriptor(), classMethodsBySignature);

    for (String signature : defaultMethodBySignature.keySet()) {
      // Check whether a default method is implemented by the class or one of its super classes.
      MethodDescriptor overrideInClass = classMethodsBySignature.get(signature);
      if (overrideInClass == null || overrideInClass.isMemberOf(type.getDeclaration())) {
        continue;
      }
      // Only if the method is implemented by a superclass it needs the disambiguating stub.
      // Note that this pass is blind to the intermediate stubs it introduces, but it does not
      // matter. If a stub was introduced in a super class, there will still be an implementation
      // higher in the hierarchy causing the stub in this class to be created as well.
      type.addMember(createDefaultDisambiguatingStub(type, overrideInClass));
    }
  }

  /**
   * Collects all the methods declared in this class and superclasses indexing them by their
   * signature.
   *
   * <p>Note that only the closest override is kept of each method, so if the method is declared in
   * the type being processed, that is the descriptor collected.
   */
  private void addClassMethodsBySignature(
      DeclaredTypeDescriptor typeDescriptor,
      Map<String, MethodDescriptor> classMethodsBySignature) {
    typeDescriptor.getDeclaredMethodDescriptors().stream()
        .filter(MethodDescriptor::isPolymorphic)
        // Skip package private methods here since if the program compiled in Java the class would
        // have inherited the default method.
        // TODO(b/204365899): The logic here needs to be revisited when the handling of visibility
        // is finalized.
        .filter(m -> !m.getVisibility().isPackagePrivate())
        .forEach(m -> classMethodsBySignature.putIfAbsent(m.getSignature(), m));
    if (typeDescriptor.getSuperTypeDescriptor() != null) {
      addClassMethodsBySignature(typeDescriptor.getSuperTypeDescriptor(), classMethodsBySignature);
    }
  }

  /** Returns disambiguating stub method that calls a method in the super class using super. */
  private static Method createDefaultDisambiguatingStub(Type type, MethodDescriptor targetMethod) {

    List<Variable> parameters =
        AstUtils.createParameterVariables(targetMethod.getParameterTypeDescriptors());

    ImmutableList<Expression> arguments =
        parameters.stream().map(Variable::createReference).collect(toImmutableList());

    Method.Builder builder =
        Method.newBuilder()
            .setMethodDescriptor(
                MethodDescriptor.Builder.from(targetMethod)
                    .setDeclarationDescriptor(null)
                    .setEnclosingTypeDescriptor(type.getTypeDescriptor())
                    .setNative(false)
                    .build())
            .setParameters(parameters)
            .setSourcePosition(type.getSourcePosition());

    // Even if the superclass method is abstract, that is the one that Java chooses, so in that
    // case create the an abstract stub.
    if (!targetMethod.isAbstract()) {
      builder.addStatements(
          AstUtils.createForwardingStatement(
              type.getSourcePosition(),
              new SuperReference(type.getTypeDescriptor()),
              targetMethod,
              false,
              arguments,
              targetMethod.getReturnTypeDescriptor()));
    }

    return builder.build();
  }
}
