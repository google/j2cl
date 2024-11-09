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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.toCollection;

import com.google.common.base.Predicates;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.FilePosition;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayCreationReference;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AssertStatement;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
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
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabelReference;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Literal;
import com.google.j2cl.transpiler.ast.LocalClassDeclarationStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodReference;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PostfixExpression;
import com.google.j2cl.transpiler.ast.PrefixExpression;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.SwitchCase;
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
import com.google.j2cl.transpiler.ast.UnaryExpression;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.WhileStatement;
import com.google.j2cl.transpiler.frontend.common.AbstractCompilationUnitBuilder;
import com.google.j2cl.transpiler.frontend.common.Nullability;
import com.google.j2cl.transpiler.frontend.common.PackageInfoCache;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCFunctionalExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.Tag;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;

/** Creates a J2CL Java AST from the AST provided by JavaC. */
@SuppressWarnings("ASTHelpersSuggestions")
public class CompilationUnitBuilder extends AbstractCompilationUnitBuilder {
  private final JavaEnvironment environment;
  private final Map<VariableElement, Variable> variableByVariableElement = new HashMap<>();
  // Keeps track of labels that are currently in scope. Even though labels cannot have the
  // same name if they are nested in the same method body, labels with the same name could
  // be lexically nested by being in different methods bodies, e.g. from local or anonymous
  // classes or lambdas.
  private final Map<String, Deque<Label>> labelsInScope = new HashMap<>();
  private JCCompilationUnit javacUnit;

  private CompilationUnitBuilder(JavaEnvironment environment) {
    this.environment = environment;
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
    TypeDeclaration typeDeclaration = environment.createDeclarationForType(typeElement);

    return new Type(
        typeDeclaration.isAnonymous()
            ? getSourcePosition(sourcePositionNode)
            : getNamePosition(sourcePositionNode),
        typeDeclaration);
  }

  private Type convertClassDeclaration(JCClassDecl classDecl) {
    return convertClassDeclaration(classDecl, classDecl);
  }

  private Type convertClassDeclaration(JCClassDecl classDecl, JCTree sourcePositionNode) {
    return convertType(classDecl.sym, classDecl.getMembers(), sourcePositionNode);
  }

  private void convertTypeBody(Type type, List<JCTree> bodyDeclarations) {
    for (JCTree bodyDeclaration : bodyDeclarations) {
      if (bodyDeclaration instanceof JCVariableDecl) {
        JCVariableDecl fieldDeclaration = (JCVariableDecl) bodyDeclaration;
        type.addMember(convertFieldDeclaration(fieldDeclaration));
      } else if (bodyDeclaration instanceof JCMethodDecl) {
        JCMethodDecl methodDeclaration = (JCMethodDecl) bodyDeclaration;
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
        // } else if (bodyDeclaration instanceof AnnotationTypeMemberDeclaration) {
        //   AnnotationTypeMemberDeclaration memberDeclaration =
        //       (AnnotationTypeMemberDeclaration) bodyDeclaration;
        //   type.addMethod(convert(memberDeclaration));
      } else if (bodyDeclaration instanceof JCBlock) {
        JCBlock initializer = (JCBlock) bodyDeclaration;
        Block block = convertBlock(initializer);
        if (initializer.isStatic()) {
          type.addStaticInitializerBlock(block);
        } else {
          type.addInstanceInitializerBlock(block);
        }
      } else if (bodyDeclaration instanceof JCClassDecl) {
        // Nested class
        JCClassDecl nestedTypeDeclaration = (JCClassDecl) bodyDeclaration;
        type.addType(convertClassDeclaration(nestedTypeDeclaration));
      } else {
        throw internalCompilerError(
            "Unimplemented translation for BodyDeclaration type: %s.",
            bodyDeclaration.getClass().getName());
      }
    }
  }

