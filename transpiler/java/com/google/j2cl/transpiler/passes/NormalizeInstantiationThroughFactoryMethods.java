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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.toOptional;
import static java.util.stream.Collectors.toCollection;

import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Streams;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionWithComment;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.ThisOrSuperReference;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableReference;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Creates factory methods for each constructor to encapsulate instantiations.
 *
 * <p>During this process it also optimizes fields to be initialized upfront in the struct creation
 * and marks them immutable. This allows optimizations both in the Wasm toolchain and runtime.
 *
 * <p>To decide which fields can be hoisted to the struct creation, constructors need to be analyzed
 * to keep track how the parameters flow from the constructor of a subclass to the place where field
 * initialization is performed in the code.
 *
 * <p>To make the analysis simple, it only tries to hoist fields that are marked as final, since in
 * those cases it can be assumed that a field initialized in the constructor can not be modifed by
 * method calls, or initialization blocks.
 */
public class NormalizeInstantiationThroughFactoryMethods extends LibraryNormalizationPass {

  /**
   * Keeps the flow of values to the delegated constructor call and the initialization of final
   * fields that are present in the body.
   *
   * <p>The idea is to keep track which parameters are passed to the delegated constructor and
   * where, constants or other optimizeable values that are passed to the delegated constructor.
   * Also keeps track of the assignments to the final fields, e.g.
   *
   * <p>
   *
   * <pre>{@code
   * class X {
   *   final a;
   *   final b;
   *   final c;
   *   X(a1, b1, c1) {
   *     this.a = a1;
   *     this.b = b1;
   *     this.c = c1;
   *   }
   *
   *   X(a2) {
   *     this(1, a2, Math.random());
   *   }
   * }
   *
   * class Y extends X {
   *   final d;
   *   Y(a3, b3) {
   *     super(a3, b3, 3)
   *     this.d = Math.random();
   *   }
   * }
   * }</pre>
   *
   * <p>For this snippet three summaries will be created.
   *
   * <p>The initial summary for {@code X(int,int,int)}, will be as follows:
   *
   * <pre>{@code{
   *   delegatedConstructor = null // it does not delegate
   *   // The following assignments from parameters to fields are recorded
   *   a <= a1
   *   b <= b1
   *   c <= c1
   * }}</pre>
   *
   * <p>The initial summary for {@code X(int)}, will be as follows:
   *
   * <pre>{@code{
   *   delegatedConstructor = X(int, int, int)
   *   // Assigments are recorded between variables (which are unique objects in our AST)
   *   a1 <= 1  // parameter a1 in the delegate constructor gets passed a constant, and so forth.
   *   b1 <= a2
   *   c1 <= UNOPTIMIZEABLE // parameter c1 gets an assigned a value that cannot be optimized.
   * }}</pre>
   *
   * <p>The initial summary for {@code Y(int, int)}, will be as follows:
   *
   * <pre>{@code{
   *   delegatedConstructor = X(int, int, int)
   *   a1 <= a3
   *   b1 <= b3
   *   c1 <= 3
   *   d <= UNOPTIMIZEABLE
   * }}</pre>
   *
   * <p>As you can see from the example each constructor summary has information only from the
   * constructor code, the lhs of the mapping can only either be a parameter or a field. The rhs can
   * be:
   *
   * <ul>
   *   <li>a literal,
   *   <li>a parameter reference (the parameter has to be final)
   *   <li>a final field accessed through a parameter reference (the parameter has to be final)
   *   <li>an array creation (empty or literal of valid rhs values)
   *   <li>UNOPTIMIZEABLE, a sentinel to denote the value assigned is not hoistable.
   * </ul>
   *
   * Once the initial summaries are collected, the information is propagated and the summaries are
   * finalized. These final summaries contain only the assignments to the fields that need to be
   * done at each struct creation. And in this example it would end as follows
   *
   * <p>The final summary for {@code X(int,int,int)}:
   *
   * <pre>{@code{
   *   delegatedConstructor = null
   *   a <= a1
   *   b <= b1
   *   c <= UNOPTIMIZEABLE denoting a non optimizeable field.
   * }}</pre>
   *
   * <p>The final summary for {@code X(int)}:
   *
   * <pre>{@code{
   *   delegatedConstructor = X(int, int, int)
   *   // Assigments are recorded between variables (which are unique objects in our AST)
   *   a <= 1  // parameter a1 in the delegate constructor gets passed a constant, and so forth.
   *   b <= a2
   *   c <= UNOPTIMIZEABLE
   * }}</pre>
   *
   * <p>The final summary for {@code Y(int, int)}:
   *
   * <pre>{@code{
   *   delegatedConstructor = X(int, int, int)
   *   a <= a3
   *   b <= b3
   *   c <= UNOPTIMIZEABLE
   *   d <= UNOPTIMIZEABLE
   * }}</pre>
   *
   * <p>At the end the initial values for ALL fields are encoded in the NewInstance call in the
   * factory method.
   *
   * <p>Prerequisites for running this pass: that parameters are marked as final is they are
   * effectively final (needs to run after MakeVariablesFinal pass).
   */
  private static class ConstructorSummary {
    MethodDescriptor delegatedConstructor;
    /**
     * Keeps track of assignments to
     *
     * <ul>
     *   <li>fields in the constructor body
     *   <li>arguments passed to the delegated constructors
     * </ul>
     *
     * and assigned values that could be hoisted such as constants, references to final constructor
     * parameters, array creations etc. If a field or a delegated constructor argument is determined
     * that it should not be hoisted a sentinel value, UNOPTIMIZEABLE, is used instead.
     */
    private final Map<Node, Expression> assignedValues = new HashMap<>();

