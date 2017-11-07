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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Add outer parameters to constructors of nested class that has capture variables and/or enclosing
 * instances, fix calls to the constructors, and add initializers to the added fields in each
 * constructor.
 *
 * <p>Normalization of nested classes are done in two parts, one is in CompilationUnitBuilder, and
 * the other one is here in NormalizeNestedClassConstructorsVisitor. CompilationUnitBuilder resolves
 * all the qualifiers and arguments. NormalizeNestedClassConstructorsVisitor does all normalization
 * and structural AST changes.
 */
public class NormalizeNestedClassConstructors extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Add parameters to constructors so they can receive captured values.
    compilationUnit.accept(new AddConstructorParameters());

    // Normalize method calls to constructors.
    compilationUnit.accept(new RewriteNestedClassInvocations(compilationUnit));

    // Makes the qualifier of nested class instantiation an argument to its constructors.
    compilationUnit.accept(new MakeNewInstanceQualifierIntoParameter());

    // Replace field accesses to capturing fields that hold references to the captured variables in
    // constructors with references to corresponding captured variable passing parameters.
    compilationUnit.accept(new FixFieldAccessInConstructors());

    // Add capturing field initialization statements in constructors. This should be executed after
    // FixFieldAccessInConstructorsVisitor because these field accesses to the capturing fields
    // should not be replaced.
    compilationUnit.accept(new AddFieldInitializers());
  }

  /** Adds parameters to constructors so they can receive captured values. */
  private static class AddConstructorParameters extends AbstractRewriter {

    @Override
    public boolean shouldProcessType(Type type) {
      return !type.isStatic() && type.getEnclosingTypeDeclaration() != null;
    }

    @Override
    public Node rewriteMethod(Method method) {
      if (!method.isConstructor()) {
        return method;
      }
      // Add parameters through which to pass captured variables for the current type.
      return Method.Builder.from(method)
          .addParameters(
              0,
              Streams.stream(getFieldsForCaptures(getCurrentType()))
                  .map(AstUtils::createOuterParamByField)
                  .collect(Collectors.toList()))
          .build();
    }
  }

  /** Makes the qualifier in a nested class instantiation an argument to its constructors. */
  private static class MakeNewInstanceQualifierIntoParameter extends AbstractRewriter {
    @Override
    public Node rewriteNewInstance(NewInstance newInstance) {
      if (newInstance.getQualifier() != null) {
        // outerClassInstance.new InnerClass(....) => new InnerClass(outerClassInstance, ....)
        DeclaredTypeDescriptor targetTypeDescriptor =
            newInstance.getTarget().getEnclosingTypeDescriptor();
        return NewInstance.Builder.from(newInstance)
            .addArgumentAndUpdateDescriptor(
                0, newInstance.getQualifier(), targetTypeDescriptor.getEnclosingTypeDescriptor())
            .setQualifier(null)
            .build();
      }
      return newInstance;
    }
  }

  /** Adds capturing field initialization statements in constructors. */
  private static class AddFieldInitializers extends AbstractRewriter {

    @Override
    public boolean shouldProcessType(Type type) {
      return !type.isStatic() && type.getEnclosingTypeDeclaration() != null;
    }

    @Override
    public Node rewriteMethod(Method method) {
      if (!method.isConstructor()) {
        return method;
      }
      // Maybe add capturing field initialization statements if the current constructor method does
      // not delegate to any other constructor method in the current class.
      if (!AstUtils.isDelegatedConstructorCall(
          AstUtils.getConstructorInvocation(method), getCurrentType().getTypeDescriptor())) {
        Method.Builder methodBuilder = Method.Builder.from(method);
        int i = 0;
        for (Field capturedField : getFieldsForCaptures(getCurrentType())) {
          Variable parameter = getParameterForCapturedField(capturedField.getDescriptor(), method);
          BinaryExpression initializer =
              BinaryExpression.Builder.asAssignmentTo(
                      FieldAccess.Builder.from(capturedField.getDescriptor())
                          .setQualifier(
                              new ThisReference(
                                  method.getDescriptor().getEnclosingTypeDescriptor()))
                          .build())
                  .setRightOperand(parameter)
                  .build();
          methodBuilder.addStatement(i++, initializer.makeStatement(method.getSourcePosition()));
        }
        return methodBuilder.build();
      }
      return method;
    }
  }

  /**
   * Fixes field accesses to capturing fields in a constructor.
   *
   * <p>All field accesses to capturing fields in a constructor are replaced with references to the
   * corresponding parameters. Otherwise, the arguments to cascaded constructor call are evaluated
   * before the capturing fields are initialized, which may lead to wrong result.
   *
   * <p>For example, <code>
   * class A {
   *   class B{
   *     final int a = 10;
   *     B (int f) {...}
   *     B () {this(a);} // if translated to B($c_a) {this(this.$c_a);} this.$c_a is evaluated to 0
   *                     // because field this.$c_a is not initialized yet.
   *   }
   * }
   * </code>
   */
  private static class FixFieldAccessInConstructors extends AbstractRewriter {

    @Override
    public boolean shouldProcessType(Type type) {
      return !type.isStatic() && type.getEnclosingTypeDeclaration() != null;
    }

    @Override
    public Node rewriteFieldAccess(FieldAccess fieldAccess) {
      Member currentMember = getCurrentMember();
      // replace references to added field in the constructor with the reference to parameter.
      if (currentMember.isConstructor()
          && fieldAccess.getTarget().isCapture()
          && fieldAccess.getTarget().inSameTypeAs(currentMember.getDescriptor())) {

        return getParameterForCapturedField(fieldAccess.getTarget(), (Method) currentMember)
            .getReference();
      }
      return fieldAccess;
    }
  }

  /** Adds outer class parameter to NewInstance and ctor invocations. */
  public static class RewriteNestedClassInvocations extends AbstractRewriter {
    Multimap<String, Variable> capturedVariablesByCapturingTypeName = LinkedHashMultimap.create();

    public RewriteNestedClassInvocations(CompilationUnit compilationUnit) {
      for (Type type : compilationUnit.getTypes()) {
        capturedVariablesByCapturingTypeName.putAll(
            type.getDeclaration().getQualifiedSourceName(),
            Streams.stream(type.getInstanceFields())
                .map(Field::getCapturedVariable)
                .filter(Predicates.notNull())
                .collect(Collectors.toList()));
      }
    }

    @Override
    public Node rewriteNewInstance(NewInstance newInstance) {
      TypeDescriptor typeDescriptor = newInstance.getTarget().getEnclosingTypeDescriptor();
      if (!capturedVariablesByCapturingTypeName.containsKey(
          typeDescriptor.getQualifiedSourceName())) {
        // No captures.
        return newInstance;
      }

      NewInstance.Builder newInstanceBuilder = NewInstance.Builder.from(newInstance);

      // Add arguments that reference the variables captured by the given type.
      addCapturedVariableArguments(newInstanceBuilder, typeDescriptor);
      return newInstanceBuilder.build();
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      MethodDescriptor target = methodCall.getTarget();
      if (!target.isConstructor()) {
        return methodCall;
      }

      MethodCall.Builder methodCallBuilder = MethodCall.Builder.from(methodCall);
      if (AstUtils.isDelegatedConstructorCall(methodCall, getCurrentType().getTypeDescriptor())) {
        // this() call, expands the given arguments list with references to the captured variable
        // passing parameters in the constructor method.

        methodCallBuilder.addArgumentsAndUpdateDescriptor(
            0,
            Streams.stream(getFieldsForCaptures(getCurrentType()))
                .map(
                    capturedField ->
                        getParameterForCapturedField(
                                capturedField.getDescriptor(), (Method) getCurrentMember())
                            .getReference())
                .collect(Collectors.toList()));
      } else {
        // thread captures to super call if necessary.
        DeclaredTypeDescriptor superTypeDescriptor = target.getEnclosingTypeDescriptor();
        addCapturedVariableArguments(methodCallBuilder, superTypeDescriptor);

        // a.super() => super(a)
        if (!AstUtils.hasThisReferenceAsQualifier(methodCall)) {
          methodCallBuilder
              .addArgumentAndUpdateDescriptor(
                  0, methodCall.getQualifier(), superTypeDescriptor.getEnclosingTypeDescriptor())
              .setQualifier(null);
        }
      }
      return methodCallBuilder.build();
    }

    /**
     * Expands the given arguments list with references to the variables captured by the given type.
     */
    private void addCapturedVariableArguments(
        Invocation.Builder invocationBuilder, TypeDescriptor typeDescriptor) {
      Collection<Variable> capturedVariables =
          capturedVariablesByCapturingTypeName.get(typeDescriptor.getQualifiedSourceName());
      if (capturedVariables.isEmpty()) {
        return;
      }
      invocationBuilder.addArgumentsAndUpdateDescriptor(
          0,
          capturedVariables
              .stream()
              .map(capturedVariable -> getCaptureReference(getCurrentType(), capturedVariable))
              .collect(Collectors.toList()));
    }

    private Expression getCaptureReference(Type type, Variable capturedVariable) {
      Field capturingField = getCapturingFieldInType(capturedVariable, type);
      return capturingField != null
          ?
          // If the capturedVariable is also a captured variable in current type,
          // pass the corresponding field in current type as an argument.
          FieldAccess.Builder.from(capturingField.getDescriptor())
              .setQualifier(new ThisReference(type.getTypeDescriptor()))
              .build()
          // otherwise, the captured variable is in the scope of the current type,
          // so pass the variable directly as an argument.
          : capturedVariable.getReference();
    }
  }

  /** Returns all the fields corresponding to captured variables or enclosing instance. */
  private static Iterable<Field> getFieldsForCaptures(Type type) {
    return Iterables.filter(type.getInstanceFields(), field -> field.getDescriptor().isCapture());
  }

  /**
   * Returns the field that captures {@code variable} in {@code type}.
   *
   * <p>Note: the same variable might be captured by many types, each type will have a corresponding
   * field.
   */
  private static Field getCapturingFieldInType(Variable variable, Type type) {
    checkArgument(variable != null);
    return Streams.stream(type.getFields())
        .filter(field -> field.getCapturedVariable() == variable)
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns the added parameter in constructor {@code method} that corresponds to the capturing
   * field {@code fieldDescriptor}.
   */
  private static Variable getParameterForCapturedField(
      FieldDescriptor fieldDescriptor, Method method) {
    checkArgument(
        method.isConstructor()
            && fieldDescriptor.isCapture()
            && method.getDescriptor().inSameTypeAs(fieldDescriptor));

    // Parameters in constructors corresponding to captures share the same name as the backing
    // field.
    Variable parameter =
        method
            .getParameters()
            .stream()
            .filter(variable -> variable.getName().equals(fieldDescriptor.getName()))
            .findFirst()
            .orElse(null);

    checkState(
        parameter == null
            || parameter.getTypeDescriptor().hasSameRawType(fieldDescriptor.getTypeDescriptor()));

    return parameter;
  }
}
