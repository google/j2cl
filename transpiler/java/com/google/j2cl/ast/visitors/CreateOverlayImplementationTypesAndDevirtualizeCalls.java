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
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptors;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates Overlay types, devirtualizes default and overlay methods and redirects member accesses:
 *
 * <p>For native types and JsFunction interfaces, creates overlays that will contain the overlay
 * methods (devirtualizing them if needed) and the overlay fields.
 *
 * <p>For regular interfaces devirtualizes default methods.
 *
 * <p>Any references to the modified fields and methods are redirected appropriately.
 */
public class CreateOverlayImplementationTypesAndDevirtualizeCalls extends NormalizationPass {

  private static final String DEFAULT_PREFIX = "__$default";

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    createOverlayImplementationTypes(compilationUnit);
    devirtualizeInterfaceMethods(compilationUnit);
    compilationUnit.accept(new MemberAccessesRedirector());
  }

  private void devirtualizeInterfaceMethods(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      if (!type.isInterface()) {
        continue;
      }
      List<Method> devirtualizedMethods = new ArrayList<>();
      for (Method method : type.getMethods()) {
        if (method.getDescriptor().isDefaultMethod()) {
          devirtualizedMethods.add(AstUtils.devirtualizeMethod(method, DEFAULT_PREFIX));
          AstUtils.stubMethod(method);
        }
      }
      type.addMethods(devirtualizedMethods);
    }
  }

  private void createOverlayImplementationTypes(CompilationUnit compilationUnit) {
    List<Type> overlayTypes = new ArrayList<>();

    for (Type type : compilationUnit.getTypes()) {
      if (!type.getDeclaration().hasOverlayImplementationType()) {
        continue;
      }
      overlayTypes.add(createOverlayImplementationType(type));
    }
    compilationUnit.getTypes().addAll(overlayTypes);
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
      if (!member.getDescriptor().isJsOverlay()) {
        continue;
      }
      if (member instanceof Method) {
        Method method = (Method) member;
        overlayClass.addMethod(createOverlayMethod(method, overlayImplTypeDescriptor));
      } else if (member instanceof Field) {
        Field field = (Field) member;
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
      }
    }
    type.getMembers()
        .removeIf(member -> member.getDescriptor().isJsOverlay() || member.isInitializerBlock());
    return overlayClass;
  }

  private static Method createOverlayMethod(
      Method method, DeclaredTypeDescriptor overlayImplTypeDescriptor) {
    return method.getDescriptor().isStatic()
        ? AstUtils.createStaticOverlayMethod(method, overlayImplTypeDescriptor)
        : AstUtils.devirtualizeMethod(method, overlayImplTypeDescriptor);
  }

  /**
   * Updates all field accesses to fields that have been moved and method calls to the methods that
   * have been moved and/or devirtualized.
   */
  private static class MemberAccessesRedirector extends AbstractRewriter {
    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      DeclaredTypeDescriptor enclosingTypeDescriptor =
          methodCall.getTarget().getEnclosingTypeDescriptor();

      if (methodCall.getTarget().isJsOverlay()) {
        return redirectCall(
            methodCall,
            TypeDescriptors.createOverlayImplementationTypeDescriptor(enclosingTypeDescriptor));
      }
      if (methodCall.getTarget().isDefaultMethod() && methodCall.isStaticDispatch()) {
        // Only redirect static dispatch to default methods.
        checkArgument(!enclosingTypeDescriptor.getTypeDeclaration().hasOverlayImplementationType());
        return AstUtils.devirtualizeMethodCall(methodCall, DEFAULT_PREFIX);
      }
      return methodCall;
    }

    private static Node redirectCall(MethodCall methodCall, DeclaredTypeDescriptor targetType) {
      if (methodCall.getTarget().isStatic()) {
        return MethodCall.Builder.from(methodCall)
            .setEnclosingTypeDescriptor(targetType)
            .setQualifier(null)
            .build();
      }
      return AstUtils.devirtualizeMethodCall(methodCall, targetType);
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
