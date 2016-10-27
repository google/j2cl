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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.toCollection;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AnonymousType;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLiteral;
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
import com.google.j2cl.ast.ConditionalExpression;
import com.google.j2cl.ast.ContinueStatement;
import com.google.j2cl.ast.DoWhileStatement;
import com.google.j2cl.ast.EmptyStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.LabeledStatement;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.SwitchCase;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.SynchronizedStatement;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.Type.Kind;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.UnaryExpression;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.ast.WhileStatement;
import com.google.j2cl.ast.sourcemap.SourcePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

/**
 * Creates a J2CL Java AST from the AST provided by JDT.
 */
public class CompilationUnitBuilder {
  private class ASTConverter {
    private PackageInfoCache packageInfoCache = PackageInfoCache.get();
    private Map<IVariableBinding, Variable> variableByJdtBinding = new HashMap<>();
    private Map<Variable, Type> enclosingTypeByVariable = new HashMap<>();
    private Multimap<TypeDescriptor, Variable> capturesByTypeDescriptor =
        LinkedHashMultimap.create();
    private List<Type> typeStack = new ArrayList<>();
    private Type currentType = null;

    private String currentSourceFile;
    private org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit;
    private CompilationUnit j2clCompilationUnit;
    private int lambdaCounter;

    private void pushType(Type type) {
      checkArgument(!type.getDescriptor().isArray());
      checkArgument(!type.getDescriptor().isUnion());

      typeStack.add(type);
      currentType = type;
    }

    private void popType() {
      typeStack.remove(typeStack.size() - 1);
      currentType = typeStack.isEmpty() ? null : typeStack.get(typeStack.size() - 1);
    }

    @SuppressWarnings({"cast", "unchecked"})
    private CompilationUnit convert(
        String sourceFilePath,
        org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit,
        Iterable<ITypeBinding> wellKnownTypeBindings) {
      JdtUtils.initTypeDescriptors(jdtCompilationUnit.getAST(), wellKnownTypeBindings);
      this.jdtCompilationUnit = jdtCompilationUnit;

      currentSourceFile = sourceFilePath;
      String packageName = JdtUtils.getCompilationUnitPackageName(jdtCompilationUnit);
      j2clCompilationUnit = new CompilationUnit(sourceFilePath, packageName);
      // Records information about package-info files supplied as source code.
      if (currentSourceFile.endsWith("package-info.java")
          && jdtCompilationUnit.getPackage() != null) {
        packageInfoCache.setInfo(
            PackageInfoCache.SOURCE_CLASS_PATH_ENTRY,
            packageName,
            jdtCompilationUnit.getPackage().annotations());
      }
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
          convertAndAddType(
              JdtUtils.isInStaticContext(typeDeclaration),
              typeDeclaration.resolveBinding(),
              JdtUtils.asTypedList(typeDeclaration.bodyDeclarations()));
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
      Type enumType =
          convertAndAddType(
              JdtUtils.isInStaticContext(enumDeclaration),
              enumDeclaration.resolveBinding(),
              JdtUtils.asTypedList(enumDeclaration.bodyDeclarations()));

      for (EnumConstantDeclaration enumConstantDeclaration :
          JdtUtils.<EnumConstantDeclaration>asTypedList(enumDeclaration.enumConstants())) {
        if (enumConstantDeclaration.getAnonymousClassDeclaration() != null) {
          convertAnonymousClassDeclaration(
              enumConstantDeclaration.getAnonymousClassDeclaration(),
              enumConstantDeclaration.resolveConstructorBinding());
        }
      }

      // this is an Enum.
      checkState(enumType.isEnum());
      enumType.addFields(
          0,
          JdtUtils.<EnumConstantDeclaration>asTypedList(enumDeclaration.enumConstants())
              .stream()
              .map(this::convert)
              .collect(toImmutableList()));
      EnumMethodsCreator.applyTo(enumType);
    }

    private Type convertAndAddType(
        boolean inStaticContext, ITypeBinding typeBinding, List<BodyDeclaration> bodyDeclarations) {
      Type type = createType(typeBinding);
      pushType(type);
      j2clCompilationUnit.addType(type);
      TypeDescriptor currentTypeDescriptor = TypeDescriptors.toNullable(type.getDescriptor());
      ITypeBinding superclassBinding = typeBinding.getSuperclass();
      if (superclassBinding != null) {
        capturesByTypeDescriptor.putAll(
            currentTypeDescriptor,
            capturesByTypeDescriptor.get(
                TypeDescriptors.toNullable(type.getSuperTypeDescriptor())));
      }
      for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
        if (bodyDeclaration instanceof FieldDeclaration) {
          FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclaration;
          type.addFields(convert(fieldDeclaration));
        } else if (bodyDeclaration instanceof MethodDeclaration) {
          MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
          type.addMethod(convert(methodDeclaration));
        } else if (bodyDeclaration instanceof Initializer) {
          Initializer initializer = (Initializer) bodyDeclaration;
          Block block = convert(initializer.getBody());
          if (JdtUtils.isStatic(initializer)) {
            type.addStaticInitializerBlock(block);
          } else {
            type.addInstanceInitializerBlock(block);
          }
        } else if (bodyDeclaration instanceof AbstractTypeDeclaration) {
          // Nested class
          AbstractTypeDeclaration nestedTypeDeclaration = (AbstractTypeDeclaration) bodyDeclaration;
          convert(nestedTypeDeclaration);
        } else {
          throw new RuntimeException(
              "Need to implement translation for BodyDeclaration type: "
                  + bodyDeclaration.getClass().getName()
                  + " file triggering this: "
                  + currentSourceFile);
        }
      }

      for (Variable capturedVariable : capturesByTypeDescriptor.get(currentTypeDescriptor)) {
        FieldDescriptor fieldDescriptor =
            AstUtils.getFieldDescriptorForCapture(currentTypeDescriptor, capturedVariable);
        type.addField(
            Field.Builder.from(fieldDescriptor).setCapturedVariable(capturedVariable).build());
      }
      if (!inStaticContext && JdtUtils.isInstanceNestedClass(typeBinding)) {
        // add field for enclosing instance.
        type.addField(
            new Field(
                AstUtils.getFieldDescriptorForEnclosingInstance(
                    currentTypeDescriptor, type.getEnclosingTypeDescriptor())));
      }

