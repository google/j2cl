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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsMemberType;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * Checks circumstances where a bridge method should be generated and creates the bridge methods.
 */
public class BridgeMethodsCreator {
  /** Creates and adds bridge methods to the java type and fixes the delegated JS methods. */
  public static void create(ITypeBinding typeBinding, Type type) {
    // create bridge methods.
    Set<String> generatedBridgeMethodMangledNames = new HashSet<>();
    Multimap<MethodDescriptor, Method> bridgeMethodsByTargetMethodDescriptor =
        LinkedHashMultimap.create();

    for (Map.Entry<IMethodBinding, IMethodBinding> entry :
        findBridgeMethods(typeBinding).entrySet()) {
      // bridgeMethod -> delegateMethod.
      Method bridgeMethod = createBridgeMethod(typeBinding, entry.getKey(), entry.getValue());
      String manglingName = ManglingNameUtils.getMangledName(bridgeMethod.getDescriptor());
      if (generatedBridgeMethodMangledNames.contains(manglingName)) {
        // do not generate duplicate bridge methods in one class.
        continue;
      }
      bridgeMethodsByTargetMethodDescriptor.put(
          JdtUtils.createMethodDescriptor(entry.getValue()), bridgeMethod);
      generatedBridgeMethodMangledNames.add(manglingName);
    }

    fixJsInfoInBridgesAndDelegatedMethods(type, bridgeMethodsByTargetMethodDescriptor);
    // add bridge methods.
    type.addMethods(bridgeMethodsByTargetMethodDescriptor.values());
  }

