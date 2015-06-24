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
package com.google.j2cl.generator;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractTransformer;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldReference;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.ParenthesizedExpression;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.VariableDeclaration;
import com.google.j2cl.ast.VariableReference;

import java.util.List;

/**
 * Generate javascript source code for {@code Statement}.
 * TODO: Keep an eye on performance and if things get slow then replace these String operations with
 * a StringBuilder.
 */
public class StatementSourceGenerator {
  // TODO: static field may be a potential threading problem.
  private static TypeReference inClinitForType = null;

  public static String toSource(Node node) {
    class ToSourceTransformer extends AbstractTransformer<String> {
      @Override
      public String transformAssertStatement(AssertStatement statement) {
        if (statement.getMessage() == null) {
          return String.format(
              "Asserts.$enabled() && Asserts.$assert(%s);", toSource(statement.getExpression()));
        } else {
          return String.format(
              "Asserts.$enabled() && Asserts.$assertWithMessage(%s, %s);",
              toSource(statement.getExpression()),
              toSource(statement.getMessage()));
        }
      }

      @Override
      public String transformBinaryExpression(BinaryExpression expression) {
        return String.format(
            "%s %s %s",
            toSource(expression.getLeftOperand()),
            expression.getOperator().toString(),
            toSource(expression.getRightOperand()));
      }

      @Override
      public String transformBooleanLiteral(BooleanLiteral expression) {
        return expression.getValue() ? "true" : "false";
      }

      @Override
      public String transformExpressionStatement(ExpressionStatement statement) {
        return toSource(statement.getExpression()) + ";";
      }

      @Override
      public String transformFieldAccess(FieldAccess fieldAccess) {
        FieldReference target = fieldAccess.getField();
        String fieldMangledName =
            ManglingNameUtils.getMangledName(
                target, isInClinit(target.getEnclosingClassReference()));

        // make 'this.' reference and static reference explicit.
        // TODO(rluble): We should probably make this explicit at the AST level, either by a
        // normalization pass or by construction.
        String qualifier =
            fieldAccess.getQualifier() == null
                ? (target.isStatic()
                    ? TranspilerUtils.getClassName(target.getEnclosingClassReference())
                    : "this")
                : toSource(fieldAccess.getQualifier());
        return String.format("%s.%s", qualifier, fieldMangledName);
      }

      @Override
      public String transformInstanceOfExpression(InstanceOfExpression expression) {
        TypeReference checkType = expression.getTestTypeRef();
        if (checkType.isArray()) {
          throw new RuntimeException(
              "TODO: Implement toSource() for instanceOf ArrayTypeReference");
        }
        return String.format(
            "%s.$isInstance(%s)",
            TranspilerUtils.getClassName(checkType),
            toSource(expression.getExpression()));
      }

      @Override
      public String transformNewArray(NewArray expression) {
        String dimensionsList =
            Joiner.on(", ").join(transformNodesToSource(expression.getDimensionExpressions()));
        String leafTypeName = TranspilerUtils.getClassName(expression.getLeafTypeRef());
        return String.format("Arrays.$create([%s], %s)", dimensionsList, leafTypeName);
      }

      @Override
      public String transformNewInstance(NewInstance expression) {
        String className =
            TranspilerUtils.getClassName(expression.getConstructorRef().getEnclosingClassRef());
        String constructorMangledName =
            ManglingNameUtils.getConstructorMangledName(expression.getConstructorRef());
        String argumentsList =
            Joiner.on(", ").join(transformNodesToSource(expression.getArguments()));
        return String.format("%s.%s(%s)", className, constructorMangledName, argumentsList);
      }

      @Override
      public String transformNullLiteral(NullLiteral expression) {
        return "null";
      }

      @Override
      public String transformNumberLiteral(NumberLiteral expression) {
        return expression.getToken();
      }

      @Override
      public String transformParenthesizedExpression(ParenthesizedExpression expression) {
        return String.format("(%s)", toSource(expression.getExpression()));
      }

      @Override
      public String transformPostfixExpression(PostfixExpression expression) {
        return String.format(
            "%s%s", toSource(expression.getOperand()), expression.getOperator().toString());
      }

      @Override
      public String transformPrefixExpression(PrefixExpression expression) {
        return String.format(
            "%s%s", expression.getOperator().toString(), toSource(expression.getOperand()));
      }

      @Override
      public String transformThisReference(ThisReference expression) {
        // We expect that after normalization (InnerClassExtractor) there should be no qualified
        // this reference.
        Preconditions.checkArgument(
            expression.getTypeRef() == null,
            "There should be no qualified thisRef after normalization.");
        return "this";
      }

      @Override
      public String transformTypeReference(TypeReference typeRef) {
        return TranspilerUtils.getClassName(typeRef);
      }

      @Override
      public String transformVariableDeclaration(VariableDeclaration statement) {
        return String.format(
            "var %s = %s;",
            statement.getVariable().getName(),
            toSource(statement.getInitializer()));
      }

      @Override
      public String transformVariableReference(VariableReference expression) {
        return expression.getTarget().getName();
      }
    }
    return new ToSourceTransformer().process(node);
  }

  public static List<String> transformNodesToSource(List<? extends Node> nodes) {
    return Lists.transform(
        nodes,
        new Function<Node, String>() {
          @Override
          public String apply(Node node) {
            return toSource(node);
          }
        });
  }

  public static void setInClinit(TypeReference type) {
    inClinitForType = type;
  }

  public static void resetInClinit() {
    inClinitForType = null;
  }

  private static boolean isInClinit(TypeReference type) {
    return type.equals(inClinitForType);
  }
}
