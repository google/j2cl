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
package com.google.j2cl.frontend.jdt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Predicates;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLength;
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
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.LabeledStatement;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
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
import com.google.j2cl.ast.TypeLiteral;
import com.google.j2cl.ast.UnaryExpression;
import com.google.j2cl.ast.UnionTypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.ast.WhileStatement;
import com.google.j2cl.common.FilePosition;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.frontend.common.AbstractCompilationUnitBuilder;
import com.google.j2cl.frontend.common.EnumMethodsCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
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
public class CompilationUnitBuilder extends AbstractCompilationUnitBuilder {

  private class ASTConverter {
    private org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit;
    private Map<IVariableBinding, Variable> variableByJdtBinding = new HashMap<>();

    @SuppressWarnings({"cast", "unchecked"})
    private CompilationUnit convert(
        String sourceFilePath,
        org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit,
        Iterable<ITypeBinding> wellKnownTypeBindings) {
      JdtUtils.initWellKnownTypes(jdtCompilationUnit.getAST(), wellKnownTypeBindings);
      this.jdtCompilationUnit = jdtCompilationUnit;

      setCurrentSourceFile(sourceFilePath);
      String packageName = JdtUtils.getCompilationUnitPackageName(jdtCompilationUnit);
      setCurrentCompilationUnit(new CompilationUnit(sourceFilePath, packageName));
      // Records information about package-info files supplied as source code.
      if (getCurrentSourceFile().endsWith("package-info.java")
          && jdtCompilationUnit.getPackage() != null) {
        setPackageJsNamespaceFromSource(packageName, getPackageJsNamespace(jdtCompilationUnit));
      }
      for (Object object : jdtCompilationUnit.types()) {
        AbstractTypeDeclaration abstractTypeDeclaration = (AbstractTypeDeclaration) object;
        convert(abstractTypeDeclaration);
      }

      return getCurrentCompilationUnit();
    }

    private String getPackageJsNamespace(
        org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit) {
      List<Annotation> packageAnnotations =
          JdtUtils.asTypedList(jdtCompilationUnit.getPackage().annotations());
      if (packageAnnotations == null) {
        return null;
      }

      return packageAnnotations.stream()
          .map(Annotation::resolveAnnotationBinding)
          .filter(JsInteropAnnotationUtils::isJsPackageAnnotation)
          .findFirst()
          .map(JsInteropAnnotationUtils::getJsNamespace)
          .orElse(null);
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
          throw internalCompilerError(
              "Unexpected node type for AbstractTypeDeclaration: %s  type name: %s ",
              typeDeclaration.getClass().getName(), typeDeclaration.getName().toString());
      }
    }

    private void convert(EnumDeclaration enumDeclaration) {
      convertAndAddType(
          enumDeclaration,
          enumType -> {
            checkState(enumType.isEnum());

            int ordinal = 0;
            for (EnumConstantDeclaration enumConstantDeclaration :
                JdtUtils.<EnumConstantDeclaration>asTypedList(enumDeclaration.enumConstants())) {
              enumType.addField(ordinal, convert(enumConstantDeclaration));
              ordinal++;
            }
            EnumMethodsCreator.applyTo(enumType);
            return null;
          });
    }

    private void convertAndAddType(AbstractTypeDeclaration typeDeclaration) {
      convertAndAddType(typeDeclaration, type -> null);
    }

    private void convertAndAddType(
        AbstractTypeDeclaration typeDeclaration, Function<Type, Void> typeProcessor) {
      convertAndAddType(
          typeDeclaration.resolveBinding(),
          JdtUtils.asTypedList(typeDeclaration.bodyDeclarations()),
          typeDeclaration.getName(),
          typeProcessor);
    }

    /**
     * Constructs a type, maintains the type stack and let's the caller to do additional work by
     * supplying a {@code typeProcessor}.
     *
     * @return {T} the value returned by {@code typeProcessor}
     */
    private <T> T convertAndAddType(
        ITypeBinding typeBinding,
        List<BodyDeclaration> bodyDeclarations,
        ASTNode sourcePositionNode,
        Function<Type, T> typeProcessor) {
      Type type = createType(typeBinding, sourcePositionNode);
      pushType(type);
      getCurrentCompilationUnit().addType(type);
      convertTypeBody(type, bodyDeclarations);
      T result = typeProcessor.apply(type);
      popType();
      return result;
    }

