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

import com.google.j2cl.ast.common.HasJsNameInfo;
import com.google.j2cl.ast.common.HasReadableDescription;
import com.google.j2cl.ast.common.JsUtils;
import com.google.j2cl.ast.sourcemap.HasSourcePosition;
import com.google.j2cl.problems.Problems;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Checks and throws errors for invalid JsInterop constructs. */
public class JsInteropRestrictionsChecker {
  private final Problems problems;

  public JsInteropRestrictionsChecker(Problems problems) {
    this.problems = problems;
  }

  public static void check(CompilationUnit compilationUnit, Problems problems) {
    new JsInteropRestrictionsChecker(problems).checkCompilationUnit(compilationUnit);
  }

  private void checkCompilationUnit(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      checkType(type);
    }
  }

  private void checkJsNameOnType(Type type) {
    if (!type.getSimpleJsName().equals("*") && !type.getSimpleJsName().equals("?")) {
      checkJsName(type);
      return;
    }

    if (!type.isNative() || !type.isInterface() || !JsUtils.isGlobal(type.getJsNamespace())) {
      problems.error(
          type.getSourcePosition(),
          "Only native interfaces in the global namespace can be named '%s'.",
          type.getSimpleJsName());
    }
  }

  private void checkType(Type type) {
    if (type.getDeclaration().isJsType()) {
      if (!checkJsType(type)) {
        return;
      }
      checkJsNameOnType(type);
      checkJsNamespace(type);
    }

    if (type.getDeclaration().isNative()) {
      if (!checkNativeJsType(type)) {
        return;
      }
    }

    if (type.getDeclaration().isJsFunctionInterface()) {
      checkJsFunctionInterface(type);
    } else if (type.getDeclaration().isJsFunctionImplementation()) {
      checkJsFunctionImplementation(type);
    } else {
      checkJsFunctionSubtype(type);
      checkJsConstructors(type);
    }

    for (Member member : type.getMembers()) {
      checkMember(member);
    }
    checkTypeMemberCollisions(type);
    checkInstanceOfNativeJsTypesOrJsFunctionImplementations(type);
  }

  private void checkTypeMemberCollisions(Type type) {
    // Native JS members are allowed to collide.
    if (type.getDeclaration().isNative()) {
      return;
    }

    Map<String, MemberDescriptor> memberDescriptorsByKey = new HashMap<>();
    List<Member> members = type.getMembers();
    for (Member member : members) {
      if (!(member instanceof Field) && !(member instanceof Method)) {
        continue;
      }
      MemberDescriptor memberDescriptor =
          member instanceof Field
              ? ((Field) member).getDescriptor()
              : ((Method) member).getDescriptor();

      // Constructors are subject to a separate check and should not be duplicatively examined here.
      if (memberDescriptor instanceof MethodDescriptor
          && ((MethodDescriptor) memberDescriptor).isConstructor()) {
        continue;
      }
      // Only look at unobfuscated JsInterop things.
      if (!memberDescriptor.isJsMember()
          || memberDescriptor.isJsFunction()
          || memberDescriptor.isJsOverlay()) {
        continue;
      }
      // Native JS members are allowed to collide.
      if (memberDescriptor.isNative()) {
        continue;
      }

      // If a collision has occurred examine it more closely.
      if (memberDescriptorsByKey.containsKey(getMemberKey(memberDescriptor))) {
        MemberDescriptor collidingMemberDescriptor =
            memberDescriptorsByKey.get(getMemberKey(memberDescriptor));
        Set<JsMemberType> jsMemberTypes = EnumSet.noneOf(JsMemberType.class);
        jsMemberTypes.add(collidingMemberDescriptor.getJsInfo().getJsMemberType());
        jsMemberTypes.add(memberDescriptor.getJsInfo().getJsMemberType());

        // A getter/setter pair can both use the same name.
        if (jsMemberTypes.contains(JsMemberType.GETTER)
            && jsMemberTypes.contains(JsMemberType.SETTER)) {
          continue;
        }

        problems.error(
            member.getSourcePosition(),
            "'%s' and '%s' cannot both use the same JavaScript name '%s'.",
            memberDescriptor.getReadableDescription(),
            collidingMemberDescriptor.getReadableDescription(),
            memberDescriptor.getSimpleJsName());
      }

      memberDescriptorsByKey.put(getMemberKey(memberDescriptor), memberDescriptor);
    }
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

  private void checkMember(Member member) {
    if (!member.isMethod() && !member.isField()) {
      return;
    }

    if (member.getDescriptor().getEnclosingClassTypeDescriptor().isNative()) {
      checkMemberOfNativeJsType(member);
    }
    if (member.getDescriptor().isJsOverlay()) {
      checkJsOverlay(member);
    }
    checkMemberQualifiedJsName((Member & HasJsNameInfo) member);
    if (member.isMethod()) {
      checkMethodParameters((Method) member);
    }
    // TODO: do other checks.
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
        .equals(member.getDescriptor().getEnclosingClassTypeDescriptor().getQualifiedJsName())) {
      // Namespace set by the enclosing type has already been checked.
      return;
    }

    if (!member.isStatic() && !member.isConstructor()) {
      problems.error(
          member.getSourcePosition(),
          "Instance member '%s' cannot declare a namespace.",
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
    if (!memberDescriptor.getEnclosingClassTypeDescriptor().isNative()
        && !memberDescriptor.getEnclosingClassTypeDescriptor().isJsFunctionInterface()) {
      problems.error(
          member.getSourcePosition(),
          "JsOverlay '%s' can only be declared in a native type or @JsFunction interface.",
          readableDescription);
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
          || (!memberDescriptor.getEnclosingClassTypeDescriptor().isFinal()
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
    if (member.isField() & !memberDescriptor.isStatic()) {
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
    if (member.getDescriptor().isJsOverlay() || member.getDescriptor().isSynthetic()) {
      return;
    }
    String readableDescription = member.getDescriptor().getReadableDescription();
    JsMemberType jsMemberType = member.getDescriptor().getJsInfo().getJsMemberType();
    switch (jsMemberType) {
      case CONSTRUCTOR:
        if (!isConstructorEmpty((Method) member)) {
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
              "Native JsType method '%s' should be native or abstract.",
              readableDescription);
        }
        break;
      case PROPERTY:
        Field field = (Field) member;
        if (field.hasInitializer()) {
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
    }
  }

  private void checkJsFunctionInterface(Type type) {
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
  }

  private void checkJsFunctionImplementation(Type type) {
    String readableDescription = type.getDeclaration().getReadableDescription();
    if (!type.getDeclaration().isFinal() && !type.isAnonymous()) {
      problems.error(
          type.getSourcePosition(),
          "JsFunction implementation '%s' must be final.",
          readableDescription);
    }

    if (type.getSuperInterfaceTypeDescriptors().size() != 1) {
      problems.error(
          type.getSourcePosition(),
          "JsFunction implementation '%s' cannot implement more than one interface.",
          readableDescription);
    }

    if (type.getDeclaration().isJsType()) {
      problems.error(
          type.getSourcePosition(),
          "'%s' cannot be both a JsFunction implementation and a JsType at the same time.",
          readableDescription);
    }

    if (!TypeDescriptors.isJavaLangObject(type.getSuperTypeDescriptor())) {
      problems.error(
          type.getSourcePosition(),
          "JsFunction implementation '%s' cannot extend a class.",
          readableDescription);
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

  private <T extends HasJsNameInfo & HasSourcePosition & HasReadableDescription> void checkJsName(
      T item) {
    String jsName = item.getSimpleJsName();
    if (jsName == null) {
      return;
    }
    if (jsName.isEmpty()) {
      problems.error(
          item.getSourcePosition(),
          "'%s' cannot have an empty name.",
          item.getReadableDescription());
    } else if ((item.isNative() && !JsUtils.isValidJsQualifiedName(jsName))
        || (!item.isNative() && !JsUtils.isValidJsIdentifier(jsName))) {
      problems.error(
          item.getSourcePosition(),
          "'%s' has invalid name '%s'.",
          item.getReadableDescription(),
          jsName);
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
    for (int i = 0; i < numberOfParameters; i++) {
      int lastParameter = numberOfParameters - 1;
      boolean isVarargsParameter = (i == lastParameter) && methodDescriptor.isJsMethodVarargs();

      if (method.isParameterOptional(i)) {
        if (methodDescriptor.getParameterTypeDescriptors().get(i).isPrimitive()) {
          problems.error(
              method.getSourcePosition(),
              "JsOptional parameter '%s' in method '%s' cannot be of a primitive type.",
              method.getParameters().get(i).getName(),
              methodDescriptor.getReadableDescription());
        }
        if (isVarargsParameter) {
          problems.error(
              method.getSourcePosition(),
              "JsOptional parameter '%s' in method '%s' cannot be a varargs parameter.",
              method.getParameters().get(i).getName(),
              methodDescriptor.getReadableDescription());
        }
        hasOptionalParameters = true;
        continue;
      }
      if (hasOptionalParameters && !isVarargsParameter) {
        problems.error(
            method.getSourcePosition(),
            "JsOptional parameter '%s' in method '%s' cannot precede parameters that are not "
                + "JsOptional.",
            method.getParameters().get(i - 1).getName(),
            methodDescriptor.getReadableDescription());
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
          methodDescriptor.getReadableDescription());
    }
  }

  private void checkJsConstructors(Type type) {
    List<Method> jsConstructors = getJsConstructors(type);

    if (type.isNative()) {
      return;
    }

    if (jsConstructors.isEmpty()) {
      return;
    }

    if (jsConstructors.size() > 1) {
      problems.error(
          type.getSourcePosition(),
          "More than one JsConstructor exists for '%s'.",
          type.getReadableDescription());
    }

    final Method jsConstructor = jsConstructors.get(0);

    if (!jsConstructor.isPrimaryConstructor()) {
      problems.error(
          jsConstructor.getSourcePosition(),
          "Constructor '%s' can be a JsConstructor only if all constructors in the class are "
              + "delegating to it.",
          jsConstructor.getReadableDescription());
    }
  }

  private List<Method> getJsConstructors(Type type) {
    return type.getConstructors()
        .stream()
        .filter(method -> method.getDescriptor().isJsConstructor())
        .collect(Collectors.toList());
  }

  /**
   * Returns true if the constructor method is locally empty (allows calls to super constructor).
   */
  private static boolean isConstructorEmpty(Method constructor) {
    List<Statement> statements = constructor.getBody().getStatements();
    return statements.isEmpty() || (statements.size() == 1 && AstUtils.hasSuperCall(constructor));
  }

  /**
   * Returns true if the type does not have any static initialization blocks and does not do any
   * initializations on static fields except for compile time constants.
   */
  private static boolean isClinitEmpty(Type type) {
    if (type.hasStaticInitializerBlocks()) {
      return false;
    }
    for (Field staticField : type.getStaticFields()) {
      if (staticField.hasInitializer() && !staticField.isCompileTimeConstant()) {
        return false;
      }
    }
    return true;
  }

  private String getMemberKey(MemberDescriptor memberDescriptor) {
    return (memberDescriptor.isStatic() ? "static " : "") + memberDescriptor.getSimpleJsName();
  }
}
