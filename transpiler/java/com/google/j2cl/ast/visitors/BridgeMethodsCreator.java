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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Collections2;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks circumstances where a bridge method should be generated and creates the bridge methods.
 */
public class BridgeMethodsCreator extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    // TODO(b/64280462): Emit the bridge as high in the class hierarchy as possible including
    //  abstract classes. That would reduce the number bridges created and also enable JavaScript
    //  to extend a class and inherit all the necessary bridges.

    if (type.getDeclaration().isAbstract()
        && !TypeDescriptors.isBoxedTypeAsJsPrimitives(type.getTypeDescriptor())) {
      return;
    }

    type.addMethods(createBridgeMethods(type));
  }

  /** Creates and adds bridge methods to the Java type and fixes the target JS methods. */
  private static Collection<Method> createBridgeMethods(Type type) {
    Set<String> usedMangledNames = new HashSet<>();
    List<Method> bridgeMethods = new ArrayList<>();

    // Plan bridge methods.
    Map<MethodDescriptor, MethodDescriptor> targetMethodDescriptorsByBridgeMethodDescriptor =
        findTargetMethodDescriptorsByBridgeMethodDescriptor(type.getDeclaration());

    // Create bridge methods.
    for (Map.Entry<MethodDescriptor, MethodDescriptor> entry :
        targetMethodDescriptorsByBridgeMethodDescriptor.entrySet()) {
      MethodDescriptor bridgeMethodDescriptor = entry.getKey();
      MethodDescriptor targetMethodDescriptor = entry.getValue();

      Method bridgeMethod =
          createBridgeMethod(type, bridgeMethodDescriptor, targetMethodDescriptor);

      if (!usedMangledNames.add(bridgeMethod.getDescriptor().getMangledName())) {
        // TODO(b/64280462): there should not be any ambiguity here depending on the order in which
        //  the bridges are created.
        // Do not generate duplicate bridge methods in one class.
        continue;
      }

      bridgeMethods.add(bridgeMethod);
    }

    return bridgeMethods;
  }

  /** Returns the mappings from the needed bridge method to the targeted method. */
  private static Map<MethodDescriptor, MethodDescriptor>
      findTargetMethodDescriptorsByBridgeMethodDescriptor(TypeDeclaration typeDeclaration) {
    // Do not create bridge methods in interfaces.
    if (typeDeclaration.isInterface()) {
      return new LinkedHashMap<>();
    }

    Map<MethodDescriptor, MethodDescriptor> targetMethodDescriptorByBridgeMethodDescriptor =
        new LinkedHashMap<>();

    for (MethodDescriptor potentialBridgeMethodDescriptor :
        getPotentialBridgeMethodDescriptors(typeDeclaration.toUnparameterizedTypeDescriptor())) {
      // Attempt to target a concrete method on the prototype chain.
      MethodDescriptor targetMethodDescriptor =
          findForwardingMethodDescriptor(
              potentialBridgeMethodDescriptor,
              typeDeclaration.toUnparameterizedTypeDescriptor(),
              false /* findDefaultMethods */);
      if (targetMethodDescriptor != null) {
        recordBridgeMethodNeeded(
            targetMethodDescriptorByBridgeMethodDescriptor,
            potentialBridgeMethodDescriptor,
            targetMethodDescriptor);
        continue;
      }

      // Failing that, attempt to target an accidental override, but of course ensure that the
      // target is concrete.
      if (!potentialBridgeMethodDescriptor.isAbstract()) {
        MethodDescriptor backwardingMethodDescriptor =
            findBridgeDueToAccidentalOverride(potentialBridgeMethodDescriptor, typeDeclaration);
        if (backwardingMethodDescriptor != null) {
          recordBridgeMethodNeeded(
              targetMethodDescriptorByBridgeMethodDescriptor,
              backwardingMethodDescriptor,
              potentialBridgeMethodDescriptor);
          continue;
        }
      }

      // Lastly, attempt to route to a default method.
      MethodDescriptor targetDefaultMethodDescriptor =
          findForwardingMethodDescriptor(
              potentialBridgeMethodDescriptor,
              typeDeclaration.toUnparameterizedTypeDescriptor(),
              true /* findDefaultMethods */);
      if (targetDefaultMethodDescriptor != null) {
        targetMethodDescriptorByBridgeMethodDescriptor.put(
            potentialBridgeMethodDescriptor, targetDefaultMethodDescriptor);
        continue;
      }
    }
    return targetMethodDescriptorByBridgeMethodDescriptor;
  }

  private static void recordBridgeMethodNeeded(
      Map<MethodDescriptor, MethodDescriptor> targetMethodDescriptorByBridgeMethodDescriptor,
      MethodDescriptor bridgeMethod,
      MethodDescriptor targetMethod) {
    if (!bridgeMethod.isJsMethod() || !targetMethod.isJsMethod()) {
      // Only create a bridge if the actual JavaScript signatures are different. If both
      // methods are JsMethod then they will have the same name, hence no need for bridges.
      targetMethodDescriptorByBridgeMethodDescriptor.put(bridgeMethod, targetMethod);
    }
  }

  /**
   * Returns all the potential methods in the super classes and super interfaces that may need a
   * bridge method in {@code type}.
   *
   * <p>A bridge method is needed in a type when the type extends or implements a parameterized
   * class or interface and type erasure changes the signature of any inherited method or when an
   * implemented interface requires a simple method that happens to collide with the signature of an
   * existing method being specialized in the current type. This inherited method is a potential
   * method that may need a bridge method.
   */
  private static List<MethodDescriptor> getPotentialBridgeMethodDescriptors(
      DeclaredTypeDescriptor typeDescriptor) {
    List<MethodDescriptor> declaredPotentialBridgeMethodDescriptors =
        getDeclaredPotentialBridgeMethodDescriptors(typeDescriptor);

    Set<String> shadowedSignatures = new HashSet<>();

    // Add signatures that are shadowed by potential bridge method descriptors which are generic
    // specializations.
    for (MethodDescriptor methodDescriptor : declaredPotentialBridgeMethodDescriptors) {
      shadowedSignatures.add(methodDescriptor.getOverrideSignature());
    }

    // Remove any that are concretely implemented in the current class.
    for (MethodDescriptor declaredMethodDescriptor :
        typeDescriptor.getDeclaredMethodDescriptors()) {
      shadowedSignatures.remove(declaredMethodDescriptor.getOverrideSignature());
    }

    // Then add any bridge method descriptors which are required by implemented interfaces but
    // which were shadowed by one of the generic specialization bridge method descriptors.
    for (DeclaredTypeDescriptor interfaceTypeDescriptor :
        typeDescriptor.getTypeDeclaration().getTransitiveInterfaceTypeDescriptors()) {
      for (MethodDescriptor methodDescriptor : interfaceTypeDescriptor.getMethodDescriptors()) {
        if (shadowedSignatures.contains(methodDescriptor.getOverrideSignature())) {
          declaredPotentialBridgeMethodDescriptors.add(methodDescriptor);
        }
      }
    }

    return declaredPotentialBridgeMethodDescriptors;
  }

  private static List<MethodDescriptor> getDeclaredPotentialBridgeMethodDescriptors(
      DeclaredTypeDescriptor typeDescriptor) {
    List<MethodDescriptor> potentialBridgeMethodDescriptors = new ArrayList<>();
    DeclaredTypeDescriptor superTypeDescriptor = typeDescriptor.getSuperTypeDescriptor();
    if (superTypeDescriptor != null) {
      // add the potential bridge methods from direct super class.
      potentialBridgeMethodDescriptors.addAll(
          getPotentialBridgeMethodDescriptorsInType(superTypeDescriptor));
      // recurse into super class.
      potentialBridgeMethodDescriptors.addAll(
          getDeclaredPotentialBridgeMethodDescriptors(superTypeDescriptor));
    }
    for (DeclaredTypeDescriptor superInterface : typeDescriptor.getInterfaceTypeDescriptors()) {
      // add the potential bridge methods from direct super interfaces.
      potentialBridgeMethodDescriptors.addAll(
          getPotentialBridgeMethodDescriptorsInType(superInterface));
      // recurse into super interfaces.
      potentialBridgeMethodDescriptors.addAll(
          getDeclaredPotentialBridgeMethodDescriptors(superInterface));
    }
    return potentialBridgeMethodDescriptors;
  }

  /** Returns the potential methods in {@code type} that may need a bridge method. */
  private static Collection<MethodDescriptor> getPotentialBridgeMethodDescriptorsInType(
      DeclaredTypeDescriptor typeDescriptor) {
    return Collections2.filter(
        typeDescriptor.getDeclaredMethodDescriptors(),
        /**
         * If {@code method}, the method with more specific type arguments, has different method
         * signature with {@code method.getMethodDeclaration()}, the original generic method, it
         * means this method is a potential method that may need a bridge method.
         */
        methodDescriptor ->
            methodDescriptor.isPolymorphic()
                && !methodDescriptor.isSynthetic()
                // is a parameterized method.
                && methodDescriptor != methodDescriptor.getDeclarationDescriptor()
                // type erasure changes the signature
                && !methodDescriptor.isJsOverride(methodDescriptor.getDeclarationDescriptor()));
  }

  /**
   * Returns the targeted method (implemented or inherited) by {@code type} that {@code
   * bridgeMethod} should be targeted to.
   *
   * <p>If a method (a method with more specific type arguments) in {@code type} or in its super
   * types has the same erasured signature with {@code bridgeMethod}, it is an overriding method for
   * {@code bridgeMethod}. And if their original method declarations are different then a bridge
   * method is needed to make overriding work.
   */
  private static MethodDescriptor findForwardingMethodDescriptor(
      MethodDescriptor bridgeMethodDescriptor,
      DeclaredTypeDescriptor typeDescriptor,
      boolean findDefaultMethods) {
    for (MethodDescriptor declaredMethodDescriptor :
        typeDescriptor.getDeclaredMethodDescriptors()) {
      if (declaredMethodDescriptor.isDefaultMethod() == findDefaultMethods
          && !declaredMethodDescriptor.equals(bridgeMethodDescriptor) // should not target itself
          && !declaredMethodDescriptor.isAbstract() // should be a concrete implementation.
          // concrete methods have the same signature, thus an overriding.
          && declaredMethodDescriptor.isJsOverride(bridgeMethodDescriptor)
          // original method declarations have different signatures
          && !declaredMethodDescriptor
              .getDeclarationDescriptor()
              .isJsOverride(bridgeMethodDescriptor.getDeclarationDescriptor())) {
        // find a overriding method (also possible accidental overriding), this is the method that
        // should be targeted.
        return declaredMethodDescriptor;
      }
    }

    // recurse to super class.
    if (typeDescriptor.getSuperTypeDescriptor() != null) {
      MethodDescriptor targetMethodDescriptor =
          findForwardingMethodDescriptor(
              bridgeMethodDescriptor, typeDescriptor.getSuperTypeDescriptor(), findDefaultMethods);
      if (targetMethodDescriptor != null) {
        return targetMethodDescriptor;
      }
    }

    if (findDefaultMethods) {
      // recurse to super interfaces.
      for (DeclaredTypeDescriptor interfaceTypeDescriptor :
          typeDescriptor.getInterfaceTypeDescriptors()) {
        MethodDescriptor targetMethodDescriptor =
            findForwardingMethodDescriptor(
                bridgeMethodDescriptor, interfaceTypeDescriptor, findDefaultMethods);
        if (targetMethodDescriptor != null) {
          return targetMethodDescriptor;
        }
      }
    }

    return null;
  }

  /**
   * Returns the method in its super interface that needs a bridge method delegating to {@code
   * bridgeMethod}.
   *
   * <p>If a method in the super interfaces of {@code type} is a method with more specific type
   * arguments, and it is overridden by a generic method, it needs a bridge method that delegates to
   * the generic method.
   */
  private static MethodDescriptor findBridgeDueToAccidentalOverride(
      MethodDescriptor bridgeMethodDescriptor, TypeDeclaration typeDeclaration) {
    for (DeclaredTypeDescriptor superInterface :
        typeDeclaration.getTransitiveInterfaceTypeDescriptors()) {
      for (MethodDescriptor methodDescriptor : superInterface.getDeclaredMethodDescriptors()) {
        if (methodDescriptor == methodDescriptor.getDeclarationDescriptor() // non-generic method,
            // generic method has been investigated by findForwardingMethod.
            && methodDescriptor.isJsOverride(bridgeMethodDescriptor)
            // is overridden by a generic method with different erasure parameter types.
            && !methodDescriptor.isJsOverride(bridgeMethodDescriptor.getDeclarationDescriptor())) {
          return methodDescriptor;
        }
      }
    }
    return null;
  }

  /**
   * Returns bridge method that calls the targeted method in its body.
   *
   * @param type where the bridge method needs to be created.
   * @param causeMethod the method from a super type causing the need for a bridge.
   * @param targetMethod the method that would handle the the call.
   */
  private static Method createBridgeMethod(
      Type type, MethodDescriptor causeMethod, MethodDescriptor targetMethod) {
    TypeDeclaration typeDeclaration = type.getDeclaration();

    MethodDescriptor bridgeMethodDescriptor =
        createBridgeMethodDescriptor(typeDeclaration, causeMethod);
    targetMethod = adjustTargetForJsFunction(bridgeMethodDescriptor, targetMethod);

    List<Variable> parameters = new ArrayList<>();
    List<Expression> arguments = new ArrayList<>();

    for (int i = 0; i < bridgeMethodDescriptor.getParameterTypeDescriptors().size(); i++) {
      Variable parameter =
          Variable.newBuilder()
              .setName("arg" + i)
              // Set the type for the parameter variables in the AST from the declaration
              // of the causing methods (using its raw type to potentially remove type variables
              // that are not in context here). This is done to support the runtime type check
              // cast, because in the cases where the method is called from a supertype, the actual
              // objects passed are only guaranteed to be of the type declared by the method in the
              // super class.
              // And If we were to declare the parameter with the more specific type, the runtime
              // type check cast that is inserted below would be considered redundant are removed at
              // a later stage.
              .setTypeDescriptor(
                  causeMethod
                      .getDeclarationDescriptor()
                      .getParameterTypeDescriptors()
                      .get(i)
                      .toRawTypeDescriptor())
              .setParameter(true)
              .build();

      parameters.add(parameter);
      Expression parameterReference = parameter.getReference();

      // If the parameter type in bridge method is different from the one in the method
      // that will handle the call, add a cast to perform the runtime type check.
      TypeDescriptor castToParameterTypeDescriptor =
          targetMethod.getParameterTypeDescriptors().get(i);
      Expression argument =
          causeMethod
                  .getDeclarationDescriptor()
                  .getParameterTypeDescriptors()
                  .get(i)
                  .hasSameRawType(castToParameterTypeDescriptor)
              ? parameterReference
              : CastExpression.newBuilder()
                  .setExpression(parameterReference)
                  .setCastTypeDescriptor(castToParameterTypeDescriptor)
                  .build();
      arguments.add(argument);
    }
    DeclaredTypeDescriptor targetEnclosingTypeDescriptor =
        targetMethod.getEnclosingTypeDescriptor();
    Expression qualifier =
        bridgeMethodDescriptor.isMemberOf(targetEnclosingTypeDescriptor)
                || targetEnclosingTypeDescriptor.isInterface()
            ? new ThisReference(targetEnclosingTypeDescriptor)
            : new SuperReference(targetEnclosingTypeDescriptor);

    Expression dispatchMethodCall =
        MethodCall.Builder.from(targetMethod)
            .setQualifier(qualifier)
            .setArguments(arguments)
            .build();

    checkArgument(bridgeMethodDescriptor.isSynthetic());
    checkArgument(bridgeMethodDescriptor.isBridge());

    // TODO(b/31312257): fix or decide to not emit @override and suppress the error.
    // Determine whether the method needs @override annotation.
    boolean isOverride = AstUtils.overrideNeedsAtOverrideAnnotation(causeMethod);
    return Method.newBuilder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setParameters(parameters)
        .addStatements(
            AstUtils.createReturnOrExpressionStatement(
                type.getSourcePosition(),
                dispatchMethodCall,
                bridgeMethodDescriptor.getReturnTypeDescriptor()))
        .setOverride(isOverride)
        .setJsDocDescription("Bridge method.")
        .setSourcePosition(type.getSourcePosition())
        .build();
  }

  private static MethodDescriptor adjustTargetForJsFunction(
      MethodDescriptor causeMethod, MethodDescriptor targetMethod) {
    // The MethodDescriptor of the targeted method.
    MethodDescriptor.Builder methodDescriptorBuilder = MethodDescriptor.Builder.from(targetMethod);

    // If a JsFunction method needs a bridge, only the bridge method is a JsFunction method, and it
    // targets to *real* implementation, which is not a JsFunction method.
    // If both a method and the bridge method are JsMethod, only the bridge method is a JsMethod,
    // and it targets the *real* implementation, which should be emit as non-JsMethod.
    if (causeMethod.isJsMethod() && targetMethod.inSameTypeAs(causeMethod)) {
      methodDescriptorBuilder.removeParameterOptionality().setJsInfo(JsInfo.NONE);
    }

    return methodDescriptorBuilder.setJsFunction(false).build();
  }

  private static MethodDescriptor createBridgeMethodDescriptor(
      TypeDeclaration typeDeclaration, MethodDescriptor bridgeMethodDescriptor) {

    MethodDescriptor bridgeOrigin = bridgeMethodDescriptor.getDeclarationDescriptor();

    return MethodDescriptor.Builder.from(bridgeMethodDescriptor)
        .setEnclosingTypeDescriptor(typeDeclaration.toUnparameterizedTypeDescriptor())
        .setBridge(bridgeOrigin)
        .build();
  }
}