      // Resolve default methods
      DefaultMethodsResolver.resolve(typeBinding, type);

      // Add dispatch methods for package private methods.
      PackagePrivateMethodsDispatcher.create(typeBinding, type);

      // Add bridge methods.
      BridgeMethodsCreator.create(typeBinding, type);

      // Add bridge methods for JsMethods
      JsBridgeMethodsCreator.create(typeBinding, type);
      popType();
      return type;
    }

    private Field convert(EnumConstantDeclaration enumConstantDeclaration) {
      Expression initializer =
          NewInstance.Builder.from(
                  JdtUtils.createMethodDescriptor(
                      enumConstantDeclaration.resolveConstructorBinding()))
              .setArguments(
                  convertExpressions(JdtUtils.asTypedList(enumConstantDeclaration.arguments())))
              .build();

      FieldDescriptor fieldDescriptor =
          JdtUtils.createFieldDescriptor(enumConstantDeclaration.resolveVariable());
      // Enum fields are always non-nullable.
      fieldDescriptor =
          FieldDescriptor.Builder.from(fieldDescriptor)
              .setTypeDescriptor(TypeDescriptors.toNonNullable(fieldDescriptor.getTypeDescriptor()))
              .build();
      return Field.Builder.from(fieldDescriptor)
          .setInitializer(initializer)
          .setIsEnumField(true)
          .setSourcePosition(getSourcePosition(enumConstantDeclaration))
          .build();
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
            Field.Builder.from(JdtUtils.createFieldDescriptor(variableBinding))
                .setInitializer(initializer)
                .setSourcePosition(getSourcePosition(fieldDeclaration))
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
        return StringLiteral.fromPlainText((String) constantValue);
      }
      if (constantValue instanceof Character) {
        return new CharacterLiteral((char) constantValue);
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
          methodDeclaration.getBody() == null ? new Block() : convert(methodDeclaration.getBody());

      IMethodBinding methodBinding = methodDeclaration.resolveBinding();
      Method.Builder methodBuilder =
          Method.newBuilder()
              .setMethodDescriptor(JdtUtils.createMethodDescriptor(methodBinding))
              .setParameters(parameters)
              .addStatements(body.getStatements())
              .setIsAbstract(JdtUtils.isAbstract(methodBinding))
              .setIsOverride(JdtUtils.isJsOverride(methodBinding))
              .setIsFinal(JdtUtils.isFinal(methodBinding));
      for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
        methodBuilder.setParameterOptional(i, JsInteropUtils.isJsOptional(methodBinding, i));
      }
      return methodBuilder.build();
    }

    private ArrayAccess convert(org.eclipse.jdt.core.dom.ArrayAccess expression) {
      return new ArrayAccess(convert(expression.getArray()), convert(expression.getIndex()));
    }

    private NewArray convert(org.eclipse.jdt.core.dom.ArrayCreation expression) {
      ArrayType arrayType = expression.getType();

      List<Expression> dimensionExpressions =
          convertExpressions(JdtUtils.asTypedList(expression.dimensions()));
      // If some dimensions are not initialized then make that explicit.
      while (dimensionExpressions.size() < arrayType.getDimensions()) {
        dimensionExpressions.add(NullLiteral.NULL);
      }

      ArrayLiteral arrayLiteral =
          expression.getInitializer() == null ? null : convert(expression.getInitializer());

      TypeDescriptor typeDescriptor =
          JdtUtils.createTypeDescriptor(expression.resolveTypeBinding());
      return new NewArray(typeDescriptor, dimensionExpressions, arrayLiteral);
    }

    @SuppressWarnings("unchecked")
    private ArrayLiteral convert(org.eclipse.jdt.core.dom.ArrayInitializer expression) {
      return new ArrayLiteral(
          JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()),
          convertExpressions(expression.expressions()));
    }

    private BooleanLiteral convert(org.eclipse.jdt.core.dom.BooleanLiteral literal) {
      return literal.booleanValue() ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
    }

    private CastExpression convert(org.eclipse.jdt.core.dom.CastExpression expression) {
      TypeDescriptor castTypeDescriptor =
          JdtUtils.createTypeDescriptor(expression.getType().resolveBinding());
      return CastExpression.newBuilder()
          .setExpression(convert(expression.getExpression()))
          .setCastTypeDescriptor(castTypeDescriptor)
          .build();
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
          constructorBinding, JdtUtils.asTypedList(expression.arguments()), arguments);

      boolean isAnonymousClassCreation = expression.getAnonymousClassDeclaration() != null;
      if (isAnonymousClassCreation) {
        return convertAnonymousClassCreation(expression, constructorMethodDescriptor, arguments);
      } else {
        return convertRegularClassCreation(expression, constructorMethodDescriptor, arguments);
      }
    }

    private AnonymousType convertAnonymousClassDeclaration(
        AnonymousClassDeclaration typeDeclaration, IMethodBinding constructorBinding) {

      // Anonymous classes might have default synthesized constructors take parameters.
      // {@code constructorImplicitParameterTypeDescriptors} are the types for the aforementioned
      // parameters.
      TypeDescriptor[] constructorImplicitParameterTypeDescriptors =
          getParameterTypeDescriptors(constructorBinding.getParameterTypes());

      ITypeBinding typeBinding = typeDeclaration.resolveBinding();
      AnonymousType anonymousType =
          (AnonymousType)
              convertAndAddType(
                  JdtUtils.isInStaticContext(typeDeclaration),
                  typeBinding,
                  JdtUtils.asTypedList(typeDeclaration.bodyDeclarations()));
      anonymousType.addConstructorParameterTypeDescriptors(
          Arrays.asList(constructorImplicitParameterTypeDescriptors));

      // Record the types of the superclass constructor's parameters, if they exist.
      ITypeBinding superTypeBinding = typeBinding.getSuperclass();
      for (IMethodBinding methodBinding : superTypeBinding.getDeclaredMethods()) {
        if (methodBinding.isConstructor() && constructorBinding.isSubsignature(methodBinding)) {
          // Note that we get the types from the declaration, not the binding, because we need the
          // declared type to figure out how to reference the method.
          TypeDescriptor[] superConstructorImplicitParameterTypeDescriptors =
              getParameterTypeDescriptors(methodBinding.getMethodDeclaration().getParameterTypes());
          anonymousType.addSuperConstructorParameterTypeDescriptors(
              Arrays.asList(superConstructorImplicitParameterTypeDescriptors));
        }
      }

      return anonymousType;
    }

    private TypeDescriptor[] getParameterTypeDescriptors(ITypeBinding[] parameterTypes) {
      return Stream.of(parameterTypes)
          .map(JdtUtils::createTypeDescriptor)
          .toArray(TypeDescriptor[]::new);
    }

    private Expression convertAnonymousClassCreation(
        org.eclipse.jdt.core.dom.ClassInstanceCreation expression,
        MethodDescriptor constructorMethodDescriptor,
        List<Expression> arguments) {
      IMethodBinding constructorBinding = expression.resolveConstructorBinding();
      ITypeBinding newInstanceTypeBinding = constructorBinding.getDeclaringClass();

      AnonymousType anonymousClass =
          convertAnonymousClassDeclaration(
              checkNotNull(expression.getAnonymousClassDeclaration()), constructorBinding);

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
      return NewInstance.Builder.from(constructorMethodDescriptor)
          .setQualifier(newInstanceQualifier)
          .setArguments(arguments)
          .setIsAnonymousClassCreation(true)
          .build();
    }

    private Expression convertRegularClassCreation(
        org.eclipse.jdt.core.dom.ClassInstanceCreation expression,
        MethodDescriptor constructorMethodDescriptor,
        List<Expression> arguments) {
      IMethodBinding constructorBinding = expression.resolveConstructorBinding();
      ITypeBinding newInstanceTypeBinding = constructorBinding.getDeclaringClass();
      Expression qualifier =
          expression.getExpression() == null ? null : convert(expression.getExpression());
      boolean hasQualifier =
          JdtUtils.isInstanceMemberClass(newInstanceTypeBinding)
              || (newInstanceTypeBinding.isAnonymous() && !JdtUtils.isInStaticContext(expression))
              || (newInstanceTypeBinding.isLocal()
                  && !JdtUtils.isStatic(
                      newInstanceTypeBinding.getTypeDeclaration().getDeclaringMember()));
      checkArgument(
          qualifier == null || hasQualifier,
          "NewInstance of non nested class should have no qualifier.");

      // Resolve the qualifier of NewInstance that creates an instance of a nested class.
      // Implicit 'this' doesn't always refer to 'this', it may refer to any enclosing instances.
      qualifier =
          hasQualifier && qualifier == null
              // find the enclosing instance in non-strict mode, which means
              // for example,
              // class A {
              //   class B {}
              //   class C extends class A {
              //     // The qualifier of new B() should be C.this, not A.this.
              //     public void test() { new B(); }
              //   }
              // }
              ? convertOuterClassReference(
                  JdtUtils.findCurrentTypeBinding(expression),
                  newInstanceTypeBinding.getDeclaringClass(),
                  false)
              : qualifier;

      return NewInstance.Builder.from(constructorMethodDescriptor)
          .setQualifier(qualifier)
          .setArguments(arguments)
          .build();
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
        case ASTNode.EXPRESSION_METHOD_REFERENCE:
        case ASTNode.CREATION_REFERENCE:
          // TODO(stalcup): Implement method references properly
          j2clExpression = NullLiteral.NULL;
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
      return expressions.stream().map(this::convert).collect(toCollection(ArrayList::new));
    }

    private List<Statement> convertStatements(List<org.eclipse.jdt.core.dom.Statement> statements) {
      return statements.stream().map(this::convert).collect(toCollection(ArrayList::new));
    }

    private ConditionalExpression convert(
        org.eclipse.jdt.core.dom.ConditionalExpression jdtConditionalExpression) {
      Expression conditionExpression = convert(jdtConditionalExpression.getExpression());
      Expression trueExpression = convert(jdtConditionalExpression.getThenExpression());
      Expression falseExpression = convert(jdtConditionalExpression.getElseExpression());

      TypeDescriptor conditionalTypeDescriptor =
          JdtUtils.createTypeDescriptor(jdtConditionalExpression.resolveTypeBinding());

      return new ConditionalExpression(
          conditionalTypeDescriptor, conditionExpression, trueExpression, falseExpression);
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

    public SourcePosition getSourcePosition(ASTNode node) {
      int startLineNumber = jdtCompilationUnit.getLineNumber(node.getStartPosition()) - 1;
      int startColumnNumber = jdtCompilationUnit.getColumnNumber(node.getStartPosition());
      int endPositionCharacterIndex = node.getStartPosition() + node.getLength();
      int endLineNumber = jdtCompilationUnit.getLineNumber(endPositionCharacterIndex) - 1;
      int endColumnNumber = jdtCompilationUnit.getColumnNumber(endPositionCharacterIndex);
      return new SourcePosition(startLineNumber, startColumnNumber, endLineNumber, endColumnNumber);
    }

    private Statement convert(org.eclipse.jdt.core.dom.Statement statement) {
      SourcePosition position = getSourcePosition(statement);
      Statement j2clStatement = convertStatement(statement);
      if (j2clStatement != null) {
        j2clStatement.setSourcePosition(position);
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
      List<Expression> initializers = convertExpressions(statement.initializers());

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
          Variable.newBuilder()
              .setName("$array")
              .setTypeDescriptor(JdtUtils.createTypeDescriptor(expressionTypeBinding))
              .setIsFinal(true)
              .build();
      VariableDeclarationFragment arrayVariableDeclarationFragment =
          new VariableDeclarationFragment(arrayVariable, convert(statement.getExpression()));

      // int index = 0;
      Variable indexVariable =
          Variable.newBuilder()
              .setName("$index")
              .setTypeDescriptor(TypeDescriptors.get().primitiveInt)
              .build();
      VariableDeclarationFragment indexVariableDeclarationFragment =
          new VariableDeclarationFragment(
              indexVariable, new NumberLiteral(TypeDescriptors.get().primitiveInt, 0));

      // $index < $array.length
      Expression condition =
          BinaryExpression.newBuilder()
              .setTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
              .setLeftOperand(indexVariable.getReference())
              .setOperator(BinaryOperator.LESS)
              .setRightOperand(
                  FieldAccess.Builder.from(AstUtils.ARRAY_LENGTH_FIELD_DESCRIPTION)
                      .setQualifier(arrayVariable.getReference())
                      .build())
              .build();

      ExpressionStatement forVariableDeclarationStatement =
          new VariableDeclarationExpression(
                  new VariableDeclarationFragment(
                      convert(statement.getParameter()),
                      new ArrayAccess(arrayVariable.getReference(), indexVariable.getReference())))
              .makeStatement();

      //  {   T t = $array[$index]; S; }
      Statement bodyStatement = convert(statement.getBody());
      Block body =
          bodyStatement instanceof Block ? (Block) bodyStatement : new Block(bodyStatement);
      body.getStatements().add(0, forVariableDeclarationStatement);

      return new ForStatement(
          condition,
          body,
          Collections.singletonList(
              new VariableDeclarationExpression(
                  arrayVariableDeclarationFragment, indexVariableDeclarationFragment)),
          Collections.singletonList(
              PostfixExpression.newBuilder()
                  .setTypeDescriptor(TypeDescriptors.get().primitiveInt)
                  .setOperand(indexVariable.getReference())
                  .setOperator(PostfixOperator.INCREMENT)
                  .build()));
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
          Variable.newBuilder()
              .setName("$iterator")
              .setTypeDescriptor(
                  JdtUtils.createTypeDescriptor(iteratorMethodBinding.getReturnType()))
              .setIsFinal(true)
              .build();

      VariableDeclarationFragment iteratorDeclaration =
          new VariableDeclarationFragment(
              iteratorVariable,
              MethodCall.Builder.from(JdtUtils.createMethodDescriptor(iteratorMethodBinding))
                  .setQualifier(convert(statement.getExpression()))
                  .build());

      // $iterator.hasNext();
      IMethodBinding hasNextMethodBinding =
          JdtUtils.getMethodBinding(iteratorMethodBinding.getReturnType(), "hasNext");
      Expression condition =
          MethodCall.Builder.from(JdtUtils.createMethodDescriptor(hasNextMethodBinding))
              .setQualifier(iteratorVariable.getReference())
              .build();

      // T v = $iterator.next();
      IMethodBinding nextMethodBinding =
          JdtUtils.getMethodBinding(iteratorMethodBinding.getReturnType(), "next");
      ExpressionStatement forVariableDeclarationStatement =
          new VariableDeclarationExpression(
                  new VariableDeclarationFragment(
                      convert(statement.getParameter()),
                      MethodCall.Builder.from(JdtUtils.createMethodDescriptor(nextMethodBinding))
                          .setQualifier(iteratorVariable.getReference())
                          .build()))
              .makeStatement();

      Statement bodyStatement = convert(statement.getBody());
      Block body =
          bodyStatement instanceof Block ? (Block) bodyStatement : new Block(bodyStatement);
      body.getStatements().add(0, forVariableDeclarationStatement);

      return new ForStatement(
          condition,
          body,
          Collections.singletonList(new VariableDeclarationExpression(iteratorDeclaration)),
          Collections.emptyList());
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

    private FunctionExpression convertLambdaToFunctionExpression(LambdaExpression expression) {
      ITypeBinding lambdaTypeBinding = expression.resolveTypeBinding();

      List<Variable> parameters = new ArrayList<>();
      for (VariableDeclaration parameter :
          JdtUtils.<VariableDeclaration>asTypedList(expression.parameters())) {
        parameters.add(convert(parameter));
      }
      TypeDescriptor returnTypeDescriptor =
          JdtUtils.createTypeDescriptor(expression.resolveMethodBinding().getReturnType());
      Block body = convertLambdaBody(expression.getBody(), returnTypeDescriptor);
      return new FunctionExpression(
          JdtUtils.createTypeDescriptor(lambdaTypeBinding), parameters, body.getStatements());
    }

    // Lambda expression bodies can be either an Expression or a Statement
    private Block convertLambdaBody(ASTNode lambdaBody, TypeDescriptor returnTypeDescriptor) {
      Block body;
      if (lambdaBody.getNodeType() == ASTNode.BLOCK) {
        body = convert((org.eclipse.jdt.core.dom.Block) lambdaBody);
      } else {
        checkArgument(lambdaBody instanceof org.eclipse.jdt.core.dom.Expression);
        Expression lambdaMethodBody = convert((org.eclipse.jdt.core.dom.Expression) lambdaBody);
        Statement statement =
            returnTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveVoid)
                ? lambdaMethodBody.makeStatement()
                : new ReturnStatement(lambdaMethodBody, returnTypeDescriptor);
        statement.setSourcePosition(getSourcePosition(lambdaBody));
        body = new Block(statement);
      }
      return body;
    }

    /**
     * Lambda expression is converted to an inner class: <pre>
     * class Enclosing$lambda$0 implements I {
     *   Enclosing$lambda$0(captures) {// initialize captures}
     *   private T lambda$0(args) {...lambda_expression_with_captures ... }
     *   public T samMethod0(args) { return this.lambda$0(args); }
     * }</pre>
     *
     * <p>And replaces the lambda with {@code new Enclosing$lambda$0(captures)}.
     */
    private Expression convert(org.eclipse.jdt.core.dom.LambdaExpression expression) {
      ITypeBinding functionalInterfaceTypeBinding = expression.resolveTypeBinding();

      TypeDescriptor functionalInterfaceTypeDescriptor =
          JdtUtils.createTypeDescriptor(
              functionalInterfaceTypeBinding.getFunctionalInterfaceMethod().getDeclaringClass());
      // TODO(goktug): use intersection type simple name instead of functional interface type.
      String lambdaSimpleName =
          "$Lambda$" + functionalInterfaceTypeDescriptor.getSimpleName() + "$" + lambdaCounter++;

      // Here we convert JsFunctions to function expressions
      if (functionalInterfaceTypeDescriptor.isJsFunctionInterface()) {
        return convertLambdaToFunctionExpression(expression);
      }

      ITypeBinding enclosingClassTypeBinding = JdtUtils.findCurrentTypeBinding(expression);
      TypeDescriptor enclosingType = JdtUtils.createTypeDescriptor(enclosingClassTypeBinding);
      TypeDescriptor lambdaTypeDescriptor =
          JdtUtils.createLambda(enclosingType, lambdaSimpleName, functionalInterfaceTypeBinding);
      Type lambdaType = new Type(Kind.CLASS, Visibility.PRIVATE, lambdaTypeDescriptor);
      pushType(lambdaType);

      // Construct lambda method and add it to lambda inner class.
      String lambdaMethodBinaryName = "lambda" + lambdaSimpleName;
      Method lambdaMethod =
          createLambdaMethod(lambdaMethodBinaryName, expression, lambdaType.getDescriptor());
      lambdaType.addMethod(lambdaMethod);

      // Construct and add SAM method that delegates to the lambda method.
      Method samMethod =
          JdtUtils.createSamMethod(functionalInterfaceTypeBinding, lambdaMethod.getDescriptor());
      lambdaType.addMethod(samMethod);

      // Add fields for captured local variables.
      for (Variable capturedVariable : capturesByTypeDescriptor.get(lambdaTypeDescriptor)) {
        lambdaType.addField(
            Field.Builder.from(
                    AstUtils.getFieldDescriptorForCapture(lambdaTypeDescriptor, capturedVariable))
                .setCapturedVariable(capturedVariable)
                .build());
      }

      if (!JdtUtils.isInStaticContext(expression)) {
        // Add field for enclosing instance.
        lambdaType.addField(
            new Field(
                AstUtils.getFieldDescriptorForEnclosingInstance(
                    lambdaTypeDescriptor, lambdaType.getEnclosingTypeDescriptor())));
      }

      // Collect all type variables that are in context. Those might come from the functional
      // interface instantiation, from the captures (which are the fields in this synthetic
      // anonymous inner class, or from the lambda body e.g.
      //
      // interface Func<T, U> {
      //   U apply(T t);
      // }
      //
      // static <T> void f() {
      //    List<T>  var = ...
      //    m((String s)-> var.get(0));
      //
      // The lambda class here should be declared
      //
      // class lambdaimplememntation<T> implements Func<String, T> {
      //   List<T> captured$var;
      //   ....
      //   public T apply(String t) {return captured$var.get(0); }
      // }
      //
      // and type variable T in this example comes for the captured variable "var" for type List<T>.
      //
      // Collect all free type variables appearing in the body of the implementation as they will
      // be modelled as type variables for the lambda implementation class.
      final Set<TypeDescriptor> lambdaTypeParameterTypeDescriptors =
          AstUtils.getAllTypeVariables(lambdaType);

      // Add the relevant type parameters to the anonymous inner class that implements the lambda.
      lambdaTypeDescriptor =
          TypeDescriptors.replaceTypeArgumentDescriptors(
              lambdaTypeDescriptor, new ArrayList<>(lambdaTypeParameterTypeDescriptors));

      // Note that the original type descriptor for the lambda was computed above.  However, once
      // we traverse the lambda method we determine the captured variables and need to add their
      // type variables to the lambda TypeDescriptor's type arguments.  This new TypeDescriptor
      // needs to replace the old one throughout the Lambda Type.
      replaceLambdaTypeDescriptor(lambdaType, lambdaTypeDescriptor);

      // Resolve default methods
      DefaultMethodsResolver.resolve(functionalInterfaceTypeBinding, lambdaType);

      // Add lambda class to compilation unit.
      j2clCompilationUnit.addType(lambdaType);
      popType();

      // Replace the lambda with new LambdaClass()
      MethodDescriptor constructorMethodDescriptor =
          AstUtils.createDefaultConstructorDescriptor(lambdaTypeDescriptor, Visibility.PUBLIC);
      // qualifier should not be null if the lambda is nested in another lambda.
      Expression qualifier =
          !JdtUtils.isInStaticContext(expression)
              ? convertOuterClassReference(
                  enclosingClassTypeBinding, enclosingClassTypeBinding, true)
              : null;
      return NewInstance.Builder.from(constructorMethodDescriptor).setQualifier(qualifier).build();
    }

    /**
     * Replace the type descriptor for a given Type. Note that this is only safe for specific cases
     * where we know the type will not occur recursively inside other TypeDescritpors (the synthetic
     * lambda class).
     */
    private void replaceLambdaTypeDescriptor(final Type type, final TypeDescriptor replacement) {
      final TypeDescriptor original = type.getDescriptor();
      checkArgument(original.isNullable() && replacement.isNullable());
      type.accept(
          new AbstractRewriter() {
            @Override
            public Node rewriteTypeDescriptor(TypeDescriptor typeDescriptor) {
              if (typeDescriptor == original) {
                return replacement;
              }
              return typeDescriptor;
            }

            @Override
            public Node rewriteFieldDescriptor(FieldDescriptor node) {
              if (node.getEnclosingClassTypeDescriptor() == original) {
                return FieldDescriptor.Builder.from(node)
                    .setEnclosingClassTypeDescriptor(replacement)
                    .build();
              }
              return node;
            }

            @Override
            public Node rewriteMethodDescriptor(MethodDescriptor node) {
              if (node.getEnclosingClassTypeDescriptor() == original) {
                return MethodDescriptor.Builder.from(node)
                    .setEnclosingClassTypeDescriptor(replacement)
                    .build();
              }
              return node;
            }
          });
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

      Block body = convertLambdaBody(expression.getBody(), returnTypeDescriptor);

      // generate parameters type descriptors.
      List<TypeDescriptor> parameterTypeDescriptors =
          Lists.transform(
              Arrays.asList(methodBinding.getParameterTypes()), JdtUtils::createTypeDescriptor);

      MethodDescriptor methodDescriptor =
          MethodDescriptor.newBuilder()
              .setJsInfo(JsInfo.RAW)
              .setVisibility(Visibility.PRIVATE)
              .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
              .setName(methodName)
              .setParameterTypeDescriptors(parameterTypeDescriptors)
              .setReturnTypeDescriptor(returnTypeDescriptor)
              .build();
      return Method.newBuilder()
          .setMethodDescriptor(methodDescriptor)
          .setParameters(parameters)
          .addStatements(body.getStatements())
          .build();
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
      return BinaryExpression.newBuilder()
          .setTypeDescriptor(JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()))
          .setLeftOperand(leftHandSide)
          .setOperator(operator)
          .setRightOperand(rightHandSide)
          .build();
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
      List<Expression> arguments = convertExpressions(JdtUtils.asTypedList(statement.arguments()));
      maybePackageVarargs(
          constructorBinding, JdtUtils.asTypedList(statement.arguments()), arguments);
      return MethodCall.Builder.from(methodDescriptor)
          .setArguments(arguments)
          .build()
          .makeStatement();
    }

    private Statement convert(org.eclipse.jdt.core.dom.ExpressionStatement statement) {
      return convert(statement.getExpression()).makeStatement();
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
              .equalsIgnoreNullability(currentType.getDescriptor())) {
        return FieldAccess.Builder.from(fieldDescriptor)
            .setQualifier(
                convertOuterClassReference(
                    JdtUtils.findCurrentTypeBinding(expression),
                    variableBinding.getDeclaringClass(),
                    false))
            .build();
      }
      return FieldAccess.Builder.from(fieldDescriptor).setQualifier(qualifier).build();
    }

    private BinaryExpression convert(org.eclipse.jdt.core.dom.InfixExpression expression) {
      Expression leftOperand = convert(expression.getLeftOperand());
      Expression rightOperand = convert(expression.getRightOperand());
      BinaryOperator operator = JdtUtils.getBinaryOperator(expression.getOperator());
      BinaryExpression binaryExpression =
          BinaryExpression.newBuilder()
              .setTypeDescriptor(JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()))
              .setLeftOperand(leftOperand)
              .setOperator(operator)
              .setRightOperand(rightOperand)
              .build();
      for (Object object : expression.extendedOperands()) {
        org.eclipse.jdt.core.dom.Expression extendedOperand =
            (org.eclipse.jdt.core.dom.Expression) object;
        binaryExpression =
            BinaryExpression.newBuilder()
                .setTypeDescriptor(JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()))
                .setLeftOperand(binaryExpression)
                .setOperator(operator)
                .setRightOperand(convert(extendedOperand))
                .build();
      }
      return binaryExpression;
    }

    /**
     * Returns a qualifier for a method invocation that doesn't have one, specifically,
     * instanceMethod() will return a resolved qualifier that may refer to "this" or to the
     * enclosing instances. A staticMethod() will return null.
     */
    private Expression getExplicitQualifier(
        org.eclipse.jdt.core.dom.MethodInvocation methodInvocation) {

      if (methodInvocation.getExpression() == null) {
        // No qualifier specified.
        IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
        if (JdtUtils.isStatic(methodBinding)) {
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

    private Expression convert(org.eclipse.jdt.core.dom.MethodInvocation methodInvocation) {

      Expression qualifier = getExplicitQualifier(methodInvocation);

      IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
      List<Expression> arguments =
          convertExpressions(JdtUtils.asTypedList(methodInvocation.arguments()));
      maybePackageVarargs(
          methodBinding, JdtUtils.asTypedList(methodInvocation.arguments()), arguments);
      MethodCall methodCall =
          MethodCall.Builder.from(methodDescriptor)
              .setQualifier(qualifier)
              .setArguments(arguments)
              .build();
      if (JdtUtils.hasUncheckedCastAnnotation(methodBinding)) {
        // Annotate the invocation with the expected type. When InsertErasureSureTypeSafetyCasts
        // runs, this invocation will be skipped as it will no longer be an assignment context.
        return JsDocAnnotatedExpression.newBuilder()
            .setExpression(methodCall)
            .setAnnotationType(methodDescriptor.getReturnTypeDescriptor())
            .build();
      }
      return methodCall;
    }

    private MethodCall convert(org.eclipse.jdt.core.dom.SuperMethodInvocation expression) {
      IMethodBinding methodBinding = expression.resolveMethodBinding();

      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
      List<Expression> arguments = convertExpressions(JdtUtils.asTypedList(expression.arguments()));
      maybePackageVarargs(methodBinding, JdtUtils.asTypedList(expression.arguments()), arguments);
      if (expression.getQualifier() == null) {
        return MethodCall.Builder.from(methodDescriptor)
            .setQualifier(new SuperReference(currentType.getSuperTypeDescriptor()))
            .setArguments(arguments)
            .build();
      } else if (expression.getQualifier().resolveTypeBinding().isInterface()) {
        // This is a default method call.
        return MethodCall.Builder.from(methodDescriptor)
            .setQualifier(new ThisReference(methodDescriptor.getEnclosingClassTypeDescriptor()))
            .setArguments(arguments)
            .setIsStaticDispatch(true)
            .build();
      } else {
        // OuterClass.super.fun() is transpiled to
        // SuperClassOfOuterClass.prototype.fun.call(OuterClass.this);
        return MethodCall.Builder.from(methodDescriptor)
            .setQualifier(
                convertOuterClassReference(
                    JdtUtils.findCurrentTypeBinding(expression),
                    expression.getQualifier().resolveTypeBinding(),
                    true))
            .setArguments(arguments)
            .setIsStaticDispatch(true)
            .build();
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
      checkArgument(methodBinding.isVarargs());
      int parametersLength = methodBinding.getParameterTypes().length;
      TypeDescriptor varargsTypeDescriptor =
          JdtUtils.createTypeDescriptor(methodBinding.getParameterTypes()[parametersLength - 1]);
      if (j2clArguments.size() < parametersLength) {
        // no argument for the varargs, add an empty array.
        return new ArrayLiteral(varargsTypeDescriptor);
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

    private Expression convert(org.eclipse.jdt.core.dom.ParenthesizedExpression expression) {
      // Preserve the parenthesis. J2CL does not yet handle properly parenthesizing the output
      // according to operator precedence.
      return new MultiExpression(convert(expression.getExpression()));
    }

    private UnaryExpression convert(org.eclipse.jdt.core.dom.PostfixExpression expression) {
      return PostfixExpression.newBuilder()
          .setTypeDescriptor(JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()))
          .setOperand(convert(expression.getOperand()))
          .setOperator(JdtUtils.getPostfixOperator(expression.getOperator()))
          .build();
    }

    private UnaryExpression convert(org.eclipse.jdt.core.dom.PrefixExpression expression) {
      return PrefixExpression.newBuilder()
          .setTypeDescriptor(JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()))
          .setOperand(convert(expression.getOperand()))
          .setOperator(JdtUtils.getPrefixOperator(expression.getOperator()))
          .build();
    }

    private Expression convert(org.eclipse.jdt.core.dom.QualifiedName expression) {
      IBinding binding = expression.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          return FieldAccess.Builder.from(JdtUtils.createFieldDescriptor(variableBinding))
              .setQualifier(convert(expression.getQualifier()))
              .build();
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
      IMethodBinding currentMethodBinding =
          checkNotNull(JdtUtils.findCurrentMethodBinding(statement));
      Expression expression =
          statement.getExpression() == null ? null : convert(statement.getExpression());
      TypeDescriptor returnTypeDescriptor =
          JdtUtils.createTypeDescriptorWithNullability(
              currentMethodBinding.getReturnType(), currentMethodBinding.getAnnotations());
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
      Type type = enclosingTypeByVariable.get(variableByJdtBinding.get(variableBinding));
      if (type != null) {
        return type.getDescriptor();
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
                  .equalsIgnoreNullability(currentType.getDescriptor())) {
            return FieldAccess.Builder.from(fieldDescriptor)
                .setQualifier(
                    convertOuterClassReference(
                        JdtUtils.findCurrentTypeBinding(expression),
                        variableBinding.getDeclaringClass(),
                        false))
                .build();
          } else {
            return FieldAccess.Builder.from(fieldDescriptor).build();
          }
        } else {
          // It refers to a local variable or parameter in a method or block.
          Variable variable = checkNotNull(variableByJdtBinding.get(variableBinding));
          // The innermost type in which this variable is declared.
          TypeDescriptor enclosingTypeDescriptor = findEnclosingTypeDescriptor(variableBinding);
          TypeDescriptor currentTypeDescriptor = currentType.getDescriptor();
          if (!enclosingTypeDescriptor.equalsIgnoreNullability(currentTypeDescriptor)) {
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
     * In the case of outside a constructor, if A is the direct enclosing class, A.this =>
     * this.f_$outer_this. if A is the enclosing class of the direct enclosing class, A.this =>
     * this.f_$outer_this$.f_$outer_this.
     * <p>
     * In the case of inside a constructor, the above two cases should be translated to $outer_this
     * and $outer_this.f_outer$, $outer_this is the added parameter of the constructor.
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
      if (!JdtUtils.createTypeDescriptor(innerTypeBinding)
          .equalsIgnoreNullability(currentType.getDescriptor())) {
        // currentType is a lambda type.
        qualifier =
            FieldAccess.Builder.from(
                    AstUtils.getFieldDescriptorForEnclosingInstance(
                        currentType.getDescriptor(), currentType.getEnclosingTypeDescriptor()))
                .setQualifier(qualifier)
                .build();
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
            FieldAccess.Builder.from(
                    AstUtils.getFieldDescriptorForEnclosingInstance(
                        enclosingTypeDescriptor, fieldTypeDescriptor))
                .setQualifier(qualifier)
                .build();
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
        if (typeStack.get(i).getDescriptor().equalsIgnoreNullability(enclosingClassDescriptor)) {
          break;
        }
        capturesByTypeDescriptor.put(
            TypeDescriptors.toNullable(typeStack.get(i).getDescriptor()), variable);
      }
      // for reference to a captured variable, if it is in a constructor, translate to
      // reference to outer parameter, otherwise, translate to reference to corresponding
      // field created for the captured variable.
      FieldDescriptor fieldDescriptor =
          AstUtils.getFieldDescriptorForCapture(currentType.getDescriptor(), variable);
      ThisReference qualifier = new ThisReference(currentType.getDescriptor());
      return FieldAccess.Builder.from(fieldDescriptor).setQualifier(qualifier).build();
    }

    private Variable convert(org.eclipse.jdt.core.dom.SingleVariableDeclaration node) {
      String name = node.getName().getFullyQualifiedName();
      TypeDescriptor typeDescriptor =
          node.getType() instanceof org.eclipse.jdt.core.dom.UnionType
              ? convert((org.eclipse.jdt.core.dom.UnionType) node.getType())
              : JdtUtils.createTypeDescriptor(node.getType().resolveBinding());
      Variable variable =
          Variable.newBuilder().setName(name).setTypeDescriptor(typeDescriptor).build();
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
          convertStatements(JdtUtils.asTypedList(statement.statements())));
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
      List<Expression> arguments = convertExpressions(JdtUtils.asTypedList(expression.arguments()));
      maybePackageVarargs(
          superConstructorBinding, JdtUtils.asTypedList(expression.arguments()), arguments);
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
          MethodCall.Builder.from(methodDescriptor)
              .setQualifier(qualifier)
              .setArguments(arguments)
              .build();
      return superCall.makeStatement();
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

      if (literalTypeDescriptor.isArray()) {
        if (literalTypeDescriptor.getLeafTypeDescriptor().isNative()) {
          // class literal of native js type array returns Object[].class
          literalTypeDescriptor =
              TypeDescriptors.getForArray(TypeDescriptors.get().javaLangObject, 1);
        }
        return convertArrayTypeLiteral(literalTypeDescriptor, javaLangClassTypeDescriptor);
      }

      if (literalTypeDescriptor.isNative()) {
        // class literal of native JsType is JavaScriptObject.class
        literalTypeDescriptor = TypeDescriptors.BootstrapType.JAVA_SCRIPT_OBJECT.getDescriptor();
      } else if (literalTypeDescriptor.isJsFunctionInterface()
          || literalTypeDescriptor.isJsFunctionImplementation()) {
        // class literal for JsFunction interfaces and implementations.
        literalTypeDescriptor = TypeDescriptors.BootstrapType.JAVA_SCRIPT_FUNCTION.getDescriptor();
      }
      return convertRegularTypeLiteral(literalTypeDescriptor, javaLangClassTypeDescriptor);
    }

    private Expression convertRegularTypeLiteral(
        TypeDescriptor literalTypeDescriptor, TypeDescriptor javaLangClassTypeDescriptor) {
      // <ClassLiteralClass>.$getClass()
      MethodDescriptor classMethodDescriptor =
          MethodDescriptor.newBuilder()
              .setJsInfo(JsInfo.RAW)
              .setIsStatic(true)
              .setEnclosingClassTypeDescriptor(javaLangClassTypeDescriptor)
              .setName("$get")
              .setParameterTypeDescriptors(Lists.newArrayList(TypeDescriptors.NATIVE_FUNCTION))
              .setReturnTypeDescriptor(javaLangClassTypeDescriptor)
              .build();
      return MethodCall.Builder.from(classMethodDescriptor)
          .setArguments(new TypeReference(literalTypeDescriptor))
          .build();
    }

    private Expression convertArrayTypeLiteral(
        TypeDescriptor literalTypeDescriptor, TypeDescriptor javaLangClassTypeDescriptor) {
      checkState(literalTypeDescriptor.isArray());

      MethodDescriptor classMethodDescriptor =
          MethodDescriptor.newBuilder()
              .setJsInfo(JsInfo.RAW)
              .setIsStatic(true)
              .setEnclosingClassTypeDescriptor(javaLangClassTypeDescriptor)
              .setName("$get")
              .setParameterTypeDescriptors(
                  Lists.newArrayList(
                      TypeDescriptors.NATIVE_FUNCTION, TypeDescriptors.get().primitiveInt))
              .setReturnTypeDescriptor(javaLangClassTypeDescriptor)
              .build();

      return MethodCall.Builder.from(classMethodDescriptor)
          .setArguments(
              new TypeReference(literalTypeDescriptor.getLeafTypeDescriptor()),
              new NumberLiteral(
                  TypeDescriptors.get().primitiveInt, literalTypeDescriptor.getDimensions()))
          .build();
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

    private TypeDescriptor convert(org.eclipse.jdt.core.dom.UnionType unionType) {
      List<TypeDescriptor> unionedTypeDescriptors = new ArrayList<>();
      for (Object object : unionType.types()) {
        org.eclipse.jdt.core.dom.Type type = (org.eclipse.jdt.core.dom.Type) object;
        unionedTypeDescriptors.add(JdtUtils.createTypeDescriptor(type.resolveBinding()));
      }
      return TypeDescriptors.createUnion(
          unionedTypeDescriptors, JdtUtils.createTypeDescriptor(unionType.resolveBinding()));
    }

    private VariableDeclarationFragment convert(
        org.eclipse.jdt.core.dom.VariableDeclarationFragment variableDeclarationFragment) {
      IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();
      Variable variable = JdtUtils.createVariable(variableBinding);
      if (!variable.getTypeDescriptor().isTypeVariable()
          && !variable.getTypeDescriptor().isPrimitive()) {
        // Local variables default to nullable. A flow analysis pass will later tighten their type
        // based on the values that are assigned to them.
        variable.setTypeDescriptor(TypeDescriptors.toNullable(variable.getTypeDescriptor()));
      }

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
     *
     * <p>Enclosing type is a broader category than declaring type since some variables (fields)
     * have a declaring type (that is also their enclosing type) while other variables do not.
     */
    private void recordEnclosingType(Variable variable, Type enclosingType) {
      enclosingTypeByVariable.put(variable, enclosingType);
    }

    private ExpressionStatement convert(
        org.eclipse.jdt.core.dom.VariableDeclarationStatement statement) {
      List<VariableDeclarationFragment> variableDeclarations = new ArrayList<>();
      for (Object object : statement.fragments()) {
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
            (org.eclipse.jdt.core.dom.VariableDeclarationFragment) object;
        variableDeclarations.add(convert(fragment));
      }
      return new VariableDeclarationExpression(variableDeclarations).makeStatement();
    }

    private Type createType(ITypeBinding typeBinding) {
      if (typeBinding == null) {
        return null;
      }
      Kind kind = JdtUtils.getKindFromTypeBinding(typeBinding);
      Visibility visibility = JdtUtils.getVisibility(typeBinding);
      TypeDescriptor typeDescriptor = JdtUtils.createTypeDescriptor(typeBinding);
      Type type =
          typeBinding.isAnonymous()
              ? new AnonymousType(kind, visibility, typeDescriptor)
              : new Type(kind, visibility, typeDescriptor);

      type.setStatic(JdtUtils.isStatic(typeBinding));
      type.setAbstract(JdtUtils.isAbstract(typeBinding));
      type.setAnonymous(typeBinding.isAnonymous());
      return type;
    }
  }

  private CompilationUnit buildCompilationUnit(
      String sourceFilePath,
      org.eclipse.jdt.core.dom.CompilationUnit compilationUnit,
      Iterable<ITypeBinding> wellKnownTypeBindings) {
    ASTConverter converter = new ASTConverter();
    return converter.convert(sourceFilePath, compilationUnit, wellKnownTypeBindings);
  }

  public static List<CompilationUnit> build(
      CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings) {

    Map<String, org.eclipse.jdt.core.dom.CompilationUnit> jdtUnitsByFilePath =
        compilationUnitsAndTypeBindings.getCompilationUnitsByFilePath();
    Iterable<ITypeBinding> wellKnownTypeBindings =
        compilationUnitsAndTypeBindings.getTypeBindings();
    CompilationUnitBuilder compilationUnitBuilder = new CompilationUnitBuilder();

    new ArrayList<>();
    List<Entry<String, org.eclipse.jdt.core.dom.CompilationUnit>> entries =
        new ArrayList<>(jdtUnitsByFilePath.entrySet());
    // Ensure that all source package-info classes come before all other classes so that the
    // freshness of the PackageInfoCache can be trusted.
    sortPackageInfoFirst(entries);

    return entries
        .stream()
        .map(
            entry ->
                compilationUnitBuilder.buildCompilationUnit(
                    entry.getKey(), entry.getValue(), wellKnownTypeBindings))
        .collect(Collectors.toList());
  }

  private static void sortPackageInfoFirst(
      List<Entry<String, org.eclipse.jdt.core.dom.CompilationUnit>> entries) {
    // Ensure that all source package-info classes come before all other classes so that the
    // freshness of the PackageInfoCache can be trusted.
    Collections.sort(
        entries,
        comparingByKey(
            (thisFilePath, thatFilePath) -> {
              boolean thisIsPackageInfo = thisFilePath.endsWith("package-info.java");
              boolean thatIsPackageInfo = thatFilePath.endsWith("package-info.java");
              return ComparisonChain.start()
                  .compareTrueFirst(thisIsPackageInfo, thatIsPackageInfo)
                  .compare(thisFilePath, thatFilePath)
                  .result();
            }));
  }

  private CompilationUnitBuilder() {}
}