    /**
     * True once all the propagation is done all the effects that happen of this constructor on all
     * the final instance fields (included the effects of the delegated constructors.
     */
    boolean completed = false;

    void recordAssignment(Node lhs, Expression rhs) {
      // Summaries only track assignments to fields and constructor parameters.
      checkState(lhs instanceof Field || lhs instanceof Variable);

      // The rhs values in the summary might be shared. They need to be cloned in two situations:
      // (1) when they need to be modified (e.g. when the parameter assignments are propagated)
      // (2) when they are finally added to the program AST.
      assignedValues.put(lhs, rhs);
    }

    /**
     * Thread the assignment from constructor summaries to obtain the actual value that will
     * initialize the field.
     */
    void inlineFieldAssignments(ConstructorSummary delegatedConstructorSummary) {
      // At this point the summary of the delegated constructor will only contain fields in
      // the lhs that get assigned by the call to the delegated constructor transitively;
      // the rhs will have expressions that can potentially refer to its parameters. So
      // here we replace these parameter references with the values in the current summary.
      delegatedConstructorSummary.assignedValues.forEach(
          (lhs, rhs) -> recordAssignment(lhs, computeAssignedValue(rhs)));

      // Remove all the intermediate assignments to delegated constructor parameters to preserve
      // only the assignments to the fields; we have just used these in the line above and no longer
      // need them.
      assignedValues.keySet().removeIf(Variable.class::isInstance);
    }

    /**
     * Computes the assigned value to a field by replacing the references to the delegated
     * constructor parameters with the values passed to it by this constructor.
     */
    private Expression computeAssignedValue(Expression value) {
      if (value instanceof VariableReference) {
        return this.assignedValues.get(((VariableReference) value).getTarget());
      }

      // Clone the value since it will be modified below (the original value needs to be preserved
      // since it is used by all other call paths).
      value = value.clone();

      boolean[] invalidated = {false};
      Expression result =
          (Expression)
              value.rewrite(
                  new AbstractRewriter() {
                    @Override
                    public Expression rewriteVariableReference(
                        VariableReference variableReference) {
                      // Found a delegated constructor parameter reference, so look up its value in
                      // the current summary.
                      Expression variableValue = assignedValues.get(variableReference.getTarget());

                      if (variableValue == UNOPTIMIZEABLE) {
                        // Instead of letting unoptimizeable values leak into deep constructs,
                        // like an array literal [1, a, UNOPTIMIZEABLE], we invalidate the rhs
                        // as a whole.
                        invalidated[0] = true;
                      }
                      return variableValue;
                    }
                  });
      return invalidated[0] ? UNOPTIMIZEABLE : result;
    }
  }

  /**
   * Set of classes that might do a dynamic dispatch from a constructor or init.
   *
   * <p>Final fields in subclasses of these can be observed uninitialized.
   */
  private final Set<TypeDeclaration> classesThatMightDoDynamicDispatchFromConstructors =
      new HashSet<>();

  private final Map<TypeDeclaration, Type> typesByDeclaration = new HashMap<>();
  private final Map<MethodDescriptor, Method> constructorsByMethodDescriptor = new HashMap<>();
  private final ListMultimap<TypeDeclaration, Field> fieldsByTypeDeclaration =
      ArrayListMultimap.create();
  private final Map<FieldDescriptor, Field> instanceFieldsByFieldDescriptor = new HashMap<>();

