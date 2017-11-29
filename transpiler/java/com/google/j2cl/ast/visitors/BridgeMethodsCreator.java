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
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.ManglingNameUtils;
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
  public void applyTo(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      // Don't bridge methods in abstract classes. Ideally we should emit and avoid re-emitting in
      // subclasses. That would reduce unnecessary bridges and make subclassing in JavaScript work
      // properly (b/64280462). In the meantime, we need bridges for boxed primitives hence that is
      // special cased here.
      if (type.getDeclaration().isAbstract()
          && !TypeDescriptors.isBoxedTypeAsJsPrimitives(type.getTypeDescriptor())) {
        continue;
      }

      type.addMethods(createBridgeMethods(type));
    }
  }

  /** Creates and adds bridge methods to the Java type and fixes the target JS methods. */
  private static Collection<Method> createBridgeMethods(Type type) {
    Set<String> usedMangledNames = new HashSet<>();
    Multimap<MethodDescriptor, Method> bridgeMethodsByTargetMethodDescriptor =
        LinkedHashMultimap.create();

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

      if (!usedMangledNames.add(ManglingNameUtils.getMangledName(bridgeMethod.getDescriptor()))) {
        // Do not generate duplicate bridge methods in one class.
        continue;
      }

      bridgeMethodsByTargetMethodDescriptor.put(
          targetMethodDescriptor.getDeclarationDescriptor(), bridgeMethod);
    }

    fixJsInfo(type, bridgeMethodsByTargetMethodDescriptor);
    return bridgeMethodsByTargetMethodDescriptor.values();
  }

  private static void fixJsInfo(
      Type type, final Multimap<MethodDescriptor, Method> bridgeMethodsByTargetMethodDescriptor) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            Collection<Method> bridgeJsMethods =
                bridgeMethodsByTargetMethodDescriptor
                    .get(methodDescriptor)
                    .stream()
                    .filter(bridgeMethod -> bridgeMethod.getDescriptor().isJsMethod())
                    .collect(toImmutableList());
            if (bridgeJsMethods.isEmpty()) {
              return method;
            }

            // Now that the bridge method is created (and marked JsMethod), "demote" to a plain
            // Java method by setting JsInfo to NONE and resetting parameter optionality.
            return Method.Builder.from(method)
                .setMethodDescriptor(
                    MethodDescriptor.Builder.from(methodDescriptor)
                        .setJsInfo(JsInfo.NONE)
                        .removeParameterOptionality()
                        .build())
                .build();
          }
        });
  }

  /** Returns the mappings from the needed bridge method to the targeted method. */
  private static Map<MethodDescriptor, MethodDescriptor>
      findTargetMethodDescriptorsByBridgeMethodDescriptor(TypeDeclaration typeDeclaration) {
    // Do not create bridge methods in interfaces.
    if (typeDeclaration.isInterface()) {
      return ImmutableMap.of();
    }

    Map<MethodDescriptor, MethodDescriptor> targetMethodDescriptorByBridgeMethodDescriptor =
        new LinkedHashMap<>();

    for (MethodDescriptor potentialBridgeMethodDescriptor :
        getPotentialBridgeMethodDescriptors(typeDeclaration.toUnparamterizedTypeDescriptor())) {
      // Attempt to target a concrete method on the prototype chain.
      MethodDescriptor targetMethodDescriptor =
          findForwardingMethodDescriptor(
              potentialBridgeMethodDescriptor,
              typeDeclaration.toUnparamterizedTypeDescriptor(),
              false /* findDefaultMethods */);
      if (targetMethodDescriptor != null) {
        targetMethodDescriptorByBridgeMethodDescriptor.put(
            potentialBridgeMethodDescriptor, targetMethodDescriptor);
        continue;
      }

      // Failing that, attempt to target an accidental override, but of course ensure that the
      // target is concrete.
      if (!potentialBridgeMethodDescriptor.isAbstract()) {
        MethodDescriptor backwardingMethodDescriptor =
            findBackwardingMethodDescriptor(potentialBridgeMethodDescriptor, typeDeclaration);
        if (backwardingMethodDescriptor != null) {
          targetMethodDescriptorByBridgeMethodDescriptor.put(
              backwardingMethodDescriptor, potentialBridgeMethodDescriptor);
          continue;
        }
      }

      // Lastly, attempt to route to a default method.
      MethodDescriptor targetDefaultMethodDescriptor =
          findForwardingMethodDescriptor(
              potentialBridgeMethodDescriptor,
              typeDeclaration.toUnparamterizedTypeDescriptor(),
              true /* findDefaultMethods */);
      if (targetDefaultMethodDescriptor != null) {
        targetMethodDescriptorByBridgeMethodDescriptor.put(
            potentialBridgeMethodDescriptor, targetDefaultMethodDescriptor);
        continue;
      }
    }
    return targetMethodDescriptorByBridgeMethodDescriptor;
  }

  /**
   * Returns all the potential methods in the super classes and super interfaces that may need a
   * bridge method generating in {@code type}.
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
            !methodDescriptor.isConstructor()
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
  private static MethodDescriptor findBackwardingMethodDescriptor(
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
   * Creates MethodDescriptor in {@code type} that has the same signature as {@code
   * methodDescriptor} with return type of {@code returnType}.
   */
  private static MethodDescriptor createMethodDescriptor(
      TypeDeclaration typeDeclaration,
      MethodDescriptor originalMethodDescriptor,
      TypeDescriptor returnTypeDescriptor) {
    checkArgument(!typeDeclaration.isInterface());

    return MethodDescriptor.Builder.from(originalMethodDescriptor)
        .setEnclosingTypeDescriptor(typeDeclaration.toUnparamterizedTypeDescriptor())
        .setReturnTypeDescriptor(returnTypeDescriptor)
        .build();
  }

  /**
   * Returns bridge method that calls the targeted method in its body.
   *
   * <p>bridgeMethod: parameterized method with more specific type arguments.
   * bridgeMethod.getMethodDeclaration(): original declaration method. targetMethod: concrete
   * implementation that should be targeted.
   */
  private static Method createBridgeMethod(
      Type type, MethodDescriptor bridgeMethodDescriptor, MethodDescriptor targetMethodDescriptor) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    MethodDescriptor originalBridgeMethodDescriptor = bridgeMethodDescriptor;

    bridgeMethodDescriptor =
        tweakBridgeSignature(typeDeclaration, bridgeMethodDescriptor, targetMethodDescriptor);
    targetMethodDescriptor = tweakTargetSignature(bridgeMethodDescriptor, targetMethodDescriptor);

    List<Variable> parameters = new ArrayList<>();
    List<Expression> arguments = new ArrayList<>();

    MethodDescriptor declarationBridgeMethodDescriptor =
        bridgeMethodDescriptor.getDeclarationDescriptor();
    for (int i = 0; i < bridgeMethodDescriptor.getParameterTypeDescriptors().size(); i++) {
      Variable parameter =
          Variable.newBuilder()
              .setName("arg" + i)
              .setTypeDescriptor(bridgeMethodDescriptor.getParameterTypeDescriptors().get(i))
              .setParameter(true)
              .build();

      parameters.add(parameter);
      Expression parameterReference = parameter.getReference();

      // The type the argument should be casted to. It should be casted to the specific parameter
      // type that is expected by the concrete parameterized method.
      TypeDescriptor castToParameterTypeDescriptor =
          originalBridgeMethodDescriptor.getParameterTypeDescriptors().get(i);
      // if the parameter type in bridge method is different from that in parameterized method,
      // add a cast.
      Expression argument =
          declarationBridgeMethodDescriptor
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
        targetMethodDescriptor.getEnclosingTypeDescriptor();
    Expression qualifier =
        bridgeMethodDescriptor.isMemberOf(targetEnclosingTypeDescriptor)
                || targetEnclosingTypeDescriptor.isInterface()
            ? new ThisReference(targetEnclosingTypeDescriptor)
            : new SuperReference(targetEnclosingTypeDescriptor);

    Expression dispatchMethodCall =
        MethodCall.Builder.from(targetMethodDescriptor)
            .setQualifier(qualifier)
            .setArguments(arguments)
            .build();

    checkArgument(bridgeMethodDescriptor.isSynthetic());
    checkArgument(bridgeMethodDescriptor.isBridge());

    // TODO(b/31312257): fix or decide to not emit @override and suppress the error.
    // Determine whether the method needs @override annotation.
    boolean isOverride = AstUtils.overrideNeedsAtOverrideAnnotation(originalBridgeMethodDescriptor);
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

  private static MethodDescriptor tweakTargetSignature(
      MethodDescriptor bridgeMethodDescriptor, MethodDescriptor targetMethodDescriptor) {
    // The MethodDescriptor of the targeted method.
    JsInfo targetMethodJsInfo = targetMethodDescriptor.getJsInfo();
    MethodDescriptor.Builder methodDescriptorBuilder =
        MethodDescriptor.Builder.from(targetMethodDescriptor);

    // If a JsFunction method needs a bridge, only the bridge method is a JsFunction method, and it
    // targets to *real* implementation, which is not a JsFunction method.
    // If both a method and the bridge method are JsMethod, only the bridge method is a JsMethod,
    // and it targets the *real* implementation, which should be emit as non-JsMethod.
    if (bridgeMethodDescriptor.isJsMethod()
        && targetMethodDescriptor.inSameTypeAs(bridgeMethodDescriptor)) {
      targetMethodJsInfo = JsInfo.NONE;
      methodDescriptorBuilder.removeParameterOptionality();
    }

    return methodDescriptorBuilder.setJsInfo(targetMethodJsInfo).setJsFunction(false).build();
  }

  private static MethodDescriptor tweakBridgeSignature(
      TypeDeclaration typeDeclaration,
      MethodDescriptor bridgeMethodDescriptor,
      MethodDescriptor targetMethodDescriptor) {
    // The MethodDescriptor of the generated bridge method should have the same signature as the
    // original declared method.
    // Using the return type of the targeted method also avoids generating redundant bridge methods
    // for two methods that have the same parameter signature but different return types.
    TypeDescriptor returnTypeDescriptor =
        bridgeMethodDescriptor == bridgeMethodDescriptor.getDeclarationDescriptor()
            ? bridgeMethodDescriptor
                .getReturnTypeDescriptor() // use its own return type if it is a concrete method
            : targetMethodDescriptor
                .getReturnTypeDescriptor(); // otherwise use the return type of the target method.

    bridgeMethodDescriptor =
        MethodDescriptor.Builder.from(
                createMethodDescriptor(
                    typeDeclaration, bridgeMethodDescriptor, returnTypeDescriptor))
            .setParameterDescriptors(
                bridgeMethodDescriptor
                    .getDeclarationDescriptor()
                    .getParameterDescriptors()
                    .stream()
                    .map(
                        p ->
                            p.toBuilder()
                                .setTypeDescriptor(p.getTypeDescriptor().toRawTypeDescriptor())
                                .build())
                    .collect(toImmutableList()))
            .setSynthetic(true)
            .setBridge(true)
            .setAbstract(false)
            .setNative(false)
            .build();
    return bridgeMethodDescriptor;
  }
}
