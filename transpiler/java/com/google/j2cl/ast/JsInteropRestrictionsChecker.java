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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Checks and throws errors for invalid JsInterop constructs. */
public class JsInteropRestrictionsChecker {

  public static void check(List<CompilationUnit> compilationUnits, Problems problems) {
    new JsInteropRestrictionsChecker(problems).checkCompilationUnits(compilationUnits);
  }

  private final Problems problems;
  private boolean wasUnusableByJsWarningReported = false;

  private JsInteropRestrictionsChecker(Problems problems) {
    this.problems = problems;
  }

  private void checkCompilationUnits(List<CompilationUnit> compilationUnits) {
    for (CompilationUnit compilationUnit : compilationUnits) {
      checkCompilationUnit(compilationUnit);
    }
    if (wasUnusableByJsWarningReported) {
      problems.info(
          "Suppress \"[unusable-by-js]\" warnings by adding a "
              + "`@SuppressWarnings(\"unusable-by-js\")` annotation to the corresponding member.");
    }
  }

  private void checkCompilationUnit(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      checkType(type);
    }
  }

  private void checkJsName(Type type) {
    if (type.getDeclaration().isStarOrUnknown()) {
      if (!type.isNative() || !type.isInterface() || !JsUtils.isGlobal(type.getJsNamespace())) {
        problems.error(
            type.getSourcePosition(),
            "Only native interfaces in the global namespace can be named '%s'.",
            type.getSimpleJsName());
      }
      return;
    }

    checkJsName(type.getSourcePosition(), type.getReadableDescription(), type);
  }

  private void checkType(Type type) {
    if (type.getDeclaration().isJsType()) {
      if (!checkJsType(type)) {
        return;
      }
      checkJsName(type);
      checkJsNamespace(type);
    }

    if (type.getDeclaration().isNative()) {
      if (!checkNativeJsType(type)) {
        return;
      }
    }

    if (type.getDeclaration().isJsFunctionInterface()) {
      checkJsFunction(type);
    } else if (type.getDeclaration().isJsFunctionImplementation()) {
      checkJsFunctionImplementation(type);
    } else {
      checkJsFunctionSubtype(type);
      if (checkJsConstructors(type)) {
        checkJsConstructorSubtype(type);
      }
    }

    Multimap<String, MemberDescriptor> instanceJsMembersByName =
        collectInstanceNames(type.getTypeDescriptor());
    Multimap<String, MemberDescriptor> staticJsMembersByName =
        collectStaticNames(type.getTypeDescriptor());
    for (Member member : type.getMembers()) {
      checkMember(member, instanceJsMembersByName, staticJsMembersByName);
    }
    checkInstanceOfNativeJsTypesOrJsFunctionImplementations(type);
  }

  private void checkInstanceOfNativeJsTypesOrJsFunctionImplementations(Type type) {
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
            TypeDescriptor type = instanceOfExpression.getTestTypeDescriptor();
            if (type.isNative() && type.isInterface()) {
              problems.error(
                  instanceOfExpression.getSourcePosition(),
                  "Cannot do instanceof against native JsType interface '%s'.",
                  type.getReadableDescription());
            } else if (type.isJsFunctionImplementation()) {
              problems.error(
                  instanceOfExpression.getSourcePosition(),
                  "Cannot do instanceof against JsFunction implementation '%s'.",
                  type.getReadableDescription());
            }
          }
        });
  }

  private boolean checkJsType(Type type) {
    if (type.getDeclaration().isLocal()) {
      problems.error(
          type.getSourcePosition(),
          "Local class '%s' cannot be a JsType.",
          type.getDeclaration().getReadableDescription());
      return false;
    }
    return true;
  }

  private void checkIllegalOverrides(Method method) {
    Optional<MethodDescriptor> jsOverlayOverride =
        method
            .getDescriptor()
            .getOverriddenMethodDescriptors()
            .stream()
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

  private void checkMember(
      Member member,
      Multimap<String, MemberDescriptor> instanceJsMembersByName,
      Multimap<String, MemberDescriptor> staticJsMembersByName) {
    MemberDescriptor memberDescriptor = member.getDescriptor();
    if ((!member.isMethod() && !member.isField()) || memberDescriptor.isSynthetic()) {
      return;
    }

    if (memberDescriptor.getEnclosingTypeDescriptor().isNative()) {
      checkMemberOfNativeJsType(member);
    }

    if (memberDescriptor.getEnclosingTypeDescriptor().isOrExtendsNativeClass()) {
      checkMemberOfNativeClassOrSubclass(member);
    }

    if (memberDescriptor.isJsOverlay()) {
      checkJsOverlay(member);
    }

    if (member.isMethod()) {
      Method method = (Method) member;
      checkIllegalOverrides(method);
      checkMethodParameters(method);
      if (memberDescriptor.isJsAsync()) {
        checkJsAsyncMethod(method);
      }
      if (!checkJsPropertyAccessor(method)) {
        return;
      }
    }

    if (memberDescriptor.canBeReferencedExternally() || memberDescriptor.isNative()) {
      checkUnusableByJs(member);
    }

    checkMemberQualifiedJsName((Member & HasJsNameInfo) member);

    if (isInstanceJsMember(memberDescriptor)) {
      checkNameCollisions(instanceJsMembersByName, member);
    }

    if (isStaticJsMember(memberDescriptor)) {
      checkNameCollisions(staticJsMembersByName, member);
    }
  }

  private void checkMemberOfNativeClassOrSubclass(Member member) {
    if (member.isStatic() || member.isConstructor()) {
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
                  "Cannot use 'super' to call '%s'. Native classes and their subclasses "
                      + "cannot use 'super' to call 'java.lang.Object' methods.",
                  target.getReadableDescription());
            }
          }
        });
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

  private void checkOverrideConsistency(Member member) {
    if (!member.isMethod() || !member.getDescriptor().isJsMember()) {
      return;
    }
    Method method = (Method) member;
    String jsName = method.getSimpleJsName();
    for (MethodDescriptor overriddenMethodDescriptor :
        method.getDescriptor().getOverriddenMethodDescriptors()) {
      if (!overriddenMethodDescriptor.isJsMember()) {
        continue;
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

      if (overriddenMethodDescriptor.isJsMethod() != method.getDescriptor().isJsMethod()) {
        // Overrides can not change JsMethod to JsProperty nor vice versa.
        problems.error(
            method.getSourcePosition(),
            "%s '%s' cannot override %s '%s'.",
            member.getDescriptor().isJsMethod() ? "JsMethod" : "JsProperty",
            member.getReadableDescription(),
            overriddenMethodDescriptor.isJsMethod() ? "JsMethod" : "JsProperty",
            overriddenMethodDescriptor.getReadableDescription(),
            parentName);
        break;
      }
    }
  }

  private <T extends Member & HasJsNameInfo> void checkMemberQualifiedJsName(T member) {
    if (member.isConstructor()) {
      // Constructors always inherit their name and namespace from the enclosing type.
      // The corresponding checks are done for the type separately.
      return;
    }

    checkJsName(member);

    if (member.getJsNamespace() == null) {
      return;
    }

    if (member
        .getJsNamespace()
        .equals(member.getDescriptor().getEnclosingTypeDescriptor().getQualifiedJsName())) {
      // Namespace set by the enclosing type has already been checked.
      return;
    }

    if (!member.isStatic()) {
      problems.error(
          member.getSourcePosition(),
          "Instance member '%s' cannot declare a namespace.",
          member.getReadableDescription());
      return;
    }

    if (!member.isNative()) {
      problems.error(
          member.getSourcePosition(),
          "Non-native member '%s' cannot declare a namespace.",
          member.getReadableDescription());
      return;
    }

    checkJsNamespace(member);
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
          "JsOverlay method '%s' cannot be nor override a JsProperty or a JsMethod.",
          readableDescription);
      return;
    }
    if (member.isMethod()) {
      Method method = (Method) member;
      if (method.isOverride()) {
        problems.error(
            method.getSourcePosition(),
            "JsOverlay method '%s' cannot override a supertype method.",
            readableDescription);
        return;
      }
      if (member.isNative()
          || (!memberDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration().isFinal()
              && !memberDescriptor.isFinal()
              && !memberDescriptor.isStatic()
              && !memberDescriptor.getVisibility().isPrivate()
              && !memberDescriptor.isDefaultMethod())) {
        problems.error(
            member.getSourcePosition(),
            "JsOverlay method '%s' cannot be non-final nor native.",
            readableDescription);
      }
    }
    if (member.isField() && !memberDescriptor.isStatic()) {
      problems.error(
          member.getSourcePosition(),
          "JsOverlay field '%s' can only be static.",
          readableDescription);
    }
  }

  private boolean checkNativeJsType(Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    String readableDescription = typeDeclaration.getReadableDescription();

    if (type.isEnumOrSubclass()) {
      problems.error(
          type.getSourcePosition(), "Enum '%s' cannot be a native JsType.", readableDescription);
      return false;
    }
    if (typeDeclaration.isCapturingEnclosingInstance()) {
      problems.error(
          type.getSourcePosition(),
          "Non static inner class '%s' cannot be a native JsType.",
          readableDescription);
      return false;
    }

    TypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor != null
        && !TypeDescriptors.isJavaLangObject(superTypeDescriptor)
        && !superTypeDescriptor.isNative()) {
      problems.error(
          type.getSourcePosition(),
          "Native JsType '%s' can only extend native JsType classes.",
          readableDescription);
    }
    for (TypeDescriptor interfaceType : type.getSuperInterfaceTypeDescriptors()) {
      if (!interfaceType.isNative()) {
        problems.error(
            type.getSourcePosition(),
            "Native JsType '%s' can only %s native JsType interfaces.",
            readableDescription,
            type.isInterface() ? "extend" : "implement");
      }
    }

    if (type.hasInstanceInitializerBlocks()) {
      problems.error(
          type.getSourcePosition(),
          "Native JsType '%s' cannot have initializer.",
          readableDescription);
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
        }
        break;
      case METHOD:
      case GETTER:
      case SETTER:
        if (!member.isAbstract() && !member.isNative()) {
          problems.error(
              member.getSourcePosition(),
              "Native JsType method '%s' should be native, abstract or overlay.",
              readableDescription);
        }
        break;
      case PROPERTY:
        Field field = (Field) member;
        if (field.getDescriptor().isFinal()) {
          problems.error(
              field.getSourcePosition(),
              "Native JsType field '%s' cannot be final.",
              member.getReadableDescription());
        } else if (field.hasInitializer()) {
          problems.error(
              field.getSourcePosition(),
              "Native JsType field '%s' cannot have initializer.",
              readableDescription);
        }
        break;
      case NONE:
        problems.error(
            member.getSourcePosition(),
            "Native JsType member '%s' cannot have @JsIgnore.",
            readableDescription);
        break;
      case UNDEFINED_ACCESSOR:
        // Nothing to check here. An error will be emitted for UNDEFINED_ACCESSOR elsewhere.
        break;
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
      checkMemberOfJsFunction(member);
    }
  }

  private void checkMemberOfJsFunction(Member member) {
    MemberDescriptor memberDescriptor = member.getDescriptor();
    if (memberDescriptor.isSynthetic()) {
      return;
    }

    if (memberDescriptor.isJsMember()) {
      problems.error(
          member.getSourcePosition(),
          "JsFunction interface member '%s' cannot be JsMethod nor JsProperty.",
          member.getReadableDescription());
      return;
    }

    if (memberDescriptor.isJsFunction() || memberDescriptor.isJsOverlay()) {
      return;
    }

    problems.error(
        member.getSourcePosition(),
        "JsFunction interface '%s' cannot declare non-JsOverlay member '%s'.",
        memberDescriptor.getEnclosingTypeDescriptor().getReadableDescription(),
        memberDescriptor.getReadableDescription());
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
      checkMemberOfJsFunctionImplementation(member);
    }
  }

  private void checkMemberOfJsFunctionImplementation(Member member) {
    MemberDescriptor memberDescriptor = member.getDescriptor();
    if (memberDescriptor.isSynthetic()) {
      return;
    }

    if (member.isMethod()) {
      Method method = (Method) member;
      Optional<MethodDescriptor> nonJsFunctionOverride =
          method
              .getDescriptor()
              .getOverriddenMethodDescriptors()
              .stream()
              .filter(Predicates.not(MethodDescriptor::isJsFunction))
              .findFirst();

      if (nonJsFunctionOverride.isPresent()) {
        // Methods that are not effectively static dispatch are disallowed. In this case these
        // could only be overrideable methods of java.lang.Object, i.e. toString, hashCode
        // and equals.

        problems.error(
            member.getSourcePosition(),
            "JsFunction implementation '%s' cannot override method '%s'.",
            memberDescriptor.getEnclosingTypeDescriptor().getReadableDescription(),
            nonJsFunctionOverride.get().getReadableDescription());
        return;
      }
    }

    if (memberDescriptor.isJsMember()) {
      problems.error(
          member.getSourcePosition(),
          "JsFunction implementation member '%s' cannot be JsMethod nor JsProperty.",
          member.getReadableDescription());
    }
  }

  private void checkJsFunctionSubtype(Type type) {
    for (TypeDescriptor superInterface : type.getSuperInterfaceTypeDescriptors()) {
      if (superInterface.isJsFunctionInterface()) {
        problems.error(
            type.getSourcePosition(),
            "'%s' cannot extend JsFunction '%s'.",
            type.getDeclaration().getReadableDescription(),
            superInterface.getReadableDescription());
      }
    }
  }

  private void checkJsName(Member member) {
    checkJsName(
        member.getSourcePosition(), member.getReadableDescription(), (HasJsNameInfo) member);
  }

  private void checkJsName(
      SourcePosition sourcePosition, String readableDescription, HasJsNameInfo item) {
    String jsName = item.getSimpleJsName();
    if (jsName == null) {
      return;
    }
    if (jsName.isEmpty()) {
      problems.error(sourcePosition, "'%s' cannot have an empty name.", readableDescription);
    } else if ((item.isNative() && !JsUtils.isValidJsQualifiedName(jsName))
        || (!item.isNative() && !JsUtils.isValidJsIdentifier(jsName))) {
      problems.error(sourcePosition, "'%s' has invalid name '%s'.", readableDescription, jsName);
    }
  }

  private <T extends HasJsNameInfo & HasSourcePosition & HasReadableDescription>
      void checkJsNamespace(T item) {
    String jsNamespace = item.getJsNamespace();
    if (jsNamespace == null || JsUtils.isGlobal(jsNamespace)) {
      return;
    }
    if (jsNamespace.isEmpty()) {
      problems.error(
          item.getSourcePosition(),
          "'%s' cannot have an empty namespace.",
          item.getReadableDescription(),
          jsNamespace);
    } else if (!JsUtils.isValidJsQualifiedName(jsNamespace)) {
      problems.error(
          item.getSourcePosition(),
          "'%s' has invalid namespace '%s'.",
          item.getReadableDescription(),
          jsNamespace);
    }
  }

  private void checkMethodParameters(Method method) {
    // TODO(rluble): When overriding is included in the AST representation, add the relevant checks,
    // i.e. that a parameter can not change from optional into non optional in an override.
    boolean hasOptionalParameters = false;
    MethodDescriptor methodDescriptor = method.getDescriptor();

    int numberOfParameters = method.getParameters().size();
    Variable varargsParameter = method.getJsVarargsParameter();
    for (int i = 0; i < numberOfParameters; i++) {
      Variable parameter = method.getParameters().get(i);
      ParameterDescriptor parameterDescriptor = methodDescriptor.getParameterDescriptors().get(i);
      if (parameterDescriptor.isJsOptional()) {
        if (parameterDescriptor.getTypeDescriptor().isPrimitive()) {
          problems.error(
              method.getSourcePosition(),
              "JsOptional parameter '%s' in method '%s' cannot be of a primitive type.",
              parameter.getName(),
              method.getReadableDescription());
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
        && !methodDescriptor.isJsMethod()
        && !methodDescriptor.isJsConstructor()
        && !methodDescriptor.isJsFunction()) {
      problems.error(
          method.getSourcePosition(),
          "JsOptional parameter in '%s' can only be declared in a JsMethod, a JsConstructor or a "
              + "JsFunction.",
          method.getReadableDescription());
    }

    // Check that parameters that are declared JsOptional in overridden methods remain JsOptional.
    for (MethodDescriptor overriddenMethodDescriptor :
        methodDescriptor.getOverriddenMethodDescriptors()) {
      for (int i = 0; i < overriddenMethodDescriptor.getParameterDescriptors().size(); i++) {
        if (!overriddenMethodDescriptor.isParameterOptional(i)) {
          continue;
        }
        if (!methodDescriptor.isParameterOptional(i)) {
          problems.error(
              method.getSourcePosition(),
              "Method '%s' should declare parameter '%s' as JsOptional",
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

    return true;
  }

  private static MethodDescriptor getPrimaryConstructorDescriptor(final Type type) {
    if (type.getConstructors().isEmpty()) {
      return type.getDeclaration()
          .getDeclaredMethodDescriptors()
          .stream()
          .filter(MethodDescriptor::isConstructor)
          .collect(MoreCollectors.onlyElement());
    }

    ImmutableList<Method> superDelegatingConstructors =
        type.getConstructors()
            .stream()
            .filter(Predicates.not(AstUtils::hasThisCall))
            .collect(ImmutableList.toImmutableList());
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
        type.getSuperTypeDescriptor().getJsConstructorMethodDescriptors();

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

  private static Method getJsConstructor(Type type) {
    return type.getConstructors()
        .stream()
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

  private boolean checkJsPropertyConsistency(
      SourcePosition sourcePosition, MethodDescriptor thisMember, MethodDescriptor thatMember) {
    MethodDescriptor setter = thisMember.isJsPropertySetter() ? thisMember : thatMember;
    MethodDescriptor getter = thisMember.isJsPropertyGetter() ? thisMember : thatMember;

    List<TypeDescriptor> setterParams = setter.getParameterTypeDescriptors();
    if (!getter.getReturnTypeDescriptor().hasSameRawType(setterParams.get(0))) {
      problems.error(
          sourcePosition,
          "JsProperty setter '%s' and getter '%s' cannot have inconsistent types.",
          setter.getReadableDescription(),
          getter.getReadableDescription());
      return false;
    }
    return true;
  }

  private void checkNameCollisions(
      Multimap<String, MemberDescriptor> jsMembersByName, Member member) {
    checkOverrideConsistency(member);
    if (member.isNative()) {
      return;
    }

    String name = member.getDescriptor().getSimpleJsName();

    Set<MemberDescriptor> potentiallyCollidingMembers =
        new LinkedHashSet<>(jsMembersByName.get(name));

    // Remove self.
    boolean removed = potentiallyCollidingMembers.removeIf(member.getDescriptor()::isSameMember);
    checkState(removed);

    // Remove native members.
    potentiallyCollidingMembers.removeIf(MemberDescriptor::isNative);

    if (potentiallyCollidingMembers.isEmpty()) {
      // No conflicting members, proceed.
      return;
    }

    MemberDescriptor potentiallyCollidingMember = potentiallyCollidingMembers.iterator().next();
    if (potentiallyCollidingMembers.size() == 1
        && isJsPropertyAccessorPair(member.getDescriptor(), potentiallyCollidingMember)) {
      if (!checkJsPropertyConsistency(
          member.getSourcePosition(),
          (MethodDescriptor) member.getDescriptor(),
          (MethodDescriptor) potentiallyCollidingMember)) {
        // remove colliding method, to avoid duplicate error messages.
        jsMembersByName.get(name).removeIf(member.getDescriptor()::isSameMember);
      }
      return;
    }

    problems.error(
        member.getSourcePosition(),
        "'%s' and '%s' cannot both use the same JavaScript name '%s'.",
        member.getDescriptor().getReadableDescription(),
        potentiallyCollidingMember.getReadableDescription(),
        name);

    // remove colliding method, to avoid duplicate error messages.
    jsMembersByName.get(name).removeIf(member.getDescriptor()::isSameMember);
  }

  private static boolean isJsPropertyAccessorPair(
      MemberDescriptor thisMember, MemberDescriptor thatMember) {
    return (thisMember.isJsPropertyGetter() && thatMember.isJsPropertySetter())
        || (thatMember.isJsPropertyGetter() && thisMember.isJsPropertySetter());
  }

  private static boolean isInstanceJsMember(MemberDescriptor memberDescriptor) {
    return !(memberDescriptor.isStatic() || memberDescriptor.isConstructor())
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
    if (typeDescriptor == null) {
      return LinkedHashMultimap.create();
    }

    Multimap<String, MemberDescriptor> instanceMembersByName =
        collectInstanceNames(typeDescriptor.getSuperTypeDescriptor());
    for (MemberDescriptor member : typeDescriptor.getDeclaredMemberDescriptors()) {
      if (isInstanceJsMember(member)) {
        addMember(instanceMembersByName, member);
      }
    }
    return instanceMembersByName;
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
    Iterables.removeIf(memberByMemberName.get(name), m -> overrides(member, m));
    memberByMemberName.put(name, member);
  }

  private static boolean overrides(
      MemberDescriptor member, MemberDescriptor potentiallyOverriddenMember) {
    if (!member.isMethod() || !potentiallyOverriddenMember.isMethod()) {
      return false;
    }

    MethodDescriptor method = (MethodDescriptor) member.getDeclarationDescriptor();
    MethodDescriptor potentiallyOverriddenMethod = (MethodDescriptor) potentiallyOverriddenMember;
    return method.isOverride(potentiallyOverriddenMethod)
        || method.isOverride(potentiallyOverriddenMethod.getDeclarationDescriptor());
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
          J2clUtils.format("Type '%s' of field", fieldTypeDescriptor.getReadableDescription()),
          member);
    }

    if (member.isMethod()) {
      Method method = (Method) member;
      MethodDescriptor methodDescriptor = method.getDescriptor();
      warnIfUnusableByJs(methodDescriptor, "Method", member);

      TypeDescriptor returnTypeDescriptor = methodDescriptor.getReturnTypeDescriptor();
      warnIfUnusableByJs(returnTypeDescriptor, "Return type of", member);

      Variable varargsParameter = method.getJsVarargsParameter();
      for (Variable parameter : method.getParameters()) {
        if (!parameter.isUnusableByJsSuppressed()) {
          TypeDescriptor parameterTypeDescriptor =
              parameter == varargsParameter
                  ? ((ArrayTypeDescriptor) parameter.getTypeDescriptor())
                      .getComponentTypeDescriptor()
                  : parameter.getTypeDescriptor();
          String prefix = J2clUtils.format("Type of parameter '%s' in", parameter.getName());
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

  private void warnIfUnusableByJs(MemberDescriptor memberDescriptor, String prefix, Member member) {
    if (memberDescriptor.canBeReferencedExternally()) {
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
}
