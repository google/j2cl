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
package com.google.j2cl.frontend;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.ASTUtils;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.BreakStatement;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CatchClause;
import com.google.j2cl.ast.CharacterLiteral;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DoWhileStatement;
import com.google.j2cl.ast.EmptyStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JavaType.Kind;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.ParenthesizedExpression;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.RegularTypeDescriptor;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.TernaryExpression;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.UnionTypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.VariableDeclarationStatement;
import com.google.j2cl.ast.VariableReference;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.ast.WhileStatement;
import com.google.j2cl.errors.Errors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Creates a J2CL Java AST from the AST provided by JDT.
 */
public class CompilationUnitBuilder {
  private class ASTConverter {
    private Map<IVariableBinding, Variable> variableByJdtBinding = new HashMap<>();
    private Multimap<TypeDescriptor, Variable> capturesByTypeDescriptor =
        LinkedHashMultimap.create();
    private List<JavaType> typeStack = new ArrayList<>();
    private JavaType currentType = null;
    private ITypeBinding javaLangObjectBinding;

    private String currentSourceFile;
    private CompilationUnit j2clCompilationUnit;

    private void pushType(JavaType type) {
      Preconditions.checkArgument(type.getDescriptor() instanceof RegularTypeDescriptor);
      typeStack.add(type);
      currentType = type;
    }

    private void popType() {
      typeStack.remove(typeStack.size() - 1);
      currentType = typeStack.isEmpty() ? null : typeStack.get(typeStack.size() - 1);
    }

    private CompilationUnit convert(
        String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit) {
      javaLangObjectBinding = jdtCompilationUnit.getAST().resolveWellKnownType("java.lang.Object");

      currentSourceFile = sourceFilePath;
      String packageName = JdtUtils.getCompilationUnitPackageName(jdtCompilationUnit);
      j2clCompilationUnit = new CompilationUnit(sourceFilePath, packageName);
      for (Object object : jdtCompilationUnit.types()) {
        AbstractTypeDeclaration abstractTypeDeclaration = (AbstractTypeDeclaration) object;
        j2clCompilationUnit.addTypes(convert(abstractTypeDeclaration));
      }
      return j2clCompilationUnit;
    }

