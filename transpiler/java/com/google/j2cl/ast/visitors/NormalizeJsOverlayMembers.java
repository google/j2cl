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
import com.google.j2cl.ast.Type;
import java.util.ArrayList;
import java.util.List;

/** Creates Overlay types, devirtualizes instance overlay methods and redirects member accesses. */
public class NormalizeJsOverlayMembers extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    createOverlayImplementationTypes(compilationUnit);
    normalizeMemberReferences(compilationUnit);
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
    // Native types and JsFunction interfaces only contribute code for the overlay class.
    // Once the overlay class is in place they can be removed.
    compilationUnit
        .getTypes()
        .removeIf(type -> type.isNative() || type.getDeclaration().isJsFunctionInterface());
  }

  private static Type createOverlayImplementationType(Type type) {
    DeclaredTypeDescriptor overlayImplTypeDescriptor =
        type.getTypeDescriptor().getOverlayImplementationTypeDescriptor();
    Type overlayClass =
        new Type(
            type.getSourcePosition(),
            type.getVisibility(),
            overlayImplTypeDescriptor.getTypeDeclaration());
    overlayClass.setNativeTypeDescriptor(type.getDeclaration().toUnparameterizedTypeDescriptor());

    for (Member member : type.getMembers()) {
      if (!member.getDescriptor().isJsOverlay()) {
        continue;
      }
      if (member.isMethod()) {
        Method method = (Method) member;
        overlayClass.addMethod(createOverlayMethod(method, overlayImplTypeDescriptor));
      } else if (member.isField()) {
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
        overlayClass.addStaticInitializerBlock(initializerBlock.getBlock());
      }
    }

    type.getMembers().removeIf(member -> member.getDescriptor().isJsOverlay());
    return overlayClass;
  }

  private static Method createOverlayMethod(
      Method method, DeclaredTypeDescriptor overlayImplTypeDescriptor) {
    return method.getDescriptor().isStatic()
        ? AstUtils.createStaticOverlayMethod(method, overlayImplTypeDescriptor)
        : AstUtils.devirtualizeMethod(method, overlayImplTypeDescriptor);
  }

  /**
   * Updates all field accesses to overlay fields that have been moved and method calls to overlay
   * methods that have been devirtualized and/or moved.
   */
  private void normalizeMemberReferences(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public MethodCall rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();

            if (methodDescriptor.isJsOverlay()) {
              return redirectCall(
                  methodCall,
                  methodDescriptor
                      .getEnclosingTypeDescriptor()
                      .getOverlayImplementationTypeDescriptor());
            }
            return methodCall;
          }

          @Override
          public FieldAccess rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor target = fieldAccess.getTarget();
            if (target.isJsOverlay()) {
              checkArgument(target.isStatic());
              DeclaredTypeDescriptor overlayTypeDescriptor =
                  target.getEnclosingTypeDescriptor().getOverlayImplementationTypeDescriptor();
              return FieldAccess.Builder.from(
                      FieldDescriptor.Builder.from(target)
                          .setEnclosingTypeDescriptor(overlayTypeDescriptor)
                          .build())
                  .build();
            }
            return fieldAccess;
          }
        });
  }

  private static MethodCall redirectCall(MethodCall methodCall, DeclaredTypeDescriptor targetType) {
    if (methodCall.getTarget().isStatic()) {
      MethodDescriptor methodDescriptor =
          MethodDescriptor.Builder.from(methodCall.getTarget())
              .setEnclosingTypeDescriptor(targetType)
              .setJsInfo(JsInfo.NONE)
              .build();
      return MethodCall.Builder.from(methodCall)
          .setMethodDescriptor(methodDescriptor)
          .setQualifier(null)
          .build();
    }
    return AstUtils.devirtualizeMethodCall(methodCall, targetType);
  }
}
