/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.MoreCollectors.onlyElement;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.HasJsNameInfo;
import com.google.j2cl.transpiler.ast.HasReadableDescription;
import com.google.j2cl.transpiler.ast.HasSourcePosition;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.JsEnumInfo;
import com.google.j2cl.transpiler.ast.JsMemberType;
import com.google.j2cl.transpiler.ast.JsUtils;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableReference;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

/** Checks and throws errors for invalid JsInterop constructs. */
public class JsInteropRestrictionsChecker {

  public static void check(
      Library library,
      Problems problems,
      boolean checkWasmRestrictions,
      boolean isNullMarkedSupported,
      boolean optimizeAutoValue) {
    new JsInteropRestrictionsChecker(
            problems, checkWasmRestrictions, isNullMarkedSupported, optimizeAutoValue)
        .checkLibrary(library);
  }

  private final Problems problems;
  private final boolean checkWasmRestrictions;
  private final boolean isNullMarkedSupported;
  private final boolean optimizeAutoValue;
  private boolean wasUnusableByJsWarningReported = false;

  private JsInteropRestrictionsChecker(
      Problems problems,
      boolean checkWasmRestrictions,
      boolean isNullMarkedSupported,
      boolean optimizeAutoValue) {
    this.problems = problems;
    this.checkWasmRestrictions = checkWasmRestrictions;
    this.isNullMarkedSupported = isNullMarkedSupported;
    this.optimizeAutoValue = optimizeAutoValue;
  }

  private void checkLibrary(Library library) {
    library.streamTypes().forEach(this::checkType);

    if (wasUnusableByJsWarningReported) {
      problems.info(
          "Suppress \"[unusable-by-js]\" warnings by adding a "
              + "`@SuppressWarnings(\"unusable-by-js\")` annotation to the corresponding member.");
    }
  }

  private void checkType(Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();

    if (!checkWasmRestrictions && !isNullMarkedSupported) {
      if (!checkJSpecifyUsage(typeDeclaration)) {
        return;
      }
    }

    if (!checkWasmRestrictions && optimizeAutoValue) {
      if (!checkAutoValue(typeDeclaration)) {
        return;
      }
    }

    if (checkWasmRestrictions && !typeDeclaration.isNative()) {
      if (!checkNativeTypeSubtype(type)) {
        return;
      }
    }

    if (typeDeclaration.isJsType()) {
      if (!checkJsType(type)) {
        return;
      }
    }

    if (typeDeclaration.isJsEnum()) {
      checkJsEnum(type);
    }

    if (typeDeclaration.isJsEnum() || typeDeclaration.isJsType()) {
      checkQualifiedJsName(type);
    }

    if (typeDeclaration.isJsFunctionInterface()) {
      checkJsFunction(type);
    } else if (typeDeclaration.isJsFunctionImplementation()) {
      checkJsFunctionImplementation(type);
    } else {
      checkJsFunctionSubtype(type);
      if (checkJsConstructors(type)) {
        checkJsConstructorSubtype(type);
      }
    }

    checkTypeVariables(type);

    checkNameCollisions(type);
    for (Member member : type.getMembers()) {
      checkMember(member);
    }
    checkTypeLiteralsAndInstanceOfs(type);
    checkJsEnumUsages(type);
    checkJsFunctionLambdas(type);
    checkSystemProperties(type);

    checkNativeTypeUsagesInWasm(type);
    checkNativeTypesAssignabilityInWasm(type);
  }

