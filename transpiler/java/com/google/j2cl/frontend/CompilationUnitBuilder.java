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
import static java.util.stream.Collectors.toList;

import com.google.common.base.Predicates;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.AstUtilConstants;
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
import com.google.j2cl.ast.DeclaredTypeDescriptor;
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
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.LabeledStatement;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.ast.PrimitiveTypes;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.RuntimeMethods;
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
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.UnaryExpression;
import com.google.j2cl.ast.UnionTypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.ast.WhileStatement;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclaration;

/** Creates a J2CL Java AST from the AST provided by JDT. */
public class CompilationUnitBuilder {
  private class ASTConverter {
    private PackageInfoCache packageInfoCache = PackageInfoCache.get();
    private Map<IVariableBinding, Variable> variableByJdtBinding = new HashMap<>();
    private Map<Variable, Type> enclosingTypeByVariable = new HashMap<>();
    private Multimap<String, Variable> capturesByTypeName = LinkedHashMultimap.create();
    private List<Type> typeStack = new ArrayList<>();
    private Type currentType = null;

    private String currentSourceFile;
    private org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit;
    private CompilationUnit j2clCompilationUnit;
    private int qualifierCounter;

    private void pushType(Type type) {
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
      JdtUtils.initWellKnownTypes(jdtCompilationUnit.getAST(), wellKnownTypeBindings);
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
        case ASTNode.TYPE_DECLARATION:
          convertAndAddType(typeDeclaration);
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
      Type enumType = convertAndAddType(enumDeclaration);
      checkState(enumType.isEnum());

      int ordinal = 0;
      for (EnumConstantDeclaration enumConstantDeclaration :
          JdtUtils.<EnumConstantDeclaration>asTypedList(enumDeclaration.enumConstants())) {
        enumType.addField(ordinal, convert(enumConstantDeclaration));
        ordinal++;
      }
      EnumMethodsCreator.applyTo(enumType);
    }

    private Type convertAndAddType(AbstractTypeDeclaration typeDeclaration) {
      ITypeBinding typeBinding = typeDeclaration.resolveBinding();
      Type type = createType(typeBinding, typeDeclaration);
      pushType(type);
      j2clCompilationUnit.addType(type);
      convertTypeBody(type, typeBinding, JdtUtils.asTypedList(typeDeclaration.bodyDeclarations()));
      popType();
      return type;
    }

    private void convertTypeBody(
        Type type, ITypeBinding typeBinding, List<BodyDeclaration> bodyDeclarations) {
      TypeDeclaration currentTypeDeclaration = type.getDeclaration();
      ITypeBinding superclassBinding = typeBinding.getSuperclass();
      if (superclassBinding != null) {
        capturesByTypeName.putAll(
            currentTypeDeclaration.getQualifiedSourceName(),
            capturesByTypeName.get(type.getSuperTypeDescriptor().getQualifiedSourceName()));
      }
      for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
        if (bodyDeclaration instanceof FieldDeclaration) {
          FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclaration;
          type.addFields(convert(fieldDeclaration));
        } else if (bodyDeclaration instanceof MethodDeclaration) {
          MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
          type.addMethod(convert(methodDeclaration));
        } else if (bodyDeclaration instanceof AnnotationTypeMemberDeclaration) {
          AnnotationTypeMemberDeclaration memberDeclaration =
              (AnnotationTypeMemberDeclaration) bodyDeclaration;
          type.addMethod(convert(memberDeclaration));
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

      for (Variable capturedVariable :
          capturesByTypeName.get(currentTypeDeclaration.getQualifiedSourceName())) {
        FieldDescriptor fieldDescriptor =
            AstUtils.getFieldDescriptorForCapture(type.getTypeDescriptor(), capturedVariable);
        type.addField(
            Field.Builder.from(fieldDescriptor)
                .setCapturedVariable(capturedVariable)
                .setSourcePosition(type.getSourcePosition())
                .build());
      }
      if (JdtUtils.capturesEnclosingInstance(typeBinding)) {
        // add field for enclosing instance.
        type.addField(
            0,
            Field.Builder.from(
                    AstUtils.getFieldDescriptorForEnclosingInstance(
                        type.getTypeDescriptor(),
                        type.getEnclosingTypeDeclaration().toUnparamterizedTypeDescriptor()))
                .setSourcePosition(type.getSourcePosition())
                .build());
      }
    }

    private Field convert(EnumConstantDeclaration enumConstantDeclaration) {
      IMethodBinding enumConstructorBinding = enumConstantDeclaration.resolveConstructorBinding();
      if (enumConstantDeclaration.getAnonymousClassDeclaration() != null) {
        convertAnonymousClassDeclaration(
            enumConstantDeclaration.getAnonymousClassDeclaration(), enumConstructorBinding, null);
      }

      Expression initializer =
          NewInstance.Builder.from(JdtUtils.createMethodDescriptor(enumConstructorBinding))
              .setArguments(
                  convertArguments(
                      enumConstructorBinding,
                      JdtUtils.asTypedList(enumConstantDeclaration.arguments())))
              .build();

      FieldDescriptor fieldDescriptor =
          JdtUtils.createFieldDescriptor(enumConstantDeclaration.resolveVariable());
      return Field.Builder.from(fieldDescriptor)
          .setInitializer(initializer)
          .setEnumField(true)
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
          initializer = convertOrNull(fragment.getInitializer());
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
            JdtUtils.createTypeDescriptor(variableBinding.getType()).toUnboxedType(),
            (Number) constantValue);
      }
      if (constantValue instanceof String) {
        return StringLiteral.fromPlainText((String) constantValue);
      }
      if (constantValue instanceof Character) {
        return new CharacterLiteral((char) constantValue);
      }
      if (constantValue instanceof Boolean) {
        return (boolean) constantValue ? BooleanLiteral.get(true) : BooleanLiteral.get(false);
      }
      throw new RuntimeException(
          "Need to implement translation for compile time constants of type: "
              + constantValue.getClass().getSimpleName()
              + ".");
    }

    private Method convert(MethodDeclaration methodDeclaration) {
      List<Variable> parameters = new ArrayList<>();
      for (SingleVariableDeclaration parameter :
          JdtUtils.<SingleVariableDeclaration>asTypedList(methodDeclaration.parameters())) {
        parameters.add(createVariable(parameter));
      }

      // If a method has no body, initialize the body with an empty list of statements.
      Block body =
          methodDeclaration.getBody() == null
              ? new Block(getSourcePosition(methodDeclaration))
              : convert(methodDeclaration.getBody());

      return newMethodBuilder(methodDeclaration.resolveBinding(), methodDeclaration)
          .setParameters(parameters)
          .addStatements(body.getStatements())
          .build();
    }

    private Method convert(AnnotationTypeMemberDeclaration memberDeclaration) {
      return newMethodBuilder(memberDeclaration.resolveBinding(), memberDeclaration).build();
    }

    private Method.Builder newMethodBuilder(IMethodBinding methodBinding, ASTNode node) {
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
      // TODO(b/31312257): fix or decide to not emit @override and suppress the error.
      boolean isOverride =
          methodDescriptor
              .getOverriddenMethodDescriptors()
              .stream()
              .anyMatch(m -> requiresOverrideAnnotation(methodDescriptor, m));
      return Method.newBuilder()
          .setMethodDescriptor(methodDescriptor)
          .setOverride(isOverride)
          .setSourcePosition(getSourcePosition(node));
    }