  private Field convertFieldDeclaration(JCVariableDecl fieldDeclaration) {
    Expression initializer;
    VariableElement variableElement = fieldDeclaration.sym;
    Object constantValue = variableElement.getConstantValue();
    if (constantValue == null) {
      initializer = convertExpressionOrNull(fieldDeclaration.getInitializer());
    } else {
      initializer = convertConstantToLiteral(variableElement);
    }
    return Field.Builder.from(
            environment.createFieldDescriptor(variableElement, fieldDeclaration.type))
        .setInitializer(initializer)
        .setSourcePosition(getSourcePosition(fieldDeclaration))
        .setNameSourcePosition(getNamePosition(fieldDeclaration))
        .build();
  }

  private Method convertMethodDeclaration(JCMethodDecl methodDeclaration) {
    // If a method has no body, initialize the body with an empty list of statements.
    List<Variable> parameters = new ArrayList<>();
    for (JCVariableDecl parameter : methodDeclaration.getParameters()) {
      parameters.add(createVariable(parameter, true));
    }

    // If a method has no body, initialize the body with an empty list of statements.
    Block body =
        methodDeclaration.getBody() == null
            ? Block.newBuilder().setSourcePosition(getSourcePosition(methodDeclaration)).build()
            : convertBlock(methodDeclaration.getBody());

    return newMethodBuilder(methodDeclaration.sym)
        .setBodySourcePosition(body.getSourcePosition())
        .setSourcePosition(getNamePosition(methodDeclaration))
        .setParameters(parameters)
        .addStatements(body.getStatements())
        .build();
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
    VariableElement variableElement = variableDeclaration.sym;
    Variable variable =
        environment.createVariable(
            getNamePosition(variableElement.getSimpleName().toString(), variableDeclaration),
            variableElement,
            isParameter);
    variableByVariableElement.put(variableElement, variable);
    return variable;
  }

  private Method.Builder newMethodBuilder(ExecutableElement methodElement) {
    MethodDescriptor methodDescriptor =
        environment.createDeclarationMethodDescriptor(methodElement);
    return Method.newBuilder().setMethodDescriptor(methodDescriptor);
  }

  private Literal convertConstantToLiteral(VariableElement variableElement) {
    return Literal.fromValue(
        variableElement.getConstantValue(),
        environment.createTypeDescriptor(variableElement.asType()));
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
        .setConditionExpression(convertConditionRemovingOuterParentheses(statement.getCondition()))
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
    switch (statement.getKind()) {
      case EXPRESSION_STATEMENT:
        return convertExpression(((JCExpressionStatement) statement).expr);
      default:
        throw new AssertionError();
    }
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
        .setConditionExpression(convertConditionRemovingOuterParentheses(statement.getCondition()))
        .setThenStatement(convertStatement(statement.getThenStatement()))
        .setElseStatement(convertStatementOrNull(statement.getElseStatement()))
        .build();
  }

  private WhileStatement convertWhileLoop(JCWhileLoop statement) {
    return WhileStatement.newBuilder()
        .setSourcePosition(getSourcePosition(statement))
        .setConditionExpression(convertConditionRemovingOuterParentheses(statement.getCondition()))
        .setBody(convertStatement(statement.getStatement()))
        .build();
  }

  private SwitchStatement convertSwitch(JCSwitch switchStatement) {

    return SwitchStatement.newBuilder()
        .setSourcePosition(getSourcePosition(switchStatement))
        .setExpression(convertExpressionOrNull(switchStatement.getExpression()))
        .setCases(
            switchStatement.getCases().stream()
                .map(
                    caseClause ->
                        SwitchCase.newBuilder()
                            .setCaseExpressions(convertExpressionToList(caseClause.getExpression()))
                            .setStatements(convertStatements(caseClause.getStatements()))
                            .build())
                .collect(toImmutableList()))
        .build();
  }

