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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Visibility;

/** Checks and throws errors for constructs which can not be transpiled to Kotlin. */
public final class J2ktRestrictionsChecker {
  private J2ktRestrictionsChecker() {}

  public static void check(Library library, Problems problems) {
    library.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterMethod(Method method) {
            checkNotGenericConstructor(method);
            checkReferencedTypeVisibilities(method);
            return true;
          }

          @Override
          public boolean enterField(Field field) {
            checkReferencedTypeVisibilities(field);
            return true;
          }

          @Override
          public boolean enterType(Type type) {
            checkSuperTypeVisibilities(type);
            checkInterfaceTypeVisibilities(type);
            return true;
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
        });
  }

  private static Iterable<TypeDescriptor> getReferencedTypeDescriptors(
      MemberDescriptor memberDescriptor) {
    if (memberDescriptor instanceof MethodDescriptor) {
      MethodDescriptor methodDescriptor = (MethodDescriptor) memberDescriptor;
      return Iterables.concat(
          methodDescriptor.getParameterTypeDescriptors(),
          ImmutableList.of(methodDescriptor.getReturnTypeDescriptor()));
    }

    if (memberDescriptor instanceof FieldDescriptor) {
      FieldDescriptor fieldDescriptor = (FieldDescriptor) memberDescriptor;
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
    switch (visibility) {
      case PUBLIC:
        return "public";
      case PROTECTED:
        return "protected";
      case PACKAGE_PRIVATE:
        return "default";
      case PRIVATE:
        return "private";
    }
    throw new AssertionError();
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
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
      Visibility typeVisibility = declaredTypeDescriptor.getTypeDeclaration().getVisibility();
      DeclaredTypeDescriptor enclosingTypeDescriptor =
          declaredTypeDescriptor.getEnclosingTypeDescriptor();
      return enclosingTypeDescriptor == null
          ? typeVisibility
          : getNarrowestOf(typeVisibility, getRequiredVisibility(enclosingTypeDescriptor));
    }

    return Visibility.PUBLIC;
  }
}
