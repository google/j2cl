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
import com.google.j2cl.ast.AnonymousJavaType;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.AstUtils;
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
import com.google.j2cl.ast.FieldBuilder;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JavaType.Kind;
import com.google.j2cl.ast.LabeledStatement;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptorBuilder;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.ParenthesizedExpression;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.RegularTypeDescriptor;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.SwitchCase;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.SynchronizedStatement;
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
import com.google.j2cl.ast.sourcemap.SourceInfo;

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
import org.eclipse.jdt.core.dom.VariableDeclaration;

import java.util.ArrayList;
import java.util.Arrays;
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
    private Map<String, String> lambdaBinaryNameByKey = new HashMap<>();
    private Map<IVariableBinding, Variable> variableByJdtBinding = new HashMap<>();
    private Map<Variable, JavaType> enclosingTypeByVariable = new HashMap<>();
    private Multimap<TypeDescriptor, Variable> capturesByTypeDescriptor =
        LinkedHashMultimap.create();
    private List<JavaType> typeStack = new ArrayList<>();
    private JavaType currentType = null;

    private String currentSourceFile;
    private org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit;
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
      TypeDescriptors.init(jdtCompilationUnit.getAST());
      currentSourceFile = sourceFilePath;
      String packageName = JdtUtils.getCompilationUnitPackageName(jdtCompilationUnit);
      j2clCompilationUnit = new CompilationUnit(sourceFilePath, packageName);
      this.jdtCompilationUnit = jdtCompilationUnit;
      for (Object object : jdtCompilationUnit.types()) {
        AbstractTypeDeclaration abstractTypeDeclaration = (AbstractTypeDeclaration) object;
        convert(abstractTypeDeclaration);
      }

      return j2clCompilationUnit;
    }

    private void convert(AbstractTypeDeclaration typeDeclaration) {
      switch (typeDeclaration.getNodeType()) {
        case ASTNode.ANNOTATION_TYPE_DECLARATION:
          // We currently do not produce any output for annotations.
          break;
        case ASTNode.TYPE_DECLARATION:
          convertAndAddJavaType(
              JdtUtils.isInStaticContext(typeDeclaration),
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
              JdtUtils.isInStaticContext(enumDeclaration),
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
      EnumMethodsCreator.applyTo(enumType);
    }

    private JavaType convertAndAddJavaType(
        boolean inStaticContext, ITypeBinding typeBinding, List<BodyDeclaration> bodyDeclarations) {
      JavaType type = createJavaType(typeBinding);
      pushType(type);
      j2clCompilationUnit.addType(type);
      TypeDescriptor currentTypeDescriptor = type.getDescriptor();
      ITypeBinding superclassBinding = typeBinding.getSuperclass();
      if (superclassBinding != null) {
        capturesByTypeDescriptor.putAll(
            currentTypeDescriptor, capturesByTypeDescriptor.get(type.getSuperTypeDescriptor()));
      }
      for (int i = 0; i < bodyDeclarations.size(); i++) {
        Object object = bodyDeclarations.get(i);
        if (object instanceof FieldDeclaration) {
          FieldDeclaration fieldDeclaration = (FieldDeclaration) object;
          type.addFields(convert(fieldDeclaration, i));
        } else if (object instanceof MethodDeclaration) {
          MethodDeclaration methodDeclaration = (MethodDeclaration) object;
          type.addMethod(convert(methodDeclaration));
        } else if (object instanceof Initializer) {
          Initializer initializer = (Initializer) object;
          Block block = convert(initializer.getBody());
          block.setPosition(i);
          if (JdtUtils.isStatic(initializer.getModifiers())) {
            type.addStaticInitializerBlock(block);
          } else {
            type.addInstanceInitializerBlock(block);
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
            AstUtils.getFieldDescriptorForCapture(currentTypeDescriptor, capturedVariable);
        type.addField(
            FieldBuilder.fromDefault(fieldDescriptor)
            .capturedVariable(capturedVariable)
            .setPosition(-1) /* Position is not important */
            .build());
      }
      if (!inStaticContext && JdtUtils.isInstanceNestedClass(typeBinding)) {
        // add field for enclosing instance.
        type.addField(
            new Field(
                AstUtils.getFieldDescriptorForEnclosingInstance(
                    currentTypeDescriptor, type.getEnclosingTypeDescriptor()),
                -1 /* Position is not important */));
      }

      // Add dispatch methods for package private methods.
      PackagePrivateMethodsDispatcher.create(typeBinding, type);

      // Add bridge methods.
      BridgeMethodsCreator.create(typeBinding, type);

      // Add unimplemented methods.
      UnimplementedMethodsCreator.create(typeBinding, type);

      // Add bridge methods for JsMethods
      JsBridgeMethodsCreator.create(typeBinding, type);
      popType();
      return type;
    }

    private Field convert(EnumConstantDeclaration enumConstantDeclaration) {
      Expression initializer =
          new NewInstance(
              null,
              JdtUtils.createMethodDescriptor(enumConstantDeclaration.resolveConstructorBinding()),
              convertExpressions(
                  JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(
                      enumConstantDeclaration.arguments())));
      return FieldBuilder.fromDefault(
              JdtUtils.createFieldDescriptor(enumConstantDeclaration.resolveVariable()))
          .initializer(initializer)
          .isEnumField(true)
          .setPosition(-1) /* Position is not important */
          .build();
    }

    private List<Field> convert(FieldDeclaration fieldDeclaration, int position) {
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
            FieldBuilder.fromDefault(JdtUtils.createFieldDescriptor(variableBinding))
                .initializer(initializer)
                .setPosition(position)
                .build();
        fields.add(field);
      }
      return fields;
    }

    private Expression convertConstantToLiteral(IVariableBinding variableBinding) {
      Object constantValue = variableBinding.getConstantValue();
      if (constantValue instanceof Number) {
        return new NumberLiteral(
            JdtUtils.createTypeDescriptor(variableBinding.getType()), (Number) constantValue);
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
        Variable j2clParameter = JdtUtils.createVariable(parameterBinding);
        parameters.add(j2clParameter);
        variableByJdtBinding.put(parameterBinding, j2clParameter);
      }

      // If a method has no body, initialize the body with an empty list of statements.
      Block body =
          methodDeclaration.getBody() == null
              ? new Block(new ArrayList<Statement>())
              : convert(methodDeclaration.getBody());

      IMethodBinding methodBinding = methodDeclaration.resolveBinding();
      Method method =
          new Method(
              JdtUtils.createMethodDescriptor(methodBinding),
              parameters,
              body,
              JdtUtils.isAbstract(methodBinding.getModifiers()),
              JdtUtils.isJsOverride(methodBinding),
              JdtUtils.isFinal(methodBinding.getModifiers()),
              null);

      return method;
    }

    @SuppressWarnings("cast")
    private ArrayAccess convert(org.eclipse.jdt.core.dom.ArrayAccess expression) {
      return new ArrayAccess(convert(expression.getArray()), convert(expression.getIndex()));
    }

    @SuppressWarnings({"cast"})
    private NewArray convert(org.eclipse.jdt.core.dom.ArrayCreation expression) {
      ArrayType arrayType = expression.getType();

      List<Expression> dimensionExpressions =
          convertExpressions(
              JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(expression.dimensions()));
      // If some dimensions are not initialized then make that explicit.
      while (dimensionExpressions.size() < arrayType.getDimensions()) {
        dimensionExpressions.add(NullLiteral.NULL);
      }

      ArrayLiteral arrayLiteral =
          expression.getInitializer() == null ? null : convert(expression.getInitializer());

      ArrayTypeDescriptor typeDescriptor =
          (ArrayTypeDescriptor) JdtUtils.createTypeDescriptor(expression.resolveTypeBinding());
      return new NewArray(typeDescriptor, dimensionExpressions, arrayLiteral);
    }

    @SuppressWarnings({"cast", "unchecked"})
    private ArrayLiteral convert(org.eclipse.jdt.core.dom.ArrayInitializer expression) {
      return new ArrayLiteral(
          (ArrayTypeDescriptor) JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()),
          convertExpressions(expression.expressions()));
    }

    private BooleanLiteral convert(org.eclipse.jdt.core.dom.BooleanLiteral literal) {
      return literal.booleanValue() ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
    }

    private CastExpression convert(org.eclipse.jdt.core.dom.CastExpression expression) {
      TypeDescriptor castTypeDescriptor =
          JdtUtils.createTypeDescriptor(expression.getType().resolveBinding());
      return new CastExpression(convert(expression.getExpression()), castTypeDescriptor);
    }

    private CharacterLiteral convert(org.eclipse.jdt.core.dom.CharacterLiteral literal) {
      return new CharacterLiteral(literal.charValue(), literal.getEscapedValue());
    }

    private Expression convert(org.eclipse.jdt.core.dom.ClassInstanceCreation expression) {
      // constructor MethodDescriptor.
      IMethodBinding constructorBinding = expression.resolveConstructorBinding();
      MethodDescriptor constructorMethodDescriptor =
          JdtUtils.createMethodDescriptor(constructorBinding);

      // arguments.
      List<Expression> arguments = new ArrayList<>();
      for (Object argument : expression.arguments()) {
        arguments.add(convert((org.eclipse.jdt.core.dom.Expression) argument));
      }
      maybePackageVarargs(
          constructorBinding,
          JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(expression.arguments()),
          arguments);

      // anonymous creation.
      if (expression.getAnonymousClassDeclaration() != null) {
        return convertAnonymousClassCreation(expression, constructorMethodDescriptor, arguments);
      } else {
        return convertRegularClassCreation(expression, constructorMethodDescriptor, arguments);
      }
    }

    private AnonymousJavaType convertAnonymousClassDeclaration(
        AnonymousClassDeclaration typeDeclaration, IMethodBinding constructorBinding) {

      // Anonymous classes might have default synthesized constructors take parameters.
      // {@code constructorImplicitParameterTypeDescriptors} are the types for the aforementioned
      // parameters.
      TypeDescriptor[] constructorImplicitParameterTypeDescriptors =
          getParameterTypeDescriptors(constructorBinding.getParameterTypes());

      ITypeBinding typeBinding = typeDeclaration.resolveBinding();
      AnonymousJavaType anonymousClass =
          (AnonymousJavaType)
              convertAndAddJavaType(
                  JdtUtils.isInStaticContext(typeDeclaration),
                  typeBinding,
                  JdtUtils.<BodyDeclaration>asTypedList(typeDeclaration.bodyDeclarations()));
      anonymousClass.addConstructorParameterTypeDescriptors(
          Arrays.asList(constructorImplicitParameterTypeDescriptors));

      // Record the types of the superclass constructor's parameters, if they exist.
      ITypeBinding superTypeBinding = typeBinding.getSuperclass();
      for (IMethodBinding methodBinding : superTypeBinding.getDeclaredMethods()) {
        if (methodBinding.isConstructor() && constructorBinding.isSubsignature(methodBinding)) {
          // Note that we get the types from the declaration, not the binding, because we need the
          // declared type to figure out how to reference the method.
          TypeDescriptor[] superConstructorImplicitParameterTypeDescriptors =
              getParameterTypeDescriptors(
                  methodBinding.getMethodDeclaration().getParameterTypes());
          anonymousClass.addSuperConstructorParameterTypeDescriptors(
              Arrays.asList(superConstructorImplicitParameterTypeDescriptors));
        }
      }

      return anonymousClass;
    }

    private TypeDescriptor[] getParameterTypeDescriptors(ITypeBinding[] parameterTypes) {
      return FluentIterable
          .from(Arrays.asList(parameterTypes))
          .transform(
              new Function<ITypeBinding, TypeDescriptor>() {
                @Override
                public TypeDescriptor apply(ITypeBinding typeBinding) {
                  return JdtUtils.createTypeDescriptor(typeBinding);
                }
              })
          .toArray(TypeDescriptor.class);
    }

    private Expression convertAnonymousClassCreation(
        org.eclipse.jdt.core.dom.ClassInstanceCreation expression,
        MethodDescriptor constructorMethodDescriptor,
        List<Expression> arguments) {
      IMethodBinding constructorBinding = expression.resolveConstructorBinding();
      ITypeBinding newInstanceTypeBinding = constructorBinding.getDeclaringClass();

      Preconditions.checkNotNull(expression.getAnonymousClassDeclaration());
      AnonymousJavaType anonymousClass =
          convertAnonymousClassDeclaration(
              expression.getAnonymousClassDeclaration(), constructorBinding);

      Expression superCallQualifier = null;
      if (JdtUtils.isInstanceMemberClass(newInstanceTypeBinding.getSuperclass())) {
        // super class is an instance member class, there should be a qualifier for super call.
        superCallQualifier =
            expression.getExpression() == null
                ? convertOuterClassReference(
                    JdtUtils.findCurrentTypeBinding(expression),
                    newInstanceTypeBinding.getSuperclass().getDeclaringClass(),
                    false)
                : convert(expression.getExpression());
        anonymousClass.setSuperCallQualifier(superCallQualifier);
      }
      // the qualifier for the NewInstance.
      Expression newInstanceQualifier =
          !JdtUtils.isInStaticContext(expression)
              ? convertOuterClassReference(
                  JdtUtils.findCurrentTypeBinding(expression),
                  newInstanceTypeBinding.getDeclaringClass(),
                  false)
              : null;
      return new NewInstance(newInstanceQualifier, constructorMethodDescriptor, arguments);
    }

    private Expression convertRegularClassCreation(
        org.eclipse.jdt.core.dom.ClassInstanceCreation expression,
        MethodDescriptor constructorMethodDescriptor,
        List<Expression> arguments) {
      IMethodBinding constructorBinding = expression.resolveConstructorBinding();
      Expression newInstance = null;
      ITypeBinding newInstanceTypeBinding = constructorBinding.getDeclaringClass();
      Expression qualifier =
          expression.getExpression() == null ? null : convert(expression.getExpression());
      boolean hasQualifier =
          JdtUtils.isInstanceMemberClass(newInstanceTypeBinding)
              || (newInstanceTypeBinding.isLocal() && !JdtUtils.isInStaticContext(expression));
      // Resolve the qualifier of NewInstance that creates an instance of a nested class.
      // Implicit 'this' doesn't always refer to 'this', it may refer to any enclosing instances.
      if (hasQualifier) {
        qualifier =
            qualifier == null
                ? convertOuterClassReference(
                    JdtUtils.findCurrentTypeBinding(expression),
                    newInstanceTypeBinding.getDeclaringClass(),
                    false) // find the enclosing instance in non-strict mode, which means
                // for example,
                // class A {
                //   class B {}
                //   class C extends class A{
                //     // The qualifier of new B() should be C.this, not A.this.
                //     public void test() { new B(); }
                //   }
                // }
                : qualifier;
        newInstance = new NewInstance(qualifier, constructorMethodDescriptor, arguments);
      } else {
        Preconditions.checkArgument(
            qualifier == null, "NewInstance of non nested class should have no qualifier.");
        newInstance = new NewInstance(qualifier, constructorMethodDescriptor, arguments);
      }

      return newInstance;
    }

    private Expression convert(org.eclipse.jdt.core.dom.Expression expression) {
      Expression j2clExpression;
      switch (expression.getNodeType()) {
        case ASTNode.ARRAY_ACCESS:
          j2clExpression = convert((org.eclipse.jdt.core.dom.ArrayAccess) expression);
          break;
        case ASTNode.ARRAY_CREATION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.ArrayCreation) expression);
          break;
        case ASTNode.ARRAY_INITIALIZER:
          j2clExpression = convert((org.eclipse.jdt.core.dom.ArrayInitializer) expression);
          break;
        case ASTNode.ASSIGNMENT:
          j2clExpression = convert((org.eclipse.jdt.core.dom.Assignment) expression);
          break;
        case ASTNode.BOOLEAN_LITERAL:
          j2clExpression = convert((org.eclipse.jdt.core.dom.BooleanLiteral) expression);
          break;
        case ASTNode.CAST_EXPRESSION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.CastExpression) expression);
          break;
        case ASTNode.CHARACTER_LITERAL:
          j2clExpression = convert((org.eclipse.jdt.core.dom.CharacterLiteral) expression);
          break;
        case ASTNode.CLASS_INSTANCE_CREATION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.ClassInstanceCreation) expression);
          break;
        case ASTNode.CONDITIONAL_EXPRESSION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.ConditionalExpression) expression);
          break;
        case ASTNode.FIELD_ACCESS:
          j2clExpression = convert((org.eclipse.jdt.core.dom.FieldAccess) expression);
          break;
        case ASTNode.INFIX_EXPRESSION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.InfixExpression) expression);
          break;
        case ASTNode.INSTANCEOF_EXPRESSION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.InstanceofExpression) expression);
          break;
        case ASTNode.LAMBDA_EXPRESSION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.LambdaExpression) expression);
          break;
        case ASTNode.METHOD_INVOCATION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.MethodInvocation) expression);
          break;
        case ASTNode.NULL_LITERAL:
          j2clExpression = convert((org.eclipse.jdt.core.dom.NullLiteral) expression);
          break;
        case ASTNode.NUMBER_LITERAL:
          j2clExpression = convert((org.eclipse.jdt.core.dom.NumberLiteral) expression);
          break;
        case ASTNode.PARENTHESIZED_EXPRESSION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.ParenthesizedExpression) expression);
          break;
        case ASTNode.POSTFIX_EXPRESSION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.PostfixExpression) expression);
          break;
        case ASTNode.PREFIX_EXPRESSION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.PrefixExpression) expression);
          break;
        case ASTNode.QUALIFIED_NAME:
          j2clExpression = convert((org.eclipse.jdt.core.dom.QualifiedName) expression);
          break;
        case ASTNode.SIMPLE_NAME:
          j2clExpression = convert((org.eclipse.jdt.core.dom.SimpleName) expression);
          break;
        case ASTNode.STRING_LITERAL:
          j2clExpression = convert((org.eclipse.jdt.core.dom.StringLiteral) expression);
          break;
        case ASTNode.SUPER_METHOD_INVOCATION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.SuperMethodInvocation) expression);
          break;
        case ASTNode.THIS_EXPRESSION:
          j2clExpression = convert((org.eclipse.jdt.core.dom.ThisExpression) expression);
          break;
        case ASTNode.TYPE_LITERAL:
          j2clExpression = convert((org.eclipse.jdt.core.dom.TypeLiteral) expression);
          break;
        case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
          j2clExpression =
              convert((org.eclipse.jdt.core.dom.VariableDeclarationExpression) expression);
          break;
        default:
          throw new RuntimeException(
              "Need to implement translation for expression type: "
                  + expression.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
      }
      return j2clExpression;
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
          JdtUtils.createTypeDescriptor(jdtConditionalExpression.resolveTypeBinding()),
          conditionExpression,
          trueExpression,
          falseExpression);
    }

    private Statement convertStatement(org.eclipse.jdt.core.dom.Statement statement) {
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
        case ASTNode.ENHANCED_FOR_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.EnhancedForStatement) statement);
        case ASTNode.IF_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.IfStatement) statement);
        case ASTNode.LABELED_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.LabeledStatement) statement);
        case ASTNode.RETURN_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.ReturnStatement) statement);
        case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
          return convert((org.eclipse.jdt.core.dom.SuperConstructorInvocation) statement);
        case ASTNode.SWITCH_CASE:
          return convert((org.eclipse.jdt.core.dom.SwitchCase) statement);
        case ASTNode.SWITCH_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.SwitchStatement) statement);
        case ASTNode.SYNCHRONIZED_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.SynchronizedStatement) statement);
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

    public SourceInfo sourceInfoForNode(ASTNode node) {
      int startLineNumber = jdtCompilationUnit.getLineNumber(node.getStartPosition()) - 1;
      int startColumnNumber = jdtCompilationUnit.getColumnNumber(node.getStartPosition());
      int endPositionCharacterIndex = node.getStartPosition() + node.getLength();
      int endLineNumber = jdtCompilationUnit.getLineNumber(endPositionCharacterIndex) - 1;
      int endColumnNumber = jdtCompilationUnit.getColumnNumber(endPositionCharacterIndex);
      return new SourceInfo(startLineNumber, startColumnNumber, endLineNumber, endColumnNumber);
    }

    private Statement convert(org.eclipse.jdt.core.dom.Statement statement) {
      SourceInfo position = sourceInfoForNode(statement);
      Statement j2clStatement = convertStatement(statement);
      if (j2clStatement != null) {
        j2clStatement.setInputSourceInfo(position);
      }
      return j2clStatement;
    }

    private LabeledStatement convert(org.eclipse.jdt.core.dom.LabeledStatement statement) {
      return new LabeledStatement(
          statement.getLabel().getIdentifier(), convert(statement.getBody()));
    }

    private BreakStatement convert(org.eclipse.jdt.core.dom.BreakStatement statement) {
      String labelName = statement.getLabel() == null ? null : statement.getLabel().getIdentifier();
      return new BreakStatement(labelName);
    }

    private ContinueStatement convert(org.eclipse.jdt.core.dom.ContinueStatement statement) {
      String labelName = statement.getLabel() == null ? null : statement.getLabel().getIdentifier();
      return new ContinueStatement(labelName);
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

    private ForStatement convert(org.eclipse.jdt.core.dom.EnhancedForStatement statement) {
      if (statement.getExpression().resolveTypeBinding().isArray()) {
        return convertForEachArray(statement);
      }

      return convertForEachInstance(statement);
    }

    private ForStatement convertForEachArray(
        org.eclipse.jdt.core.dom.EnhancedForStatement statement) {
      // Converts
      //
      //   for(T v : exp) S
      //
      // into
      //
      //   for(T[] $array = (exp), int $index = 0; $index < $array.length; $index++ ) {
      //        T v = (T) $array[$index];
      //        S;
      //   }

      ITypeBinding expressionTypeBinding = statement.getExpression().resolveTypeBinding();

      // T[] array = exp.
      Variable arrayVariable =
          new Variable("$array", JdtUtils.createTypeDescriptor(expressionTypeBinding), true, false);
      VariableDeclarationFragment arrayVariableDeclarationFragment =
          new VariableDeclarationFragment(arrayVariable, convert(statement.getExpression()));

      // int index = 0;
      Variable indexVariable =
          new Variable("$index", TypeDescriptors.get().primitiveInt, true, false);
      VariableDeclarationFragment indexVariableDeclarationFragment =
          new VariableDeclarationFragment(
              indexVariable, new NumberLiteral(TypeDescriptors.get().primitiveInt, 0));

      // $index < $array.length
      Expression condition =
          new BinaryExpression(
              TypeDescriptors.get().primitiveBoolean,
              indexVariable.getReference(),
              BinaryOperator.LESS,
              new FieldAccess(
                  arrayVariable.getReference(), AstUtils.ARRAY_LENGTH_FIELD_DESCRIPTION));

      VariableDeclarationStatement forVariableDeclarationStatement =
          new VariableDeclarationStatement(
              Collections.singletonList(
                  new VariableDeclarationFragment(
                      convert(statement.getParameter()),
                      new ArrayAccess(
                          arrayVariable.getReference(), indexVariable.getReference()))));

      //  {   T t = $array[$index]; S; }
      Statement body = convert(statement.getBody());
      if (body instanceof Block) {
        ((Block) body).getStatements().add(0, forVariableDeclarationStatement);
      } else {
        body = new Block(ImmutableList.of(forVariableDeclarationStatement, body));
      }
      return new ForStatement(
          condition,
          body,
          Collections.<Expression>singletonList(
              new VariableDeclarationExpression(
                  ImmutableList.of(
                      arrayVariableDeclarationFragment, indexVariableDeclarationFragment))),
          Collections.<Expression>singletonList(
              new PostfixExpression(
                  TypeDescriptors.get().primitiveInt,
                  indexVariable.getReference(),
                  PostfixOperator.INCREMENT)));
    }

    private ForStatement convertForEachInstance(
        org.eclipse.jdt.core.dom.EnhancedForStatement statement) {
      // Converts
      //
      //   for(T v : exp) S
      //
      // into
      //
      //   for(Iterator<T> $iterator = (exp).iterator(); $iterator.hasNext(); ) {
      //        T v = (T) $iterator.next();
      //        S;
      //   }

      ITypeBinding expressionTypeBinding = statement.getExpression().resolveTypeBinding();

      IMethodBinding iteratorMethodBinding =
          JdtUtils.getMethodBinding(expressionTypeBinding, "iterator");

      // Iterator<T> $iterator = (exp).iterator();
      Variable iteratorVariable =
          new Variable(
              "$iterator",
              JdtUtils.createTypeDescriptor(iteratorMethodBinding.getReturnType()),
              true,
              false);
      VariableDeclarationFragment iteratorDeclaration =
          new VariableDeclarationFragment(
              iteratorVariable,
              MethodCall.createRegularMethodCall(
                  convert(statement.getExpression()),
                  JdtUtils.createMethodDescriptor(iteratorMethodBinding)));

      // $iterator.hasNext();
      IMethodBinding hasNextMethodBinding =
          JdtUtils.getMethodBinding(iteratorMethodBinding.getReturnType(), "hasNext");
      Expression condition =
          MethodCall.createRegularMethodCall(
              iteratorVariable.getReference(),
              JdtUtils.createMethodDescriptor(hasNextMethodBinding),
              Collections.<Expression>emptyList());

      // T v = $iterator.next();
      IMethodBinding nextMethodBinding =
          JdtUtils.getMethodBinding(iteratorMethodBinding.getReturnType(), "next");
      VariableDeclarationStatement forVariableDeclarationStatement =
          new VariableDeclarationStatement(
              Collections.singletonList(
                  new VariableDeclarationFragment(
                      convert(statement.getParameter()),
                      MethodCall.createRegularMethodCall(
                          iteratorVariable.getReference(),
                          JdtUtils.createMethodDescriptor(nextMethodBinding),
                          Collections.<Expression>emptyList()))));

      Statement body = convert(statement.getBody());
      if (body instanceof Block) {
        ((Block) body).getStatements().add(0, forVariableDeclarationStatement);
      } else {
        body = new Block(ImmutableList.of(forVariableDeclarationStatement, body));
      }
      return new ForStatement(
          condition,
          body,
          Collections.<Expression>singletonList(
              new VariableDeclarationExpression(Collections.singletonList(iteratorDeclaration))),
          Collections.<Expression>emptyList());
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
          JdtUtils.createTypeDescriptor(expression.getRightOperand().resolveBinding());
      return new InstanceOfExpression(convert(expression.getLeftOperand()), testTypeDescriptor);
    }

    private Expression convert(org.eclipse.jdt.core.dom.LambdaExpression expression) {
      /**
       * Lambda expression is converted to an inner class:
       *
       * class Enclosing$lambda$0 implements I {
       *   Enclosing$lambda$0(captures) {// initialize captures}
       *   private T lambda$0(args) {...lambda_expression_with_captures ... }
       *   public T samMethod0(args) { return this.lambda$0(args); }
       * }
       *
       * And replaces the lambda with new Enclosing$lambda$0(captures).
       */
      // Construct a class for the lambda expression.
      ITypeBinding enclosingClassTypeBinding = JdtUtils.findCurrentTypeBinding(expression);

      IMethodBinding lambdaMethodBinding = expression.resolveMethodBinding();
      String lambdaBinaryName =
          getLambdaBinaryName(
              enclosingClassTypeBinding.getBinaryName(),
              lambdaMethodBinding.getName(),
              lambdaMethodBinding.getKey());

      JavaType lambdaType =
          JdtUtils.createLambdaJavaType(
              lambdaBinaryName,
              expression.resolveTypeBinding(),
              (RegularTypeDescriptor) JdtUtils.createTypeDescriptor(enclosingClassTypeBinding));
      pushType(lambdaType);
      TypeDescriptor lambdaTypeDescriptor = lambdaType.getDescriptor();

      // Construct lambda method and add it to lambda inner class.
      String lambdaMethodBinaryName = "lambda" + lambdaBinaryName;
      Method lambdaMethod =
          createLambdaMethod(lambdaMethodBinaryName, expression, lambdaType.getDescriptor());
      lambdaType.addMethod(lambdaMethod);

      // Construct and add SAM method that delegates to the lambda method.
      Method samMethod =
          JdtUtils.createSamMethod(expression.resolveTypeBinding(), lambdaMethod.getDescriptor());
      lambdaType.addMethod(samMethod);

      // Add fields for captured local variables.
      for (Variable capturedVariable : capturesByTypeDescriptor.get(lambdaTypeDescriptor)) {
        FieldDescriptor fieldDescriptor =
            AstUtils.getFieldDescriptorForCapture(lambdaTypeDescriptor, capturedVariable);
        lambdaType.addField(
            FieldBuilder.fromDefault(fieldDescriptor)
                .capturedVariable(capturedVariable)
                .setPosition(-1) /* Position is not important */
                .build());
      }
      // Add field for enclosing instance.
      lambdaType.addField(
          new Field(
              AstUtils.getFieldDescriptorForEnclosingInstance(
                  lambdaTypeDescriptor, lambdaType.getEnclosingTypeDescriptor()),
              -1 /* Position is not important */));

      // Add lambda class to compilation unit.
      j2clCompilationUnit.addType(lambdaType);
      popType();

      // Replace the lambda with new LambdaClass()
      MethodDescriptor constructorMethodDescriptor =
          AstUtils.createDefaultConstructorDescriptor(lambdaTypeDescriptor, Visibility.PUBLIC);
      // qualifier should not be null if the lambda is nested in another lambda.
      Expression qualifier =
          convertOuterClassReference(enclosingClassTypeBinding, enclosingClassTypeBinding, true);
      return new NewInstance(qualifier, constructorMethodDescriptor);
    }

    private Method createLambdaMethod(
        String methodName,
        org.eclipse.jdt.core.dom.LambdaExpression expression,
        TypeDescriptor enclosingClassTypeDescriptor) {
      List<Variable> parameters = new ArrayList<>();
      for (Object parameter : expression.parameters()) {
        parameters.add(convert((VariableDeclaration) parameter));
      }

      IMethodBinding methodBinding = expression.resolveMethodBinding();
      TypeDescriptor returnTypeDescriptor =
          JdtUtils.createTypeDescriptor(methodBinding.getReturnType());

      // the lambda expression body, which can be either a Block or an Expression.
      ASTNode lambdaBody = expression.getBody();
      Block body;
      if (lambdaBody.getNodeType() == ASTNode.BLOCK) {
        body = convert((org.eclipse.jdt.core.dom.Block) lambdaBody);
      } else {
        Preconditions.checkArgument(lambdaBody instanceof org.eclipse.jdt.core.dom.Expression);
        Expression lambdaMethodBody = convert((org.eclipse.jdt.core.dom.Expression) lambdaBody);
        Statement statement =
            returnTypeDescriptor == TypeDescriptors.get().primitiveVoid
                ? new ExpressionStatement(lambdaMethodBody)
                : new ReturnStatement(lambdaMethodBody, returnTypeDescriptor);
        body = new Block(Arrays.asList(statement));
      }

      // generate parameters type descriptors.
      List<TypeDescriptor> parameterTypeDescriptors =
          Lists.transform(
              Arrays.asList(methodBinding.getParameterTypes()),
              new Function<ITypeBinding, TypeDescriptor>() {
                @Override
                public TypeDescriptor apply(ITypeBinding typeBinding) {
                  return JdtUtils.createTypeDescriptor(typeBinding);
                }
              });

      MethodDescriptor methodDescriptor =
          MethodDescriptorBuilder.fromDefault()
              .isRaw(true)
              .visibility(Visibility.PRIVATE)
              .enclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
              .methodName(methodName)
              .parameterTypeDescriptors(parameterTypeDescriptors)
              .returnTypeDescriptor(returnTypeDescriptor)
              .build();
      return new Method(methodDescriptor, parameters, body);
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
          JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()),
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

    private ExpressionStatement convert(org.eclipse.jdt.core.dom.ConstructorInvocation statement) {
      IMethodBinding constructorBinding = statement.resolveConstructorBinding();
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(constructorBinding);
      List<Expression> arguments =
          convertExpressions(
              JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(statement.arguments()));
      maybePackageVarargs(
          constructorBinding,
          JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(statement.arguments()),
          arguments);
      return new ExpressionStatement(
          MethodCall.createRegularMethodCall(null, methodDescriptor, arguments));
    }

    private Statement convert(org.eclipse.jdt.core.dom.ExpressionStatement statement) {
      return new ExpressionStatement(convert(statement.getExpression()));
    }

    private FieldAccess convert(org.eclipse.jdt.core.dom.FieldAccess expression) {
      Expression qualifier = convert(expression.getExpression());
      IVariableBinding variableBinding = expression.resolveFieldBinding();
      FieldDescriptor fieldDescriptor = JdtUtils.createFieldDescriptor(variableBinding);
      // If the field is referenced like a current type field with explicit 'this' but is part of
      // some other type. (It may happen in lambda, where 'this' refers to the enclosing instance).
      if (qualifier instanceof ThisReference
          && !fieldDescriptor
              .getEnclosingClassTypeDescriptor()
              .equals(currentType.getDescriptor())) {
        return new FieldAccess(
            convertOuterClassReference(
                JdtUtils.findCurrentTypeBinding(expression),
                variableBinding.getDeclaringClass(),
                false),
            fieldDescriptor);
      }
      return new FieldAccess(qualifier, fieldDescriptor);
    }

    private BinaryExpression convert(org.eclipse.jdt.core.dom.InfixExpression expression) {
      Expression leftOperand = convert(expression.getLeftOperand());
      Expression rightOperand = convert(expression.getRightOperand());
      BinaryOperator operator = JdtUtils.getBinaryOperator(expression.getOperator());
      BinaryExpression binaryExpression =
          new BinaryExpression(
              JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()),
              leftOperand,
              operator,
              rightOperand);
      for (Object object : expression.extendedOperands()) {
        org.eclipse.jdt.core.dom.Expression extendedOperand =
            (org.eclipse.jdt.core.dom.Expression) object;
        binaryExpression =
            new BinaryExpression(
                JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()),
                binaryExpression,
                operator,
                convert(extendedOperand));
      }
      return binaryExpression;
    }

    /**
     * Returns a qualifier for a method invocation that doesn't have one, specifically,
     * instanceMethod() will return a resolved qualifier that may refer to "this" or to
     * the enclosing instances. A staticMethod() will return null.
     */
    private Expression getExplicitQualifier(
        org.eclipse.jdt.core.dom.MethodInvocation methodInvocation) {

      if (methodInvocation.getExpression() == null) {
        // No qualifier specified.
        IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
        if (JdtUtils.isStatic(methodBinding.getModifiers())) {
          return null;
        } else { // Not static so has to be a reference to 'this
          return convertOuterClassReference(
              JdtUtils.findCurrentTypeBinding(methodInvocation),
              methodBinding.getDeclaringClass(),
              false);
        }
      }
      return convert(methodInvocation.getExpression());
    }

    private MethodCall convert(org.eclipse.jdt.core.dom.MethodInvocation methodInvocation) {

      Expression qualifier = getExplicitQualifier(methodInvocation);

      IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
      List<Expression> arguments =
          convertExpressions(
              JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(
                  methodInvocation.arguments()));
      maybePackageVarargs(
          methodBinding,
          JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(methodInvocation.arguments()),
          arguments);
      return MethodCall.createRegularMethodCall(qualifier, methodDescriptor, arguments);
    }

    private MethodCall convert(org.eclipse.jdt.core.dom.SuperMethodInvocation expression) {
      IMethodBinding methodBinding = expression.resolveMethodBinding();

      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
      List<Expression> arguments =
          convertExpressions(
              JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(expression.arguments()));
      maybePackageVarargs(
          methodBinding,
          JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(expression.arguments()),
          arguments);
      if (expression.getQualifier() == null) {
        return MethodCall.createRegularMethodCall(
            new SuperReference(currentType.getSuperTypeDescriptor()), methodDescriptor, arguments);
      } else {
        // OuterClass.super.fun() is transpiled to
        // SuperClassOfOuterClass.prototype.fun.call(OuterClass.this);
        return MethodCall.createCallMethodCall(
            convertOuterClassReference(
                JdtUtils.findCurrentTypeBinding(expression),
                expression.getQualifier().resolveTypeBinding(),
                true),
            methodDescriptor,
            arguments);
      }
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
      ArrayTypeDescriptor varargsTypeDescriptor =
          (ArrayTypeDescriptor)
              JdtUtils.createTypeDescriptor(
                  methodBinding.getParameterTypes()[parametersLength - 1]);
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
      Number constantValue = (Number) literal.resolveConstantExpressionValue();
      if (constantValue instanceof Float) {
        constantValue = constantValue.doubleValue();
      }
      return new NumberLiteral(
          JdtUtils.createTypeDescriptor(literal.resolveTypeBinding()), constantValue);
    }

    private ParenthesizedExpression convert(
        org.eclipse.jdt.core.dom.ParenthesizedExpression expression) {
      return new ParenthesizedExpression(convert(expression.getExpression()));
    }

    private PostfixExpression convert(org.eclipse.jdt.core.dom.PostfixExpression expression) {
      return new PostfixExpression(
          JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()),
          convert(expression.getOperand()),
          JdtUtils.getPostfixOperator(expression.getOperator()));
    }

    private PrefixExpression convert(org.eclipse.jdt.core.dom.PrefixExpression expression) {
      return new PrefixExpression(
          JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()),
          convert(expression.getOperand()),
          JdtUtils.getPrefixOperator(expression.getOperator()));
    }

    private Expression convert(org.eclipse.jdt.core.dom.QualifiedName expression) {
      IBinding binding = expression.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          return new FieldAccess(
              convert(expression.getQualifier()), JdtUtils.createFieldDescriptor(variableBinding));
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
      IMethodBinding currentMethodBinding = JdtUtils.findCurrentMethodBinding(statement);
      Preconditions.checkNotNull(currentMethodBinding);
      Expression expression =
          statement.getExpression() == null ? null : convert(statement.getExpression());
      TypeDescriptor returnTypeDescriptor =
          JdtUtils.createTypeDescriptor(currentMethodBinding.getReturnType());
      return new ReturnStatement(expression, returnTypeDescriptor);
    }

    /**
     * Finds and returns the descriptor of the type that encloses the variable referenced by the
     * provided binding.
     */
    private TypeDescriptor findEnclosingTypeDescriptor(IVariableBinding variableBinding) {
      // The binding is for a local variable in a static or instance block. JDT does not allow for
      // retrieving the enclosing class. Answer the question from information we've been gathering
      // while processing the compilation unit.
      JavaType javaType = enclosingTypeByVariable.get(variableByJdtBinding.get(variableBinding));
      if (javaType != null) {
        return javaType.getDescriptor();
      }
      // The binding is a simple field and JDT provides direct knowledge of the declaring class.
      if (variableBinding.getDeclaringClass() != null) {
        return JdtUtils.createTypeDescriptor(variableBinding.getDeclaringClass());
      }
      // The binding is a local variable or parameter in method. JDT provides an indirect path to
      // the enclosing class.
      if (variableBinding.getDeclaringMethod() != null) {
        return JdtUtils.createTypeDescriptor(
            variableBinding.getDeclaringMethod().getDeclaringClass());
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
          FieldDescriptor fieldDescriptor = JdtUtils.createFieldDescriptor(variableBinding);
          if (!fieldDescriptor.isStatic()
              && !fieldDescriptor
                  .getEnclosingClassTypeDescriptor()
                  .equals(currentType.getDescriptor())) {
            return new FieldAccess(
                convertOuterClassReference(
                    JdtUtils.findCurrentTypeBinding(expression),
                    variableBinding.getDeclaringClass(),
                    false),
                fieldDescriptor);
          } else {
            return new FieldAccess(null, fieldDescriptor);
          }
        } else {
          // It refers to a local variable or parameter in a method or block.
          Variable variable = variableByJdtBinding.get(variableBinding);
          Preconditions.checkNotNull(variable);
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
     * Convert A.this to corresponding field access knowing A's type is {@code outerTypeBinding}.
     * <p>
     * In the case of outside a constructor,
     * if A is the direct enclosing class, A.this => this.f_$outer_this.
     * if A is the enclosing class of the direct enclosing class,
     * A.this => this.f_$outer_this$.f_$outer_this.
     * <p>
     * In the case of inside a constructor, the above two cases should be translated to
     * $outer_this and $outer_this.f_outer$, $outer_this is the added parameter of the constructor.
     * <p>
     * In the context where this function is called, {@code typeBinding} may be concretely resolved,
     * or it may be inferred by method call or field access. In the first case, it does a 'strict'
     * check, meaning that {@typeBinding} is the exact type that should be found in the type stack.
     * In the second case, it does a 'non-strict' check, meaning that {@typeBinding} is or is a
     * super type of one type in the stack.
     */
    private Expression convertOuterClassReference(
        ITypeBinding currentTypeBinding, ITypeBinding outerTypeBinding, boolean strict) {
      Expression qualifier = new ThisReference(currentType.getDescriptor());
      ITypeBinding innerTypeBinding = currentTypeBinding;
      if (JdtUtils.createTypeDescriptor(innerTypeBinding) != currentType.getDescriptor()) {
        // currentType is a lambda type.
        qualifier =
            new FieldAccess(
                qualifier,
                AstUtils.getFieldDescriptorForEnclosingInstance(
                    currentType.getDescriptor(), currentType.getEnclosingTypeDescriptor()));
      }
      while (innerTypeBinding.getDeclaringClass() != null) {
        boolean found =
            strict
                ? JdtUtils.areSameErasedType(innerTypeBinding, outerTypeBinding)
                : JdtUtils.isSubType(innerTypeBinding, outerTypeBinding);
        if (found) {
          break;
        }

        TypeDescriptor enclosingTypeDescriptor = JdtUtils.createTypeDescriptor(innerTypeBinding);
        TypeDescriptor fieldTypeDescriptor =
            JdtUtils.createTypeDescriptor(innerTypeBinding.getDeclaringClass());

        qualifier =
            new FieldAccess(
                qualifier,
                AstUtils.getFieldDescriptorForEnclosingInstance(
                    enclosingTypeDescriptor, fieldTypeDescriptor));
        innerTypeBinding = innerTypeBinding.getDeclaringClass();
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
          AstUtils.getFieldDescriptorForCapture(currentType.getDescriptor(), variable);
      return new FieldAccess(new ThisReference(currentType.getDescriptor()), fieldDescriptor);
    }

    private Variable convert(org.eclipse.jdt.core.dom.SingleVariableDeclaration node) {
      String name = node.getName().getFullyQualifiedName();
      TypeDescriptor typeDescriptor =
          node.getType() instanceof org.eclipse.jdt.core.dom.UnionType
              ? convert((org.eclipse.jdt.core.dom.UnionType) node.getType())
              : JdtUtils.createTypeDescriptor(node.getType().resolveBinding());
      Variable variable = new Variable(name, typeDescriptor, false, false);
      variableByJdtBinding.put(node.resolveBinding(), variable);
      recordEnclosingType(variable, currentType);
      return variable;
    }

    private StringLiteral convert(org.eclipse.jdt.core.dom.StringLiteral literal) {
      return new StringLiteral(literal.getEscapedValue());
    }

    private SwitchCase convert(org.eclipse.jdt.core.dom.SwitchCase statement) {
      return statement.isDefault()
          ? new SwitchCase()
          : new SwitchCase(convert(statement.getExpression()));
    }

    private SwitchStatement convert(org.eclipse.jdt.core.dom.SwitchStatement statement) {
      return new SwitchStatement(
          convert(statement.getExpression()),
          convertStatements(
              JdtUtils.<org.eclipse.jdt.core.dom.Statement>asTypedList(statement.statements())));
    }

    private SynchronizedStatement convert(
        org.eclipse.jdt.core.dom.SynchronizedStatement statement) {
      return new SynchronizedStatement(
          convert(statement.getExpression()), convert(statement.getBody()));
    }

    private ExpressionStatement convert(
        org.eclipse.jdt.core.dom.SuperConstructorInvocation expression) {
      IMethodBinding superConstructorBinding = expression.resolveConstructorBinding();
      ITypeBinding superclassBinding = superConstructorBinding.getDeclaringClass();
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(superConstructorBinding);
      List<Expression> arguments =
          convertExpressions(
              JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(expression.arguments()));
      maybePackageVarargs(
          superConstructorBinding,
          JdtUtils.<org.eclipse.jdt.core.dom.Expression>asTypedList(expression.arguments()),
          arguments);
      Expression qualifier =
          expression.getExpression() == null ? null : convert(expression.getExpression());
      // super() call to an inner class without explicit qualifier, find the enclosing instance.
      if (qualifier == null && JdtUtils.isInstanceNestedClass(superclassBinding)) {
        qualifier =
            convertOuterClassReference(
                JdtUtils.findCurrentTypeBinding(expression),
                superclassBinding.getDeclaringClass(),
                false);
      }
      MethodCall superCall =
          MethodCall.createRegularMethodCall(qualifier, methodDescriptor, arguments);
      return new ExpressionStatement(superCall);
    }

    private Expression convert(org.eclipse.jdt.core.dom.ThisExpression expression) {
      return convertOuterClassReference(
          JdtUtils.findCurrentTypeBinding(expression), expression.resolveTypeBinding(), true);
    }

    private Expression convert(org.eclipse.jdt.core.dom.TypeLiteral literal) {
      ITypeBinding typeBinding = literal.getType().resolveBinding();

      TypeDescriptor literalTypeDescriptor = JdtUtils.createTypeDescriptor(typeBinding);
      TypeDescriptor javaLangClassTypeDescriptor =
          JdtUtils.createTypeDescriptor(literal.resolveTypeBinding());
      if (literalTypeDescriptor.isNative()) {
        // class literal of native js type returns JavaScriptObject.class
        return convertRegularTypeLiteral(
            TypeDescriptors.BootstrapType.JAVA_SCRIPT_OBJECT.getDescriptor(),
            javaLangClassTypeDescriptor);
      }
      if (literalTypeDescriptor.isArray()
          && literalTypeDescriptor.getLeafTypeDescriptor().isNative()) {
        // class literal of native js type array returns Object[].class
        return convertArrayTypeLiteral(
            TypeDescriptors.get().javaLangObject.getForArray(1), javaLangClassTypeDescriptor);
      }
      if (typeBinding.getDimensions() == 0) {
        return convertRegularTypeLiteral(literalTypeDescriptor, javaLangClassTypeDescriptor);
      }
      return convertArrayTypeLiteral(literalTypeDescriptor, javaLangClassTypeDescriptor);
    }

    private Expression convertRegularTypeLiteral(
        TypeDescriptor literalTypeDescriptor, TypeDescriptor javaLangClassTypeDescriptor) {
      // <ClassLiteralClass>.$getClass()
      MethodDescriptor classMethodDescriptor =
          MethodDescriptorBuilder.fromDefault()
              .isRaw(true)
              .isStatic(true)
              .enclosingClassTypeDescriptor(literalTypeDescriptor)
              .methodName("$getClass")
              .returnTypeDescriptor(javaLangClassTypeDescriptor)
              .build();
      return MethodCall.createRegularMethodCall(null, classMethodDescriptor);
    }

    private Expression convertArrayTypeLiteral(
        TypeDescriptor literalTypeDescriptor, TypeDescriptor javaLangClassTypeDescriptor) {
      MethodDescriptor classMethodDescriptor =
          MethodDescriptorBuilder.fromDefault()
              .isRaw(true)
              .isStatic(true)
              .enclosingClassTypeDescriptor(literalTypeDescriptor.getLeafTypeDescriptor())
              .methodName("$getClass")
              .returnTypeDescriptor(javaLangClassTypeDescriptor)
              .build();

      MethodDescriptor forArrayMethodDescriptor =
          MethodDescriptorBuilder.fromDefault()
              .isRaw(true)
              .enclosingClassTypeDescriptor(javaLangClassTypeDescriptor)
              .methodName("$forArray")
              .parameterTypeDescriptors(Lists.newArrayList(TypeDescriptors.get().primitiveInt))
              .returnTypeDescriptor(javaLangClassTypeDescriptor)
              .build();

      // <ClassLiteralClass>.$getClass().forArray(<dimensions>)
      return MethodCall.createRegularMethodCall(
          MethodCall.createRegularMethodCall(
              null, classMethodDescriptor, new ArrayList<Expression>()),
          forArrayMethodDescriptor,
          ImmutableList.<Expression>of(
              new NumberLiteral(
                  TypeDescriptors.get().primitiveInt, literalTypeDescriptor.getDimensions())));
    }

    private ThrowStatement convert(org.eclipse.jdt.core.dom.ThrowStatement statement) {
      return new ThrowStatement(convert(statement.getExpression()));
    }

    private TryStatement convert(org.eclipse.jdt.core.dom.TryStatement statement) {
      List<VariableDeclarationExpression> resources = new ArrayList<>();
      for (Object expression : statement.resources()) {
        resources.add(convert((org.eclipse.jdt.core.dom.VariableDeclarationExpression) expression));
      }
      Block body = convert(statement.getBody());
      List<CatchClause> catchClauses = new ArrayList<>();
      for (Object catchClause : statement.catchClauses()) {
        catchClauses.add(convert((org.eclipse.jdt.core.dom.CatchClause) catchClause));
      }
      Block finallyBlock = statement.getFinally() == null ? null : convert(statement.getFinally());
      return new TryStatement(resources, body, catchClauses, finallyBlock);
    }

    private UnionTypeDescriptor convert(org.eclipse.jdt.core.dom.UnionType unionType) {
      List<TypeDescriptor> types = new ArrayList<>();
      for (Object object : unionType.types()) {
        org.eclipse.jdt.core.dom.Type type = (org.eclipse.jdt.core.dom.Type) object;
        types.add(JdtUtils.createTypeDescriptor(type.resolveBinding()));
      }
      return UnionTypeDescriptor.create(types);
    }

    private VariableDeclarationFragment convert(
        org.eclipse.jdt.core.dom.VariableDeclarationFragment variableDeclarationFragment) {
      IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();
      Variable variable = JdtUtils.createVariable(variableBinding);
      recordEnclosingType(variable, currentType);
      Expression initializer =
          variableDeclarationFragment.getInitializer() == null
              ? null
              : convert(variableDeclarationFragment.getInitializer());
      variableByJdtBinding.put(variableBinding, variable);
      return new VariableDeclarationFragment(variable, initializer);
    }

    private Variable convert(org.eclipse.jdt.core.dom.VariableDeclaration variableDeclaration) {
      if (variableDeclaration instanceof org.eclipse.jdt.core.dom.SingleVariableDeclaration) {
        return convert((org.eclipse.jdt.core.dom.SingleVariableDeclaration) variableDeclaration);
      } else {
        return convert((org.eclipse.jdt.core.dom.VariableDeclarationFragment) variableDeclaration)
            .getVariable();
      }
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

    /**
     * Calculates or returns a consistent binary name for the given lambda method source name +
     * unique key.
     */
    private String getLambdaBinaryName(
        String enclosingTypeName, String lambdaSourceName, String lambdaKey) {
      String fullyQualifiedUniqueKey = enclosingTypeName + lambdaKey;
      if (!lambdaBinaryNameByKey.containsKey(fullyQualifiedUniqueKey)) {
        // Construct the binary name as the unique index number + the source name, so that it is
        // both unique and somewhat readable.
        String lambdaBinaryName = lambdaBinaryNameByKey.size() + lambdaSourceName;
        lambdaBinaryNameByKey.put(fullyQualifiedUniqueKey, lambdaBinaryName);
      }
      return lambdaBinaryNameByKey.get(fullyQualifiedUniqueKey);
    }

    private JavaType createJavaType(ITypeBinding typeBinding) {
      if (typeBinding == null) {
        return null;
      }
      ITypeBinding superclassBinding = typeBinding.getSuperclass();
      Kind kind = JdtUtils.getKindFromTypeBinding(typeBinding);
      Visibility visibility = JdtUtils.getVisibility(typeBinding.getModifiers());
      TypeDescriptor typeDescriptor = JdtUtils.createTypeDescriptor(typeBinding);
      JavaType type =
          typeBinding.isAnonymous()
              ? new AnonymousJavaType(kind, visibility, typeDescriptor)
              : new JavaType(kind, visibility, typeDescriptor);

      type.setEnclosingTypeDescriptor(
          JdtUtils.createTypeDescriptor(typeBinding.getDeclaringClass()));

      TypeDescriptor superTypeDescriptor = JdtUtils.createTypeDescriptor(superclassBinding);
      type.setSuperTypeDescriptor(superTypeDescriptor);
      for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
        type.addSuperInterfaceDescriptor(JdtUtils.createTypeDescriptor(superInterface));
      }
      type.setLocal(typeBinding.isLocal());
      type.setStatic(JdtUtils.isStatic(typeBinding.getModifiers()));
      type.setAbstract(JdtUtils.isAbstract(typeBinding.getModifiers()));
      return type;
    }
  }

  private CompilationUnit buildCompilationUnit(
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
    ASTConverter converter = new ASTConverter();
    return converter.convert(sourceFilePath, compilationUnit);
  }

  public static List<CompilationUnit> build(
      Map<String, org.eclipse.jdt.core.dom.CompilationUnit> jdtUnitsByFilePath) {
    CompilationUnitBuilder compilationUnitBuilder = new CompilationUnitBuilder();

    List<CompilationUnit> compilationUnits = new ArrayList<>();
    for (Entry<String, org.eclipse.jdt.core.dom.CompilationUnit> entry :
        jdtUnitsByFilePath.entrySet()) {
      compilationUnits.add(
          compilationUnitBuilder.buildCompilationUnit(entry.getKey(), entry.getValue()));
    }
    return compilationUnits;
  }

  private CompilationUnitBuilder() {}
}