  private List<Expression> convertExpressionToList(JCExpression expression) {
    if (expression == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of(convertExpression(expression));
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
    // Grab the type of the return statement from the method declaration, not from the expression.
    return ReturnStatement.newBuilder()
        .setExpression(convertExpressionOrNull(statement.getExpression()))
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
    switch (jcStatement.getKind()) {
      case ASSERT:
        return convertAssert((JCAssert) jcStatement);
      case BLOCK:
        return convertBlock((JCBlock) jcStatement);
      case BREAK:
        return convertBreak((JCBreak) jcStatement);
      case CLASS:
        return new LocalClassDeclarationStatement(
            convertClassDeclaration((JCClassDecl) jcStatement), getSourcePosition(jcStatement));
      case CONTINUE:
        return convertContinue((JCContinue) jcStatement);
      case DO_WHILE_LOOP:
        return convertDoWhileLoop((JCDoWhileLoop) jcStatement);
      case EMPTY_STATEMENT:
        return Statement.createNoopStatement();
      case ENHANCED_FOR_LOOP:
        return convertEnhancedForLoop((JCEnhancedForLoop) jcStatement);
      case EXPRESSION_STATEMENT:
        return convertExpressionStatement((JCExpressionStatement) jcStatement);
      case FOR_LOOP:
        return convertForLoop((JCForLoop) jcStatement);
      case IF:
        return convertIf((JCIf) jcStatement);
      case LABELED_STATEMENT:
        return convertLabeledStatement((JCLabeledStatement) jcStatement);
      case RETURN:
        return convertReturn((JCReturn) jcStatement);
      case SWITCH:
        return convertSwitch((JCSwitch) jcStatement);
      case THROW:
        return convertThrow((JCThrow) jcStatement);
      case TRY:
        return convertTry((JCTry) jcStatement);
      case VARIABLE:
        return convertVariableDeclaration((JCVariableDecl) jcStatement);
      case WHILE_LOOP:
        return convertWhileLoop((JCWhileLoop) jcStatement);
      case SYNCHRONIZED:
        return convertSynchronized((JCSynchronized) jcStatement);
      default:
        throw new AssertionError("Unknown statement node type: " + jcStatement.getKind());
    }
  }

  @Nullable
  private Statement convertStatementOrNull(JCStatement statement) {
    return statement != null ? convertStatement(statement) : null;
  }

  private ImmutableList<Statement> convertStatements(List<JCStatement> statements) {
    return statements.stream().map(this::convertStatement).collect(toImmutableList());
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
        .setFilePath(javacUnit.getSourceFile().getName())
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
                .setByteOffset(endCharacterPosition + 1)
                .build())
        .build();
  }

