/*
 * Copyright 2019 Google Inc.
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
package com.google.j2cl.frontend.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.annotations.FormatMethod;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Base class for implementing that AST conversion from different front ends. */
public abstract class AbstractCompilationUnitBuilder {

  private final PackageInfoCache packageInfoCache = PackageInfoCache.get();
  private final Map<Variable, Type> enclosingTypeByVariable = new HashMap<>();
  private final Multimap<String, Variable> capturesByTypeName = LinkedHashMultimap.create();
  /** Type stack to keep track of the lexically enclosing types as they are being created. */
  private final List<Type> typeStack = new ArrayList<>();

  private String currentSourceFile;
  private CompilationUnit currentCompilationUnit;

  /** Push a type the type stack, making it the current type. */
  protected void pushType(Type type) {
    typeStack.add(type);
  }

  /** Pops the top of the type stack. */
  protected void popType() {
    typeStack.remove(typeStack.size() - 1);
  }

  /** Returns the current type. */
  protected Type getCurrentType() {
    return Iterables.getLast(typeStack, null);
  }

  /** Sets the JS namespace for a package that is being compiled from source. */
  protected void setPackageJsNamespaceFromSource(String packageName, String jsNamespace) {
    packageInfoCache.setPackageJsNamespace(
        PackageInfoCache.SOURCE_CLASS_PATH_ENTRY, packageName, jsNamespace);
  }

  protected String getCurrentSourceFile() {
    return currentSourceFile;
  }

  protected void setCurrentSourceFile(String currentSourceFile) {
    this.currentSourceFile = currentSourceFile;
  }

  protected CompilationUnit getCurrentCompilationUnit() {
    return currentCompilationUnit;
  }

