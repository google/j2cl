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
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.BreakStatement;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CatchClause;
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
    private boolean inConstructor = false;
    private ITypeBinding javaLangObjectBinding;

    private String currentSourceFile;
    private CompilationUnit j2clCompilationUnit;

    private void pushType(JavaType type) {
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
      for (Object object : node.bodyDeclarations()) {
        if (object instanceof FieldDeclaration) {
          FieldDeclaration fieldDeclaration = (FieldDeclaration) object;
          type.addFields(convert(fieldDeclaration));
        } else if (object instanceof MethodDeclaration) {
          MethodDeclaration methodDeclaration = (MethodDeclaration) object;
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
          if (!JdtUtils.isStatic(nestedTypeDeclaration.getModifiers())) {
            throw new RuntimeException("Need to implement translation for (nested) inner classes");
          }
          types.addAll(convert(nestedTypeDeclaration));
        } else {
          throw new RuntimeException(
              "Need to implement translation for BodyDeclaration type: "
                  + node.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
        }
      }
      TypeDescriptor currentTypeDescriptor = currentType.getDescriptor();
      for (Variable capturedVariable : capturesByTypeDescriptor.get(currentTypeDescriptor)) {
        FieldDescriptor fieldDescriptor =
            ASTUtils.createFieldForCapture(currentTypeDescriptor, capturedVariable);
        currentType.addField(new Field(fieldDescriptor, null, true)); // captured field.
      }
      popType();
      return types;
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

      inConstructor = node.resolveBinding().isConstructor();
      // If a method has no body, initialize the body with an empty list of statements.
      Block body =
          node.getBody() == null ? new Block(new ArrayList<Statement>()) : convert(node.getBody());
      inConstructor = false;
      return new Method(
          JdtUtils.createMethodDescriptor(node.resolveBinding(), compilationUnitNameLocator),
          parameters,
          body);
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

      ITypeBinding leafTypeBinding = arrayType.getElementType().resolveBinding();
      return new NewArray(
          dimensionExpressions,
          JdtUtils.createTypeDescriptor(leafTypeBinding, compilationUnitNameLocator),
          arrayLiteral);
    }

    @SuppressWarnings({"cast", "unchecked"})
    private ArrayLiteral convert(org.eclipse.jdt.core.dom.ArrayInitializer node) {
      return new ArrayLiteral(
          convert((List<org.eclipse.jdt.core.dom.Expression>) node.expressions()));
    }

    private BooleanLiteral convert(org.eclipse.jdt.core.dom.BooleanLiteral node) {
      return node.booleanValue() ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
    }

    private CastExpression convert(org.eclipse.jdt.core.dom.CastExpression node) {
      Expression expression = convert(node.getExpression());
      TypeDescriptor castType =
          JdtUtils.createTypeDescriptor(
              node.getType().resolveBinding(), compilationUnitNameLocator);
      return new CastExpression(expression, castType);
    }

    private NewInstance convert(org.eclipse.jdt.core.dom.ClassInstanceCreation node) {
      Expression qualifier = node.getExpression() == null ? null : convert(node.getExpression());
      MethodDescriptor constructor =
          JdtUtils.createMethodDescriptor(
              node.resolveConstructorBinding(), compilationUnitNameLocator);
      List<Expression> arguments = new ArrayList<>();
      for (Object argument : node.arguments()) {
        arguments.add(convert((org.eclipse.jdt.core.dom.Expression) argument));
      }
      NewInstance newInstance = new NewInstance(qualifier, constructor, arguments);
      // If the class has captures, add corresponding arguments to NewInstance's extraArguments,
      // which will be added to real arguments by {@code NormalizeLocalClassConstructorsVisitor}.
      for (Variable capturedVariable :
          capturesByTypeDescriptor.get(constructor.getEnclosingClassDescriptor())) {
        if (capturesByTypeDescriptor.containsEntry(currentType.getDescriptor(), capturedVariable)) {
          // If the capturedVariable is also a captured variable in current type, pass the
          // corresponding field in current type as an argument.
          newInstance.addExtraArgument(
              new FieldAccess(
                  new ThisReference(null),
                  ASTUtils.createFieldForCapture(
                      currentType.getDescriptor(), capturedVariable)));
        } else {
          // otherwise, the captured variable is in the scope of the current type, so pass the
          // variable directly as an argument.
          newInstance.addExtraArgument(new VariableReference(capturedVariable));
        }
      }
      return newInstance;
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
      return new TernaryExpression(conditionExpression, trueExpression, falseExpression);
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
      TypeDescriptor rightOperand = createTypeDescriptor(node.getRightOperand().resolveBinding());
      return new InstanceOfExpression(leftOperand, rightOperand);
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
      return new BinaryExpression(leftHandSide, operator, rightHandSide);
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
      FieldDescriptor field =
          JdtUtils.createFieldDescriptor(variableBinding, compilationUnitNameLocator);
      return new FieldAccess(qualifier, field);
    }

    private BinaryExpression convert(org.eclipse.jdt.core.dom.InfixExpression node) {
      Expression leftOperand = convert(node.getLeftOperand());
      Expression rightOperand = convert(node.getRightOperand());
      BinaryOperator operator = JdtUtils.getBinaryOperator(node.getOperator());
      BinaryExpression binaryExpression = new BinaryExpression(leftOperand, operator, rightOperand);
      for (Object object : node.extendedOperands()) {
        org.eclipse.jdt.core.dom.Expression extendedOperand =
            (org.eclipse.jdt.core.dom.Expression) object;
        binaryExpression =
            new BinaryExpression(binaryExpression, operator, convert(extendedOperand));
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
      TypeDescriptor returnTypeDescriptor =
          JdtUtils.createTypeDescriptor(methodBinding.getReturnType(), compilationUnitNameLocator);
      int originalParameterCount = methodBinding.getParameterTypes().length;

      TypeDescriptor[] parameterTypeDescriptors = new TypeDescriptor[originalParameterCount + 1];
      // Turn the instance into now a first parameter to the devirtualized method.
      parameterTypeDescriptors[0] = TypeDescriptor.OBJECT_TYPE_DESCRIPTOR;
      for (int i = 0; i < originalParameterCount; i++) {
        parameterTypeDescriptors[i + 1] =
            JdtUtils.createTypeDescriptor(
                methodBinding.getParameterTypes()[i], compilationUnitNameLocator);
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
      return new NumberLiteral(node.getToken());
    }

    private ParenthesizedExpression convert(org.eclipse.jdt.core.dom.ParenthesizedExpression node) {
      return new ParenthesizedExpression(convert(node.getExpression()));
    }

    private PostfixExpression convert(org.eclipse.jdt.core.dom.PostfixExpression node) {
      return new PostfixExpression(
          convert(node.getOperand()), JdtUtils.getPostfixOperator(node.getOperator()));
    }

    private PrefixExpression convert(org.eclipse.jdt.core.dom.PrefixExpression node) {
      return new PrefixExpression(
          convert(node.getOperand()), JdtUtils.getPrefixOperator(node.getOperator()));
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
          return new FieldAccess(
              null, JdtUtils.createFieldDescriptor(variableBinding, compilationUnitNameLocator));
        } else {
          // local variable or parameter.
          Preconditions.checkNotNull(variableBinding.getDeclaringMethod());
          Variable variable = variableByJdtBinding.get(variableBinding);
          // the innermost type that this variable are declared.
          TypeDescriptor enclosingTypeDescriptor =
              JdtUtils.createTypeDescriptor(
                  variableBinding.getDeclaringMethod().getDeclaringClass(),
                  compilationUnitNameLocator);
          TypeDescriptor currentTypeDescriptor = currentType.getDescriptor();
          if (!enclosingTypeDescriptor.equals(currentTypeDescriptor)) {
            // the variable is declared outside current type, i.e. a captured variable to current
            // type, and also a captured variable to the outer class in the type stack that is
            // inside {@code enclosingTypeDescriptor}.
            for (int i = typeStack.size() - 1; i >= 0; i--) {
              if (typeStack.get(i).getDescriptor().equals(enclosingTypeDescriptor)) {
                break;
              }
              capturesByTypeDescriptor.put(typeStack.get(i).getDescriptor(), variable);
            }
            // for reference to a captured variable, if it is in a constructor, translate to
            // reference to outer parameter, otherwise, translate to reference to corresponding
            // field created for the captured variable.
            if (inConstructor) {
              Variable param = ASTUtils.createParamForCapture(variable);
              return new VariableReference(param);
            } else {
              FieldDescriptor fieldDescriptor =
                  ASTUtils.createFieldForCapture(currentTypeDescriptor, variable);
              return new FieldAccess(new ThisReference(null), fieldDescriptor);
            }
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

    private Variable convert(org.eclipse.jdt.core.dom.SingleVariableDeclaration node) {
      String name = node.getName().getFullyQualifiedName();
      TypeDescriptor typeDescriptor =
          node.getType() instanceof org.eclipse.jdt.core.dom.UnionType
              ? convert((org.eclipse.jdt.core.dom.UnionType) node.getType())
              : JdtUtils.createTypeDescriptor(
                  node.getType().resolveBinding(), compilationUnitNameLocator);
      Variable variable = new Variable(name, typeDescriptor, false, false);
      variableByJdtBinding.put(node.resolveBinding(), variable);
      return variable;
    }

    private StringLiteral convert(org.eclipse.jdt.core.dom.StringLiteral node) {
      return new StringLiteral(node.getEscapedValue());
    }

    private ExpressionStatement convert(org.eclipse.jdt.core.dom.SuperConstructorInvocation node) {
      IMethodBinding constructorBinding = node.resolveConstructorBinding();
      MethodDescriptor methodDescriptor =
          JdtUtils.createMethodDescriptor(constructorBinding, compilationUnitNameLocator);
      @SuppressWarnings("unchecked")
      List<Expression> arguments = convert(node.arguments());
      return new ExpressionStatement(new MethodCall(null, methodDescriptor, arguments));
    }

    private ThisReference convert(org.eclipse.jdt.core.dom.ThisExpression node) {
      Preconditions.checkState(
          node.getQualifier() == null, "Qualified this expression not yet implemented");
      return new ThisReference(null);
    }

    private Expression convert(org.eclipse.jdt.core.dom.TypeLiteral typeLiteral) {
      ITypeBinding typeBinding = typeLiteral.getType().resolveBinding();

      TypeDescriptor literalTypeDescriptor =
          JdtUtils.createTypeDescriptor(typeBinding, compilationUnitNameLocator);
      TypeDescriptor javaLangClassTypeDescriptor =
          JdtUtils.createTypeDescriptor(
              typeLiteral.resolveTypeBinding(), compilationUnitNameLocator);
      if (typeBinding.getDimensions() == 0) {
        // <ClassLiteralClass>.$class
        FieldDescriptor classFieldDescriptor = FieldDescriptor.createRaw(
            true, literalTypeDescriptor, "$class", javaLangClassTypeDescriptor);
        return new FieldAccess(null, classFieldDescriptor);
      }

      FieldDescriptor classFieldDescriptor =
          FieldDescriptor.createRaw(true, literalTypeDescriptor.getLeafTypeDescriptor(),
              "$class", javaLangClassTypeDescriptor);

      MethodDescriptor forArray =
          MethodDescriptor.createRaw(true, javaLangClassTypeDescriptor, "$forArray");

      // <ClassLiteralClass>.$class.forArray(<dimensions>)
      return new MethodCall(
          new FieldAccess(null, classFieldDescriptor),
          forArray,
          ImmutableList.<Expression>of(
              new NumberLiteral(String.valueOf(typeBinding.getDimensions()))));
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
        types.add(JdtUtils.createTypeDescriptor(type.resolveBinding(), compilationUnitNameLocator));
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

      TypeDescriptor superType = createTypeDescriptor(superclassBinding);
      type.setSuperTypeDescriptor(superType);
      for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
        type.addSuperInterfaceDescriptor(createTypeDescriptor(superInterface));
      }
      type.setLocal(typeBinding.isLocal());
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
