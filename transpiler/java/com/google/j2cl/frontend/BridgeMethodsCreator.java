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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;

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
  public static List<Method> createBridgeMethods(ITypeBinding typeBinding) {
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
      IMethodBinding delegateMethod = findDelegatedMethod(bridgeMethod, typeBinding);
      // If no delegated method is found, it means that no overriding method exists, then no bridge
      // method is needed.
      if (delegateMethod != null) {
        bridgeMethodByDelegateMethod.put(bridgeMethod, delegateMethod);
      }
    }
    return bridgeMethodByDelegateMethod;
  }

  /**
   * Returns all the potential methods in the super classes and super interfaces that may need a
   * bridge method generating in {@code type}.
   * <p>
   * A bridge method is needed in a type when the type extends or implements a parameterized class
   * or interface and type erasure changes the signature of any inherited method. This inherited
   * method is a potential method that may need a bridge method.
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
   */
  private static IMethodBinding findDelegatedMethod(
      IMethodBinding bridgeMethod, ITypeBinding type) {
    for (IMethodBinding method : type.getDeclaredMethods()) {
      if (!method.isEqualTo(bridgeMethod) // should not delegate to itself
          && !Modifier.isAbstract(method.getModifiers()) // should be a concrete implementation.
          && JdtUtils.areParameterErasureEqual(method, bridgeMethod)) { // has the same signature
        // find a overriding method (also possible accidental overriding), this is the method that
        // should be delegated to.
        return method;
      }
    }
    // recurse to super class.
    if (type.getSuperclass() != null) {
      IMethodBinding delegateMethodInParent =
          findDelegatedMethod(bridgeMethod, type.getSuperclass());
      if (delegateMethodInParent != null) {
        return delegateMethodInParent;
      }
    }
    // recurse to super interfaces.
    for (ITypeBinding superInterface : type.getInterfaces()) {
      IMethodBinding delegateMethodInInterface = findDelegatedMethod(bridgeMethod, superInterface);
      if (delegateMethodInInterface != null) {
        return delegateMethodInInterface;
      }
    }
    return null;
  }

  /**
   * Creates MethodDescriptor in current class that has the same erasure signature of
   * {@code methodBinding}.
   */
  private MethodDescriptor createMethodDescriptorInCurrentType(IMethodBinding methodBinding) {
    boolean isStatic = JdtUtils.isStatic(methodBinding.getModifiers());
    Visibility visibility = JdtUtils.getVisibility(methodBinding.getModifiers());
    TypeDescriptor enclosingClassTypeDescriptor = JdtUtils.createTypeDescriptor(typeBinding);
    String methodName = methodBinding.getName();
    boolean isConstructor = methodBinding.isConstructor();
    boolean isNative = false;

    // create erasureMethodDescriptor by the original method declaration.
    IMethodBinding declaredMethodBinding = methodBinding.getMethodDeclaration();
    Iterable<TypeDescriptor> erasureParameterTypeDescriptors =
        FluentIterable.from(Arrays.asList(declaredMethodBinding.getParameterTypes()))
            .transform(
                new Function<ITypeBinding, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(ITypeBinding typeBinding) {
                    return JdtUtils.createTypeDescriptor(typeBinding.getErasure());
                  }
                });
    TypeDescriptor erasureReturnTypeDescriptor =
        JdtUtils.createTypeDescriptor(declaredMethodBinding.getReturnType().getErasure());
    MethodDescriptor erasureMethodDescriptor =
        MethodDescriptor.create(
            isStatic,
            visibility,
            enclosingClassTypeDescriptor,
            methodName,
            isConstructor,
            isNative,
            erasureReturnTypeDescriptor,
            erasureParameterTypeDescriptors);

    return MethodDescriptor.create(
        isStatic,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        JdtUtils.createTypeDescriptor(methodBinding.getReturnType().getErasure()), // return type
        Iterables.transform(
            Arrays.asList(methodBinding.getParameterTypes()),
            new Function<ITypeBinding, TypeDescriptor>() {
              @Override
              public TypeDescriptor apply(ITypeBinding typeBinding) {
                return JdtUtils.createTypeDescriptor(typeBinding.getErasure()); // use erasure type.
              }
            }),
        ImmutableList.<TypeDescriptor>of(),
        erasureMethodDescriptor);
  }

  /**
   * Returns bridge method that calls the delegated method in its body.
   * <p>
   * bridgeMethod: concrete parameterized method with the concrete type arguments.
   * bridgeMethod.getMethodDeclaration(): original declaration method.
   * targetMethod: concrete implementation that should be delegated to.
   */
  private Method createBridgeMethod(IMethodBinding bridgeMethod, IMethodBinding targetMethod) {
    // The MethodDescriptor of the generated bridge method should have the same signature as the
    // original declared method.
    MethodDescriptor bridgeMethodDescriptor = createMethodDescriptorInCurrentType(bridgeMethod);
    // The MethodDescriptor of the delegated method.
    MethodDescriptor targetMethodDescriptor =
        createMethodDescriptorInCurrentType(targetMethod.getMethodDeclaration());
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

      // The type the argument should be casted to.
      TypeDescriptor castToParameterTypeDescriptor =
          JdtUtils.createTypeDescriptor(bridgeMethod.getParameterTypes()[i].getErasure());
      // if the parameter type in bridge method is different from that in parameterized method,
      // add a cast.
      Expression argument =
          bridgeMethodDescriptor.getErasureMethodDescriptor().getParameterTypeDescriptors().get(i)
                  == castToParameterTypeDescriptor
              ? parameterReference
              : new CastExpression(parameterReference, castToParameterTypeDescriptor);
      arguments.add(argument);
    }
    Expression dispatchMethodCall = new MethodCall(null, targetMethodDescriptor, arguments);
    Statement statement =
        bridgeMethodDescriptor.getReturnTypeDescriptor() == TypeDescriptors.VOID_TYPE_DESCRIPTOR
            ? new ExpressionStatement(dispatchMethodCall)
            : new ReturnStatement(
                dispatchMethodCall, bridgeMethodDescriptor.getReturnTypeDescriptor());
    return Method.createSynthetic(
        bridgeMethodDescriptor, parameters, new Block(Arrays.asList(statement)), false);
  }
}
