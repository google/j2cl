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
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a Overlay implementation types that contain devirtualized JsOverlay methods from native
 * JsTypes and Interfaces that have default methods, and devirtualized the corresponding method
 * calls
 */
public class CreateOverlayImplementationTypesAndDevirtualizeCalls {

  public static void applyTo(CompilationUnit compilationUnit) {
    List<JavaType> replacementTypeList = new ArrayList<>();

    for (JavaType type : compilationUnit.getTypes()) {
      if (!type.getDescriptor().isNative()) {
        replacementTypeList.add(type);
      }
      if (type.getDescriptor().isNative() || type.containsDefaultMethods()) {
        replacementTypeList.add(createOverlayImplementationType(type));
      }
    }
    compilationUnit.getTypes().clear();
    compilationUnit.getTypes().addAll(replacementTypeList);

    // Rewrite method calls.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            if (!shouldRewriteMethodCall(methodCall)) {
              return methodCall;
            }

            TypeDescriptor originalTypeDescriptor =
                methodCall.getTarget().getEnclosingClassTypeDescriptor();
            TypeDescriptor overlayTypeDescriptor =
                AstUtils.createOverlayImplementationClassTypeDescriptor(originalTypeDescriptor);
            if (methodCall.getTarget().isStatic()) {
              return MethodCall.Builder.from(methodCall)
                  .setEnclosingClass(overlayTypeDescriptor)
                  .setQualifier(null)
                  .build();
            }
            // Devirtualize *instance* JsOverlay method.
            return AstUtils.createDevirtualizedMethodCall(methodCall, overlayTypeDescriptor);
          }

          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor target = fieldAccess.getTarget();
            if (target.isJsOverlay()) {
              checkArgument(target.isStatic());
              TypeDescriptor overlayTypeDescriptor =
                  AstUtils.createOverlayImplementationClassTypeDescriptor(
                      target.getEnclosingClassTypeDescriptor());
              return new FieldAccess(
                  null,
                  FieldDescriptor.Builder.from(target)
                      .setEnclosingClass(overlayTypeDescriptor)
                      .build());
            }
            return fieldAccess;
          }
        });
  }

  private static JavaType createOverlayImplementationType(JavaType type) {
    TypeDescriptor overlayImplTypeDescriptor =
        AstUtils.createOverlayImplementationClassTypeDescriptor(type.getDescriptor());
    JavaType overlayClass =
        new JavaType(type.getKind(), type.getVisibility(), overlayImplTypeDescriptor);
    overlayClass.setNativeTypeDescriptor(type.getDescriptor());
    // Copy static JsOverlay methods. Even instance methods should already be devirtualized to
    // static.
    for (Method method : type.getMethods()) {
      if (method.getDescriptor().isJsOverlay() || method.getDescriptor().isDefault()) {
        overlayClass.addMethod(
            Method.Builder.from(
                    method.getDescriptor().isStatic()
                        ? method
                        : AstUtils.createDevirtualizedMethod(method))
                .setEnclosingClass(overlayImplTypeDescriptor)
                .build());

        // clear the method body from the original type.
        method.getBody().getStatements().clear();
      }
    }
    // Copy static JsOverlay fields.
    for (Field field : type.getFields()) {
      if (field.getDescriptor().isJsOverlay()) {
        checkState(field.getDescriptor().isStatic());
        overlayClass.addField(
            Field.Builder.from(field).setEnclosingClass(overlayImplTypeDescriptor).build());
      }
    }
    return overlayClass;
  }

  private static boolean shouldRewriteMethodCall(MethodCall methodCall) {
    MethodDescriptor methodDescriptor = methodCall.getTarget();
    // do not devirtualize non-JsOverlay method.
    TypeDescriptor enclosingClassTypeDescriptor =
        methodDescriptor.getEnclosingClassTypeDescriptor();

    return enclosingClassTypeDescriptor.isNative() && methodDescriptor.isJsOverlay()
        || methodDescriptor.isDefault() && methodCall.isStaticDispatch();
  }
}
