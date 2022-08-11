/*
 * Copyright 2022 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.MoreCollectors.toOptional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.UnaryExpression;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Marks instance fields as final if they are effectively final.
 *
 * <p>A field can be made final if:
 *
 * <ul>
 *   <li>it is never modified except for its initialization.
 *   <li>it is not explicitly read before initialization.
 * </ul>
 *
 * <p>There are only three possible mutually exclusive scenarios of where the field initialization
 * can happen for effectively final fields:
 *
 * <ul>
 *   <li>it is only initialized in the declaration.
 *   <li>it is only initialized in an initializer block.
 *   <li>it is initialized only in all non delegating constructors.
 * </ul>
 */
public class MakeFieldsFinal extends LibraryNormalizationPass {

  private final Map<FieldDescriptor, Field> candidates = new HashMap<>();

  @Override
  public void applyTo(Library library) {
    computeFinalFieldCandidates(library);
    removeModifiedFieldsFromCandidates(library);
    markFieldsFinal(library);
  }

  private void computeFinalFieldCandidates(Library library) {
    library.streamTypes().forEach(this::computeFinalFieldCandidates);
  }

  /**
   * Computes the candidate fields to be made final.
   *
   * <p>This involves analyzing just the constructors and initializers, to see if the field
   * initialization meets the proper conditions to be considered final.
   */
  public void computeFinalFieldCandidates(Type type) {

    Set<FieldDescriptor> candidateDescriptors = collectFieldsInitializedLikeFinal(type);

    type.getInstanceFields().stream()
        .filter(f -> candidateDescriptors.contains(f.getDescriptor()))
        .forEach(f -> candidates.put(f.getDescriptor(), f));
  }

  /**
   * Collects fields that satisfy the initialization requirements to be considered final in a type.
   *
   * <p>To simplify the analysis the code in the init method is considered as if run within each
   * constructor, just before the actual code in the constructor.
   *
   * <p>This is a slightly inaccurate model, because the call to the super constructor (and the
   * evaluation of its arguments) happen before the executing the init method. However, such
   * inaccuracy does not affect the correctness of the analysis (Java does not allow to "this" in
   * super constructor arguments, so code like super(this.a) or super(this.a = x) in not valid
   * Java).
   *
   * <p>Since the init method is considered a part of each constructor, fields that satisfy the
   * initialization sequences in all constructors are our initial candidates.
   */
  private Set<FieldDescriptor> collectFieldsInitializedLikeFinal(Type type) {
    List<Statement> initMethodStatements =
        type.getMethods().stream()
            .filter(member -> member.getDescriptor().isInitMethod())
            .collect(toOptional())
            .map(m -> m.getBody().getStatements())
            .orElse(ImmutableList.of());

    return type.getConstructors().stream()
        // Skip delegating constructors. Assignments to fields in delegating constructors make
        // those fields non-final.
        .filter(MakeFieldsFinal::isNonDelegatingConstructor)
        .map(
            c ->
                collectFieldsInitializedLikeFinal(
                    type.getTypeDescriptor(),
                    // Consider the sequence of the code in the init method followed by the code
                    // in the constructor.
                    Streams.concat(
                        initMethodStatements.stream(), c.getBody().getStatements().stream())))
        .reduce(Sets::intersection)
        .orElse(ImmutableSet.of());
  }

