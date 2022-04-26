/*
 * Copyright 2021 Google Inc.
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.JsDocExpression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.VariableReference;

/** Optimize enums by initializing enum constant out of the clinit. */
public class OptimizeEnums extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.getTypes().stream()
        .filter(OptimizeEnums::isOptimizeableEnum)
        .forEach(OptimizeEnums::optimizeEnum);
  }

  private static void optimizeEnum(Type type) {
    type.setOptimizedEnum(true);
    markEnumConstantsAsCompileTimeConstants(type);
    annotateNewInstancesAsPure(type);
  }

  /**
   * By marking enum constants as compile time constants, their initialization will be done at load
   * time instead of during the class initilization. References to enum constants will not require a
   * call to the clinit method anymore and JsCompiler will be able to remove the clinit if there are
   * no other static initializations.
   */
  private static void markEnumConstantsAsCompileTimeConstants(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteField(Field field) {
            if (!field.isEnumField()) {
              return field;
            }
            return Field.Builder.from(field)
                .setDescriptor(markFieldDescriptorAsCompileTimeConstant(field.getDescriptor()))
                .build();
          }

          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            if (!fieldAccess.getTarget().isEnumConstant()
                || !type.getTypeDescriptor()
                    .hasSameRawType(fieldAccess.getTarget().getTypeDescriptor())) {
              return fieldAccess;
            }
            // Rewrite accesses to enum fields inside the enum because they don't use getter method
            // and the name of the field changed as we made the enum fields compile time constant.
            return FieldAccess.Builder.from(fieldAccess)
                .setTarget(markFieldDescriptorAsCompileTimeConstant(fieldAccess.getTarget()))
                .build();
          }

          private FieldDescriptor markFieldDescriptorAsCompileTimeConstant(
              FieldDescriptor fieldDescriptor) {
            return FieldDescriptor.Builder.from(fieldDescriptor)
                .setCompileTimeConstant(true)
                .build();
          }
        });
  }

  /**
   * Enum constant initializations will be part of the load time statements of the enum class. In
   * order CrossChunkCodeMotion to move them close where they are used, we need to annotated them as
   * "pure" so JsCompiler knows it can freely move them.
   */
  private static void annotateNewInstancesAsPure(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            if (!newInstance.getTypeDescriptor().hasSameRawType(type.getTypeDescriptor())) {
              return newInstance;
            }

            return JsDocExpression.newBuilder()
                .setAnnotation("pureOrBreakMyCode")
                .setExpression(newInstance)
                .build();
          }
        });
  }

  private static boolean isOptimizeableEnum(Type type) {
    return type.isEnum()
        && !type.isJsEnum()
        && hasTrivialInitialization(type)
        && hasTrivialConstructors(type)
        && !hasSubclasses(type);
  }

  private static boolean hasTrivialInitialization(Type type) {
    // Initializer blocks are mostly used to introduce logic for initializing fields. If they are
    // present, we don't optimize the enum.
    if (!type.getInstanceInitializerBlocks().isEmpty()) {
      return false;
    }

    // check that if instance fields are initialized, they are initialized to constants.
    if (!type.getInstanceFields().stream()
        .filter(Field::hasInitializer)
        .map(Field::getInitializer)
        .allMatch(Expression::isCompileTimeConstant)) {
      return false;
    }

    // We can only optimize enums where compile time constants are used to initialize enum fields.
    return type.getEnumFields().stream()
        .map(Field::getInitializer)
        // We only expect ctor calls.
        .map(NewInstance.class::cast)
        .flatMap(c -> c.getArguments().stream())
        .allMatch(Expression::isCompileTimeConstant);
  }

  private static boolean hasTrivialConstructors(Type type) {
    for (Method ctor : type.getConstructors()) {
      // As constructors will be invoked at load time, we need to check if ctors contains statements
      // that are safe to be executed at load time. The only load time safe statements we accept is
      // this() or super() calls and instance fields assignments.

      for (Statement s : ctor.getBody().getStatements()) {
        if (s instanceof ExpressionStatement) {
          Expression expression = ((ExpressionStatement) s).getExpression();
          if (isTrivialThisCall(expression) || isTrivialFieldAssignment(expression)) {
            continue;
          }
        }
        return false;
      }
    }
    return true;
  }

  private static boolean isTrivialThisCall(Expression expression) {
    if (!(expression instanceof MethodCall)) {
      return false;
    }

    MethodCall methodCall = (MethodCall) expression;
    if (!methodCall.getTarget().isConstructor()) {
      return false;
    }
    // call to super() or this() accepted if they are called with compile time constants or
    // constructor's parameters as arguments.
    return methodCall.getArguments().stream()
        .allMatch(OptimizeEnums::isCompileTimeConstantExpressionOrVariableReference);
  }

  private static boolean isTrivialFieldAssignment(Expression expression) {
    if (!expression.isSimpleAssignment()) {
      return false;
    }
    BinaryExpression binaryExpression = (BinaryExpression) expression;
    Expression left = binaryExpression.getLeftOperand();
    Expression right = binaryExpression.getRightOperand();
    if (!(left instanceof FieldAccess
        && ((FieldAccess) left).getQualifier() instanceof ThisReference)) {
      return false;
    }
    // instance field assignments are allowed as long as they are a compile time constant or a
    // variable reference.
    return isCompileTimeConstantExpressionOrVariableReference(right);
  }

  private static boolean hasSubclasses(Type type) {
    // if one enum initializer is not type of the enum, it means the enum is extended (one of the
    // enum constant has a class body) and we cannot optimize it.
    return type.getEnumFields().stream()
        .map(e -> checkNotNull(e.getInitializer()).getTypeDescriptor())
        .anyMatch(t -> !t.hasSameRawType(type.getTypeDescriptor()));
  }

  private static boolean isCompileTimeConstantExpressionOrVariableReference(Expression expression) {
    return expression.isCompileTimeConstant() || expression instanceof VariableReference;
  }
}
