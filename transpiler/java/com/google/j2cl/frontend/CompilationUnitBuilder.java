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

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JavaType.Kind;
import com.google.j2cl.ast.TypeReference;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Creates a J2CL Java AST from the AST provided by JDT.
 */
public class CompilationUnitBuilder {

  private class Builder extends ASTVisitor {
    @Override
    public boolean visit(TypeDeclaration node) {
      ITypeBinding typeBinding = node.resolveBinding();
      JavaType type = createJavaType(typeBinding);
      j2clCompilationUnit.addType(type);
      return super.visit(node);
    }

    private JavaType createJavaType(ITypeBinding typeBinding) {
      if (typeBinding == null) {
        return null;
      }
      JavaType type =
          new JavaType(
              typeBinding.isInterface() ? Kind.INTERFACE : Kind.CLASS,
              JdtUtils.getVisibility(typeBinding.getModifiers()),
              JdtUtils.createTypeReference(typeBinding));

      ITypeBinding superclassBinding = typeBinding.getSuperclass();
      TypeReference superType = JdtUtils.createTypeReference(superclassBinding);
      type.setSuperType(superType);
      return type;
    }
  }

  private CompilationUnit j2clCompilationUnit;

  public static CompilationUnit build(
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit) {
    return new CompilationUnitBuilder().buildCompilationUnit(sourceFilePath, jdtCompilationUnit);
  }

  private CompilationUnit buildCompilationUnit(
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit) {
    Builder builder = new Builder();

    String packageName = JdtUtils.getCompilationUnitPackageName(jdtCompilationUnit);
    j2clCompilationUnit = new CompilationUnit(sourceFilePath, packageName);

    jdtCompilationUnit.accept(builder);
    return j2clCompilationUnit;
  }

  private CompilationUnitBuilder() {}
}
