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
package com.google.j2cl.frontend;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptorBuilder;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks circumstances where a bridge method should be generated and creates the bridge methods.
 */
public class BridgeMethodsCreator {
  /**
   * Returns generated bridge methods.
   */
  public static List<Method> create(ITypeBinding typeBinding) {
    BridgeMethodsCreator bridgeMethodsCreator = new BridgeMethodsCreator(typeBinding);
    List<Method> generatedBridgeMethods = new ArrayList<>();
    Set<MethodDescriptor> generatedBridgeMethodDescriptors = new HashSet<>();
    for (Map.Entry<IMethodBinding, IMethodBinding> entry :
        bridgeMethodsCreator.findBridgeMethods().entrySet()) { // bridgeMethod -> delegateMethod.
      Method bridgeMethod =
          bridgeMethodsCreator.createBridgeMethod(entry.getKey(), entry.getValue());
      if (generatedBridgeMethodDescriptors.contains(bridgeMethod.getDescriptor())) {
        // do not generate duplicate bridge methods in one class.
        continue;
      }
      generatedBridgeMethods.add(bridgeMethod);
      generatedBridgeMethodDescriptors.add(bridgeMethod.getDescriptor());
    }
    return generatedBridgeMethods;
  }

  private final ITypeBinding typeBinding;

  private BridgeMethodsCreator(ITypeBinding typeBinding) {
    this.typeBinding = typeBinding;
  }

  /**
   * Returns the mappings from the needed bridge method to the delegated method.
   */
  public Map<IMethodBinding, IMethodBinding> findBridgeMethods() {
    Map<IMethodBinding, IMethodBinding> bridgeMethodByDelegateMethod = new LinkedHashMap<>();
    for (IMethodBinding bridgeMethod : getPotentialBridgeMethods(typeBinding)) {
      IMethodBinding delegateMethod = findForwardingMethod(bridgeMethod, typeBinding);
      if (delegateMethod != null) {
        bridgeMethodByDelegateMethod.put(bridgeMethod, delegateMethod);
      } else {
        // If no delegated method is found, it means that no overriding method exists, normally no
        // bridge method is needed, except in the case when it overrides an interface method with
        // more specific types, and in this case a bridge method for the interface method is needed.
        IMethodBinding backwardingMethod = findBackwardingMethod(bridgeMethod, typeBinding);
        if (backwardingMethod != null) {
          bridgeMethodByDelegateMethod.put(backwardingMethod, bridgeMethod.getMethodDeclaration());
        }
      }
    }
    return bridgeMethodByDelegateMethod;
  }

  /**
   * Returns all the potential methods in the super classes and super interfaces that may need a
   * bridge method generating in {@code type}.
   *
   * <p>A bridge method is needed in a type when the type extends or implements a parameterized
   * class or interface and type erasure changes the signature of any inherited method. This
   * inherited method is a potential method that may need a bridge method.
   */
  private static List<IMethodBinding> getPotentialBridgeMethods(ITypeBinding type) {
    List<IMethodBinding> potentialBridgeMethods = new ArrayList<>();
    ITypeBinding superclass = type.getSuperclass();
    if (superclass != null) {
      // add the potential bridge methods from direct super class.
      potentialBridgeMethods.addAll(getPotentialBridgeMethodsInType(superclass));
      // recurse into super class.
      potentialBridgeMethods.addAll(getPotentialBridgeMethods(superclass));
    }
    for (ITypeBinding superInterface : type.getInterfaces()) {
      // add the potential bridge methods from direct super interfaces.
      potentialBridgeMethods.addAll(getPotentialBridgeMethodsInType(superInterface));
      // recurse into super interfaces.
      potentialBridgeMethods.addAll(getPotentialBridgeMethods(superInterface));
    }
    return potentialBridgeMethods;
  }

  /**
   * Returns the potential methods in {@code type} that may need a bridge method.
   */
  private static Collection<IMethodBinding> getPotentialBridgeMethodsInType(ITypeBinding type) {
    return Collections2.filter(
        Arrays.asList(type.getDeclaredMethods()),
        new Predicate<IMethodBinding>() {
          @Override
          /**
           * If {@code method}, the method with more specific type arguments, has different
           * method signature with {@code method.getMethodDeclaration()}, the original generic
           * method, it means this method is a potential method that may need a bridge method.
           */
          public boolean apply(IMethodBinding method) {
            return !method.isConstructor()
                && !method.isSynthetic()
                // is a parameterized method.
                && method != method.getMethodDeclaration()
                // type erasure changes the signature
                && !JdtUtils.areParameterErasureEqual(method, method.getMethodDeclaration());
          }
        });
  }

  /**
   * Returns the delegated method (implemented or inherited) by {@code type} that
   * {@code bridgeMethod} should be delegated to.
   *
   * <p>If a method (a method with more specific type arguments) in {@code type} or in its
   * super types has the same erasured signature with {@code bridgeMethod}, it is an overriding
   * method for {@code bridgeMethod}. And if their original method declarations are different then
   * a bridge method is needed to make overriding work.
   */
  private static IMethodBinding findForwardingMethod(
      IMethodBinding bridgeMethod, ITypeBinding type) {
    for (IMethodBinding method : type.getDeclaredMethods()) {
      if (!method.isEqualTo(bridgeMethod) // should not delegate to itself
          && !Modifier.isAbstract(method.getModifiers()) // should be a concrete implementation.
          && JdtUtils.areParameterErasureEqual(
              method, bridgeMethod) // concrete methods have the same signature, thus an overriding.
          && !JdtUtils
              .areParameterErasureEqual( // original method declarations have different signatures
              method.getMethodDeclaration(), bridgeMethod.getMethodDeclaration())) {
        // find a overriding method (also possible accidental overriding), this is the method that
        // should be delegated to.
        return method;
      }
    }
    // recurse to super class.
    if (type.getSuperclass() != null) {
      IMethodBinding delegateMethodInParent =
          findForwardingMethod(bridgeMethod, type.getSuperclass());
      if (delegateMethodInParent != null) {
        return delegateMethodInParent;
      }
    }
    // recurse to super interfaces.
    for (ITypeBinding superInterface : type.getInterfaces()) {
      IMethodBinding delegateMethodInInterface = findForwardingMethod(bridgeMethod, superInterface);
      if (delegateMethodInInterface != null) {
        return delegateMethodInInterface;
      }
    }
    return null;
  }

