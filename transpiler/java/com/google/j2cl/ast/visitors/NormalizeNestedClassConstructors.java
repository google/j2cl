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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.ExpressionStatement;
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
import java.util.HashMap;
import java.util.Map;

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
    Map<TypeDescriptor, Type> typeByTypeDescriptor = new HashMap<>();
    for (Type type : compilationUnit.getTypes()) {
      typeByTypeDescriptor.put(type.getDescriptor().getRawTypeDescriptor(), type);
    }

    // Replace new InnerClass() with the wrapper function call OuterClass.m_$create__InnerClass();
    compilationUnit.accept(new FixNewInstanceOfInnerClasses());

    // Add parameters to constructors so they can receive captured values.
    compilationUnit.accept(new AddConstructorParameters());

    // Normalize method calls to constructors.
    compilationUnit.accept(new RewriteNestedClassInvocations(typeByTypeDescriptor));

    // Replace field accesses to capturing fields that hold references to the captured variables in
    // constructors with references to corresponding captured variable passing parameters.
    compilationUnit.accept(new FixFieldAccessInConstructors());

    // Add capturing field initialization statements in constructors. This should be executed after
    // FixFieldAccessInConstructorsVisitor because these field accesses to the capturing fields
    // should not be replaced.
    compilationUnit.accept(new AddFieldInitializers());
  }

  /**
   * Adds parameters to constructors so they can receive captured values.
   */
  private static class AddConstructorParameters extends AbstractRewriter {

    @Override
    public boolean shouldProcessType(Type type) {
      return !type.isStatic() && type.getEnclosingTypeDescriptor() != null;
    }

    @Override
    public Node rewriteMethod(Method method) {
      if (!method.isConstructor()) {
        return method;
      }
      // Add parameters through which to pass captured variables for the current type.
      return Method.Builder.from(method)
          .addParameters(
              Iterables.transform(
                  getFieldsForAllCaptures(getCurrentType()),
                  new Function<Field, Variable>() {
                    @Override
                    public Variable apply(Field capturedField) {
                      return AstUtils.createOuterParamByField(capturedField);
                    }
                  }))
          .build();
    }
  }

  /**
   * Replaces NewInstance of an inner class with wrapper function calls.
   */
  private static class FixNewInstanceOfInnerClasses extends AbstractRewriter {

    @Override
    public Node rewriteNewInstance(NewInstance newInstance) {
      MethodDescriptor targetMethod = newInstance.getTarget();
      TypeDescriptor targetTypeDescriptor = targetMethod.getEnclosingClassTypeDescriptor();
      if (targetTypeDescriptor.isInstanceMemberClass()) {
        // outerClassInstance.new InnerClass() => new InnerClass(outerClassInstance)
        return NewInstance.Builder.from(newInstance)
            .appendArgumentAndUpdateDescriptor(
                newInstance.getQualifier(), targetTypeDescriptor.getEnclosingTypeDescriptor())
            .build();
      }
      return newInstance;
    }
  }

  /**
   * Adds capturing field initialization statements in constructors.
   */
  private static class AddFieldInitializers extends AbstractRewriter {

    @Override
    public boolean shouldProcessType(Type type) {
      return !type.isStatic() && type.getEnclosingTypeDescriptor() != null;
    }

    @Override
    public Node rewriteMethod(Method method) {
      if (!method.isConstructor()) {
        return method;
      }
      // Maybe add capturing field initialization statements if the current constructor method does
      // not delegate to any other constructor method in the current class.
      if (!AstUtils.isDelegatedConstructorCall(
          AstUtils.getConstructorInvocation(method), getCurrentType().getDescriptor())) {
        Method.Builder methodBuilder = Method.Builder.from(method);
        int i = 0;
        for (Field capturedField : getFieldsForAllCaptures(getCurrentType())) {
          Variable parameter = getParameterForCapturedField(capturedField.getDescriptor(), method);
          BinaryExpression initializer =
              new BinaryExpression(
                  capturedField.getDescriptor().getTypeDescriptor(),
                  new FieldAccess(
                      new ThisReference(method.getDescriptor().getEnclosingClassTypeDescriptor()),
                      capturedField.getDescriptor()),
                  BinaryOperator.ASSIGN,
                  parameter.getReference());
          methodBuilder.addStatement(i++, new ExpressionStatement(initializer));
        }
        return methodBuilder.build();
      }
      return method;
    }
  }

  /**
   * Fixes field accesses to capturing fields in a constructor.
   *
   * <p>
   * All field accesses to capturing fields in a constructor are replaced with references to the
   * corresponding parameters. Otherwise, the arguments to cascaded constructor call are evaluated
   * before the capturing fields are initialized, which may lead to wrong result.
   *
   * <p>For example,
   * <code>
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
      return !type.isStatic() && type.getEnclosingTypeDescriptor() != null;
    }

    @Override
    public Node rewriteFieldAccess(FieldAccess fieldAccess) {
      Member currentMember = getCurrentMember();
      // replace references to added field in the constructor with the reference to parameter.
      if (currentMember.isConstructor()
          && fieldAccess.getTarget().isFieldDescriptorForAllCaptures()) {
        Variable parameter =
            getParameterForCapturedField(fieldAccess.getTarget(), (Method) currentMember);
        if (parameter != null) {
          return parameter.getReference();
        }
      }
      return fieldAccess;
    }
  }

  /**
   * Adds outer class parameter to NewInstance and ctor invocations.
   */
  public static class RewriteNestedClassInvocations extends AbstractRewriter {
    private final Map<TypeDescriptor, Type> typeByTypeDescriptor;

    private RewriteNestedClassInvocations(Map<TypeDescriptor, Type> typeByTypeDescriptor) {
      this.typeByTypeDescriptor = typeByTypeDescriptor;
    }

    @Override
    public Node rewriteNewInstance(NewInstance newInstance) {
      TypeDescriptor typeDescriptor = newInstance.getTarget().getEnclosingClassTypeDescriptor();
      Type type = getType(typeDescriptor);
      if (type == null || type.isStatic() || type.getEnclosingTypeDescriptor() == null) {
        return newInstance;
      }

      NewInstance.Builder newInstanceBuilder = NewInstance.Builder.from(newInstance);
      // Add arguments that reference the variables captured by the given type.
      addCapturedVariableArguments(newInstanceBuilder, typeDescriptor);

      // Maybe add the qualifier of the NewInstance as the last argument to the constructor of a
      // local class. The qualifier may be null if the local class is in a static context.
      if (type.getDescriptor().isLocal() && newInstance.getQualifier() != null) {
        newInstanceBuilder.appendArgumentAndUpdateDescriptor(
            newInstance.getQualifier(), newInstance.getQualifier().getTypeDescriptor());
      }

      return newInstanceBuilder.build();
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      MethodDescriptor target = methodCall.getTarget();
      if (!target.isConstructor()) {
        return methodCall;
      }

      MethodCall.Builder methodCallBuilder = MethodCall.Builder.from(methodCall);
      if (AstUtils.isDelegatedConstructorCall(methodCall, getCurrentType().getDescriptor())) {
        // this() call, expands the given arguments list with references to the captured variable
        // passing parameters in the constructor method.
        for (Field capturedField : getFieldsForAllCaptures(getCurrentType())) {
          Variable parameter =
              getParameterForCapturedField(
                  capturedField.getDescriptor(), (Method) getCurrentMember());
          methodCallBuilder.appendArgumentAndUpdateDescriptor(
              parameter.getReference(), parameter.getTypeDescriptor());
        }
      } else {
        // super() call
        TypeDescriptor superTypeDescriptor = target.getEnclosingClassTypeDescriptor();
        addCapturedVariableArguments(methodCallBuilder, superTypeDescriptor);

        // a.super() => super(a)
        if (!AstUtils.hasThisReferenceAsQualifier(methodCall)) {
          methodCallBuilder
              .appendArgumentAndUpdateDescriptor(
                  methodCall.getQualifier(), superTypeDescriptor.getEnclosingTypeDescriptor())
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
      Type type = getType(typeDescriptor);
      if (type == null) {
        return;
      }

      for (Field field : type.getFields()) {
        Variable capturedVariable = field.getCapturedVariable();
        if (capturedVariable == null) {
          continue;
        }
        Field capturingField = getCapturingFieldInType(capturedVariable, getCurrentType());
        if (capturingField != null) {
          // If the capturedVariable is also a captured variable in current type, pass the
          // corresponding field in current type as an argument.
          invocationBuilder.appendArgumentAndUpdateDescriptor(
              new FieldAccess(new ThisReference(typeDescriptor), capturingField.getDescriptor()),
              capturingField.getDescriptor().getTypeDescriptor());
        } else {
          // otherwise, the captured variable is in the scope of the current type, so pass the
          // variable directly as an argument.
          invocationBuilder.appendArgumentAndUpdateDescriptor(
              capturedVariable.getReference(), capturedVariable.getTypeDescriptor());
        }
      }
    }

    private Type getType(TypeDescriptor typeDescriptor) {
      return typeByTypeDescriptor.get(typeDescriptor.getRawTypeDescriptor());
    }
  }

  /** Returns all the added fields corresponding to captured variables or enclosing instance. */
  private static Iterable<Field> getFieldsForAllCaptures(Type type) {
    return Iterables.filter(
        type.getInstanceFields(),
        new Predicate<Field>() {
          @Override
          public boolean apply(Field field) {
            return field.getDescriptor().isFieldDescriptorForAllCaptures();
          }
        });
  }

  private static Field getCapturingFieldInType(Variable variable, Type type) {
    for (Field field : type.getFields()) {
      if (field.getCapturedVariable() == variable) {
        return field;
      }
    }
    return null;
  }

  /**
   * Returns the added parameter in constructor {@code method} that corresponds to the capturing
   * field {@code fieldDescriptor}.
   */
  private static Variable getParameterForCapturedField(
      FieldDescriptor fieldDescriptor, Method method) {
    Preconditions.checkArgument(method.isConstructor());
    Preconditions.checkArgument(fieldDescriptor.isFieldDescriptorForAllCaptures());
    for (Variable parameter : method.getParameters()) {
      if (parameter.getName().equals(fieldDescriptor.getName())
          && parameter
              .getTypeDescriptor()
              .equalsIgnoreNullability(fieldDescriptor.getTypeDescriptor())
          && method
              .getDescriptor()
              .getEnclosingClassTypeDescriptor()
              .equalsIgnoreNullability(fieldDescriptor.getEnclosingClassTypeDescriptor())) {
        return parameter;
      }
    }
    return null;
  }
}
