/*
 * Copyright 2020 Google Inc.
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
package com.google.j2cl.transpiler.backend.wasm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveVoid;
import static com.google.j2cl.transpiler.backend.wasm.GenerationEnvironment.getGetterInstruction;
import static java.lang.String.format;

import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionWithComment;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.ThisOrSuperReference;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.UnaryExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.VariableReference;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms expressions into WASM code.
 *
 * <p>As is typical in stack based VMs, expressions evaluate leaving the result in the stack.
 */
final class ExpressionTranspiler {

  public static void renderWithUnusedResult(
      Expression expression, SourceBuilder sourceBuilder, GenerationEnvironment environment) {
    if (returnsVoid(expression)) {
      render(expression, sourceBuilder, environment);
    } else {
      sourceBuilder.append("(drop ");
      render(expression, sourceBuilder, environment);
      sourceBuilder.append(")");
    }
  }

  public static void render(
      Expression expression,
      final SourceBuilder sourceBuilder,
      final GenerationEnvironment environment) {

    new AbstractVisitor() {
      @Override
      public boolean enterBooleanLiteral(BooleanLiteral booleanLiteral) {
        sourceBuilder.append("(i32.const " + (booleanLiteral.getValue() ? "1" : "0") + ")");
        return false;
      }

      @Override
      public boolean enterBinaryExpression(BinaryExpression expression) {
        if (expression.isSimpleAssignment()) {
          return renderAssignment(expression.getLeftOperand(), expression.getRightOperand());
        }

        renderBinaryOperation(expression);
        return false;
      }

      private void renderBinaryOperation(BinaryExpression expression) {
        WasmBinaryOperation wasmOperation = WasmBinaryOperation.getOperation(expression);

        sourceBuilder.append("(" + wasmOperation.getInstruction(expression) + " ");
        render(expression.getLeftOperand());
        sourceBuilder.append(" ");
        render(expression.getRightOperand());
        sourceBuilder.append(")");
      }

      @CanIgnoreReturnValue
      private boolean renderAssignment(Expression left, Expression right) {
        sourceBuilder.append("(");
        renderAccessExpression(left, /*setter=*/ true);
        sourceBuilder.append(" ");
        render(right);
        sourceBuilder.append(")");
        return false;
      }

      private void renderAccessExpression(Expression expression, boolean setter) {
        if (expression instanceof VariableReference) {
          sourceBuilder.append(
              format(
                  "local.%s %s",
                  setter ? "set" : "get",
                  environment.getDeclarationName(((VariableReference) expression).getTarget())));

        } else if (expression instanceof FieldAccess) {
          FieldDescriptor fieldDescriptor = ((FieldAccess) expression).getTarget();
          Expression qualifier = ((FieldAccess) expression).getQualifier();
          if (fieldDescriptor.isStatic()) {
            sourceBuilder.append(
                format(
                    "global.%s %s",
                    setter ? "set" : "get", environment.getFieldName(fieldDescriptor)));
          } else {
            sourceBuilder.append(
                format(
                    "struct.%s %s %s",
                    setter ? "set" : getGetterInstruction(fieldDescriptor.getTypeDescriptor()),
                    environment.getWasmTypeName(fieldDescriptor.getEnclosingTypeDescriptor()),
                    environment.getFieldName(fieldDescriptor)));
            render(qualifier);
          }

        } else if (expression instanceof ArrayAccess) {
          ArrayAccess arrayAccess = (ArrayAccess) expression;
          Expression arrayExpression = arrayAccess.getArrayExpression();

          sourceBuilder.append(
              format(
                  "array.%s %s ",
                  setter ? "set" : getGetterInstruction(arrayAccess.getTypeDescriptor()),
                  environment.getWasmTypeName(arrayExpression.getTypeDescriptor())));
          render(arrayExpression);
          sourceBuilder.append(" ");
          render(arrayAccess.getIndexExpression());
        }
      }

      @Override
      public boolean enterArrayAccess(ArrayAccess arrayAccess) {
        sourceBuilder.append("(");
        renderAccessExpression(arrayAccess, /*setter=*/ false);
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterArrayLength(ArrayLength arrayLength) {
        sourceBuilder.append(
            format(
                "(array.len %s ",
                environment.getWasmTypeName(arrayLength.getArrayExpression().getTypeDescriptor())));
        render(arrayLength.getArrayExpression());
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterCastExpression(CastExpression castExpression) {
        TypeDescriptor castTypeDescriptor =
            castExpression.getCastTypeDescriptor().toRawTypeDescriptor();

        if (TypeDescriptors.isJavaLangObject(castTypeDescriptor)) {
          // Avoid unncessary casts to j.l.Object. This also helps avoiding a WASM cast whem going
          // from native arrays to WasmOpaque objects (and vice versa) which would otherwise fail.
          render(castExpression.getExpression());
          return false;
        }

        if (castTypeDescriptor.isInterface()) {
          // TODO(b/183769034): At the moment the actual cast check is performed at interface
          // method call. Depending on whether WASM NPEs become catchable there might need to be
          // instrumentation code here.
          render(castExpression.getExpression());
          return false;
        }

        // TODO(b/184675805): implement array cast expressions beyond this nominal
        // implementation.
        sourceBuilder.append(
            format("(ref.cast_static %s ", environment.getWasmTypeName(castTypeDescriptor)));
        render(castExpression.getExpression());
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterJsDocCastExpression(JsDocCastExpression expression) {
        // TODO(b/183661534): JsDocCastExpressions should not reach the output stage.
        // Render JsDoc casts as regular casts for now.
        return enterCastExpression(
            CastExpression.newBuilder()
                .setExpression(expression.getExpression())
                .setCastTypeDescriptor(expression.getTypeDescriptor())
                .build());
      }

      @Override
      public boolean enterConditionalExpression(ConditionalExpression conditionalExpression) {
        TypeDescriptor typeDescriptor = conditionalExpression.getTypeDescriptor();
        sourceBuilder.append("(if (result " + environment.getWasmType(typeDescriptor) + ") ");
        render(conditionalExpression.getConditionExpression());
        sourceBuilder.append(" (then ");
        render(conditionalExpression.getTrueExpression());
        sourceBuilder.append(") (else ");
        render(conditionalExpression.getFalseExpression());
        sourceBuilder.append("))");
        return false;
      }

      @Override
      public boolean enterFieldAccess(FieldAccess fieldAccess) {
        sourceBuilder.append("(");
        renderAccessExpression(fieldAccess, /*setter=*/ false);
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterExpressionWithComment(ExpressionWithComment expressionWithComment) {
        sourceBuilder.append(";; " + expressionWithComment.getComment());
        render(expressionWithComment.getExpression());
        return false;
      }

      @Override
      public boolean enterInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
        TypeDescriptor testTypeDescriptor = instanceOfExpression.getTestTypeDescriptor();
        // TODO(b/184675805): implement array instanceof expressions beyond this nominal
        // implementation.
        if (!testTypeDescriptor.isInterface()) {
          sourceBuilder.append(
              format("(ref.test_static %s ", environment.getWasmTypeName(testTypeDescriptor)));
          render(instanceOfExpression.getExpression());
          sourceBuilder.append(")");
        } else {
          DeclaredTypeDescriptor targetTypeDescriptor = (DeclaredTypeDescriptor) testTypeDescriptor;
          int interfaceSlot =
              environment.getInterfaceSlot(targetTypeDescriptor.getTypeDeclaration());
          if (interfaceSlot == -1) {
            // The interface does not have implementors, hence instanceof is false.
            sourceBuilder.append("(i32.const 0)");
            return false;
          }
          sourceBuilder.append("(if (result i32) (ref.is_null ");
          render(instanceOfExpression.getExpression());
          sourceBuilder.append(")");
          sourceBuilder.indent();
          sourceBuilder.newLine();
          // Reference is null, return false.
          sourceBuilder.append("(then (i32.const 0))");
          sourceBuilder.newLine();
          sourceBuilder.append("(else (if (result i32)");
          // Classes have itables that are large enough to contain the highest slot of its
          // implemented interfaces. For example a class that does not implement any interface will
          // have an itable of size 0. So given an interface slot, we first need to see if the slot
          // is past the end of the itable.
          sourceBuilder.indent();
          sourceBuilder.newLine();
          sourceBuilder.append(
              String.format(
                  "(i32.ge_u (i32.const %d) (array.len $itable (struct.get $java.lang.Object"
                      + " $itable ",
                  interfaceSlot));
          render(instanceOfExpression.getExpression());
          sourceBuilder.append("))) ");
          sourceBuilder.newLine();
          // Interface slot is past the end.
          sourceBuilder.append("(then (i32.const 0))");
          sourceBuilder.newLine();
          // itable could contain the interface, so retrieve the vtable in the specific interface
          // slot and check that it is the vtable for the interface in the instanceof; recall
          // that slots are shared and a class that does not implement this interface might
          // implement a different interface that shares the same slot.
          sourceBuilder.append(
              format(
                  "(else (ref.test_static %s (array.get $itable (struct.get $java.lang.Object"
                      + " $itable ",
                  environment.getWasmVtableTypeName(targetTypeDescriptor)));
          render(instanceOfExpression.getExpression());
          sourceBuilder.append(String.format(" ) (i32.const %d))))", interfaceSlot));
          sourceBuilder.unindent();
          sourceBuilder.newLine();
          sourceBuilder.append("))");
          sourceBuilder.unindent();
          sourceBuilder.newLine();
          sourceBuilder.append(")");
        }
        return false;
      }

      @Override
      public boolean enterMethodCall(MethodCall methodCall) {
        MethodDescriptor target = methodCall.getTarget();
        DeclaredTypeDescriptor enclosingTypeDescriptor = target.getEnclosingTypeDescriptor();

        if (methodCall.isPolymorphic()) {
          if (methodCall.hasSideEffects()) {
            sourceBuilder.append(format("(call_ref %s ", environment.getFunctionTypeName(target)));
          } else {
            sourceBuilder.append(
                format("(call %s ", environment.getNoSideEffectWrapperFunctionName(target)));
          }

          // Pass the implicit parameter.
          renderReceiver(methodCall);

          // Pass the rest of the parameters.
          methodCall.getArguments().forEach(this::render);

          // Retrieve the method reference from the appropriate vtable to provide it to call_ref.
          sourceBuilder.append(
              format(
                  "(struct.get %s %s ",
                  environment.getWasmVtableTypeName(enclosingTypeDescriptor),
                  environment.getVtableSlot(target)));

          // Retrieve the corresponding vtable.
          if (target.isClassDynamicDispatch()) {
            // For a class dynamic dispatch the vtable resides in the $vtable field of object passed
            // as the qualifier.
            sourceBuilder.append(
                format(
                    "(struct.get %s $vtable",
                    environment.getWasmTypeName(enclosingTypeDescriptor)));
            render(methodCall.getQualifier());
            sourceBuilder.append(")");
          } else {
            // For a an interface dynamic dispatch the vtable resides in the $itable array field of
            // object passed as the qualifier.

            // Retrieve the interface vtable...
            sourceBuilder.append(
                String.format(
                    "(ref.cast_static %s (array.get $itable (struct.get %s $itable ",
                    environment.getWasmVtableTypeName(enclosingTypeDescriptor),
                    environment.getWasmTypeName(enclosingTypeDescriptor)));
            render(methodCall.getQualifier());
            // ... from the assigned $table array slot and cast to the particular interface vtable.
            sourceBuilder.append(
                String.format(
                    ") (i32.const %d))) ",
                    environment.getInterfaceSlot(enclosingTypeDescriptor.getTypeDeclaration())));
          }

          sourceBuilder.append("))");
          return false;
        } else {
          // Non polymorphic methods are called directly, regardless of whether they are
          // instance methods or not.

          String wasmInfo = target.getWasmInfo();
          if (wasmInfo == null) {
            sourceBuilder.append(
                String.format(
                    "(call %s ",
                    methodCall.hasSideEffects()
                        ? environment.getMethodImplementationName(target)
                        : environment.getNoSideEffectWrapperFunctionName(target)));
          } else {
            Matcher m = Pattern.compile("typeof\\((\\d+)\\)").matcher(wasmInfo);
            while (m.find()) {
              TypeDescriptor type =
                  methodCall.getArguments().get(Integer.parseInt(m.group(1))).getTypeDescriptor();
              wasmInfo = wasmInfo.replace(m.group(), environment.getWasmTypeName(type));
            }
            sourceBuilder.append("(" + wasmInfo + " ");
          }

          if (!target.isStatic()) {
            // Constructors, non static private methods and super method calls receive the qualifier
            // as the first parameter, then the corresponding arguments.
            renderReceiver(methodCall);
          }
          // Render the parameters.
          methodCall.getArguments().forEach(this::render);

          // The binaryen intrinsic that implements calls without side effect needs the function
          // reference to the function to be called as the last parameter.
          if (!methodCall.hasSideEffects()) {
            sourceBuilder.append(
                String.format("(ref.func %s) ", environment.getMethodImplementationName(target)));
          }

          sourceBuilder.append(")");
        }
        return false;
      }

      private void renderReceiver(MethodCall methodCall) {
        // The receiver parameters is always declared as non-nullable, so perform the not null
        // check before passing it.
        sourceBuilder.append("(ref.as_non_null ");
        render(methodCall.getQualifier());
        sourceBuilder.append(")");
      }

      @Override
      public boolean enterMultiExpression(MultiExpression multiExpression) {
        List<Expression> expressions = multiExpression.getExpressions();
        Expression returnValue = Iterables.getLast(expressions);
        sourceBuilder.openParens("block ");
        sourceBuilder.append(
            "(result " + environment.getWasmType(returnValue.getDeclaredTypeDescriptor()) + ")");
        expressions.forEach(
            expression -> {
              sourceBuilder.newLine();
              if (expression == returnValue) {
                render(expression);
              } else {
                renderWithUnusedResult(expression, sourceBuilder, environment);
              }
            });
        sourceBuilder.closeParens();
        return false;
      }

      @Override
      public boolean enterArrayLiteral(ArrayLiteral arrayLiteral) {
        checkArgument(arrayLiteral.getTypeDescriptor().isNativeWasmArray());

        String arrayType = environment.getWasmTypeName(arrayLiteral.getTypeDescriptor());

        sourceBuilder.append(format("(array.init_static %s ", arrayType));
        arrayLiteral.getValueExpressions().forEach(this::render);
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterNewArray(NewArray newArray) {
        checkArgument(newArray.getTypeDescriptor().isNativeWasmArray());

        Expression dimensionExpression = newArray.getDimensionExpressions().get(0);
        String arrayType = environment.getWasmTypeName(newArray.getTypeDescriptor());

        sourceBuilder.append(format("(array.new_default %s ", arrayType));
        render(dimensionExpression);
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterNewInstance(NewInstance newInstance) {
        sourceBuilder.append(
            format(
                "(struct.new %s "
                    + "(ref.as_non_null (global.get %s)) (ref.as_non_null (global.get %s))",
                environment.getWasmTypeName(newInstance.getTypeDescriptor()),
                environment.getWasmVtableGlobalName(newInstance.getTypeDescriptor()),
                environment.getWasmItableGlobalName(newInstance.getTypeDescriptor())));

        // TODO(b/178728155): Go back to using struct.new_default_with_rtt once it supports
        // assigning immutable fields at construction. See b/178738025 for an alternative design
        // that might have better runtime tradeoffs.

        // Initialize instance fields to their default values, whose initial values are represented
        // as arguments.
        // Note that struct.new_default_with_rtt cannot be used here since the vtable needs to an
        // immutable field to enable sub-typing hence will need to be initialized at construction.
        newInstance
            .getArguments()
            .forEach(
                e -> {
                  sourceBuilder.append(" ");
                  render(e);
                });

        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterNullLiteral(NullLiteral nullLiteral) {
        sourceBuilder.append(
            "(ref.null " + environment.getWasmTypeName(nullLiteral.getTypeDescriptor()) + ")");
        return false;
      }

      @Override
      public boolean enterNumberLiteral(NumberLiteral numberLiteral) {
        PrimitiveTypeDescriptor typeDescriptor = numberLiteral.getTypeDescriptor();
        String wasmType = checkNotNull(environment.getWasmType(typeDescriptor));
        String value = convertToWasmLiteral(numberLiteral.getValue());
        sourceBuilder.append("(" + wasmType + ".const " + value + ")");
        return false;
      }

      private String convertToWasmLiteral(Number value) {
        // Handle NaN, Infinity, -Infinity as nan, inf and -inf.
        return value.toString().replace("NaN", "nan").replace("Infinity", "inf");
      }

      @Override
      public boolean enterThisOrSuperReference(ThisOrSuperReference receiverReference) {
        sourceBuilder.append("(local.get $this)");
        return false;
      }

      @Override
      public boolean enterUnaryExpression(UnaryExpression expression) {
        WasmUnaryOperation wasmUnaryOperation = WasmUnaryOperation.get(expression);

        sourceBuilder.append("(" + wasmUnaryOperation.getInstruction(expression) + " ");
        render(expression.getOperand());

        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterVariableDeclarationExpression(VariableDeclarationExpression expression) {
        // Render the first declaration with no preceeding newline. Most places that can have a
        // declaration already emit the newline (e.g. ExpressionStatement and MultiExpression).
        boolean isFirst = true;
        for (VariableDeclarationFragment fragment : expression.getFragments()) {
          if (fragment.getInitializer() != null) {
            if (!isFirst) {
              sourceBuilder.newLine();
            }
            isFirst = false;

            renderAssignment(fragment.getVariable().createReference(), fragment.getInitializer());
          }
        }

        return false;
      }

      @Override
      public boolean enterVariableReference(VariableReference variableReference) {
        sourceBuilder.append("(");
        renderAccessExpression(variableReference, /*setter=*/ false);
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterExpression(Expression expression) {
        throw new IllegalStateException("Unhandled expression " + expression);
      }

      private void render(Expression expression) {
        expression.accept(this);
      }
    }.render(expression);
  }

  public static boolean returnsVoid(Expression expression) {
    if (expression instanceof MethodCall && ((MethodCall) expression).getTarget().isConstructor()) {
      // This is a super() or this() call and the generated constructor for WASM is actually returns
      // the instance (as opposed to how it is modeled in the AST where the return is void).
      return false;
    }
    // Even though per our Java based AST an assignment is an expression that returns the value of
    // its rhs, the AST is transformed so that the resulting value is never used and the assignment
    // can be safely considered not to produce a value.
    return isPrimitiveVoid(expression.getTypeDescriptor()) || expression.isSimpleAssignment();
  }

  private ExpressionTranspiler() {}
}