  /**
   * Summaries for each constructor detailing the flow of values to the delegated constructor and
   * the assignments to the final fields.
   */
  private final Map<MethodDescriptor, ConstructorSummary> constructorSummaries = new HashMap<>();

  /** Sentinel to simplify the represetantiation of non optimizeable assignments. */
  private static final Expression UNOPTIMIZEABLE =
      new Expression() {
        @Override
        public TypeDescriptor getTypeDescriptor() {
          throw new UnsupportedOperationException();
        }

        @Override
        public Precedence getPrecedence() {
          throw new UnsupportedOperationException();
        }

        @Override
        public Expression clone() {
          return this;
        }
      };

  @Override
  public void applyTo(Library library) {
    // 1. do the immutability analysis.
    computeClassesThatMightDoDynamicDispatchFromConstructors(library);
    collectDescriptorMappings(library);
    initializeConstructorSummaries(library);
    propagateConstructorSummaries(library);
    computeImmutableFields(library);

    // 2. Actually implement instantiation using factory methods.
    replaceNewInstancesWithFactoryMethodCalls(library);
    rewriteConstructors(library);
  }

  private void computeClassesThatMightDoDynamicDispatchFromConstructors(Library library) {
    library
        .streamTypes()
        .forEach(
            t -> {
              if (canThisEscapeFromConstructorOrInit(t)) {
                classesThatMightDoDynamicDispatchFromConstructors.add(t.getDeclaration());
              }
            });
  }

  /**
   * Returns true if there is a possibility for one of the constructors to directly or indirectly
   * perform a dynamic dispatch on this class.
   */
  private static boolean canThisEscapeFromConstructorOrInit(Type type) {
    // Only look at code that runs at initialization, that includes constructors and the init
    // method, as a virtual method call in any of these in reachable from either of those
    // places can observe uninitialized values.
    //
    // Note that since the pass is run after instance initialization normalization, there will be no
    // InitializerBlocks and the Fields will have initializers that don't need to be looked at.
    // However for completeness they are not skipped during filtering below.
    return type.getInstanceMembers().stream()
        .filter(
            member ->
                !member.isMethod()
                    || member.isConstructor()
                    || member.getDescriptor().isInitMethod())
        .anyMatch(NormalizeInstantiationThroughFactoryMethods::doesThisEscapeFromMember);
  }

