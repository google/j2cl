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
package com.google.j2cl.transpiler.frontend.jdt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.FilePosition;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayCreationReference;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AssertStatement;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
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
import com.google.j2cl.transpiler.ast.Expression.Associativity;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
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
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.WhileStatement;
import com.google.j2cl.transpiler.frontend.common.AbstractCompilationUnitBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
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
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclaration;

/** Creates a J2CL Java AST from the AST provided by JDT. */
public class CompilationUnitBuilder extends AbstractCompilationUnitBuilder {

  private final JdtEnvironment environment;

  private class ASTConverter {
    private org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit;
    private final Map<IVariableBinding, Variable> variableByJdtBinding = new HashMap<>();

    // Keeps track of labels that are currently in scope. Even though labels cannot have the
    // same name if they are nested in the same method body, labels with the same name could
    // be lexically nested by being in different methods bodies, e.g. from local or anonymous
    // classes or lambdas.
    private final Map<String, Deque<Label>> labelsInScope = new HashMap<>();

    private CompilationUnit convert(
        String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit) {
      this.jdtCompilationUnit = jdtCompilationUnit;

      setCurrentSourceFile(sourceFilePath);
      PackageDeclaration packageDeclaration = jdtCompilationUnit.getPackage();
      String packageName =
          packageDeclaration == null ? "" : packageDeclaration.getName().getFullyQualifiedName();
      setCurrentCompilationUnit(CompilationUnit.createForFile(sourceFilePath, packageName));

      for (Object object : jdtCompilationUnit.types()) {
        AbstractTypeDeclaration abstractTypeDeclaration = (AbstractTypeDeclaration) object;
        getCurrentCompilationUnit().addType(convert(abstractTypeDeclaration));
      }

      return getCurrentCompilationUnit();
    }
    private Type convert(AbstractTypeDeclaration typeDeclaration) {
      switch (typeDeclaration.getNodeType()) {
        case ASTNode.ANNOTATION_TYPE_DECLARATION:
        case ASTNode.TYPE_DECLARATION:
          return convertType(typeDeclaration);
        case ASTNode.ENUM_DECLARATION:
          return convert((EnumDeclaration) typeDeclaration);
        default:
          throw internalCompilerError(
              "Unexpected node type for AbstractTypeDeclaration: %s  type name: %s ",
              typeDeclaration.getClass().getName(), typeDeclaration.getName().toString());
      }
    }

    private Type convert(EnumDeclaration enumDeclaration) {
      Type enumType = convertType(enumDeclaration);
      checkState(enumType.isEnum());

      processEnclosedBy(
          enumType,
          () -> {
            int ordinal = 0;
            for (EnumConstantDeclaration enumConstantDeclaration :
                JdtEnvironment.<EnumConstantDeclaration>asTypedList(
                    enumDeclaration.enumConstants())) {
              enumType.addMember(ordinal, convert(enumConstantDeclaration));
              ordinal++;
            }
            return null;
          });
      return enumType;
    }

    private Type convertType(AbstractTypeDeclaration typeDeclaration) {
      return convertType(
          typeDeclaration.resolveBinding(),
          JdtEnvironment.asTypedList(typeDeclaration.bodyDeclarations()),
          typeDeclaration.getName());
    }

    /**
     * Constructs a type, maintains the type stack and let's the caller to do additional work by
     * supplying a {@code typeProcessor}.
     *
     * @return {T} the value returned by {@code typeProcessor}
     */
    private Type convertType(
        ITypeBinding typeBinding,
        List<BodyDeclaration> bodyDeclarations,
        ASTNode sourcePositionNode) {
      Type type = createType(typeBinding, sourcePositionNode);
      processEnclosedBy(
          type,
          () -> {
            convertTypeBody(type, bodyDeclarations);
            return null;
          });
      return type;
    }

    private void convertTypeBody(Type type, List<BodyDeclaration> bodyDeclarations) {
      for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
        if (bodyDeclaration instanceof FieldDeclaration) {
          FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclaration;
          type.addMembers(convert(fieldDeclaration));
        } else if (bodyDeclaration instanceof MethodDeclaration) {
          MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
          type.addMember(convert(methodDeclaration));
        } else if (bodyDeclaration instanceof AnnotationTypeMemberDeclaration) {
          AnnotationTypeMemberDeclaration memberDeclaration =
              (AnnotationTypeMemberDeclaration) bodyDeclaration;
          type.addMember(convert(memberDeclaration));
        } else if (bodyDeclaration instanceof Initializer) {
          Initializer initializer = (Initializer) bodyDeclaration;
          Block block = convert(initializer.getBody());
          if (JdtEnvironment.isStatic(initializer)) {
            type.addStaticInitializerBlock(block);
          } else {
            type.addInstanceInitializerBlock(block);
          }
        } else if (bodyDeclaration instanceof AbstractTypeDeclaration) {
          // Nested class
          AbstractTypeDeclaration nestedTypeDeclaration = (AbstractTypeDeclaration) bodyDeclaration;
          type.addType(convert(nestedTypeDeclaration));
        } else {
          throw internalCompilerError(
              "Unexpected type for BodyDeclaration: %s, in type: %s",
              bodyDeclaration.getClass().getName(), type.getDeclaration().getQualifiedSourceName());
        }
      }
    }