  /** Collects fields that satisfy the initialization requirements in a particular constructor. */
  private Set<FieldDescriptor> collectFieldsInitializedLikeFinal(
      DeclaredTypeDescriptor enclosingType, Stream<Statement> statements) {
    Map<FieldDescriptor, State> fieldStates = new HashMap<>();

    // Iterate over the statements in the body classifying instance fields accesses that occur in
    // them.
    //
    // In particular, field assignments at the top level scope are considered valid writes, fields
    // modified by anything other than a simple assignment are invalidated, and so are fields
    // assigned in a scope that is not the top level scope. All other accesses are recorded as
    // reads.
    //
    // Note that this is a conservative analysis and excludes fields that are not initialized at
    // the top level scope but could be declared as final as in:
    //
    // A() {
    //   this.f = 3;  // this will be considered to be a potential initialization.
    //   {
    //      this.g = 4;  // this will be considered a write that excludes the field.
    //   }
    // }
    //
    // A.f is considered to be a potential initialization of an effectively final field, but A.g is
    // excluded since the assignment does not happen in the top level scope. This is a
    // simplification that is unlikely to miss many fields in practice.
    statements.forEach(
        s ->
            s.accept(
                new AbstractVisitor() {
                  @Override
                  public void exitFieldAccess(FieldAccess fieldAccess) {
                    if (!isInstanceFieldAccess(enclosingType, fieldAccess)) {
                      return;
                    }

                    // The parent of a field access is not always an expression.
                    Object parent = getParent();
                    FieldDescriptor target = fieldAccess.getTarget();

                    if (fieldAccess != getModifiedFieldAccess(parent)) {
                      // A simple read, record it and continue traversal.
                      recordFieldRead(fieldStates, target);
                      return;
                    }

                    // At this point the parent is either a unary or binary expression.
                    if (isTopLevelScopeAssignment((Expression) parent)) {
                      recordFieldWrite(fieldStates, target);
                      return;
                    }
                    // This is either a compound assignment or an assignment at the top level,
                    // invalidate the field
                    invalidateField(fieldStates, target);
                  }

                  private boolean isTopLevelScopeAssignment(Expression expression) {
                    if (!expression.isSimpleAssignment()) {
                      return false;
                    }

                    if (!(s instanceof ExpressionStatement)) {
                      // The top level statement is not an expression, hence it can not be an
                      // assignment.
                      return false;
                    }
                    ExpressionStatement expressionStatement = (ExpressionStatement) s;
                    return expressionStatement.getExpression() == expression;
                  }
                }));

    // Finally, return only the fields that are potentially final in this constructor.
    fieldStates.values().removeIf(not(equalTo(State.EXACTLY_ONE_WRITE)));
    return fieldStates.keySet();
  }

  /** Track the state of a candidate in the initialization sequence. */
  private enum State {
    /**
     * Exactly one write have been seen and no read before it. This is the only state in which a
     * field can be considered to be made final.
     */
    EXACTLY_ONE_WRITE,
    /** A read before the first write or multiple writes have been seen. */
    READ_BEFORE_WRITE_OR_MULTIPLE_WRITES
  }

  private static void recordFieldWrite(
      Map<FieldDescriptor, State> fieldStates, FieldDescriptor target) {
    FieldDescriptor declarationDescriptor = target.getDeclarationDescriptor();
    if (fieldStates.putIfAbsent(declarationDescriptor, State.EXACTLY_ONE_WRITE) != null) {
      // Already accessed before; invalidate.
      fieldStates.put(declarationDescriptor, State.READ_BEFORE_WRITE_OR_MULTIPLE_WRITES);
    }
  }

  private static void recordFieldRead(
      Map<FieldDescriptor, State> fieldStates, FieldDescriptor target) {
    // If we haven't recorded an access before; make sure it is invalidated. Otherwise we are OK.
    FieldDescriptor declarationDescriptor = target.getDeclarationDescriptor();
    fieldStates.putIfAbsent(declarationDescriptor, State.READ_BEFORE_WRITE_OR_MULTIPLE_WRITES);
  }

  private static void invalidateField(
      Map<FieldDescriptor, State> fieldStates, FieldDescriptor target) {
    // Invalidates the field.
    FieldDescriptor declarationDescriptor = target.getDeclarationDescriptor();
    fieldStates.put(declarationDescriptor, State.READ_BEFORE_WRITE_OR_MULTIPLE_WRITES);
  }

