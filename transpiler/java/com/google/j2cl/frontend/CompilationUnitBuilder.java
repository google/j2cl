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
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JavaType.Kind;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.UnknownNode;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.errors.Errors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Creates a J2CL Java AST from the AST provided by JDT.
 */
public class CompilationUnitBuilder {

  private class Builder extends ASTVisitor {
    private JavaType currentType;
    private List<Node> nodeStack = new ArrayList<>();

    private void push(Node node) {
      nodeStack.add(node);
    }

    private Node pop() {
      return nodeStack.remove(nodeStack.size() - 1);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
      ITypeBinding typeBinding = node.resolveBinding();
      JavaType type = createJavaType(typeBinding);
      currentType = type;
      j2clCompilationUnit.addType(type);
      return super.visit(node);
    }

    @Override
    public void endVisit(TypeDeclaration node) {
      currentType = null;
    }

    @Override
    public void endVisit(FieldDeclaration node) {
      for (Object object : node.fragments()) {
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
        Expression initializer = fragment.getInitializer() == null ? null : (Expression) pop();
        currentType.addField(
            new Field(
                JdtUtils.createFieldReference(
                    fragment.resolveBinding(), compilationUnitNameLocator),
                initializer));
      }
    }

    @Override
    public void endVisit(MethodDeclaration node) {
      // TODO: resolve the method body.
      List<Variable> parameters = new ArrayList<>();
      for (Object element : node.parameters()) {
        SingleVariableDeclaration parameter = (SingleVariableDeclaration) element;
        parameters.add(
            JdtUtils.createVariable(parameter.resolveBinding(), compilationUnitNameLocator));
      }
      currentType.addMethod(
          new Method(
              JdtUtils.createMethodReference(node.resolveBinding(), compilationUnitNameLocator),
              parameters,
              new UnknownNode()));
    }

    @Override
    public void endVisit(org.eclipse.jdt.core.dom.NumberLiteral node) {
      push(new NumberLiteral(node.getToken()));
    }

    private JavaType createJavaType(ITypeBinding typeBinding) {
      if (typeBinding == null) {
        return null;
      }
      JavaType type =
          new JavaType(
              typeBinding.isInterface() ? Kind.INTERFACE : Kind.CLASS,
              JdtUtils.getVisibility(typeBinding.getModifiers()),
              createTypeReference(typeBinding));

      ITypeBinding superclassBinding = typeBinding.getSuperclass();
      TypeReference superType = createTypeReference(superclassBinding);
      type.setSuperType(superType);
      return type;
    }
  }

  private CompilationUnit j2clCompilationUnit;
  private CompilationUnitNameLocator compilationUnitNameLocator;

  private CompilationUnit buildCompilationUnit(
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit) {
    Builder builder = new Builder();

    String packageName = JdtUtils.getCompilationUnitPackageName(jdtCompilationUnit);
    j2clCompilationUnit = new CompilationUnit(sourceFilePath, packageName);

    jdtCompilationUnit.accept(builder);
    return j2clCompilationUnit;
  }

  private TypeReference createTypeReference(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }
    TypeReference typeReference =
        JdtUtils.createTypeReference(typeBinding, compilationUnitNameLocator);
    return typeReference;
  }

  public static List<CompilationUnit> build(
      Map<String, org.eclipse.jdt.core.dom.CompilationUnit> jdtUnitsByFilePath,
      FrontendOptions options,
      Errors errors) {
    CompilationUnitBuilder compilationUnitBuilder =
        new CompilationUnitBuilder(
            new CompilationUnitNameLocator(jdtUnitsByFilePath, options, errors));

    List<CompilationUnit> compilationUnits = new ArrayList<>();
    for (Entry<String, org.eclipse.jdt.core.dom.CompilationUnit> entry :
        jdtUnitsByFilePath.entrySet()) {
      compilationUnits.add(
          compilationUnitBuilder.buildCompilationUnit(entry.getKey(), entry.getValue()));
    }
    return compilationUnits;
  }

  private CompilationUnitBuilder(CompilationUnitNameLocator compilationUnitNameLocator) {
    this.compilationUnitNameLocator = compilationUnitNameLocator;
  }
}