  /** Javac does not always report the end position of a construct. */
  private int guessEndPosition(JCTree node) {
    int startCharacterPosition = node.getStartPosition();
    int endCharacterPosition = node.getEndPosition(javacUnit.endPositions);
    if (endCharacterPosition == -1) {
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
          || kind == Kind.INTERFACE) {
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
    TypeDescriptor castTypeDescriptor = environment.createTypeDescriptor(expression.getType().type);
    return CastExpression.newBuilder()
        .setExpression(convertExpression(expression.getExpression()))
        .setCastTypeDescriptor(castTypeDescriptor)
        .build();
  }

  private ConditionalExpression convertConditional(JCConditional conditionalExpression) {
    return ConditionalExpression.newBuilder()
        .setTypeDescriptor(environment.createTypeDescriptor(conditionalExpression.type))
        .setConditionExpression(convertExpression(conditionalExpression.getCondition()))
        .setTrueExpression(convertExpression(conditionalExpression.getTrueExpression()))
        .setFalseExpression(convertExpression(conditionalExpression.getFalseExpression()))
        .build();
  }

  private InstanceOfExpression convertInstanceOf(JCInstanceOf expression) {
    return InstanceOfExpression.newBuilder()
        .setSourcePosition(getSourcePosition(expression))
        .setExpression(convertExpression(expression.getExpression()))
        .setTestTypeDescriptor(environment.createTypeDescriptor(expression.getType().type))
        .build();
  }

  private Expression convertLambda(JCLambda expression) {
    MethodDescriptor functionalMethodDescriptor =
        environment.getJsFunctionMethodDescriptor(expression.type);

    return FunctionExpression.newBuilder()
        .setTypeDescriptor(getTargetType(expression))
        .setParameters(
            expression.getParameters().stream()
                .map(variable -> createVariable((JCVariableDecl) variable, true))
                .collect(toImmutableList()))
        .setStatements(
            convertLambdaBody(
                    expression.getBody(), functionalMethodDescriptor.getReturnTypeDescriptor())
                .getStatements())
        .setSourcePosition(getSourcePosition(expression))
        .build();
  }

  private TypeDescriptor getTargetType(JCFunctionalExpression expression) {
    return environment.createTypeDescriptor(expression.type);
  }

  // Lambda expression bodies can be either an Expression or a Statement
  private Block convertLambdaBody(JCTree lambdaBody, TypeDescriptor returnTypeDescriptor) {
    Block body;
    if (lambdaBody.getKind() == Kind.BLOCK) {
      body = convertBlock((JCBlock) lambdaBody);
    } else {
      checkArgument(lambdaBody instanceof JCExpression);
      Expression lambdaMethodBody = convertExpression((JCExpression) lambdaBody);
      Statement statement =
          AstUtils.createReturnOrExpressionStatement(
              getSourcePosition(lambdaBody), lambdaMethodBody, returnTypeDescriptor);
      body =
          Block.newBuilder()
              .setSourcePosition(getSourcePosition(lambdaBody))
              .setStatements(statement)
              .build();
    }
    return body;
  }

  /**
   * Converts method reference expressions of the form:
   *
   * <p>
   *
   * <pre> {@code A::m}    into    {@code  (par1, ..., parN) -> A.m(par1, ..., parN) } </pre>
   */
  private Expression convertMemberReference(JCMemberReference memberReference) {
    MethodSymbol methodSymbol = (MethodSymbol) memberReference.sym;

    DeclaredTypeDescriptor expressionTypeDescriptor =
        environment.createDeclaredTypeDescriptor(memberReference.type);
    MethodDescriptor functionalMethodDescriptor =
        environment.getJsFunctionMethodDescriptor(memberReference.type);

    if (methodSymbol.getEnclosingElement().getQualifiedName().contentEquals("Array")) {
      // Arrays member references are seen as references to members on a class Array.
      return ArrayCreationReference.newBuilder()
          .setTargetTypeDescriptor(
              environment.createTypeDescriptor(
                  memberReference.getQualifierExpression().type, ArrayTypeDescriptor.class))
          .setInterfaceMethodDescriptor(functionalMethodDescriptor)
          .setSourcePosition(getSourcePosition(memberReference))
          .build();
    }

    com.sun.tools.javac.code.Type returnType =
        methodSymbol.isConstructor()
            ? methodSymbol.getEnclosingElement().asType()
            : memberReference.referentType.getReturnType();
    MethodDescriptor targetMethodDescriptor =
        environment.createMethodDescriptor(
            (ExecutableType) memberReference.referentType, returnType, methodSymbol);
    Expression qualifier = convertExpressionOrNull(memberReference.getQualifierExpression());
    if (qualifier instanceof JavaScriptConstructorReference) {
      // The qualifier was just the class name, remove it.
      qualifier = null;
    }

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
        environment.createTypeDescriptor(expression.type, ArrayTypeDescriptor.class);

    List<Expression> dimensionExpressions = convertExpressions(expression.getDimensions());
    // Pad the dimension expressions with null values to denote omitted dimensions.
    AstUtils.addNullPadding(dimensionExpressions, typeDescriptor.getDimensions());

    ArrayLiteral arrayLiteral =
        expression.getInitializers() == null
            ? null
            : new ArrayLiteral(typeDescriptor, convertExpressions(expression.getInitializers()));

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
    if (fieldAccess.name.contentEquals("class")) {
      return new TypeLiteral(
          getSourcePosition(fieldAccess), environment.createTypeDescriptor(expression.type));
    }

    if (fieldAccess.name.contentEquals("this")) {
      return new ThisReference(
          environment
              .createDeclarationForType((ClassSymbol) ((JCIdent) expression).sym)
              .toDescriptor(),
          /* isQualified= */ true);
    }
    if (fieldAccess.name.contentEquals("super")) {
      DeclaredTypeDescriptor typeDescriptor =
          environment
              .createDeclarationForType((ClassSymbol) ((JCIdent) expression).sym)
              .toDescriptor();

      boolean isQualified = !typeDescriptor.isInterface();
      if (isQualified) {
        // This is a qualified super call, targeting an outer class method.
        return new SuperReference(typeDescriptor, isQualified);
      }

      // Call targeting a method in the super types, including superinterfaces.
      return new SuperReference(getCurrentType().getTypeDescriptor());
    }

    Expression qualifier;
    if (fieldAccess.sym instanceof VariableElement) {
      qualifier = convertExpression(expression);
      if (qualifier instanceof JavaScriptConstructorReference) {
        // Remove qualifier if it a type. A type can only be a qualifier for a static field and
        // in such cases the actual target type is part of the field descriptor.
        checkState(fieldAccess.sym.isStatic());
        qualifier = null;
      }
      if (fieldAccess.name.contentEquals("length") && qualifier.getTypeDescriptor().isArray()) {
        return ArrayLength.newBuilder().setArrayExpression(qualifier).build();
      }
      FieldDescriptor fieldDescriptor =
          environment.createFieldDescriptor((VariableElement) fieldAccess.sym, fieldAccess.type);
      return FieldAccess.newBuilder().setQualifier(qualifier).setTarget(fieldDescriptor).build();
    }
    return null;
  }

  private Expression convertNewClass(JCNewClass expression) {
    Type anonymousInnerClass =
        expression.getClassBody() != null
            ? convertClassDeclaration(expression.getClassBody(), expression)
            : null;

    MethodSymbol constructorBinding = (MethodSymbol) expression.constructor;
    DeclaredTypeDescriptor targetType = environment.createDeclaredTypeDescriptor(expression.type);
    MethodDescriptor constructorMethodDescriptor =
        environment.createMethodDescriptor(
            targetType,
            (MethodSymbol)
                constructorBinding.asMemberOf(expression.type, environment.internalTypes),
            constructorBinding);
    Expression qualifier = convertExpressionOrNull(expression.getEnclosingExpression());
    List<Expression> arguments =
        convertArguments(constructorMethodDescriptor, expression.getArguments());
    DeclaredTypeDescriptor targetClassDescriptor =
        constructorMethodDescriptor.getEnclosingTypeDescriptor();

    if (targetClassDescriptor.getTypeDeclaration().isAnonymous() && qualifier != null) {
      // This is the qualifier to for the super invocation, pass as first parameter, since the
      // constructor for the anonymous class expects it there.
      arguments.add(0, qualifier);
      qualifier = null;
    }
    boolean needsQualifier =
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
    JCExpression jcQualifier = getExplicitQualifier(methodInvocation);
    Expression qualifier = convertExpressionOrNull(jcQualifier);
    MethodSymbol methodSymbol = getMemberSymbol(methodInvocation.getMethodSelect());

    MethodDescriptor methodDescriptor =
        environment.createMethodDescriptor(
            (ExecutableType) methodInvocation.getMethodSelect().type,
            methodInvocation.type,
            methodSymbol);

    if (methodDescriptor.isConstructor()
        && methodDescriptor.isMemberOf(TypeDescriptors.get().javaLangEnum)) {
      // Fix inconsitencies in calls to JRE's Enum constructor calls. Enum constructor has 2
      // implicit parameters (name and ordinal) that are added by a normalization pass. This removes
      // the parameter definition from the descriptor so that they are consistent.
      checkArgument(
          methodDescriptor.getParameterDescriptors().size()
              == methodInvocation.getArguments().size() + 2);
      methodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor)
              .setParameterDescriptors(ImmutableList.of())
              .makeDeclaration()
              .build();
    }

    List<Expression> arguments =
        convertArguments(methodDescriptor, methodInvocation.getArguments());

    if (isSuperConstructorCall(methodInvocation)) {
      return MethodCall.Builder.from(methodDescriptor)
          .setQualifier(qualifier)
          .setArguments(arguments)
          .setSourcePosition(getSourcePosition(methodInvocation))
          .build();
    }

    boolean hasSuperQualifier = isSuperExpression(jcQualifier);
    boolean isStaticDispatch = isQualifiedSuperExpression(jcQualifier);
    if (hasSuperQualifier
        && (qualifier.getTypeDescriptor().isInterface() || methodDescriptor.isDefaultMethod())) {
      // This is a default method call through super.
      isStaticDispatch = true;
    }

    return MethodCall.Builder.from(methodDescriptor)
        .setQualifier(qualifier)
        .setArguments(arguments)
        .setStaticDispatch(isStaticDispatch)
        .setSourcePosition(getSourcePosition(methodInvocation))
        .build();
  }

