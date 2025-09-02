/*
 * Copyright 2022 Google Inc.
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

import static com.google.common.base.Predicates.not;
import static com.google.j2cl.transpiler.ast.J2ktAstUtils.isSubtypeOfJ2ktMonitor;
import static com.google.j2cl.transpiler.ast.J2ktAstUtils.isValidSynchronizedStatementExpressionTypeDescriptor;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveVoid;
import static java.lang.Character.isUpperCase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.HasSourcePosition;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.SynchronizedStatement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Visibility;

/** Checks and throws errors for constructs which can not be transpiled to Kotlin. */
public final class J2ktRestrictionsChecker {
  private J2ktRestrictionsChecker() {}

  public static void check(Library library, Problems problems) {
    library.accept(
        new AbstractVisitor() {
          private SourcePosition getClosestSourcePosition() {
            HasSourcePosition hasSourcePosition =
                (HasSourcePosition) getParent(HasSourcePosition.class::isInstance);
            return hasSourcePosition != null
                ? hasSourcePosition.getSourcePosition()
                : SourcePosition.NONE;
          }

          @Override
          public void exitMethod(Method method) {
            checkNotGenericConstructor(method);
            checkReferencedTypeVisibilities(method);
            checkKtProperty(method);
            checkObjectiveCName(method);
          }

          @Override
          public void exitField(Field field) {
            checkReferencedTypeVisibilities(field);
            checkFieldShadowing(field);
          }

          @Override
          public void exitType(Type type) {
            problems.abortIfCancelled();
            checkNullMarked(type);
            checkSuperTypeVisibilities(type);
            checkInterfaceTypeVisibilities(type);
            checkSynchronizedMethods(type);
          }

          @Override
          public void exitSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
            checkSynchronizedStatement(synchronizedStatement);
          }

          @Override
          public void exitMethodCall(MethodCall methodCall) {
            checkExplicitQualifierInConstructorCall(methodCall);
          }

          private void checkNotGenericConstructor(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            if (method.isConstructor()
                && !methodDescriptor.getTypeParameterTypeDescriptors().isEmpty()) {
              problems.error(
                  method.getSourcePosition(),
                  "Constructor '%s' cannot declare type variables.",
                  method.getReadableDescription());
            }
          }

          private void checkReferencedTypeVisibilities(Member member) {
            if (member.isEnumField()) {
              return;
            }

            MemberDescriptor memberDescriptor = member.getDescriptor();
            Visibility methodVisibility = getRequiredVisibility(memberDescriptor);
            for (TypeDescriptor referencedTypeDescriptor :
                getReferencedTypeDescriptors(memberDescriptor)) {
              Visibility referencedVisibility = getRequiredVisibility(referencedTypeDescriptor);
              if (isWiderThan(methodVisibility, referencedVisibility)) {
                problems.warning(
                    member.getSourcePosition(),
                    "Member '%s' (%s) should not have wider visibility than '%s' (%s).",
                    member.getReadableDescription(),
                    getDescription(methodVisibility),
                    referencedTypeDescriptor.getReadableDescription(),
                    getDescription(referencedVisibility));
              }
            }
          }

          private void checkFieldShadowing(Field field) {
            if (shadowsAnySuperTypeField(field.getDescriptor())) {
              problems.error(
                  field.getSourcePosition(),
                  "Field '%s' cannot shadow a super type field.",
                  field.getReadableDescription());
            }
          }

          private void checkKtProperty(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            if (!methodDescriptor.isKtProperty()) {
              return;
            }

            if (methodDescriptor.isConstructor()) {
              problems.error(
                  method.getSourcePosition(),
                  "Constructor '%s' can not be '@KtProperty'.",
                  method.getReadableDescription());
            }

            if (!methodDescriptor.getParameterDescriptors().isEmpty()) {
              problems.error(
                  method.getSourcePosition(),
                  "Method '%s' can not be '@KtProperty', as it has non-empty parameters.",
                  method.getReadableDescription());
            }

            if (isPrimitiveVoid(methodDescriptor.getReturnTypeDescriptor())) {
              problems.error(
                  method.getSourcePosition(),
                  "Method '%s' can not be '@KtProperty', as it has void return type.",
                  method.getReadableDescription());
            }
          }

          /**
           * Checks that first component in @ObjectiveCName contains at least one upper-case
           * character. Otherwise, it can not be translated to Kotlin. See:
           * https://youtrack.jetbrains.com/issue/KT-80557
           */
          private void checkObjectiveCName(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();

            // Constructors and zero-arg methods are not affected.
            if (methodDescriptor.isConstructor()
                || methodDescriptor.getParameterDescriptors().isEmpty()) {
              return;
            }

            String objectiveCName = methodDescriptor.getObjectiveCName();
            if (objectiveCName == null) {
              return;
            }

            int colonIndex = objectiveCName.indexOf(':');
            if (colonIndex == -1) {
              return;
            }

            String objectiveCNameFirstComponent = objectiveCName.substring(0, colonIndex);
            for (char ch : objectiveCNameFirstComponent.toCharArray()) {
              if (isUpperCase(ch)) {
                return;
              }
            }

            problems.error(
                method.getSourcePosition(),
                "Method '%s' is annotated with '@ObjectiveCName(\"%s\")' which can not be"
                    + " translated to Kotlin. "
                    + "The first component of Objective C selector must contains at least one"
                    + " uppercase character, so it can be split in two parts and translated into"
                    + " two '@ObjCName' annotations, on function and its first parameter. "
                    + "Consider renaming to '@ObjectiveCName(\"%s\")' or removing the "
                    + "annotation. Reference bug: https://youtrack.jetbrains.com/issue/KT-80557",
                method.getReadableDescription(),
                objectiveCName,
                objectiveCNameFirstComponent
                    + "With"
                    + objectiveCName.substring(objectiveCNameFirstComponent.length()));
          }

          private void checkNullMarked(Type type) {
            // Don't check for NullMarked in our own integration and readable tests where this is
            // often intentionally omitted.
            if (isFromJ2clReadableOrIntegrationTest(type)) {
              return;
            }

            if (!type.getDeclaration().isNullMarked() && !isExemptFromNullMarked(type)) {
              if (Boolean.getBoolean(
                  "com.google.j2cl.transpiler.passes.J2ktRestrictionsChecker.treatMissingNullMarkedAsWarning")) {
                problems.warning(
                    type.getSourcePosition(),
                    "Type '%s' must be directly or indirectly @NullMarked.",
                    type.getDeclaration().getQualifiedSourceName());
              } else {
                problems.error(
                    type.getSourcePosition(),
                    "Type '%s' must be directly or indirectly @NullMarked.",
                    type.getDeclaration().getQualifiedSourceName());
              }
            }
          }

          private boolean isExemptFromNullMarked(Type type) {
            // Annotations are not propagated by J2KT anyway.
            if (type.getDeclaration().isAnnotation()) {
              return true;
            }

            // We're pretty relaxed about enums as they generally don't have nullness issues.
            if (type.getDeclaration().isEnum()) {
              return true;
            }

            // Exclude empty marker classes. These generally only exist to drive code generation
            // and don't have any practical use on their own.
            if (type.getMembers().isEmpty()
                && type.getTypes().isEmpty()
                && hasNoExplicitSuperType(type)) {
              return true;
            }

            return false;
          }

          private boolean hasNoExplicitSuperType(Type type) {
            return type.getSuperTypeDescriptor() == null
                || TypeDescriptors.isJavaLangObject(type.getSuperTypeDescriptor());
          }

          private void checkSuperTypeVisibilities(Type type) {
            TypeDeclaration typeDeclaration = type.getDeclaration();
            if (typeDeclaration.getKind() != Kind.CLASS) {
              return;
            }

            TypeDeclaration superTypeDeclaration = typeDeclaration.getSuperTypeDeclaration();
            if (superTypeDeclaration == null) {
              return;
            }

            Visibility visibility = typeDeclaration.getVisibility();
            Visibility superVisibility = superTypeDeclaration.getVisibility();
            if (isWiderThan(visibility, superVisibility)) {
              problems.warning(
                  type.getSourcePosition(),
                  "Type '%s' (%s) should not have wider visibility than its super type '%s' (%s).",
                  type.getReadableDescription(),
                  getDescription(visibility),
                  superTypeDeclaration.getReadableDescription(),
                  getDescription(superVisibility));
            }
          }

          private void checkInterfaceTypeVisibilities(Type type) {
            TypeDeclaration typeDeclaration = type.getDeclaration();
            if (typeDeclaration.getKind() != Kind.INTERFACE) {
              return;
            }

            ImmutableList<DeclaredTypeDescriptor> interfaceTypeDescriptors =
                typeDeclaration.getInterfaceTypeDescriptors();

            Visibility visibility = typeDeclaration.getVisibility();
            for (DeclaredTypeDescriptor interfaceTypeDescriptor : interfaceTypeDescriptors) {
              Visibility interfaceVisibility =
                  interfaceTypeDescriptor.getTypeDeclaration().getVisibility();
              if (isWiderThan(visibility, interfaceVisibility)) {
                problems.warning(
                    type.getSourcePosition(),
                    "Type '%s' (%s) should not have wider visibility than its super type '%s'"
                        + " (%s).",
                    type.getReadableDescription(),
                    getDescription(visibility),
                    interfaceTypeDescriptor.getReadableDescription(),
                    getDescription(interfaceVisibility));
              }
            }
          }

          private void checkSynchronizedMethods(Type type) {
            boolean hasSynchronizedMethods =
                type.getMethods().stream().anyMatch(it -> it.getDescriptor().isSynchronized());
            if (!hasSynchronizedMethods) {
              return;
            }

            if (isSubtypeOfJ2ktMonitor(type.getTypeDescriptor())) {
              return;
            }

            problems.error(
                type.getSourcePosition(),
                "Type '%s' does not support synchronized methods as it does not extend '%s' or is"
                    + " not a direct subclass of '%s'.",
                type.getReadableDescription(),
                TypeDescriptors.get().javaemulLangJ2ktMonitor.getReadableDescription(),
                TypeDescriptors.get().javaLangObject.getReadableDescription());
          }

          private void checkSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
            TypeDescriptor expressionTypeDescriptor =
                synchronizedStatement.getExpression().getTypeDescriptor();
            if (isValidSynchronizedStatementExpressionTypeDescriptor(expressionTypeDescriptor)) {
              return;
            }

            // TODO(b/381246369): Remove this check when the bug is fixed.
            if (isInstanceOf(expressionTypeDescriptor, "com.google.common.base.XplatMonitor")) {
              return;
            }

            problems.error(
                synchronizedStatement.getSourcePosition(),
                "Synchronized statement is valid only on instances of '%s' or '%s'.",
                TypeDescriptors.get().javaLangClass.toRawTypeDescriptor().getReadableDescription(),
                TypeDescriptors.get()
                    .javaemulLangJ2ktMonitor
                    .toRawTypeDescriptor()
                    .getReadableDescription());
          }

          private void checkExplicitQualifierInConstructorCall(MethodCall methodCall) {
            if (methodCall.getTarget().isConstructor()
                && methodCall.getQualifier() != null
                && !(methodCall.getQualifier() instanceof ThisReference)) {
              problems.error(
                  getClosestSourcePosition(),
                  "Explicit qualifier in constructor call is not supported.");
            }
          }
        });
  }

  private static Iterable<TypeDescriptor> getReferencedTypeDescriptors(
      MemberDescriptor memberDescriptor) {
    if (memberDescriptor instanceof MethodDescriptor methodDescriptor) {
      return Iterables.concat(
          methodDescriptor.getParameterTypeDescriptors(),
          ImmutableList.of(methodDescriptor.getReturnTypeDescriptor()));
    }

    if (memberDescriptor instanceof FieldDescriptor fieldDescriptor) {
      return ImmutableList.of(fieldDescriptor.getTypeDescriptor());
    }

    return ImmutableList.of();
  }

  private static boolean isWiderThan(Visibility visibility, Visibility otherVisibility) {
    return visibility.compareTo(otherVisibility) < 0;
  }

  private static Visibility getNarrowestOf(Visibility visibility, Visibility otherVisibility) {
    return isWiderThan(visibility, otherVisibility) ? otherVisibility : visibility;
  }

  private static String getDescription(Visibility visibility) {
    return switch (visibility) {
      case PUBLIC -> "public";
      case PROTECTED -> "protected";
      case PACKAGE_PRIVATE -> "default";
      case PRIVATE -> "private";
    };
  }

  /**
   * Returns required visibility of this member, which is the narrowest of the declared visibility
   * and the inferred visibility of its enclosing type.
   */
  private static Visibility getRequiredVisibility(MemberDescriptor memberDescriptor) {
    Visibility memberVisibility = memberDescriptor.getVisibility();
    return getNarrowestOf(
        memberVisibility, getRequiredVisibility(memberDescriptor.getEnclosingTypeDescriptor()));
  }

  /**
   * Returns required visibility of this type, which is the narrowest of the declared visibility and
   * the inferred visibility of its enclosing type (if present).
   */
  private static Visibility getRequiredVisibility(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor descriptor) {
      Visibility typeVisibility = descriptor.getTypeDeclaration().getVisibility();
      DeclaredTypeDescriptor enclosingTypeDescriptor = descriptor.getEnclosingTypeDescriptor();
      return enclosingTypeDescriptor == null
          ? typeVisibility
          : getNarrowestOf(typeVisibility, getRequiredVisibility(enclosingTypeDescriptor));
    }

    return Visibility.PUBLIC;
  }

  private static boolean isFromJ2clReadableOrIntegrationTest(Type type) {
    String sourceFilePath = type.getSourcePosition().getFilePath();
    return sourceFilePath != null
        && (sourceFilePath.contains("javatests/com/google/j2cl/integration")
            || sourceFilePath.contains("javatests/com/google/j2cl/readable"));
  }

  private static boolean isInstanceOf(TypeDescriptor typeDescriptor, String qualifiedSourceName) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor descriptor) {
      return descriptor.getTypeDeclaration().getAllSuperTypesIncludingSelf().stream()
          .map(TypeDeclaration::getQualifiedSourceName)
          .anyMatch(it -> it.equals(qualifiedSourceName));
    }
    return false;
  }

  private static boolean shadowsAnySuperTypeField(FieldDescriptor fieldDescriptor) {
    TypeDeclaration typeDeclaration =
        fieldDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration();
    return !typeDeclaration.isInterface()
        && !fieldDescriptor.isStatic()
        && typeDeclaration.getAllSuperTypesIncludingSelf().stream()
            .filter(not(typeDeclaration::equals))
            .filter(not(TypeDeclaration::isInterface))
            .flatMap(td -> td.getDeclaredFieldDescriptors().stream())
            .filter(fd -> !fd.isStatic())
            .filter(fd -> fd.getName().equals(fieldDescriptor.getName()))
            .anyMatch(fd -> shadowsSuperTypeField(fieldDescriptor, fd));
  }

  private static boolean shadowsSuperTypeField(
      FieldDescriptor fieldDescriptor, FieldDescriptor superFieldDescriptor) {
    return switch (superFieldDescriptor.getVisibility()) {
      case PUBLIC, PROTECTED -> true;
      case PACKAGE_PRIVATE ->
          fieldDescriptor
              .getEnclosingTypeDescriptor()
              .isInSamePackage(superFieldDescriptor.getEnclosingTypeDescriptor());
      case PRIVATE -> false;
    };
  }
}