    private boolean requiresOverrideAnnotation(
        MethodDescriptor methodDescriptor, MethodDescriptor overriddenMethodDescriptor) {
      if (methodDescriptor.isJsMember()) {
        return overriddenMethodDescriptor.isJsMember()
            && AstUtils.overrideNeedsAtOverrideAnnotation(overriddenMethodDescriptor);
      }
      return methodDescriptor.isJsOverride(overriddenMethodDescriptor)
          && AstUtils.overrideNeedsAtOverrideAnnotation(overriddenMethodDescriptor);
    }

    private ArrayAccess convert(org.eclipse.jdt.core.dom.ArrayAccess expression) {
      return ArrayAccess.newBuilder()
          .setArrayExpression(convert(expression.getArray()))
          .setIndexExpression(convert(expression.getIndex()))
          .build();
    }

    private NewArray convert(org.eclipse.jdt.core.dom.ArrayCreation expression) {
      ArrayType arrayType = expression.getType();

      List<Expression> dimensionExpressions =
          convertExpressions(JdtUtils.asTypedList(expression.dimensions()));
      // If some dimensions are not initialized then make that explicit.
      while (dimensionExpressions.size() < arrayType.getDimensions()) {
        dimensionExpressions.add(NullLiteral.get());
      }

      ArrayLiteral arrayLiteral =
          expression.getInitializer() == null ? null : convert(expression.getInitializer());

      ArrayTypeDescriptor typeDescriptor =
          (ArrayTypeDescriptor) JdtUtils.createTypeDescriptor(expression.resolveTypeBinding());
      return NewArray.newBuilder()
          .setTypeDescriptor(typeDescriptor)
          .setDimensionExpressions(dimensionExpressions)
          .setArrayLiteral(arrayLiteral)
          .build();
    }