  private List<Expression> convertArguments(
      MethodDescriptor methodDescriptor, List<JCExpression> argumentExpressions) {
    List<Expression> arguments =
        argumentExpressions.stream().map(this::convertExpression).collect(toImmutableList());

    return AstUtils.maybePackageVarargs(methodDescriptor, arguments);
  }

  /**
   * Returns a qualifier for a method invocation that doesn't have one, specifically,
   * instanceMethod() will return a resolved qualifier that may refer to "this" or to the enclosing
   * instances. A staticMethod() will return null.
   */
  @Nullable
  private static JCExpression getExplicitQualifier(JCMethodInvocation methodInvocation) {

    if (methodInvocation.getMethodSelect().getKind() != Kind.IDENTIFIER) {
      return getQualifier(methodInvocation.getMethodSelect());
    }

    // No qualifier specified.
    MethodSymbol memberSymbol = getMemberSymbol(methodInvocation.getMethodSelect());
    if (memberSymbol.isStatic()) {
      return null;
    }

    return getQualifier(methodInvocation.getMethodSelect());
  }

  private Expression convertIdent(JCIdent identifier) {
    if (isThisExpression(identifier)) {
      return new ThisReference(getCurrentType().getTypeDescriptor());
    }
    if (isSuperExpression(identifier)) {
      return new SuperReference(getCurrentType().getTypeDescriptor());
    }
    Symbol symbol = identifier.sym;
    if (symbol instanceof ClassSymbol) {
      return new JavaScriptConstructorReference(
          environment.createDeclarationForType((ClassSymbol) identifier.sym));
    }
    if (symbol instanceof MethodSymbol) {
      throw new AssertionError("Unexpected symbol class: " + symbol.getClass());
    }

    VarSymbol varSymbol = (VarSymbol) symbol;
    if (symbol.getKind() == ElementKind.LOCAL_VARIABLE
        || symbol.getKind() == ElementKind.RESOURCE_VARIABLE
        || symbol.getKind() == ElementKind.PARAMETER
        || symbol.getKind() == ElementKind.EXCEPTION_PARAMETER) {
      Variable variable = variableByVariableElement.get(symbol);
      return variable.createReference();
    }

    FieldDescriptor fieldDescriptor = environment.createFieldDescriptor(varSymbol, identifier.type);
    return FieldAccess.newBuilder().setTarget(fieldDescriptor).build();
  }

