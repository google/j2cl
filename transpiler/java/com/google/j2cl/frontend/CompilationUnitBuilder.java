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
import com.google.common.collect.FluentIterable;
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
import com.google.j2cl.ast.ContinueStatement;
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
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.SwitchCase;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.TernaryExpression;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.UnionTypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.VariableDeclarationStatement;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.ast.WhileStatement;
import com.google.j2cl.errors.Errors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.Arrays;
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
    private Map<Variable, JavaType> enclosingTypeByVariable = new HashMap<>();
    private Multimap<TypeDescriptor, Variable> capturesByTypeDescriptor =
        LinkedHashMultimap.create();
    private List<JavaType> typeStack = new ArrayList<>();
    private JavaType currentType = null;

    private String currentSourceFile;
    private CompilationUnit j2clCompilationUnit;
    private org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit;

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
      this.jdtCompilationUnit = jdtCompilationUnit;
      currentSourceFile = sourceFilePath;
      String packageName = JdtUtils.getCompilationUnitPackageName(jdtCompilationUnit);
      j2clCompilationUnit = new CompilationUnit(sourceFilePath, packageName);
      for (Object object : jdtCompilationUnit.types()) {
        AbstractTypeDeclaration abstractTypeDeclaration = (AbstractTypeDeclaration) object;
        convert(abstractTypeDeclaration);
      }
      return j2clCompilationUnit;
    }

    private void convert(AbstractTypeDeclaration typeDeclaration) {
      Preconditions.checkArgument(
          typeDeclaration instanceof TypeDeclaration || typeDeclaration instanceof EnumDeclaration);
      switch (typeDeclaration.getNodeType()) {
        case ASTNode.TYPE_DECLARATION:
          convertAndAddJavaType(
              typeDeclaration.resolveBinding(),
              JdtUtils.<BodyDeclaration>asTypedList(typeDeclaration.bodyDeclarations()));
          break;
        case ASTNode.ENUM_DECLARATION:
          convert((EnumDeclaration) typeDeclaration);
          break;
        default:
          throw new RuntimeException(
              "Need to implement translation for AbstractTypeDeclaration type: "
                  + typeDeclaration.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
      }
    }

    private void convert(EnumDeclaration enumDeclaration) {
      JavaType enumType =
          convertAndAddJavaType(
              enumDeclaration.resolveBinding(),
              JdtUtils.<BodyDeclaration>asTypedList(enumDeclaration.bodyDeclarations()));

      for (EnumConstantDeclaration enumConstantDeclaration :
          JdtUtils.<EnumConstantDeclaration>asTypedList(enumDeclaration.enumConstants())) {
        if (enumConstantDeclaration.getAnonymousClassDeclaration() != null) {
          convertAnonymousClassDeclaration(
              enumConstantDeclaration.getAnonymousClassDeclaration(),
              enumConstantDeclaration.resolveConstructorBinding());
        }
      }

      // this is an Enum.
      Preconditions.checkState(enumType.isEnum());
      enumType.addFields(
          FluentIterable.from(
                  JdtUtils.<EnumConstantDeclaration>asTypedList(enumDeclaration.enumConstants()))
              .transform(
                  new Function<EnumConstantDeclaration, Field>() {
                    @Override
                    public Field apply(EnumConstantDeclaration enumConstantDeclaration) {
                      return convert(enumConstantDeclaration);
                    }
                  })
              .toList());
    }

    private JavaType convertAndAddJavaType(
        ITypeBinding typeBinding,
        List<BodyDeclaration> bodyDeclarations,
        TypeDescriptor... constructorImplicitParameterTypeDescriptors) {
      JavaType type = createJavaType(typeBinding);
      pushType(type);
      j2clCompilationUnit.addType(type);
      boolean hasConstructor = false;
      TypeDescriptor currentTypeDescriptor = type.getDescriptor();
      ITypeBinding superclassBinding = typeBinding.getSuperclass();
      if (superclassBinding != null) {
        capturesByTypeDescriptor.putAll(
            currentTypeDescriptor, capturesByTypeDescriptor.get(type.getSuperTypeDescriptor()));
      }
      for (Object object : bodyDeclarations) {
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
        } else if (object instanceof AbstractTypeDeclaration) {
          // Nested class
          AbstractTypeDeclaration nestedTypeDeclaration = (AbstractTypeDeclaration) object;
          convert(nestedTypeDeclaration);
        } else {
          throw new RuntimeException(
              "Need to implement translation for BodyDeclaration type: "
                  + object.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
        }
      }

      for (Variable capturedVariable : capturesByTypeDescriptor.get(currentTypeDescriptor)) {
        FieldDescriptor fieldDescriptor =
            ASTUtils.getFieldDescriptorForCapture(currentTypeDescriptor, capturedVariable);
        type.addField(new Field(fieldDescriptor, null, capturedVariable)); // captured field.
      }
      if (JdtUtils.isInstanceNestedClass(typeBinding)) {
        // add field for enclosing instance.
        type.addField(
            new Field(
                ASTUtils.getFieldDescriptorForEnclosingInstance(
                    currentTypeDescriptor, type.getEnclosingTypeDescriptor()),
                null));
      }

      // If the class has no default constructor, synthesize the default constructor.
      if (!typeBinding.isInterface() && !hasConstructor) {
        type.addMethod(
            synthesizeDefaultConstructor(
                type.getDescriptor(),
                superclassBinding,
                type.getVisibility(),
                constructorImplicitParameterTypeDescriptors));
      }

      // Add wrapper function to its enclosing class for each constructor.
      if (JdtUtils.isInstanceMemberClass(typeBinding)) {
        Preconditions.checkArgument(typeStack.size() >= 2);
        JavaType outerclassType = typeStack.get(typeStack.size() - 2);
        for (Method innerclassConstructor : type.getConstructors()) {
          outerclassType.addMethod(
              ASTUtils.createMethodForInnerClassCreation(
                  outerclassType.getDescriptor(), innerclassConstructor));
        }
      }

      // Add dispatch methods for package private methods.
      currentType.addMethods(
          PackagePrivateMethodsDispatcher.createDispatchMethods(
              typeBinding, compilationUnitNameLocator));

      // Add bridge methods.
      currentType.addMethods(
          BridgeMethodsCreator.createBridgeMethods(typeBinding, compilationUnitNameLocator));
      popType();
      return type;
    }

    private Field convert(EnumConstantDeclaration enumConstantDeclaration) {
      Expression initializer =
          new NewInstance(
              null,
              JdtUtils.createMethodDescriptor(
                  enumConstantDeclaration.resolveConstructorBinding(), compilationUnitNameLocator),
              convertExpressions(
                  JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(
                      enumConstantDeclaration.arguments())));
      return new Field(
          JdtUtils.createFieldDescriptor(
              enumConstantDeclaration.resolveVariable(), compilationUnitNameLocator),
          initializer);
    }

    private Method synthesizeDefaultConstructor(
        TypeDescriptor typeDescriptor,
        ITypeBinding superclassBinding,
        Visibility visibility,
        TypeDescriptor... constructorImplicitParameterTypeDescriptors) {
      MethodDescriptor methodDescriptor =
          ASTUtils.createConstructorDescriptor(
              typeDescriptor, visibility, constructorImplicitParameterTypeDescriptors);
      Block body = new Block();
      List<Variable> parameters = new ArrayList<>();
      int i = 0;
      for (TypeDescriptor parameterTypeDescriptor : constructorImplicitParameterTypeDescriptors) {
        parameters.add(new Variable("$_" + i++, parameterTypeDescriptor, false, true));
      }
      if (superclassBinding != null) {
        // add super() call.
        body.getStatements().add(synthesizeSuperCall(superclassBinding, parameters));
      }

      Method defaultConstructor = new Method(methodDescriptor, parameters, body);
      return defaultConstructor;
    }

    private Statement synthesizeSuperCall(
        ITypeBinding superTypeBinding, Collection<Variable> constructorImplicitParameters) {
      TypeDescriptor superTypeDescriptor = createTypeDescriptor(superTypeBinding);
      MethodDescriptor superMethodDescriptor =
          ASTUtils.createConstructorDescriptor(
              superTypeDescriptor,
              JdtUtils.getVisibility(superTypeBinding.getModifiers()),
              FluentIterable.from(constructorImplicitParameters)
                  .transform(
                      new Function<Variable, TypeDescriptor>() {
                        @Override
                        public TypeDescriptor apply(Variable variable) {
                          return variable.getTypeDescriptor();
                        }
                      })
                  .toArray(TypeDescriptor.class));

      // find the qualifier of the super() call.
      Expression qualifier =
          JdtUtils.isInstanceNestedClass(superTypeBinding)
              ? convertOuterClassReference(
                  createTypeDescriptor(superTypeBinding.getDeclaringClass()))
              : null;
      MethodCall superCall =
          new MethodCall(
              qualifier,
              superMethodDescriptor,
              FluentIterable.from(constructorImplicitParameters)
                  .transform(
                      new Function<Variable, Expression>() {
                        @Override
                        public Expression apply(Variable variable) {
                          return variable.getReference();
                        }
                      })
                  .toList());
      return new ExpressionStatement(superCall);
    }

    private List<Field> convert(FieldDeclaration fieldDeclaration) {
      List<Field> fields = new ArrayList<>();
      for (Object object : fieldDeclaration.fragments()) {
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
            (org.eclipse.jdt.core.dom.VariableDeclarationFragment) object;
        Expression initializer;
        IVariableBinding variableBinding = fragment.resolveBinding();
        if (variableBinding.getConstantValue() == null) {
          initializer =
              fragment.getInitializer() == null ? null : convert(fragment.getInitializer());
        } else {
          initializer = convertConstantToLiteral(variableBinding);
        }
        Field field =
            new Field(
                JdtUtils.createFieldDescriptor(variableBinding, compilationUnitNameLocator),
                initializer);
        field.setCompileTimeConstant(variableBinding.getConstantValue() != null);
        fields.add(field);
      }
      return fields;
    }

    private Expression convertConstantToLiteral(IVariableBinding variableBinding) {
      Object constantValue = variableBinding.getConstantValue();
      if (constantValue instanceof Number) {
        return new NumberLiteral(
            createTypeDescriptor(variableBinding.getType()), (Number) constantValue);
      }
      if (constantValue instanceof String) {
        return new StringLiteral(
            "\"" + StringEscapeUtils.escapeJava((String) constantValue) + "\"");
      }
      if (constantValue instanceof Character) {
        return new CharacterLiteral(
            (char) constantValue,
            "\"" + StringEscapeUtils.escapeJava(String.valueOf((char) constantValue)) + "\"");
      }
      if (constantValue instanceof Boolean) {
        return (boolean) constantValue ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
      }
      throw new RuntimeException(
          "Need to implement translation for compile time constants of type: "
              + constantValue.getClass().getSimpleName()
              + ".");
    }

    private Method convert(MethodDeclaration methodDeclaration) {
      List<Variable> parameters = new ArrayList<>();
      for (Object element : methodDeclaration.parameters()) {
        SingleVariableDeclaration parameter = (SingleVariableDeclaration) element;
        IVariableBinding parameterBinding = parameter.resolveBinding();
        Variable j2clParameter =
            JdtUtils.createVariable(parameterBinding, compilationUnitNameLocator);
        parameters.add(j2clParameter);
        variableByJdtBinding.put(parameterBinding, j2clParameter);
      }

      // If a method has no body, initialize the body with an empty list of statements.
      Block body =
          methodDeclaration.getBody() == null
              ? new Block(new ArrayList<Statement>())
              : convert(methodDeclaration.getBody());
      // synthesize default super() call.
      if (methodDeclaration.isConstructor() && needSynthesizeSuperCall(methodDeclaration)) {
        body
            .getStatements()
            .add(
                0,
                synthesizeSuperCall(
                    methodDeclaration.resolveBinding().getDeclaringClass().getSuperclass(),
                    Collections.<Variable>emptyList()));
      }

      Method method =
          new Method(
              JdtUtils.createMethodDescriptor(
                  methodDeclaration.resolveBinding(), compilationUnitNameLocator),
              parameters,
              body,
              JdtUtils.isOverride(methodDeclaration.resolveBinding()));
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
    private ArrayAccess convert(org.eclipse.jdt.core.dom.ArrayAccess expression) {
      return new ArrayAccess(convert(expression.getArray()), convert(expression.getIndex()));
    }

    @SuppressWarnings({"cast", "unchecked"})
    private NewArray convert(org.eclipse.jdt.core.dom.ArrayCreation expression) {
      ArrayType arrayType = expression.getType();

      List<Expression> dimensionExpressions =
          convertExpressions((List<org.eclipse.jdt.core.dom.Expression>) expression.dimensions());
      // If some dimensions are not initialized then make that explicit.
      while (dimensionExpressions.size() < arrayType.getDimensions()) {
        dimensionExpressions.add(NullLiteral.NULL);
      }

      ArrayLiteral arrayLiteral =
          expression.getInitializer() == null ? null : convert(expression.getInitializer());

      ArrayTypeDescriptor typeDescriptor =
          (ArrayTypeDescriptor) createTypeDescriptor(expression.resolveTypeBinding());
      return new NewArray(typeDescriptor, dimensionExpressions, arrayLiteral);
    }

    @SuppressWarnings({"cast", "unchecked"})
    private ArrayLiteral convert(org.eclipse.jdt.core.dom.ArrayInitializer expression) {
      return new ArrayLiteral(
          createTypeDescriptor(expression.resolveTypeBinding()),
          convertExpressions((List<org.eclipse.jdt.core.dom.Expression>) expression.expressions()));
    }

    private BooleanLiteral convert(org.eclipse.jdt.core.dom.BooleanLiteral literal) {
      return literal.booleanValue() ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
    }

    private CastExpression convert(org.eclipse.jdt.core.dom.CastExpression expression) {
      TypeDescriptor castTypeDescriptor =
          JdtUtils.createTypeDescriptor(
              expression.getType().resolveBinding(), compilationUnitNameLocator);
      return new CastExpression(convert(expression.getExpression()), castTypeDescriptor);
    }

    private CharacterLiteral convert(org.eclipse.jdt.core.dom.CharacterLiteral literal) {
      return new CharacterLiteral(literal.charValue(), literal.getEscapedValue());
    }

    @SuppressWarnings("unchecked")
    private Expression convert(org.eclipse.jdt.core.dom.ClassInstanceCreation expression) {
      IMethodBinding constructorBinding = expression.resolveConstructorBinding();

      if (expression.getAnonymousClassDeclaration() != null) {
        convertAnonymousClassDeclaration(
            expression.getAnonymousClassDeclaration(), constructorBinding);
      }

      Expression qualifier =
          expression.getExpression() == null ? null : convert(expression.getExpression());
      MethodDescriptor constructorMethodDescriptor =
          JdtUtils.createMethodDescriptor(constructorBinding, compilationUnitNameLocator);
      List<Expression> arguments = new ArrayList<>();
      for (Object argument : expression.arguments()) {
        arguments.add(convert((org.eclipse.jdt.core.dom.Expression) argument));
      }
      maybePackageVarargs(constructorBinding, expression.arguments(), arguments);
      Expression newInstance = null;
      if (JdtUtils.isInstanceMemberClass(constructorBinding.getDeclaringClass())) {
        // outerclass.new InnerClass() => outerClass.$create_InnerClass();
        TypeDescriptor outerclassTypeDescriptor =
            createTypeDescriptor(constructorBinding.getDeclaringClass());
        newInstance =
            new MethodCall(
                qualifier,
                ASTUtils.createMethodDescriptorForInnerClassCreation(
                    outerclassTypeDescriptor, constructorMethodDescriptor),
                arguments);
      } else {
        newInstance = new NewInstance(qualifier, constructorMethodDescriptor, arguments);
      }
      if (newInstance.getTypeDescriptor().isParameterizedType()) {
        // add type annotation to ClassInstanceCreation of generic type.
        return CastExpression.createRaw(newInstance, newInstance.getTypeDescriptor());
      } else {
        return newInstance;
      }
    }

    private void convertAnonymousClassDeclaration(
        AnonymousClassDeclaration typeDeclaration, IMethodBinding constructorBinding) {

      // Anonymous classes might have default synthesized constructors take parameters.
      // {@code constructorImplicitParameterTypeDescriptors} are the types for the aforementioned
      // parameters.
      TypeDescriptor[] constructorImplicitParameterTypeDescriptors =
          FluentIterable.from(Arrays.asList(constructorBinding.getParameterTypes()))
              .transform(
                  new Function<ITypeBinding, TypeDescriptor>() {
                    @Override
                    public TypeDescriptor apply(ITypeBinding typeBinding) {
                      return createTypeDescriptor(typeBinding);
                    }
                  })
              .toArray(TypeDescriptor.class);

      convertAndAddJavaType(
          typeDeclaration.resolveBinding(),
          JdtUtils.<BodyDeclaration>asTypedList(typeDeclaration.bodyDeclarations()),
          constructorImplicitParameterTypeDescriptors);
    }

    private Expression convert(org.eclipse.jdt.core.dom.Expression expression) {
      switch (expression.getNodeType()) {
        case ASTNode.ARRAY_ACCESS:
          return convert((org.eclipse.jdt.core.dom.ArrayAccess) expression);
        case ASTNode.ARRAY_CREATION:
          return convert((org.eclipse.jdt.core.dom.ArrayCreation) expression);
        case ASTNode.ARRAY_INITIALIZER:
          return convert((org.eclipse.jdt.core.dom.ArrayInitializer) expression);
        case ASTNode.ASSIGNMENT:
          return convert((org.eclipse.jdt.core.dom.Assignment) expression);
        case ASTNode.BOOLEAN_LITERAL:
          return convert((org.eclipse.jdt.core.dom.BooleanLiteral) expression);
        case ASTNode.CAST_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.CastExpression) expression);
        case ASTNode.CHARACTER_LITERAL:
          return convert((org.eclipse.jdt.core.dom.CharacterLiteral) expression);
        case ASTNode.CLASS_INSTANCE_CREATION:
          return convert((org.eclipse.jdt.core.dom.ClassInstanceCreation) expression);
        case ASTNode.CONDITIONAL_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.ConditionalExpression) expression);
        case ASTNode.FIELD_ACCESS:
          return convert((org.eclipse.jdt.core.dom.FieldAccess) expression);
        case ASTNode.INFIX_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.InfixExpression) expression);
        case ASTNode.INSTANCEOF_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.InstanceofExpression) expression);
        case ASTNode.METHOD_INVOCATION:
          return convert((org.eclipse.jdt.core.dom.MethodInvocation) expression);
        case ASTNode.NULL_LITERAL:
          return convert((org.eclipse.jdt.core.dom.NullLiteral) expression);
        case ASTNode.NUMBER_LITERAL:
          return convert((org.eclipse.jdt.core.dom.NumberLiteral) expression);
        case ASTNode.PARENTHESIZED_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.ParenthesizedExpression) expression);
        case ASTNode.POSTFIX_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.PostfixExpression) expression);
        case ASTNode.PREFIX_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.PrefixExpression) expression);
        case ASTNode.QUALIFIED_NAME:
          return convert((org.eclipse.jdt.core.dom.QualifiedName) expression);
        case ASTNode.SIMPLE_NAME:
          return convert((org.eclipse.jdt.core.dom.SimpleName) expression);
        case ASTNode.STRING_LITERAL:
          return convert((org.eclipse.jdt.core.dom.StringLiteral) expression);
        case ASTNode.SUPER_METHOD_INVOCATION:
          return convert((org.eclipse.jdt.core.dom.SuperMethodInvocation) expression);
        case ASTNode.THIS_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.ThisExpression) expression);
        case ASTNode.TYPE_LITERAL:
          return convert((org.eclipse.jdt.core.dom.TypeLiteral) expression);
        case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.VariableDeclarationExpression) expression);
        default:
          throw new RuntimeException(
              "Need to implement translation for expression type: "
                  + expression.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
      }
    }

    private VariableDeclarationExpression convert(
        org.eclipse.jdt.core.dom.VariableDeclarationExpression expression) {
      @SuppressWarnings("unchecked")
      List<org.eclipse.jdt.core.dom.VariableDeclarationFragment> fragments = expression.fragments();

      List<VariableDeclarationFragment> variableDeclarations = new ArrayList<>();

      for (org.eclipse.jdt.core.dom.VariableDeclarationFragment variableDeclarationFragment :
          fragments) {
        variableDeclarations.add(convert(variableDeclarationFragment));
      }
      return new VariableDeclarationExpression(variableDeclarations);
    }

    private List<Expression> convertExpressions(
        List<org.eclipse.jdt.core.dom.Expression> expressions) {
      return new ArrayList<>(
          Lists.transform(
              expressions,
              new Function<org.eclipse.jdt.core.dom.Expression, Expression>() {
                @Override
                public Expression apply(org.eclipse.jdt.core.dom.Expression expression) {
                  return convert(expression);
                }
              }));
    }

    private List<Statement> convertStatements(List<org.eclipse.jdt.core.dom.Statement> statements) {
      return new ArrayList<>(
          Lists.transform(
              statements,
              new Function<org.eclipse.jdt.core.dom.Statement, Statement>() {
                @Override
                public Statement apply(org.eclipse.jdt.core.dom.Statement expression) {
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

    private Statement convert(org.eclipse.jdt.core.dom.Statement statement) {
      switch (statement.getNodeType()) {
        case ASTNode.ASSERT_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.AssertStatement) statement);
        case ASTNode.BLOCK:
          return convert((org.eclipse.jdt.core.dom.Block) statement);
        case ASTNode.BREAK_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.BreakStatement) statement);
        case ASTNode.CONSTRUCTOR_INVOCATION:
          return convert((org.eclipse.jdt.core.dom.ConstructorInvocation) statement);
        case ASTNode.CONTINUE_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.ContinueStatement) statement);
        case ASTNode.DO_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.DoStatement) statement);
        case ASTNode.EMPTY_STATEMENT:
          return new EmptyStatement();
        case ASTNode.EXPRESSION_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.ExpressionStatement) statement);
        case ASTNode.FOR_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.ForStatement) statement);
        case ASTNode.IF_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.IfStatement) statement);
        case ASTNode.RETURN_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.ReturnStatement) statement);
        case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
          return convert((org.eclipse.jdt.core.dom.SuperConstructorInvocation) statement);
        case ASTNode.SWITCH_CASE:
          return convert((org.eclipse.jdt.core.dom.SwitchCase) statement);
        case ASTNode.SWITCH_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.SwitchStatement) statement);
        case ASTNode.THROW_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.ThrowStatement) statement);
        case ASTNode.TRY_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.TryStatement) statement);
        case ASTNode.TYPE_DECLARATION_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.TypeDeclarationStatement) statement);
        case ASTNode.VARIABLE_DECLARATION_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.VariableDeclarationStatement) statement);
        case ASTNode.WHILE_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.WhileStatement) statement);
        default:
          throw new RuntimeException(
              "Need to implement translation for statement type: "
                  + statement.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
      }
    }

    private BreakStatement convert(org.eclipse.jdt.core.dom.BreakStatement statement) {
      Preconditions.checkState(
          statement.getLabel() == null, "Break statement with label not supported yet");
      return new BreakStatement();
    }

    private ContinueStatement convert(org.eclipse.jdt.core.dom.ContinueStatement statement) {
      Preconditions.checkState(
          statement.getLabel() == null, "Continue statement with label not supported yet");
      return new ContinueStatement();
    }

    private ForStatement convert(org.eclipse.jdt.core.dom.ForStatement statement) {
      // The order here is important since initializers can define new variables
      // These can be used in the expression, updaters or the body
      // This is why we need to process initializers first
      @SuppressWarnings("unchecked")
      List<org.eclipse.jdt.core.dom.VariableDeclarationExpression> jdtInitializers =
          statement.initializers();
      List<Expression> initializers = Lists.newArrayList();
      for (org.eclipse.jdt.core.dom.Expression expression : jdtInitializers) {
        initializers.add(convert(expression));
      }

      Expression conditionExpression =
          statement.getExpression() != null ? convert(statement.getExpression()) : null;

      Statement body = convert(statement.getBody());
      @SuppressWarnings("unchecked")
      List<Expression> updaters = convertExpressions(statement.updaters());
      return new ForStatement(conditionExpression, body, initializers, updaters);
    }

    private DoWhileStatement convert(org.eclipse.jdt.core.dom.DoStatement statement) {
      Statement body = convert(statement.getBody());
      Expression conditionExpression = convert(statement.getExpression());
      return new DoWhileStatement(conditionExpression, body);
    }

    private Statement convert(
        org.eclipse.jdt.core.dom.TypeDeclarationStatement jdtTypeDeclStatement) {
      convert(jdtTypeDeclStatement.getDeclaration());
      return null;
    }

    private WhileStatement convert(org.eclipse.jdt.core.dom.WhileStatement statement) {
      Expression conditionExpression = convert(statement.getExpression());
      Statement body = convert(statement.getBody());
      return new WhileStatement(conditionExpression, body);
    }

    private IfStatement convert(org.eclipse.jdt.core.dom.IfStatement statement) {
      Expression conditionExpression = convert(statement.getExpression());
      Statement thenStatement = convert(statement.getThenStatement());
      Statement elseStatement =
          statement.getElseStatement() == null ? null : convert(statement.getElseStatement());
      return new IfStatement(conditionExpression, thenStatement, elseStatement);
    }

    private InstanceOfExpression convert(org.eclipse.jdt.core.dom.InstanceofExpression expression) {
      TypeDescriptor testTypeDescriptor =
          createTypeDescriptor(expression.getRightOperand().resolveBinding());
      return new InstanceOfExpression(convert(expression.getLeftOperand()), testTypeDescriptor);
    }

    private AssertStatement convert(org.eclipse.jdt.core.dom.AssertStatement statement) {
      Expression message = statement.getMessage() == null ? null : convert(statement.getMessage());
      Expression expression = convert(statement.getExpression());
      return new AssertStatement(expression, message);
    }

    private BinaryExpression convert(org.eclipse.jdt.core.dom.Assignment expression) {
      Expression leftHandSide = convert(expression.getLeftHandSide());
      Expression rightHandSide = convert(expression.getRightHandSide());
      BinaryOperator operator = JdtUtils.getBinaryOperator(expression.getOperator());
      return new BinaryExpression(
          createTypeDescriptor(expression.resolveTypeBinding()),
          leftHandSide,
          operator,
          rightHandSide);
    }

    private Block convert(org.eclipse.jdt.core.dom.Block block) {
      List<Statement> body = new ArrayList<>();
      for (Object object : block.statements()) {
        Statement statement = convert((org.eclipse.jdt.core.dom.Statement) object);
        if (statement != null) {
          body.add(statement);
        }
      }
      return new Block(body);
    }

    private CatchClause convert(org.eclipse.jdt.core.dom.CatchClause catchClause) {
      Variable exceptionVar = convert(catchClause.getException());
      Block body = convert(catchClause.getBody());
      return new CatchClause(body, exceptionVar);
    }

    @SuppressWarnings("unchecked")
    private ExpressionStatement convert(org.eclipse.jdt.core.dom.ConstructorInvocation statement) {
      IMethodBinding constructorBinding = statement.resolveConstructorBinding();
      MethodDescriptor methodDescriptor =
          JdtUtils.createMethodDescriptor(constructorBinding, compilationUnitNameLocator);
      List<Expression> arguments = convertExpressions(statement.arguments());
      maybePackageVarargs(constructorBinding, statement.arguments(), arguments);
      return new ExpressionStatement(new MethodCall(null, methodDescriptor, arguments));
    }

    private Statement convert(org.eclipse.jdt.core.dom.ExpressionStatement statement) {
      return new ExpressionStatement(convert(statement.getExpression()));
    }

    private FieldAccess convert(org.eclipse.jdt.core.dom.FieldAccess expression) {
      Expression qualifier = convert(expression.getExpression());
      IVariableBinding variableBinding = expression.resolveFieldBinding();
      FieldDescriptor fieldDescriptor =
          JdtUtils.createFieldDescriptor(variableBinding, compilationUnitNameLocator);
      return new FieldAccess(qualifier, fieldDescriptor);
    }

    private BinaryExpression convert(org.eclipse.jdt.core.dom.InfixExpression expression) {
      Expression leftOperand = convert(expression.getLeftOperand());
      Expression rightOperand = convert(expression.getRightOperand());
      BinaryOperator operator = JdtUtils.getBinaryOperator(expression.getOperator());
      BinaryExpression binaryExpression =
          new BinaryExpression(
              createTypeDescriptor(expression.resolveTypeBinding()),
              leftOperand,
              operator,
              rightOperand);
      for (Object object : expression.extendedOperands()) {
        org.eclipse.jdt.core.dom.Expression extendedOperand =
            (org.eclipse.jdt.core.dom.Expression) object;
        binaryExpression =
            new BinaryExpression(
                createTypeDescriptor(expression.resolveTypeBinding()),
                binaryExpression,
                operator,
                convert(extendedOperand));
      }
      return binaryExpression;
    }

    @SuppressWarnings("unchecked")
    private MethodCall convert(org.eclipse.jdt.core.dom.MethodInvocation expression) {
      IMethodBinding methodBinding = expression.resolveMethodBinding();

      Expression qualifier =
          expression.getExpression() == null ? null : convert(expression.getExpression());
      MethodDescriptor methodDescriptor =
          JdtUtils.createMethodDescriptor(methodBinding, compilationUnitNameLocator);
      List<Expression> arguments = convertExpressions(expression.arguments());
      maybePackageVarargs(methodBinding, expression.arguments(), arguments);
      MethodCall methodCall = new MethodCall(qualifier, methodDescriptor, arguments);

      // Perform Object method devirtualization.
      if (JdtUtils.isObjectInstanceMethodBinding(methodBinding, jdtCompilationUnit)) {
        return ASTUtils.createDevirtualizedMethodCall(
            methodCall,
            TypeDescriptors.OBJECTS_TYPE_DESCRIPTOR,
            TypeDescriptors.OBJECT_TYPE_DESCRIPTOR);
      }
      return methodCall;
    }

    @SuppressWarnings("unchecked")
    private MethodCall convert(org.eclipse.jdt.core.dom.SuperMethodInvocation expression) {
      IMethodBinding methodBinding = expression.resolveMethodBinding();

      // Do *not* perform Object method devirtualization. The point with super method calls is to
      // *not* call the default version of the method on the prototype and instead call the specific
      // version of the method in the super class. If we were to perform Object method
      // devirtualization then the resulting routing through Objects.doFoo() would end up calling
      // back onto the version of the method on the prototype (aka the wrong one).

      MethodDescriptor methodDescriptor =
          JdtUtils.createMethodDescriptor(methodBinding, compilationUnitNameLocator);
      @SuppressWarnings("unchecked")
      List<Expression> arguments = convertExpressions(expression.arguments());
      maybePackageVarargs(methodBinding, expression.arguments(), arguments);
      return new MethodCall(
          new SuperReference(currentType.getSuperTypeDescriptor()), methodDescriptor, arguments);
    }

    /**
     * Returns if a method call is invoked with varargs that are not in an explicit array format.
     */
    private boolean shouldPackageVarargs(
        IMethodBinding methodBinding, List<org.eclipse.jdt.core.dom.Expression> jdtArguments) {
      int parametersLength = methodBinding.getParameterTypes().length;
      if (!methodBinding.isVarargs()) {
        return false;
      }
      if (jdtArguments.size() != parametersLength) {
        return true;
      }
      org.eclipse.jdt.core.dom.Expression lastArgument = jdtArguments.get(parametersLength - 1);
      return !lastArgument
          .resolveTypeBinding()
          .isAssignmentCompatible(methodBinding.getParameterTypes()[parametersLength - 1]);
    }

    /**
     * Packages the varargs into an array and returns the array.
     */
    private Expression getPackagedVarargs(
        IMethodBinding methodBinding, List<Expression> j2clArguments) {
      Preconditions.checkArgument(methodBinding.isVarargs());
      int parametersLength = methodBinding.getParameterTypes().length;
      TypeDescriptor varargsTypeDescriptor =
          createTypeDescriptor(methodBinding.getParameterTypes()[parametersLength - 1]);
      if (j2clArguments.size() < parametersLength) {
        // no argument for the varargs, add an empty array.
        return new ArrayLiteral(varargsTypeDescriptor, new ArrayList<Expression>());
      }
      List<Expression> valueExpressions = new ArrayList<>();
      for (int i = parametersLength - 1; i < j2clArguments.size(); i++) {
        valueExpressions.add(j2clArguments.get(i));
      }
      return new ArrayLiteral(varargsTypeDescriptor, valueExpressions);
    }

    /**
     * Replaces the var arguments with packaged array.
     */
    private void maybePackageVarargs(
        IMethodBinding methodBinding,
        List<org.eclipse.jdt.core.dom.Expression> jdtArguments,
        List<Expression> j2clArguments) {
      if (shouldPackageVarargs(methodBinding, jdtArguments)) {
        Expression packagedVarargs = getPackagedVarargs(methodBinding, j2clArguments);
        int parameterLength = methodBinding.getParameterTypes().length;
        while (j2clArguments.size() >= parameterLength) {
          j2clArguments.remove(parameterLength - 1);
        }
        j2clArguments.add(packagedVarargs);
      }
    }

    @SuppressWarnings("unused")
    private NullLiteral convert(org.eclipse.jdt.core.dom.NullLiteral literal) {
      return NullLiteral.NULL;
    }

    private NumberLiteral convert(org.eclipse.jdt.core.dom.NumberLiteral literal) {
      return new NumberLiteral(
          createTypeDescriptor(literal.resolveTypeBinding()),
          (Number) literal.resolveConstantExpressionValue());
    }

    private ParenthesizedExpression convert(
        org.eclipse.jdt.core.dom.ParenthesizedExpression expression) {
      return new ParenthesizedExpression(convert(expression.getExpression()));
    }

    private PostfixExpression convert(org.eclipse.jdt.core.dom.PostfixExpression expression) {
      return new PostfixExpression(
          createTypeDescriptor(expression.resolveTypeBinding()),
          convert(expression.getOperand()),
          JdtUtils.getPostfixOperator(expression.getOperator()));
    }

    private PrefixExpression convert(org.eclipse.jdt.core.dom.PrefixExpression expression) {
      return new PrefixExpression(
          createTypeDescriptor(expression.resolveTypeBinding()),
          convert(expression.getOperand()),
          JdtUtils.getPrefixOperator(expression.getOperator()));
    }

    private Expression convert(org.eclipse.jdt.core.dom.QualifiedName expression) {
      IBinding binding = expression.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          return new FieldAccess(
              convert(expression.getQualifier()),
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

    private ReturnStatement convert(org.eclipse.jdt.core.dom.ReturnStatement statement) {
      Expression expression =
          statement.getExpression() == null ? null : convert(statement.getExpression());
      return new ReturnStatement(expression);
    }

    /**
     * Finds and returns the descriptor of the type that encloses the variable referenced by the
     * provided binding.
     */
    private TypeDescriptor findEnclosingTypeDescriptor(IVariableBinding variableBinding) {
      // The binding is a simple field and JDT provides direct knowledge of the declaring class.
      if (variableBinding.getDeclaringClass() != null) {
        return createTypeDescriptor(variableBinding.getDeclaringClass());
      }
      // The binding is a local variable or parameter in method. JDT provides an indirect path to
      // the enclosing class.
      if (variableBinding.getDeclaringMethod() != null) {
        return createTypeDescriptor(variableBinding.getDeclaringMethod().getDeclaringClass());
      }
      // The binding is for a local variable in a static or instance block. JDT does not allow for
      // retrieving the enclosing class. Answer the question from information we've been gathering
      // while processing the compilation unit.
      JavaType javaType = enclosingTypeByVariable.get(variableByJdtBinding.get(variableBinding));
      if (javaType != null) {
        return javaType.getDescriptor();
      }

      throw new RuntimeException(
          "Need to be able to locate the declaring class for variable binding: " + variableBinding);
    }

    private Expression convert(org.eclipse.jdt.core.dom.SimpleName expression) {
      IBinding binding = expression.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          // It refers to a field.
          FieldDescriptor fieldDescriptor =
              JdtUtils.createFieldDescriptor(variableBinding, compilationUnitNameLocator);
          if (!fieldDescriptor.isStatic()
              && !fieldDescriptor
                  .getEnclosingClassTypeDescriptor()
                  .equals(currentType.getDescriptor())) {
            return new FieldAccess(
                convertOuterClassReference(fieldDescriptor.getEnclosingClassTypeDescriptor()),
                fieldDescriptor);
          } else {
            return new FieldAccess(null, fieldDescriptor);
          }
        } else {
          // It refers to a local variable or parameter in a method or block.
          Variable variable = variableByJdtBinding.get(variableBinding);
          // The innermost type in which this variable is declared.
          TypeDescriptor enclosingTypeDescriptor = findEnclosingTypeDescriptor(variableBinding);
          TypeDescriptor currentTypeDescriptor = currentType.getDescriptor();
          if (!enclosingTypeDescriptor.equals(currentTypeDescriptor)) {
            return convertCapturedVariableReference(variable, enclosingTypeDescriptor);
          } else {
            return variable.getReference();
          }
        }
      } else if (binding instanceof ITypeBinding) {
        return null;
      } else {
        // TODO: to be implemented
        throw new RuntimeException(
            "Need to implement translation for SimpleName binding: "
                + expression.getClass().getName());
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
      Expression qualifier = new ThisReference(currentType.getDescriptor());
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
        qualifier = new ThisReference(currentType.getDescriptor());
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
      return new FieldAccess(new ThisReference(currentType.getDescriptor()), fieldDescriptor);
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

    private StringLiteral convert(org.eclipse.jdt.core.dom.StringLiteral literal) {
      return new StringLiteral(literal.getEscapedValue());
    }

    @SuppressWarnings("unchecked")
    private SwitchCase convert(org.eclipse.jdt.core.dom.SwitchCase statement) {
      return statement.isDefault()
          ? new SwitchCase()
          : new SwitchCase(convert(statement.getExpression()));
    }

    @SuppressWarnings("unchecked")
    private SwitchStatement convert(org.eclipse.jdt.core.dom.SwitchStatement statement) {
      return new SwitchStatement(
          convert(statement.getExpression()), convertStatements(statement.statements()));
    }

    @SuppressWarnings("unchecked")
    private ExpressionStatement convert(
        org.eclipse.jdt.core.dom.SuperConstructorInvocation expression) {
      IMethodBinding superConstructorBinding = expression.resolveConstructorBinding();
      ITypeBinding superclassBinding = superConstructorBinding.getDeclaringClass();
      TypeDescriptor superclassDescriptor =
          createTypeDescriptor(superclassBinding.getDeclaringClass());
      MethodDescriptor methodDescriptor =
          JdtUtils.createMethodDescriptor(superConstructorBinding, compilationUnitNameLocator);
      @SuppressWarnings("unchecked")
      List<Expression> arguments = convertExpressions(expression.arguments());
      maybePackageVarargs(superConstructorBinding, expression.arguments(), arguments);
      Expression qualifier =
          expression.getExpression() == null ? null : convert(expression.getExpression());
      // super() call to an inner class without explicit qualifier, find the enclosing instance.
      if (qualifier == null && JdtUtils.isInstanceNestedClass(superclassBinding)) {
        qualifier = convertOuterClassReference(superclassDescriptor);
      }
      MethodCall superCall = new MethodCall(qualifier, methodDescriptor, arguments);
      return new ExpressionStatement(superCall);
    }

    private Expression convert(org.eclipse.jdt.core.dom.ThisExpression expression) {
      if (expression.getQualifier() == null) {
        TypeDescriptor thisTypeDescriptor = createTypeDescriptor(expression.resolveTypeBinding());
        return new ThisReference(thisTypeDescriptor);
      } else {
        return convertOuterClassReference(createTypeDescriptor(expression.resolveTypeBinding()));
      }
    }

    private Expression convert(org.eclipse.jdt.core.dom.TypeLiteral literal) {
      ITypeBinding typeBinding = literal.getType().resolveBinding();

      TypeDescriptor literalTypeDescriptor = createTypeDescriptor(typeBinding);
      TypeDescriptor javaLangClassTypeDescriptor =
          createTypeDescriptor(literal.resolveTypeBinding());
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
          MethodDescriptor.createRaw(
              true,
              javaLangClassTypeDescriptor,
              "$forArray",
              Lists.newArrayList(TypeDescriptors.INT_TYPE_DESCRIPTOR),
              javaLangClassTypeDescriptor);

      // <ClassLiteralClass>.$class.forArray(<dimensions>)
      return new MethodCall(
          new FieldAccess(null, classFieldDescriptor),
          forArrayMethodDescriptor,
          ImmutableList.<Expression>of(
              new NumberLiteral(TypeDescriptors.INT_TYPE_DESCRIPTOR, typeBinding.getDimensions())));
    }

    private ThrowStatement convert(org.eclipse.jdt.core.dom.ThrowStatement statement) {
      return new ThrowStatement(convert(statement.getExpression()));
    }

    private TryStatement convert(org.eclipse.jdt.core.dom.TryStatement statement) {
      Block body = convert(statement.getBody());
      List<CatchClause> catchClauses = new ArrayList<>();
      for (Object catchClause : statement.catchClauses()) {
        catchClauses.add(convert((org.eclipse.jdt.core.dom.CatchClause) catchClause));
      }
      Block finallyBlock = statement.getFinally() == null ? null : convert(statement.getFinally());
      return new TryStatement(body, catchClauses, finallyBlock);
    }

    private UnionTypeDescriptor convert(org.eclipse.jdt.core.dom.UnionType unionType) {
      List<TypeDescriptor> types = new ArrayList<>();
      for (Object object : unionType.types()) {
        org.eclipse.jdt.core.dom.Type type = (org.eclipse.jdt.core.dom.Type) object;
        types.add(createTypeDescriptor(type.resolveBinding()));
      }
      return UnionTypeDescriptor.create(types);
    }

    private VariableDeclarationFragment convert(
        org.eclipse.jdt.core.dom.VariableDeclarationFragment variableDeclarationFragment) {
      IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();
      Variable variable = JdtUtils.createVariable(variableBinding, compilationUnitNameLocator);
      recordEnclosingType(variable, currentType);
      Expression initializer =
          variableDeclarationFragment.getInitializer() == null
              ? null
              : convert(variableDeclarationFragment.getInitializer());
      variableByJdtBinding.put(variableBinding, variable);
      return new VariableDeclarationFragment(variable, initializer);
    }

    /**
     * Records associations of variables and their enclosing type.
     * <p>
     * Enclosing type is a broader category than declaring type since some variables (fields) have a
     * declaring type (that is also their enclosing type) while other variables do not.
     */
    private void recordEnclosingType(Variable variable, JavaType enclosingType) {
      enclosingTypeByVariable.put(variable, enclosingType);
    }

    private VariableDeclarationStatement convert(
        org.eclipse.jdt.core.dom.VariableDeclarationStatement statement) {
      List<VariableDeclarationFragment> variableDeclarations = new ArrayList<>();
      for (Object object : statement.fragments()) {
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
              JdtUtils.getKindFromTypeBinding(typeBinding),
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
  }

  private CompilationUnitNameLocator compilationUnitNameLocator;

  private CompilationUnit buildCompilationUnit(
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
    ASTConverter converter = new ASTConverter();
    return converter.convert(sourceFilePath, compilationUnit);
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
