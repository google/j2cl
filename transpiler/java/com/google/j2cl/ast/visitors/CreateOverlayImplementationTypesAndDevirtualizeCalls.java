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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.InitializerBlock;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates Overlay types, bridges to overlay methods and redirects member accesses:
 *
 * <p>Creates Overlay types (by moving fields and methods from the original type to the new Overlay
 * type) for JsTypes that have fields or methods and for Interfaces that have default methods.
 *
 * <p>Any references to the moved fields and methods are redirected to the new location.
 *
 * <p>When moving instance methods out of a JsType, those methods must be devirtualized. As a result
 * reference sites must be converted from instance method calls to static method calls.
 */
public class CreateOverlayImplementationTypesAndDevirtualizeCalls extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    createOverlayImplementationTypes(compilationUnit);
    compilationUnit.accept(new MemberAccessesRedirector());
  }

  private void createOverlayImplementationTypes(CompilationUnit compilationUnit) {
    List<Type> replacementTypeList = new ArrayList<>();

    for (Type type : compilationUnit.getTypes()) {
      if (!(type.getDeclaration().isNative() || type.getDeclaration().isJsFunctionInterface())) {
        replacementTypeList.add(type);
      }
      if (type.getDeclaration().hasOverlayImplementationType()) {
        replacementTypeList.add(createOverlayImplementationType(type));
      }
    }
    compilationUnit.getTypes().clear();
    compilationUnit.getTypes().addAll(replacementTypeList);
  }

  private static Type createOverlayImplementationType(Type type) {
    DeclaredTypeDescriptor overlayImplTypeDescriptor =
        TypeDescriptors.createOverlayImplementationTypeDescriptor(
            type.getDeclaration().toUnparamterizedTypeDescriptor());
    Type overlayClass =
        new Type(
            type.getSourcePosition(),
            type.getVisibility(),
            overlayImplTypeDescriptor.getTypeDeclaration());
    overlayClass.setNativeTypeDescriptor(type.getDeclaration().toUnparamterizedTypeDescriptor());

    for (Member member : type.getMembers()) {
      if (member instanceof Method) {
        Method method = (Method) member;
        if (!method.getDescriptor().isJsOverlay() && !method.getDescriptor().isDefaultMethod()) {
          continue;
        }
        overlayClass.addMethod(createOverlayMethod(method, overlayImplTypeDescriptor));
      } else if (member instanceof Field) {
        Field field = (Field) member;
        if (!field.getDescriptor().isJsOverlay()) {
          continue;
        }
        checkState(field.getDescriptor().isStatic());
        overlayClass.addField(
            Field.Builder.from(field)
                .setInitializer(AstUtils.clone(field.getInitializer()))
                .setEnclosingClass(overlayImplTypeDescriptor)
                .build());
      } else {
        InitializerBlock initializerBlock = (InitializerBlock) member;
        checkState(initializerBlock.isStatic());
        overlayClass.addStaticInitializerBlock(AstUtils.clone(initializerBlock.getBlock()));
        initializerBlock.getBlock().getStatements().clear();
      }
    }
    return overlayClass;
  }

  private static Method createOverlayMethod(
      Method method, DeclaredTypeDescriptor overlayImplTypeDescriptor) {
    Method statifiedMethod =
        method.getDescriptor().isStatic() ? method : AstUtils.createDevirtualizedMethod(method);
    Method movedMethod =
        Method.Builder.from(statifiedMethod)
            .setMethodDescriptor(
                MethodDescriptor.Builder.from(statifiedMethod.getDescriptor())
                    .setJsInfo(
                        JsInfo.Builder.from(JsInfo.NONE)
                            .setJsAsync(method.getDescriptor().isJsAsync())
                            .build())
                    .setEnclosingTypeDescriptor(overlayImplTypeDescriptor)
                    .removeParameterOptionality()
                    .build())
            .build();

    // clear the method body from the original type and use a fresh list of parameters.
    method.getBody().getStatements().clear();
    List<Variable> newParameters = AstUtils.clone(method.getParameters());
    method.getParameters().clear();
    method.getParameters().addAll(newParameters);

    return movedMethod;
  }

  /**
   * Finds accesses to properties that have been moved to overlay classes, and rewrites the accesses
   * to the new location.
   */
  private static class MemberAccessesRedirector extends AbstractRewriter {
    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      MethodDescriptor methodDescriptor = methodCall.getTarget();
      DeclaredTypeDescriptor enclosingTypeDescriptor =
          methodDescriptor.getEnclosingTypeDescriptor();

      boolean targetIsJsOverlayInNativeClass =
          (enclosingTypeDescriptor.isNative() || enclosingTypeDescriptor.isJsFunctionInterface())
              && methodCall.getTarget().isJsOverlay();

      boolean targetIsDefaultMethodAccessedStatically =
          methodCall.getTarget().isDefaultMethod() && methodCall.isStaticDispatch();

      if (targetIsDefaultMethodAccessedStatically || targetIsJsOverlayInNativeClass) {

        DeclaredTypeDescriptor overlayTypeDescriptor =
            TypeDescriptors.createOverlayImplementationTypeDescriptor(enclosingTypeDescriptor);
        if (methodCall.getTarget().isStatic()) {
          return MethodCall.Builder.from(methodCall)
              .setEnclosingTypeDescriptor(overlayTypeDescriptor)
              .setQualifier(null)
              .build();
        }
        // Devirtualize *instance* JsOverlay method.
        return AstUtils.createDevirtualizedMethodCall(methodCall, overlayTypeDescriptor);
      }

      return methodCall;
    }

    @Override
    public Node rewriteFieldAccess(FieldAccess fieldAccess) {
      FieldDescriptor target = fieldAccess.getTarget();
      if (target.isJsOverlay()) {
        checkArgument(target.isStatic());
        DeclaredTypeDescriptor overlayTypeDescriptor =
            TypeDescriptors.createOverlayImplementationTypeDescriptor(
                target.getEnclosingTypeDescriptor());
        return FieldAccess.Builder.from(
                FieldDescriptor.Builder.from(target)
                    .setEnclosingTypeDescriptor(overlayTypeDescriptor)
                    .build())
            .build();
      }
      return fieldAccess;
    }
  }
}