  private static void fixJsInfoInBridgesAndDelegatedMethods(
      Type type, final Multimap<MethodDescriptor, Method> bridgeMethodsByTargetMethodDescriptor) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            Collection<Method> bridgeJsMethods =
                bridgeMethodsByTargetMethodDescriptor
                    .get(method.getDescriptor())
                    .stream()
                    .filter(bridgeMethod -> bridgeMethod.getDescriptor().isJsMethod())
                    .collect(Collectors.toList());
            if (bridgeJsMethods.isEmpty()) {
              return method;
            }
            for (Method bridgeJsMethod : bridgeJsMethods) {
              // Transfer parameter optionality from the target methods which will be "demoted"
              // to non JsMethod.
              // TODO(rluble): parameter optionality might better be moved to MethodDescriptor
              // from method and all this business of moving optionality would have been
              // handled in a simpler manner.
              for (int i = 0; i < method.getParameters().size(); i++) {
                bridgeJsMethod.setParameterOptionality(i, method.isParameterOptional(i));
              }
            }
            // Now that the bridge method is created (and marked JsMethod), "demote" to a plain
            // Java method by setting JsInfo to NONE and resetting parameter optionality.
            Method.Builder methodBuilder = Method.Builder.from(method).setJsInfo(JsInfo.NONE);
            for (int i = 0; i < method.getParameters().size(); i++) {
              methodBuilder.setParameterOptional(i, false);
            }
            return methodBuilder.build();
          }
        });
  }

  /** Returns the mappings from the needed bridge method to the delegated method. */
  private static Map<IMethodBinding, IMethodBinding> findBridgeMethods(ITypeBinding typeBinding) {
    if (typeBinding.isInterface()) {
      // Do not create bridge methods in interfaces.
      return ImmutableMap.of();
    }
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
        /**
         * If {@code method}, the method with more specific type arguments, has different method
         * signature with {@code method.getMethodDeclaration()}, the original generic method, it
         * means this method is a potential method that may need a bridge method.
         */
        method ->
            !method.isConstructor()
                && !method.isSynthetic()
                // is a parameterized method.
                && method != method.getMethodDeclaration()
                // type erasure changes the signature
                && !JdtUtils.areMethodsOverrideEquivalent(method, method.getMethodDeclaration()));
  }

  /**
   * Returns the delegated method (implemented or inherited) by {@code type} that
   * {@code bridgeMethod} should be delegated to.
   *
   * <p>
   * If a method (a method with more specific type arguments) in {@code type} or in its super types
   * has the same erasured signature with {@code bridgeMethod}, it is an overriding method for
   * {@code bridgeMethod}. And if their original method declarations are different then a bridge
   * method is needed to make overriding work.
   */
  private static IMethodBinding findForwardingMethod(
      IMethodBinding bridgeMethod, ITypeBinding type) {
    for (IMethodBinding method : type.getDeclaredMethods()) {
      if (!method.isEqualTo(bridgeMethod) // should not delegate to itself
          && !Modifier.isAbstract(method.getModifiers()) // should be a concrete implementation.
          // concrete methods have the same signature, thus an overriding.
          && JdtUtils.areMethodsOverrideEquivalent(method, bridgeMethod)
          // original method declarations have different signatures
          && !JdtUtils.areMethodsOverrideEquivalent(
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
   * <p>
   * If a method in the super interfaces of {@code type} is a method with more specific type
   * arguments, and it is overridden by a generic method, it needs a bridge method that delegates to
   * the generic method.
   */
  private static IMethodBinding findBackwardingMethod(
      IMethodBinding bridgeMethod, ITypeBinding type) {
    for (ITypeBinding superInterface : JdtUtils.getAllInterfaces(type)) {
      for (IMethodBinding methodBinding : superInterface.getDeclaredMethods()) {
        if (methodBinding == methodBinding.getMethodDeclaration() // non-generic method,
            // generic method has been investigated by findForwardingMethod.
            && JdtUtils.areMethodsOverrideEquivalent(methodBinding, bridgeMethod)
            // is overridden by a generic method with different erasure parameter types.
            && !JdtUtils.areMethodsOverrideEquivalent(
                methodBinding, bridgeMethod.getMethodDeclaration())) {
          return methodBinding;
        }
      }
    }
    return null;
  }

  /**
   * Creates MethodDescriptor in {@code typeBinding} that has the same signature as
   * {@code methodBinding} with return type of {@code returnType}.
   */
  private static MethodDescriptor createMethodDescriptor(
      ITypeBinding typeBinding, IMethodBinding methodBinding, ITypeBinding returnType) {
    checkArgument(!typeBinding.isInterface());

    TypeDescriptor enclosingClassTypeDescriptor = JdtUtils.createTypeDescriptor(typeBinding);
    TypeDescriptor returnTypeDescriptor = JdtUtils.createTypeDescriptor(returnType);

    MethodDescriptor originalMethodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);

    return MethodDescriptor.Builder.from(originalMethodDescriptor)
        .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .setReturnTypeDescriptor(returnTypeDescriptor)
        .build();
  }

  /**
   * Returns bridge method that calls the delegated method in its body.
   *
   * <p>
   * bridgeMethod: parameterized method with more specific type arguments.
   * bridgeMethod.getMethodDeclaration(): original declaration method. targetMethod: concrete
   * implementation that should be delegated to.
   */
  private static Method createBridgeMethod(
      ITypeBinding typeBinding, IMethodBinding bridgeMethod, IMethodBinding targetMethod) {
    // The MethodDescriptor of the generated bridge method should have the same signature as the
    // original declared method.
    // Using the return type of the delegated method also avoids generating redundant bridge methods
    // for two methods that have the same parameter signature but different return types.
    ITypeBinding returnType =
        bridgeMethod == bridgeMethod.getMethodDeclaration()
            ? bridgeMethod.getReturnType() // use its own return type if it is a concrete method
            : targetMethod.getReturnType(); // otherwise use the return type of the target method.
    MethodDescriptor bridgeMethodDescriptor =
        MethodDescriptor.Builder.from(createMethodDescriptor(typeBinding, bridgeMethod, returnType))
            .setParameterTypeDescriptors(
                Arrays.stream(bridgeMethod.getMethodDeclaration().getParameterTypes())
                    .map(m -> JdtUtils.createTypeDescriptor(m.getErasure()).getRawTypeDescriptor())
                    .toArray(TypeDescriptor[]::new))
            .setSynthetic(true)
            .build();
    // The MethodDescriptor of the delegated method.
    MethodDescriptor targetMethodDescriptor = JdtUtils.createMethodDescriptor(targetMethod);
    JsInfo targetMethodJsInfo = targetMethodDescriptor.getJsInfo();

    // If a JsFunction method needs a bridge, only the bridge method is a JsFunction method, and it
    // delegates to the *real* implementation, which is not a JsFunction method.
    // If both a method and the bridge method are JsMethod, only the bridge method is a JsMethod,
    // and it delegates to the *real* implementation, which should be emit as non-JsMethod.
    if (targetMethodJsInfo.getJsMemberType() == JsMemberType.JS_FUNCTION
        || (bridgeMethodDescriptor.isJsMethod()
            && targetMethodDescriptor.inSameTypeAs(bridgeMethodDescriptor))) {
      targetMethodJsInfo = JsInfo.NONE;
    }
    targetMethodDescriptor =
        MethodDescriptor.Builder.from(targetMethodDescriptor).setJsInfo(targetMethodJsInfo).build();
    List<Variable> parameters = new ArrayList<>();
    List<Expression> arguments = new ArrayList<>();

    for (int i = 0;
        i
            < bridgeMethodDescriptor
                .getDeclarationMethodDescriptor()
                .getParameterTypeDescriptors()
                .size();
        i++) {
      Variable parameter =
          Variable.newBuilder()
              .setName("arg" + i)
              .setTypeDescriptor(bridgeMethodDescriptor.getParameterTypeDescriptors().get(i))
              .setIsParameter(true)
              .build();

      parameters.add(parameter);
      Expression parameterReference = parameter.getReference();

      // The type the argument should be casted to. It should be casted to the specific parameter
      // type that is expected by the concrete parameterized method.
      TypeDescriptor castToParameterTypeDescriptor =
          JdtUtils.createTypeDescriptor(bridgeMethod.getParameterTypes()[i]);
      // if the parameter type in bridge method is different from that in parameterized method,
      // add a cast.
      Expression argument =
          bridgeMethodDescriptor
                  .getDeclarationMethodDescriptor()
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
    TypeDescriptor targetEnclosingClassTypeDescriptor =
        targetMethodDescriptor.getEnclosingClassTypeDescriptor();
    Expression qualifier =
        bridgeMethodDescriptor.isMemberOf(targetEnclosingClassTypeDescriptor)
                || targetEnclosingClassTypeDescriptor.isInterface()
            ? new ThisReference(targetEnclosingClassTypeDescriptor)
            : new SuperReference(targetEnclosingClassTypeDescriptor);
    Expression dispatchMethodCall =
        MethodCall.Builder.from(targetMethodDescriptor)
            .setQualifier(qualifier)
            .setArguments(arguments)
            .build();
    TypeDescriptor returnTypeDescriptor = bridgeMethodDescriptor.getReturnTypeDescriptor();
    Statement statement =
        AstUtils.createReturnOrExpressionStatement(dispatchMethodCall, returnTypeDescriptor);
    checkArgument(bridgeMethodDescriptor.isSynthetic());
    return Method.newBuilder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setParameters(parameters)
        .addStatements(statement)
        .setIsOverride(true)
        .setIsBridge(true)
        .setJsDocDescription("Bridge method.")
        .build();
  }

}