  protected void setCurrentCompilationUnit(CompilationUnit currentCompilationUnit) {
    this.currentCompilationUnit = currentCompilationUnit;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Helpers to synthesize lambdas for method references.
  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a class instantiation lambda from a method reference.
   *
   * <p>
   *
   * <pre>{@code
   * A:new into (par1, ..., parN) -> new A(par1, ..., parN) or
   *            (par1, ..., parN) -> B.this.new A(par1, ..., parN)
   * }</pre>
   */
  public static Expression createInstantiationLambda(
      MethodDescriptor functionalMethodDescriptor,
      MethodDescriptor targetConstructorMethodDescriptor,
      Expression qualifier,
      SourcePosition sourcePosition) {

    List<Variable> parameters =
        AstUtils.createParameterVariables(functionalMethodDescriptor.getParameterTypeDescriptors());
    checkArgument(
        targetConstructorMethodDescriptor.getParameterTypeDescriptors().size()
            == parameters.size());

    NewInstance instantiation =
        NewInstance.Builder.from(targetConstructorMethodDescriptor)
            .setQualifier(qualifier)
            .setArguments(
                parameters.stream().map(Variable::getReference).collect(toImmutableList()))
            .build();

    return FunctionExpression.newBuilder()
        .setTypeDescriptor(functionalMethodDescriptor.getEnclosingTypeDescriptor())
        .setParameters(parameters)
        .setStatements(
            ReturnStatement.newBuilder()
                .setExpression(instantiation)
                .setTypeDescriptor(functionalMethodDescriptor.getReturnTypeDescriptor())
                .setSourcePosition(sourcePosition)
                .build())
        .setSourcePosition(sourcePosition)
        .build();
  }

  /**
   * Creates a lambda that implements an array creation method reference.
   *
   * <p>
   *
   * <pre>{@code convert A[]::new into (size) -> new A[size]} </pre>
   */
  public static Expression createArrayCreationLambda(
      MethodDescriptor targetFunctionalMethodDescriptor,
      ArrayTypeDescriptor arrayType,
      SourcePosition sourcePosition) {

    // Array creation method references always have exactly one parameter.
    Variable parameter =
        Iterables.getOnlyElement(
            AstUtils.createParameterVariables(
                targetFunctionalMethodDescriptor.getParameterTypeDescriptors()));

    // The size of the array is the only parameter in the implemented function. It's legal for
    // the source to provide only one dimension parameter to to create a multidimensional array
    // but our AST expects NewArray nodes to provide an expression for each dimension in the
    // array type, hence the missing dimensions are padded with null.
    ImmutableList<Expression> dimensionExpressions =
        ImmutableList.<Expression>builder()
            .add(parameter.getReference())
            .addAll(Collections.nCopies(arrayType.getDimensions() - 1, NullLiteral.get()))
            .build();

    return FunctionExpression.newBuilder()
        .setTypeDescriptor(targetFunctionalMethodDescriptor.getEnclosingTypeDescriptor())
        .setParameters(parameter)
        .setStatements(
            ReturnStatement.newBuilder()
                .setExpression(
                    NewArray.newBuilder()
                        .setTypeDescriptor(arrayType)
                        .setDimensionExpressions(dimensionExpressions)
                        .build())
                .setTypeDescriptor(targetFunctionalMethodDescriptor.getReturnTypeDescriptor())
                .setSourcePosition(sourcePosition)
                .build())
        .setSourcePosition(sourcePosition)
        .build();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Helpers to resolve captures.
  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Records associations of variables and their enclosing type.
   *
   * <p>Enclosing type is a broader category than declaring type since some variables (fields) have
   * a declaring type (that is also their enclosing type) while other variables do not.
   */
  protected void recordEnclosingType(Variable variable, Type enclosingType) {
    enclosingTypeByVariable.put(variable, enclosingType);
  }

  /** Returns the enclosing type for a variable. */
  protected Type getEnclosingType(Variable variable) {
    return enclosingTypeByVariable.get(variable);
  }

  /**
   * Returns the required nested path of field accesses needed to refer to a specific enclosing
   * given by an explicit reference like {@code A.this} or {@code A.super}.
   *
   * <p>
   *
   * <pre><code>
   *   class A extends Super {
   *     void m() {}
   *     class B {
   *       class C {
   *         { A a = A.this;}
   *       }
   *     }
   *   }
   * </code></pre>
   *
   * <p>In this example the outer class referred by {@code A.this} is 2 hops from the reference,
   * hence the access returned would be {@code this.$outer_this.$outer_this}.
   *
   * <p>NOTE: Explicit outer references have to match the class exactly, i.e. references like {@code
   * Super.this} instead of {@code A.this} will be rejected by the compiler.
   */
  protected Expression resolveExplicitOuterClassReference(
      DeclaredTypeDescriptor targetTypeDescriptor) {
    return resolveOuterClassReference(targetTypeDescriptor, true);
  }

  /**
   * Returns the required nested path of field accesses needed to refer to a specific enclosing
   * given by an implicit reference like an unqualified method call {@code m()} that might be a
   * method of an enclosing class. E.g:
   *
   * <p>
   *
   * <pre><code>
   *   class A {
   *     void m() {}
   *     class B {
   *     }
   *   }
   *   class ASub extends A{
   *     class BSub extends B{
   *       { m(); }
   *     }
   *   }
   * </code></pre>
   *
   * <p>In this example the outer class referred by the call to {@code A.m()} is {@code ASub} and
   * not {@code A}, an the path return would be {@code this.$outer_this} since it is the immediate
   * enclosing class of {@code BSub}.
   */
  protected Expression resolveImplicitOuterClassReference(
      DeclaredTypeDescriptor targetTypeDescriptor) {
    return resolveOuterClassReference(targetTypeDescriptor, false);
  }

  /**
   * Returns a nested path of field accesses {@code this.$outer_this.....$outer_this} required to
   * address a specific outer class.
   *
   * <p>The search for the enclosing class might be strict or non-strict, depending whether a
   * subclass of the enclosing class is acceptable as a qualifier. When the member's qualifier is
   * implicit, superclasses of the enclosing class are acceptable.
   */
  private Expression resolveOuterClassReference(
      DeclaredTypeDescriptor targetTypeDescriptor, boolean strict) {
    DeclaredTypeDescriptor currentTypeDescriptor = getCurrentType().getTypeDescriptor();
    Expression qualifier = new ThisReference(currentTypeDescriptor);
    DeclaredTypeDescriptor innerTypeDescriptor = currentTypeDescriptor;
    while (innerTypeDescriptor.getTypeDeclaration().isCapturingEnclosingInstance()) {
      boolean found =
          strict
              ? innerTypeDescriptor.hasSameRawType(targetTypeDescriptor)
              : innerTypeDescriptor.isSubtypeOf(targetTypeDescriptor);
      if (found) {
        break;
      }

      qualifier =
          FieldAccess.Builder.from(
                  AstUtils.getFieldDescriptorForEnclosingInstance(
                      innerTypeDescriptor, innerTypeDescriptor.getEnclosingTypeDescriptor()))
              .setQualifier(qualifier)
              .build();
      innerTypeDescriptor = innerTypeDescriptor.getEnclosingTypeDescriptor();
    }
    return qualifier;
  }

  /**
   * Returns the expression to reference {@code variable} in the current context.
   *
   * <p>If the variable is not declared in the current context it is a capture. References to
   * captured variables are recorded in the process of doing this resolution to determine the
   * backing fields that are needed.
   */
  protected Expression resolveVariableReference(Variable variable) {
    TypeDeclaration enclosingClassDeclaration =
        checkNotNull(enclosingTypeByVariable.get(variable).getDeclaration());

    if (getCurrentType().getDeclaration().equals(enclosingClassDeclaration)) {
      return variable.getReference();
      }

    propagateCaptureOutward(variable, enclosingClassDeclaration);

    // for reference to a captured variable, if it is in a constructor, translate to
    // reference to outer parameter, otherwise, translate to reference to corresponding
    // field created for the captured variable.
    DeclaredTypeDescriptor currentTypeDescriptor = getCurrentType().getTypeDescriptor();
      FieldDescriptor fieldDescriptor =
          AstUtils.getFieldDescriptorForCapture(currentTypeDescriptor, variable);
      ThisReference qualifier = new ThisReference(currentTypeDescriptor);
      return FieldAccess.Builder.from(fieldDescriptor).setQualifier(qualifier).build();
  }

  /**
   * Propagates the capture variable to the enclosing classes.
   *
   * <p>A reference to a captured variable from an inner class means that the inner class and all
   * its enclosing classes up to (but excluding) the variable scope need to capture the variable,
   * e.g.
   *
   * <pre><code>
   *   void m(int captured) {}
   *     class A {
   *       class B {
   *         { int a = captured; } // captured is captured by both A and B.
   *       }
   *     }
   *   }
   * </code></pre>
   */
  private void propagateCaptureOutward(
      Variable variable, TypeDeclaration enclosingClassDeclarationDescriptor) {
    // the variable is declared outside current type, i.e. a captured variable to current
    // type, and also a captured variable to the outer class in the type stack that is
    // inside {@code enclosingClassRef}.
    for (int i = typeStack.size() - 1; i >= 0; i--) {
      if (typeStack.get(i).getDeclaration().equals(enclosingClassDeclarationDescriptor)) {
        break;
      }
      capturesByTypeName.put(typeStack.get(i).getDeclaration().getQualifiedSourceName(), variable);
    }
  }

  /** Returns the variables captured by a type. */
  protected Collection<Variable> getCapturedVariables(TypeDeclaration typeDeclaration) {
    return capturesByTypeName.get(typeDeclaration.getQualifiedSourceName());
  }

  /**
   * Propagates the variables captured by the supertype.
   *
   * <p>The purpose of propagating the captures down the class hierarchy is to cover the following
   * corner case for local classes.
   *
   * <p>
   *
   * <pre><code>
   *     void f(int n) {
   *       class LocalSuper {
   *         // ....  code referencing n
   *          int m() {  return n;  }
   *       }
   *       class LocalSub extends LocalSuper {
   *       }
   *
   *       new LocalSub().m(); // Here due to capture conversion the captured variable needs to be
   *                           // threaded throw LocalSub constructor.
   *     }
   *   </code></pre>
   *
   * <p>Propagating the capture allows an uniform treatment elsewhere, with the drawback that it
   * ends up defining an extra field in LocalSub,
   */
  protected void propagateCapturesFromSupertype(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.getSuperTypeDescriptor() != null) {
      capturesByTypeName.putAll(
          typeDeclaration.getQualifiedSourceName(),
          capturesByTypeName.get(
              typeDeclaration.getSuperTypeDescriptor().getQualifiedSourceName()));
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // General helpers.
  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates temporary variables for a resource that is declared outside of the try-catch statement.
   */
  protected static VariableDeclarationExpression toResource(Expression expression) {
    if (expression instanceof VariableDeclarationExpression) {
      return (VariableDeclarationExpression) expression;
    }

    // Create temporary variables for resources declared outside of the try statement.
    return VariableDeclarationExpression.newBuilder()
        .addVariableDeclaration(
            Variable.newBuilder()
                .setName("$resource")
                .setTypeDescriptor(expression.getTypeDescriptor())
                .setFinal(true)
                .build(),
            expression)
        .build();
  }

  protected boolean requiresOverrideAnnotation(
      MethodDescriptor methodDescriptor, MethodDescriptor overriddenMethodDescriptor) {
    if (methodDescriptor.isJsMember()) {
      return overriddenMethodDescriptor.isJsMember()
          && AstUtils.overrideNeedsAtOverrideAnnotation(overriddenMethodDescriptor);
    }
    return methodDescriptor.isJsOverride(overriddenMethodDescriptor)
        && AstUtils.overrideNeedsAtOverrideAnnotation(overriddenMethodDescriptor);
  }

  protected Expression convertConstantToLiteral(
      Object constantValue, TypeDescriptor typeDescriptor) {
    if (constantValue instanceof Boolean) {
      return (boolean) constantValue ? BooleanLiteral.get(true) : BooleanLiteral.get(false);
    }
    if (constantValue instanceof Number) {
      return new NumberLiteral(typeDescriptor.toUnboxedType(), (Number) constantValue);
    }
    if (constantValue instanceof Character) {
      return NumberLiteral.fromChar((Character) constantValue);
    }
    if (constantValue instanceof String) {
      return new StringLiteral((String) constantValue);
    }
    throw internalCompilerError(
        "Unexpected type for compile time constant: %s", constantValue.getClass().getSimpleName());
  }

  @FormatMethod
  protected RuntimeException internalCompilerError(String format, Object... params) {
      return new RuntimeException(internalCompilerErrorMessage(format, params));
    }

  @FormatMethod
  protected String internalCompilerErrorMessage(String format, Object... params) {
      return String.format(format, params) + ", in file: " + currentSourceFile;
  }
}