    private Field convert(EnumConstantDeclaration enumConstantDeclaration) {
      IMethodBinding enumConstructorBinding = enumConstantDeclaration.resolveConstructorBinding();
      Type anonymousInnerClass =
          enumConstantDeclaration.getAnonymousClassDeclaration() != null
              ? convertAnonymousClassDeclaration(
                  enumConstantDeclaration.getAnonymousClassDeclaration(),
                  enumConstructorBinding,
                  null)
              : null;

      FieldDescriptor fieldDescriptor =
          environment.createFieldDescriptor(enumConstantDeclaration.resolveVariable());

      // Since initializing custom values for JsEnum requires literals, we fold constant expressions
      // to give more options to the user. E.g. -1 is a unary expression but the expression is a
      // constant that can be evaluated at compile time, hence it makes sense to allow it.
      boolean foldConstantArguments = fieldDescriptor.getEnclosingTypeDescriptor().isJsEnum();

      MethodDescriptor methodDescriptor =
          environment.createMethodDescriptor(enumConstructorBinding);
      Expression initializer =
          NewInstance.Builder.from(methodDescriptor)
              .setArguments(
                  convertArguments(
                      enumConstructorBinding,
                      JdtEnvironment.asTypedList(enumConstantDeclaration.arguments()),
                      foldConstantArguments))
              .setAnonymousInnerClass(anonymousInnerClass)
              .build();

      checkArgument(fieldDescriptor.isEnumConstant());
      return Field.Builder.from(fieldDescriptor)
          .setInitializer(initializer)
          .setSourcePosition(getSourcePosition(enumConstantDeclaration))
          .setNameSourcePosition(getSourcePosition(enumConstantDeclaration.getName()))
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
              Literal.fromValue(
                  variableBinding.getConstantValue(),
                  environment.createTypeDescriptor(variableBinding.getType()));
        }
        Field field =
            Field.Builder.from(environment.createFieldDescriptor(variableBinding))
                .setInitializer(initializer)
                .setSourcePosition(getSourcePosition(fieldDeclaration))
                .setNameSourcePosition(getSourcePosition(fragment.getName()))
                .build();
        fields.add(field);
      }
      return fields;
    }

    private Method convert(MethodDeclaration methodDeclaration) {
      boolean inNullMarkedScope = getCurrentType().getDeclaration().isNullMarked();
      List<Variable> parameters = new ArrayList<>();
      for (SingleVariableDeclaration parameter :
          JdtEnvironment.<SingleVariableDeclaration>asTypedList(methodDeclaration.parameters())) {
        parameters.add(createVariable(parameter, inNullMarkedScope));
      }

      MethodDescriptor methodDescriptor =
          environment.createMethodDescriptor(methodDeclaration.resolveBinding());

      // If a method has no body, initialize the body with an empty list of statements.
      Block body =
          methodDeclaration.getBody() == null
              ? Block.newBuilder().setSourcePosition(getSourcePosition(methodDeclaration)).build()
              : convert(methodDeclaration.getBody());

      return newMethodBuilder(methodDescriptor)
          .setBodySourcePosition(body.getSourcePosition())
          .setSourcePosition(getSourcePosition(methodDeclaration.getName()))
          .setParameters(parameters)
          .addStatements(body.getStatements())
          .build();
    }

    private Method convert(AnnotationTypeMemberDeclaration memberDeclaration) {
      MethodDescriptor methodDescriptor =
          environment.createMethodDescriptor(memberDeclaration.resolveBinding());

      return newMethodBuilder(methodDescriptor)
          .setSourcePosition(getSourcePosition(memberDeclaration))
          .build();
    }

    private Method.Builder newMethodBuilder(MethodDescriptor methodDescriptor) {
      return Method.newBuilder().setMethodDescriptor(methodDescriptor);
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
          convertExpressions(JdtEnvironment.asTypedList(expression.dimensions()));
      // Pad the dimension expressions with null values to denote omitted dimensions.
      AstUtils.addNullPadding(dimensionExpressions, arrayType.getDimensions());

      ArrayLiteral arrayLiteral =
          expression.getInitializer() == null ? null : convert(expression.getInitializer());

      ArrayTypeDescriptor typeDescriptor =
          (ArrayTypeDescriptor)
              environment.createTypeDescriptor(
                  expression.resolveTypeBinding(),
                  getCurrentType().getDeclaration().isNullMarked());
      return NewArray.newBuilder()
          .setTypeDescriptor(typeDescriptor)
          .setDimensionExpressions(dimensionExpressions)
          .setInitializer(arrayLiteral)
          .build();
    }

    private ArrayLiteral convert(org.eclipse.jdt.core.dom.ArrayInitializer expression) {
      return new ArrayLiteral(
          (ArrayTypeDescriptor)
              environment.createTypeDescriptor(
                  expression.resolveTypeBinding(),
                  getCurrentType().getDeclaration().isNullMarked()),
          convertExpressions(JdtEnvironment.asTypedList(expression.expressions())));
    }

    private BooleanLiteral convert(org.eclipse.jdt.core.dom.BooleanLiteral literal) {
      return literal.booleanValue() ? BooleanLiteral.get(true) : BooleanLiteral.get(false);
    }

    private CastExpression convert(org.eclipse.jdt.core.dom.CastExpression expression) {
      // Resolve the cast type descriptor in the proper @NullMarked scope so that type arguments
      // are inferred with the correct nullability. The nullability of cast type itself will be
      // inferred from the expression.
      TypeDescriptor castTypeDescriptor =
          environment.createTypeDescriptor(
              expression.getType().resolveBinding(),
              getCurrentType().getDeclaration().isNullMarked());

      Expression castExpression = convert(expression.getExpression());

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

    private NumberLiteral convert(org.eclipse.jdt.core.dom.CharacterLiteral literal) {
      return NumberLiteral.fromChar(literal.charValue());
    }

    private Expression convert(org.eclipse.jdt.core.dom.ClassInstanceCreation expression) {
      IMethodBinding constructorBinding = expression.resolveConstructorBinding();

      Expression qualifier = convertOrNull(expression.getExpression());
      List<Expression> arguments =
          convertArguments(constructorBinding, JdtEnvironment.asTypedList(expression.arguments()));

      MethodDescriptor constructorDescriptor =
          environment.createMethodDescriptor(constructorBinding);
      Type anonymousInnerClass = null;
      if (expression.getAnonymousClassDeclaration() != null) {
        anonymousInnerClass =
            convertAnonymousClassDeclaration(
                expression.getAnonymousClassDeclaration(), constructorBinding, qualifier);

        constructorDescriptor =
            Iterables.getOnlyElement(anonymousInnerClass.getConstructors()).getDescriptor();

        qualifier = null;
      }
      return NewInstance.Builder.from(constructorDescriptor)
          .setQualifier(qualifier)
          .setArguments(arguments)
          .setAnonymousInnerClass(anonymousInnerClass)
          .build();
    }

    private Type convertAnonymousClassDeclaration(
        AnonymousClassDeclaration typeDeclaration,
        IMethodBinding constructorBinding,
        Expression superQualifier) {

      Type type =
          convertType(
              typeDeclaration.resolveBinding(),
              JdtEnvironment.asTypedList(typeDeclaration.bodyDeclarations()),
              typeDeclaration);
      // The initial constructor descriptor does not include the super call qualifier.
      MethodDescriptor constructorDescriptor =
          environment.createMethodDescriptor(constructorBinding);

      // Find the corresponding superconstructor, the ClassInstanceCreation construct does not
      // have a reference the super constructor that needs to be called. But the synthetic
      // anonymous class constructor is a subsignature of the corresponding super constructor.
      IMethodBinding superConstructorBinding =
          stream(typeDeclaration.resolveBinding().getSuperclass().getDeclaredMethods())
              .filter(IMethodBinding::isConstructor)
              .filter(constructorBinding::isSubsignature)
              .findFirst()
              .get();
      MethodDescriptor superConstructorDescriptor =
          environment.createMethodDescriptor(superConstructorBinding);

      type.addMember(
          0,
          AstUtils.createImplicitAnonymousClassConstructor(
              type.getSourcePosition(),
              constructorDescriptor,
              superConstructorDescriptor,
              superQualifier));
      return type;
    }

    @Nullable
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
          return environment.createTypeDescriptor(expression.resolveTypeBinding()).getNullValue();
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
          JdtEnvironment.asTypedList(expression.fragments());

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
      TypeDescriptor conditionalTypeDescriptor =
          environment.createTypeDescriptor(
              conditionalExpression.resolveTypeBinding(),
              getCurrentType().getDeclaration().isNullMarked());
      Expression condition = convert(conditionalExpression.getExpression());
      Expression trueExpression = convert(conditionalExpression.getThenExpression());
      Expression falseExpression = convert(conditionalExpression.getElseExpression());
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
          return Statement.createNoopStatement();
        case ASTNode.EXPRESSION_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.ExpressionStatement) statement);
        case ASTNode.FOR_STATEMENT:
          return convert((org.eclipse.jdt.core.dom.ForStatement) statement);
        case ASTNode.ENHANCED_FOR_STATEMENT:
          return convert((EnhancedForStatement) statement);
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

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    private <T extends Statement> T convertOrNull(org.eclipse.jdt.core.dom.Statement statement) {
      return statement != null ? (T) convert(statement) : null;
    }

    private Statement convert(org.eclipse.jdt.core.dom.Statement statement) {
      return convertStatement(statement);
    }

    private LabeledStatement convert(org.eclipse.jdt.core.dom.LabeledStatement statement) {
      Label label = Label.newBuilder().setName(statement.getLabel().getIdentifier()).build();
      labelsInScope.computeIfAbsent(label.getName(), n -> new ArrayDeque<>()).push(label);
      LabeledStatement labeledStatement =
          LabeledStatement.newBuilder()
              .setSourcePosition(getSourcePosition(statement))
              .setLabel(label)
              .setStatement(convert(statement.getBody()))
              .build();
      labelsInScope.get(label.getName()).pop();
      return labeledStatement;
    }

    private BreakStatement convert(org.eclipse.jdt.core.dom.BreakStatement statement) {
      return BreakStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setLabelReference(getLabelReferenceOrNull(statement.getLabel()))
          .build();
    }

    private ContinueStatement convert(org.eclipse.jdt.core.dom.ContinueStatement statement) {
      return ContinueStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setLabelReference(getLabelReferenceOrNull(statement.getLabel()))
          .build();
    }

    @Nullable
    private LabelReference getLabelReferenceOrNull(SimpleName label) {
      return label == null
          ? null
          : labelsInScope.get(label.getIdentifier()).peek().createReference();
    }

    private ForStatement convert(org.eclipse.jdt.core.dom.ForStatement statement) {
      return ForStatement.newBuilder()
          // The order here is important since initializers can define new variables
          // These can be used in the expression, updaters or the body
          // This is why we need to process initializers first
          .setInitializers(convertExpressions(JdtEnvironment.asTypedList(statement.initializers())))
          .setConditionExpression(
              statement.getExpression() == null
                  ? BooleanLiteral.get(true)
                  : convert(statement.getExpression()))
          .setBody(convert(statement.getBody()))
          .setUpdates(convertExpressions(JdtEnvironment.asTypedList(statement.updaters())))
          .setSourcePosition(getSourcePosition(statement))
          .build();
    }

    private Statement convert(EnhancedForStatement statement) {
      return ForEachStatement.newBuilder()
          .setLoopVariable(convert(statement.getParameter()))
          .setIterableExpression(convert(statement.getExpression()))
          .setBody(convert(statement.getBody()))
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

      return new LocalClassDeclarationStatement(
          convert(typeDeclarationStatement.getDeclaration()),
          getSourcePosition(typeDeclarationStatement));
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
              environment.createTypeDescriptor(expression.getRightOperand().resolveBinding()))
          .build();
    }

    private Expression convert(LambdaExpression expression) {
      MethodDescriptor functionalMethodDescriptor =
          environment.createMethodDescriptor(expression.resolveMethodBinding());

      return FunctionExpression.newBuilder()
          .setTypeDescriptor(
              environment.createTypeDescriptor(
                  expression.resolveTypeBinding(),
                  getCurrentType().getDeclaration().isNullMarked()))
          .setJsAsync(functionalMethodDescriptor.isJsAsync())
          .setParameters(
              JdtEnvironment.<VariableDeclaration>asTypedList(expression.parameters()).stream()
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
          environment.createTypeDescriptor(
              expression.resolveTypeBinding(), getCurrentType().getDeclaration().isNullMarked());

      // MethodDescriptor target of the method reference.
      MethodDescriptor referencedMethodDescriptor = resolveMethodReferenceTarget(expression);

      // Functional interface method that the expression implements.
      MethodDescriptor functionalMethodDescriptor =
          environment.createMethodDescriptor(
              expression.resolveTypeBinding().getFunctionalInterfaceMethod());

      Expression qualifier = convert(expression.getExpression());

      return MethodReference.newBuilder()
          .setTypeDescriptor(expressionTypeDescriptor)
          .setReferencedMethodDescriptor(referencedMethodDescriptor)
          .setInterfaceMethodDescriptor(functionalMethodDescriptor)
          .setQualifier(qualifier)
          .setSourcePosition(sourcePosition)
          .build();
    }

    private MethodDescriptor resolveMethodReferenceTarget(ExpressionMethodReference expression) {
      IMethodBinding methodBinding = expression.resolveMethodBinding();
      if (methodBinding == null) {
        // JDT did not resolve the method binding but it was not a compilation error. This situation
        // seems to happen only for method references on array objects.
        checkArgument(expression.getExpression().resolveTypeBinding().isArray());

        // Array methods are provided by java.lang.Object and are matched here by name. This is safe
        // because there is only a handful of method in java.lang.Object and if methods are added
        // in the future they will be caught be the MoreCollectors.onlyElement. Resolving the target
        // correctly if there were many overloads is extra complexity that can be left out until
        // it is really needed.
        String targetMethodName = expression.getName().getIdentifier();
        return TypeDescriptors.get().javaLangObject.getDeclaredMethodDescriptors().stream()
            .filter(m -> m.getName().equals(targetMethodName))
            .collect(onlyElement());
      }
      return environment.createMethodDescriptor(methodBinding);
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
      TypeDescriptor expressionTypeDescriptor =
          environment.createTypeDescriptor(
              expressionTypeBinding, getCurrentType().getDeclaration().isNullMarked());
      MethodDescriptor functionalMethodDescriptor =
          environment.createMethodDescriptor(
              expression.resolveTypeBinding().getFunctionalInterfaceMethod());

      // There are 3 flavors for CreationReferences: 1) array creations, 2) unqualified
      // constructors, and 3) implicitly qualified constructors.

      // If the expression does not resolve, it is an array creation.
      SourcePosition sourcePosition = getSourcePosition(expression);
      if (expression.resolveMethodBinding() == null) {
        return ArrayCreationReference.newBuilder()
            .setTargetTypeDescriptor(
                (ArrayTypeDescriptor) environment.createTypeDescriptor(expressionTypeBinding))
            .setInterfaceMethodDescriptor(functionalMethodDescriptor)
            .setSourcePosition(sourcePosition)
            .build();
      }

      MethodDescriptor targetConstructorMethodDescriptor =
          environment.createMethodDescriptor(expression.resolveMethodBinding());

      return MethodReference.newBuilder()
          .setTypeDescriptor(expressionTypeDescriptor)
          .setReferencedMethodDescriptor(targetConstructorMethodDescriptor)
          .setInterfaceMethodDescriptor(functionalMethodDescriptor)
          .setSourcePosition(sourcePosition)
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
      return MethodReference.newBuilder()
          .setTypeDescriptor(
              environment.createDeclaredTypeDescriptor(
                  expressionTypeBinding, getCurrentType().getDeclaration().isNullMarked()))
          .setReferencedMethodDescriptor(
              environment.createMethodDescriptor(expression.resolveMethodBinding()))
          .setInterfaceMethodDescriptor(
              environment.createMethodDescriptor(
                  expressionTypeBinding.getFunctionalInterfaceMethod()))
          .setSourcePosition(getSourcePosition(expression))
          .build();
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
          environment.createMethodDescriptor(expression.resolveMethodBinding());

      Expression qualifier = createSuperReference(expression.getQualifier());
      return MethodReference.newBuilder()
          .setTypeDescriptor(environment.createTypeDescriptor(expression.resolveTypeBinding()))
          .setReferencedMethodDescriptor(methodDescriptor)
          .setInterfaceMethodDescriptor(
              environment.createMethodDescriptor(
                  expression.resolveTypeBinding().getFunctionalInterfaceMethod()))
          .setQualifier(qualifier)
          .setSourcePosition(getSourcePosition(expression))
          .build();
    }

    private SuperReference createSuperReference(Name qualifier) {
      ITypeBinding qualifierTypeBinding =
          qualifier != null ? (ITypeBinding) qualifier.resolveBinding() : null;

      // Don't consider calls that select a default method in the super hierarchy as qualified
      // (to have the same representation as in kotlin). Only consider calls to be qualified if
      // they are targeting a method in an enclosing class. Note that enclosing classes can never
      // be interfaces.
      boolean isQualified =
          qualifierTypeBinding != null && !qualifierTypeBinding.getErasure().isInterface();

      if (isQualified) {
        // This is a qualified super call, targeting an outer class method;
        return new SuperReference(
            environment.createDeclaredTypeDescriptor(qualifierTypeBinding), true);
      }

      // Call targeting a method in the super types.
      return new SuperReference(getCurrentType().getTypeDescriptor());
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
          .setOperator(JdtEnvironment.getBinaryOperator(expression.getOperator()))
          .setRightOperand(convert(expression.getRightHandSide()))
          .build();
    }

    private Block convert(org.eclipse.jdt.core.dom.Block block) {
      List<org.eclipse.jdt.core.dom.Statement> statements =
          JdtEnvironment.asTypedList(block.statements());

      return Block.newBuilder()
          .setSourcePosition(getSourcePosition(block))
          .setStatements(
              statements.stream()
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
      MethodDescriptor methodDescriptor = environment.createMethodDescriptor(constructorBinding);
      return MethodCall.Builder.from(methodDescriptor)
          .setArguments(
              convertArguments(
                  constructorBinding, JdtEnvironment.asTypedList(statement.arguments())))
          .setSourcePosition(getSourcePosition(statement))
          .build()
          .makeStatement(getSourcePosition(statement));
    }

    private Statement convert(org.eclipse.jdt.core.dom.ExpressionStatement statement) {
      return convert(statement.getExpression()).makeStatement(getSourcePosition(statement));
    }

    private FieldAccess convert(org.eclipse.jdt.core.dom.SuperFieldAccess expression) {
      IVariableBinding variableBinding = expression.resolveFieldBinding();
      FieldDescriptor fieldDescriptor = environment.createFieldDescriptor(variableBinding);

      SuperReference qualifier = createSuperReference(expression.getQualifier());
      return FieldAccess.Builder.from(fieldDescriptor).setQualifier(qualifier).build();
    }

    private Expression convert(org.eclipse.jdt.core.dom.FieldAccess expression) {
      Expression qualifier = convert(expression.getExpression());
      return environment.createFieldAccess(qualifier, expression.resolveFieldBinding());
    }

    private BinaryExpression convert(org.eclipse.jdt.core.dom.InfixExpression expression) {
      Expression leftOperand = convert(expression.getLeftOperand());
      Expression rightOperand = convert(expression.getRightOperand());
      BinaryOperator operator = JdtEnvironment.getBinaryOperator(expression.getOperator());

      checkArgument(
          !expression.hasExtendedOperands()
              || operator.getPrecedence().getAssociativity() == Associativity.LEFT);

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

    private Expression convert(org.eclipse.jdt.core.dom.MethodInvocation methodInvocation) {

      Expression qualifier =
          methodInvocation.getExpression() != null
              ? convert(methodInvocation.getExpression())
              : null;

      IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
      MethodDescriptor methodDescriptor = environment.createMethodDescriptor(methodBinding);
      List<Expression> arguments =
          convertArguments(methodBinding, JdtEnvironment.asTypedList(methodInvocation.arguments()));
      return MethodCall.Builder.from(methodDescriptor)
          .setQualifier(qualifier)
          .setArguments(arguments)
          .setSourcePosition(getSourcePosition(methodInvocation))
          .build();
    }

    private MethodCall convert(SuperMethodInvocation expression) {
      IMethodBinding methodBinding = expression.resolveMethodBinding();

      MethodDescriptor methodDescriptor = environment.createMethodDescriptor(methodBinding);

      return MethodCall.Builder.from(methodDescriptor)
          .setQualifier(createSuperReference(expression.getQualifier()))
          .setArguments(
              convertArguments(methodBinding, JdtEnvironment.asTypedList(expression.arguments())))
          .setSourcePosition(getSourcePosition(expression))
          .build();
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
      MethodDescriptor methodDescriptor = environment.createMethodDescriptor(methodBinding);
      List<Expression> arguments =
          argumentExpressions.stream()
              .map(
                  expression ->
                      foldConstants ? convertAndFoldExpression(expression) : convert(expression))
              .collect(toList());
      return AstUtils.maybePackageVarargs(methodDescriptor, arguments);
    }

    private NumberLiteral convert(org.eclipse.jdt.core.dom.NumberLiteral literal) {
      Number constantValue = (Number) literal.resolveConstantExpressionValue();
      PrimitiveTypeDescriptor typeDescriptor =
          (PrimitiveTypeDescriptor) environment.createTypeDescriptor(literal.resolveTypeBinding());
      return new NumberLiteral(typeDescriptor, constantValue);
    }

    private Expression convert(org.eclipse.jdt.core.dom.ParenthesizedExpression expression) {
      return convert(expression.getExpression());
    }

    private UnaryExpression convert(org.eclipse.jdt.core.dom.PostfixExpression expression) {
      return PostfixExpression.newBuilder()
          .setOperand(convert(expression.getOperand()))
          .setOperator(JdtEnvironment.getPostfixOperator(expression.getOperator()))
          .build();
    }

    private UnaryExpression convert(org.eclipse.jdt.core.dom.PrefixExpression expression) {
      return PrefixExpression.newBuilder()
          .setOperand(convert(expression.getOperand()))
          .setOperator(JdtEnvironment.getPrefixOperator(expression.getOperator()))
          .build();
    }

    @Nullable
    private Expression convert(org.eclipse.jdt.core.dom.QualifiedName expression) {
      IBinding binding = expression.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        checkArgument(
            variableBinding.isField(),
            internalCompilerErrorMessage("Unexpected QualifiedName that is not a field"));

        Expression qualifier = convert(expression.getQualifier());
        return environment.createFieldAccess(qualifier, variableBinding);
      }

      if (binding instanceof ITypeBinding) {
        return null;
      }

      throw internalCompilerError(
          "Unexpected type for QualifiedName binding: %s ", binding.getClass().getName());
    }

    private ReturnStatement convert(org.eclipse.jdt.core.dom.ReturnStatement statement) {
      // Grab the type of the return statement from the method declaration, not from the expression.

      return ReturnStatement.newBuilder()
          .setExpression(convertOrNull(statement.getExpression()))
          .setSourcePosition(getSourcePosition(statement))
          .build();
    }

    @Nullable
    private Expression convert(org.eclipse.jdt.core.dom.SimpleName expression) {
      IBinding binding = expression.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          // It refers to a field.
          FieldDescriptor fieldDescriptor = environment.createFieldDescriptor(variableBinding);
          return FieldAccess.Builder.from(fieldDescriptor).build();
        } else {
          // It refers to a local variable or parameter in a method or block.
          return variableByJdtBinding.get(variableBinding).createReference();
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
      boolean inNullMarkedScope = getCurrentType().getDeclaration().isNullMarked();
      Variable variable = createVariable(variableDeclaration, inNullMarkedScope);
      if (variableDeclaration.getType() instanceof org.eclipse.jdt.core.dom.UnionType) {
        // Union types are only relevant in multi catch variable declarations, which appear in the
        // AST as a SingleVariableDeclaration.
        variable.setTypeDescriptor(
            convert((org.eclipse.jdt.core.dom.UnionType) variableDeclaration.getType()));
      }
      return variable;
    }

    private Variable createVariable(
        VariableDeclaration variableDeclaration, boolean inNullMarkedScope) {
      IVariableBinding variableBinding = variableDeclaration.resolveBinding();
      Variable variable =
          environment.createVariable(
              getSourcePosition(variableBinding.getName(), variableDeclaration.getName()),
              variableBinding,
              inNullMarkedScope);
      variableByJdtBinding.put(variableBinding, variable);
      return variable;
    }

    private StringLiteral convert(org.eclipse.jdt.core.dom.StringLiteral literal) {
      return new StringLiteral(literal.getLiteralValue());
    }

    private SwitchStatement convert(org.eclipse.jdt.core.dom.SwitchStatement switchStatement) {
      Expression switchExpression = convert(switchStatement.getExpression());

      List<SwitchCase.Builder> caseBuilders = new ArrayList<>();
      for (org.eclipse.jdt.core.dom.Statement statement :
          JdtEnvironment.<org.eclipse.jdt.core.dom.Statement>asTypedList(
              switchStatement.statements())) {
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
          : SwitchCase.newBuilder()
              // Fold the constant in the switch case to avoid complex expressions. Otherwise JDT
              // would represent negative values as unary expressions, e.g - <constant>. The Wasm
              // backend relies on switch case constant for switch on integral values to be
              // literals.
              .setCaseExpression(convertAndFoldExpression(statement.getExpression()));
    }

    private SynchronizedStatement convert(
        org.eclipse.jdt.core.dom.SynchronizedStatement statement) {
      return SynchronizedStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setExpression(convert(statement.getExpression()))
          .setBody(convert(statement.getBody()))
          .build();
    }

    private ExpressionStatement convert(
        org.eclipse.jdt.core.dom.SuperConstructorInvocation expression) {
      IMethodBinding superConstructorBinding = expression.resolveConstructorBinding();
      MethodDescriptor methodDescriptor =
          environment.createMethodDescriptor(superConstructorBinding);

      return MethodCall.Builder.from(methodDescriptor)
          .setQualifier(convertOrNull(expression.getExpression()))
          .setArguments(
              convertArguments(
                  superConstructorBinding, JdtEnvironment.asTypedList(expression.arguments())))
          .setSourcePosition(getSourcePosition(expression))
          .build()
          .makeStatement(getSourcePosition(expression));
    }

    private Expression convert(org.eclipse.jdt.core.dom.ThisExpression expression) {
      Name qualifier = expression.getQualifier();
      boolean isQualified = qualifier != null;
      DeclaredTypeDescriptor typeDescriptor =
          isQualified
              ? environment.createDeclaredTypeDescriptor(qualifier.resolveTypeBinding())
              : getCurrentType().getTypeDescriptor();
      return new ThisReference(typeDescriptor, isQualified);
    }

    private Expression convert(org.eclipse.jdt.core.dom.TypeLiteral literal) {
      ITypeBinding typeBinding = literal.getType().resolveBinding();

      TypeDescriptor literalTypeDescriptor = environment.createTypeDescriptor(typeBinding);
      return new TypeLiteral(getSourcePosition(literal), literalTypeDescriptor);
    }

    private ThrowStatement convert(org.eclipse.jdt.core.dom.ThrowStatement statement) {
      return ThrowStatement.newBuilder()
          .setSourcePosition(getSourcePosition(statement))
          .setExpression(convert(statement.getExpression()))
          .build();
    }

    private TryStatement convert(org.eclipse.jdt.core.dom.TryStatement statement) {
      List<org.eclipse.jdt.core.dom.Expression> resources =
          JdtEnvironment.asTypedList(statement.resources());
      List<org.eclipse.jdt.core.dom.CatchClause> catchClauses =
          JdtEnvironment.asTypedList(statement.catchClauses());

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
              JdtEnvironment.<org.eclipse.jdt.core.dom.Type>asTypedList(unionType.types()).stream()
                  .map(org.eclipse.jdt.core.dom.Type::resolveBinding)
                  .map(environment::createTypeDescriptor)
                  .collect(toImmutableList()))
          .build();
    }

    private VariableDeclarationFragment convert(
        org.eclipse.jdt.core.dom.VariableDeclarationFragment variableDeclarationFragment) {
      boolean inNullMarkedScope = getCurrentType().getDeclaration().isNullMarked();
      Variable variable = createVariable(variableDeclarationFragment, inNullMarkedScope);
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
          JdtEnvironment.asTypedList(statement.fragments());
      return VariableDeclarationExpression.newBuilder()
          .setVariableDeclarationFragments(
              fragments.stream().map(this::convert).collect(toImmutableList()))
          .build()
          .makeStatement(getSourcePosition(statement));
    }

    private Expression convertAndFoldExpression(org.eclipse.jdt.core.dom.Expression expression) {
      Object constantValue = expression.resolveConstantExpressionValue();
      return constantValue != null
          ? Literal.fromValue(
              constantValue, environment.createTypeDescriptor(expression.resolveTypeBinding()))
          : convert(expression);
    }

    @Nullable
    private Type createType(ITypeBinding typeBinding, ASTNode sourcePositionNode) {
      if (typeBinding == null) {
        return null;
      }
      TypeDeclaration typeDeclaration = environment.createDeclarationForType(typeBinding);

      return new Type(getSourcePosition(sourcePositionNode), typeDeclaration);
    }
  }

  private CompilationUnit buildCompilationUnit(
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
    ASTConverter converter = new ASTConverter();
    return converter.convert(sourceFilePath, compilationUnit);
  }

  public static List<CompilationUnit> build(
      CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings, JdtParser jdtParser) {
    JdtEnvironment environment =
        new JdtEnvironment(
            PackageAnnotationsResolver.create(
                compilationUnitsAndTypeBindings.getCompilationUnitsByFilePath().entrySet().stream()
                    .filter(e -> e.getKey().endsWith("package-info.java"))
                    .map(Entry::getValue),
                jdtParser));

    Map<String, org.eclipse.jdt.core.dom.CompilationUnit> jdtUnitsByFilePath =
        compilationUnitsAndTypeBindings.getCompilationUnitsByFilePath();
    List<ITypeBinding> wellKnownTypeBindings = compilationUnitsAndTypeBindings.getTypeBindings();
    CompilationUnitBuilder compilationUnitBuilder =
        new CompilationUnitBuilder(wellKnownTypeBindings, environment);

    return jdtUnitsByFilePath.entrySet().stream()
        .map(entry -> compilationUnitBuilder.buildCompilationUnit(entry.getKey(), entry.getValue()))
        .collect(toImmutableList());
  }

  private CompilationUnitBuilder(
      List<ITypeBinding> wellKnownTypeBindings, JdtEnvironment environment) {
    this.environment = environment;
    environment.initWellKnownTypes(wellKnownTypeBindings);
  }
}