  /**
   * Remove all fields that are modified from the candidates. Note that initialization doesn't count
   * as a modification.
   */
  private void removeModifiedFieldsFromCandidates(Library library) {
    library.accept(
        new AbstractVisitor() {
          @Override
          public void exitExpression(Expression expression) {
            FieldAccess modifiedField = getModifiedFieldAccess(expression);
            if ((isNonDelegatingConstructor(getCurrentMember())
                    || getCurrentMember().getDescriptor().isInitMethod())
                && isInstanceFieldAccess(getCurrentType().getTypeDescriptor(), modifiedField)) {
              // Some writes in non delegating constructors and initializers are allowed hence they
              // were separately analyzed to determine candidate fields. So we need to ignore them
              // here.
              return;
            }
            if (modifiedField != null) {
              // Any other write to a field makes it not longer a candidate.
              candidates.remove(modifiedField.getTarget().getDeclarationDescriptor());
            }
          }
        });
  }

  /**
   * Make all fields that remain candidates as final by rewriting their declaration and accesses.
   */
  private void markFieldsFinal(Library library) {
    library.accept(
        new AbstractRewriter() {
          @Override
          public Field rewriteField(Field field) {
            FieldDescriptor fieldDescriptor = field.getDescriptor();
            if (!candidates.containsKey(fieldDescriptor)) {
              return field;
            }

            return Field.Builder.from(field)
                .setDescriptor(createFinalFieldDescriptor(fieldDescriptor))
                .build();
          }

          @Override
          public FieldAccess rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
            if (!candidates.containsKey(fieldDescriptor.getDeclarationDescriptor())) {
              return fieldAccess;
            }
            return FieldAccess.Builder.from(fieldAccess)
                .setTarget(createFinalFieldDescriptor(fieldDescriptor))
                .build();
          }
        });
  }

  /**
   * Determines whether the expression is a field access that can be initializing a final field.
   *
   * <p>The form of the field access that is allowed to initialize a final field follow the
   * following rules:
   *
   * <ul>
   *   <li>it is an instance field of the class.
   *   <li>its qualifier has to be implicitly or explicitly {@code this}.
   * </ul>
   */
  private static boolean isInstanceFieldAccess(
      DeclaredTypeDescriptor enclosingType, Expression expression) {
    if (expression instanceof FieldAccess) {
      Expression qualifier = ((FieldAccess) expression).getQualifier();
      FieldDescriptor target = ((FieldAccess) expression).getTarget();
      // Static members should have no qualifiers at this point.
      checkState(target.isInstanceMember() || qualifier == null);

      return qualifier instanceof ThisReference && target.isMemberOf(enclosingType);
    }
    return false;
  }

  /** Returns the field an expression writes to or null if it does not. */
  @Nullable
  private static FieldAccess getModifiedFieldAccess(Object expression) {
    Expression modifiedExpression = getModifiedExpression(expression);
    if (modifiedExpression instanceof FieldAccess) {
      return (FieldAccess) modifiedExpression;
    }
    return null;
  }

  /** Returns the subexpressoin an expression writes to or null if it does not. */
  @Nullable
  private static Expression getModifiedExpression(Object expression) {
    if (expression instanceof UnaryExpression) {
      UnaryExpression unaryExpression = (UnaryExpression) expression;
      return unaryExpression.getOperator().hasSideEffect() ? unaryExpression.getOperand() : null;
    } else if (expression instanceof BinaryExpression) {
      BinaryExpression binaryExpression = (BinaryExpression) expression;
      return binaryExpression.getOperator().hasSideEffect()
          ? binaryExpression.getLeftOperand()
          : null;
    }
    return null;
  }

  private static FieldDescriptor createFinalFieldDescriptor(FieldDescriptor fieldDescriptor) {
    return fieldDescriptor.transform(builder -> builder.setFinal(true));
  }

  private static boolean isNonDelegatingConstructor(Member member) {
    return member.isConstructor() && AstUtils.hasSuperCall((Method) member);
  }
}
