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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.JsTypeAnnotation;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Inserts a cast when there is a mismatch in nullability in the return value of a method, a
 * parameter of a method/constructor or a field assignment. This is necessary since some libraries
 * are not annotated for nullability and the type analysis in the generated JS might fail to infer
 * that some variable is non nullable.
 */
// TODO(simionato): Add a cast when initializing arrays.
// TODO(simionato): Handle method calls with erased types, for example
// List<@NotNull String> x; x.add(nullableString) requires cast.
public class InsertCastOnNullabilityMismatch extends AbstractRewriter {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new InsertCastOnNullabilityMismatch());
  }

  @Override
  public Node rewriteField(Field field) {
    if (field.hasInitializer()) {
      Expression initializer = field.getInitializer();
      TypeDescriptor fieldType = field.getDescriptor().getTypeDescriptor();
      TypeDescriptor assignedType = initializer.getTypeDescriptor();
      TypeDescriptor fixedType = getTypeWithMatchingNullability(fieldType, assignedType);
      if (fixedType != assignedType) {
        return Field.Builder.from(field)
            .setInitializer(JsTypeAnnotation.createTypeAnnotation(initializer, fixedType))
            .build();
      }
    }
    return field;
  }

  @Override
  public Node rewriteVariableDeclarationFragment(VariableDeclarationFragment declarationFragment) {
    if (declarationFragment.getInitializer() == null) {
      return declarationFragment;
    }
    TypeDescriptor variableType = declarationFragment.getVariable().getTypeDescriptor();
    TypeDescriptor assignedType = declarationFragment.getInitializer().getTypeDescriptor();
    TypeDescriptor fixedType = getTypeWithMatchingNullability(variableType, assignedType);
    if (fixedType != assignedType) {
      declarationFragment.setInitializer(
          JsTypeAnnotation.createTypeAnnotation(declarationFragment.getInitializer(), fixedType));
    }
    return declarationFragment;
  }

  @Override
  public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
    if (binaryExpression.getOperator() == BinaryOperator.ASSIGN) {
      Expression leftOperand = binaryExpression.getLeftOperand();
      Expression rightOperand = binaryExpression.getRightOperand();
      TypeDescriptor lhsType = leftOperand.getTypeDescriptor();
      TypeDescriptor assignedType = rightOperand.getTypeDescriptor();
      TypeDescriptor fixedType = getTypeWithMatchingNullability(lhsType, assignedType);
      if (fixedType != assignedType) {
        binaryExpression.setRightOperand(
            JsTypeAnnotation.createTypeAnnotation(rightOperand, fixedType));
      }
    }
    return binaryExpression;
  }

  @Override
  public Node rewriteInvocation(Invocation invocation) {
    List<TypeDescriptor> parameterTypes = invocation.getTarget().getParameterTypeDescriptors();
    List<Expression> arguments = invocation.getArguments();

    for (int i = 0; i < arguments.size(); i++) {
      // Use the last parameter type if there are more arguments than parameters, to account for
      // varargs methods.
      TypeDescriptor parameterType =
          i < parameterTypes.size()
              ? parameterTypes.get(i)
              : parameterTypes.get(parameterTypes.size() - 1);
      TypeDescriptor argumentType = arguments.get(i).getTypeDescriptor();

      TypeDescriptor fixedType = getTypeWithMatchingNullability(parameterType, argumentType);
      if (fixedType != argumentType) {
        arguments.set(i, JsTypeAnnotation.createTypeAnnotation(arguments.get(i), fixedType));
      }
    }
    return invocation;
  }

  @Override
  public Node rewriteReturnStatement(ReturnStatement returnStatement) {
    if (returnStatement.getExpression() == null) {
      // Void method.
      return returnStatement;
    }

    TypeDescriptor methodReturnType = returnStatement.getTypeDescriptor();
    TypeDescriptor actualReturnType = returnStatement.getExpression().getTypeDescriptor();
    TypeDescriptor fixedType = getTypeWithMatchingNullability(methodReturnType, actualReturnType);

    if (fixedType != actualReturnType) {
      returnStatement.setExpression(
          JsTypeAnnotation.createTypeAnnotation(returnStatement.getExpression(), fixedType));
    }
    return returnStatement;
  }

  /**
   * Returns a modified version of actualType so that its nullability matches the nullability of
   * requiredType. For example, if requiredType is "!Number" and actualType is "?Integer", this
   * method will return "!Integer'.
   */
  private TypeDescriptor getTypeWithMatchingNullability(
      TypeDescriptor requiredType, TypeDescriptor actualType) {
    if (actualType.equalsIgnoreNullability(TypeDescriptors.get().javaLangObject)) {
      // Object is exported as the all type, so there is no point in casting it.
      return actualType;
    }
    if (requiredType.isTypeVariable()) {
      return actualType;
    }

    if (actualType.isJsFunctionInterface() || actualType.isJsFunctionImplementation()) {
      // TODO(simionato): Examine function parameters/return type to produce the appropriate cast.
      if (!requiredType.isNullable() && actualType.isNullable()) {
        return TypeDescriptors.toNonNullable(actualType);
      }
    }

    if (requiredType.isArray() && actualType.isArray()) {
      TypeDescriptor requiredLeafType = requiredType.getLeafTypeDescriptor();
      TypeDescriptor actualLeafType = actualType.getLeafTypeDescriptor();
      TypeDescriptor fixedLeafType =
          getTypeWithMatchingNullability(requiredLeafType, actualLeafType);
      if (actualLeafType != fixedLeafType) {
        actualType =
            TypeDescriptors.getForArray(
                fixedLeafType, actualType.getDimensions(), actualType.isNullable());
      }
    } else if (requiredType.isParameterizedType()) {
      TypeDescriptor matchingType =
          TypeDescriptors.getMatchingTypeInHierarchy(actualType, requiredType);
      if (matchingType == null) {
        // No type match. Can happen for example for the type of 'null'.
        return actualType;
      }
      // Make every type parameter match.
      List<TypeDescriptor> requiredTypeArguments = requiredType.getTypeArgumentDescriptors();
      List<TypeDescriptor> matchingTypeArguments = matchingType.getTypeArgumentDescriptors();
      List<TypeDescriptor> fixedTypeArguments = new ArrayList<>();
      if (requiredTypeArguments.size() == matchingTypeArguments.size()) {
        for (int i = 0; i < requiredTypeArguments.size(); i++) {
          fixedTypeArguments.add(
              getTypeWithMatchingNullability(
                  requiredTypeArguments.get(i), matchingTypeArguments.get(i)));
        }
        if (!fixedTypeArguments.equals(matchingTypeArguments)) {
          actualType =
              TypeDescriptors.replaceTypeArgumentDescriptors(matchingType, fixedTypeArguments);
        }
      } else {
        // The number of type parameters is different. This can happen when there is a raw type in
        // Java, e.g: foo((List) a). No casting should be needed in this case.
      }
    }

    if (!requiredType.isNullable() && actualType.isNullable()) {
      return TypeDescriptors.toNonNullable(actualType);
    }
    return actualType;
  }
}