    private ArrayLiteral convert(org.eclipse.jdt.core.dom.ArrayInitializer expression) {
      return new ArrayLiteral(
          (ArrayTypeDescriptor) JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()),
          convertExpressions(JdtUtils.asTypedList(expression.expressions())));
    }

    private BooleanLiteral convert(org.eclipse.jdt.core.dom.BooleanLiteral literal) {
      return literal.booleanValue() ? BooleanLiteral.get(true) : BooleanLiteral.get(false);
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
      boolean isAnonymousClassCreation = expression.getAnonymousClassDeclaration() != null;
      if (isAnonymousClassCreation) {
        return convertAnonymousClassCreation(expression);
      } else {
        return convertRegularClassCreation(expression);
      }
    }

    private MethodDescriptor convertAnonymousClassDeclaration(
        AnonymousClassDeclaration typeDeclaration,
        IMethodBinding constructorBinding,
        TypeDescriptor superQualifierTypeDescriptor) {

      ITypeBinding typeBinding = typeDeclaration.resolveBinding();
      Type type = createType(typeBinding, typeDeclaration);
      j2clCompilationUnit.addType(type);
      pushType(type);
      convertTypeBody(type, typeBinding, JdtUtils.asTypedList(typeDeclaration.bodyDeclarations()));

      // The initial constructor descriptor does not include the super call qualifier.
      MethodDescriptor constructorDescriptor = JdtUtils.createMethodDescriptor(constructorBinding);

      // Find the corresponding superconstructor, the ClassInstanceCreation construct does not
      // have a reference the super constructor that needs to be called. But the synthetic
      // anonymous class constructor is a subsignature of the corresponding super constructor.
      IMethodBinding superConstructorBinding =
          Arrays.stream(typeBinding.getSuperclass().getDeclaredMethods())
              .filter(IMethodBinding::isConstructor)
              .filter(constructorBinding::isSubsignature)
              .findFirst()
              .orElse(constructorBinding);
      MethodDescriptor superConstructorDescriptor =
          MethodDescriptor.Builder.from(JdtUtils.createMethodDescriptor(superConstructorBinding))
              .setEnclosingTypeDescriptor(type.getSuperTypeDescriptor())
              .build();

      if (superQualifierTypeDescriptor != null) {
        // If an explicit super qualifier was specified add it as the first parameter to the
        // constructor.
        constructorDescriptor =
            MethodDescriptor.Builder.from(constructorDescriptor)
                .addParameterTypeDescriptors(0, superQualifierTypeDescriptor)
                .build();
      }

      type.addMethod(
          0,
          AstUtils.createImplicitAnonymousClassConstructor(
              type.getSourcePosition(), constructorDescriptor, superConstructorDescriptor));
      popType();
      return constructorDescriptor;
    }

    private Expression convertAnonymousClassCreation(
        org.eclipse.jdt.core.dom.ClassInstanceCreation expression) {
      IMethodBinding constructorBinding = expression.resolveConstructorBinding();
      ITypeBinding newInstanceTypeBinding = constructorBinding.getDeclaringClass();

      List<Expression> arguments =
          convertArguments(constructorBinding, JdtUtils.asTypedList(expression.arguments()));
      if (expression.getExpression() != null) {
        // Add the explicit super constructor as the first parameter.
        arguments.add(0, convert(expression.getExpression()));
      }

      MethodDescriptor constructorDescriptor =
          convertAnonymousClassDeclaration(
              checkNotNull(expression.getAnonymousClassDeclaration()),
              constructorBinding,
              expression.getExpression() == null
                  ? null
                  : JdtUtils.createTypeDescriptor(expression.getExpression().resolveTypeBinding()));

      // the qualifier for the NewInstance.
      Expression newInstanceQualifier =
          constructorDescriptor
                  .getEnclosingTypeDescriptor()
                  .getTypeDeclaration()
                  .isCapturingEnclosingInstance()
              ? convertOuterClassReference(
                  JdtUtils.findCurrentTypeBinding(expression),
                  newInstanceTypeBinding.getDeclaringClass(),
                  false)
              : null;
      return NewInstance.Builder.from(constructorDescriptor)
          .setQualifier(newInstanceQualifier)
          .setArguments(arguments)
          .build();
    }

    private Expression convertRegularClassCreation(
        org.eclipse.jdt.core.dom.ClassInstanceCreation expression) {

      IMethodBinding constructorBinding = expression.resolveConstructorBinding();
      List<Expression> arguments =
          convertArguments(constructorBinding, JdtUtils.asTypedList(expression.arguments()));

      MethodDescriptor constructorMethodDescriptor =
          JdtUtils.createMethodDescriptor(constructorBinding);
      ITypeBinding newInstanceTypeBinding = constructorBinding.getDeclaringClass();
      Expression qualifier = convertOrNull(expression.getExpression());
      checkArgument(!newInstanceTypeBinding.isAnonymous());
      boolean needsQualifier = JdtUtils.capturesEnclosingInstance(newInstanceTypeBinding);
      checkArgument(
          qualifier == null || needsQualifier,
          "NewInstance of non nested class should have no qualifier.");

      // Resolve the qualifier of NewInstance that creates an instance of a nested class.
      // Implicit 'this' doesn't always refer to 'this', it may refer to any enclosing instances.
      qualifier =
          needsQualifier && qualifier == null
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

    private Expression convertOrNull(org.eclipse.jdt.core.dom.Expression expression) {
      return expression != null ? convert(expression) : null;
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
        case ASTNode.EXPRESSION_METHOD_REFERENCE:
          return convert((org.eclipse.jdt.core.dom.ExpressionMethodReference) expression);
        case ASTNode.CREATION_REFERENCE:
          return convert((org.eclipse.jdt.core.dom.CreationReference) expression);
        case ASTNode.TYPE_METHOD_REFERENCE:
          return convert((org.eclipse.jdt.core.dom.TypeMethodReference) expression);
        case ASTNode.SUPER_METHOD_REFERENCE:
          return convert((org.eclipse.jdt.core.dom.SuperMethodReference) expression);
        case ASTNode.FIELD_ACCESS:
          return convert((org.eclipse.jdt.core.dom.FieldAccess) expression);
        case ASTNode.INFIX_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.InfixExpression) expression);
        case ASTNode.INSTANCEOF_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.InstanceofExpression) expression);
        case ASTNode.LAMBDA_EXPRESSION:
          return convert((org.eclipse.jdt.core.dom.LambdaExpression) expression);
        case ASTNode.METHOD_INVOCATION:
          return convert((org.eclipse.jdt.core.dom.MethodInvocation) expression);
        case ASTNode.NULL_LITERAL:
          return NullLiteral.get();
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
        case ASTNode.SUPER_FIELD_ACCESS:
          return convert((org.eclipse.jdt.core.dom.SuperFieldAccess) expression);
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
      List<org.eclipse.jdt.core.dom.VariableDeclarationFragment> fragments =
          JdtUtils.asTypedList(expression.fragments());

      return VariableDeclarationExpression.newBuilder()
          .setVariableDeclarationFragments(
              fragments.stream().map(this::convert).collect(toImmutableList()))
          .build();
    }

    private List<Expression> convertExpressions(
        List<org.eclipse.jdt.core.dom.Expression> expressions) {
      return expressions.stream().map(this::convert).collect(toCollection(ArrayList::new));
    }

    private List<Statement> convertStatements(List<org.eclipse.jdt.core.dom.Statement> statements) {
      return statements.stream().map(this::convert).collect(toCollection(ArrayList::new));
    }

    private ConditionalExpression convert(
        org.eclipse.jdt.core.dom.ConditionalExpression conditionalExpression) {
      return new ConditionalExpression(
          JdtUtils.createTypeDescriptor(conditionalExpression.resolveTypeBinding()),
          convert(conditionalExpression.getExpression()),
          convert(conditionalExpression.getThenExpression()),
          convert(conditionalExpression.getElseExpression()));
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
          return new EmptyStatement(getSourcePosition(statement));
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
      return getSourcePosition(null, node);
    }

    public SourcePosition getSourcePosition(String name, ASTNode node) {
      int startLineNumber = jdtCompilationUnit.getLineNumber(node.getStartPosition()) - 1;
      int startColumnNumber = jdtCompilationUnit.getColumnNumber(node.getStartPosition());
      int lastCharacterPosition = node.getStartPosition() + node.getLength() - 1;
      int endLineNumber = jdtCompilationUnit.getLineNumber(lastCharacterPosition) - 1;
      int endColumnNumber = jdtCompilationUnit.getColumnNumber(lastCharacterPosition) + 1;
      return SourcePosition.newBuilder()
          .setFilePath(j2clCompilationUnit.getFilePath())
          .setName(name)
          .setStartPosition(startLineNumber, startColumnNumber)
          .setEndPosition(endLineNumber, endColumnNumber)
          .build();
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    private <T extends Statement> T convertOrNull(org.eclipse.jdt.core.dom.Statement statement) {
      return statement != null ? (T) convert(statement) : null;
    }

    private Statement convert(org.eclipse.jdt.core.dom.Statement statement) {
      Statement j2clStatement = convertStatement(statement);
      if (j2clStatement != null) {
        j2clStatement.setSourcePosition(getSourcePosition(statement));
      }
      return j2clStatement;
    }

    private LabeledStatement convert(org.eclipse.jdt.core.dom.LabeledStatement statement) {
      return new LabeledStatement(
          getSourcePosition(statement),
          statement.getLabel().getIdentifier(),
          convert(statement.getBody()));
    }

    private BreakStatement convert(org.eclipse.jdt.core.dom.BreakStatement statement) {
      return new BreakStatement(
          getSourcePosition(statement), getIdentifierOrNull(statement.getLabel()));
    }

    private ContinueStatement convert(org.eclipse.jdt.core.dom.ContinueStatement statement) {
      return new ContinueStatement(
          getSourcePosition(statement), getIdentifierOrNull(statement.getLabel()));
    }

    private String getIdentifierOrNull(SimpleName label) {
      return label == null ? null : label.getIdentifier();
    }

    private ForStatement convert(org.eclipse.jdt.core.dom.ForStatement statement) {
      return ForStatement.newBuilder()
          // The order here is important since initializers can define new variables
          // These can be used in the expression, updaters or the body
          // This is why we need to process initializers first
          .setInitializers(convertExpressions(JdtUtils.asTypedList(statement.initializers())))
          .setConditionExpression(convertOrNull(statement.getExpression()))
          .setBody(convert(statement.getBody()))
          .setUpdates(convertExpressions(JdtUtils.asTypedList(statement.updaters())))
          .setSourcePosition(getSourcePosition(statement))
          .build();
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
              .setFinal(true)
              .build();

      // int index = 0;
      Variable indexVariable =
          Variable.newBuilder().setName("$index").setTypeDescriptor(PrimitiveTypes.INT).build();

      // $index < $array.length
      Expression condition =
          BinaryExpression.newBuilder()
              .setLeftOperand(indexVariable)
              .setOperator(BinaryOperator.LESS)
              .setRightOperand(
                  FieldAccess.Builder.from(AstUtilConstants.getArrayLengthFieldDescriptor())
                      .setQualifier(arrayVariable.getReference())
                      .build())
              .build();

      ExpressionStatement forVariableDeclarationStatement =
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(
                  convert(statement.getParameter()),
                  ArrayAccess.newBuilder()
                      .setArrayExpression(arrayVariable.getReference())
                      .setIndexExpression(indexVariable.getReference())
                      .build())
              .build()
              .makeStatement(getSourcePosition(statement));

      return ForStatement.newBuilder()
          .setConditionExpression(condition)
          //  {   T t = $array[$index]; S; }
          .setBody(convert(statement.getBody()))
          // prepend the variable declaration.
          .addStatement(0, forVariableDeclarationStatement)
          .setInitializers(
              VariableDeclarationExpression.newBuilder()
                  .addVariableDeclaration(arrayVariable, convert(statement.getExpression()))
                  .addVariableDeclaration(indexVariable, NumberLiteral.of(0))
                  .build())
          .setUpdates(
              PostfixExpression.newBuilder()
                  .setOperand(indexVariable.getReference())
                  .setOperator(PostfixOperator.INCREMENT)
                  .build())
          .setSourcePosition(getSourcePosition(statement))
          .build();
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
              .setFinal(true)
              .build();

      VariableDeclarationExpression iteratorDeclaration =
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(
                  iteratorVariable,
                  MethodCall.Builder.from(JdtUtils.createMethodDescriptor(iteratorMethodBinding))
                      .setQualifier(convert(statement.getExpression()))
                      .build())
              .build();

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
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(
                  convert(statement.getParameter()),
                  MethodCall.Builder.from(JdtUtils.createMethodDescriptor(nextMethodBinding))
                      .setQualifier(iteratorVariable.getReference())
                      .build())
              .build()
              .makeStatement(getSourcePosition(statement));

      return ForStatement.newBuilder()
          .setConditionExpression(condition)
          .setBody(convert(statement.getBody()))
          // Prepend the variable declaration.
          .addStatement(0, forVariableDeclarationStatement)
          .setInitializers(iteratorDeclaration)
          .setSourcePosition(getSourcePosition(statement))
          .build();
    }

    private DoWhileStatement convert(org.eclipse.jdt.core.dom.DoStatement statement) {
      return new DoWhileStatement(
          getSourcePosition(statement),
          convert(statement.getExpression()),
          convert(statement.getBody()));
    }

    private Statement convert(
        org.eclipse.jdt.core.dom.TypeDeclarationStatement typeDeclarationStatement) {
      convert(typeDeclarationStatement.getDeclaration());
      return null;
    }

    private WhileStatement convert(org.eclipse.jdt.core.dom.WhileStatement statement) {
      return new WhileStatement(
          getSourcePosition(statement),
          convert(statement.getExpression()),
          convert(statement.getBody()));
    }

    private IfStatement convert(org.eclipse.jdt.core.dom.IfStatement statement) {
      return new IfStatement(
          getSourcePosition(statement),
          convert(statement.getExpression()),
          convert(statement.getThenStatement()),
          convertOrNull(statement.getElseStatement()));
    }

    private InstanceOfExpression convert(org.eclipse.jdt.core.dom.InstanceofExpression expression) {
      return new InstanceOfExpression(
          getSourcePosition(expression),
          convert(expression.getLeftOperand()),
          JdtUtils.createTypeDescriptor(expression.getRightOperand().resolveBinding()));
    }

    private Expression convert(LambdaExpression expression) {
      MethodDescriptor functionalMethodDescriptor =
          JdtUtils.createMethodDescriptor(expression.resolveMethodBinding());

      return FunctionExpression.newBuilder()
          .setTypeDescriptor(JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()))
          .setParameters(
              JdtUtils.<VariableDeclaration>asTypedList(expression.parameters())
                  .stream()
                  .map(this::convert)
                  .collect(toImmutableList()))
          .setStatements(
              convertLambdaBody(
                      expression.getBody(), functionalMethodDescriptor.getReturnTypeDescriptor())
                  .getStatements())
          .setSourcePosition(getSourcePosition(expression))
          .build();
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
            AstUtils.createReturnOrExpressionStatement(
                getSourcePosition(lambdaBody), lambdaMethodBody, returnTypeDescriptor);
        statement.setSourcePosition(getSourcePosition(lambdaBody));
        body = new Block(getSourcePosition(lambdaBody), statement);
      }
      return body;
    }

    /**
     * Converts method reference expressions of the form:
     *
     * <p>
     *
     * <pre>
     *          q::m    into    (par1, ..., parN) -> q.m(par1, ..., parN)  or
     *                          (let $q = q, (par1, ..., parN) -> $q.m(par1, ..., parN))
     * </pre>
     *
     * <p>Depending on whether the qualifier can be evaluated inside the functional expression
     * preserving semantics.
     */
    private Expression convert(ExpressionMethodReference expression) {
      checkNotNull(expression.getExpression());
      org.eclipse.jdt.core.dom.Expression qualifier = expression.getExpression();
      TypeDescriptor expressionTypeDescriptor =
          JdtUtils.createTypeDescriptor(expression.resolveTypeBinding());
      if (JdtUtils.isEffectivelyConstant(qualifier)) {
        // There is no need to introduce a temporary variable for the qualifier.
        return createForwardingFunctionExpression(
            getSourcePosition(expression),
            expressionTypeDescriptor,
            // functional interface method that the expression implements.
            JdtUtils.createMethodDescriptor(
                expression.resolveTypeBinding().getFunctionalInterfaceMethod()),
            convert(qualifier),
            // target method to forward to.
            JdtUtils.createMethodDescriptor(expression.resolveMethodBinding()),
            false);
      }
      // The semantics require that the qualifier be evaluated in the context where the method
      // reference appears, so here we introduce a temporary variable to store the evaluated
      // qualifier.
      Variable variable =
          Variable.newBuilder()
              .setFinal(true)
              .setName("$$qualifier" + qualifierCounter++)
              .setTypeDescriptor(JdtUtils.createTypeDescriptor(qualifier.resolveTypeBinding()))
              .build();
      // Store the declaring type in the local scope so that variable declaration scope points to
      // the right type when the functional expression is effectively constructed.
      final Type declaringType = currentType;
      return MultiExpression.newBuilder()
          .setExpressions(
              // Declare the temporary variable and initialize to the evaluated qualifier.
              VariableDeclarationExpression.newBuilder()
                  .addVariableDeclaration(variable, convert(qualifier))
                  .build(),
              // Construct the functional expression.
              createForwardingFunctionExpression(
                  getSourcePosition(expression),
                  expressionTypeDescriptor,
                  // functional interface method that the expression implements.
                  JdtUtils.createMethodDescriptor(
                      expression.resolveTypeBinding().getFunctionalInterfaceMethod()),
                  declaringType == currentType
                      ? variable.getReference()
                      : convertCapturedVariableReference(variable, declaringType.getDeclaration()),
                  // target method to forward to.
                  JdtUtils.createMethodDescriptor(expression.resolveMethodBinding()),
                  false))
          .build();
    }

    /**
     * Converts method reference expressions of the forms:
     *
     * <p>
     *
     * <pre>
     *          A[]::new    into     (size) -> new A[size]
     *          A:new       into     (par1, ..., parN) -> new A(par1, ..., parN)
     * </pre>
     */
    private Expression convert(CreationReference expression) {
      ITypeBinding expressionTypeBinding = expression.getType().resolveBinding();
      MethodDescriptor functionalMethodDescriptor =
          JdtUtils.createMethodDescriptor(
              expression.resolveTypeBinding().getFunctionalInterfaceMethod());

      List<Variable> parameters =
          AstUtils.createParameterVariables(
              functionalMethodDescriptor.getParameterTypeDescriptors());

      // There are 3 flavors for CreationReferences: 1) unqualified constructors, 2) implicitly
      // qualified constructors  and 3) array creations.

      // If the expression does not resolve, it is an array creation.
      if (expression.resolveMethodBinding() == null) {
        // convert A[]::new into (size) -> new A[size]
        ArrayTypeDescriptor arrayType =
            (ArrayTypeDescriptor) JdtUtils.createTypeDescriptor(expressionTypeBinding);

        checkArgument(arrayType.isArray());
        // Array creation method references always have exactly one parameter.
        checkArgument(parameters.size() == 1);

        // The size of the array is the only parameter in the implemented function. It's legal for
        // the source to provide only one dimension parameter to to create a multidimensional array
        // but our AST expects NewArray nodes to provide an expression for each dimension in the
        // array type, hence the missing dimensions are padded with null.
        ImmutableList<Expression> dimensionExpressions =
            ImmutableList.<Expression>builder()
                .add(parameters.get(0).getReference())
                .addAll(Collections.nCopies(arrayType.getDimensions() - 1, NullLiteral.get()))
                .build();

        return FunctionExpression.newBuilder()
            .setTypeDescriptor(functionalMethodDescriptor.getEnclosingTypeDescriptor())
            .setParameters(parameters)
            .setStatements(
                ReturnStatement.newBuilder()
                    .setExpression(
                        NewArray.newBuilder()
                            .setTypeDescriptor(arrayType)
                            .setDimensionExpressions(dimensionExpressions)
                            .build())
                    .setTypeDescriptor(functionalMethodDescriptor.getReturnTypeDescriptor())
                    .setSourcePosition(getSourcePosition(expression))
                    .build())
            .setSourcePosition(getSourcePosition(expression))
            .build();
      }

      MethodDescriptor targetConstructorMethodDescriptor =
          JdtUtils.createMethodDescriptor(expression.resolveMethodBinding());

      // This is a class instantiation.
      // convert A:new into (par1, ..., parN) -> new A(par1, ..., parN) or
      // (par1, ..., parN) -> B.this.new A(par1, ..., parN)
      checkArgument(
          targetConstructorMethodDescriptor.getParameterTypeDescriptors().size()
              == parameters.size());

      Expression qualifier =
          targetConstructorMethodDescriptor
                  .getEnclosingTypeDescriptor()
                  .getTypeDeclaration()
                  .isCapturingEnclosingInstance()
              // Inner classes may have an implicit enclosing class qualifier (2).
              ? convertOuterClassReference(
                  JdtUtils.findCurrentTypeBinding(expression),
                  expressionTypeBinding.getDeclaringClass(),
                  true)
              : null;

      return FunctionExpression.newBuilder()
          .setTypeDescriptor(functionalMethodDescriptor.getEnclosingTypeDescriptor())
          .setParameters(parameters)
          .setStatements(
              (Statement)
                  ReturnStatement.newBuilder()
                      .setExpression(
                          NewInstance.Builder.from(targetConstructorMethodDescriptor)
                              .setQualifier(qualifier)
                              .setArguments(
                                  parameters
                                      .stream()
                                      .map(Variable::getReference)
                                      .collect(toImmutableList()))
                              .build())
                      .setTypeDescriptor(functionalMethodDescriptor.getReturnTypeDescriptor())
                      .setSourcePosition(getSourcePosition(expression))
                      .build())
          .setSourcePosition(getSourcePosition(expression))
          .build();
    }

    /**
     * Converts method reference expressions of the form:
     *
     * <p>
     *
     * <pre>
     *          A::m    into     (par1, ..., parN) -> A.m(par1, ..., parN)
     * </pre>
     */
    private Expression convert(TypeMethodReference expression) {
      ITypeBinding expressionTypeBinding = expression.resolveTypeBinding();
      return createForwardingFunctionExpression(
          getSourcePosition(expression),
          JdtUtils.createDeclaredTypeDescriptor(expressionTypeBinding),
          JdtUtils.createMethodDescriptor(expressionTypeBinding.getFunctionalInterfaceMethod()),
          null,
          JdtUtils.createMethodDescriptor(expression.resolveMethodBinding()),
          false);
    }

    /**
     * Converts method reference expressions of the form:
     *
     * <p>
     *
     * <pre>
     *          super::m    into     (par1, ..., parN) -> super.m(par1, ..., parN)
     * </pre>
     */
    private Expression convert(SuperMethodReference expression) {
      IMethodBinding methodBinding = expression.resolveMethodBinding();

      return createForwardingFunctionExpression(
          getSourcePosition(expression),
          JdtUtils.createDeclaredTypeDescriptor(expression.resolveTypeBinding()),
          JdtUtils.createMethodDescriptor(
              expression.resolveTypeBinding().getFunctionalInterfaceMethod()),
          convertOuterClassReference(
              JdtUtils.findCurrentTypeBinding(expression),
              methodBinding.getDeclaringClass(),
              false),
          JdtUtils.createMethodDescriptor(methodBinding),
          true);
    }

    /**
     * Creates a FunctionExpression described by {@code functionalMethodDescriptor} that forwards to
     * {@code targetMethodDescriptor}.
     */
    private FunctionExpression createForwardingFunctionExpression(
        SourcePosition sourcePosition,
        TypeDescriptor expressionTypeDescriptor,
        MethodDescriptor functionalMethodDescriptor,
        Expression qualifier,
        MethodDescriptor targetMethodDescriptor,
        boolean isStaticDispatch) {

      List<Variable> parameters =
          AstUtils.createParameterVariables(
              functionalMethodDescriptor.getParameterTypeDescriptors());

      List<Variable> forwardingParameters = parameters;
      if (!targetMethodDescriptor.isStatic()
          && (qualifier == null || qualifier instanceof JavaScriptConstructorReference)) {
        // The qualifier for the instance method becomes the first parameter. Method references to
        // instance methods without an explicit qualifier use the first parameter in the functional
        // interface as the qualifier for the method call.
        checkArgument(
            parameters.size() == targetMethodDescriptor.getParameterTypeDescriptors().size() + 1);
        qualifier = parameters.get(0).getReference();
        forwardingParameters = parameters.subList(1, parameters.size());
      }

      Statement forwardingStatement =
          AstUtils.createForwardingStatement(
              sourcePosition,
              qualifier,
              targetMethodDescriptor,
              isStaticDispatch,
              forwardingParameters,
              functionalMethodDescriptor.getReturnTypeDescriptor());
      forwardingStatement.setSourcePosition(sourcePosition);
      return FunctionExpression.newBuilder()
          .setTypeDescriptor(expressionTypeDescriptor)
          .setParameters(parameters)
          .setStatements(forwardingStatement)
          .setSourcePosition(sourcePosition)
          .build();
    }

    private AssertStatement convert(org.eclipse.jdt.core.dom.AssertStatement statement) {
      return new AssertStatement(
          getSourcePosition(statement),
          convert(statement.getExpression()),
          convertOrNull(statement.getMessage()));
    }

    private BinaryExpression convert(org.eclipse.jdt.core.dom.Assignment expression) {
      return BinaryExpression.newBuilder()
          .setLeftOperand(convert(expression.getLeftHandSide()))
          .setOperator(JdtUtils.getBinaryOperator(expression.getOperator()))
          .setRightOperand(convert(expression.getRightHandSide()))
          .build();
    }

    private Block convert(org.eclipse.jdt.core.dom.Block block) {
      List<org.eclipse.jdt.core.dom.Statement> statements =
          JdtUtils.asTypedList(block.statements());

      return new Block(
          getSourcePosition(block),
          statements
              .stream()
              .map(this::convert)
              .filter(Predicates.notNull())
              .collect(toImmutableList()));
    }

    private CatchClause convert(org.eclipse.jdt.core.dom.CatchClause catchClause) {
      // Order is important here, exception declaration must be converted before body.
      return new CatchClause(convert(catchClause.getException()), convert(catchClause.getBody()));
    }

    private ExpressionStatement convert(org.eclipse.jdt.core.dom.ConstructorInvocation statement) {
      IMethodBinding constructorBinding = statement.resolveConstructorBinding();
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(constructorBinding);
      return MethodCall.Builder.from(methodDescriptor)
          .setArguments(
              convertArguments(constructorBinding, JdtUtils.asTypedList(statement.arguments())))
          .build()
          .makeStatement(getSourcePosition(statement));
    }

    private Statement convert(org.eclipse.jdt.core.dom.ExpressionStatement statement) {
      return convert(statement.getExpression()).makeStatement(getSourcePosition(statement));
    }

    private FieldAccess convert(org.eclipse.jdt.core.dom.SuperFieldAccess expression) {
      IVariableBinding variableBinding = expression.resolveFieldBinding();
      FieldDescriptor fieldDescriptor = JdtUtils.createFieldDescriptor(variableBinding);
      Expression qualifier =
          convertOuterClassReference(
              JdtUtils.findCurrentTypeBinding(expression),
              variableBinding.getDeclaringClass(),
              false);

      return FieldAccess.Builder.from(fieldDescriptor).setQualifier(qualifier).build();
    }

    private FieldAccess convert(org.eclipse.jdt.core.dom.FieldAccess expression) {
      Expression qualifier = convert(expression.getExpression());
      IVariableBinding variableBinding = expression.resolveFieldBinding();
      FieldDescriptor fieldDescriptor = JdtUtils.createFieldDescriptor(variableBinding);
      // If the field is referenced like a current type field with explicit 'this' but is part of
      // some other type. (It may happen in lambda, where 'this' refers to the enclosing instance).
      if (qualifier instanceof ThisReference
          && !fieldDescriptor.isMemberOf(currentType.getTypeDescriptor())) {
        qualifier =
            convertOuterClassReference(
                JdtUtils.findCurrentTypeBinding(expression),
                variableBinding.getDeclaringClass(),
                false);
      }
      return FieldAccess.Builder.from(fieldDescriptor).setQualifier(qualifier).build();
    }

    private BinaryExpression convert(org.eclipse.jdt.core.dom.InfixExpression expression) {
      Expression leftOperand = convert(expression.getLeftOperand());
      Expression rightOperand = convert(expression.getRightOperand());
      BinaryOperator operator = JdtUtils.getBinaryOperator(expression.getOperator());

      checkArgument(!expression.hasExtendedOperands() || operator.isLeftAssociative());

      BinaryExpression binaryExpression =
          BinaryExpression.newBuilder()
              .setLeftOperand(leftOperand)
              .setOperator(operator)
              .setRightOperand(rightOperand)
              .build();

      for (Object object : expression.extendedOperands()) {
        org.eclipse.jdt.core.dom.Expression extendedOperand =
            (org.eclipse.jdt.core.dom.Expression) object;
        binaryExpression =
            BinaryExpression.newBuilder()
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

      if (methodInvocation.getExpression() != null) {
        return convert(methodInvocation.getExpression());
      }

      // No qualifier specified.
      IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
      if (JdtUtils.isStatic(methodBinding)) {
        return null;
      }

      // Not static so has to be a reference to 'this
      return convertOuterClassReference(
          JdtUtils.findCurrentTypeBinding(methodInvocation),
          methodBinding.getDeclaringClass(),
          false);
    }

    private Expression convert(org.eclipse.jdt.core.dom.MethodInvocation methodInvocation) {

      Expression qualifier = getExplicitQualifier(methodInvocation);

      IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
      List<Expression> arguments =
          convertArguments(methodBinding, JdtUtils.asTypedList(methodInvocation.arguments()));
      MethodCall methodCall =
          MethodCall.Builder.from(methodDescriptor)
              .setQualifier(qualifier)
              .setArguments(arguments)
              .build();
      if (JdtUtils.hasUncheckedCastAnnotation(methodBinding)) {
        // Annotate the invocation with the expected type. When InsertErasureSureTypeSafetyCasts
        // runs, this invocation will be skipped as it will no longer be an assignment context.
        return JsDocCastExpression.newBuilder()
            .setExpression(methodCall)
            .setCastType(methodDescriptor.getReturnTypeDescriptor())
            .build();
      }
      return methodCall;
    }

    private MethodCall convert(org.eclipse.jdt.core.dom.SuperMethodInvocation expression) {
      IMethodBinding methodBinding = expression.resolveMethodBinding();

      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
      List<Expression> arguments =
          convertArguments(methodBinding, JdtUtils.asTypedList(expression.arguments()));
      if (expression.getQualifier() == null) {
        return MethodCall.Builder.from(methodDescriptor)
            .setQualifier(new SuperReference(currentType.getSuperTypeDescriptor()))
            .setArguments(arguments)
            .build();
      } else if (expression.getQualifier().resolveTypeBinding().isInterface()) {
        // This is a default method call.
        return MethodCall.Builder.from(methodDescriptor)
            .setQualifier(new ThisReference(methodDescriptor.getEnclosingTypeDescriptor()))
            .setArguments(arguments)
            .setStaticDispatch(true)
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
            .setStaticDispatch(true)
            .build();
      }
    }

    private List<Expression> convertArguments(
        IMethodBinding methodBinding,
        List<org.eclipse.jdt.core.dom.Expression> argumentExpressions) {
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
      List<Expression> arguments =
          argumentExpressions.stream().map(this::convert).collect(toList());
      AstUtils.maybePackageVarargs(methodDescriptor, arguments);
      return arguments;
    }

    private NumberLiteral convert(org.eclipse.jdt.core.dom.NumberLiteral literal) {
      Number constantValue = (Number) literal.resolveConstantExpressionValue();
      if (constantValue instanceof Float) {
        constantValue = constantValue.doubleValue();
      }
      return new NumberLiteral(
          (PrimitiveTypeDescriptor) JdtUtils.createTypeDescriptor(literal.resolveTypeBinding()),
          constantValue);
    }

    private Expression convert(org.eclipse.jdt.core.dom.ParenthesizedExpression expression) {
      // Preserve the parenthesis. J2CL does not yet handle properly parenthesizing the output
      // according to operator precedence.
      return MultiExpression.newBuilder()
          .setExpressions(convert(expression.getExpression()))
          .build();
    }

    private UnaryExpression convert(org.eclipse.jdt.core.dom.PostfixExpression expression) {
      return PostfixExpression.newBuilder()
          .setOperand(convert(expression.getOperand()))
          .setOperator(JdtUtils.getPostfixOperator(expression.getOperator()))
          .build();
    }

    private UnaryExpression convert(org.eclipse.jdt.core.dom.PrefixExpression expression) {
      return PrefixExpression.newBuilder()
          .setOperand(convert(expression.getOperand()))
          .setOperator(JdtUtils.getPrefixOperator(expression.getOperator()))
          .build();
    }

    private Expression convert(org.eclipse.jdt.core.dom.QualifiedName expression) {
      IBinding binding = expression.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        checkArgument(
            variableBinding.isField(),
            "Need to implement translation for QualifiedName that is not a field.");
        return FieldAccess.Builder.from(JdtUtils.createFieldDescriptor(variableBinding))
            .setQualifier(convert(expression.getQualifier()))
            .build();
      }

      if (binding instanceof ITypeBinding) {
        return null;
      }

      throw new RuntimeException(
          "Need to implement translation for QualifiedName that is not a variable or a type.");
    }

    private ReturnStatement convert(org.eclipse.jdt.core.dom.ReturnStatement statement) {
      // Grab the type of the return statement from the method declaration, not from the expression.
      TypeDescriptor returnTypeDescriptor =
          JdtUtils.createTypeDescriptor(
              checkNotNull(JdtUtils.findCurrentMethodBinding(statement)).getReturnType());

      return ReturnStatement.newBuilder()
          .setExpression(convertOrNull(statement.getExpression()))
          .setTypeDescriptor(returnTypeDescriptor)
          .setSourcePosition(getSourcePosition(statement))
          .build();
    }

    /**
     * Finds and returns the descriptor of the type that encloses the variable referenced by the
     * provided binding.
     */
    private TypeDeclaration findEnclosingTypeDeclaration(IVariableBinding variableBinding) {
      // The binding is for a local variable in a static or instance block. JDT does not allow for
      // retrieving the enclosing class. Answer the question from information we've been gathering
      // while processing the compilation unit.
      Type type = enclosingTypeByVariable.get(variableByJdtBinding.get(variableBinding));
      if (type != null) {
        return type.getDeclaration();
      }
      // The binding is a simple field and JDT provides direct knowledge of the declaring class.
      if (variableBinding.getDeclaringClass() != null) {
        return JdtUtils.createDeclarationForType(variableBinding.getDeclaringClass());
      }
      // The binding is a local variable or parameter in method. JDT provides an indirect path to
      // the enclosing class.
      if (variableBinding.getDeclaringMethod() != null) {
        return JdtUtils.createDeclarationForType(
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
              && !fieldDescriptor.isMemberOf(currentType.getTypeDescriptor())) {
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
          TypeDeclaration enclosingTypeDeclaration = findEnclosingTypeDeclaration(variableBinding);
          TypeDeclaration currentTypeDeclaration = currentType.getDeclaration();
          if (!enclosingTypeDeclaration.equals(currentTypeDeclaration)) {
            return convertCapturedVariableReference(variable, enclosingTypeDeclaration);
          } else {
            return variable.getReference();
          }
        }
      }

      if (binding instanceof ITypeBinding) {
        return null;
      }
      // TODO(rluble): to be implemented
      throw new RuntimeException(
          "Need to implement translation for SimpleName binding: "
              + expression.getClass().getName());
    }

    /**
     * Convert A.this to corresponding field access knowing A's type is {@code outerTypeBinding}.
     *
     * <p>In the case of outside a constructor, if A is the direct enclosing class, A.this =>
     * this.f_$outer_this. if A is the enclosing class of the direct enclosing class, A.this =>
     * this.f_$outer_this$.f_$outer_this.
     *
     * <p>In the case of inside a constructor, the above two cases should be translated to
     * $outer_this and $outer_this.f_outer$, $outer_this is the added parameter of the constructor.
     *
     * <p>In the context where this function is called, {@code typeBinding} may be concretely
     * resolved, or it may be inferred by method call or field access. In the first case, it does a
     * 'strict' check, meaning that {@typeBinding} is the exact type that should be found in the
     * type stack. In the second case, it does a 'non-strict' check, meaning that {@typeBinding} is
     * or is a super type of one type in the stack.
     */
    private Expression convertOuterClassReference(
        ITypeBinding currentTypeBinding, ITypeBinding outerTypeBinding, boolean strict) {
      DeclaredTypeDescriptor currentTypeDescriptor = currentType.getTypeDescriptor();
      Expression qualifier = new ThisReference(currentTypeDescriptor);
      ITypeBinding innerTypeBinding = currentTypeBinding;
      if (!JdtUtils.createTypeDescriptor(innerTypeBinding).hasSameRawType(currentTypeDescriptor)) {
        // currentType is a lambda type.
        qualifier =
            FieldAccess.Builder.from(
                    AstUtils.getFieldDescriptorForEnclosingInstance(
                        currentTypeDescriptor,
                        currentType.getEnclosingTypeDeclaration().toUnparamterizedTypeDescriptor()))
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

        DeclaredTypeDescriptor enclosingTypeDescriptor =
            JdtUtils.createDeclaredTypeDescriptor(innerTypeBinding);
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
        Variable variable, TypeDeclaration enclosingClassDeclarationDescriptor) {
      // the variable is declared outside current type, i.e. a captured variable to current
      // type, and also a captured variable to the outer class in the type stack that is
      // inside {@code enclosingClassRef}.
      for (int i = typeStack.size() - 1; i >= 0; i--) {
        if (typeStack.get(i).getDeclaration().equals(enclosingClassDeclarationDescriptor)) {
          break;
        }
        capturesByTypeName.put(
            typeStack.get(i).getDeclaration().getQualifiedSourceName(), variable);
      }
      // for reference to a captured variable, if it is in a constructor, translate to
      // reference to outer parameter, otherwise, translate to reference to corresponding
      // field created for the captured variable.
      DeclaredTypeDescriptor currentTypeDescriptor = currentType.getTypeDescriptor();
      FieldDescriptor fieldDescriptor =
          AstUtils.getFieldDescriptorForCapture(currentTypeDescriptor, variable);
      ThisReference qualifier = new ThisReference(currentTypeDescriptor);
      return FieldAccess.Builder.from(fieldDescriptor).setQualifier(qualifier).build();
    }

    private Variable convert(
        org.eclipse.jdt.core.dom.SingleVariableDeclaration variableDeclaration) {
      Variable variable = createVariable(variableDeclaration);
      if (variableDeclaration.getType() instanceof org.eclipse.jdt.core.dom.UnionType) {
        // Union types are only relevant in multi catch variable declarations, which appear in the
        // AST as a SingleVariableDeclaration.
        variable.setTypeDescriptor(
            convert((org.eclipse.jdt.core.dom.UnionType) variableDeclaration.getType()));
      }
      return variable;
    }

    private Variable createVariable(VariableDeclaration variableDeclaration) {
      IVariableBinding variableBinding = variableDeclaration.resolveBinding();
      Variable variable =
          JdtUtils.createVariable(
              getSourcePosition(variableBinding.getName(), variableDeclaration.getName()),
              variableBinding);
      variableByJdtBinding.put(variableBinding, variable);
      recordEnclosingType(variable, currentType);
      return variable;
    }

    private StringLiteral convert(org.eclipse.jdt.core.dom.StringLiteral literal) {
      return new StringLiteral(literal.getEscapedValue());
    }

    private SwitchCase convert(org.eclipse.jdt.core.dom.SwitchCase statement) {
      return statement.isDefault()
          ? new SwitchCase(getSourcePosition(statement))
          : new SwitchCase(getSourcePosition(statement), convert(statement.getExpression()));
    }

    private SwitchStatement convert(org.eclipse.jdt.core.dom.SwitchStatement statement) {
      return new SwitchStatement(
          getSourcePosition(statement),
          convert(statement.getExpression()),
          convertStatements(JdtUtils.asTypedList(statement.statements())));
    }

    private SynchronizedStatement convert(
        org.eclipse.jdt.core.dom.SynchronizedStatement statement) {
      return new SynchronizedStatement(
          getSourcePosition(statement),
          convert(statement.getExpression()),
          convert(statement.getBody()));
    }

    private ExpressionStatement convert(
        org.eclipse.jdt.core.dom.SuperConstructorInvocation expression) {
      IMethodBinding superConstructorBinding = expression.resolveConstructorBinding();
      ITypeBinding superclassBinding = superConstructorBinding.getDeclaringClass();
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(superConstructorBinding);
      List<Expression> arguments =
          convertArguments(superConstructorBinding, JdtUtils.asTypedList(expression.arguments()));
      Expression qualifier = convertOrNull(expression.getExpression());
      // super() call to an inner class without explicit qualifier, find the enclosing instance.
      if (qualifier == null && JdtUtils.capturesEnclosingInstance(superclassBinding)) {
        qualifier =
            convertOuterClassReference(
                JdtUtils.findCurrentTypeBinding(expression),
                superclassBinding.getDeclaringClass(),
                false);
      }
      return MethodCall.Builder.from(methodDescriptor)
          .setQualifier(qualifier)
          .setArguments(arguments)
          .build()
          .makeStatement(getSourcePosition(expression));
    }

    private Expression convert(org.eclipse.jdt.core.dom.ThisExpression expression) {
      return convertOuterClassReference(
          JdtUtils.findCurrentTypeBinding(expression), expression.resolveTypeBinding(), true);
    }

    private Expression convert(org.eclipse.jdt.core.dom.TypeLiteral literal) {
      ITypeBinding typeBinding = literal.getType().resolveBinding();

      TypeDescriptor literalTypeDescriptor = JdtUtils.createTypeDescriptor(typeBinding);
      DeclaredTypeDescriptor parameterizedJavaLangClassTypeDescriptor =
          JdtUtils.createDeclaredTypeDescriptor(literal.resolveTypeBinding());

      if (literalTypeDescriptor.isArray()) {
        return convertArrayTypeLiteral(
            (ArrayTypeDescriptor) literalTypeDescriptor, parameterizedJavaLangClassTypeDescriptor);
      }

      if (literalTypeDescriptor.isNative()) {
        // class literal of native JsType is JavaScriptObject.class
        return convertTypeLiteral(
            TypeDescriptors.BootstrapType.JAVA_SCRIPT_OBJECT.getDescriptor(),
            parameterizedJavaLangClassTypeDescriptor);
      }

      if (literalTypeDescriptor.isJsFunctionInterface()
          || literalTypeDescriptor.isJsFunctionImplementation()) {
        // class literal for JsFunction interfaces and implementations.
        return convertTypeLiteral(
            TypeDescriptors.BootstrapType.JAVA_SCRIPT_FUNCTION.getDescriptor(),
            parameterizedJavaLangClassTypeDescriptor);
      }
      return convertTypeLiteral(literalTypeDescriptor, parameterizedJavaLangClassTypeDescriptor);
    }

    private Expression convertTypeLiteral(
        TypeDescriptor literalTypeDescriptor,
        DeclaredTypeDescriptor parameterizedJavaLangClassTypeDescriptor) {
      // Class.$get(constructor)
      return RuntimeMethods.createClassGetMethodCall(
          parameterizedJavaLangClassTypeDescriptor,
          AstUtils.getMetadataConstructorReference(literalTypeDescriptor));
    }

    private Expression convertArrayTypeLiteral(
        ArrayTypeDescriptor literalTypeDescriptor,
        DeclaredTypeDescriptor parameterizedJavaLangClassTypeDescriptor) {
      if (literalTypeDescriptor.isUntypedArray()) {
        // class literal of native js type array returns Object[].class
        literalTypeDescriptor = TypeDescriptors.get().javaLangObjectArray;
      }

      // Class.$get(leafConstructor, dimenstions)
      return RuntimeMethods.createClassGetMethodCall(
          parameterizedJavaLangClassTypeDescriptor,
          AstUtils.getMetadataConstructorReference(literalTypeDescriptor.getLeafTypeDescriptor()),
          NumberLiteral.of(literalTypeDescriptor.getDimensions()));
    }

    private ThrowStatement convert(org.eclipse.jdt.core.dom.ThrowStatement statement) {
      return new ThrowStatement(getSourcePosition(statement), convert(statement.getExpression()));
    }

    private TryStatement convert(org.eclipse.jdt.core.dom.TryStatement statement) {
      List<org.eclipse.jdt.core.dom.VariableDeclarationExpression> resources =
          JdtUtils.asTypedList(statement.resources());
      List<org.eclipse.jdt.core.dom.CatchClause> catchClauses =
          JdtUtils.asTypedList(statement.catchClauses());

      return new TryStatement(
          getSourcePosition(statement),
          resources.stream().map(this::convert).collect(toImmutableList()),
          convert(statement.getBody()),
          catchClauses.stream().map(this::convert).collect(toImmutableList()),
          convertOrNull(statement.getFinally()));
    }

    private TypeDescriptor convert(org.eclipse.jdt.core.dom.UnionType unionType) {
      return UnionTypeDescriptor.newBuilder()
          .setUnionTypeDescriptors(
              JdtUtils.<org.eclipse.jdt.core.dom.Type>asTypedList(unionType.types())
                  .stream()
                  .map(org.eclipse.jdt.core.dom.Type::resolveBinding)
                  .map(JdtUtils::createTypeDescriptor)
                  .collect(toImmutableList()))
          .build();
    }

    private VariableDeclarationFragment convert(
        org.eclipse.jdt.core.dom.VariableDeclarationFragment variableDeclarationFragment) {
      Variable variable = createVariable(variableDeclarationFragment);
      return new VariableDeclarationFragment(
          variable, convertOrNull(variableDeclarationFragment.getInitializer()));
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
      List<org.eclipse.jdt.core.dom.VariableDeclarationFragment> fragments =
          JdtUtils.asTypedList(statement.fragments());
      return VariableDeclarationExpression.newBuilder()
          .setVariableDeclarationFragments(
              fragments.stream().map(this::convert).collect(toImmutableList()))
          .build()
          .makeStatement(getSourcePosition(statement));
    }

    private Type createType(ITypeBinding typeBinding, ASTNode typeDeclarationNode) {
      if (typeBinding == null) {
        return null;
      }
      Visibility visibility = JdtUtils.getVisibility(typeBinding);
      TypeDeclaration typeDeclaration = JdtUtils.createDeclarationForType(typeBinding);

      Type type = new Type(getSourcePosition(typeDeclarationNode), visibility, typeDeclaration);
      type.setStatic(JdtUtils.isStatic(typeBinding));
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
        .collect(toImmutableList());
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