  private static boolean isSuperConstructorCall(JCMethodInvocation methodInvocation) {
    return isSuperExpression(methodInvocation.getMethodSelect());
  }

  private static boolean isSuperExpression(JCExpression expression) {
    if (expression instanceof JCIdent) {
      JCIdent ident = (JCIdent) expression;
      return ident.getName().contentEquals("super");
    }
    return false;
  }

  private static boolean isQualifiedSuperExpression(JCExpression expression) {
    return expression instanceof JCFieldAccess
        && ((JCFieldAccess) expression).getIdentifier().contentEquals("super");
  }

  private static boolean isThisExpression(JCExpression expression) {
    if (expression instanceof JCIdent) {
      JCIdent ident = (JCIdent) expression;
      return ident.getName().contentEquals("this");
    }
    return false;
  }

  private static MethodSymbol getMemberSymbol(JCTree.JCExpression node) {
    switch (node.getKind()) {
      case IDENTIFIER:
        return (MethodSymbol) ((JCTree.JCIdent) node).sym.baseSymbol();
      case MEMBER_SELECT:
        return (MethodSymbol) ((JCTree.JCFieldAccess) node).sym;
      default:
        throw new AssertionError("Unexpected tree kind: " + node.getKind());
    }
  }

  @Nullable
  private static JCExpression getQualifier(JCTree.JCExpression node) {
    switch (node.getKind()) {
      case IDENTIFIER:
        return null;
      case MEMBER_SELECT:
        return ((JCTree.JCFieldAccess) node).getExpression();
      default:
        throw new AssertionError("Unexpected tree kind: " + node.getKind());
    }
  }

