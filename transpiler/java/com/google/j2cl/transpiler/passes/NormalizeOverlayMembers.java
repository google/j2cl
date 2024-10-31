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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.j2cl.transpiler.ast.AstUtils.isBoxableJsEnumType;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.InitializerBlock;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import java.util.ArrayList;
import java.util.List;

/** Creates Overlay types, devirtualizes instance overlay methods and redirects member accesses. */
public class NormalizeOverlayMembers extends NormalizationPass {

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
      if (!AstUtils.isJsEnumBoxingSupported() && type.getDeclaration().isJsEnum()) {
        continue;
      }
      overlayTypes.add(createOverlayImplementationType(type));
    }
    compilationUnit.getTypes().addAll(overlayTypes);
  }

  private static Type createOverlayImplementationType(Type type) {
    TypeDeclaration overlayTypeDeclaration =
        type.getDeclaration().getOverlayImplementationTypeDeclaration();
    DeclaredTypeDescriptor overlayTypeDescriptor = overlayTypeDeclaration.toDescriptor();
    Type overlayClass = new Type(type.getSourcePosition(), overlayTypeDeclaration);

    for (Member member : type.getMembers()) {
      if (!isOverlay(member.getDescriptor())) {
        continue;
      }
      if (member.isMethod()) {
        Method method = (Method) member;
        overlayClass.addMember(createOverlayMethod(method, overlayTypeDescriptor));
      } else if (member.isField()) {
        Field field = (Field) member;
        checkState(field.getDescriptor().isStatic());
        overlayClass.addMember(
            Field.Builder.from(field)
                .setInitializer(AstUtils.clone(field.getInitializer()))
                .setEnclosingClass(overlayTypeDescriptor)
                .build());
      } else {
        InitializerBlock initializerBlock = (InitializerBlock) member;
        checkState(initializerBlock.isStatic());
        overlayClass.addStaticInitializerBlock(initializerBlock.getBody());
      }
    }

    type.getMembers().removeIf(member -> isOverlay(member.getDescriptor()));
    return overlayClass;
  }

  private static final String OVERLAY_METHOD_SUFFIX = "__$devirt";

  private static Method createOverlayMethod(
      Method method, DeclaredTypeDescriptor overlayImplTypeDescriptor) {
    return method.getDescriptor().isStatic()
        ? AstUtils.createStaticOverlayMethod(method, overlayImplTypeDescriptor)
        : AstUtils.devirtualizeMethod(method, overlayImplTypeDescriptor, OVERLAY_METHOD_SUFFIX);
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

            if (isOverlay(methodDescriptor)) {
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
            if (isOverlay(target)) {
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
    MethodDescriptor methodDescriptor = methodCall.getTarget();
    if (methodDescriptor.isStatic()) {
      return MethodCall.Builder.from(methodCall)
          .setTarget(
              methodDescriptor.transform(
                  builder ->
                      builder
                          .setEnclosingTypeDescriptor(targetType)
                          .setOriginalJsInfo(JsInfo.NONE)))
          .setQualifier(null)
          .build();
    }
    return AstUtils.devirtualizeMethodCall(methodCall, targetType, OVERLAY_METHOD_SUFFIX);
  }

  private static boolean isOverlay(MemberDescriptor memberDescriptor) {
    if (!AstUtils.isJsEnumBoxingSupported()
        && memberDescriptor.getEnclosingTypeDescriptor().isJsEnum()) {
      return false;
    }
    return memberDescriptor.isJsOverlay()
        || (isBoxableJsEnumType(memberDescriptor.getEnclosingTypeDescriptor())
            && !memberDescriptor.isEnumConstant());
  }
}
