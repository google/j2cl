/*
 * Copyright 2021 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.FieldDescriptor.FieldOrigin;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Creates backing fields for captures and rewrites accesses. */
public class ResolveCaptures extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    collectCaptures(compilationUnit);
    declareBackingFields(compilationUnit);
    passCapturesAsConstructorArguments(compilationUnit);
    addConstructorParametersForCaptures(compilationUnit);
    replaceCaptureReferencesWithBackingFields(compilationUnit);
  }

  /** Collects all the variables declared in the compilation unit by their corresponding type. */
  private final SetMultimap<TypeDeclaration, Variable> capturedVariablesByTypeDeclaration =
      LinkedHashMultimap.create();
  /** Maps variables to the types they are declared in. */
  private final Map<Variable, TypeDeclaration> declaringTypeByVariable = new HashMap<>();

  /**
   * Collects information to be able to resolve captures.
   *
   * <p>NOTE: that variable captures need to be explicitly propagated through superclasses and
   * NewInstance expressions, but enclosing instances do not. This is due to the fact that they are
   * materialized as qualifiers by the preceding passes.
   */
  private void collectCaptures(CompilationUnit compilationUnit) {

    compilationUnit.accept(
        new AbstractVisitor() {
          /**
           * Used to record the actual state of nested types in the AST at the moment this pass is
           * run.
           *
           * <p>Note that due to transformations that might run before this pass, classes might be
           * added and removed from the nesting structure so we cannot rely on the type model.
           */
          private final Deque<Type> enclosingTypes = new ArrayDeque<>();

          @Override
          public boolean enterType(Type type) {
            enclosingTypes.push(type);
            return true;
          }

          @Override
          public void exitType(Type type) {
            // Propagate captures from supertypes, these need to be threaded in the call to the
            // super constructor.
            // TODO(b/215282471): There is one scenario where some captures might appear in the
            //  supertype after finishing its traversal.
            for (DeclaredTypeDescriptor superType = type.getSuperTypeDescriptor();
                superType != null;
                superType = superType.getSuperTypeDescriptor()) {
              recordCaptures(
                  capturedVariablesByTypeDeclaration.get(superType.getTypeDeclaration()));
            }
            checkState(enclosingTypes.pop() == type);
          }

          @Override
          public boolean enterVariable(Variable variable) {
            // Record variable declaration (always occurs before references).
            checkState(
                declaringTypeByVariable.put(variable, getCurrentType().getDeclaration()) == null);
            return true;
          }

          @Override
          public boolean enterVariableReference(VariableReference variableReference) {
            recordCapture(variableReference.getTarget());
            return true;
          }

          @Override
          public boolean enterNewInstance(NewInstance newInstance) {
            // Consider the following scenario:.
            //
            //  class A {
            //    SupplierInt m() {
            //      int i = f();
            //      class Local implements SupplierInt{
            //         int get() { return i; } <-- capture of i occurs here.
            //      }
            //      class UnrelatedLocal {
            //        SupplierInt n() {
            //          return new Local(); <-- there is an *implicit* capture of i here, since when
            //                                  instances of Local are created, their captures need
            //                                  to be passed here to the constructor.
            //      }
            //      return new UnrelatedLocal();
            //  }
            // }
            //
            recordCaptures(
                capturedVariablesByTypeDeclaration.get(
                    newInstance.getTypeDescriptor().getTypeDeclaration()));
            return true;
          }

          private void recordCapture(Variable variable) {
            TypeDeclaration declaringType = declaringTypeByVariable.get(variable);
            for (Type currentType : enclosingTypes) {
              // Propagate the recording of the captured variable until the type that declares it
              // reached.
              if (currentType.getDeclaration().equals(declaringType)) {
                break;
              }
              capturedVariablesByTypeDeclaration.put(currentType.getDeclaration(), variable);
            }
          }

          private void recordCaptures(Collection<Variable> variables) {
            variables.forEach(this::recordCapture);
          }
        });
  }

  /** Declares the backing fields for each captured variable and enclosing instance. */
  private void declareBackingFields(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterType(Type type) {
            for (Variable variable :
                capturedVariablesByTypeDeclaration.get(type.getDeclaration())) {
              type.addMember(
                  Field.Builder.from(
                          FieldDescriptor.Builder.from(
                                  getFieldDescriptorForCapture(type.getDeclaration(), variable))
                              .build())
                      .setSourcePosition(type.getSourcePosition())
                      .build());
            }
            if (type.getDeclaration().isCapturingEnclosingInstance()) {
              type.addMember(
                  0,
                  Field.Builder.from(
                          type.getTypeDescriptor().getFieldDescriptorForEnclosingInstance())
                      .setSourcePosition(type.getSourcePosition())
                      .build());
            }
            return true;
          }
        });
  }

  /** Thread captures by passing them as arguments in the constructors. */
  private void passCapturesAsConstructorArguments(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteInvocation(Invocation invocation) {
            if (!invocation.getTarget().isConstructor()) {
              return invocation;
            }
            DeclaredTypeDescriptor targetTypeDescriptor =
                invocation.getTarget().getDeclarationDescriptor().getEnclosingTypeDescriptor();
            TypeDeclaration targetTypeDeclaration = targetTypeDescriptor.getTypeDeclaration();

            Set<Variable> captures = capturedVariablesByTypeDeclaration.get(targetTypeDeclaration);
            Expression qualifier = invocation.getQualifier();
            if (captures.isEmpty() && qualifier == null) {
              return invocation;
            }

            // Pass the captured variables.
            Invocation.Builder<?, ?> invocationBuilder =
                Invocation.Builder.from(invocation)
                    .addArgumentsAndUpdateDescriptor(
                        0,
                        captures.stream()
                            .map(Variable::createReference)
                            .collect(toImmutableList()));

            if (qualifier != null) {
              // Pass the enclosing instance as the first parameter.
              checkArgument(targetTypeDeclaration.isCapturingEnclosingInstance());
              invocationBuilder
                  .addArgumentAndUpdateDescriptor(
                      0,
                      invocation.getQualifier(),
                      // Consider the outer instance type to be nullable to be make the type
                      // consistent across all places where it is used (backing field and
                      // constructor parameters).
                      targetTypeDescriptor.getEnclosingTypeDescriptor().toNullable())
                  .setQualifier(null);
            }
            return invocationBuilder.build();
          }
        });
  }

  /**
   * Declares the parameters that will receive the captures and modifies the constructors to
   * initialize the backing fields.
   */
  private void addConstructorParametersForCaptures(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            if (!method.isConstructor()) {
              return method;
            }

            TypeDeclaration typeDeclaration = getCurrentType().getDeclaration();
            Method.Builder methodBuilder = Method.Builder.from(method);
            boolean isDelegatingConstructor = AstUtils.hasThisCall(method);
            Map<Variable, Variable> parameterByCapturedVariable = new HashMap<>();

            // If this is a JsConstructor, ensure we don't put any statements before the super()
            // call.
            int statementOffset =
                method.getDescriptor().isJsConstructor()
                        && getCurrentType().getSuperTypeDescriptor().hasJsConstructor()
                    ? method
                            .getBody()
                            .getStatements()
                            .indexOf(AstUtils.getConstructorInvocationStatement(method))
                        + 1
                    : 0;

            // Declare a parameter for each captured variable and initialize the backing field
            // only if this constructor is not a delegating constructor.
            int i = 0;
            for (Variable variable : capturedVariablesByTypeDeclaration.get(typeDeclaration)) {
              Variable parameter =
                  addParameterAndInitializeBackingField(
                      methodBuilder,
                      i++,
                      statementOffset,
                      getFieldDescriptorForCapture(typeDeclaration, variable),
                      isDelegatingConstructor,
                      // Keep the source position of the original variable to keep the name mapping.
                      variable.getSourcePosition());
              parameterByCapturedVariable.put(variable, parameter);
            }

            Variable enclosingInstanceParameter =
                typeDeclaration.isCapturingEnclosingInstance()
                    ? addParameterAndInitializeBackingField(
                        methodBuilder,
                        0,
                        statementOffset,
                        getCurrentType()
                            .getTypeDescriptor()
                            .getFieldDescriptorForEnclosingInstance(),
                        isDelegatingConstructor,
                        getCurrentType().getSourcePosition())
                    : null;

            replaceCaptureReferencesByParameters(
                method, parameterByCapturedVariable, enclosingInstanceParameter);

            return methodBuilder.build();
          }
        });
  }

  /** Adds a parameter to the constructor and if necessary initializes the backing field. */
  private Variable addParameterAndInitializeBackingField(
      Method.Builder methodBuilder,
      int position,
      int statementOffsetPosition,
      FieldDescriptor captureBackingField,
      boolean isDelegatingConstructor,
      SourcePosition sourcePosition) {
    Variable parameter = createParameterMatchingField(captureBackingField);

    // Add the parameter.
    methodBuilder.addParameters(position, parameter);
    if (!isDelegatingConstructor) {
      // Assign the parameter to the backing field.
      methodBuilder.addStatement(
          statementOffsetPosition + position,
          BinaryExpression.Builder.asAssignmentTo(captureBackingField)
              .setRightOperand(parameter.createReference())
              .build()
              .makeStatement(sourcePosition));
    }
    return parameter;
  }

  /**
   * Replaces references to captured variables and enclosing classes by references to the parameters
   * in which the values are passed.
   *
   * <p>Note that in constructors those values are passed as parameters, so there is no need to
   * retrieve the values from the fields.
   */
  private void replaceCaptureReferencesByParameters(
      Method constructor,
      Map<Variable, Variable> parameterByCapturedVariable,
      Variable enclosingInstanceParameter) {
    // References to captured variables or instances that occur in the constructor get
    // replaced by references to the corresponding enclosingInstanceParameter and not their
    // backing field.
    constructor.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessType(Type type) {
            // Do not recurse into types, if a local or anonymous class is defined in a
            // constructor, the captures on that class need to be resolved in its own scope.
            return false;
          }

          @Override
          public VariableReference rewriteVariableReference(VariableReference variableReference) {
            Variable replacement = parameterByCapturedVariable.get(variableReference.getTarget());
            if (replacement != null) {
              return replacement.createReference();
            }
            return variableReference;
          }

          @Override
          public Expression rewriteThisReference(ThisReference thisReference) {
            DeclaredTypeDescriptor capturedEnclosingType =
                constructor
                    .getDescriptor()
                    .getEnclosingTypeDescriptor()
                    .getEnclosingTypeDescriptor();
            if (enclosingInstanceParameter != null
                && thisReference.getTypeDescriptor().hasSameRawType(capturedEnclosingType)) {
              return enclosingInstanceParameter.createReference();
            }
            return thisReference;
          }
        });
  }

  /**
   * Replaces references for the captured variables and enclosing instances with the corresponding
   * backing fields.
   */
  private void replaceCaptureReferencesWithBackingFields(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteVariableReference(VariableReference variableReference) {
            Variable variable = variableReference.getTarget();
            TypeDeclaration declaringType = declaringTypeByVariable.get(variable);
            if (declaringType == null || declaringType.equals(getCurrentType().getDeclaration())) {
              // not a capture.
              return variableReference;
            }

            return FieldAccess.newBuilder()
                .setTarget(
                    getFieldDescriptorForCapture(getCurrentType().getDeclaration(), variable))
                .setQualifier(new ThisReference(getCurrentType().getTypeDescriptor()))
                .build();
          }

          @Override
          public Expression rewriteThisReference(ThisReference thisReference) {
            DeclaredTypeDescriptor currentTypeDescriptor = getCurrentType().getTypeDescriptor();
            // The implicit qualifier is the first class from inner to outer context that has
            // a targetTypeDescriptor supertype.
            DeclaredTypeDescriptor targetTypeDescriptor = thisReference.getTypeDescriptor();
            if (currentTypeDescriptor.hasSameRawType(targetTypeDescriptor)) {
              return thisReference;
            }

            Expression outerFieldAccess = new ThisReference(currentTypeDescriptor);
            do {
              outerFieldAccess =
                  FieldAccess.newBuilder()
                      .setTarget(currentTypeDescriptor.getFieldDescriptorForEnclosingInstance())
                      .setQualifier(outerFieldAccess)
                      .build();
              currentTypeDescriptor = currentTypeDescriptor.getEnclosingTypeDescriptor();
              if (currentTypeDescriptor == null) {
                checkArgument(targetTypeDescriptor.isInterface());
                return thisReference;
              }
            } while (!currentTypeDescriptor.hasSameRawType(targetTypeDescriptor));

            return outerFieldAccess;
          }
        });
  }

  /** Returns the FieldDescriptor corresponding to the captured variable. */
  private static FieldDescriptor getFieldDescriptorForCapture(
      TypeDeclaration typeDeclaration, Variable capturedVariable) {
    return FieldDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(typeDeclaration.toUnparameterizedTypeDescriptor())
        .setName("$captured_" + capturedVariable.getName())
        .setTypeDescriptor(capturedVariable.getTypeDescriptor())
        .setStatic(false)
        .setFinal(true)
        .setSynthetic(true)
        .setOrigin(FieldOrigin.SYNTHETIC_CAPTURE_FIELD)
        .build();
  }

  /** Creates a variable that matches a field definition. */
  private static Variable createParameterMatchingField(FieldDescriptor fieldDescriptor) {
    return Variable.newBuilder()
        .setName(fieldDescriptor.getOrigin().getPrefix() + fieldDescriptor.getName())
        .setTypeDescriptor(fieldDescriptor.getTypeDescriptor())
        .setParameter(true)
        .setFinal(true)
        .build();
  }
}
