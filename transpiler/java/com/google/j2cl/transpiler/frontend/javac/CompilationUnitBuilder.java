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
package com.google.j2cl.transpiler.frontend.javac;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveVoid;
import static java.util.stream.Collectors.toCollection;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.FilePosition;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AnyPattern;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayCreationReference;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AssertStatement;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BindingPattern;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CatchClause;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.DoWhileStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.ForEachStatement;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabelReference;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.LocalClassDeclarationStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodReference;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.Pattern;
import com.google.j2cl.transpiler.ast.PatternMatchExpression;
import com.google.j2cl.transpiler.ast.PostfixExpression;
import com.google.j2cl.transpiler.ast.PrefixExpression;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RecordPattern;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchCaseDefault;
import com.google.j2cl.transpiler.ast.SwitchCaseExpressions;
import com.google.j2cl.transpiler.ast.SwitchCasePattern;
import com.google.j2cl.transpiler.ast.SwitchExpression;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.SynchronizedStatement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TryStatement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnaryExpression;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.WhileStatement;
import com.google.j2cl.transpiler.ast.YieldStatement;
import com.google.j2cl.transpiler.frontend.common.AbstractCompilationUnitBuilder;
import com.sun.source.tree.CaseTree.CaseKind;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.DefaultCaseLabelTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.RecordComponent;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnyPattern;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBindingPattern;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCConstantCaseLabel;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMemberReference.ReferenceKind;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCPattern;
import com.sun.tools.javac.tree.JCTree.JCPatternCaseLabel;
import com.sun.tools.javac.tree.JCTree.JCRecordPattern;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSwitchExpression;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCYield;
import com.sun.tools.javac.tree.JCTree.Tag;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/** Creates a J2CL Java AST from the AST provided by JavaC. */
@SuppressWarnings("ASTHelpersSuggestions")
public class CompilationUnitBuilder extends AbstractCompilationUnitBuilder {
  private final JavaEnvironment environment;
  private final Problems problems;

  private final Map<VariableElement, Variable> variableByVariableElement = new HashMap<>();
  // Keeps track of labels that are currently in scope. Even though labels cannot have the
  // same name if they are nested in the same method body, labels with the same name could
  // be lexically nested by being in different methods bodies, e.g. from local or anonymous
  // classes or lambdas.
  private final Map<String, Deque<Label>> labelsInScope = new HashMap<>();
  private JCCompilationUnit javacUnit;

  CompilationUnitBuilder(JavaEnvironment environment, Problems problems) {
    this.environment = environment;
    this.problems = problems;
    this.problems.abortIfCancelled();
  }

  /**
   * Constructs a type and maintains the type stack.
   *
   * @return The created {@link Type}.
   */
  private Type convertType(
      ClassSymbol typeElement, List<JCTree> bodyDeclarations, JCTree sourcePositionNode) {
    Type type = createType(typeElement, sourcePositionNode);
    return processEnclosedBy(
        type,
        () -> {
          convertTypeBody(type, bodyDeclarations);
          return type;
        });
  }

  @Nullable
  private Type createType(ClassSymbol typeElement, JCTree sourcePositionNode) {
    if (typeElement == null) {
      return null;
    }
    TypeDeclaration typeDeclaration = environment.createTypeDeclaration(typeElement);

    return new Type(
        typeDeclaration.isAnonymous()
            ? getSourcePosition(sourcePositionNode)
            : getNamePosition(sourcePositionNode),
        typeDeclaration,
        typeElement.isRecord());
  }

  private Type convertClassDeclaration(JCClassDecl classDecl) {
    return convertClassDeclaration(classDecl, classDecl);
  }

  private Type convertClassDeclaration(JCClassDecl classDecl, JCTree sourcePositionNode) {
    return convertType(classDecl.sym, classDecl.getMembers(), sourcePositionNode);
  }

  private void convertTypeBody(Type type, List<JCTree> bodyDeclarations) {
    problems.abortIfCancelled();
    for (JCTree bodyDeclaration : bodyDeclarations) {
      switch (bodyDeclaration) {
        case JCVariableDecl fieldDeclaration ->
            type.addMember(convertFieldDeclaration(fieldDeclaration));

        case JCMethodDecl methodDeclaration -> {
          if ((methodDeclaration.mods.flags & Flags.GENERATEDCONSTR) != 0
              && (methodDeclaration.mods.flags & Flags.ANONCONSTR) == 0) {
            // Skip constructors that are generated by javac. This allows to differentiate between
            // a user written empty constructor and an implicit (generated) constructor.
            // J2CL already has logic to synthesize default constructors for JDT.
            // TODO(b/135123615): When the migration is completed, the pass synthesizing default
            // constructors can be removed by adding isGenerated or isSynthetic to Method.
            continue;
          }
          type.addMember(convertMethodDeclaration(methodDeclaration));
        }

        case JCBlock initializer -> {
          Block block = convertBlock(initializer);
          if (initializer.isStatic()) {
            type.addStaticInitializerBlock(block);
          } else {
            type.addInstanceInitializerBlock(block);
          }
        }

        case JCClassDecl nestedTypeDeclaration ->
            // Nested class
            type.addType(convertClassDeclaration(nestedTypeDeclaration));

        default ->
            throw internalCompilerError(
                "Unimplemented translation for BodyDeclaration type: %s.",
                bodyDeclaration.getClass().getName());
      }
      problems.abortIfCancelled();
    }
  }

  private Field convertFieldDeclaration(JCVariableDecl fieldDeclaration) {
    Expression initializer;
    VariableElement variableElement = fieldDeclaration.sym;
    initializer = convertExpressionOrNull(fieldDeclaration.getInitializer());
    return Field.Builder.from(environment.createFieldDescriptor(variableElement))
        .setInitializer(initializer)
        .setSourcePosition(getNamePosition(fieldDeclaration))
        .build();
  }

  private Method convertMethodDeclaration(JCMethodDecl methodDeclaration) {
    // If a method has no body, initialize the body with an empty list of statements.
    List<Variable> parameters = new ArrayList<>();
    for (JCVariableDecl parameter : methodDeclaration.getParameters()) {
      parameters.add(createVariable(parameter, true));
    }

    Method.Builder builder =
        newMethodBuilder(methodDeclaration.sym)
            .setSourcePosition(getNamePosition(methodDeclaration))
            .setParameters(parameters);

    if (methodDeclaration.getBody() != null) {
      builder.setBody(convertBlock(methodDeclaration.getBody()));
    }

    return builder.build();
  }

  private Block convertBlock(JCBlock block) {
    return Block.newBuilder()
        .setSourcePosition(getSourcePosition(block))
        .setStatements(
            block.getStatements().stream()
                .map(this::convertStatement)
                .filter(Predicates.notNull())
                .collect(toImmutableList()))
        .build();
  }

  private Variable createVariable(JCVariableDecl variableDeclaration, boolean isParameter) {
    VarSymbol variableElement = variableDeclaration.sym;
    String name =
        variableElement.getSimpleName().isEmpty()
            ? "_"
            : variableElement.getSimpleName().toString();
    Variable variable =
        environment.createVariable(
            getNamePosition(name, variableDeclaration),
            name,
            variableElement,
            isParameter,
            inNullMarkedScope());
    variableByVariableElement.put(variableElement, variable);
    return variable;
  }