    private void convertTypeBody(Type type, List<BodyDeclaration> bodyDeclarations) {
      TypeDeclaration currentTypeDeclaration = type.getDeclaration();
      propagateCapturesFromSupertype(currentTypeDeclaration);
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
          throw internalCompilerError(
              "Unexpected type for BodyDeclaration: %s, in type: %s",
              bodyDeclaration.getClass().getName(), type.getDeclaration().getQualifiedSourceName());
        }
      }

      for (Variable capturedVariable : getCapturedVariables(currentTypeDeclaration)) {
        FieldDescriptor fieldDescriptor =
            AstUtils.getFieldDescriptorForCapture(type.getTypeDescriptor(), capturedVariable);
        type.addField(
            Field.Builder.from(fieldDescriptor)
                .setCapturedVariable(capturedVariable)
                .setSourcePosition(type.getSourcePosition())
                .setNameSourcePosition(capturedVariable.getSourcePosition())
                .build());
      }
      if (type.getDeclaration().isCapturingEnclosingInstance()) {
        // add field for enclosing instance.
        type.addField(
            0,
            Field.Builder.from(
                    AstUtils.getFieldDescriptorForEnclosingInstance(
                        type.getTypeDescriptor(),
                        type.getEnclosingTypeDeclaration().toUnparameterizedTypeDescriptor()))
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

      FieldDescriptor fieldDescriptor =
          JdtUtils.createFieldDescriptor(enumConstantDeclaration.resolveVariable());

      // Since initializing custom values for JsEnum requires literals, we fold constant expressions
      // to give more options to the user. E.g. -1 is a unary expression but the expression is a
      // constant that can be evaluated at compile time, hence it makes sense to allow it.
      boolean foldConstantArguments = fieldDescriptor.getEnclosingTypeDescriptor().isJsEnum();

      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(enumConstructorBinding);
      Expression initializer =
          NewInstance.Builder.from(methodDescriptor)
              .setArguments(
                  convertArguments(
                      enumConstructorBinding,
                      JdtUtils.asTypedList(enumConstantDeclaration.arguments()),
                      foldConstantArguments))
              .build();

      checkArgument(fieldDescriptor.isEnumConstant());
      return Field.Builder.from(fieldDescriptor)
          .setInitializer(initializer)
          .setSourcePosition(getSourcePosition(enumConstantDeclaration))
          .setNameSourcePosition(Optional.of(getSourcePosition(enumConstantDeclaration.getName())))
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
          initializer =
              convertConstantToLiteral(
                  variableBinding.getConstantValue(),
                  JdtUtils.createTypeDescriptor(variableBinding.getType()));
        }
        Field field =
            Field.Builder.from(JdtUtils.createFieldDescriptor(variableBinding))
                .setInitializer(initializer)
                .setSourcePosition(getSourcePosition(fieldDeclaration))
                .setNameSourcePosition(Optional.of(getSourcePosition(fragment.getName())))
                .build();
        fields.add(field);
      }
      return fields;
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
              ? Block.newBuilder().setSourcePosition(getSourcePosition(methodDeclaration)).build()
              : convert(methodDeclaration.getBody());

      return newMethodBuilder(methodDeclaration.resolveBinding())
          .setSourcePosition(getSourcePosition(methodDeclaration.getName()))
          .setParameters(parameters)
          .addStatements(body.getStatements())
          .build();
    }

    private Method convert(AnnotationTypeMemberDeclaration memberDeclaration) {
      return newMethodBuilder(memberDeclaration.resolveBinding())
          .setSourcePosition(getSourcePosition(memberDeclaration))
          .build();
    }

    private Method.Builder newMethodBuilder(IMethodBinding methodBinding) {
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
      // TODO(b/31312257): fix or decide to not emit @override and suppress the error.
      boolean isOverride =
          methodDescriptor
              .getOverriddenMethodDescriptors()
              .stream()
              .anyMatch(m -> requiresOverrideAnnotation(methodDescriptor, m));
      return Method.newBuilder().setMethodDescriptor(methodDescriptor).setOverride(isOverride);
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

    private NumberLiteral convert(org.eclipse.jdt.core.dom.CharacterLiteral literal) {
      return NumberLiteral.fromChar(literal.charValue());
    }

    private Expression convert(org.eclipse.jdt.core.dom.ClassInstanceCreation expression) {
      boolean isAnonymousClassCreation = expression.getAnonymousClassDeclaration() != null;
      if (isAnonymousClassCreation) {
        return convertAnonymousClassCreation(expression);
      } else {
        return convertInstanceCreation(expression);
      }
    }

    private MethodDescriptor convertAnonymousClassDeclaration(
        AnonymousClassDeclaration typeDeclaration,
        IMethodBinding constructorBinding,
        TypeDescriptor superQualifierTypeDescriptor) {

      return convertAndAddType(
          typeDeclaration.resolveBinding(),
          JdtUtils.asTypedList(typeDeclaration.bodyDeclarations()),
          typeDeclaration,
          type -> {
            // The initial constructor descriptor does not include the super call qualifier.
            MethodDescriptor constructorDescriptor =
                JdtUtils.createMethodDescriptor(constructorBinding);

            // Find the corresponding superconstructor, the ClassInstanceCreation construct does not
            // have a reference the super constructor that needs to be called. But the synthetic
            // anonymous class constructor is a subsignature of the corresponding super constructor.
            IMethodBinding superConstructorBinding =
                Arrays.stream(typeDeclaration.resolveBinding().getSuperclass().getDeclaredMethods())
                    .filter(IMethodBinding::isConstructor)
                    .filter(constructorBinding::isSubsignature)
                    .findFirst()
                    .orElse(constructorBinding);
            MethodDescriptor superConstructorDescriptor =
                MethodDescriptor.Builder.from(
                        JdtUtils.createMethodDescriptor(superConstructorBinding))
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
            return constructorDescriptor;
          });
    }

    private Expression convertAnonymousClassCreation(
        org.eclipse.jdt.core.dom.ClassInstanceCreation expression) {
      IMethodBinding constructorBinding = expression.resolveConstructorBinding();

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
      DeclaredTypeDescriptor anonymousClassTypeDescriptor =
          constructorDescriptor.getEnclosingTypeDescriptor();
      Expression newInstanceQualifier =
          anonymousClassTypeDescriptor.getTypeDeclaration().isCapturingEnclosingInstance()
              ? resolveImplicitOuterClassReference(
                  anonymousClassTypeDescriptor.getEnclosingTypeDescriptor())
              : null;
      return NewInstance.Builder.from(constructorDescriptor)
          .setQualifier(newInstanceQualifier)
          .setArguments(arguments)
          .build();
    }

    private Expression convertInstanceCreation(
        org.eclipse.jdt.core.dom.ClassInstanceCreation expression) {

      IMethodBinding constructorBinding = expression.resolveConstructorBinding();
      List<Expression> arguments =
          convertArguments(constructorBinding, JdtUtils.asTypedList(expression.arguments()));

      MethodDescriptor constructorMethodDescriptor =
          JdtUtils.createMethodDescriptor(constructorBinding);
      DeclaredTypeDescriptor targetTypeDescriptor =
          constructorMethodDescriptor.getEnclosingTypeDescriptor();

      // Instantiation implicitly references all captured variables since in the flat class model
      // captures become fields and need to be threaded through the constructors.
      // This is crucial to cover some corner cases where the capture is never referenced in the
      // class nor its superclasses but is implicitly referenced by invoking a constructor of
      // the capturing class.
      propagateAllCapturesOutward(targetTypeDescriptor.getTypeDeclaration());

      Expression qualifier = convertOrNull(expression.getExpression());
      checkArgument(!targetTypeDescriptor.getTypeDeclaration().isAnonymous());
      qualifier = resolveInstantiationQualifier(qualifier, targetTypeDescriptor);

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
          throw internalCompilerError(
              "Unexpected type for Expression: %s", expression.getClass().getName());
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

    private ConditionalExpression convert(
        org.eclipse.jdt.core.dom.ConditionalExpression conditionalExpression) {
      return ConditionalExpression.newBuilder()
          .setTypeDescriptor(
              JdtUtils.createTypeDescriptor(conditionalExpression.resolveTypeBinding()))
          .setConditionExpression(convert(conditionalExpression.getExpression()))
          .setTrueExpression(convert(conditionalExpression.getThenExpression()))
          .setFalseExpression(convert(conditionalExpression.getElseExpression()))
          .build();
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
          throw internalCompilerError(
              "Unexpected type for Statement: %s", statement.getClass().getName());
      }
    }

    public SourcePosition getSourcePosition(ASTNode node) {
      return getSourcePosition(null, node);
    }

    public SourcePosition getSourcePosition(String name, ASTNode node) {
      int startCharacterPosition = node.getStartPosition();
      int startLine = jdtCompilationUnit.getLineNumber(startCharacterPosition) - 1;
      int startColumn = jdtCompilationUnit.getColumnNumber(startCharacterPosition);
      int endCharacterPosition = startCharacterPosition + node.getLength() - 1;
      int endLine = jdtCompilationUnit.getLineNumber(endCharacterPosition) - 1;
      int endColumn = jdtCompilationUnit.getColumnNumber(endCharacterPosition) + 1;
      return SourcePosition.newBuilder()
          .setFilePath(getCurrentCompilationUnit().getFilePath())
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

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    private <T extends Statement> T convertOrNull(org.eclipse.jdt.core.dom.Statement statement) {
      return statement != null ? (T) convert(statement) : null;
    }

    private Statement convert(org.eclipse.jdt.core.dom.Statement statement) {
      return convertStatement(statement);
    }

    private LabeledStatement convert(org.eclipse.jdt.core.dom.LabeledStatement statement) {
      return LabeledStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setLabel(statement.getLabel().getIdentifier())
          .setStatement(convert(statement.getBody()))
          .build();
    }

    private BreakStatement convert(org.eclipse.jdt.core.dom.BreakStatement statement) {
      return BreakStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setLabel(getIdentifierOrNull(statement.getLabel()))
          .build();
    }

    private ContinueStatement convert(org.eclipse.jdt.core.dom.ContinueStatement statement) {
      return ContinueStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setLabel(getIdentifierOrNull(statement.getLabel()))
          .build();
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
                  ArrayLength.newBuilder().setArrayExpression(arrayVariable.getReference()).build())
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
                  .addVariableDeclaration(indexVariable, NumberLiteral.fromInt(0))
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
      return DoWhileStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setConditionExpression(convert(statement.getExpression()))
          .setBody(convert(statement.getBody()))
          .build();
    }

    private Statement convert(
        org.eclipse.jdt.core.dom.TypeDeclarationStatement typeDeclarationStatement) {
      convert(typeDeclarationStatement.getDeclaration());
      return null;
    }

    private WhileStatement convert(org.eclipse.jdt.core.dom.WhileStatement statement) {
      return WhileStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setConditionExpression(convert(statement.getExpression()))
          .setBody(convert(statement.getBody()))
          .build();
    }

    private IfStatement convert(org.eclipse.jdt.core.dom.IfStatement statement) {
      return IfStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setConditionExpression(convert(statement.getExpression()))
          .setThenStatement(convert(statement.getThenStatement()))
          .setElseStatement(convertOrNull(statement.getElseStatement()))
          .build();
    }

    private InstanceOfExpression convert(org.eclipse.jdt.core.dom.InstanceofExpression expression) {
      return InstanceOfExpression.newBuilder()
          .setSourcePosition(getSourcePosition(expression))
          .setExpression(convert(expression.getLeftOperand()))
          .setTestTypeDescriptor(
              JdtUtils.createTypeDescriptor(expression.getRightOperand().resolveBinding()))
          .build();
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
     * <pre>
     *          q::m    into    (par1, ..., parN) -> q.m(par1, ..., parN)  or
     *                          (let $q = q, (par1, ..., parN) -> $q.m(par1, ..., parN))
     * </pre>
     *
     * <p>Depending on whether the qualifier can be evaluated inside the functional expression
     * preserving semantics.
     */
    private Expression convert(ExpressionMethodReference expression) {

      SourcePosition sourcePosition = getSourcePosition(expression);
      TypeDescriptor expressionTypeDescriptor =
          JdtUtils.createTypeDescriptor(expression.resolveTypeBinding());

      // MethodDescriptor target of the method reference.
      MethodDescriptor referencedMethodDescriptor =
          JdtUtils.createMethodDescriptor(expression.resolveMethodBinding());

      // Functional interface method that the expression implements.
      MethodDescriptor functionalMethodDescriptor =
          JdtUtils.createMethodDescriptor(
              expression.resolveTypeBinding().getFunctionalInterfaceMethod());

      Expression qualifier = convert(expression.getExpression());

      return createMethodReferenceLambda(
          sourcePosition,
          qualifier,
          referencedMethodDescriptor,
          expressionTypeDescriptor,
          functionalMethodDescriptor);
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

      // There are 3 flavors for CreationReferences: 1) array creations, 2) unqualified
      // constructors, and 3) implicitly qualified constructors.

      // If the expression does not resolve, it is an array creation.
      SourcePosition sourcePosition = getSourcePosition(expression);
      if (expression.resolveMethodBinding() == null) {

        return createArrayCreationLambda(
            functionalMethodDescriptor,
            (ArrayTypeDescriptor) JdtUtils.createTypeDescriptor(expressionTypeBinding),
            sourcePosition);
      }

      MethodDescriptor targetConstructorMethodDescriptor =
          JdtUtils.createMethodDescriptor(expression.resolveMethodBinding());

      return createInstantiationLambda(
          functionalMethodDescriptor, targetConstructorMethodDescriptor, null, sourcePosition);
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
      return createMethodReferenceLambda(
          getSourcePosition(expression),
          null,
          JdtUtils.createMethodDescriptor(expression.resolveMethodBinding()),
          JdtUtils.createDeclaredTypeDescriptor(expressionTypeBinding),
          JdtUtils.createMethodDescriptor(expressionTypeBinding.getFunctionalInterfaceMethod()));
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
      MethodDescriptor methodDescriptor =
          JdtUtils.createMethodDescriptor(expression.resolveMethodBinding());

      return AbstractCompilationUnitBuilder.createForwardingFunctionExpression(
          getSourcePosition(expression),
          JdtUtils.createDeclaredTypeDescriptor(expression.resolveTypeBinding()),
          JdtUtils.createMethodDescriptor(
              expression.resolveTypeBinding().getFunctionalInterfaceMethod()),
          resolveImplicitOuterClassReference(methodDescriptor.getEnclosingTypeDescriptor()),
          methodDescriptor,
          true);
    }

    private AssertStatement convert(org.eclipse.jdt.core.dom.AssertStatement statement) {
      return AssertStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setExpression(convert(statement.getExpression()))
          .setMessage(convertOrNull(statement.getMessage()))
          .build();
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

      return Block.newBuilder()
          .setSourcePosition(getSourcePosition(block))
          .setStatements(
              statements
                  .stream()
                  .map(this::convert)
                  .filter(Predicates.notNull())
                  .collect(toImmutableList()))
          .build();
    }

    private CatchClause convert(org.eclipse.jdt.core.dom.CatchClause catchClause) {
      // Order is important here, exception declaration must be converted before body.
      return CatchClause.newBuilder()
          .setExceptionVariable(convert(catchClause.getException()))
          .setBody(convert(catchClause.getBody()))
          .build();
    }

    private ExpressionStatement convert(org.eclipse.jdt.core.dom.ConstructorInvocation statement) {
      IMethodBinding constructorBinding = statement.resolveConstructorBinding();
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(constructorBinding);
      return MethodCall.Builder.from(methodDescriptor)
          .setArguments(
              convertArguments(constructorBinding, JdtUtils.asTypedList(statement.arguments())))
          .setSourcePosition(getSourcePosition(statement))
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
          resolveImplicitOuterClassReference(fieldDescriptor.getEnclosingTypeDescriptor());

      return FieldAccess.Builder.from(fieldDescriptor).setQualifier(qualifier).build();
    }

    private Expression convert(org.eclipse.jdt.core.dom.FieldAccess expression) {
      Expression qualifier = convert(expression.getExpression());
      return JdtUtils.createFieldAccess(qualifier, expression.resolveFieldBinding());
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
      return resolveImplicitOuterClassReference(
          JdtUtils.createDeclaredTypeDescriptor(methodBinding.getDeclaringClass()));
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
              .setSourcePosition(getSourcePosition(methodInvocation))
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
            .setQualifier(new SuperReference(getCurrentType().getSuperTypeDescriptor()))
            .setArguments(arguments)
            .setSourcePosition(getSourcePosition(expression))
            .build();
      } else if (expression.getQualifier().resolveTypeBinding().isInterface()) {
        // This is a default method call.
        return MethodCall.Builder.from(methodDescriptor)
            .setQualifier(new ThisReference(methodDescriptor.getEnclosingTypeDescriptor()))
            .setArguments(arguments)
            .setStaticDispatch(true)
            .setSourcePosition(getSourcePosition(expression))
            .build();
      } else {
        // OuterClass.super.fun() is transpiled to
        // SuperClassOfOuterClass.prototype.fun.call(OuterClass.this);
        return MethodCall.Builder.from(methodDescriptor)
            .setQualifier(
                resolveExplicitOuterClassReference(
                    JdtUtils.createDeclaredTypeDescriptor(
                        expression.getQualifier().resolveTypeBinding())))
            .setArguments(arguments)
            .setStaticDispatch(true)
            .setSourcePosition(getSourcePosition(expression))
            .build();
      }
    }

    private List<Expression> convertArguments(
        IMethodBinding methodBinding,
        List<org.eclipse.jdt.core.dom.Expression> argumentExpressions) {
      return convertArguments(methodBinding, argumentExpressions, false);
    }

    private List<Expression> convertArguments(
        IMethodBinding methodBinding,
        List<org.eclipse.jdt.core.dom.Expression> argumentExpressions,
        boolean foldConstants) {
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
      List<Expression> arguments =
          argumentExpressions.stream()
              .map(
                  expression ->
                      foldConstants && expression.resolveConstantExpressionValue() != null
                          ? convertConstantToLiteral(
                              expression.resolveConstantExpressionValue(),
                              JdtUtils.createTypeDescriptor(expression.resolveTypeBinding()))
                          : convert(expression))
              .collect(toList());
      AstUtils.maybePackageVarargs(methodDescriptor, arguments);
      return arguments;
    }

    private NumberLiteral convert(org.eclipse.jdt.core.dom.NumberLiteral literal) {
      Number constantValue = (Number) literal.resolveConstantExpressionValue();
      PrimitiveTypeDescriptor typeDescriptor =
          (PrimitiveTypeDescriptor) JdtUtils.createTypeDescriptor(literal.resolveTypeBinding());
      return new NumberLiteral(typeDescriptor, constantValue);
    }

    private Expression convert(org.eclipse.jdt.core.dom.ParenthesizedExpression expression) {
      // Preserve the parenthesis. J2CL does not yet handle properly parenthesizing the output
      // according to operator precedence.
      return convert(expression.getExpression()).parenthesize();
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
            internalCompilerErrorMessage("Unexpected QualifiedName that is not a field"));

        Expression qualifier = convert(expression.getQualifier());
        return JdtUtils.createFieldAccess(qualifier, variableBinding);
      }

      if (binding instanceof ITypeBinding) {
        return null;
      }

      throw internalCompilerError(
          "Unexpected type for QualifiedName binding: %s ", binding.getClass().getName());
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

    private Expression convert(org.eclipse.jdt.core.dom.SimpleName expression) {
      IBinding binding = expression.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          // It refers to a field.
          FieldDescriptor fieldDescriptor = JdtUtils.createFieldDescriptor(variableBinding);
          if (!fieldDescriptor.isStatic()
              && !fieldDescriptor.isMemberOf(getCurrentType().getTypeDescriptor())) {
            return FieldAccess.Builder.from(fieldDescriptor)
                .setQualifier(
                    resolveImplicitOuterClassReference(
                        fieldDescriptor.getEnclosingTypeDescriptor()))
                .build();
          } else {
            return FieldAccess.Builder.from(fieldDescriptor).build();
          }
        } else {
          // It refers to a local variable or parameter in a method or block.
          Variable variable = checkNotNull(variableByJdtBinding.get(variableBinding));
          return resolveVariableReference(variable);
        }
      }

      if (binding instanceof ITypeBinding) {
        return null;
      }

      throw internalCompilerError(
          "Unexpected binding class for SimpleName: %s", expression.getClass().getName());
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
      recordEnclosingType(variable, getCurrentType());
      return variable;
    }

    private StringLiteral convert(org.eclipse.jdt.core.dom.StringLiteral literal) {
      return new StringLiteral(literal.getLiteralValue());
    }

    private SwitchStatement convert(org.eclipse.jdt.core.dom.SwitchStatement switchStatement) {
      Expression switchExpression = convert(switchStatement.getExpression());

      List<SwitchCase.Builder> caseBuilders = new ArrayList<>();
      for (org.eclipse.jdt.core.dom.Statement statement :
          JdtUtils.<org.eclipse.jdt.core.dom.Statement>asTypedList(switchStatement.statements())) {
        if (statement instanceof org.eclipse.jdt.core.dom.SwitchCase) {
          caseBuilders.add(convert((org.eclipse.jdt.core.dom.SwitchCase) statement));
        } else {
          Iterables.getLast(caseBuilders).addStatement(convertStatement(statement));
        }
      }

      return SwitchStatement.newBuilder()
          .setSourcePosition(getSourcePosition(switchStatement))
          .setSwitchExpression(switchExpression)
          .setCases(caseBuilders.stream().map(SwitchCase.Builder::build).collect(toImmutableList()))
          .build();
    }

    private SwitchCase.Builder convert(org.eclipse.jdt.core.dom.SwitchCase statement) {
      return statement.isDefault()
          ? SwitchCase.newBuilder()
          : SwitchCase.newBuilder().setCaseExpression(convert(statement.getExpression()));
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
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(superConstructorBinding);
      List<Expression> arguments =
          convertArguments(superConstructorBinding, JdtUtils.asTypedList(expression.arguments()));
      Expression qualifier = convertOrNull(expression.getExpression());
      // super() call to an inner class without explicit qualifier, find the enclosing instance.
      DeclaredTypeDescriptor targetTypeDescriptor = methodDescriptor.getEnclosingTypeDescriptor();
      if (qualifier == null
          && targetTypeDescriptor.getTypeDeclaration().isCapturingEnclosingInstance()) {
        qualifier =
            resolveImplicitOuterClassReference(targetTypeDescriptor.getEnclosingTypeDescriptor());
      }
      return MethodCall.Builder.from(methodDescriptor)
          .setQualifier(qualifier)
          .setArguments(arguments)
          .setSourcePosition(getSourcePosition(expression))
          .build()
          .makeStatement(getSourcePosition(expression));
    }

    private Expression convert(org.eclipse.jdt.core.dom.ThisExpression expression) {
      if (expression.getQualifier() == null) {
        // Unqualified this reference.
        return new ThisReference(getCurrentType().getTypeDescriptor());
      }
      return resolveExplicitOuterClassReference(
          JdtUtils.createDeclaredTypeDescriptor(expression.getQualifier().resolveTypeBinding()));
    }

    private Expression convert(org.eclipse.jdt.core.dom.TypeLiteral literal) {
      ITypeBinding typeBinding = literal.getType().resolveBinding();

      TypeDescriptor literalTypeDescriptor = JdtUtils.createTypeDescriptor(typeBinding);
      return new TypeLiteral(getSourcePosition(literal), literalTypeDescriptor);
    }

    private ThrowStatement convert(org.eclipse.jdt.core.dom.ThrowStatement statement) {
      return new ThrowStatement(getSourcePosition(statement), convert(statement.getExpression()));
    }

    private TryStatement convert(org.eclipse.jdt.core.dom.TryStatement statement) {
      List<org.eclipse.jdt.core.dom.Expression> resources =
          JdtUtils.asTypedList(statement.resources());
      List<org.eclipse.jdt.core.dom.CatchClause> catchClauses =
          JdtUtils.asTypedList(statement.catchClauses());

      return TryStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setResourceDeclarations(
              resources.stream()
                  .map(this::convert)
                  .map(CompilationUnitBuilder::toResource)
                  .collect(toImmutableList()))
          .setBody(convert(statement.getBody()))
          .setCatchClauses(catchClauses.stream().map(this::convert).collect(toImmutableList()))
          .setFinallyBlock(convertOrNull(statement.getFinally()))
          .build();
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
      return VariableDeclarationFragment.newBuilder()
          .setVariable(variable)
          .setInitializer(convertOrNull(variableDeclarationFragment.getInitializer()))
          .build();
    }

    private Variable convert(org.eclipse.jdt.core.dom.VariableDeclaration variableDeclaration) {
      if (variableDeclaration instanceof org.eclipse.jdt.core.dom.SingleVariableDeclaration) {
        return convert((org.eclipse.jdt.core.dom.SingleVariableDeclaration) variableDeclaration);
      } else {
        return convert((org.eclipse.jdt.core.dom.VariableDeclarationFragment) variableDeclaration)
            .getVariable();
      }
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

    private Type createType(ITypeBinding typeBinding, ASTNode sourcePositionNode) {
      if (typeBinding == null) {
        return null;
      }
      Visibility visibility = JdtUtils.getVisibility(typeBinding);
      TypeDeclaration typeDeclaration = JdtUtils.createDeclarationForType(typeBinding);

      Type type = new Type(getSourcePosition(sourcePositionNode), visibility, typeDeclaration);
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