  /** Returns true if the implicit instance reference escapes out of the member. */
  private static boolean doesThisEscapeFromMember(Member member) {
    boolean[] hasPotentiallyEscapingThis = {false};
    member.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterFieldAccess(FieldAccess fieldAccess) {
            // Field accesses through "this" reference are allowed, all other usages of "this"
            // or "super" will be considered escaping references and invalidate the constructor.
            return !(fieldAccess.getQualifier() instanceof ThisReference);
          }

          @Override
          public boolean enterMethodCall(MethodCall methodCall) {
            if (methodCall.getTarget().isInitMethod()) {
              // Since "init" is an instance method its qualifier will be "this". However since we
              // are separately analyzing "init" methods to check if "this" escapes from them, we
              // need to not flag the "this" qualifier as a potential escape.

              // Skip the qualifier in the call to init (but still visit its arguments, even though
              // with the current normalizations we don't expect to have any).
              methodCall.getArguments().forEach(arg -> arg.accept(this));
              return false;
            }
            return true;
          }

          @Override
          public void exitThisOrSuperReference(ThisOrSuperReference receiverReference) {
            // Dispatches through "this" or "super" make the receiver available to the called
            // method, making the value potentially escape.
            hasPotentiallyEscapingThis[0] = true;
          }
        });

    return hasPotentiallyEscapingThis[0];
  }

  /** Build some mappings to allow direct access to types, methods and fields. */
  private void collectDescriptorMappings(Library library) {
    library.streamTypes().forEach(t -> typesByDeclaration.put(t.getDeclaration(), t));
    library
        .streamTypes()
        .flatMap(t -> t.getConstructors().stream())
        .forEach(c -> constructorsByMethodDescriptor.put(c.getDescriptor(), c));
    library
        .streamTypes()
        .flatMap(t -> t.getInstanceFields().stream())
        .forEach(
            c ->
                instanceFieldsByFieldDescriptor.put(
                    c.getDescriptor().getDeclarationDescriptor(), c));
  }

  /** Collects the initial constructor summary for all constructors in the library. */
  private void initializeConstructorSummaries(Library library) {
    library
        .streamTypes()
        .forEach(
            t -> {
              for (Method constructor : t.getConstructors()) {
                constructorSummaries.put(
                    constructor.getDescriptor(),
                    buildInitialConstructorSummary(constructor, getInitMethod(t)));
              }
            });
  }

  /**
   * Builds the initial constructor summary.
   *
   * <p>This summary only includes the information directly contained in the constructor body; i.e.
   * all assignments to final instance fields and the arguments passed to the delegated constructor.
   */
  private ConstructorSummary buildInitialConstructorSummary(Method constructor, Method initMethod) {
    ConstructorSummary constructorSummary = new ConstructorSummary();

    MethodCall delegatedConstructorCall = AstUtils.getConstructorInvocation(constructor);
    if (delegatedConstructorCall != null) {
      MethodDescriptor delegatedConstructor =
          delegatedConstructorCall.getTarget().getDeclarationDescriptor();
      constructorSummary.delegatedConstructor = delegatedConstructor;

      List<Variable> delegatedConstructorParameters =
          constructorsByMethodDescriptor.get(delegatedConstructor).getParameters();

      // Record the relation between the parameter in the delegated constructor and the value that
      // is passed.
      // Uses the fact that variable objects (like parameters) are unique.
      Streams.forEachPair(
          delegatedConstructorParameters.stream(),
          delegatedConstructorCall.getArguments().stream(),
          (parameter, argument) ->
              constructorSummary.recordAssignment(
                  parameter, getImmutableFieldInitializer(argument)));
    }

    // Collect assignments to potentially immutable instance fields, by looking at the body of
    // the instance initializers and the constructor.
    SetMultimap<Field, Expression> immutableFieldAssignments =
        collectImmutableFieldAssignments(initMethod, constructor);

    immutableFieldAssignments
        .asMap()
        .forEach(
            (field, assignments) ->
                constructorSummary.recordAssignment(
                    field, getImmutableFieldInitializer(assignments)));
    return constructorSummary;
  }

  private Expression getImmutableFieldInitializer(Collection<Expression> assignments) {
    // For a field that is assigned the constructors to be considered immutable, it has to
    // be assigned only once and to an optimizeable initial value.
    return (assignments.size() == 1)
        ? getImmutableFieldInitializer(Iterables.getOnlyElement(assignments))
        : UNOPTIMIZEABLE;
  }

  private Expression getImmutableFieldInitializer(Expression expression) {
    return isValidImmutableFieldInitializer(expression) ? expression : UNOPTIMIZEABLE;
  }

  /** Collects all assignments to potentially immutable fields in statements. */
  private SetMultimap<Field, Expression> collectImmutableFieldAssignments(
      Method initMethod, Method constructor) {
    List<Statement> statements = new ArrayList<>();
    // Consider the statements of the init method before the ones in the constructor, even though
    // the current implementation does not consider ordering.
    if (initMethod != null) {
      statements.add(initMethod.getBody());
    }
    statements.add(constructor.getBody());

    SetMultimap<Field, Expression> initializersByFinalInstanceFields = HashMultimap.create();
    statements.forEach(
        s ->
            s.accept(
                new AbstractVisitor() {
                  @Override
                  public void exitBinaryExpression(BinaryExpression binaryExpression) {
                    Expression lhs = binaryExpression.getLeftOperand();
                    if (binaryExpression.getOperator().isSimpleAssignment()
                        && lhs instanceof FieldAccess) {

                      FieldDescriptor target = ((FieldAccess) lhs).getTarget();
                      if (target.isFinal() && !target.isStatic()) {
                        initializersByFinalInstanceFields.put(
                            instanceFieldsByFieldDescriptor.get(target.getDeclarationDescriptor()),
                            binaryExpression.getRightOperand());
                      }
                    }
                  }
                }));
    return initializersByFinalInstanceFields;
  }

  /**
   * Returns {@code true} if the expression used to initialize a final field can be hoisted and
   * executed in a different order.
   *
   * <p>This is required since all the immutable fields will have to be initialized when the struct
   * is instantiated, which happens before the code int the constructors and init methods is
   * executed.
   */
  private static boolean isValidImmutableFieldInitializer(Expression rhs) {
    if (rhs instanceof VariableReference) {
      Variable variable = ((VariableReference) rhs).getTarget();
      // A reference to a final parameter of the constructor can be hoisted.
      return variable.isParameter() && variable.isFinal();
    }

    if (rhs instanceof NewArray) {
      NewArray newArray = (NewArray) rhs;
      checkState(newArray.getInitializer() == null);
      return newArray.getDimensionExpressions().stream()
          .allMatch(NormalizeInstantiationThroughFactoryMethods::isValidImmutableFieldInitializer);
    }
    if (rhs instanceof ArrayLiteral) {
      ArrayLiteral arrayLiteral = (ArrayLiteral) rhs;
      return arrayLiteral.getValueExpressions().stream()
          .allMatch(NormalizeInstantiationThroughFactoryMethods::isValidImmutableFieldInitializer);
    }

    if (rhs instanceof CastExpression) {
      CastExpression castExpression = (CastExpression) rhs;
      return isValidImmutableFieldInitializer(castExpression.getExpression());
    }

    if (rhs instanceof JsDocCastExpression) {
      JsDocCastExpression castExpression = (JsDocCastExpression) rhs;
      return isValidImmutableFieldInitializer(castExpression.getExpression());
    }

    if (rhs instanceof ExpressionWithComment) {
      ExpressionWithComment expressionWithComment = (ExpressionWithComment) rhs;
      return isValidImmutableFieldInitializer(expressionWithComment.getExpression());
    }

    if (rhs instanceof FieldAccess) {
      // A final field has two states; either the uninitialized state or the initialized. However,
      // hoisting them will not change which state is observed from other constructors, since the
      // transition cannot happen during this constructor call (i.e. the value in the hoisted
      // location will be same as the value that would have observed during constructor execution).
      FieldAccess fieldAccess = (FieldAccess) rhs;
      FieldDescriptor target = fieldAccess.getTarget();
      return target.isFinal()
          && target.isInstanceMember()
          && isValidImmutableFieldInitializer(fieldAccess.getQualifier());
    }

    if (rhs instanceof Invocation) {
      // Many constructs have already been lowered here, e.g. string literals, class literals and
      // array creations and rewritten as method calls.
      Invocation invocation = (Invocation) rhs;
      MethodDescriptor target = invocation.getTarget();
      if (isOptimizeableMethodCall(target)) {
        return invocation.getArguments().stream()
            .allMatch(
                NormalizeInstantiationThroughFactoryMethods::isValidImmutableFieldInitializer);
      }
    }

    // All compile time constants are optimizeable, that includes all the primitive type literals.
    return rhs.isCompileTimeConstant();
  }

  /**
   * Returns true for methods that are the implementation of optimizeable constructs like string
   * literals, class literals or new arrays.
   */
  private static boolean isOptimizeableMethodCall(MethodDescriptor target) {
    // TODO(b/229382504): Encode the origin information so that the implementation does not have to
    // have hardcoded names.

    // Array creations, array literals, string literals and class literals have already been lowered
    // so match by recognizing the method calls.
    if (target.getQualifiedBinaryName().equals("javaemul.internal.WasmArray.createMultiDimensional")
        || (target.isConstructor()
            && TypeDescriptors.isWasmArraySubtype(target.getEnclosingTypeDescriptor()))) {
      // These are NewArray and ArrayLiteral.
      return true;
    }

    // Class and string literals
    return target.getOrigin() == MethodOrigin.SYNTHETIC_CLASS_LITERAL_GETTER
        || target.getOrigin() == MethodOrigin.SYNTHETIC_STRING_LITERAL_GETTER;
  }

  /** Propagates transitively the constructor summaries. */
  private void propagateConstructorSummaries(Library library) {
    library
        .streamTypes()
        .flatMap(t -> t.getConstructors().stream())
        .forEach(c -> finalizeConstructorSummary(constructorSummaries.get(c.getDescriptor())));
  }

  /**
   * Propagates the summary information so that assignments to potentially immutable fields that are
   * declared in supertypes are collected for each constructor of all subclasses.
   */
  private void finalizeConstructorSummary(ConstructorSummary constructorSummary) {
    if (constructorSummary.completed) {
      return;
    }

    ConstructorSummary delegatedConstructorSummary =
        constructorSummaries.get(constructorSummary.delegatedConstructor);
    if (delegatedConstructorSummary == null) {
      return;
    }

    // Compute the complete transitive summary information for the delegated constructor.
    finalizeConstructorSummary(delegatedConstructorSummary);

    // With the finalized summary for the delegated constructor, we can thread the values passed
    // in the delegation call and add the effects of the fields assignments from the delegated
    // constructor in this summary, completing the current summary.
    constructorSummary.inlineFieldAssignments(delegatedConstructorSummary);

    // Finally, mark it as complete since it does not need to be computed for each possible caller.
    constructorSummary.completed = true;
  }

  /**
   * Determines which fields can be made immutable and updates the constructor summaries to reflect
   * that information.
   */
  private void computeImmutableFields(Library library) {
    // Collect potential hoisteable fields.
    Set<Field> hoisteableFields =
        constructorSummaries.values().stream()
            .flatMap(summary -> summary.assignedValues.keySet().stream())
            .map(Field.class::cast)
            .collect(toCollection(HashSet::new));

    // If a field can not be hoisted under all possible constructor delegation calls, remove
    // it from consideration.
    for (ConstructorSummary constructorSummary : constructorSummaries.values()) {
      for (Entry<Node, Expression> parameterMapping :
          constructorSummary.assignedValues.entrySet()) {
        Field field = (Field) parameterMapping.getKey();
        if (parameterMapping.getValue() == UNOPTIMIZEABLE
            || !isOptimizeable(
                field.getDescriptor().getEnclosingTypeDescriptor().getTypeDeclaration())) {
          // Field is assigned a value that can not be hoisted in this particular constructor.
          hoisteableFields.remove(field);
        }
      }
    }

    // Remove all fields that are not hoisteable from all the summaries, since the summaries will
    // be used to compute the initial values for the fields.
    for (ConstructorSummary constructorSummary : constructorSummaries.values()) {
      constructorSummary
          .assignedValues
          .keySet()
          .removeIf(Predicates.not(hoisteableFields::contains));
    }

    // Remove all assignments from hoisteable fields in the code, those are the only statements in
    // the code that can change the value of these fields since they are final. No need to worry
    // about compound, nor unary sideeffecting operations.
    library.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            Expression lhs = binaryExpression.getLeftOperand();
            if (binaryExpression.getOperator().isSimpleAssignment() && lhs instanceof FieldAccess) {
              FieldAccess fieldAccess = (FieldAccess) lhs;
              Field field =
                  instanceFieldsByFieldDescriptor.get(
                      fieldAccess.getTarget().getDeclarationDescriptor());
              if (field != null && hoisteableFields.contains(field)) {
                // Remove the assignment of the field. Its initial value is still retained in the
                // constructor summary.
                return fieldAccess.getTypeDescriptor().getDefaultValue();
              }
            }
            return binaryExpression;
          }
        });

    // Finally, mark the fields as immutable.
    hoisteableFields.forEach(f -> f.setImmutable(true));
  }

  /** Returns true if uninitialized fields cannot be observed in this class. */
  private boolean isOptimizeable(TypeDeclaration typeDeclaration) {
    if (classesThatMightDoDynamicDispatchFromConstructors.contains(typeDeclaration)) {
      return false;
    }

    TypeDeclaration superTypeDeclaration = typeDeclaration.getSuperTypeDeclaration();
    if (superTypeDeclaration != null && !isOptimizeable(superTypeDeclaration)) {
      // Add the class as unoptimizeable to memoize the result and avoid unnecessary recursive
      // traversals.
      classesThatMightDoDynamicDispatchFromConstructors.add(typeDeclaration);
      return false;
    }
    return true;
  }

  /** Rewrite NewInstance to be a MethodCall to the $create factory method. */
  private void replaceNewInstancesWithFactoryMethodCalls(Library library) {
    // Replace all the NewInstances in the library AST tree.
    doReplaceNewInstancesWithFactoryMethodCalls(library);
    // Replace all the NewInstances in the AST fragments that represent right hand sides in
    // the constructor summaries, since they wil need to be reintroduced to the AST later on.

    // These summaries are shared by it is safe to traverse them multimple time since the
    // NewInstance will be rewritten to a MethodCall and no longer be handled when found in the
    // subsequent traversals.
    constructorSummaries.forEach(
        (methodDescriptor, summary) ->
            summary.assignedValues.replaceAll(
                (lhs, rhs) -> (Expression) doReplaceNewInstancesWithFactoryMethodCalls(rhs)));
  }

  /** Rewrite NewInstance to be a MethodCall to the $create factory method. */
  private static Node doReplaceNewInstancesWithFactoryMethodCalls(Node node) {
    return node.rewrite(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewInstance(NewInstance constructorInvocation) {
            if (constructorInvocation.getTarget().getEnclosingTypeDescriptor().isNative()) {
              return constructorInvocation;
            }
            return MethodCall.Builder.from(
                    getFactoryDescriptorForConstructor(constructorInvocation.getTarget()))
                .setArguments(constructorInvocation.getArguments())
                .build();
          }
        });
  }

  private void rewriteConstructors(Library library) {
    library
        .streamTypes()
        .filter(Predicates.not(Type::isNative))
        .filter(t -> !t.getConstructors().isEmpty())
        .forEach(
            t -> {
              insertFactoryMethods(t);
              rewriteConstructorsAsCtorMethods(t);
            });
  }

  /** Inserts $create methods for each constructor. */
  private void insertFactoryMethods(Type type) {
    if (type.isAbstract()) {
      return;
    }
    List<Member> members = type.getMembers();
    for (int i = 0; i < members.size(); i++) {
      if (!members.get(i).isConstructor()) {
        continue;
      }
      Method method = (Method) members.get(i);

      // Insert the factory method just before the corresponding constructor, and advance.
      members.add(i++, synthesizeFactoryMethod(method, type.getTypeDescriptor()));
    }
  }

  private static void rewriteConstructorsAsCtorMethods(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            if (!method.isConstructor()) {
              return method;
            }

            return Method.newBuilder()
                .setMethodDescriptor(getCtorMethodDescriptorForConstructor(method.getDescriptor()))
                .setParameters(method.getParameters())
                .addStatements(method.getBody().getStatements())
                .setSourcePosition(method.getSourcePosition())
                .build();
          }

          // Rewrite super() constructor calls.
          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.getTarget().isConstructor()) {
              return methodCall;
            }

            return MethodCall.Builder.from(
                    getCtorMethodDescriptorForConstructor(methodCall.getTarget()))
                .setQualifier(
                    methodCall.getQualifier() == null
                        ? new ThisReference(methodCall.getTarget().getEnclosingTypeDescriptor())
                        : methodCall.getQualifier())
                .setArguments(methodCall.getArguments())
                .build();
          }
        });
  }

  /**
   * Generates code of the form:
   *
   * <pre>{@code
   * static $create(args)
   *   Type $instance = new Type(<original arguments>);
   *   $instance.$ctor...(args);
   *   return $instance;
   * }</pre>
   */
  private Method synthesizeFactoryMethod(Method constructor, DeclaredTypeDescriptor enclosingType) {

    List<Statement> statements = new ArrayList<>();

    List<Variable> factoryMethodParameters = AstUtils.clone(constructor.getParameters());

    Variable newInstance =
        Variable.newBuilder().setName("$instance").setTypeDescriptor(enclosingType).build();

    SourcePosition constructorSourcePosition = constructor.getSourcePosition();

    List<Field> instanceFields = getAllInstanceFields(enclosingType.getTypeDeclaration());

    // Type $instance = new Type(<initial values of the struct fields>);
    Statement newInstanceStatement =
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(
                newInstance,
                NewInstance.Builder.from(
                        getStructNewMethodDescriptor(
                            enclosingType,
                            instanceFields.stream()
                                .map(f -> f.getDescriptor().getTypeDescriptor())
                                .collect(toImmutableList())))
                    .setArguments(
                        instanceFields.stream()
                            .map(
                                f ->
                                    computetInitialFieldValue(
                                        f, constructor, factoryMethodParameters))
                            .collect(toImmutableList()))
                    .build())
            .build()
            .makeStatement(constructorSourcePosition);
    statements.add(newInstanceStatement);

    // $instance.$ctor...();
    Statement ctorCallStatement =
        MethodCall.Builder.from(constructor.getDescriptor())
            .setQualifier(newInstance.createReference())
            .setArguments(AstUtils.getReferences(factoryMethodParameters))
            .build()
            .makeStatement(constructorSourcePosition);
    statements.add(ctorCallStatement);

    if (enclosingType.isAssignableTo(TypeDescriptors.get().javaLangThrowable)) {
      // $instance.privateInitError(Exceptions.createJsError);
      statements.add(
          createThrowableInit(newInstance.createReference())
              .makeStatement(constructorSourcePosition));
    }

    // return $instance
    Statement returnStatement =
        ReturnStatement.newBuilder()
            .setExpression(newInstance.createReference())
            .setSourcePosition(constructorSourcePosition)
            .build();
    statements.add(returnStatement);

    return Method.newBuilder()
        .setMethodDescriptor(getFactoryDescriptorForConstructor(constructor.getDescriptor()))
        .setParameters(factoryMethodParameters)
        .addStatements(statements)
        .setSourcePosition(constructorSourcePosition)
        .build();
  }

  /** Returns the initialization value for a field in a struct.new. */
  private Expression computetInitialFieldValue(
      Field field, Method constructor, List<Variable> factoryMethodParameters) {

    // Get the constructor summary. It contains the initial values for all immutable fields in
    // the object.
    ConstructorSummary constructorSummary = constructorSummaries.get(constructor.getDescriptor());
    List<Variable> constructorParameters = constructor.getParameters();

    if (constructorSummary.assignedValues.containsKey(field) && field.isImmutable()) {
      // The field is immutable and assigned in the constructor, gets its value and clone it
      // before modifying it since the values in the summary might be shared.
      Expression assignedValue = constructorSummary.assignedValues.get(field).clone();

      return AstUtils.replaceDeclarations(
          constructorParameters, factoryMethodParameters, assignedValue);
    }

    Expression initializer = field.getInitializer();
    if (initializer != null) {
      // The field might be an immutable field that is assigned at declaration, clone its value
      // as it might be shared by more than one constructor chain.
      return initializer.clone();
    }

    return field.getDescriptor().getTypeDescriptor().getDefaultValue();
  }

  /** Method descriptor for $ctor methods. */
  private static MethodDescriptor getCtorMethodDescriptorForConstructor(
      MethodDescriptor constructor) {
    checkArgument(constructor.isConstructor());
    return constructor.transform(
        builder ->
            builder
                .setReturnTypeDescriptor(PrimitiveTypes.VOID)
                .setName(MethodDescriptor.CTOR_METHOD_PREFIX)
                .setConstructor(false)
                .setStatic(false)
                .setOrigin(MethodOrigin.SYNTHETIC_CTOR_FOR_CONSTRUCTOR)
                // Set to private to avoid putting the method in the vtable. (These methods might
                // be called by a subclass though)
                .setVisibility(Visibility.PRIVATE)
                .setOriginalJsInfo(JsInfo.NONE)
                // Clear side effect free flag since the ctor method does not return a value and
                // would be
                // pruned incorrectly.
                // The factory method still preserves the side effect free flags if it was present.
                .setSideEffectFree(false));
  }

  /** Method descriptor for $create methods. */
  private static MethodDescriptor getFactoryDescriptorForConstructor(MethodDescriptor constructor) {
    checkArgument(constructor.isConstructor());
    return constructor.transform(
        builder ->
            builder
                .setStatic(true)
                .setName(MethodDescriptor.CREATE_METHOD_NAME)
                .setConstructor(false)
                .addTypeParameterTypeDescriptors(
                    0,
                    builder
                        .getEnclosingTypeDescriptor()
                        .getTypeDeclaration()
                        .getTypeParameterDescriptors())
                .setOrigin(MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR)
                .setOriginalJsInfo(JsInfo.NONE)
                .setVisibility(Visibility.PUBLIC));
  }

  private List<Field> getAllInstanceFields(TypeDeclaration typeDeclaration) {
    if (fieldsByTypeDeclaration.containsKey(typeDeclaration)) {
      return fieldsByTypeDeclaration.get(typeDeclaration);
    }

    TypeDeclaration superTypeDeclaration = typeDeclaration.getSuperTypeDeclaration();
    List<Field> superTypeFields =
        superTypeDeclaration == null
            ? ImmutableList.of()
            : getAllInstanceFields(superTypeDeclaration);
    ImmutableList<Field> instanceFields =
        ImmutableList.<Field>builder()
            .addAll(superTypeFields)
            .addAll(typesByDeclaration.get(typeDeclaration).getInstanceFields())
            .build();
    fieldsByTypeDeclaration.putAll(typeDeclaration, instanceFields);
    return instanceFields;
  }

  /** Method descriptor for the implicit new instance constructor */
  private static MethodDescriptor getStructNewMethodDescriptor(
      DeclaredTypeDescriptor enclosingType, List<TypeDescriptor> immutableFieldDescriptors) {
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingType)
        .setParameterTypeDescriptors(immutableFieldDescriptors)
        .setConstructor(true)
        .setVisibility(Visibility.PUBLIC)
        // TODO(b/229382504): introduce a new synthetic method here.
        .setOrigin(MethodOrigin.SYNTHETIC_NOOP_JAVASCRIPT_CONSTRUCTOR)
        .build();
  }

  /** Returns the init method for a type if it has one. */
  @Nullable
  private static Method getInitMethod(Type type) {
    return type.getMethods().stream()
        .filter(m -> m.getDescriptor().isInitMethod())
        .collect(toOptional())
        .orElse(null);
  }

  private static Expression createThrowableInit(Expression newInstanceRef) {
    return RuntimeMethods.createThrowableInitMethodCall(
        newInstanceRef,
        RuntimeMethods.createExceptionsMethodCall("createJsError", newInstanceRef.clone()));
  }
}