  private Method.Builder newMethodBuilder(ExecutableElement methodElement) {
    MethodDescriptor methodDescriptor = environment.createMethodDescriptor(methodElement);
    return Method.newBuilder().setMethodDescriptor(methodDescriptor);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////
  // Statements.
  //////////////////////////////////////////////////////////////////////////////////////////////

  private AssertStatement convertAssert(JCAssert statement) {
    return AssertStatement.newBuilder()
        .setSourcePosition(getSourcePosition(statement))
        .setExpression(convertExpression(statement.getCondition()))
        .setMessage(convertExpressionOrNull(statement.getDetail()))
        .build();
  }

  private LabeledStatement convertLabeledStatement(JCLabeledStatement statement) {
    Label label = Label.newBuilder().setName(statement.getLabel().toString()).build();
    labelsInScope.computeIfAbsent(label.getName(), n -> new ArrayDeque<>()).push(label);
    LabeledStatement labeledStatment =
        LabeledStatement.newBuilder()
            .setSourcePosition(getSourcePosition(statement))
            .setLabel(label)
            .setStatement(convertStatement(statement.getStatement()))
            .build();
    labelsInScope.get(label.getName()).pop();
    return labeledStatment;
  }

  private BreakStatement convertBreak(JCBreak statement) {
    return BreakStatement.newBuilder()
        .setSourcePosition(getSourcePosition(statement))
        .setLabelReference(getLabelReferenceOrNull(statement.getLabel()))
        .build();
  }

  private ContinueStatement convertContinue(JCContinue statement) {
    return ContinueStatement.newBuilder()
        .setSourcePosition(getSourcePosition(statement))
        .setLabelReference(getLabelReferenceOrNull(statement.getLabel()))
        .build();
  }

  @Nullable
  private LabelReference getLabelReferenceOrNull(Name label) {
    return label == null ? null : labelsInScope.get(label.toString()).peek().createReference();
  }

  private DoWhileStatement convertDoWhileLoop(JCDoWhileLoop statement) {
    return DoWhileStatement.newBuilder()
        .setSourcePosition(getSourcePosition(statement))
        .setConditionExpression(convertExpression(statement.getCondition()))
        .setBody(convertStatement(statement.getStatement()))
        .build();
  }

  private Statement convertExpressionStatement(JCExpressionStatement statement) {
    return convertExpression(statement.getExpression()).makeStatement(getSourcePosition(statement));
  }

  private ForStatement convertForLoop(JCForLoop statement) {
    return ForStatement.newBuilder()
        // The order here is important since initializers can define new variables
        // These can be used in the expression, updaters or the body
        // This is why we need to process initializers first
        .setInitializers(convertInitializers(statement.getInitializer()))
        .setConditionExpression(
            statement.getCondition() == null
                ? BooleanLiteral.get(true)
                : convertExpression(statement.getCondition()))
        .setBody(convertStatement(statement.getStatement()))
        .setUpdates(
            convertExpressions(
                statement.getUpdate().stream()
                    .map(JCExpressionStatement::getExpression)
                    .collect(toImmutableList())))
        .setSourcePosition(getSourcePosition(statement))
        .build();
  }

  private List<Expression> convertInitializers(List<JCStatement> statements) {
    if (statements.stream().anyMatch(s -> s.getKind() == Kind.VARIABLE)) {
      // The statements are all variable declaration statements, collect them into one
      // variable declaration expression.
      return convertVariableDeclarations(statements);
    }

    return statements.stream().map(this::convertInitializer).collect(toImmutableList());
  }

  private ImmutableList<Expression> convertVariableDeclarations(List<JCStatement> statements) {
    return ImmutableList.of(
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclarationFragments(
                statements.stream()
                    .map(s -> createVariableDeclarationFragment((JCVariableDecl) s))
                    .collect(toImmutableList()))
            .build());
  }

  private Expression convertInitializer(JCStatement statement) {
    return switch (statement.getKind()) {
      case EXPRESSION_STATEMENT -> convertExpression(((JCExpressionStatement) statement).expr);
      default -> throw new AssertionError();
    };
  }

  private ForEachStatement convertEnhancedForLoop(JCEnhancedForLoop statement) {
    return ForEachStatement.newBuilder()
        .setLoopVariable(createVariable(statement.getVariable(), false))
        .setIterableExpression(convertExpression(statement.getExpression()))
        .setBody(convertStatement(statement.getStatement()))
        .setSourcePosition(getSourcePosition(statement))
        .build();
  }

  private IfStatement convertIf(JCIf statement) {
    return IfStatement.newBuilder()
        .setSourcePosition(getSourcePosition(statement))
        .setConditionExpression(convertExpression(statement.getCondition()))
        .setThenStatement(convertStatement(statement.getThenStatement()))
        .setElseStatement(convertStatementOrNull(statement.getElseStatement()))
        .build();
  }

  private WhileStatement convertWhileLoop(JCWhileLoop statement) {
    return WhileStatement.newBuilder()
        .setSourcePosition(getSourcePosition(statement))
        .setConditionExpression(convertExpression(statement.getCondition()))
        .setBody(convertStatement(statement.getStatement()))
        .build();
  }

  private SwitchStatement convertSwitch(JCSwitch switchStatement) {
    return SwitchStatement.newBuilder()
        .setSourcePosition(getSourcePosition(switchStatement))
        .setExpression(convertExpressionOrNull(switchStatement.getExpression()))
        .setCases(
            switchStatement.getCases().stream()
                .map(c -> convertSwitchCase(c, PrimitiveTypes.VOID))
                .collect(toImmutableList()))
        .setAllowsNulls(allowsNulls(switchStatement.getCases()))
        .build();
  }

  private Expression convertSwitchExpression(JCSwitchExpression expression) {
    TypeDescriptor typeDescriptor = environment.createTypeDescriptor(expression.type);
    return SwitchExpression.newBuilder()
        .setSourcePosition(getSourcePosition(expression))
        .setExpression(convertExpressionOrNull(expression.getExpression()))
        .setTypeDescriptor(typeDescriptor)
        .setCases(
            expression.getCases().stream()
                .map(c -> convertSwitchCase(c, typeDescriptor))
                .collect(toImmutableList()))
        .setAllowsNulls(allowsNulls(expression.getCases()))
        .build();
  }

  private static boolean allowsNulls(List<JCCase> cases) {
    return cases.stream()
        .flatMap(c -> c.getExpressions().stream().map(JCExpression::getKind))
        .anyMatch(Kind.NULL_LITERAL::equals);
  }

  private SwitchCase convertSwitchCase(JCCase caseClause, TypeDescriptor resultType) {
    boolean canFallthrough = caseClause.getCaseKind() == CaseKind.STATEMENT;

    boolean isDefault =
        caseClause.getLabels().stream().anyMatch(DefaultCaseLabelTree.class::isInstance);
    if (isDefault) {
      return SwitchCaseDefault.newBuilder()
          .setStatements(getCaseStatements(caseClause, resultType))
          .setCanFallthrough(canFallthrough)
          .setSourcePosition(getSourcePosition(caseClause))
          .build();
    }

    // If a case has expressions, all the labels are of JCConstantCaseLabel type.
    if (caseClause.getLabels().stream().allMatch(JCConstantCaseLabel.class::isInstance)) {
      return SwitchCaseExpressions.newBuilder()
          .setCaseExpressions(convertCaseExpressions(caseClause))
          .setStatements(getCaseStatements(caseClause, resultType))
          .setCanFallthrough(canFallthrough)
          .setSourcePosition(getSourcePosition(caseClause))
          .build();
    }

    // If a case has patterns, it has exactly one label of JCPatternCaseLabel type.
    JCPattern pattern =
        ((JCPatternCaseLabel) Iterables.getOnlyElement(caseClause.getLabels())).getPattern();

    return SwitchCasePattern.newBuilder()
        .setPattern(convertPattern(pattern))
        .setGuard(convertExpressionOrNull(caseClause.getGuard()))
        .setStatements(getCaseStatements(caseClause, resultType))
        .setCanFallthrough(canFallthrough)
        .setSourcePosition(getSourcePosition(caseClause))
        .build();
  }

  private List<Statement> getCaseStatements(JCCase caseClause, TypeDescriptor resultType) {
    if (caseClause.getCaseKind() == com.sun.source.tree.CaseTree.CaseKind.STATEMENT) {
      return convertStatements(caseClause.getStatements());
    }
    return convertSwitchCaseRuleBody(caseClause.getBody(), resultType);
  }

  /** Converts the body of a case rule into the equivalent statement format. */
  private List<Statement> convertSwitchCaseRuleBody(
      JCTree body, TypeDescriptor resultTypeDescriptor) {
    if (body instanceof JCExpression) {
      // The body is just an expression, convert it to a yield statement.
      checkState(!isPrimitiveVoid(resultTypeDescriptor));
      return ImmutableList.of(
          YieldStatement.newBuilder()
              .setExpression(convertExpression((JCExpression) body))
              .setSourcePosition(getSourcePosition(body))
              .build());
    }


    checkState(body == null || body instanceof JCStatement);
    var caseBody = body == null ? null : convertStatement((JCStatement) body);

    // If the result of the switch construct is void, it means that it is a switch statement;
    // otherwise the result should have been the type of the resulting expression and no case
    // falls through.
    var isSwitchStatement = isPrimitiveVoid(resultTypeDescriptor);
    if (isSwitchStatement && !(caseBody != null && caseBody.terminatesAbruptly())) {
      // Add a break statement to prevent fallthrough since this is a case rule and the body
      // can complete normally.
      return ImmutableList.of(
          caseBody, BreakStatement.newBuilder().setSourcePosition(SourcePosition.NONE).build());
    }

    return ImmutableList.of(caseBody);
  }

  private List<Expression> convertCaseExpressions(JCCase caseClause) {
    return convertExpressions(caseClause.getExpressions());
  }

  private ThrowStatement convertThrow(JCThrow statement) {
    return ThrowStatement.newBuilder()
        .setSourcePosition(getSourcePosition(statement))
        .setExpression(convertExpression(statement.getExpression()))
        .build();
  }

  private TryStatement convertTry(JCTry statement) {
    List<JCCatch> catchClauses = statement.getCatches();

    return TryStatement.newBuilder()
        .setSourcePosition(getSourcePosition(statement))
        .setResourceDeclarations(
            statement.getResources().stream().map(this::toResource).collect(toImmutableList()))
        .setBody(convertBlock(statement.getBlock()))
        .setCatchClauses(
            catchClauses.stream().map(this::convertCatchClause).collect(toImmutableList()))
        .setFinallyBlock((Block) convertStatementOrNull(statement.getFinallyBlock()))
        .build();
  }

  private VariableDeclarationExpression toResource(JCTree resourceTree) {
    if (resourceTree.getTag() == Tag.VARDEF) {
      return createVariableDeclarationExpression((JCVariableDecl) resourceTree);
    }
    checkArgument(resourceTree.getTag() == Tag.IDENT);
    return toResource((JCIdent) resourceTree);
  }

  private VariableDeclarationExpression toResource(JCIdent ident) {
    // Create temporary variables for resources declared outside of the try statement.
    Expression expression = convertIdent(ident);
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

  private CatchClause convertCatchClause(JCCatch catchClause) {
    // Order is important here, exception declaration must be converted before body.
    return CatchClause.newBuilder()
        .setExceptionVariable(createVariable(catchClause.getParameter(), false))
        .setBody(convertBlock(catchClause.getBlock()))
        .build();
  }

  private ReturnStatement convertReturn(JCReturn statement) {
    return ReturnStatement.newBuilder()
        .setExpression(convertExpressionOrNull(statement.getExpression()))
        .setSourcePosition(getSourcePosition(statement))
        .build();
  }

  private Statement convertYield(JCYield statement) {
    return YieldStatement.newBuilder()
        .setExpression(convertExpressionOrNull(statement.value))
        .setSourcePosition(getSourcePosition(statement))
        .build();
  }

  private SynchronizedStatement convertSynchronized(JCSynchronized statement) {
    return SynchronizedStatement.newBuilder()
        .setSourcePosition(getSourcePosition(statement))
        .setExpression(convertExpression(statement.getExpression()))
        .setBody(convertBlock(statement.getBlock()))
        .build();
  }

  private Statement convertVariableDeclaration(JCVariableDecl variableDeclaration) {
    return createVariableDeclarationExpression(variableDeclaration)
        .makeStatement(getSourcePosition(variableDeclaration));
  }

  private VariableDeclarationExpression createVariableDeclarationExpression(
      JCVariableDecl variableDeclaration) {
    return VariableDeclarationExpression.newBuilder()
        .addVariableDeclarationFragments(createVariableDeclarationFragment(variableDeclaration))
        .build();
  }

  private VariableDeclarationFragment createVariableDeclarationFragment(
      JCVariableDecl variableDeclaration) {
    Variable variable = createVariable(variableDeclaration, false);
    return VariableDeclarationFragment.newBuilder()
        .setVariable(variable)
        .setInitializer(convertExpressionOrNull(variableDeclaration.getInitializer()))
        .build();
  }

  @Nullable
  private Statement convertStatement(JCStatement jcStatement) {
    return switch (jcStatement.getKind()) {
      case ASSERT -> convertAssert((JCAssert) jcStatement);
      case BLOCK -> convertBlock((JCBlock) jcStatement);
      case BREAK -> convertBreak((JCBreak) jcStatement);
      case CLASS, RECORD ->
          new LocalClassDeclarationStatement(
              convertClassDeclaration((JCClassDecl) jcStatement), getSourcePosition(jcStatement));
      case CONTINUE -> convertContinue((JCContinue) jcStatement);
      case DO_WHILE_LOOP -> convertDoWhileLoop((JCDoWhileLoop) jcStatement);
      case EMPTY_STATEMENT -> Statement.createNoopStatement();
      case ENHANCED_FOR_LOOP -> convertEnhancedForLoop((JCEnhancedForLoop) jcStatement);
      case EXPRESSION_STATEMENT -> convertExpressionStatement((JCExpressionStatement) jcStatement);
      case FOR_LOOP -> convertForLoop((JCForLoop) jcStatement);
      case IF -> convertIf((JCIf) jcStatement);
      case LABELED_STATEMENT -> convertLabeledStatement((JCLabeledStatement) jcStatement);
      case RETURN -> convertReturn((JCReturn) jcStatement);
      case YIELD -> convertYield((JCYield) jcStatement);
      case SWITCH -> convertSwitch((JCSwitch) jcStatement);
      case THROW -> convertThrow((JCThrow) jcStatement);
      case TRY -> convertTry((JCTry) jcStatement);
      case VARIABLE -> convertVariableDeclaration((JCVariableDecl) jcStatement);
      case WHILE_LOOP -> convertWhileLoop((JCWhileLoop) jcStatement);
      case SYNCHRONIZED -> convertSynchronized((JCSynchronized) jcStatement);
      default -> throw new AssertionError("Unknown statement node type: " + jcStatement.getKind());
    };
  }

  @Nullable
  private Statement convertStatementOrNull(JCStatement statement) {
    return statement != null ? convertStatement(statement) : null;
  }

  private ImmutableList<Statement> convertStatements(List<JCStatement> statements) {
    return statements.stream()
        .map(this::convertStatement)
        .filter(Predicates.notNull())
        .collect(toImmutableList());
  }

  private SourcePosition getSourcePosition(JCTree node) {
    return getSourcePosition(null, node);
  }

  private SourcePosition getSourcePosition(String name, JCTree node) {
    int startCharacterPosition = node.getStartPosition();
    int endCharacterPosition = guessEndPosition(node);

    return getSourcePosition(name, startCharacterPosition, endCharacterPosition);
  }

  private SourcePosition getSourcePosition(
      String name, int startCharacterPosition, int endCharacterPosition) {
    int startLine = javacUnit.getLineMap().getLineNumber(startCharacterPosition) - 1;
    int startColumn = javacUnit.getLineMap().getColumnNumber(startCharacterPosition) - 1;
    int endLine = javacUnit.getLineMap().getLineNumber(endCharacterPosition) - 1;
    int endColumn = javacUnit.getLineMap().getColumnNumber(endCharacterPosition) - 1;
    return SourcePosition.newBuilder()
        .setFilePath(getCurrentCompilationUnit().getFilePath())
        .setPackageRelativePath(getCurrentCompilationUnit().getPackageRelativePath())
        .setName(name)
        .setStartFilePosition(
            FilePosition.newBuilder()
                .setLine(startLine)
                .setColumn(startColumn)
                .setByteOffset(startCharacterPosition)
                .build())
        .setEndFilePosition(
            FilePosition.newBuilder()
                .setLine(endLine)
                // TODO(b/92372836): Document which character this should point to
                .setColumn(endColumn)
                .setByteOffset(endCharacterPosition)
                .build())
        .build();
  }

  /** Javac does not always report the end position of a construct. */
  private int guessEndPosition(JCTree node) {
    int startCharacterPosition = node.getStartPosition();
    int endCharacterPosition = getEndPosition(node, javacUnit);
    if (endCharacterPosition == -1 || endCharacterPosition == startCharacterPosition) {
      try {
        // Scan the source file for the end of an identifier.
        String src = javacUnit.sourcefile.getCharContent(true).toString();
        endCharacterPosition = startCharacterPosition;
        while (endCharacterPosition < src.length()
            && Character.isJavaIdentifierPart(src.charAt(endCharacterPosition++))) {}
      } catch (IOException e) {
        throw internalCompilerError(e, "Error getting endPosition for: %s.", node);
      }
    }
    return endCharacterPosition;
  }

  private static int getEndPosition(JCTree tree, JCCompilationUnit unit) {
    // TODO: remove method handles once the minimum supported JDK is JDK 27+
    try {
      return (int) GET_END_POS_HANDLE.invokeExact(tree, unit);
    } catch (Throwable e) {
      Throwables.throwIfUnchecked(e);
      throw new AssertionError(e);
    }
  }

  private static final MethodHandle GET_END_POS_HANDLE = getEndPosMethodHandle();

  private static MethodHandle getEndPosMethodHandle() {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      // JDK versions after https://bugs.openjdk.org/browse/JDK-8372948
      return MethodHandles.dropArguments(
          lookup.findVirtual(
              JCTree.class, "getEndPosition", java.lang.invoke.MethodType.methodType(int.class)),
          1,
          JCCompilationUnit.class);
    } catch (ReflectiveOperationException e1) {
      // JDK versions before https://bugs.openjdk.org/browse/JDK-8372948
      try {
        Class<?> endPosTableClass = Class.forName("com.sun.tools.javac.tree.EndPosTable");
        return MethodHandles.filterArguments(
            lookup.findVirtual(
                JCTree.class,
                "getEndPosition",
                java.lang.invoke.MethodType.methodType(int.class, endPosTableClass)),
            1,
            lookup
                .findVarHandle(JCCompilationUnit.class, "endPositions", endPosTableClass)
                .toMethodHandle(VarHandle.AccessMode.GET));
      } catch (ReflectiveOperationException e2) {
        e2.addSuppressed(e1);
        throw new LinkageError(e2.getMessage(), e2);
      }
    }
  }

  private SourcePosition getNamePosition(JCTree node) {
    return getNamePosition(null, node);
  }

  // Return best guess for the position of a declaration node's name.
  @Nullable
  private SourcePosition getNamePosition(String name, JCTree node) {
    int start = node.getPreferredPosition();
    if (start == -1) {
      return null;
    }

    try {
      String src = javacUnit.sourcefile.getCharContent(true).toString();
      Kind kind = node.getKind();
      if (kind == Kind.ANNOTATION_TYPE
          || kind == Kind.CLASS
          || kind == Kind.ENUM
          || kind == Kind.INTERFACE
          || kind == Kind.RECORD) {
        // Skip the class/enum/interface token.
        while (src.charAt(start++) != ' ') {}
      } else if (kind != Kind.METHOD && kind != Kind.VARIABLE) {
        return getSourcePosition(node);
      }
      if (!Character.isJavaIdentifierStart(src.charAt(start))) {
        return getSourcePosition(node);
      }
      int endPos = start + 1;
      while (Character.isJavaIdentifierPart(src.charAt(endPos))) {
        endPos++;
      }
      return getSourcePosition(name, start, endPos);
    } catch (IOException e) {
      throw internalCompilerError(e, "Error getting name Position for: %s.", node);
    }
  }

  // Expressions
  private ArrayAccess convertArrayAccess(JCArrayAccess expression) {
    return ArrayAccess.newBuilder()
        .setArrayExpression(convertExpression(expression.getExpression()))
        .setIndexExpression(convertExpression(expression.getIndex()))
        .build();
  }

  private BinaryExpression convertAssignment(JCAssign expression) {
    return BinaryExpression.newBuilder()
        .setLeftOperand(convertExpression(expression.getVariable()))
        .setOperator(JavaEnvironment.getBinaryOperator(expression.getKind()))
        .setRightOperand(convertExpression(expression.getExpression()))
        .build();
  }

  /** Convert compound assigment. */
  private BinaryExpression convertAssignment(JCAssignOp expression) {
    return BinaryExpression.newBuilder()
        .setLeftOperand(convertExpression(expression.getVariable()))
        .setOperator(JavaEnvironment.getBinaryOperator(expression.getKind()))
        .setRightOperand(convertExpression(expression.getExpression()))
        .build();
  }

  private BinaryExpression convertBinary(JCBinary expression) {
    return BinaryExpression.newBuilder()
        .setLeftOperand(convertExpression(expression.getLeftOperand()))
        .setOperator(JavaEnvironment.getBinaryOperator(expression.getKind()))
        .setRightOperand(convertExpression(expression.getRightOperand()))
        .build();
  }

  private UnaryExpression convertPostfixUnary(JCUnary expression) {
    return PostfixExpression.newBuilder()
        .setOperand(convertExpression(expression.getExpression()))
        .setOperator(JavaEnvironment.getPostfixOperator(expression.getKind()))
        .build();
  }

  private UnaryExpression convertPrefixUnary(JCUnary expression) {
    return PrefixExpression.newBuilder()
        .setOperand(convertExpression(expression.getExpression()))
        .setOperator(JavaEnvironment.getPrefixOperator(expression.getKind()))
        .build();
  }

  private CastExpression convertCast(JCTypeCast expression) {
    TypeDescriptor castTypeDescriptor =
        environment.createTypeDescriptor(expression.getType().type, inNullMarkedScope());

    Expression castExpression = convertExpression(expression.getExpression());

    if (!castExpression.canBeNull()) {
      castTypeDescriptor = castTypeDescriptor.toNonNullable();
    } else if (castExpression.getTypeDescriptor().isNullable()) {
      castTypeDescriptor = castTypeDescriptor.toNullable();
    }

    return CastExpression.newBuilder()
        .setExpression(castExpression)
        .setCastTypeDescriptor(castTypeDescriptor)
        .build();
  }

  private ConditionalExpression convertConditional(JCConditional conditionalExpression) {
    TypeDescriptor conditionalTypeDescriptor =
        environment.createTypeDescriptor(conditionalExpression.type, inNullMarkedScope());

    Expression condition = convertExpression(conditionalExpression.getCondition());
    Expression trueExpression = convertExpression(conditionalExpression.getTrueExpression());
    Expression falseExpression = convertExpression(conditionalExpression.getFalseExpression());
    return ConditionalExpression.newBuilder()
        .setTypeDescriptor(
            trueExpression.getTypeDescriptor().canBeNull()
                    || falseExpression.getTypeDescriptor().canBeNull()
                ? conditionalTypeDescriptor.toNullable()
                : conditionalTypeDescriptor)
        .setConditionExpression(condition)
        .setTrueExpression(trueExpression)
        .setFalseExpression(falseExpression)
        .build();
  }

  private Expression convertInstanceOf(JCInstanceOf instanceofExpression) {
    SourcePosition sourcePosition = getSourcePosition(instanceofExpression);
    Expression expression = convertExpression(instanceofExpression.getExpression());
    return instanceofExpression.getPattern() == null
        ? InstanceOfExpression.newBuilder()
            .setSourcePosition(sourcePosition)
            .setExpression(expression)
            .setTestTypeDescriptor(
                environment.createTypeDescriptor(instanceofExpression.getType().type))
            .build()
        : PatternMatchExpression.newBuilder()
            .setSourcePosition(sourcePosition)
            .setExpression(expression)
            .setPattern(convertPattern(instanceofExpression.getPattern()))
            .build();
  }

  private Pattern convertPattern(JCPattern pattern) {
    return switch (pattern) {
      case JCBindingPattern bindingPattern ->
          new BindingPattern(createVariable(bindingPattern.var, false));
      case JCRecordPattern recordPattern ->
          new RecordPattern(
              environment.createDeclaredTypeDescriptor(getRecordType(recordPattern)),
              getAccessorsMethodDescriptor(recordPattern),
              recordPattern.getNestedPatterns().stream()
                  .map(this::convertPattern)
                  .collect(toCollection(ArrayList::new)));
      case JCAnyPattern anyPattern -> new AnyPattern();
      case null -> null;
      default -> throw new IllegalArgumentException("Unexpected pattern: " + pattern);
    };
  }

  // TODO(b/465778762): Consider whether the accessors, which are members of the type descriptor,
  // can be be provided in order by an api on DeclaredTypeDescriptor.
  private List<MethodDescriptor> getAccessorsMethodDescriptor(JCRecordPattern recordPattern) {
    var recordType = getRecordType(recordPattern);
    var parameterizedTypeDescriptor = environment.createDeclaredTypeDescriptor(recordType);
    var parameterization = parameterizedTypeDescriptor.getParameterization();
    return ((ClassSymbol) recordType.tsym)
        .getRecordComponents().stream()
            .map(RecordComponent::getAccessor)
            .map(environment::createMethodDescriptor)
            .map(m -> m.specializeTypeVariables(parameterization))
            .collect(toCollection(ArrayList::new));
  }

  private static com.sun.tools.javac.code.Type getRecordType(JCRecordPattern recordPattern) {
    return ((JCExpression) recordPattern.getDeconstructor()).type;
  }

  private Expression convertLambda(JCLambda expression) {
    TypeDescriptor expressionTypeDescriptor =
        environment.createTypeDescriptor(expression.type, inNullMarkedScope());
    MethodDescriptor functionalMethodDescriptor =
        expressionTypeDescriptor.getFunctionalInterface().getSingleAbstractMethodDescriptor();

    return FunctionExpression.newBuilder()
        .setTypeDescriptor(expressionTypeDescriptor)
        .setJsAsync(functionalMethodDescriptor.isJsAsync())
        .setParameters(
            expression.getParameters().stream()
                .map(variable -> createVariable((JCVariableDecl) variable, true))
                .collect(toImmutableList()))
        .setStatements(
            convertLambdaBody(
                expression.getBody(), functionalMethodDescriptor.getReturnTypeDescriptor()))
        .setSourcePosition(getSourcePosition(expression))
        .build();
  }

  /** Converts a body of a lambda expression. */
  private List<Statement> convertLambdaBody(JCTree body, TypeDescriptor returnTypeDescriptor) {
    SourcePosition sourcePosition = getSourcePosition(body);
    if (body instanceof JCExpression expression) {
      // The lambda body is just an expression; convert it to statements. If the lambda function
      // is not void, then it needs to make the implicit return explicit.
      Expression lambdaExpression = convertExpression(expression);
      Statement statement =
          isPrimitiveVoid(returnTypeDescriptor)
              ? lambdaExpression.makeStatement(sourcePosition)
              : ReturnStatement.newBuilder()
                  .setExpression(lambdaExpression)
                  .setSourcePosition(sourcePosition)
                  .build();
      return ImmutableList.of(statement);
    }

    if (body instanceof JCBlock block) {
      return convertBlock(block).getStatements();
    }

    if (body instanceof JCStatement statement) {
      return ImmutableList.of(convertStatement(statement));
    }
    throw new AssertionError("Unexpected node type in lambda body: " + body.getKind());
  }

  /**
   * Converts method reference expressions of the form:
   *
   * <p>
   *
   * <pre> {@code A::m}    into    {@code  (par1, ..., parN) -> A.m(par1, ..., parN) } </pre>
   */
  private Expression convertMemberReference(JCMemberReference memberReference) {
    var methodSymbol = (MethodSymbol) memberReference.sym;

    var expressionTypeDescriptor =
        environment.createTypeDescriptor(memberReference.type, inNullMarkedScope());
    var functionalMethodDescriptor =
        expressionTypeDescriptor.getFunctionalInterface().getSingleAbstractMethodDescriptor();

    if (memberReference.kind == ReferenceKind.ARRAY_CTOR) {
      return ArrayCreationReference.newBuilder()
          .setTargetTypeDescriptor(
              environment
                  .createTypeDescriptor(
                      memberReference.getQualifierExpression().type,
                      inNullMarkedScope(),
                      ArrayTypeDescriptor.class)
                  .toNonNullable())
          .setInterfaceMethodDescriptor(functionalMethodDescriptor)
          .setSourcePosition(getSourcePosition(memberReference))
          .build();
    }

    var qualifier = convertExpressionOrNull(memberReference.getQualifierExpression());
    var methodType = memberReference.referentType.asMethodType();

    // Retrieve the potentially parameterized type for the qualifier that will be used to infer
    // type arguments for the enclosing type. We can not use qualifier.getTypeDescriptor() because
    // for the case in which is just the parameterized outer class name, it would be a
    // JsConstructorReference which looses the parameterization.
    var qualifierTypeDescriptor =
        memberReference.getQualifierExpression() == null
                || memberReference.getQualifierExpression().type.isRaw()
            ? null
            : environment.createTypeDescriptor(
                memberReference.getQualifierExpression().type, inNullMarkedScope());

    var enclosingTypeDescriptor =
        getEnclosingTypeDescriptor(methodSymbol, methodType, qualifierTypeDescriptor);

    var typeArguments =
        convertTypeArguments(
            memberReference.getTypeArguments(),
            methodSymbol,
            methodType,
            enclosingTypeDescriptor.getTypeDeclaration().isNullMarked());

    var targetMethodDescriptor =
        environment.createMethodDescriptor(
            enclosingTypeDescriptor, methodType.asMethodType(), methodSymbol, typeArguments);

    return MethodReference.newBuilder()
        .setTypeDescriptor(expressionTypeDescriptor)
        .setReferencedMethodDescriptor(targetMethodDescriptor)
        .setInterfaceMethodDescriptor(functionalMethodDescriptor)
        .setQualifier(qualifier)
        .setSourcePosition(getSourcePosition(memberReference))
        .build();
  }

  private NewArray convertNewArray(JCNewArray expression) {
    ArrayTypeDescriptor typeDescriptor =
        environment.createTypeDescriptor(
            expression.type, inNullMarkedScope(), ArrayTypeDescriptor.class);

    List<Expression> dimensionExpressions = convertExpressions(expression.getDimensions());
    // Pad the dimension expressions with null values to denote omitted dimensions.
    AstUtils.addNullPadding(dimensionExpressions, typeDescriptor.getDimensions());

    ArrayLiteral arrayLiteral =
        expression.getInitializers() == null
            ? null
            : ArrayLiteral.newBuilder()
                .setTypeDescriptor(typeDescriptor)
                .setValueExpressions(convertExpressions(expression.getInitializers()))
                .build();

    return NewArray.newBuilder()
        .setTypeDescriptor(typeDescriptor)
        .setDimensionExpressions(dimensionExpressions)
        .setInitializer(arrayLiteral)
        .build();
  }

  private static StringLiteral convertStringLiteral(JCLiteral literal) {
    return new StringLiteral((String) literal.getValue());
  }

  private static BooleanLiteral convertBooleanLiteral(JCLiteral literal) {
    return BooleanLiteral.get((Boolean) literal.getValue());
  }

  private static NumberLiteral convertCharLiteral(JCLiteral literal) {
    return NumberLiteral.fromChar((Character) literal.getValue());
  }

  private NumberLiteral convertNumberLiteral(JCLiteral literal) {
    return new NumberLiteral(
        (PrimitiveTypeDescriptor) environment.createTypeDescriptor(literal.type),
        (Number) literal.getValue());
  }

  @Nullable
  private Expression convertFieldAccess(JCFieldAccess fieldAccess) {
    JCExpression expression = fieldAccess.getExpression();

    // Handle special cases that are modeled as field accesses with a special field names.
    switch (fieldAccess.name.toString()) {
      // Qualified `this` expressions are seen as a qualified field accesses to a field named "this"
      // with its qualifier being the type accessible through `expression.type`.
      case "this" -> {
        return new ThisReference(
            environment.createDeclaredTypeDescriptor(expression.type), /* isQualified= */ true);
      }

      // Qualified `super` expressions are seen as a qualified field accesses to a field named
      // "super" with its qualifier being the type accessible through `expression.type`.
      case "super" -> {
        DeclaredTypeDescriptor typeDescriptor =
            environment.createDeclaredTypeDescriptor(expression.type);

        if (typeDescriptor.isInterface()) {
          // Don't consider calls that select a default method in the super hierarchy as qualified.
          // This qualified super expression can only be a qualifier of a method call. The actual
          // class where the method resides will be present in the target of the method call that
          // has this expression as its qualifier (this is the same modeling we do for the Kotlin
          // frontend).
          return new SuperReference(getCurrentType().getTypeDescriptor());
        }

        // This is a qualified super expression referring to an enclosing class.
        return new SuperReference(typeDescriptor, /* isQualified= */ true);
      }

      // Type literals (Type.class) are seen as a qualified field accesses to a field named "class"
      // with its qualifier being the type accessible through `expression.type`.
      case "class" -> {
        return new TypeLiteral(
            getSourcePosition(fieldAccess), environment.createTypeDescriptor(expression.type));
      }

      // The `length` field is special, but only in array types.
      case "length" -> {
        if (expression.type.getKind() == TypeKind.ARRAY) {
          return ArrayLength.newBuilder().setArrayExpression(convertExpression(expression)).build();
        }
      }
    }

    if (fieldAccess.sym.baseSymbol() instanceof VariableElement variableElement) {
      Expression qualifier = convertExpression(expression);
      DeclaredTypeDescriptor parameterizedEnclosingType =
          getParameterizedEnclosingType(
              environment.createDeclaredTypeDescriptor(
                  JavaEnvironment.getEnclosingTypeElement(variableElement).asType()),
              qualifier);

      FieldDescriptor fieldDescriptor =
          environment.createFieldDescriptor(
              parameterizedEnclosingType, variableElement, fieldAccess.type);
      return FieldAccess.newBuilder().setQualifier(qualifier).setTarget(fieldDescriptor).build();
    }

    // Ignore qualified class names. These only appear as qualifiers for static members, class
    // literals, etc. and are handled when processing the parent node.
    checkState(fieldAccess.sym instanceof ClassSymbol);
    return null;
  }

  private Expression convertNewClass(JCNewClass expression) {
    var anonymousInnerClass =
        expression.getClassBody() != null
            ? convertClassDeclaration(expression.getClassBody(), expression)
            : null;

    var constructorSymbol = (MethodSymbol) expression.constructor.baseSymbol();
    // Obtain @NullMarked scope from the enclosing type declaration so that both the enclosing type
    // descriptor and the MethodDescriptor are created in the right context.
    var typeElement = (TypeElement) expression.type.asElement();
    var inNullMarkedScope = environment.createTypeDeclaration(typeElement).isNullMarked();
    var enclosingTypeDescriptor =
        environment.createDeclaredTypeDescriptor(expression.type, inNullMarkedScope);

    var methodType = expression.constructorType.baseType().asMethodType();
    var typeArguments =
        convertTypeArguments(
            expression.getTypeArguments(),
            constructorSymbol,
            methodType,
            enclosingTypeDescriptor.getTypeDeclaration().isNullMarked());

    var constructorMethodDescriptor =
        environment.createMethodDescriptor(
            enclosingTypeDescriptor, methodType, constructorSymbol, typeArguments);

    var qualifier = convertExpressionOrNull(expression.getEnclosingExpression());
    var arguments = convertArguments(constructorMethodDescriptor, expression.getArguments());
    var targetClassDescriptor = constructorMethodDescriptor.getEnclosingTypeDescriptor();

    if (targetClassDescriptor.getTypeDeclaration().isAnonymous() && qualifier != null) {
      // This is the qualifier to for the super invocation, pass as first parameter, since the
      // constructor for the anonymous class expects it there.
      arguments.add(0, qualifier);
      qualifier = null;
    }
    var needsQualifier =
        constructorMethodDescriptor
            .getEnclosingTypeDescriptor()
            .getTypeDeclaration()
            .isCapturingEnclosingInstance();
    checkArgument(
        qualifier == null || needsQualifier,
        "NewInstance of non nested class should have no qualifier.");

    return NewInstance.Builder.from(constructorMethodDescriptor)
        .setAnonymousInnerClass(anonymousInnerClass)
        .setQualifier(qualifier)
        .setArguments(arguments)
        .build();
  }

  private Expression convertMethodInvocation(JCMethodInvocation methodInvocation) {
    MethodSymbol methodSymbol;
    Expression qualifier = null;
    JCExpression methodSelectorExpression = methodInvocation.getMethodSelect();

    // The target and qualifier of method calls are in the method selector which is either
    // an identifier (JCIdent) for unqualified calls or member select expression (JCFieldAccess)
    // for qualified calls.
    switch (methodSelectorExpression) {
      case JCIdent ident -> {
        // This is an unqualified method call, where `ident.sym` is the method symbol.
        methodSymbol = (MethodSymbol) ident.sym.baseSymbol();
      }
      case JCFieldAccess fieldAccess -> {
        // This is a qualified method call, where `fieldAccess.sym` is the method symbol and
        // `fieldAccess.getExpression()` is the qualifier.
        methodSymbol = (MethodSymbol) fieldAccess.sym.baseSymbol();
        qualifier = convertExpression(fieldAccess.getExpression());
      }
      default ->
          throw new IllegalArgumentException(
              "Unexpected method selector " + methodSelectorExpression);
    }

    var methodType = methodInvocation.getMethodSelect().type.asMethodType();
    var enclosingTypeDescriptor =
        getEnclosingTypeDescriptor(
            methodSymbol,
            methodType,
            qualifier == null || methodSymbol.isConstructor()
                ? null
                : qualifier.getTypeDescriptor());

    if (methodSymbol.isConstructor()) {
      // For constructor calls, make the enclosing type of the method either the current enclosing
      // type or the super type as declared. Javac does not always provide enough information here
      // to determine the correct type parameterization.
      enclosingTypeDescriptor =
          getCurrentType().getTypeDescriptor().isSameBaseType(enclosingTypeDescriptor)
              // this()
              ? getCurrentType().getTypeDescriptor()
              // super()
              : getCurrentType().getSuperTypeDescriptor();
    }

    // The type arguments for the method itself. For example `String` in `C.<String>m()`.
    var typeArguments =
        convertTypeArguments(
            methodInvocation.getTypeArguments(),
            methodSymbol,
            methodType,
            enclosingTypeDescriptor.getTypeDeclaration().isNullMarked());
    var methodDescriptor =
        fixSpecialMethodDescriptors(
            qualifier,
            environment.createMethodDescriptor(
                enclosingTypeDescriptor, methodType, methodSymbol, typeArguments));

    List<Expression> arguments =
        convertArguments(methodDescriptor, methodInvocation.getArguments());

    boolean isStaticDispatch =
        qualifier instanceof SuperReference superReference
            && (superReference.isQualified()
                || methodDescriptor.getEnclosingTypeDescriptor().isInterface());

    return MethodCall.Builder.from(methodDescriptor)
        .setQualifier(qualifier)
        .setArguments(arguments)
        .setStaticDispatch(isStaticDispatch)
        .setSourcePosition(getSourcePosition(methodInvocation))
        .build();
  }

  /**
   * Fixes up method descriptors that are modeled inconsistently with respect to the {@code
   * JCMethodInvocation} node.
   */
  private MethodDescriptor fixSpecialMethodDescriptors(
      Expression qualifier, MethodDescriptor methodDescriptor) {
    if (qualifier != null
        && qualifier.getTypeDescriptor().isArray()
        && methodDescriptor.getSignature().equals("clone()")) {
      // A call to `q.clone()` on an array `q` returns the same array type as the array `q`. That
      // is reflected in the type of the JCMethodInvocation node but not in the return type
      // of the method descriptor.
      // This is a bit hacky since there is not a declared type that represents an array to be the
      // enclosing type of the `clone()` method.
      return MethodDescriptor.Builder.from(methodDescriptor)
          .setReturnTypeDescriptor(qualifier.getTypeDescriptor().toNonNullable())
          .build();
    }

    // TODO(b/491913371): Cleanup after JDT is gone.
    if (methodDescriptor.isConstructor()
        && methodDescriptor.isMemberOf(TypeDescriptors.get().javaLangEnum)) {
      // The constructor for `java.lang.Enum` has two parameters (ordinal and name) but those are
      // implicit in the method calls. Here the JCMethodInvocation lacks those parameters but the
      // method descriptor contains them.
      // In our AST we require that the parameters declared  method descriptor in an `Invocation`
      // are consistent with the arguments passed.
      return MethodDescriptor.Builder.from(methodDescriptor)
          .setParameterDescriptors(ImmutableList.of())
          .makeDeclaration()
          .build();
    }
    return methodDescriptor;
  }

  private ImmutableList<TypeDescriptor> convertTypeArguments(
      List<? extends JCExpression> typeArguments,
      MethodSymbol methodSymbol,
      com.sun.tools.javac.code.Type methodType,
      boolean inNullMarkedScope) {
    var typeArgumentsDescriptors =
        typeArguments == null
            ? ImmutableList.<TypeDescriptor>of()
            : environment.createTypeDescriptors(
                typeArguments.stream().map(e -> e.type).collect(toImmutableList()),
                inNullMarkedScope);

    if (typeArgumentsDescriptors.isEmpty() && !methodSymbol.getTypeParameters().isEmpty()) {
      // Retrieve the inferred type arguments
      var mapping = JavaEnvironment.getTypeSubstitution(methodType, methodSymbol);
      typeArgumentsDescriptors =
          methodSymbol.getTypeParameters().stream()
              .map(t -> mapping.get(t).stream().findFirst().orElse(getDeclaredUpperBound(t)))
              .map(t -> environment.createTypeDescriptor(t, inNullMarkedScope))
              .collect(toImmutableList());
    }
    return typeArgumentsDescriptors;
  }

  private static com.sun.tools.javac.code.Type getDeclaredUpperBound(TypeVariableSymbol t) {
    var upperBound = t.type.getUpperBound();
    if (upperBound.getKind() == TypeKind.TYPEVAR) {
      return getDeclaredUpperBound((TypeVariableSymbol) upperBound.tsym);
    }
    return upperBound;
  }

  private List<Expression> convertArguments(
      MethodDescriptor methodDescriptor, List<JCExpression> argumentExpressions) {
    List<Expression> arguments =
        argumentExpressions.stream()
            .map(this::convertExpression)
            .collect(toCollection(ArrayList::new));

    return AstUtils.maybePackageVarargs(methodDescriptor, arguments);
  }

  private DeclaredTypeDescriptor getEnclosingTypeDescriptor(
      MethodSymbol methodSymbol, MethodType methodType, TypeDescriptor qualifierTypeDescriptor) {
    // Obtain @NullMarked scope from the enclosing type declaration so that both the enclosing type
    // descriptor and the MethodDescriptor are created in the right context.
    TypeElement typeElement = (TypeElement) methodSymbol.getEnclosingElement();
    boolean inNullMarkedScope = environment.createTypeDeclaration(typeElement).isNullMarked();
    DeclaredTypeDescriptor unparameterizedEnclosingTypeDescriptor =
        environment.createDeclaredTypeDescriptor(
            methodSymbol.getEnclosingElement().asType(), inNullMarkedScope);

    if (qualifierTypeDescriptor != null) {
      return getParameterizedEnclosingType(
          unparameterizedEnclosingTypeDescriptor, qualifierTypeDescriptor);
    }

    Map<TypeVariable, TypeDescriptor> enclosingTypeArguments = new HashMap<>();
    var mapping =
        methodSymbol.isConstructor()
            ? JavaEnvironment.getTypeSubstitution(
                methodType.getReturnType(), methodType.getReturnType().tsym)
            : JavaEnvironment.getTypeSubstitution(methodType, methodSymbol);

    // Use the raw upperbound for unparameterized type variables.
    for (var tv :
        unparameterizedEnclosingTypeDescriptor.getTypeDeclaration().getTypeParameterDescriptors()) {
      enclosingTypeArguments.put(tv, tv.toRawTypeDescriptor());
    }

    // ... and use the actual type arguments if they were found.
    for (var tv : mapping.keySet()) {
      enclosingTypeArguments.put(
          (TypeVariable) environment.createTypeDescriptor(tv.asType(), inNullMarkedScope),
          environment.createTypeDescriptor(
              mapping.get(tv).getFirst(),
              unparameterizedEnclosingTypeDescriptor.getTypeDeclaration().isNullMarked()));
    }
    return (DeclaredTypeDescriptor)
        unparameterizedEnclosingTypeDescriptor.specializeTypeVariables(enclosingTypeArguments);
  }

  private DeclaredTypeDescriptor getParameterizedEnclosingType(
      DeclaredTypeDescriptor enclosingTypeDescriptor, Expression qualifier) {
    return getParameterizedEnclosingType(
        enclosingTypeDescriptor, qualifier == null ? null : qualifier.getTypeDescriptor());
  }

  private DeclaredTypeDescriptor getParameterizedEnclosingType(
      DeclaredTypeDescriptor enclosingTypeDescriptor, TypeDescriptor qualifierTypeDescriptor) {

    if (qualifierTypeDescriptor == null) {
      return enclosingTypeDescriptor;
    }

    if (qualifierTypeDescriptor.isRaw()) {
      return enclosingTypeDescriptor.toRawTypeDescriptor();
    }

    // In order to get the correct parameterization, find the parameterized type from the qualifier,
    // if available. The methodSymbol's enclosing element will not have the necessary type
    // information from javac.
    return checkNotNull(
        qualifierTypeDescriptor.findSupertype(enclosingTypeDescriptor.getTypeDeclaration()));
  }

  /**
   * Converts a syntactic element which is just a plain identifier in the source code.
   *
   * <p>A `JCIdent` can represent a variety of different source code elements:
   *
   * <ul>
   *   <li>`this`.
   *   <li>`super`.
   *   <li>a variable name.
   *   <li>an unqualified field name.
   *   <li>an unqualified type name.
   * </ul>
   */
  private Expression convertIdent(JCIdent identifier) {
    switch (identifier.name.toString()) {
      case "this" -> {
        // Unqualified `this`.
        return new ThisReference(getCurrentType().getTypeDescriptor());
      }
      case "super" -> {
        // Unqualified `super`.
        return new SuperReference(getCurrentType().getTypeDescriptor());
      }
    }

    // In some cases `identifier.sym` will point to a synthetic symbol for the field and not the
    // actual declaration `Symbol`. For those cases we need to get declaration `Symbol` by calling
    // `baseSymbol()`.
    Symbol symbol = identifier.sym.baseSymbol();
    if (symbol instanceof VarSymbol varSymbol) {
      if (symbol.getKind() == ElementKind.FIELD || symbol.getKind() == ElementKind.ENUM_CONSTANT) {
        // An unqualified field access in a subclass might have a different type than its
        // declaration:
        //
        // class A<T> {
        //   T field;
        // }
        // class B extends A<String> {
        //   // Unqualifed access to `field` here is of type `String`.
        //   private String zoo() { return field; }
        // }
        //
        // For unqualified field access, the actual type of the field access is provided by
        // `identifier.type` (vs. `identifier.sym` which might not be reflect the parameterization).
        FieldDescriptor fieldDescriptor =
            environment.createFieldDescriptor(varSymbol, identifier.type);
        return FieldAccess.newBuilder().setTarget(fieldDescriptor).build();
      }

      Variable variable = variableByVariableElement.get(symbol);
      return variable.createReference();
    }

    // Ignore unqualified class names. These only appear as qualifiers for static members, class
    // literals, etc. and are handled when processing the parent node.
    checkState(identifier.sym instanceof ClassSymbol);
    return null;
  }

  private Expression convertParens(JCParens expression) {
    return convertExpression(expression.getExpression());
  }

  @Nullable
  private Expression convertExpression(JCExpression jcExpression) {
    switch (jcExpression.getKind()) {
      case ARRAY_ACCESS:
        return convertArrayAccess((JCArrayAccess) jcExpression);
      case ASSIGNMENT:
        return convertAssignment((JCAssign) jcExpression);
      case CONDITIONAL_EXPRESSION:
        return convertConditional((JCConditional) jcExpression);
      case IDENTIFIER:
        return convertIdent((JCIdent) jcExpression);
      case PARAMETERIZED_TYPE:
        return null;
      case INSTANCE_OF:
        return convertInstanceOf((JCInstanceOf) jcExpression);
      case LAMBDA_EXPRESSION:
        return convertLambda((JCLambda) jcExpression);
      case MEMBER_REFERENCE:
        return convertMemberReference((JCMemberReference) jcExpression);
      case MEMBER_SELECT:
        return convertFieldAccess((JCFieldAccess) jcExpression);
      case METHOD_INVOCATION:
        return convertMethodInvocation((JCMethodInvocation) jcExpression);
      case NEW_ARRAY:
        return convertNewArray((JCNewArray) jcExpression);
      case NEW_CLASS:
        return convertNewClass((JCNewClass) jcExpression);
      case PARENTHESIZED:
        return convertParens((JCParens) jcExpression);
      case SWITCH_EXPRESSION:
        return convertSwitchExpression((JCSwitchExpression) jcExpression);
      case TYPE_CAST:
        return convertCast((JCTypeCast) jcExpression);
      case BOOLEAN_LITERAL:
        return convertBooleanLiteral((JCLiteral) jcExpression);
      case CHAR_LITERAL:
        return convertCharLiteral((JCLiteral) jcExpression);
      case DOUBLE_LITERAL:
      case FLOAT_LITERAL:
      case INT_LITERAL:
      case LONG_LITERAL:
        // fallthrough
        return convertNumberLiteral((JCLiteral) jcExpression);
      case STRING_LITERAL:
        return convertStringLiteral((JCLiteral) jcExpression);
      case NULL_LITERAL:
        return environment.createTypeDescriptor(jcExpression.type).getNullValue();
      case AND:
      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
      case DIVIDE:
      case EQUAL_TO:
      case GREATER_THAN:
      case GREATER_THAN_EQUAL:
      case LEFT_SHIFT:
      case LESS_THAN:
      case LESS_THAN_EQUAL:
      case MINUS:
      case MULTIPLY:
      case NOT_EQUAL_TO:
      case OR:
      case PLUS:
      case REMAINDER:
      case RIGHT_SHIFT:
      case UNSIGNED_RIGHT_SHIFT:
      case XOR:
        return convertBinary((JCBinary) jcExpression);

      case BITWISE_COMPLEMENT:
      case LOGICAL_COMPLEMENT:
      case PREFIX_DECREMENT:
      case PREFIX_INCREMENT:
      case UNARY_MINUS:
      case UNARY_PLUS:
        return convertPrefixUnary((JCUnary) jcExpression);

      case POSTFIX_DECREMENT:
      case POSTFIX_INCREMENT:
        return convertPostfixUnary((JCUnary) jcExpression);

      case AND_ASSIGNMENT:
      case DIVIDE_ASSIGNMENT:
      case LEFT_SHIFT_ASSIGNMENT:
      case MINUS_ASSIGNMENT:
      case MULTIPLY_ASSIGNMENT:
      case OR_ASSIGNMENT:
      case PLUS_ASSIGNMENT:
      case REMAINDER_ASSIGNMENT:
      case RIGHT_SHIFT_ASSIGNMENT:
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
      case XOR_ASSIGNMENT:
        return convertAssignment((JCAssignOp) jcExpression);

      case OTHER:
        if (jcExpression.hasTag(Tag.NULLCHK)) {
          // This is an expression with an implicit null check.
          return RuntimeMethods.createCheckNotNullCall(
              convertExpression(((JCUnary) jcExpression).arg));
        }
      // fall through
      default:
        throw new AssertionError(
            "Unknown expression " + jcExpression + " (node type: " + jcExpression.getKind() + ")");
    }
  }

  @Nullable
  private Expression convertExpressionOrNull(JCExpression expression) {
    return expression != null ? convertExpression(expression) : null;
  }

  private List<Expression> convertExpressions(List<JCExpression> expressions) {
    return expressions.stream().map(this::convertExpression).collect(toCollection(ArrayList::new));
  }

  public CompilationUnit buildCompilationUnit(String sourcePath, CompilationUnitTree javacUnit) {
    this.javacUnit = (JCCompilationUnit) javacUnit;
    setCurrentCompilationUnit(
        CompilationUnit.createForFile(
            sourcePath,
            javacUnit.getPackageName() == null ? "" : javacUnit.getPackageName().toString()));
    for (Tree tree : javacUnit.getTypeDecls()) {
      if (tree instanceof JCClassDecl classDeclaration) {
        getCurrentCompilationUnit().addType(convertClassDeclaration(classDeclaration));
      }
    }
    return getCurrentCompilationUnit();
  }

  // TODO(b/394094907): Support for annotating methods as @NullMarked.
  private boolean inNullMarkedScope() {
    return getCurrentType().getDeclaration().isNullMarked();
  }
}