  private boolean checkJSpecifyUsage(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.isNullMarked()) {
      problems.error("@NullMarked annotation is not supported without enabling static analysis.");
      return false;
    }
    return true;
  }

  private boolean checkAutoValue(TypeDeclaration typeDeclaration) {
    TypeDeclaration superType = typeDeclaration.getSuperTypeDeclaration();
    if (superType == null) {
      return true;
    }

    if ((!superType.isAnnotatedWithAutoValue() || checkAutoValueTypeName(typeDeclaration))
        && (!superType.isAnnotatedWithAutoValueBuilder()
            || checkAutoValueTypeName(typeDeclaration.getEnclosingTypeDeclaration()))) {
      return true;
    }

    problems.error(
        "Extending @AutoValue with %s is not supported when AutoValue optimization is enabled."
            + " (Also see https://errorprone.info/bugpattern/ExtendsAutoValue)",
        typeDeclaration.getReadableDescription());

    return false;
  }

  private boolean checkAutoValueTypeName(TypeDeclaration typeDeclaration) {
    // TODO(goktug): Replace with checking the generator name passed via @Generated when J2CL starts
    // modeling annotations in the AST.
    return typeDeclaration != null
        && typeDeclaration.getSimpleSourceName().matches("\\$*AutoValue_.+");
  }

  private void checkSystemProperties(Type type) {
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethodCall(MethodCall methodCall) {
            MethodDescriptor target = methodCall.getTarget();
            List<Expression> args = methodCall.getArguments();
            if (target
                    .getEnclosingTypeDescriptor()
                    .getQualifiedBinaryName()
                    .equals("java.lang.System")
                && target.getName().equals("getProperty")
                && !(args.get(0) instanceof StringLiteral)) {
              problems.error(
                  methodCall.getSourcePosition(),
                  "Method '%s' can only take a string literal as its first parameter",
                  target.getReadableDescription());
            }
          }
        });
  }

  private boolean checkNativeTypeSubtype(Type type) {
    if (type.getDeclaration().isNative()) {
      return true;
    }

    Optional<DeclaredTypeDescriptor> nativeSupertype =
        type.getSuperTypesStream().filter(TypeDescriptor::isNative).findFirst();
    if (nativeSupertype.isPresent()) {
      problems.error(
          type.getSourcePosition(),
          "Non-native type '%s' cannot %s native JsType '%s'.",
          type.getReadableDescription(),
          type.isInterface() || nativeSupertype.get().isClass() ? "extend" : "implement",
          nativeSupertype.get().getReadableDescription());
      return false;
    }

    return true;
  }

  private void checkNativeTypeUsagesInWasm(Type type) {
    if (!checkWasmRestrictions) {
      return;
    }

    checkNativeTypeArguments(type);
    checkNativeTypeArrays(type);
  }

  private void checkNativeTypeArguments(Type type) {
    checkAllowedTypes(
        type,
        TypeDescriptor::isNative,
        /* onlyCheckTypeSpecialization= */ true,
        // Native type arrays are checked elsewhere.
        /* checkArrayComponent= */ false,
        /* disallowedTypeDescription= */ "type with Native type argument",
        /* messageSuffix= */ " (b/290992813)");
  }

  private void checkNativeTypeArrays(Type type) {
    checkAllowedTypes(
        type,
        TypeDescriptor::isNativeJsArray,
        /* onlyCheckTypeSpecialization= */ false,
        /* checkArrayComponent= */ true,
        /* disallowedTypeDescription= */ "Native type array",
        /* messageSuffix= */ " (b/261079024)");
  }

  private void checkNativeTypesAssignabilityInWasm(Type type) {
    if (!checkWasmRestrictions) {
      return;
    }

    checkTypeAssignments(
        type,
        JsInteropRestrictionsChecker::isDisallowedNativeJsTypeAssignment,
        /* errorMessageSuffix= */ " (b/262009761)");
  }

  private static boolean isDisallowedNativeJsTypeAssignment(
      TypeDescriptor toTypeDescriptor, TypeDescriptor fromTypeDescriptor) {
    return toTypeDescriptor.isNative() != fromTypeDescriptor.isNative();
  }

  private void checkJsFunctionLambdas(Type type) {
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitFunctionExpression(FunctionExpression functionExpression) {
            if (!functionExpression.getTypeDescriptor().isIntersection()) {
              return;
            }
            IntersectionTypeDescriptor intersectionTypeDescriptor =
                (IntersectionTypeDescriptor) functionExpression.getTypeDescriptor();
            if (intersectionTypeDescriptor.getIntersectionTypeDescriptors().stream()
                .anyMatch(TypeDescriptor::isJsFunctionInterface)) {
              problems.error(
                  functionExpression.getSourcePosition(),
                  "JsFunction lambda can only implement the JsFunction interface.");
            }
          }
        });
  }

  private void checkTypeVariables(Type type) {
    if (type.getDeclaration().getTypeParameterDescriptors().stream()
        .map(TypeDescriptor::toRawTypeDescriptor)
        .anyMatch(AstUtils::isNonNativeJsEnum)) {
      problems.error(
          type.getSourcePosition(),
          "Type '%s' cannot define a type variable with a JsEnum as a bound.",
          type.getReadableDescription());
    }
  }

  /**
   * Checks that the JsEnum complies with all restrictions.
   *
   * <p>There are three flavors of JsEnum:
   *
   * <ul>
   *   <li>(1) JsEnum that does not customize the value type.
   *   <li>(2) JsEnum that customizes the value type.
   *   <li>(3) native JsEnum.
   * </ul>
   *
   * <pre>
   *       | has value field | has constructor | implements Comparable | can call ordinal() |
   *  (1)  |                 |                 |          x            |        x           |
   *  (2)  |       x         |        x        |                       |                    |
   *  (3)  |       x         |                 |          x            |                    |
   *
   * </pre>
   */
  private void checkJsEnum(Type type) {
    JsEnumInfo jsEnumInfo = type.getTypeDescriptor().getJsEnumInfo();

    if (!type.isEnum()) {
      problems.error(
          type.getSourcePosition(),
          "JsEnum '%s' has to be an enum type.",
          type.getReadableDescription());
      return;
    }

    if (type.getDeclaration().isJsType()) {
      problems.error(
          type.getSourcePosition(),
          "'%s' cannot be both a JsEnum and a JsType at the same time.",
          type.getReadableDescription());
    }

    Field valueField = getJsEnumValueField(type);
    if (valueField == null && jsEnumInfo.hasCustomValue()) {
      problems.error(
          type.getSourcePosition(),
          "Custom-valued JsEnum '%s' does not have a field named 'value'.",
          type.getReadableDescription());
    } else if (valueField != null && !jsEnumInfo.hasCustomValue()) {
      problems.error(
          type.getSourcePosition(),
          "Non-custom-valued JsEnum '%s' cannot have a field named 'value'.",
          type.getReadableDescription());
    }

    if (type.getConstructors().isEmpty() && requiresConstructor(type.getDeclaration())) {
      problems.error(
          type.getSourcePosition(),
          "Custom-valued JsEnum '%s' should have a constructor.",
          type.getReadableDescription());
    }

    if (!type.getSuperInterfaceTypeDescriptors().isEmpty()) {
      problems.error(
          type.getSourcePosition(),
          "JsEnum '%s' cannot implement any interface.",
          type.getReadableDescription());
    }

    for (Member member : type.getMembers()) {
      if (member.getDescriptor().isJsOverlay()) {
        // JsOverlays are checked independently.
        continue;
      }
      checkMemberOfJsEnum(type, member);
    }
  }

  @Nullable
  private static Field getJsEnumValueField(Type type) {
    checkState(type.isJsEnum());
    return type.getFields().stream()
        .filter(member -> AstUtils.isJsEnumCustomValueField(member.getDescriptor()))
        .findFirst()
        .orElse(null);
  }

  private static boolean requiresConstructor(TypeDeclaration typeDeclaration) {
    return typeDeclaration.getJsEnumInfo().hasCustomValue() && !typeDeclaration.isNative();
  }

  private void checkMemberOfJsEnum(Type type, Member member) {
    if (member.isEnumField()) {
      checkJsEnumConstant(type, (Field) member);
    } else if (AstUtils.isJsEnumCustomValueField(member.getDescriptor())) {
      checkJsEnumValueField((Field) member);
    } else if (member.isField() && !member.isStatic()) {
      problems.error(
          member.getSourcePosition(),
          "JsEnum '%s' cannot have instance field '%s'.",
          type.getReadableDescription(),
          member.getReadableDescription());
    } else if (member.isInitializerBlock() && !member.isStatic()) {
      problems.error(
          member.getSourcePosition(),
          "JsEnum '%s' cannot have an instance initializer.",
          type.getReadableDescription());
    } else {
      if (member.isConstructor()) {
        checkJsEnumConstructor(
            (Method) member, AstUtils.getJsEnumValueFieldType(type.getDeclaration()));
      } else if (type.isNative()) {
        checkMustBeJsOverlay(member, "Native JsEnum");
      }
      checkImplementableStatically(member, "JsEnum");
    }
  }

  private static boolean canDefineValueField(TypeDescriptor typeDescriptor) {
    return typeDescriptor.getJsEnumInfo().hasCustomValue();
  }

  private void checkJsEnumConstant(Type type, Field field) {
    if (!field.getInitializer().getTypeDescriptor().isSameBaseType(type.getTypeDescriptor())) {
      problems.error(
          field.getSourcePosition(),
          "JsEnum constant '%s' cannot have a class body.",
          field.getReadableDescription());
      return;
    }

    if (!canDefineValueField(type.getTypeDescriptor())) {
      // Non-custom-valued enum is already invalid if it has a value field,
      // further constant related checking is unnecessary.
      return;
    }

    Expression enumFieldValue = getEnumConstantValue(field);
    if (enumFieldValue == null || enumFieldValue.isCompileTimeConstant()) {
      // Integer.MIN_VALUE is considered null in Wasm for int-valued JsEnums.
      // We can check for NumberLiteral here because constant expressions are folded in the
      // frontend.
      if (enumFieldValue instanceof NumberLiteral
          && ((NumberLiteral) enumFieldValue).getValue().intValue() == Integer.MIN_VALUE) {
        problems.error(
            field.getSourcePosition(),
            "Custom-valued JsEnum constant '%s' cannot be equal to Integer.MIN_VALUE.",
            field.getReadableDescription());
      }
      return;
    }
    problems.error(
        field.getSourcePosition(),
        "Custom-valued JsEnum constant '%s' cannot have a non-literal value.",
        field.getReadableDescription());
  }

  @Nullable
  private static Expression getEnumConstantValue(Field field) {
    NewInstance initializer = (NewInstance) field.getInitializer();
    List<Expression> arguments = initializer.getArguments();
    if (arguments.size() != 1) {
      // Not a valid initialization. The code will be rejected.
      return null;
    }
    return arguments.get(0);
  }

  private void checkJsEnumValueField(Field field) {
    FieldDescriptor fieldDescriptor = field.getDescriptor();

    if (!canDefineValueField(fieldDescriptor.getEnclosingTypeDescriptor())) {
      // Non-custom-valued enum is already invalid if it has a value field,
      // further value related checking is unnecessary.
      return;
    }

    TypeDescriptor valueTypeDescriptor = fieldDescriptor.getTypeDescriptor();
    String messagePrefix =
        String.format("Custom-valued JsEnum value field '%s'", field.getReadableDescription());

    if (fieldDescriptor.isStatic()
        || fieldDescriptor.isJsOverlay()
        || fieldDescriptor.isJsMember()) {
      problems.error(
          field.getSourcePosition(),
          "%s cannot be static nor JsOverlay nor JsMethod nor JsProperty.",
          messagePrefix);
    }

    if (!checkJsEnumCustomValueType(valueTypeDescriptor)) {
      String bugMessage = "";
      if (valueTypeDescriptor.isPrimitive()) {
        bugMessage = " (b/295240966)";
      }
      problems.error(
          field.getSourcePosition(),
          "%s cannot have the type '%s'. The only valid value types are 'int' and"
              + " 'java.lang.String'.%s",
          messagePrefix,
          valueTypeDescriptor.getReadableDescription(),
          bugMessage);
    }

    if (field.getInitializer() != null) {
      problems.error(field.getSourcePosition(), "%s cannot have initializer.", messagePrefix);
    }
  }

  private static boolean checkJsEnumCustomValueType(TypeDescriptor valueTypeDescriptor) {
    return TypeDescriptors.isPrimitiveInt(valueTypeDescriptor)
        || TypeDescriptors.isJavaLangString(valueTypeDescriptor);
  }

  private void checkJsEnumConstructor(Method constructor, TypeDescriptor customValueType) {
    TypeDeclaration enclosingTypeDeclaration =
        constructor.getDescriptor().getEnclosingTypeDescriptor().getTypeDeclaration();
    if (!requiresConstructor(enclosingTypeDeclaration)) {
      problems.error(
          constructor.getSourcePosition(),
          "%s cannot have constructor '%s'.",
          getReadableDescriptionWithPrefix(enclosingTypeDeclaration),
          constructor.getReadableDescription());
      return;
    }

    if (checkCustomValuedJsEnumConstructor(constructor, customValueType)) {
      return;
    }
    problems.error(
        constructor.getSourcePosition(),
        "Custom-valued JsEnum constructor '%s' should have one parameter of the value type and its "
            + "body should only be the assignment to the value field.",
        constructor.getReadableDescription());
  }

  /**
   * Custom valued JsEnums must have exactly one constructor of the following form:
   *
   * <pre>{@code
   * JsEnumType(CustomValueType parameter) {
   *   this.value = parameter;
   * }
   * }</pre>
   */
  private static boolean checkCustomValuedJsEnumConstructor(
      Method constructor, TypeDescriptor customValueType) {
    MethodDescriptor constructorDescriptor = constructor.getDescriptor();
    // Check that the parameter to the constructor is consistent with the custom value type.
    if (constructorDescriptor.getParameterDescriptors().size() != 1
        || !constructorDescriptor
            .getParameterDescriptors()
            .get(0)
            .getTypeDescriptor()
            .isSameBaseType(customValueType)) {
      // Method declaration is invalid.
      return false;
    }

    // Verify that the body only contains the assignment to the custom value field.
    // Note that at this stage, we don't have super calls for enums (JLS won't let them to be
    // explicit and we haven't normalized the AST yet).
    return constructor.getBody().getStatements().size() == 1
        && checkJsEnumConstructorStatement(
            constructorDescriptor.getEnclosingTypeDescriptor(),
            constructor.getBody().getStatements().get(0),
            constructor.getParameters().get(0));
  }

  /**
   * Checks that the only statement in a custom valued JsEnum is of the form:
   *
   * <pre>{@code
   * this.value = parameter;
   * }</pre>
   */
  private static boolean checkJsEnumConstructorStatement(
      DeclaredTypeDescriptor typeDescriptor, Statement statement, Variable valueParameter) {
    if (!(statement instanceof ExpressionStatement)) {
      return false;
    }
    Expression expression = ((ExpressionStatement) statement).getExpression();
    if (!(expression instanceof BinaryExpression)) {
      return false;
    }

    BinaryExpression binaryExpression = (BinaryExpression) expression;
    if (!binaryExpression.isSimpleAssignment()
        || !(binaryExpression.getRightOperand() instanceof VariableReference)
        || !(binaryExpression.getLeftOperand() instanceof FieldAccess)) {
      return false;
    }
    FieldAccess lhs = (FieldAccess) binaryExpression.getLeftOperand();
    Variable variable = ((VariableReference) binaryExpression.getRightOperand()).getTarget();

    return (lhs.getQualifier() == null || lhs.getQualifier() instanceof ThisReference)
        && lhs.getTarget().isMemberOf(typeDescriptor)
        && AstUtils.isJsEnumCustomValueField(lhs.getTarget())
        && variable == valueParameter;
  }

  private void checkJsEnumUsages(Type type) {
    checkJsEnumMethodCalls(type);
    checkJsEnumAssignments(type);
    checkJsEnumArrays(type);
    checkJsEnumValueFieldAssignment(type);

    checkOverrides(
        type,
        // Not checking parameters.
        (a, b) -> false,
        JsInteropRestrictionsChecker::isDisallowedJsEnumAssignment,
        /* checkedTypeMessage= */ "JsEnum");
  }

  private void checkJsEnumMethodCalls(Type type) {
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethodCall(MethodCall methodCall) {
            MethodDescriptor target = methodCall.getTarget();

            TypeDescriptor qualifierTypeDescriptor =
                target.isInstanceMember()
                    ? methodCall.getQualifier().getTypeDescriptor()
                    : target.getEnclosingTypeDescriptor();
            if (!qualifierTypeDescriptor.isJsEnum()) {
              // If the actual target of the method is not a JsEnum, nothing to check.
              return;
            }

            if (target.getEnclosingTypeDescriptor().isJsEnum() && !target.isEnumSyntheticMethod()) {
              // Methods declared by the user in JsEnum are callable.
              return;
            }

            if (target.isOrOverridesJavaLangObjectMethod()) {
              return;
            }

            String messagePrefix = "JsEnum ";

            String targetMethodSignature = target.getDeclarationDescriptor().getSignature();
            if (targetMethodSignature.equals("compareTo(java.lang.Enum)")) {
              if (qualifierTypeDescriptor.getJsEnumInfo().supportsComparable()) {
                return;
              }
              // Customize the message to give a better idea why compareTo() is forbidden.
              messagePrefix = getTypeKindPrefix(qualifierTypeDescriptor);
            }
            if (targetMethodSignature.equals("ordinal()")) {
              if (qualifierTypeDescriptor.getJsEnumInfo().supportsOrdinal()) {
                return;
              }
              // Customize the message to give a better idea why ordinal() is forbidden.
              messagePrefix = getTypeKindPrefix(qualifierTypeDescriptor);
            }

            String bugMessage = "";
            if (targetMethodSignature.equals("values()")) {
              bugMessage = " (b/118228329)";
            }

            problems.error(
                methodCall.getSourcePosition(),
                "%s'%s' does not support '%s'.%s",
                messagePrefix,
                qualifierTypeDescriptor.getReadableDescription(),
                target.getReadableDescription(),
                bugMessage);
          }
        });
  }

  private void checkJsEnumAssignments(Type type) {
    type.getMembers().forEach(this::checkJsEnumAssignments);
  }

  private void checkJsEnumAssignments(Member member) {
    member.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                // Handle here all the scenarios related to the JsEnum being used as an enum, e.g.
                // assignment, parameter passing, etc.
                checkJsEnumAssignment(inferredTypeDescriptor, expression);
                return expression;
              }

              @Override
              public Expression rewriteMemberQualifierContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression qualifierExpression) {
                // Skip checking here explicitly, members are checked explicitly elsewhere.
                return qualifierExpression;
              }

              private void checkJsEnumAssignment(
                  TypeDescriptor toTypeDescriptor, Expression expression) {
                TypeDescriptor expressionTypeDescriptor = expression.getTypeDescriptor();

                if (isDisallowedJsEnumAssignment(toTypeDescriptor, expressionTypeDescriptor)) {
                  // TODO(b/65465035): When source position is tracked at the expression level,
                  // the error reporting here should include source position.
                  problems.error(
                      getSourcePosition(),
                      "%s'%s' cannot be assigned to '%s'.",
                      TypeDescriptors.isJavaLangComparable(toTypeDescriptor.toRawTypeDescriptor())
                          ? getTypeKindPrefix(expressionTypeDescriptor)
                          : "JsEnum ",
                      expressionTypeDescriptor.getReadableDescription(),
                      toTypeDescriptor.getReadableDescription());
                }
              }
            }));
  }

  private static boolean isDisallowedJsEnumAssignment(
      TypeDescriptor toTypeDescriptor, TypeDescriptor expressionTypeDescriptor) {
    TypeDescriptor targetRawTypeDescriptor = toTypeDescriptor.toRawTypeDescriptor();
    if (!expressionTypeDescriptor.isJsEnum() || targetRawTypeDescriptor.isJsEnum()) {
      return false;
    }

    if (TypeDescriptors.isJavaLangObject(targetRawTypeDescriptor)
        || TypeDescriptors.isJavaIoSerializable(targetRawTypeDescriptor)) {
      // Assignment to Object or Serializable is allowed.
      return false;
    }

    if (TypeDescriptors.isJavaLangComparable(targetRawTypeDescriptor)
        && expressionTypeDescriptor.getJsEnumInfo().supportsComparable()) {
      return false;
    }

    return true;
  }

  private void checkJsEnumArrays(Type type) {
    // JsEnum arrays cannot be assigned to Object[], T[], T..., and so on.
    checkTypeAssignments(
        type,
        JsInteropRestrictionsChecker::isDisallowedJsEnumArrayAssignment,
        /* errorMessageSuffix= */ "");

    // JsEnum arrays are not allowed to be used in most type parameters.
    checkAllowedTypes(
        type,
        AstUtils::isNonNativeJsEnumArray,
        /* onlyCheckTypeSpecialization= */ true,
        /* checkArrayComponent= */ true,
        /* disallowedTypeDescription= */ "JsEnum array type",
        /* messageSuffix= */ "");

    checkOverrides(
        type,
        JsInteropRestrictionsChecker::isDisallowedJsEnumArrayOverride,
        JsInteropRestrictionsChecker::isDisallowedJsEnumArrayOverride,
        /* checkedTypeMessage= */ "JsEnum array");
  }

  private static boolean isDisallowedJsEnumArrayAssignment(
      TypeDescriptor toTypeDescriptor, TypeDescriptor expressionTypeDescriptor) {
    return !expressionTypeDescriptor.isSameBaseType(toTypeDescriptor)
        && (hasDisallowedType(
                expressionTypeDescriptor,
                expressionTypeDescriptor,
                AstUtils::isNonNativeJsEnumArray,
                /* onlyCheckTypeSpecialization= */ false,
                /* checkArrayComponent= */ true)
            || hasDisallowedType(
                toTypeDescriptor,
                toTypeDescriptor,
                AstUtils::isNonNativeJsEnumArray,
                /* onlyCheckTypeSpecialization= */ false,
                /* checkArrayComponent= */ true));
  }

  private static boolean isDisallowedJsEnumArrayOverride(
      TypeDescriptor toTypeDescriptor, TypeDescriptor expressionTypeDescriptor) {
    // For override checking, We want to check both (base=T, child=MyJsEnum[]) and (base=List<T>,
    // child=List<MyJsEnum[]>), so also perform the check when the parameter types match but the
    // base type has type variables.
    return (!expressionTypeDescriptor.isSameBaseType(toTypeDescriptor)
            || !toTypeDescriptor.getAllTypeVariables().isEmpty())
        && hasDisallowedType(
            expressionTypeDescriptor,
            expressionTypeDescriptor,
            AstUtils::isNonNativeJsEnumArray,
            /* onlyCheckTypeSpecialization= */ false,
            /* checkArrayComponent= */ true);
  }

  private void checkOverrides(
      Type type,
      BiFunction<TypeDescriptor, TypeDescriptor, Boolean> isDisallowedParameterOverride,
      BiFunction<TypeDescriptor, TypeDescriptor, Boolean> isDisallowedReturnTypeOverride,
      String checkedTypeMessage) {
    type.getMembers()
        .forEach(
            member ->
                checkOverrides(
                    member,
                    isDisallowedParameterOverride,
                    isDisallowedReturnTypeOverride,
                    checkedTypeMessage));
  }

  private void checkOverrides(
      Member member,
      BiFunction<TypeDescriptor, TypeDescriptor, Boolean> isDisallowedParameterOverride,
      BiFunction<TypeDescriptor, TypeDescriptor, Boolean> isDisallowedReturnTypeOverride,
      String checkedTypeMessage) {
    if (!member.isMethod()) {
      return;
    }

    Method method = (Method) member;
    MethodDescriptor methodDescriptor = method.getDescriptor();
    for (MethodDescriptor m : methodDescriptor.getJavaOverriddenMethodDescriptors()) {
      MethodDescriptor overriddenMethodDescriptor = m.getDeclarationDescriptor();
      for (int argIndex = 0;
          argIndex < methodDescriptor.getParameterDescriptors().size();
          argIndex++) {
        TypeDescriptor parameterDescriptor =
            methodDescriptor.getParameterDescriptors().get(argIndex).getTypeDescriptor();
        TypeDescriptor overriddenParameterDescriptor =
            overriddenMethodDescriptor.getParameterDescriptors().get(argIndex).getTypeDescriptor();

        if (isDisallowedParameterOverride.apply(
            overriddenParameterDescriptor, parameterDescriptor)) {
          problems.error(
              member.getSourcePosition(),
              "Method '%s' cannot override method '%s' with a %s parameter.",
              methodDescriptor.getReadableDescription(),
              overriddenMethodDescriptor.getReadableDescription(),
              checkedTypeMessage);
        }
      }

      if (isDisallowedReturnTypeOverride.apply(
          overriddenMethodDescriptor.getReturnTypeDescriptor(),
          methodDescriptor.getReturnTypeDescriptor())) {
        problems.error(
            member.getSourcePosition(),
            "Method '%s' cannot override method '%s' with a %s return type.",
            methodDescriptor.getReadableDescription(),
            overriddenMethodDescriptor.getReadableDescription(),
            checkedTypeMessage);
      }
    }
  }

  private void checkJsEnumValueFieldAssignment(Type type) {
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitBinaryExpression(BinaryExpression binaryExpression) {
            if (!binaryExpression.getOperator().isSimpleOrCompoundAssignment()) {
              return;
            }
            Expression lhs = binaryExpression.getLeftOperand();
            if (!(lhs instanceof FieldAccess)) {
              return;
            }

            FieldAccess fieldAccess = (FieldAccess) lhs;
            FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
            if (!AstUtils.isJsEnumCustomValueField(fieldDescriptor)) {
              return;
            }

            if (getCurrentMember().isConstructor()
                && getCurrentMember().getDescriptor().getEnclosingTypeDescriptor().isJsEnum()) {
              // JsEnum constructors have more stringent checks elsewhere.
              return;
            }
            SourcePosition sourcePosition = fieldAccess.getSourcePosition();
            problems.error(
                sourcePosition == SourcePosition.NONE ? type.getSourcePosition() : sourcePosition,
                "Custom-valued JsEnum value field '%s' cannot be assigned.",
                fieldDescriptor.getReadableDescription());
          }
        });
  }

  /** Checks that type references in casts, instanceof and type literals are valid. */
  private void checkTypeLiteralsAndInstanceOfs(Type type) {
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
            TypeDescriptor testTypeDescriptor = instanceOfExpression.getTestTypeDescriptor();
            if (testTypeDescriptor.isNative()
                && testTypeDescriptor.isInterface()
                && !((DeclaredTypeDescriptor) testTypeDescriptor).hasCustomIsInstanceMethod()) {
              problems.error(
                  instanceOfExpression.getSourcePosition(),
                  "Cannot do instanceof against native JsType interface '%s'.",
                  testTypeDescriptor.getReadableDescription());
            } else if (checkWasmRestrictions && testTypeDescriptor.isNative()) {
              // We currently do a "ref.test extern" in Wasm for instanceof for all native types,
              // which is not useful.
              problems.error(
                  instanceOfExpression.getSourcePosition(),
                  "Cannot do instanceof against native JsType '%s'.",
                  testTypeDescriptor.getReadableDescription());
            } else if (AstUtils.isNonNativeJsEnumArray(
                instanceOfExpression.getTestTypeDescriptor())) {
              problems.error(
                  instanceOfExpression.getSourcePosition(),
                  "Cannot do instanceof against JsEnum array '%s'.",
                  instanceOfExpression.getTestTypeDescriptor().getReadableDescription());
            } else if (testTypeDescriptor.isJsFunctionImplementation()) {
              problems.error(
                  instanceOfExpression.getSourcePosition(),
                  "Cannot do instanceof against JsFunction implementation '%s'.",
                  testTypeDescriptor.getReadableDescription());
            } else if (testTypeDescriptor.isJsEnum() && testTypeDescriptor.isNative()) {
              problems.error(
                  instanceOfExpression.getSourcePosition(),
                  "Cannot do instanceof against native JsEnum '%s'.",
                  testTypeDescriptor.getReadableDescription());
            }
          }

          @Override
          public void exitTypeLiteral(TypeLiteral typeLiteral) {
            TypeDescriptor literalTypeDescriptor = typeLiteral.getReferencedTypeDescriptor();
            if (literalTypeDescriptor.isJsEnum() && literalTypeDescriptor.isNative()) {
              problems.error(
                  typeLiteral.getSourcePosition(),
                  "Cannot use native JsEnum literal '%s.class'.",
                  literalTypeDescriptor.getReadableDescription());
            }
          }
        });
  }

  private boolean checkJsType(Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    if (typeDeclaration.isLocal()) {
      problems.error(
          type.getSourcePosition(),
          "Local class '%s' cannot be a JsType.",
          type.getDeclaration().getReadableDescription());
      return false;
    }

    if (typeDeclaration.isNative()) {
      if (!checkNativeJsType(type)) {
        return false;
      }
    }

    return true;
  }

  private void checkIllegalOverrides(Method method) {
    Optional<MethodDescriptor> jsOverlayOverride =
        method.getDescriptor().getJavaOverriddenMethodDescriptors().stream()
            .filter(MethodDescriptor::isJsOverlay)
            .findFirst();

    if (jsOverlayOverride.isPresent()) {
      checkState(!jsOverlayOverride.get().isSynthetic());
      problems.error(
          method.getSourcePosition(),
          "Method '%s' cannot override a JsOverlay method '%s'.",
          method.getReadableDescription(),
          jsOverlayOverride.get().getReadableDescription());
    }
  }

  private void checkMember(Member member) {
    MemberDescriptor memberDescriptor = member.getDescriptor();
    if ((!member.isMethod() && !member.isField()) || memberDescriptor.isSynthetic()) {
      return;
    }

    DeclaredTypeDescriptor enclosingTypeDescriptor = memberDescriptor.getEnclosingTypeDescriptor();
    if (enclosingTypeDescriptor.isNative() && enclosingTypeDescriptor.isJsType()) {
      checkMemberOfNativeJsType(member);
    }

    if (enclosingTypeDescriptor.extendsNativeClass()) {
      checkMemberOfSubclassOfNativeClass(member);
    }

    if (memberDescriptor.isJsOverlay()) {
      checkJsOverlay(member);
    }

    if (member.isMethod()) {
      Method method = (Method) member;
      checkIllegalOverrides(method);
      checkMethodParameters(method);
      checkMethodWasmInfo(method);

      if (memberDescriptor.isNative()) {
        checkNativeMethod(method);
      }
      if (memberDescriptor.isJsAsync()) {
        checkJsAsyncMethod(method);
      }
      if (!checkJsPropertyAccessor(method)) {
        return;
      }
      if (method.getDescriptor().isCustomIsInstanceMethod()) {
        checkCustomIsInstanceMethod(method);
      }
    }

    if (memberDescriptor.canBeReferencedExternally()) {
      checkUnusableByJs(member);
    }

    if (!checkQualifiedJsName(member)) {
      return;
    }

    checkOverrideConsistency(member);
  }

  private void checkMemberOfSubclassOfNativeClass(Member member) {
    if (member.isStatic() || member.isConstructor() || member.getDescriptor().isJsOverlay()) {
      return;
    }

    member.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethodCall(MethodCall methodCall) {
            if (!(methodCall.getQualifier() instanceof SuperReference)) {
              return;
            }
            MethodDescriptor target = methodCall.getTarget();
            if (target.isOrOverridesJavaLangObjectMethod()) {
              problems.error(
                  methodCall.getSourcePosition(),
                  "Cannot use 'super' to call '%s' from a subclass of a native class.",
                  target.getReadableDescription());
            }
          }
        });
  }

  private void checkNativeMethod(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();

    if (checkWasmRestrictions) {
      // Members of native types are checked elsewhere.
      checkWasmNativeMethodSignature(method);
      return;
    }

    if (isUnusableByJsSuppressed(methodDescriptor)) {
      return;
    }

    if (!methodDescriptor.isJsMember()) {
      problems.error(
          method.getSourcePosition(),
          "[unusable-by-js] Native '%s' is exposed to JavaScript without @JsMethod.",
          method.getReadableDescription());
    }
  }

  private void checkJsAsyncMethod(Method method) {
    TypeDescriptor returnType = method.getDescriptor().getReturnTypeDescriptor();
    if (returnType instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor returnTypeDescriptor = (DeclaredTypeDescriptor) returnType;
      String qualifiedJsName = returnTypeDescriptor.getQualifiedJsName();
      if (qualifiedJsName.equals("IThenable") || qualifiedJsName.equals("Promise")) {
        return;
      }
    }
    problems.error(
        method.getSourcePosition(),
        "JsAsync method '%s' should return either 'IThenable' or 'Promise' but returns '%s'.",
        method.getReadableDescription(),
        returnType.getReadableDescription());
  }

  private void checkCustomIsInstanceMethod(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    if (methodDescriptor.isInstanceMember()
        // If the custom `isInstance` method is defined in a Kotlin companion object, The method
        // will be later moved to the enclosing type and become static.
        && !methodDescriptor.getEnclosingTypeDescriptor().isOptimizableKotlinCompanion()) {
      problems.error(
          method.getSourcePosition(),
          "Custom '$isInstance' method '%s' has to be static.",
          method.getReadableDescription());
    }
    if (methodDescriptor.getVisibility().isPrivate()) {
      problems.error(
          method.getSourcePosition(),
          "Custom '$isInstance' method '%s' has to be non private.",
          method.getReadableDescription());
    }
    if (!TypeDescriptors.isPrimitiveBoolean(methodDescriptor.getReturnTypeDescriptor())) {
      problems.error(
          method.getSourcePosition(),
          "Custom '$isInstance' method '%s' has to return 'boolean'.",
          method.getReadableDescription());
    }
  }

  private void checkOverrideConsistency(Member member) {
    if (!member.isMethod() || !member.getDescriptor().isJsMember()) {
      return;
    }
    Method method = (Method) member;
    String jsName = method.getSimpleJsName();
    MethodDescriptor methodDescriptor = method.getDescriptor();
    for (MethodDescriptor overriddenMethodDescriptor :
        methodDescriptor.getJavaOverriddenMethodDescriptors()) {
      if (!overriddenMethodDescriptor.isJsMember() || overriddenMethodDescriptor.isBridge()) {
        continue;
      }

      if (!methodDescriptor.canInheritsJsInfoFrom(overriddenMethodDescriptor)) {
        // Only methods that have the same jsinfo compatibility signature have to agree in the
        // name. An override that specializes the parameters needs to have a different name than
        // the method it overrides.
        continue;
      }

      if (overriddenMethodDescriptor.isJsMethod() != methodDescriptor.isJsMethod()) {
        // Overrides can not change JsMethod to JsProperty nor vice versa.
        problems.error(
            method.getSourcePosition(),
            "%s '%s' cannot override %s '%s'.",
            member.getDescriptor().isJsMethod() ? "JsMethod" : "JsProperty",
            member.getReadableDescription(),
            overriddenMethodDescriptor.isJsMethod() ? "JsMethod" : "JsProperty",
            overriddenMethodDescriptor.getReadableDescription());
        break;
      }

      String parentName = overriddenMethodDescriptor.getSimpleJsName();
      if (!parentName.equals(jsName)) {
        problems.error(
            method.getSourcePosition(),
            "'%s' cannot be assigned JavaScript name '%s' that is different from the"
                + " JavaScript name of a method it overrides ('%s' with JavaScript name '%s').",
            member.getReadableDescription(),
            jsName,
            overriddenMethodDescriptor.getReadableDescription(),
            parentName);
        break;
      }
    }
  }

  private void checkQualifiedJsName(Type type) {
    if (type.getDeclaration().isStarOrUnknown()) {
      if (!type.isNative() || !type.isInterface() || !JsUtils.isGlobal(type.getJsNamespace())) {
        problems.error(
            type.getSourcePosition(),
            "Only native interfaces in the global namespace can be named '%s'.",
            type.getSimpleJsName());
      }
      return;
    }

    checkJsName(type);

    String namespace = type.getJsNamespace();
    // Permit empty namespaces on native JsTypes to represent a top-level non-extern type. This
    // works since the first component of the name is always used as the last component of the
    // import statement.
    boolean isValidEmptyNamespace = namespace.isEmpty() && type.getDeclaration().isNative();

    if (!isValidEmptyNamespace) {
      checkJsNamespace(type);
    }
  }

  private boolean checkQualifiedJsName(Member member) {
    if (member.isConstructor()) {
      // Constructors always inherit their name and namespace from the enclosing type.
      // The corresponding checks are done for the type separately.
      return true;
    }

    if (!isValidUnnamedJsMember(member) && !checkJsName(member)) {
      return false;
    }

    if (member.getJsNamespace() == null) {
      return true;
    }

    if (member
        .getJsNamespace()
        .equals(member.getDescriptor().getEnclosingTypeDescriptor().getQualifiedJsName())) {
      // Namespace set by the enclosing type has already been checked.
      return true;
    }

    if (!member.isStatic()) {
      problems.error(
          member.getSourcePosition(),
          "Instance member '%s' cannot declare a namespace.",
          member.getReadableDescription());
      return false;
    }

    if (!member.isNative()) {
      problems.error(
          member.getSourcePosition(),
          "Non-native member '%s' cannot declare a namespace.",
          member.getReadableDescription());
      return false;
    }

    return checkJsNamespace(member);
  }

  /**
   * Checks if the given member is properly using an empty JS name.
   *
   * <p>A valid use-case for this is for when you need to refer to a JS property/function that is
   * itself a namespace.
   */
  private static boolean isValidUnnamedJsMember(Member member) {
    String jsName = member.getSimpleJsName();
    if (jsName == null || !jsName.isEmpty() || !member.isStatic()) {
      return false;
    }
    // If you're unnammed then you must have an explicit namespace.
    if (member.getDescriptor().getJsInfo().getJsNamespace() == null) {
      return false;
    }
    return member.isMethod() && member.isNative();
  }

  private void checkJsOverlay(Member member) {
    if (member.getDescriptor().isSynthetic()) {
      return;
    }

    MemberDescriptor memberDescriptor = member.getDescriptor();
    String readableDescription = memberDescriptor.getReadableDescription();
    if (!memberDescriptor.getEnclosingTypeDescriptor().isNative()
        && !memberDescriptor.getEnclosingTypeDescriptor().isJsFunctionInterface()) {
      problems.error(
          member.getSourcePosition(),
          "JsOverlay '%s' can only be declared in a native type or @JsFunction interface.",
          readableDescription);
    }

    if (memberDescriptor.isJsMember()) {
      problems.error(
          member.getSourcePosition(),
          "JsOverlay '%s' cannot be nor override a JsProperty or a JsMethod.",
          readableDescription);
      return;
    }
    if (member.isMethod()) {
      if (!isEffectivelyFinal(memberDescriptor)
          && !memberDescriptor.isStatic()
          && !memberDescriptor.isDefaultMethod()) {
        problems.error(
            member.getSourcePosition(),
            "JsOverlay method '%s' has to be final.",
            readableDescription);
        return;
      }
    }

    if (member.isField() && !memberDescriptor.isStatic()) {
      problems.error(
          member.getSourcePosition(),
          "JsOverlay field '%s' can only be static.",
          readableDescription);
    }

    checkImplementableStatically(member, "JsOverlay");
  }

  // Do not move this one to MemberDescriptor since getMemberTypeDeclarations is not correct for
  // enums with subtypes from the dependencies.
  boolean isEffectivelyFinal(MemberDescriptor memberDescriptor) {
    TypeDeclaration enclosingTypeDeclaration =
        memberDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration();
    return memberDescriptor.isFinal()
        || memberDescriptor.getVisibility().isPrivate()
        || enclosingTypeDeclaration.isFinal()
        // TODO(b/341721484) : Remove this once the bug is fixed.
        || (enclosingTypeDeclaration.isEnum()
            && enclosingTypeDeclaration.getMemberTypeDeclarations().isEmpty());
  }

  private boolean checkNativeJsType(Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    String readableDescription = typeDeclaration.getReadableDescription();

    if (type.isEnumOrSubclass()) {
      problems.error(
          type.getSourcePosition(),
          "Enum '%s' cannot be a native JsType.%s",
          readableDescription,
          // TODO(b/288145698): After supporting native JsEnum for Wasm, add the JsEnum text to
          // error messages.
          checkWasmRestrictions ? "" : " Use '@JsEnum(isNative = true)' instead.");
      return false;
    }
    if (typeDeclaration.isCapturingEnclosingInstance()) {
      problems.error(
          type.getSourcePosition(),
          "Non static inner class '%s' cannot be a native JsType.",
          readableDescription);
      return false;
    }

    type.getSuperTypesStream()
        .filter(Predicates.not(TypeDescriptors::isJavaLangObject))
        .filter(Predicates.not(TypeDescriptor::isNative))
        .findFirst()
        .ifPresent(
            t ->
                problems.error(
                    type.getSourcePosition(),
                    "Native JsType '%s' can only %s native JsType %s.",
                    readableDescription,
                    type.isInterface() || t.isClass() ? "extend" : "implement",
                    t.isClass() ? "classes" : "interfaces"));

    if (type.hasInstanceInitializerBlocks()) {
      problems.error(
          type.getSourcePosition(),
          "Native JsType '%s' cannot have an instance initializer.",
          type.getDeclaration().getReadableDescription());
    }
    return true;
  }

  private void checkMemberOfNativeJsType(Member member) {
    MemberDescriptor memberDescriptor = member.getDescriptor();
    if (memberDescriptor.isJsOverlay() || memberDescriptor.isSynthetic()) {
      return;
    }

    String readableDescription = member.getReadableDescription();
    JsMemberType jsMemberType = memberDescriptor.getJsInfo().getJsMemberType();
    switch (jsMemberType) {
      case CONSTRUCTOR:
        if (!((Method) member).isEmpty()) {
          problems.error(
              member.getSourcePosition(),
              "Native JsType constructor '%s' cannot have non-empty method body.",
              readableDescription);
          return;
        }
        break;
      case METHOD:
      case GETTER:
      case SETTER:
        if (!member.isAbstract() && !member.isNative()) {
          problems.error(
              member.getSourcePosition(),
              "Native JsType method '%s' should be native, abstract or JsOverlay.",
              readableDescription);
          return;
        }
        break;
      case PROPERTY:
        Field field = (Field) member;
        if (field.getDescriptor().isFinal()) {
          problems.error(
              field.getSourcePosition(),
              "Native JsType field '%s' cannot be final.",
              member.getReadableDescription());
          return;
        } else if (field.hasInitializer()) {
          problems.error(
              field.getSourcePosition(),
              "Native JsType field '%s' cannot have initializer.",
              readableDescription);
          return;
        }
        break;
      case NONE:
        problems.error(
            member.getSourcePosition(),
            "Native JsType member '%s' cannot have @JsIgnore.",
            readableDescription);
        return;
      case UNDEFINED_ACCESSOR:
        // Nothing to check here. An error will be emitted for UNDEFINED_ACCESSOR elsewhere.
        return;
    }

    if (!checkWasmRestrictions) {
      return;
    }

    // At this point all JsMember methods reach here and those are all implemented by the native
    // type.
    checkState(memberDescriptor.isJsMember());

    // Check Wasm restrictions on native JsMember methods and fields.
    if (member.isMethod()) {
      checkWasmNativeMethodSignature((Method) member);
    } else {
      Field field = (Field) member;
      if (!canCrossWasmJavaScriptBoundary(field.getDescriptor().getTypeDescriptor())) {
        problems.error(
            field.getSourcePosition(),
            "Native JsType field '%s' cannot be of type '%s'.",
            readableDescription,
            field.getDescriptor().getTypeDescriptor().getReadableDescription());
      }
    }
  }

  private void checkJsFunction(Type type) {
    String readableDescription = type.getDeclaration().getReadableDescription();
    if (!type.getDeclaration().isFunctionalInterface()) {
      problems.error(
          type.getSourcePosition(),
          "JsFunction '%s' has to be a functional interface.",
          readableDescription);
      return;
    }

    if (!type.getSuperInterfaceTypeDescriptors().isEmpty()) {
      problems.error(
          type.getSourcePosition(),
          "JsFunction '%s' cannot extend other interfaces.",
          readableDescription);
    }

    if (type.getDeclaration().isJsType()) {
      problems.error(
          type.getSourcePosition(),
          "'%s' cannot be both a JsFunction and a JsType at the same time.",
          readableDescription);
    }

    for (Member member : type.getMembers()) {
      checkMemberOfJsFunction(type, member);
    }
  }

  private void checkMemberOfJsFunction(Type type, Member member) {
    MemberDescriptor memberDescriptor = member.getDescriptor();
    String messagePrefix = "JsFunction interface";

    if (memberDescriptor.isSynthetic()) {
      return;
    }

    if (!checkNotJsMember(member, messagePrefix)) {
      return;
    }

    if (member.isMethod()) {
      Method method = (Method) member;
      if (method.getDescriptor().isOrOverridesJsFunction()) {
        checkJsFunctionMethodSignature(type, method);
        return;
      }
    }

    checkMustBeJsOverlay(member, messagePrefix);
  }

  private void checkMethodWasmInfo(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    if (method.getWasmInfo() != null && !(method.isNative() && method.isStatic())) {
      problems.warning(
          method.getSourcePosition(),
          "Wasm method '%s' needs to be static native",
          methodDescriptor.getReadableDescription());
    }
  }

  private void checkWasmNativeMethodSignature(Method method) {
    if (method.getWasmInfo() != null) {
      return;
    }

    checkMethodSignature(method, JsInteropRestrictionsChecker::canCrossWasmJavaScriptBoundary, "");
  }

  /**
   * Checks whether the specified type can be imported from JavaScript or exported to JavaScript
   * from Wasm.
   */
  private static boolean canCrossWasmJavaScriptBoundary(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isPrimitive()
        || TypeDescriptors.isJavaLangString(typeDescriptor)
        || typeDescriptor.isNative();
  }

  private void checkMethodSignature(
      MethodLike methodLike, Predicate<TypeDescriptor> checkType, String errorMessageSuffix) {
    for (Variable parameter : methodLike.getParameters()) {
      TypeDescriptor parameterTypeDescriptor = parameter.getTypeDescriptor();
      SourcePosition sourcePosition = parameter.getSourcePosition();
      if (!checkType.test(parameterTypeDescriptor)) {
        problems.error(
            sourcePosition == SourcePosition.NONE ? methodLike.getSourcePosition() : sourcePosition,
            "Parameter '%s' in '%s' cannot be of type '%s'.%s",
            parameter.getName(),
            methodLike.getReadableDescription(),
            parameterTypeDescriptor.getReadableDescription(),
            errorMessageSuffix);
      }
    }

    if (methodLike.getDescriptor() == null) {
      // TODO(b/115566064): Emit the correct error for lambdas that are not JsFunctions and
      // return JsEnum arrays.
      return;
    }
    TypeDescriptor returnTypeDescriptor = methodLike.getDescriptor().getReturnTypeDescriptor();
    if (!checkType.test(returnTypeDescriptor)) {
      problems.error(
          methodLike.getSourcePosition(),
          "Return type of '%s' cannot be of type '%s'.%s",
          methodLike.getReadableDescription(),
          returnTypeDescriptor.getReadableDescription(),
          errorMessageSuffix);
    }
  }

  private void checkJsFunctionMethodSignature(Type type, Method method) {
    Set<TypeDeclaration> foundJsFunctions = new LinkedHashSet<>();

    Queue<MethodDescriptor> unexploredJsFunctionMethods = new ArrayDeque<>();
    MethodDescriptor jsFunctionMethodDescriptor = method.getDescriptor();

    if (!jsFunctionMethodDescriptor.getTypeParameterTypeDescriptors().isEmpty()) {
      problems.error(
          method.getSourcePosition(),
          "JsFunction '%s' cannot declare type parameters. Type parameters must "
              + "be declared on the enclosing interface instead.",
          method.getReadableDescription());
      return;
    }
    unexploredJsFunctionMethods.offer(jsFunctionMethodDescriptor);

    MethodDescriptor jsFunctionMethod;
    while ((jsFunctionMethod = unexploredJsFunctionMethods.poll()) != null) {

      Set<TypeDescriptor> referencedTypes = new LinkedHashSet<>();
      // Find references to JsFunctions in the parameters,
      jsFunctionMethod
          .getParameterTypeDescriptors()
          .forEach(t -> addReferencedTypes(t, referencedTypes));
      // and in the return type,
      addReferencedTypes(jsFunctionMethod.getReturnTypeDescriptor(), referencedTypes);

      // only explore further the newly found jsfunctions.
      referencedTypes.forEach(
          t -> {
            if (!t.isJsFunctionInterface()) {
              return;
            }
            DeclaredTypeDescriptor jsFunctionType = (DeclaredTypeDescriptor) t;
            if (foundJsFunctions.add(jsFunctionType.getTypeDeclaration())) {
              unexploredJsFunctionMethods.offer(
                  jsFunctionType.getDeclarationDescriptor().getJsFunctionMethodDescriptor());
            }
          });

      if (foundJsFunctions.contains(type.getDeclaration())) {
        problems.error(
            method.getSourcePosition(),
            "JsFunction '%s' cannot refer recursively to itself %s(b/153591461).",
            method.getReadableDescription(),
            jsFunctionMethod.getEnclosingTypeDescriptor() == type.getTypeDescriptor()
                ? ""
                : "(via " + jsFunctionMethod.getReadableDescription() + ") ");
        return;
      }
    }
  }

  // Collects all references to JsFunctions that appear in the type signature.
  private static void addReferencedTypes(
      TypeDescriptor typeDescriptor, Set<TypeDescriptor> referencedTypes) {
    if (!referencedTypes.add(typeDescriptor)) {
      return;
    }
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
      declaredTypeDescriptor
          .getTypeArgumentDescriptors()
          .forEach(t -> addReferencedTypes(t, referencedTypes));
    } else if (typeDescriptor.isArray()) {
      addReferencedTypes(
          ((ArrayTypeDescriptor) typeDescriptor).getLeafTypeDescriptor(), referencedTypes);
    } else if (typeDescriptor.isIntersection()) {
      ((IntersectionTypeDescriptor) typeDescriptor)
          .getIntersectionTypeDescriptors()
          .forEach(t -> addReferencedTypes(t, referencedTypes));
    } else if (typeDescriptor.isTypeVariable()) {
      addReferencedTypes(
          ((TypeVariable) typeDescriptor).getUpperBoundTypeDescriptor(), referencedTypes);
    } else {
      checkState(typeDescriptor.isPrimitive());
    }
  }

  private void checkMustBeJsOverlay(Member member, String messagePrefix) {
    MemberDescriptor memberDescriptor = member.getDescriptor();
    if (memberDescriptor.isJsOverlay()) {
      return;
    }

    problems.error(
        member.getSourcePosition(),
        "%s '%s' cannot declare non-JsOverlay member '%s'.",
        messagePrefix,
        memberDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration().getReadableDescription(),
        member.getReadableDescription());
  }

  private void checkJsFunctionImplementation(Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    String readableDescription = typeDeclaration.getReadableDescription();
    if (!typeDeclaration.isFinal() && !typeDeclaration.isAnonymous()) {
      problems.error(
          type.getSourcePosition(),
          "JsFunction implementation '%s' must be final.",
          readableDescription);
    }

    if (typeDeclaration.isJsType()) {
      problems.error(
          type.getSourcePosition(),
          "'%s' cannot be both a JsFunction implementation and a JsType at the same time.",
          readableDescription);
    }

    if (type.getSuperInterfaceTypeDescriptors().size() != 1) {
      problems.error(
          type.getSourcePosition(),
          "JsFunction implementation '%s' cannot implement more than one interface.",
          readableDescription);
      return;
    }

    if (!TypeDescriptors.isJavaLangObject(type.getSuperTypeDescriptor())) {
      problems.error(
          type.getSourcePosition(),
          "JsFunction implementation '%s' cannot extend a class.",
          readableDescription);
      return;
    }

    for (Member member : type.getMembers()) {
      checkMemberOfJsFunctionImplementation(type, member);
    }
  }

  private void checkMemberOfJsFunctionImplementation(Type type, Member member) {
    MemberDescriptor memberDescriptor = member.getDescriptor();
    if (memberDescriptor.isSynthetic()) {
      return;
    }

    checkImplementableStatically(member, "JsFunction implementation");

    if (member.isMethod()) {
      Method method = (Method) member;
      if (method.getDescriptor().isOrOverridesJsFunction()) {
        checkJsFunctionMethodSignature(type, method);
      }
    }
  }

  private void checkImplementableStatically(Member member, String messagePrefix) {
    MemberDescriptor memberDescriptor = member.getDescriptor();

    if (member.isMethod()) {
      Method method = (Method) member;
      boolean hasNonJsFunctionOverride =
          method.getDescriptor().getJavaOverriddenMethodDescriptors().stream()
              .anyMatch(Predicates.not(MethodDescriptor::isJsFunction));

      if (hasNonJsFunctionOverride) {
        // Methods that are not effectively static dispatch are disallowed.
        problems.error(
            member.getSourcePosition(),
            "%s method '%s' cannot override a supertype method.",
            messagePrefix,
            memberDescriptor.getReadableDescription());
        return;
      }

      if (method.isNative()) {
        // Only perform this check for methods to avoid giving error on fields that are not
        // explicitly marked native.
        problems.error(
            method.getSourcePosition(),
            "%s method '%s' cannot be native.",
            messagePrefix,
            method.getReadableDescription());
        return;
      }

      method.accept(
          new AbstractVisitor() {
            @Override
            public void exitSuperReference(SuperReference superReference) {
              problems.error(
                  method.getSourcePosition(),
                  "Cannot use 'super' in %s method '%s'.",
                  messagePrefix,
                  method.getReadableDescription());
            }
          });
    }

    checkNotJsMember(member, messagePrefix);
  }

  private boolean checkNotJsMember(Member member, String messagePrefix) {
    if (!member.isInitializerBlock() && member.getDescriptor().isJsMember()) {
      problems.error(
          member.getSourcePosition(),
          "%s member '%s' cannot be JsMethod nor JsProperty nor JsConstructor.",
          messagePrefix,
          member.getReadableDescription());
      return false;
    }
    return true;
  }

  private void checkJsFunctionSubtype(Type type) {
    type.getSuperTypesStream()
        .filter(DeclaredTypeDescriptor::isJsFunctionInterface)
        .findFirst()
        .ifPresent(
            superInterface ->
                problems.error(
                    type.getSourcePosition(),
                    "'%s' cannot extend JsFunction '%s'.",
                    type.getDeclaration().getReadableDescription(),
                    superInterface.getReadableDescription()));
  }

  private <T extends HasJsNameInfo & HasSourcePosition & HasReadableDescription>
      boolean checkJsName(T item) {
    String jsName = item.getSimpleJsName();
    if (jsName == null || JsUtils.isValidJsIdentifier(jsName)) {
      return true;
    }
    if (item.isNative() && JsUtils.isValidJsQualifiedName(jsName)) {
      return true;
    }

    errorInvalidName(jsName, "name", item);
    return false;
  }

  private <T extends HasJsNameInfo & HasSourcePosition & HasReadableDescription>
      boolean checkJsNamespace(T item) {
    String jsNamespace = item.getJsNamespace();
    if (jsNamespace == null
        || JsUtils.isGlobal(jsNamespace)
        || JsUtils.isValidJsQualifiedName(jsNamespace)) {
      return true;
    }

    errorInvalidName(jsNamespace, "namespace", item);
    return false;
  }

  private <T extends HasJsNameInfo & HasSourcePosition & HasReadableDescription>
      void errorInvalidName(String name, String nameType, T item) {
    if (name.isEmpty()) {
      problems.error(
          item.getSourcePosition(),
          "'%s' cannot have an empty %s.",
          item.getReadableDescription(),
          nameType);
    } else {
      problems.error(
          item.getSourcePosition(),
          "'%s' has invalid %s '%s'.",
          item.getReadableDescription(),
          nameType,
          name);
    }
  }

  private void checkMethodParameters(Method method) {
    if (checkWasmRestrictions) {
      // TODO(b/317164851): Reconsider when all the .setIgnoreXXXX is rethought.
      return;
    }
    // TODO(rluble): When overriding is included in the AST representation, add the relevant checks,
    // i.e. that a parameter can not change from optional into non optional in an override.
    boolean hasOptionalParameters = false;
    MethodDescriptor methodDescriptor = method.getDescriptor();

    int numberOfParameters = method.getParameters().size();
    Variable varargsParameter = method.getVarargsParameter();
    if (!methodDescriptor.isJsMethodVarargs() && !methodDescriptor.isOrOverridesJsMethod()) {
      // Consider the varargs parameter to be a JsVarargs if this method overrides a JsMethod but
      // it is not a JsMethod itself, just with the purpose to avoid reporting an error when
      // there is a JsOptional parameters before a varargs and the JsMethod becomes a regular
      // method due to signature incompatibility.
      varargsParameter = null;
    }
    for (int i = 0; i < numberOfParameters; i++) {
      Variable parameter = method.getParameters().get(i);
      ParameterDescriptor parameterDescriptor = methodDescriptor.getParameterDescriptors().get(i);
      TypeDescriptor parameterTypeDescriptor = parameterDescriptor.getTypeDescriptor();
      if (parameterDescriptor.isJsOptional()) {
        if (parameterTypeDescriptor.isPrimitive()) {
          problems.error(
              method.getSourcePosition(),
              "JsOptional parameter '%s' in method '%s' cannot be of a primitive type.",
              parameter.getName(),
              method.getReadableDescription());
          continue;
        }

        if (!parameterTypeDescriptor.isNullable()) {
          problems.error(
              method.getSourcePosition(),
              "JsOptional parameter '%s' in method '%s' has to be nullable.",
              parameter.getName(),
              method.getReadableDescription());
          continue;
        }

        if (parameter == varargsParameter) {
          problems.error(
              method.getSourcePosition(),
              "JsOptional parameter '%s' in method '%s' cannot be a varargs parameter.",
              parameter.getName(),
              method.getReadableDescription());
        }
        hasOptionalParameters = true;
        continue;
      }
      if (hasOptionalParameters && parameter != varargsParameter) {
        problems.error(
            method.getSourcePosition(),
            "JsOptional parameter '%s' in method '%s' cannot precede parameters that are not "
                + "JsOptional.",
            method.getParameters().get(i - 1).getName(),
            method.getReadableDescription());
        break;
      }
    }
    if (hasOptionalParameters
        && !methodDescriptor.isJsConstructor()
        && !methodDescriptor.isJsFunction()
        // Allow parameters to be marked @JsOptional even if we decide that a method will not
        // be considered a JsMethod due to having a different signature than a JsMethod it
        // overrides.
        && !methodDescriptor.isOrOverridesJsMethod()) {
      problems.error(
          method.getSourcePosition(),
          "JsOptional parameter in '%s' can only be declared in a JsMethod, a JsConstructor or a "
              + "JsFunction.",
          method.getReadableDescription());
    }

    // Check that parameters that are declared JsOptional in overridden methods remain JsOptional.
    for (MethodDescriptor overriddenMethodDescriptor :
        methodDescriptor.getJavaOverriddenMethodDescriptors()) {
      for (int i = 0; i < overriddenMethodDescriptor.getParameterDescriptors().size(); i++) {
        if (!overriddenMethodDescriptor.isParameterOptional(i)) {
          continue;
        }
        if (!methodDescriptor.isParameterOptional(i)) {
          problems.error(
              method.getSourcePosition(),
              "Method '%s' should declare parameter '%s' as JsOptional.",
              method.getReadableDescription(),
              method.getParameters().get(i).getName());
          return;
        }
      }
    }
  }

  private boolean checkJsConstructors(Type type) {
    if (type.isNative()) {
      return true;
    }

    if (!type.getDeclaration().hasJsConstructor()) {
      return true;
    }

    List<MethodDescriptor> jsConstructorDescriptors =
        type.getDeclaration().getJsConstructorMethodDescriptors();
    if (jsConstructorDescriptors.size() > 1) {
      problems.error(
          type.getSourcePosition(),
          "More than one JsConstructor exists for '%s'.",
          type.getReadableDescription());
      return false;
    }

    MethodDescriptor jsConstructorDescriptor = jsConstructorDescriptors.get(0);
    MethodDescriptor primaryConstructorDescriptor = getPrimaryConstructorDescriptor(type);
    if (primaryConstructorDescriptor != jsConstructorDescriptor) {
      problems.error(
          type.getSourcePosition(),
          "JsConstructor '%s' can be a JsConstructor only if all other constructors in the class "
              + "delegate to it.",
          jsConstructorDescriptor.getReadableDescription());
      return false;
    }

    // TODO(b/129550499): Remove this check once NormalizeConstructors is fixed to handle arbitrary
    // constructor delegation chains for JsConstructor classes.
    for (Method constructor : type.getConstructors()) {
      if (constructor.getDescriptor().isJsConstructor()) {
        continue;
      }
      MethodDescriptor delegatedConstructor =
          AstUtils.getConstructorInvocation(constructor).getTarget();
      if (delegatedConstructor == null || !delegatedConstructor.isJsConstructor()) {
        problems.error(
            type.getSourcePosition(),
            "Constructor '%s' should delegate to the JsConstructor '%s'. (b/129550499)",
            constructor.getReadableDescription(),
            jsConstructorDescriptor.getReadableDescription());
        return false;
      }
    }

    return true;
  }

  @Nullable
  private static MethodDescriptor getPrimaryConstructorDescriptor(final Type type) {
    if (type.getConstructors().isEmpty()) {
      return type.getDeclaration().getDeclaredMethodDescriptors().stream()
          .filter(MethodDescriptor::isConstructor)
          .collect(onlyElement());
    }

    ImmutableList<Method> superDelegatingConstructors =
        type.getConstructors().stream()
            .filter(Predicates.not(AstUtils::hasThisCall))
            .collect(toImmutableList());
    return superDelegatingConstructors.size() != 1
        ? null
        : superDelegatingConstructors.get(0).getDescriptor();
  }

  private void checkJsConstructorSubtype(Type type) {
    if (type.isNative()) {
      return;
    }

    if (!type.getDeclaration().isJsConstructorSubtype()) {
      return;
    }

    if (!type.getDeclaration().hasJsConstructor()) {
      problems.error(
          type.getSourcePosition(),
          "Class '%s' should have a JsConstructor.",
          type.getReadableDescription());
      return;
    }

    List<MethodDescriptor> superJsConstructorMethodDescriptors =
        type.getSuperTypeDescriptor().getTypeDeclaration().getJsConstructorMethodDescriptors();

    Method jsConstructor = getJsConstructor(type);
    if (jsConstructor == null) {
      // The JsConstructor is the implicit constructor and delegates to the default constructor
      // for the super class.
      MethodDescriptor implicitJsConstructorDescriptor =
          type.getDeclaration().getJsConstructorMethodDescriptors().get(0);
      if (!type.getSuperTypeDescriptor()
          .getDefaultConstructorMethodDescriptor()
          .isJsConstructor()) {
        problems.error(
            type.getSourcePosition(),
            "Implicit JsConstructor '%s' can only delegate to super JsConstructor '%s'.",
            implicitJsConstructorDescriptor.getReadableDescription(),
            superJsConstructorMethodDescriptors.get(0).getReadableDescription());
      }
      return;
    }

    MethodDescriptor delegatedSuperConstructor =
        AstUtils.getDelegatedSuperConstructorDescriptor(jsConstructor);
    if (!delegatedSuperConstructor.isJsConstructor()) {
      problems.error(
          jsConstructor.getSourcePosition(),
          "JsConstructor '%s' can only delegate to super JsConstructor '%s'.",
          jsConstructor.getDescriptor().getReadableDescription(),
          superJsConstructorMethodDescriptors.get(0).getReadableDescription());
    }
  }

  @Nullable
  private static Method getJsConstructor(Type type) {
    return type.getConstructors().stream()
        .filter(constructor -> constructor.getDescriptor().isJsConstructor())
        .findFirst()
        .orElse(null);
  }

  private boolean checkJsPropertyAccessor(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    JsMemberType memberType = methodDescriptor.getJsInfo().getJsMemberType();

    if (methodDescriptor.getSimpleJsName() == null) {
      checkArgument(memberType.isPropertyAccessor());
      problems.error(
          method.getSourcePosition(),
          "JsProperty '%s' should either follow Java Bean naming conventions or provide a name.",
          method.getReadableDescription());
      return false;
    }

    switch (memberType) {
      case UNDEFINED_ACCESSOR:
        problems.error(
            method.getSourcePosition(),
            "JsProperty '%s' should have a correct setter or getter signature.",
            method.getReadableDescription());
        break;
      case GETTER:
        TypeDescriptor returnTypeDescriptor = methodDescriptor.getReturnTypeDescriptor();
        if (methodDescriptor.getName().startsWith("is")
            && !TypeDescriptors.isPrimitiveBoolean(returnTypeDescriptor)) {
          problems.error(
              method.getSourcePosition(),
              "JsProperty '%s' cannot have a non-boolean return.",
              method.getReadableDescription());
        }
        break;
      case SETTER:
        if (methodDescriptor.isVarargs()) {
          problems.error(
              method.getSourcePosition(),
              "JsProperty '%s' cannot have a vararg parameter.",
              method.getReadableDescription());
        }
        break;
      default:
        break;
    }

    return true;
  }

  @CanIgnoreReturnValue
  private boolean checkJsPropertyConsistency(
      SourcePosition sourcePosition, MethodDescriptor thisMember, MethodDescriptor thatMember) {
    MethodDescriptor setter = thisMember.isJsPropertySetter() ? thisMember : thatMember;
    MethodDescriptor getter = thisMember.isJsPropertyGetter() ? thisMember : thatMember;

    ImmutableList<TypeDescriptor> setterParams = setter.getParameterTypeDescriptors();
    if (!getter.getReturnTypeDescriptor().isSameBaseType(setterParams.get(0))) {
      problems.error(
          sourcePosition,
          "JsProperty setter '%s' and getter '%s' cannot have inconsistent types.",
          setter.getReadableDescription(),
          getter.getReadableDescription());
      return false;
    }
    return true;
  }

  /**
   * Checks whether a type accidentally overrides a JsMethod with another of different signature
   * creating a name collision.
   */
  private void checkNameCollisions(Type type) {
    Multimap<String, MemberDescriptor> instanceJsMembersByName =
        collectInstanceNames(type.getTypeDescriptor());
    Multimap<String, MemberDescriptor> staticJsMembersByName =
        collectStaticNames(type.getTypeDescriptor());

    ImmutableMap<MemberDescriptor, SourcePosition> sourcePositionByDescriptor =
        type.getMembers().stream()
            .filter(Predicates.not(Member::isInitializerBlock))
            .filter(m -> m.getDescriptor().isJsMember())
            .collect(toImmutableMap(Member::getDescriptor, Member::getSourcePosition));

    checkNameCollisions(type, sourcePositionByDescriptor, instanceJsMembersByName);
    checkNameCollisions(type, sourcePositionByDescriptor, staticJsMembersByName);
  }

  private void checkNameCollisions(
      Type type,
      Map<MemberDescriptor, SourcePosition> sourcePositionsByDescriptor,
      Multimap<String, MemberDescriptor> membersByJsName) {
    for (Entry<String, Collection<MemberDescriptor>> entry : membersByJsName.asMap().entrySet()) {
      String name = entry.getKey();
      if (name == null || !JsUtils.isValidJsIdentifier(name)) {
        // Don't check for collisions if the jsname is invalid, as validity of the names is
        // checked independently.
        continue;
      }

      Collection<MemberDescriptor> potentiallyCollidingMembers = entry.getValue();
      if (potentiallyCollidingMembers.size() < 2) {
        // At most one member, so no collision is possible.
        continue;
      }

      // At this point potentiallyCollidingMembers has members for the current type and super types
      // that are not overrides of each other but have the same js name.

      // We select the last member on this list to prefer a member for current type, which we have
      // the source position of, so that we can report the error line accurately for the most
      // common case, which is when the conflict happens due to a member of the current type.
      MemberDescriptor memberDescriptor = Iterables.getLast(potentiallyCollidingMembers);

      // If the member has a source position use it; otherwise the conflict was introduced by
      // subclassing supertypes, hence use the type source position to report the error.
      SourcePosition sourcePosition =
          sourcePositionsByDescriptor.getOrDefault(memberDescriptor, type.getSourcePosition());

      checkNameCollisions(sourcePosition, memberDescriptor, potentiallyCollidingMembers);
    }
  }

  private void checkNameCollisions(
      SourcePosition sourcePosition,
      MemberDescriptor memberDescriptor,
      Collection<MemberDescriptor> potentiallyCollidingMembers) {
    // At this point memberDescriptor is the last member in potentiallyCollidingMembers, so
    // select a different one to check for collisions and report the error.

    // We are currently selecting the first member, which in the case of multiple colliding members
    // it would potentially be the furthest up the hierarchy since supers are collected first. This
    // might result in suboptimal error messages in the unlikely situation that there are multiple
    // collisions on a name that happen up in the hierarchy and the collision is not detected
    // in other types.
    MemberDescriptor potentiallyCollidingMember = potentiallyCollidingMembers.iterator().next();
    checkState(memberDescriptor != potentiallyCollidingMember);

    if (potentiallyCollidingMembers.size() == 2
        && isJsPropertyAccessorPair(memberDescriptor, potentiallyCollidingMember)) {
      checkJsPropertyConsistency(
          sourcePosition,
          (MethodDescriptor) memberDescriptor,
          (MethodDescriptor) potentiallyCollidingMember);
      return;
    }

    problems.error(
        sourcePosition,
        "'%s' and '%s' cannot both use the same JavaScript name '%s'.",
        memberDescriptor.getReadableDescription(),
        potentiallyCollidingMember.getReadableDescription(),
        memberDescriptor.getSimpleJsName());
  }

  private static boolean isJsPropertyAccessorPair(
      MemberDescriptor thisMember, MemberDescriptor thatMember) {
    return (thisMember.isJsPropertyGetter() && thatMember.isJsPropertySetter())
        || (thatMember.isJsPropertyGetter() && thisMember.isJsPropertySetter());
  }

  private static boolean isInstanceJsMember(MemberDescriptor memberDescriptor) {
    return memberDescriptor.isInstanceMember()
        && memberDescriptor.isJsMember()
        && !memberDescriptor.isSynthetic();
  }

  private static boolean isStaticJsMember(MemberDescriptor memberDescriptor) {
    // Constructors are checked specifically to give a better error message.
    return memberDescriptor.isStatic()
        && memberDescriptor.isJsMember()
        && !memberDescriptor.isSynthetic();
  }

  private static Multimap<String, MemberDescriptor> collectInstanceNames(
      DeclaredTypeDescriptor typeDescriptor) {
    LinkedHashMultimap<String, MemberDescriptor> instanceJsMembersByName =
        LinkedHashMultimap.create();
    collectInstanceNames(typeDescriptor, instanceJsMembersByName);
    return instanceJsMembersByName;
  }

  private static void collectInstanceNames(
      DeclaredTypeDescriptor typeDescriptor,
      Multimap<String, MemberDescriptor> instanceJsMembersByName) {
    if (typeDescriptor == null) {
      return;
    }

    typeDescriptor
        .getSuperTypesStream()
        .forEach(t -> collectInstanceNames(t, instanceJsMembersByName));
    for (MemberDescriptor member : typeDescriptor.getDeclaredMemberDescriptors()) {
      if (isInstanceJsMember(member)) {
        addMember(instanceJsMembersByName, member);
      }
    }
  }

  private static Multimap<String, MemberDescriptor> collectStaticNames(
      DeclaredTypeDescriptor typeDescriptor) {
    Multimap<String, MemberDescriptor> staticMembersByName = LinkedHashMultimap.create();
    for (MemberDescriptor member : typeDescriptor.getDeclaredMemberDescriptors()) {
      if (isStaticJsMember(member)) {
        addMember(staticMembersByName, member);
      }
    }
    return staticMembersByName;
  }

  private static void addMember(
      Multimap<String, MemberDescriptor> memberByMemberName, MemberDescriptor member) {
    String name = member.getSimpleJsName();

    // Remove methods that overridden by this member to keep only one representative method
    // for each override signature in the collision list;
    Collection<MemberDescriptor> members = memberByMemberName.get(name);
    if (!member.isNative() || !member.getEnclosingTypeDescriptor().isInterface()) {
      // To achieve  what we call "one live implementation" rule, only remove overrides for
      // (1) non-native methods since member will be added as a representative override for that
      // name; and (2) from native methods if it is a class, since those explicitly remove the
      // conflict.

      // Match also the declaration to allow for the cases in which the types in kotlin and
      // Java disagree. For example MyList<String>.contains(String) needs to be considered an
      // override of List<String>.contains(Object).
      members.removeIf(
          m ->
              isCompatibleOverride(member, m)
                  || isCompatibleOverride(member.getDeclarationDescriptor(), m));
    }

    // Don't collect native members since those are never involved in collisions.
    if (!member.isNative()) {
      members.add(member);
    }
  }

  private static boolean isCompatibleOverride(
      MemberDescriptor member, MemberDescriptor potentiallyOverriddenMember) {
    if (!member.isMethod() || !potentiallyOverriddenMember.isMethod()) {
      return false;
    }

    // TODO(b/254859483): Remove getDeclarationDescriptor in the next line and in the return
    // statement once this is fixed. The treatment of methods like Collections.contains(Object) as
    // being overridden by SomeCollectionSubclass.contains(T) breaks the check here since there
    // is an inconsistency on whether they are considered overrides and whether they inherit the
    // JsInfo.
    MethodDescriptor method = (MethodDescriptor) member.getDeclarationDescriptor();
    MethodDescriptor potentiallyOverriddenMethod = (MethodDescriptor) potentiallyOverriddenMember;
    return (method.isOverride(potentiallyOverriddenMethod)
            || method.isOverride(potentiallyOverriddenMethod.getDeclarationDescriptor()))
        && method.canInheritsJsInfoFrom(potentiallyOverriddenMethod);
  }

  private void checkUnusableByJs(Member member) {
    if (isUnusableByJsSuppressed(member.getDescriptor())) {
      return;
    }

    if (member.isField()) {
      FieldDescriptor fieldDescriptor = (FieldDescriptor) member.getDescriptor();
      TypeDescriptor fieldTypeDescriptor = fieldDescriptor.getTypeDescriptor();
      warnIfUnusableByJs(
          fieldTypeDescriptor,
          String.format("Type '%s' of field", fieldTypeDescriptor.getReadableDescription()),
          member);
    }

    if (member.isMethod()) {
      Method method = (Method) member;

      TypeDescriptor returnTypeDescriptor = method.getDescriptor().getReturnTypeDescriptor();
      warnIfUnusableByJs(returnTypeDescriptor, "Return type of", member);

      Variable varargsParameter = method.getJsVarargsParameter();
      for (Variable parameter : method.getParameters()) {
        if (!parameter.isUnusableByJsSuppressed()) {
          TypeDescriptor parameterTypeDescriptor =
              parameter == varargsParameter
                  ? ((ArrayTypeDescriptor) parameter.getTypeDescriptor())
                      .getComponentTypeDescriptor()
                  : parameter.getTypeDescriptor();
          String prefix = String.format("Type of parameter '%s' in", parameter.getName());
          warnIfUnusableByJs(parameterTypeDescriptor, prefix, member);
        }
      }
    }
  }

  private static boolean isUnusableByJsSuppressed(MemberDescriptor memberDescriptor) {
    // TODO(b/36227943): Abide by standard rules regarding suppression annotations in
    // enclosing elements.
    return memberDescriptor.isUnusableByJsSuppressed()
        || isUnusableByJsSuppressed(memberDescriptor.getEnclosingTypeDescriptor());
  }

  private static boolean isUnusableByJsSuppressed(DeclaredTypeDescriptor typeDescriptor) {
    // TODO(b/36227943): Abide by standard rules regarding suppression annotations in
    // enclosing elements.
    if (typeDescriptor.isUnusableByJsSuppressed()) {
      return true;
    }

    DeclaredTypeDescriptor enclosingTypeDescriptor = typeDescriptor.getEnclosingTypeDescriptor();
    return enclosingTypeDescriptor != null && isUnusableByJsSuppressed(enclosingTypeDescriptor);
  }

  private void warnIfUnusableByJs(TypeDescriptor typeDescriptor, String prefix, Member member) {
    if (typeDescriptor.canBeReferencedExternally()) {
      return;
    }

    warnUnusableByJs(prefix, member);
  }

  private void warnUnusableByJs(String prefix, Member member) {
    // TODO(b/36362935): consider [unusable-by-js] (suppressible) errors instead of warnings.
    problems.warning(
        member.getSourcePosition(),
        "[unusable-by-js] %s '%s' is not usable by but exposed to JavaScript.",
        prefix,
        member.getReadableDescription());
    wasUnusableByJsWarningReported = true;
  }

  private void checkTypeAssignments(
      Type type,
      BiFunction<TypeDescriptor, TypeDescriptor, Boolean> isAssignmentDisallowed,
      String errorMessageSuffix) {
    type.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                if (isAssignmentDisallowed(
                    declaredTypeDescriptor, expression, isAssignmentDisallowed)) {
                  problems.error(
                      getSourcePosition(),
                      "%s cannot be assigned to %s.%s",
                      getReadableDescriptionWithPrefix(expression.getTypeDescriptor()),
                      getReadableDescriptionWithPrefix(declaredTypeDescriptor),
                      errorMessageSuffix);
                }
                return expression;
              }

              @Override
              public Expression rewriteMemberQualifierContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression qualifierExpression) {
                if (isAssignmentDisallowed(
                    declaredTypeDescriptor, qualifierExpression, isAssignmentDisallowed)) {
                  problems.error(
                      getSourcePosition(),
                      "Cannot access member of %s with %s.%s",
                      getReadableDescriptionWithPrefix(declaredTypeDescriptor),
                      getReadableDescriptionWithPrefix(qualifierExpression.getTypeDescriptor()),
                      errorMessageSuffix);
                }
                return qualifierExpression;
              }

              @Override
              public Expression rewriteCastContext(CastExpression castExpression) {
                if (isAssignmentDisallowed(
                    castExpression.getCastTypeDescriptor(),
                    castExpression.getExpression(),
                    isAssignmentDisallowed)) {
                  problems.error(
                      getSourcePosition(),
                      "%s cannot be cast to %s.%s",
                      getReadableDescriptionWithPrefix(
                          castExpression.getExpression().getTypeDescriptor()),
                      getReadableDescriptionWithPrefix(castExpression.getCastTypeDescriptor()),
                      errorMessageSuffix);
                }
                return castExpression;
              }
            }));
  }

  private boolean isAssignmentDisallowed(
      TypeDescriptor toTypeDescriptor,
      Expression expression,
      BiFunction<TypeDescriptor, TypeDescriptor, Boolean> isAssignmentDisallowed) {
    // Allow NullLiteral to be assigned. NullLiteral is typed as Object until a later normalization
    // pass.
    if (expression instanceof NullLiteral) {
      return false;
    }

    return isAssignmentDisallowed.apply(toTypeDescriptor, expression.getTypeDescriptor());
  }

  /**
   * Returns the readable description of the specified type with a descriptive prefix (see {@link
   * #getTypeKindPrefix}).
   */
  private static String getReadableDescriptionWithPrefix(TypeDeclaration typeDeclaration) {
    return getReadableDescriptionWithPrefix(typeDeclaration.toDescriptor());
  }

  /**
   * Returns the readable description of the specified type with a descriptive prefix (see {@link
   * #getTypeKindPrefix}).
   */
  private static String getReadableDescriptionWithPrefix(TypeDescriptor typeDescriptor) {
    return getTypeKindPrefix(typeDescriptor) + "'" + typeDescriptor.getReadableDescription() + "'";
  }

  /**
   * Returns a string prefix describing the specified type. For example, native types may return
   * "Native JsType ".
   */
  private static String getTypeKindPrefix(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isJsEnum()) {
      if (typeDescriptor.isNative()) {
        return "Native JsEnum ";
      }
      if (typeDescriptor.getJsEnumInfo().hasCustomValue()) {
        return "Custom-valued JsEnum ";
      }
      return "Non-custom-valued JsEnum ";
    }

    if (typeDescriptor.isNative()) {
      return "Native JsType ";
    }

    if (AstUtils.isNonNativeJsEnumArray(typeDescriptor)) {
      return "JsEnum array ";
    }

    return "";
  }

  /**
   * Checks the specified {@link Type} structure for any reference to invalid types as specified by
   * `isTypeDisallowed`.
   */
  private void checkAllowedTypes(
      Type type,
      Predicate<TypeDescriptor> isTypeDisallowed,
      boolean onlyCheckTypeSpecialization,
      boolean checkArrayComponent,
      String disallowedTypeDescription,
      String messageSuffix) {
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type nestedType) {
            TypeDescriptor superTypeDescriptor = type.getTypeDescriptor().getSuperTypeDescriptor();
            if (superTypeDescriptor == null) {
              return;
            }
            String messagePrefix =
                String.format("Supertype of '%s'", nestedType.getReadableDescription());
            errorIfDisallowedType(
                superTypeDescriptor,
                superTypeDescriptor,
                isTypeDisallowed,
                onlyCheckTypeSpecialization,
                checkArrayComponent,
                nestedType.getSourcePosition(),
                messagePrefix,
                messageSuffix);
          }

          @Override
          public void exitVariable(Variable variable) {
            if (variable.isParameter()) {
              // Parameters are checked at the declaration site to give a better error message.
              return;
            }
            TypeDescriptor variableTypeDescriptor = variable.getTypeDescriptor();
            String messagePrefix = String.format("Variable '%s'", variable.getName());
            SourcePosition sourcePosition = variable.getSourcePosition();
            errorIfDisallowedType(
                variableTypeDescriptor,
                variableTypeDescriptor,
                isTypeDisallowed,
                onlyCheckTypeSpecialization,
                checkArrayComponent,
                sourcePosition == SourcePosition.NONE
                    ? getCurrentMember().getSourcePosition()
                    : sourcePosition,
                messagePrefix,
                messageSuffix);
          }

          @Override
          public void exitMethod(Method method) {
            checkMethodSignature(
                method,
                Predicates.not(
                    t ->
                        hasDisallowedType(
                            t,
                            t,
                            isTypeDisallowed,
                            onlyCheckTypeSpecialization,
                            checkArrayComponent)),
                messageSuffix);
          }

          @Override
          public void exitFunctionExpression(FunctionExpression functionExpression) {
            checkMethodSignature(
                functionExpression,
                Predicates.not(
                    t ->
                        hasDisallowedType(
                            t,
                            t,
                            isTypeDisallowed,
                            onlyCheckTypeSpecialization,
                            checkArrayComponent)),
                messageSuffix);
          }

          @Override
          public void exitField(Field field) {
            FieldDescriptor fieldDescriptor = field.getDescriptor();
            TypeDescriptor fieldTypeDescriptor = fieldDescriptor.getTypeDescriptor();
            String messagePrefix = String.format("Field '%s'", field.getReadableDescription());
            errorIfDisallowedType(
                fieldTypeDescriptor,
                fieldTypeDescriptor,
                isTypeDisallowed,
                onlyCheckTypeSpecialization,
                checkArrayComponent,
                field.getSourcePosition(),
                messagePrefix,
                messageSuffix);
          }

          @Override
          public void exitFieldAccess(FieldAccess fieldAccess) {
            TypeDescriptor inferredTypeDescriptor = fieldAccess.getTypeDescriptor();
            TypeDescriptor declaredTypeDescriptor =
                fieldAccess.getTarget().getDeclarationDescriptor().getTypeDescriptor();
            if (inferredTypeDescriptor.equals(declaredTypeDescriptor)) {
              // No inference, the error will be given at declaration if needed.
              return;
            }
            String messagePrefix =
                String.format(
                    "Reference to field '%s'", fieldAccess.getTarget().getReadableDescription());
            errorIfDisallowedType(
                inferredTypeDescriptor,
                declaredTypeDescriptor,
                isTypeDisallowed,
                onlyCheckTypeSpecialization,
                checkArrayComponent,
                getCurrentMember().getSourcePosition(),
                messagePrefix,
                messageSuffix);
          }

          @Override
          public void exitMethodCall(MethodCall methodCall) {
            TypeDescriptor inferredTypeDescriptor =
                methodCall.getTarget().getReturnTypeDescriptor();
            TypeDescriptor declaredTypeDescriptor =
                methodCall.getTarget().getDeclarationDescriptor().getReturnTypeDescriptor();
            if (inferredTypeDescriptor.equals(declaredTypeDescriptor)) {
              // No inference, the error will be given at declaration if needed.
              return;
            }
            String messagePrefix =
                String.format(
                    "Returned type in call to method '%s'",
                    methodCall.getTarget().getReadableDescription());
            errorIfDisallowedType(
                inferredTypeDescriptor,
                declaredTypeDescriptor,
                isTypeDisallowed,
                onlyCheckTypeSpecialization,
                checkArrayComponent,
                getCurrentMember().getSourcePosition(),
                messagePrefix,
                messageSuffix);
          }

          @Override
          public void exitNewArray(NewArray newArray) {
            ArrayTypeDescriptor newArrayTypeDescriptor = newArray.getTypeDescriptor();
            // TODO(b/65465035): Emit the expression source position when it is tracked, and avoid
            // toString() in an AST nodes.
            String messagePrefix = String.format("Array creation '%s'", newArray);
            errorIfDisallowedType(
                newArrayTypeDescriptor,
                newArrayTypeDescriptor,
                isTypeDisallowed,
                onlyCheckTypeSpecialization,
                checkArrayComponent,
                getCurrentMember().getSourcePosition(),
                messagePrefix,
                messageSuffix);
          }

          @Override
          public void exitNewInstance(NewInstance newInstance) {
            TypeDescriptor instanceTypeDescriptor = newInstance.getTypeDescriptor();
            String messagePrefix = String.format("Object creation '%s'", newInstance);
            errorIfDisallowedType(
                instanceTypeDescriptor,
                instanceTypeDescriptor,
                isTypeDisallowed,
                onlyCheckTypeSpecialization,
                checkArrayComponent,
                getCurrentMember().getSourcePosition(),
                messagePrefix,
                messageSuffix);
          }

          @Override
          public void exitInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
            TypeDescriptor testTypeDescriptor = instanceOfExpression.getTestTypeDescriptor();
            if (hasDisallowedType(
                testTypeDescriptor,
                testTypeDescriptor,
                isTypeDisallowed,
                onlyCheckTypeSpecialization,
                checkArrayComponent)) {
              problems.error(
                  instanceOfExpression.getSourcePosition(),
                  "Cannot do instanceof against %s '%s'.%s",
                  disallowedTypeDescription,
                  testTypeDescriptor.getReadableDescription(),
                  messageSuffix);
            }
          }

          @Override
          public void exitCastExpression(CastExpression castExpression) {
            TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
            if (hasDisallowedType(
                castTypeDescriptor,
                castTypeDescriptor,
                isTypeDisallowed,
                onlyCheckTypeSpecialization,
                checkArrayComponent)) {
              // TODO(b/65465035): Emit the expression source position when it is tracked.
              problems.error(
                  getCurrentMember().getSourcePosition(),
                  "Cannot cast to %s '%s'.%s",
                  disallowedTypeDescription,
                  castTypeDescriptor.getReadableDescription(),
                  messageSuffix);
            }
          }
        });
  }

  private void errorIfDisallowedType(
      TypeDescriptor inferredTypeDescriptor,
      TypeDescriptor declaredTypeDescriptor,
      Predicate<TypeDescriptor> isTypeDisallowed,
      boolean onlyCheckTypeSpecialization,
      boolean checkArrayComponent,
      SourcePosition sourcePosition,
      String messagePrefix,
      String messageSuffix) {
    if (hasDisallowedType(
        inferredTypeDescriptor,
        declaredTypeDescriptor,
        isTypeDisallowed,
        onlyCheckTypeSpecialization,
        checkArrayComponent)) {
      problems.error(
          sourcePosition,
          "%s cannot be of type '%s'.%s",
          messagePrefix,
          inferredTypeDescriptor.getReadableDescription(),
          messageSuffix);
    }
  }

  /**
   * Deeply checks the given type against the specified predicate. Returns {@code true} if the
   * predicate returns true for any type, type argument, or array component.
   */
  private static boolean hasDisallowedType(
      TypeDescriptor inferredTypeDescriptor,
      TypeDescriptor declaredTypeDescriptor,
      Predicate<TypeDescriptor> isTypeDisallowed,
      boolean onlyCheckTypeSpecialization,
      boolean checkArrayComponent) {
    if (!onlyCheckTypeSpecialization && isTypeDisallowed.test(inferredTypeDescriptor)) {
      return true;
    }
    if (declaredTypeDescriptor.isTypeVariable() && isTypeDisallowed.test(inferredTypeDescriptor)) {
      return true;
    }

    if (declaredTypeDescriptor.isArray()) {
      ArrayTypeDescriptor declaredArrayTypeDescriptor =
          (ArrayTypeDescriptor) declaredTypeDescriptor;
      if (declaredArrayTypeDescriptor.getLeafTypeDescriptor().isTypeVariable()
          && isTypeDisallowed.test(inferredTypeDescriptor)) {
        return true;
      }
    }

    if (inferredTypeDescriptor.isArray()) {
      ArrayTypeDescriptor inferredArrayTypeDescriptor =
          (ArrayTypeDescriptor) inferredTypeDescriptor;
      if (checkArrayComponent) {
        return hasDisallowedType(
            inferredArrayTypeDescriptor.getComponentTypeDescriptor(),
            declaredTypeDescriptor.isArray()
                ? ((ArrayTypeDescriptor) declaredTypeDescriptor).getComponentTypeDescriptor()
                : inferredArrayTypeDescriptor.getComponentTypeDescriptor(),
            isTypeDisallowed,
            onlyCheckTypeSpecialization,
            checkArrayComponent);
      }

      // If we don't check array components (`A` in `A[]`), we should still check the type arguments
      // of the leaf type (for example, `A` in `List<A>[][]`).
      inferredTypeDescriptor = inferredArrayTypeDescriptor.getLeafTypeDescriptor();
      declaredTypeDescriptor =
          declaredTypeDescriptor.isArray()
              ? ((ArrayTypeDescriptor) declaredTypeDescriptor).getLeafTypeDescriptor()
              : inferredArrayTypeDescriptor.getLeafTypeDescriptor();
    }

    if (inferredTypeDescriptor instanceof DeclaredTypeDescriptor) {
      List<TypeDescriptor> inferredTypeArguments =
          ((DeclaredTypeDescriptor) inferredTypeDescriptor).getTypeArgumentDescriptors();
      List<TypeDescriptor> declaredTypeArguments =
          declaredTypeDescriptor instanceof DeclaredTypeDescriptor
              ? ((DeclaredTypeDescriptor) declaredTypeDescriptor).getTypeArgumentDescriptors()
              : ImmutableList.of();
      for (int typeArgIndex = 0; typeArgIndex < inferredTypeArguments.size(); typeArgIndex++) {
        if (hasDisallowedType(
            inferredTypeArguments.get(typeArgIndex),
            typeArgIndex < declaredTypeArguments.size()
                ? declaredTypeArguments.get(typeArgIndex)
                : inferredTypeArguments.get(typeArgIndex),
            isTypeDisallowed,
            /* onlyCheckTypeSpecialization= */ false,
            checkArrayComponent)) {
          return true;
        }
      }
    }
    return false;
  }
}