  /**
   * Returns the method in its super interface that needs a bridge method delegating to
   * {@code bridgeMethod}.
   *
   * <p>If a method in the super interfaces of {@code type} is a method with more specific type
   * arguments, and it is overridden by a generic method, it needs a bridge method that delegates to
   * the generic method.
   */
  private static IMethodBinding findBackwardingMethod(
      IMethodBinding bridgeMethod, ITypeBinding type) {
    for (ITypeBinding superInterface : JdtUtils.getAllInterfaces(type)) {
      for (IMethodBinding methodBinding : superInterface.getDeclaredMethods()) {
        if (methodBinding == methodBinding.getMethodDeclaration() // non-generic method,
            // generic method has been investigated by findForwardingMethod.
            && JdtUtils.areParameterErasureEqual(methodBinding, bridgeMethod)
            // is overridden by a generic method with different erasure parameter types.
            && !JdtUtils.areParameterErasureEqual(
                methodBinding, bridgeMethod.getMethodDeclaration())) {
          return methodBinding;
        }
      }
    }
    return null;
  }

  /**
   * Creates MethodDescriptor in current class that has the same signature of {@code methodBinding},
   * with return type of {@code returnType}.
   */
  private MethodDescriptor createMethodDescriptorInCurrentType(
      IMethodBinding methodBinding, ITypeBinding returnType) {
    TypeDescriptor enclosingClassTypeDescriptor = JdtUtils.createTypeDescriptor(typeBinding);
    TypeDescriptor returnTypeDescriptor = JdtUtils.createTypeDescriptor(returnType);
    MethodDescriptor originalMethodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);

    return MethodDescriptorBuilder.from(originalMethodDescriptor)
        .enclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .returnTypeDescriptor(returnTypeDescriptor)
        .build();
  }

  /**
   * Returns bridge method that calls the delegated method in its body.
   *
   * <p>bridgeMethod: parameterized method with more specific type arguments.
   * bridgeMethod.getMethodDeclaration(): original declaration method.
   * targetMethod: concrete implementation that should be delegated to.
   */
  private Method createBridgeMethod(IMethodBinding bridgeMethod, IMethodBinding targetMethod) {
    // The MethodDescriptor of the generated bridge method should have the same signature as the
    // original declared method.
    // Using the return type of the delegated method also avoids generating redundant bridge methods
    // for two methods that have the same parameter signature but different return types.
    ITypeBinding returnType =
        bridgeMethod == bridgeMethod.getMethodDeclaration()
            ? bridgeMethod.getReturnType() // use its own return type if it is a concrete method
            : targetMethod.getReturnType(); // otherwise use the return type of the target method.
    MethodDescriptor bridgeMethodDescriptor =
        createMethodDescriptorInCurrentType(bridgeMethod, returnType);
    // The MethodDescriptor of the delegated method.
    MethodDescriptor targetMethodDescriptor =
        createMethodDescriptorInCurrentType(targetMethod, targetMethod.getReturnType());
    List<Variable> parameters = new ArrayList<>();
    List<Expression> arguments = new ArrayList<>();

    for (int i = 0; i < bridgeMethodDescriptor.getParameterTypeDescriptors().size(); i++) {
      Variable parameter =
          new Variable(
              "arg" + i,
              JdtUtils.createTypeDescriptor(bridgeMethod.getParameterTypes()[i]),
              false,
              true);
      parameters.add(parameter);
      Expression parameterReference = parameter.getReference();

      // The type the argument should be casted to. It should be casted to the specific parameter
      // type that is expected by the concrete parameterized method.
      TypeDescriptor castToParameterTypeDescriptor =
          JdtUtils.createTypeDescriptor(bridgeMethod.getParameterTypes()[i]);
      // if the parameter type in bridge method is different from that in parameterized method,
      // add a cast.
      Expression argument =
          bridgeMethodDescriptor.getParameterTypeDescriptors().get(i)
                  == castToParameterTypeDescriptor
              ? parameterReference
              : new CastExpression(parameterReference, castToParameterTypeDescriptor);
      arguments.add(argument);
    }
    Expression dispatchMethodCall =
        MethodCall.createRegularMethodCall(
            new ThisReference(targetMethodDescriptor.getEnclosingClassTypeDescriptor()),
            targetMethodDescriptor,
            arguments);
    Statement statement =
        bridgeMethodDescriptor.getReturnTypeDescriptor() == TypeDescriptors.get().primitiveVoid
            ? new ExpressionStatement(dispatchMethodCall)
            : new ReturnStatement(
                dispatchMethodCall, bridgeMethodDescriptor.getReturnTypeDescriptor());
    return new Method(
        bridgeMethodDescriptor,
        parameters,
        new Block(Arrays.asList(statement)),
        false,
        false,
        false,
        "Synthetic method.");
  }
}