  private Expression convertConditionRemovingOuterParentheses(JCExpression expression) {
    return convertExpression(((JCParens) expression).getExpression());
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

  private CompilationUnit build(JCCompilationUnit javacUnit) {
    this.javacUnit = javacUnit;
    if (javacUnit.getSourceFile().getName().endsWith("package-info.java")
        && javacUnit.getPackage() != null) {
      String packageName = javacUnit.getPackageName().toString();
      String packageJsNamespace = getPackageJsNamespace(javacUnit);
      boolean isNullMarked = isNullMarked(javacUnit);
      PackageInfoCache.get()
          .setPackageProperties(
              packageName,
              packageJsNamespace,
              // TODO(b/135123615): need to support ObjectiveCName extraction.
              null,
              isNullMarked);
    }
    setCurrentCompilationUnit(
        CompilationUnit.createForFile(
            javacUnit.getSourceFile().getName(),
            javacUnit.getPackageName() == null ? "" : javacUnit.getPackageName().toString()));
    for (JCTree tree : javacUnit.getTypeDecls()) {
      if (tree instanceof JCClassDecl) {
        getCurrentCompilationUnit().addType(convertClassDeclaration((JCClassDecl) tree));
      }
    }
    return getCurrentCompilationUnit();
  }

  public static ImmutableList<CompilationUnit> build(
      List<CompilationUnitTree> compilationUnits, JavaEnvironment javaEnvironment) {

    CompilationUnitBuilder compilationUnitBuilder = new CompilationUnitBuilder(javaEnvironment);

    // Ensure that all source package-info classes come before all other classes so that the
    // freshness of the PackageInfoCache can be trusted.
    sortPackageInfoFirst(compilationUnits);

    return compilationUnits.stream()
        .map(JCCompilationUnit.class::cast)
        .map(compilationUnitBuilder::build)
        .collect(toImmutableList());
  }

  @Nullable
  private static String getPackageJsNamespace(JCCompilationUnit javacUnit) {
    PackageSymbol packge = javacUnit.packge;
    if (packge == null) {
      return null;
    }

    return JsInteropAnnotationUtils.getJsNamespace(packge);
  }

  private static boolean isNullMarked(JCCompilationUnit javacUnit) {
    PackageSymbol packge = javacUnit.packge;
    if (packge == null) {
      return false;
    }

    return packge.getAnnotationMirrors().stream()
        .anyMatch(a -> Nullability.isNullMarkedAnnotation(AnnotationUtils.getAnnotationName(a)));
  }

  private static void sortPackageInfoFirst(List<CompilationUnitTree> compilationUnits) {
    // Ensure that all source package-info classes come before all other classes so that the
    // freshness of the PackageInfoCache can be trusted.
    Collections.sort(
        compilationUnits,
        (thisCompilationUnit, thatCompilationUnit) -> {
          String thisFilePath = thisCompilationUnit.getSourceFile().getName();
          String thatFilePath = thatCompilationUnit.getSourceFile().getName();
          boolean thisIsPackageInfo = thisFilePath.endsWith("package-info.java");
          boolean thatIsPackageInfo = thatFilePath.endsWith("package-info.java");
          return ComparisonChain.start()
              .compareTrueFirst(thisIsPackageInfo, thatIsPackageInfo)
              .compare(thisFilePath, thatFilePath)
              .result();
        });
  }
}
