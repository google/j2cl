package com.google.j2cl.ast.visitors;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds locations where implicit casts should occur and makes them explicit.
 * <p>
 * Currently handles longs.
 */
// TODO: general method call params, String + operator, field/variable initializers, and numeric
// assignment/non-assignment binary operations.
public class InsertImplicitCastsVisitor extends AbstractRewriter {

  private static final TypeDescriptor LONG = TypeDescriptors.get().primitiveLong;

  public static void applyTo(CompilationUnit compilationUnit) {
    new InsertImplicitCastsVisitor().insertImplicitCasts(compilationUnit);
  }

  private void insertImplicitCasts(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
    if (AstUtils.isAssignmentOperator(binaryExpression.getOperator())) {
      // =, +=, -=, etc
      return rewriteAssignmentBinaryExpression(binaryExpression);
    } else {
      // +, -, etc
      return rewriteNonAssignmentBinaryExpression(binaryExpression);
    }
  }

  @Override
  public Node rewriteField(Field field) {
    if (field.getDescriptor().getTypeDescriptor() == LONG) {
      return rewriteLongField(field);
    }
    return field;
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    ImmutableList<TypeDescriptor> parameterTypeDescriptors =
        methodCall.getTarget().getParameterTypeDescriptors();
    List<Expression> arguments = methodCall.getArguments();

    // Look at each param/argument pair.
    List<Expression> newArguments = new ArrayList<>();
    for (int argIndex = 0; argIndex < parameterTypeDescriptors.size(); argIndex++) {
      TypeDescriptor parameterTypeDescriptor = parameterTypeDescriptors.get(argIndex);
      newArguments.add(maybeCastTo(arguments.get(argIndex), parameterTypeDescriptor));
    }

    return new MethodCall(methodCall.getQualifier(), methodCall.getTarget(), newArguments);
  }

  @Override
  public Node rewriteNewInstance(NewInstance newInstance) {
    ImmutableList<TypeDescriptor> parameterTypeDescriptors =
        newInstance.getTarget().getParameterTypeDescriptors();
    List<Expression> arguments = newInstance.getArguments();

    // Look at each param/argument pair.
    List<Expression> newArguments = new ArrayList<>();
    for (int argIndex = 0; argIndex < parameterTypeDescriptors.size(); argIndex++) {
      TypeDescriptor parameterTypeDescriptor = parameterTypeDescriptors.get(argIndex);
      newArguments.add(maybeCastTo(arguments.get(argIndex), parameterTypeDescriptor));
    }

    return new NewInstance(newInstance.getQualifier(), newInstance.getTarget(), newArguments);
  }

  @Override
  public Node rewriteReturnStatement(ReturnStatement returnStatement) {
    return new ReturnStatement(
        maybeCastTo(returnStatement.getExpression(), returnStatement.getTypeDescriptor()),
        returnStatement.getTypeDescriptor());
  }

  @Override
  public Node rewriteVariableDeclarationFragment(
      VariableDeclarationFragment variableDeclarationFragment) {
    if (variableDeclarationFragment.getVariable().getTypeDescriptor() == LONG) {
      return rewriteLongVariableDeclarationFragment(variableDeclarationFragment);
    }
    return variableDeclarationFragment;
  }

  private Node rewriteAssignmentBinaryExpression(BinaryExpression binaryExpression) {
    TypeDescriptor leftTypeDescriptor = binaryExpression.getLeftOperand().getTypeDescriptor();
    if (leftTypeDescriptor == LONG) {
      return rewriteLongAssignmentBinaryExpression(binaryExpression);
    }
    return binaryExpression;
  }

