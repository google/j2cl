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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.TypeDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a Overlay implementation types that contain devirtualized JsOverlay methods from native
 * JsTypes.
 */
public class CreateOverlayImplementationTypes {

  public static void applyTo(CompilationUnit compilationUnit) {
    List<JavaType> replacementTypeList = new ArrayList<>();

    for (JavaType type : compilationUnit.getTypes()) {
      if (!type.getDescriptor().isNative()) {
        replacementTypeList.add(type);
      } else {
        replacementTypeList.add(createOverlayImplementationType(type));
      }
    }
    compilationUnit.getTypes().clear();
    compilationUnit.getTypes().addAll(replacementTypeList);
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
      // Overlay methods have been already devirtualized, only copy the static versions.
      if (method.getDescriptor().isStatic() && method.getDescriptor().isJsOverlay()) {
        overlayClass.addMethod(
            Method.Builder.from(method).enclosingClass(overlayImplTypeDescriptor).build());
        // Clear the method body from the original type.
        method.getBody().getStatements().clear();
      }
    }
    // Copy static JsOverlay fields.
    for (Field field : type.getFields()) {
      if (field.getDescriptor().isJsOverlay()) {
        checkState(field.getDescriptor().isStatic());
        overlayClass.addField(
            Field.Builder.from(field).enclosingClass(overlayImplTypeDescriptor).build());
      }
    }
    return overlayClass;
  }
}
