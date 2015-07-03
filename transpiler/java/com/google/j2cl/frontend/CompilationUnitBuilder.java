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
import com.google.common.collect.Lists;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DoWhileStatement;
import com.google.j2cl.ast.EmptyStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldReference;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JavaType.Kind;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodReference;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.ParenthesizedExpression;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.RegularTypeReference;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclaration;
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
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

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

    private String currentSourceFile;

    private CompilationUnit convert(
        String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit) {
      currentSourceFile = sourceFilePath;
      String packageName = JdtUtils.getCompilationUnitPackageName(jdtCompilationUnit);
      CompilationUnit j2clCompilationUnit = new CompilationUnit(sourceFilePath, packageName);
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
      return types;
    }

    private List<Field> convert(FieldDeclaration node) {
      List<Field> fields = new ArrayList<>();
      for (Object object : node.fragments()) {
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
        Expression initializer =
            fragment.getInitializer() == null ? null : convert(fragment.getInitializer());
        fields.add(
            new Field(
                JdtUtils.createFieldReference(
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
      return new Method(
          JdtUtils.createMethodReference(node.resolveBinding(), compilationUnitNameLocator),
          parameters,
          body);
    }

    @SuppressWarnings("cast")
    private ArrayAccess convert(org.eclipse.jdt.core.dom.ArrayAccess node) {
      return new ArrayAccess(convert(node.getArray()), convert(node.getIndex()));
    }

    @SuppressWarnings("cast")
    private NewArray convert(org.eclipse.jdt.core.dom.ArrayCreation node) {
      ArrayType arrayType = node.getType();

      @SuppressWarnings("unchecked")
      List<Expression> dimensionExpressions =
          convert((List<org.eclipse.jdt.core.dom.Expression>) node.dimensions());
      // If some dimensions are not initialized then make that explicit.
      while (dimensionExpressions.size() < arrayType.getDimensions()) {
        dimensionExpressions.add(new NullLiteral());
      }

      ITypeBinding leafTypeBinding = arrayType.getElementType().resolveBinding();
      return new NewArray(
          dimensionExpressions,
          JdtUtils.createTypeReference(leafTypeBinding, compilationUnitNameLocator));
    }

    private BooleanLiteral convert(org.eclipse.jdt.core.dom.BooleanLiteral node) {
      return node.booleanValue() ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
    }

    private CastExpression convert(org.eclipse.jdt.core.dom.CastExpression node) {
      Expression expression = convert(node.getExpression());
      TypeReference castType =
          JdtUtils.createTypeReference(node.getType().resolveBinding(), compilationUnitNameLocator);
      return new CastExpression(expression, castType);
    }

    private NewInstance convert(org.eclipse.jdt.core.dom.ClassInstanceCreation node) {
      Expression qualifier = node.getExpression() == null ? null : convert(node.getExpression());
      MethodReference constructor =
          JdtUtils.createMethodReference(
              node.resolveConstructorBinding(), compilationUnitNameLocator);
      List<Expression> arguments = new ArrayList<>();
      for (Object argument : node.arguments()) {
        arguments.add(convert((org.eclipse.jdt.core.dom.Expression) argument));
      }
      return new NewInstance(qualifier, constructor, arguments);
    }

    private Expression convert(org.eclipse.jdt.core.dom.Expression node) {
      switch (node.getNodeType()) {
        case ASTNode.ARRAY_ACCESS:
          return convert((org.eclipse.jdt.core.dom.ArrayAccess) node);
        case ASTNode.ARRAY_CREATION:
          return convert((org.eclipse.jdt.core.dom.ArrayCreation) node);
        case ASTNode.ASSIGNMENT:
          return convert((org.eclipse.jdt.core.dom.Assignment) node);
        case ASTNode.BOOLEAN_LITERAL:
          return convert((org.eclipse.jdt.core.dom.BooleanLiteral) node);
        case ASTNode.CAST_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.CastExpression) node);
        case ASTNode.CLASS_INSTANCE_CREATION:
          return convert((org.eclipse.jdt.core.dom.ClassInstanceCreation) node);
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
        default:
          throw new RuntimeException(
              "Need to implement translation for expression type: "
                  + node.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
      }
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

    private Collection<Statement> convert(org.eclipse.jdt.core.dom.Statement node) {
      switch (node.getNodeType()) {
        case ASTNode.ASSERT_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.AssertStatement) node));
        case ASTNode.CONSTRUCTOR_INVOCATION:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.ConstructorInvocation) node));
        case ASTNode.DO_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.DoStatement) node));
        case ASTNode.EMPTY_STATEMENT:
          return singletonStatement(new EmptyStatement());
        case ASTNode.EXPRESSION_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.ExpressionStatement) node));
        case ASTNode.IF_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.IfStatement) node));
        case ASTNode.RETURN_STATEMENT:
          return singletonStatement(convert((org.eclipse.jdt.core.dom.ReturnStatement) node));
        case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
          return singletonStatement(
              convert((org.eclipse.jdt.core.dom.SuperConstructorInvocation) node));
        case ASTNode.VARIABLE_DECLARATION_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.VariableDeclarationStatement) node);
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

    private DoWhileStatement convert(org.eclipse.jdt.core.dom.DoStatement jdtDoStatement) {
      Block block = extractBlock(jdtDoStatement.getBody());
      Expression conditionExpression = convert(jdtDoStatement.getExpression());
      return new DoWhileStatement(conditionExpression, block);
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
      TypeReference rightOperand = createTypeReference(node.getRightOperand().resolveBinding());
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

    private ExpressionStatement convert(org.eclipse.jdt.core.dom.ConstructorInvocation node) {
      IMethodBinding constructorBinding = node.resolveConstructorBinding();
      MethodReference methodRef =
          JdtUtils.createMethodReference(constructorBinding, compilationUnitNameLocator);
      @SuppressWarnings("unchecked")
      List<Expression> arguments = convert(node.arguments());
      return new ExpressionStatement(new MethodCall(null, methodRef, arguments));
    }

    private Statement convert(org.eclipse.jdt.core.dom.ExpressionStatement node) {
      return new ExpressionStatement(convert(node.getExpression()));
    }

    private FieldAccess convert(org.eclipse.jdt.core.dom.FieldAccess node) {
      Expression qualifier = convert(node.getExpression());
      IVariableBinding variableBinding = node.resolveFieldBinding();
      FieldReference field =
          JdtUtils.createFieldReference(variableBinding, compilationUnitNameLocator);
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
      Expression qualifier = node.getExpression() == null ? null : convert(node.getExpression());
      MethodReference methodRef =
          JdtUtils.createMethodReference(methodBinding, compilationUnitNameLocator);
      @SuppressWarnings("unchecked")
      List<Expression> arguments = convert(node.arguments());
      return new MethodCall(qualifier, methodRef, arguments);
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
              JdtUtils.createFieldReference(variableBinding, compilationUnitNameLocator));
        } else {
          throw new RuntimeException(
              "Need to implement translation for QualifiedName that is not a field.");
        }
      } else if (binding instanceof ITypeBinding) {
        ITypeBinding typeBinding = (ITypeBinding) binding;
        return JdtUtils.createTypeReference(typeBinding, compilationUnitNameLocator);
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
              null, JdtUtils.createFieldReference(variableBinding, compilationUnitNameLocator));
        } else if (variableBinding.isParameter()) {
          return new VariableReference(variableByJdtBinding.get(variableBinding));
        } else {
          // local variable.
          return new VariableReference(variableByJdtBinding.get(variableBinding));
        }
      } else if (binding instanceof ITypeBinding) {
        ITypeBinding typeBinding = (ITypeBinding) binding;
        return JdtUtils.createTypeReference(typeBinding, compilationUnitNameLocator);
      } else {
        // TODO: to be implemented
        throw new RuntimeException(
            "Need to implement translation for SimpleName binding: " + node.getClass().getName());
      }
    }

    private StringLiteral convert(org.eclipse.jdt.core.dom.StringLiteral node) {
      return new StringLiteral(node.getEscapedValue());
    }

    private ExpressionStatement convert(org.eclipse.jdt.core.dom.SuperConstructorInvocation node) {
      IMethodBinding constructorBinding = node.resolveConstructorBinding();
      MethodReference methodRef =
          JdtUtils.createMethodReference(constructorBinding, compilationUnitNameLocator);
      @SuppressWarnings("unchecked")
      List<Expression> arguments = convert(node.arguments());
      return new ExpressionStatement(new MethodCall(null, methodRef, arguments));
    }

    private ThisReference convert(org.eclipse.jdt.core.dom.ThisExpression node) {
      RegularTypeReference typeRef =
          node.getQualifier() == null ? null : (RegularTypeReference) convert(node.getQualifier());
      return new ThisReference(typeRef);
    }

    private VariableDeclaration convert(org.eclipse.jdt.core.dom.VariableDeclarationFragment node) {
      IVariableBinding variableBinding = node.resolveBinding();
      Variable variable = JdtUtils.createVariable(variableBinding, compilationUnitNameLocator);
      Expression initializer =
          node.getInitializer() == null ? null : convert(node.getInitializer());
      variableByJdtBinding.put(variableBinding, variable);
      return new VariableDeclaration(variable, initializer);
    }

    private Collection<Statement> convert(
        org.eclipse.jdt.core.dom.VariableDeclarationStatement node) {
      List<Statement> variableDeclarations = new ArrayList<>();
      for (Object object : node.fragments()) {
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
            (org.eclipse.jdt.core.dom.VariableDeclarationFragment) object;
        variableDeclarations.add(convert(fragment));
      }
      return variableDeclarations;
    }

    private JavaType createJavaType(ITypeBinding typeBinding) {
      if (typeBinding == null) {
        return null;
      }
      JavaType type =
          new JavaType(
              typeBinding.isInterface() ? Kind.INTERFACE : Kind.CLASS,
              JdtUtils.getVisibility(typeBinding.getModifiers()),
              createTypeReference(typeBinding));

      ITypeBinding superclassBinding = typeBinding.getSuperclass();
      TypeReference superType = createTypeReference(superclassBinding);
      type.setSuperTypeRef(superType);
      for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
        type.addSuperInterfaceRef(createTypeReference(superInterface));
      }
      return type;
    }
  }

  private CompilationUnit j2clCompilationUnit;
  private CompilationUnitNameLocator compilationUnitNameLocator;

  private CompilationUnit buildCompilationUnit(
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit) {
    ASTConverter converter = new ASTConverter();
    j2clCompilationUnit = converter.convert(sourceFilePath, jdtCompilationUnit);
    return j2clCompilationUnit;
  }

  private TypeReference createTypeReference(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }
    TypeReference typeReference =
        JdtUtils.createTypeReference(typeBinding, compilationUnitNameLocator);
    return typeReference;
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