  private Node rewriteLongAssignmentBinaryExpression(BinaryExpression binaryExpression) {
    if (AstUtils.isShiftOperator(binaryExpression.getOperator())) {
      // promote long += int -> long += long
      return new BinaryExpression(
          binaryExpression.getTypeDescriptor(),
          binaryExpression.getLeftOperand(),
          binaryExpression.getOperator(),
          maybeCastToLong(binaryExpression.getRightOperand()));
    } else if (binaryExpression.getRightOperand().getTypeDescriptor() == LONG) {
      // demote long <<= long -> long <<= int
      // This demotion isn't done to match the Java spec, it's done to match the Longs.rightShift()
      // etc API which does not accept a long in that function parameter slot.
      return new BinaryExpression(
          binaryExpression.getTypeDescriptor(),
          maybeCastToLong(binaryExpression.getLeftOperand()),
          binaryExpression.getOperator(),
          maybeCastTo(binaryExpression.getRightOperand(), TypeDescriptors.get().primitiveInt));
    } else {
      return binaryExpression;
    }
  }

  private Node rewriteLongNonAssignmentBinaryExpression(BinaryExpression binaryExpression) {
    if (AstUtils.isShiftOperator(binaryExpression.getOperator())) {
      // promote long + int -> long + long
      return new BinaryExpression(
          binaryExpression.getTypeDescriptor(),
          maybeCastToLong(binaryExpression.getLeftOperand()),
          binaryExpression.getOperator(),
          maybeCastToLong(binaryExpression.getRightOperand()));
    } else if (binaryExpression.getRightOperand().getTypeDescriptor() == LONG) {
      // demote long << long -> long << int
      return new BinaryExpression(
          binaryExpression.getTypeDescriptor(),
          maybeCastToLong(binaryExpression.getLeftOperand()),
          binaryExpression.getOperator(),
          maybeCastTo(binaryExpression.getRightOperand(), TypeDescriptors.get().primitiveInt));
    } else {
      return binaryExpression;
    }
  }

  private Node rewriteLongField(Field field) {
    Field newField = new Field(field.getDescriptor(), maybeCastToLong(field.getInitializer()));
    newField.setCapturedVariable(field.getCapturedVariable());
    newField.setCompileTimeConstant(field.isCompileTimeConstant());
    return newField;
  }

  private Node rewriteNonAssignmentBinaryExpression(BinaryExpression binaryExpression) {
    if (binaryExpression.getTypeDescriptor() == LONG) {
      return rewriteLongNonAssignmentBinaryExpression(binaryExpression);
    }
    return binaryExpression;
  }

  private Node rewriteLongVariableDeclarationFragment(
      VariableDeclarationFragment variableDeclarationFragment) {
    // promote long foo = int; to long foo = long;
    return new VariableDeclarationFragment(
        variableDeclarationFragment.getVariable(),
        maybeCastToLong(variableDeclarationFragment.getInitializer()));
  }

  public static Expression maybeCastToLong(Expression expression) {
    return maybeCastTo(expression, LONG);
  }

  /**
   * Returns either the provided expressions or a wrapped version of it that has been cast to the
   * provided destination type.
   * <p>
   * The cast is omitted either if it is redundant or it is known that it would be removed by the
   * following NormalizeCastsVisitor's filter.
   */
  private static Expression maybeCastTo(Expression expression, TypeDescriptor toTypeDescriptor) {
    if (expression == null) {
      // Can't cast a null expression.
      return expression;
    }
    TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();
    if (!fromTypeDescriptor.isPrimitive() || !toTypeDescriptor.isPrimitive()) {
      // Non-primitives don't get implicit casts.
      return expression;
    }
    if (fromTypeDescriptor == toTypeDescriptor) {
      // If the type didn't change there's no need for a cast.
      return expression;
    }
    if (AstUtils.canRemoveCast(expression.getTypeDescriptor(), toTypeDescriptor)) {
      // If a cast would make no difference (because both types are both already encoded as JS
      // number primitives and have compatible sign and truncation conventions) then don't bother
      // emitting a cast.
      return expression;
    }

    return new CastExpression(expression, toTypeDescriptor);
  }
}
