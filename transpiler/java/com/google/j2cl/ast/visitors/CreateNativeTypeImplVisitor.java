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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldBuilder;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodBuilder;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;

/**
 * Creates a JavaType that contains the devirtualized JsOverlay methods in a native JS type.
 */
public class CreateNativeTypeImplVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new CreateNativeTypeImplVisitor().createNativeTypeImpl(compilationUnit);
  }

  public void createNativeTypeImpl(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  private TypeDescriptor jsOverlayImplTypeDescriptor;

  @Override
  public boolean shouldProcessJavaType(JavaType javaType) {
    if (!javaType.getDescriptor().isNative()) {
      return false;
    }
    jsOverlayImplTypeDescriptor =
        AstUtils.createJsOverlayImplTypeDescriptor(javaType.getDescriptor());
    return true;
  }

  @Override
  public Node rewriteField(Field field) {
    if (!(field.getDescriptor().isStatic() && field.getDescriptor().isJsOverlay())) {
      return field;
    }
    // Replace the enclosingClassTypeDescriptor.
    return FieldBuilder.from(field).enclosingClass(jsOverlayImplTypeDescriptor).build();
  }

  @Override
  public Node rewriteMethod(Method method) {
    if (!(method.getDescriptor().isStatic() && method.getDescriptor().isJsOverlay())) {
      return method;
    }
    // Replace the enclosingClassTypeDescriptor.
    return MethodBuilder.from(method).enclosingClass(jsOverlayImplTypeDescriptor).build();
  }

  @Override
  public Node rewriteJavaType(JavaType javaType) {
    JavaType overlayJavaType =
        new JavaType(
            javaType.getKind(),
            javaType.getVisibility(),
            AstUtils.createJsOverlayImplTypeDescriptor(javaType.getDescriptor()));
    overlayJavaType.setNativeTypeDescriptor(javaType.getDescriptor());
    // Copy static JsOverlay methods. Even instance methods should already be devirtualized to
    // static.
    for (Method method : javaType.getMethods()) {
      if (method.getDescriptor().isStatic() && method.getDescriptor().isJsOverlay()) {
        overlayJavaType.addMethod(method);
      }
    }
    // Copy static JsOverlay fields.
    for (Field field : javaType.getFields()) {
      if (field.getDescriptor().isStatic() && field.getDescriptor().isJsOverlay()) {
        overlayJavaType.addField(field);
      }
    }
    return overlayJavaType;
  }
}