    private Collection<JavaType> convert(AbstractTypeDeclaration node) {
      switch (node.getNodeType()) {
        case ASTNode.TYPE_DECLARATION:
          return convert((TypeDeclaration) node);
        default:
          throw new RuntimeException(
              "Need to implement translation for AbstractTypeDeclaration type: "
                  + node.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
      }
    }

    private Collection<JavaType> convert(TypeDeclaration node) {
      ITypeBinding typeBinding = node.resolveBinding();
      JavaType type = createJavaType(typeBinding);
      pushType(type);
      List<JavaType> types = new ArrayList<>();
      types.add(type);
      boolean hasConstructor = false;
      TypeDescriptor currentTypeDescriptor = currentType.getDescriptor();
      ITypeBinding superclassBinding = typeBinding.getSuperclass();
      if (superclassBinding != null) {
        capturesByTypeDescriptor.putAll(
            currentTypeDescriptor,
            capturesByTypeDescriptor.get(currentType.getSuperTypeDescriptor()));
      }
      for (Object object : node.bodyDeclarations()) {
        if (object instanceof FieldDeclaration) {
          FieldDeclaration fieldDeclaration = (FieldDeclaration) object;
          type.addFields(convert(fieldDeclaration));
        } else if (object instanceof MethodDeclaration) {
          MethodDeclaration methodDeclaration = (MethodDeclaration) object;
          if (methodDeclaration.isConstructor()) {
            hasConstructor = true;
          }
          type.addMethod(convert(methodDeclaration));
        } else if (object instanceof Initializer) {
          Initializer initializer = (Initializer) object;
          if (JdtUtils.isStatic(initializer.getModifiers())) {
            type.addStaticInitializerBlock(convert(initializer.getBody()));
          } else {
            type.addInstanceInitializerBlock(convert(initializer.getBody()));
          }
        } else if (object instanceof TypeDeclaration) {
          // Nested class
          TypeDeclaration nestedTypeDeclaration = (TypeDeclaration) object;
          types.addAll(convert(nestedTypeDeclaration));
        } else {
          throw new RuntimeException(
              "Need to implement translation for BodyDeclaration type: "
                  + node.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
        }
      }

      for (Variable capturedVariable : capturesByTypeDescriptor.get(currentTypeDescriptor)) {
        FieldDescriptor fieldDescriptor =
            ASTUtils.getFieldDescriptorForCapture(currentTypeDescriptor, capturedVariable);
        currentType.addField(new Field(fieldDescriptor, null, capturedVariable)); // captured field.
      }
      if (JdtUtils.isInstanceNestedClass(typeBinding)) {
        // add field for enclosing instance.
        currentType.addField(
            new Field(
                ASTUtils.getFieldDescriptorForEnclosingInstance(
                    currentTypeDescriptor, currentType.getEnclosingTypeDescriptor()),
                null));
      }

      // If the class has no default constructor, synthesize the default constructor.
      if (!typeBinding.isInterface() && !hasConstructor) {
        currentType.addMethod(
            synthesizeDefaultConstructor(
                currentType.getDescriptor(), superclassBinding, currentType.getVisibility()));
      }
      popType();
      return types;
    }

    private Method synthesizeDefaultConstructor(
        TypeDescriptor typeDescriptor, ITypeBinding superclassBinding, Visibility visibility) {
      MethodDescriptor methodDescriptor =
          ASTUtils.createConstructorDescriptor(typeDescriptor, visibility);
      List<Statement> statements = new ArrayList<>();
      if (superclassBinding != null) {
        // add super() call.
        statements.add(synthesizeSuperCall(superclassBinding));
      }
      Block body = new Block(statements);
      List<Variable> parameters = new ArrayList<>();
      Method defaultConstructor = new Method(methodDescriptor, parameters, body);
      return defaultConstructor;
    }

    private Statement synthesizeSuperCall(ITypeBinding superTypeBinding) {
      TypeDescriptor superTypeDescriptor = createTypeDescriptor(superTypeBinding);
      MethodDescriptor superMethodDescriptor =
          ASTUtils.createConstructorDescriptor(
              superTypeDescriptor, JdtUtils.getVisibility(superTypeBinding.getModifiers()));

      // find the qualifier of the super() call.
      Expression qualifier =
          JdtUtils.isInstanceNestedClass(superTypeBinding)
              ? convertOuterClassReference(
                  createTypeDescriptor(superTypeBinding.getDeclaringClass()))
              : null;
      MethodCall superCall =
          new MethodCall(qualifier, superMethodDescriptor, new ArrayList<Expression>());
      return new ExpressionStatement(superCall);
    }

    private List<Field> convert(FieldDeclaration node) {
      List<Field> fields = new ArrayList<>();
      for (Object object : node.fragments()) {
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
            (org.eclipse.jdt.core.dom.VariableDeclarationFragment) object;
        Expression initializer =
            fragment.getInitializer() == null ? null : convert(fragment.getInitializer());
        fields.add(
            new Field(
                JdtUtils.createFieldDescriptor(
                    fragment.resolveBinding(), compilationUnitNameLocator),
                initializer));
      }
      return fields;
    }

    private Method convert(MethodDeclaration node) {
      List<Variable> parameters = new ArrayList<>();
      for (Object element : node.parameters()) {
        SingleVariableDeclaration parameter = (SingleVariableDeclaration) element;
        IVariableBinding parameterBinding = parameter.resolveBinding();
        Variable j2clParameter =
            JdtUtils.createVariable(parameterBinding, compilationUnitNameLocator);
        parameters.add(j2clParameter);
        variableByJdtBinding.put(parameterBinding, j2clParameter);
      }

      // If a method has no body, initialize the body with an empty list of statements.
      Block body =
          node.getBody() == null ? new Block(new ArrayList<Statement>()) : convert(node.getBody());
      // synthesize default super() call.
      if (node.isConstructor() && needSynthesizeSuperCall(node)) {
        body
            .getStatements()
            .add(0, synthesizeSuperCall(node.resolveBinding().getDeclaringClass().getSuperclass()));
      }

      Method method =
          new Method(
              JdtUtils.createMethodDescriptor(node.resolveBinding(), compilationUnitNameLocator),
              parameters,
              body);
      method.setOverride(JdtUtils.isOverride(node.resolveBinding()));
      return method;
    }

    private boolean needSynthesizeSuperCall(MethodDeclaration method) {
      Preconditions.checkArgument(method.isConstructor());
      if (method.getBody().statements().isEmpty()) {
        return true;
      }
      int firstStatementType = ((ASTNode) method.getBody().statements().get(0)).getNodeType();

      return firstStatementType != ASTNode.SUPER_CONSTRUCTOR_INVOCATION
          && firstStatementType != ASTNode.CONSTRUCTOR_INVOCATION;
    }

    @SuppressWarnings("cast")
    private ArrayAccess convert(org.eclipse.jdt.core.dom.ArrayAccess node) {
      return new ArrayAccess(convert(node.getArray()), convert(node.getIndex()));
    }

    @SuppressWarnings({"cast", "unchecked"})
    private NewArray convert(org.eclipse.jdt.core.dom.ArrayCreation node) {
      ArrayType arrayType = node.getType();

      List<Expression> dimensionExpressions =
          convert((List<org.eclipse.jdt.core.dom.Expression>) node.dimensions());
      // If some dimensions are not initialized then make that explicit.
      while (dimensionExpressions.size() < arrayType.getDimensions()) {
        dimensionExpressions.add(new NullLiteral());
      }

      ArrayLiteral arrayLiteral =
          node.getInitializer() == null ? null : convert(node.getInitializer());

      ArrayTypeDescriptor typeDescriptor =
          (ArrayTypeDescriptor) createTypeDescriptor(node.resolveTypeBinding());
      return new NewArray(typeDescriptor, dimensionExpressions, arrayLiteral);
    }

    @SuppressWarnings({"cast", "unchecked"})
    private ArrayLiteral convert(org.eclipse.jdt.core.dom.ArrayInitializer node) {
      return new ArrayLiteral(
          createTypeDescriptor(node.resolveTypeBinding()),
          convert((List<org.eclipse.jdt.core.dom.Expression>) node.expressions()));
    }

    private BooleanLiteral convert(org.eclipse.jdt.core.dom.BooleanLiteral node) {
      return node.booleanValue() ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
    }

    private CastExpression convert(org.eclipse.jdt.core.dom.CastExpression node) {
      Expression expression = convert(node.getExpression());
      TypeDescriptor castTypeDescriptor = createTypeDescriptor(node.getType().resolveBinding());
      return new CastExpression(expression, castTypeDescriptor);
    }

    private CharacterLiteral convert(org.eclipse.jdt.core.dom.CharacterLiteral node) {
      return new CharacterLiteral(node.charValue(), node.getEscapedValue());
    }

    private NewInstance convert(org.eclipse.jdt.core.dom.ClassInstanceCreation node) {
      Expression qualifier = node.getExpression() == null ? null : convert(node.getExpression());
      IMethodBinding constructorBinding = node.resolveConstructorBinding();
      // If the constructor is an instance nested class, add explicit qualifier.
      if (JdtUtils.isInstanceNestedClass(constructorBinding.getDeclaringClass())
          && qualifier == null) {
        qualifier =
            new ThisReference(
                (RegularTypeDescriptor)
                    createTypeDescriptor(constructorBinding.getDeclaringClass()));
      }
      MethodDescriptor constructorMethodDescriptor =
          JdtUtils.createMethodDescriptor(constructorBinding, compilationUnitNameLocator);
      List<Expression> arguments = new ArrayList<>();
      for (Object argument : node.arguments()) {
        arguments.add(convert((org.eclipse.jdt.core.dom.Expression) argument));
      }
      return new NewInstance(qualifier, constructorMethodDescriptor, arguments);
    }

    private Expression convert(org.eclipse.jdt.core.dom.Expression node) {
      switch (node.getNodeType()) {
        case ASTNode.ARRAY_ACCESS:
          return convert((org.eclipse.jdt.core.dom.ArrayAccess) node);
        case ASTNode.ARRAY_CREATION:
          return convert((org.eclipse.jdt.core.dom.ArrayCreation) node);
        case ASTNode.ARRAY_INITIALIZER:
          return convert((org.eclipse.jdt.core.dom.ArrayInitializer) node);
        case ASTNode.ASSIGNMENT:
          return convert((org.eclipse.jdt.core.dom.Assignment) node);
        case ASTNode.BOOLEAN_LITERAL:
          return convert((org.eclipse.jdt.core.dom.BooleanLiteral) node);
        case ASTNode.CAST_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.CastExpression) node);
        case ASTNode.CHARACTER_LITERAL:
          return convert((org.eclipse.jdt.core.dom.CharacterLiteral) node);
        case ASTNode.CLASS_INSTANCE_CREATION:
          return convert((org.eclipse.jdt.core.dom.ClassInstanceCreation) node);
        case ASTNode.CONDITIONAL_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.ConditionalExpression) node);
        case ASTNode.FIELD_ACCESS:
          return convert((org.eclipse.jdt.core.dom.FieldAccess) node);
        case ASTNode.INFIX_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.InfixExpression) node);
        case ASTNode.INSTANCEOF_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.InstanceofExpression) node);
        case ASTNode.METHOD_INVOCATION:
          return convert((org.eclipse.jdt.core.dom.MethodInvocation) node);
        case ASTNode.NULL_LITERAL:
          return convert((org.eclipse.jdt.core.dom.NullLiteral) node);
        case ASTNode.NUMBER_LITERAL:
          return convert((org.eclipse.jdt.core.dom.NumberLiteral) node);
        case ASTNode.PARENTHESIZED_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.ParenthesizedExpression) node);
        case ASTNode.POSTFIX_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.PostfixExpression) node);
        case ASTNode.PREFIX_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.PrefixExpression) node);
        case ASTNode.QUALIFIED_NAME:
          return convert((org.eclipse.jdt.core.dom.QualifiedName) node);
        case ASTNode.SIMPLE_NAME:
          return convert((org.eclipse.jdt.core.dom.SimpleName) node);
        case ASTNode.STRING_LITERAL:
          return convert((org.eclipse.jdt.core.dom.StringLiteral) node);
        case ASTNode.THIS_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.ThisExpression) node);
        case ASTNode.TYPE_LITERAL:
          return convert((org.eclipse.jdt.core.dom.TypeLiteral) node);
        case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.VariableDeclarationExpression) node);
        default:
          throw new RuntimeException(
              "Need to implement translation for expression type: "
                  + node.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
      }
    }

    private VariableDeclarationExpression convert(
        org.eclipse.jdt.core.dom.VariableDeclarationExpression node) {
      @SuppressWarnings("unchecked")
      List<org.eclipse.jdt.core.dom.VariableDeclarationFragment> fragments = node.fragments();

      List<VariableDeclarationFragment> variableDeclarations = new ArrayList<>();

      for (org.eclipse.jdt.core.dom.VariableDeclarationFragment variableDeclarationFragment :
          fragments) {
        variableDeclarations.add(convert(variableDeclarationFragment));
      }
      return new VariableDeclarationExpression(variableDeclarations);
    }

    private List<Expression> convert(List<org.eclipse.jdt.core.dom.Expression> nodes) {
      return new ArrayList<>(
          Lists.transform(
              nodes,
              new Function<org.eclipse.jdt.core.dom.Expression, Expression>() {
                @Override
                public Expression apply(org.eclipse.jdt.core.dom.Expression expression) {
                  return convert(expression);
                }
              }));
    }

    private TernaryExpression convert(
        org.eclipse.jdt.core.dom.ConditionalExpression jdtConditionalExpression) {
      Expression conditionExpression = convert(jdtConditionalExpression.getExpression());
      Expression trueExpression = convert(jdtConditionalExpression.getThenExpression());
      Expression falseExpression = convert(jdtConditionalExpression.getElseExpression());
      return new TernaryExpression(
          createTypeDescriptor(jdtConditionalExpression.resolveTypeBinding()),
          conditionExpression,
          trueExpression,
          falseExpression);
    }

    private Collection<Statement> convert(org.eclipse.jdt.core.dom.Statement node) {
      switch (node.getNodeType()) {
        case ASTNode.ASSERT_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.AssertStatement) node));
        case ASTNode.BLOCK:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.Block) node));
        case ASTNode.BREAK_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.BreakStatement) node));
        case ASTNode.CONSTRUCTOR_INVOCATION:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.ConstructorInvocation) node));
        case ASTNode.DO_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.DoStatement) node));
        case ASTNode.EMPTY_STATEMENT:
          return singletonStatement(new EmptyStatement());
        case ASTNode.EXPRESSION_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.ExpressionStatement) node));
        case ASTNode.FOR_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.ForStatement) node));
        case ASTNode.IF_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.IfStatement) node));
        case ASTNode.RETURN_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.ReturnStatement) node));
        case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
          return singletonStatement(
              convert((org.eclipse.jdt.core.dom.SuperConstructorInvocation) node));
        case ASTNode.THROW_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.ThrowStatement) node));
        case ASTNode.TRY_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.TryStatement) node));
        case ASTNode.TYPE_DECLARATION_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.TypeDeclarationStatement) node);
        case ASTNode.VARIABLE_DECLARATION_STATEMENT:
          return singletonStatement(
              convert((org.eclipse.jdt.core.dom.VariableDeclarationStatement) node));
        case ASTNode.WHILE_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.WhileStatement) node));
        default:
          throw new RuntimeException(
              "Need to implement translation for statement type: "
                  + node.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
      }
    }

    private BreakStatement convert(org.eclipse.jdt.core.dom.BreakStatement jdtBreakStatement) {
      Preconditions.checkState(
          jdtBreakStatement.getLabel() == null, "Break statement with label not supported yet");
      return new BreakStatement();
    }

    private ForStatement convert(org.eclipse.jdt.core.dom.ForStatement jdtForStatement) {
      // The order here is important since initializers can define new variables
      // These can be used in the expression, updaters or the block
      // This is why we need to process initializers first
      @SuppressWarnings("unchecked")
      List<org.eclipse.jdt.core.dom.VariableDeclarationExpression> jdtInitializers =
          jdtForStatement.initializers();
      List<Expression> initializers = Lists.newArrayList();
      for (org.eclipse.jdt.core.dom.Expression expression : jdtInitializers) {
        initializers.add(convert(expression));
      }

      Expression conditionExpression =
          jdtForStatement.getExpression() != null ? convert(jdtForStatement.getExpression()) : null;

      Block block = extractBlock(jdtForStatement.getBody());
      @SuppressWarnings("unchecked")
      List<Expression> updaters = convert(jdtForStatement.updaters());
      return new ForStatement(conditionExpression, block, initializers, updaters);
    }

    private DoWhileStatement convert(org.eclipse.jdt.core.dom.DoStatement jdtDoStatement) {
      Block block = extractBlock(jdtDoStatement.getBody());
      Expression conditionExpression = convert(jdtDoStatement.getExpression());
      return new DoWhileStatement(conditionExpression, block);
    }

    private Collection<Statement> convert(
        org.eclipse.jdt.core.dom.TypeDeclarationStatement jdtTypeDeclStatement) {
      j2clCompilationUnit.addTypes(convert(jdtTypeDeclStatement.getDeclaration()));
      return Collections.emptyList();
    }

    private WhileStatement convert(org.eclipse.jdt.core.dom.WhileStatement jdtWhileStatement) {
      Expression conditionExpression = convert(jdtWhileStatement.getExpression());
      Block block = extractBlock(jdtWhileStatement.getBody());
      return new WhileStatement(conditionExpression, block);
    }

    private IfStatement convert(org.eclipse.jdt.core.dom.IfStatement jdtIf) {
      Expression conditionExpression = convert(jdtIf.getExpression());

      org.eclipse.jdt.core.dom.Statement jdtTrueStatement = jdtIf.getThenStatement();
      org.eclipse.jdt.core.dom.Statement jdtFalseStatement = jdtIf.getElseStatement();
      Block trueBlock = extractBlock(jdtTrueStatement);
      Block falseBlock = extractBlock(jdtFalseStatement);

      return new IfStatement(conditionExpression, trueBlock, falseBlock);
    }

    private Block extractBlock(org.eclipse.jdt.core.dom.Statement statement) {
      if (statement == null) {
        return null;
      }
      if (statement instanceof org.eclipse.jdt.core.dom.Block) {
        return convert((org.eclipse.jdt.core.dom.Block) statement);
      }
      return new Block(Lists.newArrayList(convert(statement)));
    }

    private InstanceOfExpression convert(org.eclipse.jdt.core.dom.InstanceofExpression node) {
      Expression leftOperand = convert(node.getLeftOperand());
      TypeDescriptor testTypeDescriptor =
          createTypeDescriptor(node.getRightOperand().resolveBinding());
      return new InstanceOfExpression(leftOperand, testTypeDescriptor);
    }

    private Collection<Statement> singletonStatement(Statement statement) {
      return Collections.singletonList(statement);
    }

    private AssertStatement convert(org.eclipse.jdt.core.dom.AssertStatement node) {
      Expression message = node.getMessage() == null ? null : convert(node.getMessage());
      Expression expression = convert(node.getExpression());
      return new AssertStatement(expression, message);
    }

    private BinaryExpression convert(org.eclipse.jdt.core.dom.Assignment node) {
      Expression leftHandSide = convert(node.getLeftHandSide());
      Expression rightHandSide = convert(node.getRightHandSide());
      BinaryOperator operator = JdtUtils.getBinaryOperator(node.getOperator());
      return new BinaryExpression(
          createTypeDescriptor(node.resolveTypeBinding()), leftHandSide, operator, rightHandSide);
    }

    private Block convert(org.eclipse.jdt.core.dom.Block node) {
      List<Statement> body = new ArrayList<>();
      for (Object object : node.statements()) {
        org.eclipse.jdt.core.dom.Statement statement = (org.eclipse.jdt.core.dom.Statement) object;
        body.addAll(convert(statement));
      }
      return new Block(body);
    }

    private CatchClause convert(org.eclipse.jdt.core.dom.CatchClause node) {
      Variable exceptionVar = convert(node.getException());
      Block body = convert(node.getBody());
      return new CatchClause(body, exceptionVar);
    }

    private ExpressionStatement convert(org.eclipse.jdt.core.dom.ConstructorInvocation node) {
      IMethodBinding constructorBinding = node.resolveConstructorBinding();
      MethodDescriptor methodDescriptor =
          JdtUtils.createMethodDescriptor(constructorBinding, compilationUnitNameLocator);
      @SuppressWarnings("unchecked")
      List<Expression> arguments = convert(node.arguments());
      return new ExpressionStatement(new MethodCall(null, methodDescriptor, arguments));
    }

    private Statement convert(org.eclipse.jdt.core.dom.ExpressionStatement node) {
      return new ExpressionStatement(convert(node.getExpression()));
    }

    private FieldAccess convert(org.eclipse.jdt.core.dom.FieldAccess node) {
      Expression qualifier = convert(node.getExpression());
      IVariableBinding variableBinding = node.resolveFieldBinding();
      FieldDescriptor fieldDescriptor =
          JdtUtils.createFieldDescriptor(variableBinding, compilationUnitNameLocator);
      return new FieldAccess(qualifier, fieldDescriptor);
    }

    private BinaryExpression convert(org.eclipse.jdt.core.dom.InfixExpression node) {
      Expression leftOperand = convert(node.getLeftOperand());
      Expression rightOperand = convert(node.getRightOperand());
      BinaryOperator operator = JdtUtils.getBinaryOperator(node.getOperator());
      BinaryExpression binaryExpression =
          new BinaryExpression(
              createTypeDescriptor(node.resolveTypeBinding()), leftOperand, operator, rightOperand);
      for (Object object : node.extendedOperands()) {
        org.eclipse.jdt.core.dom.Expression extendedOperand =
            (org.eclipse.jdt.core.dom.Expression) object;
        binaryExpression =
            new BinaryExpression(
                createTypeDescriptor(node.resolveTypeBinding()),
                binaryExpression,
                operator,
                convert(extendedOperand));
      }
      return binaryExpression;
    }

    private MethodCall convert(org.eclipse.jdt.core.dom.MethodInvocation node) {
      IMethodBinding methodBinding = node.resolveMethodBinding();

      // Perform Object method devirtualization.
      if (isObjectMethodBinding(methodBinding)) {
        return createDevirtualizedObjectMethodCall(node);
      }

      Expression qualifier = node.getExpression() == null ? null : convert(node.getExpression());
      MethodDescriptor methodDescriptor =
          JdtUtils.createMethodDescriptor(methodBinding, compilationUnitNameLocator);
      @SuppressWarnings("unchecked")
      List<Expression> arguments = convert(node.arguments());
      return new MethodCall(qualifier, methodDescriptor, arguments);
    }

    private MethodCall createDevirtualizedObjectMethodCall(MethodInvocation node) {
      IMethodBinding methodBinding = node.resolveMethodBinding();
      Expression originalQualifier =
          node.getExpression() == null ? null : convert(node.getExpression());
      String methodName = methodBinding.getName();
      TypeDescriptor returnTypeDescriptor = createTypeDescriptor(methodBinding.getReturnType());
      int originalParameterCount = methodBinding.getParameterTypes().length;

      TypeDescriptor[] parameterTypeDescriptors = new TypeDescriptor[originalParameterCount + 1];
      // Turn the instance into now a first parameter to the devirtualized method.
      parameterTypeDescriptors[0] = TypeDescriptor.OBJECT_TYPE_DESCRIPTOR;
      for (int i = 0; i < originalParameterCount; i++) {
        parameterTypeDescriptors[i + 1] =
            createTypeDescriptor(methodBinding.getParameterTypes()[i]);
      }
      MethodDescriptor methodDescriptor =
          MethodDescriptor.create(
              true, // Static method.
              JdtUtils.getVisibility(methodBinding.getModifiers()),
              TypeDescriptor.OBJECTS_TYPE_DESCRIPTOR, // Enclosing class reference.
              methodName,
              false, // Not a constructor.
              returnTypeDescriptor,
              parameterTypeDescriptors);
      @SuppressWarnings("unchecked")

      List<Expression> arguments = convert(node.arguments());
      // Turn the instance into now a first parameter to the devirtualized method.
      arguments.add(0, originalQualifier);
      // Call the method like Objects.foo(instance, ...)
      return new MethodCall(null, methodDescriptor, arguments);
    }

    @SuppressWarnings("unused")
    private NullLiteral convert(org.eclipse.jdt.core.dom.NullLiteral node) {
      return new NullLiteral();
    }

    private NumberLiteral convert(org.eclipse.jdt.core.dom.NumberLiteral node) {
      return new NumberLiteral(createTypeDescriptor(node.resolveTypeBinding()), node.getToken());
    }

    private ParenthesizedExpression convert(org.eclipse.jdt.core.dom.ParenthesizedExpression node) {
      return new ParenthesizedExpression(convert(node.getExpression()));
    }

    private PostfixExpression convert(org.eclipse.jdt.core.dom.PostfixExpression node) {
      return new PostfixExpression(
          createTypeDescriptor(node.resolveTypeBinding()),
          convert(node.getOperand()),
          JdtUtils.getPostfixOperator(node.getOperator()));
    }

    private PrefixExpression convert(org.eclipse.jdt.core.dom.PrefixExpression node) {
      return new PrefixExpression(
          createTypeDescriptor(node.resolveTypeBinding()),
          convert(node.getOperand()),
          JdtUtils.getPrefixOperator(node.getOperator()));
    }

    private Expression convert(org.eclipse.jdt.core.dom.QualifiedName node) {
      IBinding binding = node.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          return new FieldAccess(
              convert(node.getQualifier()),
              JdtUtils.createFieldDescriptor(variableBinding, compilationUnitNameLocator));
        } else {
          throw new RuntimeException(
              "Need to implement translation for QualifiedName that is not a field.");
        }
      } else if (binding instanceof ITypeBinding) {
        return null;
      } else {
        throw new RuntimeException(
            "Need to implement translation for QualifiedName that is not a variable or a type.");
      }
    }

    private ReturnStatement convert(org.eclipse.jdt.core.dom.ReturnStatement node) {
      Expression expression = node.getExpression() == null ? null : convert(node.getExpression());
      return new ReturnStatement(expression);
    }

    private Expression convert(org.eclipse.jdt.core.dom.SimpleName node) {
      IBinding binding = node.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          FieldDescriptor fieldDescriptor =
              JdtUtils.createFieldDescriptor(variableBinding, compilationUnitNameLocator);
          if (!fieldDescriptor
              .getEnclosingClassTypeDescriptor()
              .equals(currentType.getDescriptor())) {
            return new FieldAccess(
                convertOuterClassReference(fieldDescriptor.getEnclosingClassTypeDescriptor()),
                fieldDescriptor);
          } else {
            return new FieldAccess(null, fieldDescriptor);
          }
        } else {
          // local variable or parameter.
          Preconditions.checkNotNull(variableBinding.getDeclaringMethod());
          Variable variable = variableByJdtBinding.get(variableBinding);
          // the innermost type that this variable are declared.
          TypeDescriptor enclosingTypeDescriptor =
              createTypeDescriptor(variableBinding.getDeclaringMethod().getDeclaringClass());
          TypeDescriptor currentTypeDescriptor = currentType.getDescriptor();
          if (!enclosingTypeDescriptor.equals(currentTypeDescriptor)) {
            return convertCapturedVariableReference(variable, enclosingTypeDescriptor);
          } else {
            return new VariableReference(variable);
          }
        }
      } else if (binding instanceof ITypeBinding) {
        return null;
      } else {
        // TODO: to be implemented
        throw new RuntimeException(
            "Need to implement translation for SimpleName binding: " + node.getClass().getName());
      }
    }

    /**
     * Convert A.this to corresponding field access.
     * <p>
     * In the case of outside a constructor,
     * if A is the direct enclosing class, A.this => this.f_$outer_this.
     * if A is the enclosing class of the direct enclosing class,
     * A.this => this.f_$outer_this$.f_$outer_this.
     * <p>
     * In the case of inside a constructor, the above two cases should be translated to
     * this$ and this$.f_this$, this$ is the added parameter of the constructor.
     */
    private Expression convertOuterClassReference(TypeDescriptor typeDescriptor) {
      Expression qualifier = new ThisReference((RegularTypeDescriptor) currentType.getDescriptor());
      int i = typeStack.size() - 1;
      for (; i > 0; i--) {
        if (typeStack.get(i).getDescriptor().equals(typeDescriptor)) {
          break;
        }
        TypeDescriptor enclosingTypeDescriptor = typeStack.get(i).getDescriptor();
        TypeDescriptor fieldTypeDescriptor = typeStack.get(i - 1).getDescriptor();
        qualifier =
            new FieldAccess(
                qualifier,
                ASTUtils.getFieldDescriptorForEnclosingInstance(
                    enclosingTypeDescriptor, fieldTypeDescriptor));
      }
      if (!typeStack.get(i).getDescriptor().equals(typeDescriptor)) {
        // not a type of the outer classes.
        qualifier = new ThisReference((RegularTypeDescriptor) currentType.getDescriptor());
      }
      return qualifier;
    }

    private Expression convertCapturedVariableReference(
        Variable variable, TypeDescriptor enclosingClassDescriptor) {
      // the variable is declared outside current type, i.e. a captured variable to current
      // type, and also a captured variable to the outer class in the type stack that is
      // inside {@code enclosingClassRef}.
      for (int i = typeStack.size() - 1; i >= 0; i--) {
        if (typeStack.get(i).getDescriptor().equals(enclosingClassDescriptor)) {
          break;
        }
        capturesByTypeDescriptor.put(typeStack.get(i).getDescriptor(), variable);
      }
      // for reference to a captured variable, if it is in a constructor, translate to
      // reference to outer parameter, otherwise, translate to reference to corresponding
      // field created for the captured variable.
      FieldDescriptor fieldDescriptor =
          ASTUtils.getFieldDescriptorForCapture(currentType.getDescriptor(), variable);
      return new FieldAccess(
          new ThisReference((RegularTypeDescriptor) currentType.getDescriptor()), fieldDescriptor);
    }

    private Variable convert(org.eclipse.jdt.core.dom.SingleVariableDeclaration node) {
      String name = node.getName().getFullyQualifiedName();
      TypeDescriptor typeDescriptor =
          node.getType() instanceof org.eclipse.jdt.core.dom.UnionType
              ? convert((org.eclipse.jdt.core.dom.UnionType) node.getType())
              : createTypeDescriptor(node.getType().resolveBinding());
      Variable variable = new Variable(name, typeDescriptor, false, false);
      variableByJdtBinding.put(node.resolveBinding(), variable);
      return variable;
    }

    private StringLiteral convert(org.eclipse.jdt.core.dom.StringLiteral node) {
      return new StringLiteral(node.getEscapedValue());
    }

    private ExpressionStatement convert(org.eclipse.jdt.core.dom.SuperConstructorInvocation node) {
      IMethodBinding superConstructorBinding = node.resolveConstructorBinding();
      ITypeBinding superclassBinding = superConstructorBinding.getDeclaringClass();
      TypeDescriptor superclassDescriptor =
          createTypeDescriptor(superclassBinding.getDeclaringClass());
      MethodDescriptor methodDescriptor =
          JdtUtils.createMethodDescriptor(superConstructorBinding, compilationUnitNameLocator);
      @SuppressWarnings("unchecked")
      List<Expression> arguments = convert(node.arguments());
      Expression qualifier = node.getExpression() == null ? null : convert(node.getExpression());
      // super() call to an inner class without explicit qualifier, find the enclosing instance.
      if (qualifier == null && JdtUtils.isInstanceNestedClass(superclassBinding)) {
        qualifier = convertOuterClassReference(superclassDescriptor);
      }
      MethodCall superCall = new MethodCall(qualifier, methodDescriptor, arguments);
      return new ExpressionStatement(superCall);
    }

    private Expression convert(org.eclipse.jdt.core.dom.ThisExpression node) {
      if (node.getQualifier() == null) {
        TypeDescriptor thisTypeDescriptor = createTypeDescriptor(node.resolveTypeBinding());
        return new ThisReference((RegularTypeDescriptor) thisTypeDescriptor);
      } else {
        return convertOuterClassReference(createTypeDescriptor(node.resolveTypeBinding()));
      }
    }

    private Expression convert(org.eclipse.jdt.core.dom.TypeLiteral typeLiteral) {
      ITypeBinding typeBinding = typeLiteral.getType().resolveBinding();

      TypeDescriptor literalTypeDescriptor = createTypeDescriptor(typeBinding);
      TypeDescriptor javaLangClassTypeDescriptor =
          createTypeDescriptor(typeLiteral.resolveTypeBinding());
      if (typeBinding.getDimensions() == 0) {
        // <ClassLiteralClass>.$class
        FieldDescriptor classFieldDescriptor =
            FieldDescriptor.createRaw(
                true, literalTypeDescriptor, "$class", javaLangClassTypeDescriptor);
        return new FieldAccess(null, classFieldDescriptor);
      }

      FieldDescriptor classFieldDescriptor =
          FieldDescriptor.createRaw(
              true,
              literalTypeDescriptor.getLeafTypeDescriptor(),
              "$class",
              javaLangClassTypeDescriptor);

      MethodDescriptor forArrayMethodDescriptor =
          MethodDescriptor.createRaw(true, javaLangClassTypeDescriptor, "$forArray");

      // <ClassLiteralClass>.$class.forArray(<dimensions>)
      return new MethodCall(
          new FieldAccess(null, classFieldDescriptor),
          forArrayMethodDescriptor,
          ImmutableList.<Expression>of(
              new NumberLiteral(
                  TypeDescriptor.INT_TYPE_DESCRIPTOR,
                  String.valueOf(typeBinding.getDimensions()))));
    }

    private ThrowStatement convert(org.eclipse.jdt.core.dom.ThrowStatement node) {
      return new ThrowStatement(convert(node.getExpression()));
    }

    private TryStatement convert(org.eclipse.jdt.core.dom.TryStatement node) {
      Block body = convert(node.getBody());
      List<CatchClause> catchClauses = new ArrayList<>();
      for (Object catchClause : node.catchClauses()) {
        catchClauses.add(convert((org.eclipse.jdt.core.dom.CatchClause) catchClause));
      }
      Block finallyBlock = node.getFinally() == null ? null : convert(node.getFinally());
      return new TryStatement(body, catchClauses, finallyBlock);
    }

    private UnionTypeDescriptor convert(org.eclipse.jdt.core.dom.UnionType node) {
      List<TypeDescriptor> types = new ArrayList<>();
      for (Object object : node.types()) {
        org.eclipse.jdt.core.dom.Type type = (org.eclipse.jdt.core.dom.Type) object;
        types.add(createTypeDescriptor(type.resolveBinding()));
      }
      return UnionTypeDescriptor.create(types);
    }

    private VariableDeclarationFragment convert(
        org.eclipse.jdt.core.dom.VariableDeclarationFragment node) {
      IVariableBinding variableBinding = node.resolveBinding();
      Variable variable = JdtUtils.createVariable(variableBinding, compilationUnitNameLocator);
      Expression initializer =
          node.getInitializer() == null ? null : convert(node.getInitializer());
      variableByJdtBinding.put(variableBinding, variable);
      return new VariableDeclarationFragment(variable, initializer);
    }

    private VariableDeclarationStatement convert(
        org.eclipse.jdt.core.dom.VariableDeclarationStatement node) {
      List<VariableDeclarationFragment> variableDeclarations = new ArrayList<>();
      for (Object object : node.fragments()) {
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
            (org.eclipse.jdt.core.dom.VariableDeclarationFragment) object;
        variableDeclarations.add(convert(fragment));
      }
      return new VariableDeclarationStatement(variableDeclarations);
    }

    private JavaType createJavaType(ITypeBinding typeBinding) {
      if (typeBinding == null) {
        return null;
      }
      ITypeBinding superclassBinding = typeBinding.getSuperclass();
      JavaType type =
          new JavaType(
              typeBinding.isInterface() ? Kind.INTERFACE : Kind.CLASS,
              JdtUtils.getVisibility(typeBinding.getModifiers()),
              createTypeDescriptor(typeBinding));

      type.setEnclosingTypeDescriptor(createTypeDescriptor(typeBinding.getDeclaringClass()));

      TypeDescriptor superTypeDescriptor = createTypeDescriptor(superclassBinding);
      type.setSuperTypeDescriptor(superTypeDescriptor);
      for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
        type.addSuperInterfaceDescriptor(createTypeDescriptor(superInterface));
      }
      type.setLocal(typeBinding.isLocal());
      type.setStatic(JdtUtils.isStatic(typeBinding.getModifiers()));
      return type;
    }

    private boolean isObjectMethodBinding(IMethodBinding methodBinding) {
      for (IMethodBinding objectMethodBinding : javaLangObjectBinding.getDeclaredMethods()) {
        if (methodBinding == objectMethodBinding || methodBinding.overrides(objectMethodBinding)) {
          return true;
        }
      }
      return false;
    }
  }

  private CompilationUnitNameLocator compilationUnitNameLocator;

  private CompilationUnit buildCompilationUnit(
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit) {
    ASTConverter converter = new ASTConverter();
    return converter.convert(sourceFilePath, jdtCompilationUnit);
  }

  private TypeDescriptor createTypeDescriptor(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }
    TypeDescriptor typeDescriptor =
        JdtUtils.createTypeDescriptor(typeBinding, compilationUnitNameLocator);
    return typeDescriptor;
  }

  public static List<CompilationUnit> build(
      Map<String, org.eclipse.jdt.core.dom.CompilationUnit> jdtUnitsByFilePath,
      FrontendOptions options,
      Errors errors) {
    CompilationUnitBuilder compilationUnitBuilder =
        new CompilationUnitBuilder(
            new CompilationUnitNameLocator(jdtUnitsByFilePath, options, errors));

    List<CompilationUnit> compilationUnits = new ArrayList<>();
    for (Entry<String, org.eclipse.jdt.core.dom.CompilationUnit> entry :
        jdtUnitsByFilePath.entrySet()) {
      compilationUnits.add(
          compilationUnitBuilder.buildCompilationUnit(entry.getKey(), entry.getValue()));
    }
    return compilationUnits;
  }

  private CompilationUnitBuilder(CompilationUnitNameLocator compilationUnitNameLocator) {
    this.compilationUnitNameLocator = compilationUnitNameLocator;
  }
}
