/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.transpiler.ast;

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveVoid;

import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.TypeDeclaration.SourceLanguage;
import javax.annotation.Nullable;

/** Utility functions for JsInterop properties. */
public final class JsInteropAstUtils {

  public static boolean isJsType(TypeDeclaration typeDeclaration) {
    return typeDeclaration.hasAnnotation("jsinterop.annotations.JsType");
  }

  // TODO(b/340930928): This is a temporary hack since JsFunction is not supported in Wasm.
  private static final ThreadLocal<Boolean> ignoreJsFunctionAnnotations =
      ThreadLocal.withInitial(() -> false);

  public static void setIgnoreJsFunctionAnnotations() {
    ignoreJsFunctionAnnotations.set(true);
  }

  public static boolean isJsFunction(TypeDeclaration typeDeclaration) {
    if (ignoreJsFunctionAnnotations.get()) {
      return false;
    }
    return typeDeclaration.hasAnnotation("jsinterop.annotations.JsFunction");
  }

  public static boolean isJsNative(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.isEnum() && TypeDeclaration.implementWasmJsInteropSemantics()) {
      return false;
    }
    Annotation annotation = getJsTypeOrJsEnumAnnotation(typeDeclaration);
    return annotation != null && annotation.getBooleanValue("isNative", false);
  }

  @Nullable
  public static JsEnumInfo getJsEnumInfo(TypeDeclaration typeDeclaration) {
    Annotation annotation = typeDeclaration.getAnnotation("jsinterop.annotations.JsEnum");
    if (annotation == null) {
      return null;
    }
    if (TypeDeclaration.implementWasmJsInteropSemantics()
        && annotation.getBooleanValue("isNative", false)) {
      return null;
    }
    boolean hasCustomValue = annotation.getBooleanValue("hasCustomValue", false);
    boolean isNative = typeDeclaration.isNative();
    return JsEnumInfo.builder()
        .setHasCustomValue(hasCustomValue)
        .setSupportsComparable(!hasCustomValue || isNative)
        .setSupportsOrdinal(!hasCustomValue && !isNative)
        .build();
  }

  public static String getJsName(TypeDeclaration typeDeclaration) {
    Annotation annotation = getJsTypeOrJsEnumAnnotation(typeDeclaration);
    String jsName = getJsName(annotation);
    return jsName != null ? jsName : typeDeclaration.getSimpleSourceName();
  }

  @Nullable
  public static String getJsNamespace(TypeDeclaration typeDeclaration) {
    return getJsNamespace(getJsTypeOrJsEnumAnnotation(typeDeclaration));
  }

  @Nullable
  private static Annotation getJsTypeOrJsEnumAnnotation(TypeDeclaration typeDeclaration) {
    Annotation jsType = typeDeclaration.getAnnotation("jsinterop.annotations.JsType");
    return jsType != null ? jsType : typeDeclaration.getAnnotation("jsinterop.annotations.JsEnum");
  }

  // TODO(b/317164851): Remove hack that makes jsinfo ignored for non-native types in Wasm.
  private static final ThreadLocal<Boolean> ignoreNonNativeJsInfo =
      ThreadLocal.withInitial(() -> false);

  public static void setIgnoreNonNativeJsInfo() {
    ignoreNonNativeJsInfo.set(true);
  }

  /** Return the original JsInfo for the given member descriptor. */
  public static JsInfo getOriginalJsInfo(MemberDescriptor member, @Nullable JsInfo originalJsInfo) {
    if (ignoreNonNativeJsInfo.get()
        && !member.getEnclosingTypeDescriptor().isNative()
        && !member.getEnclosingTypeDescriptor().isJsFunctionInterface()
        && !(member instanceof MethodDescriptor method && method.isNative())) {
      return JsInfo.NONE;
    }
    return originalJsInfo != null ? originalJsInfo : computeOriginalJsInfo(member);
  }

  private static JsInfo computeOriginalJsInfo(MemberDescriptor member) {
    Annotation memberAnnotation = getJsMemberAnnotation(member);
    return JsInfo.builder()
        .setJsMemberType(getJsMemberType(member, memberAnnotation))
        .setJsName(getJsName(memberAnnotation))
        .setJsNamespace(getJsNamespace(memberAnnotation))
        .setJsOverlay(isJsOverlay(member))
        .setJsAsync(member.hasAnnotation("jsinterop.annotations.JsAsync"))
        .setHasJsMemberAnnotation(memberAnnotation != null)
        .build();
  }

  @Nullable
  private static Annotation getJsMemberAnnotation(MemberDescriptor member) {
    return switch (member) {
      case FieldDescriptor field -> field.getAnnotation("jsinterop.annotations.JsProperty");
      case MethodDescriptor method when method.isConstructor() ->
          method.getAnnotation("jsinterop.annotations.JsConstructor");
      case MethodDescriptor method -> {
        Annotation annotation = method.getAnnotation("jsinterop.annotations.JsMethod");
        yield annotation != null
            ? annotation
            : method.getAnnotation("jsinterop.annotations.JsProperty");
      }
      default -> null;
    };
  }

  private static boolean isImplicitJsMember(MemberDescriptor member) {
    if (isJsOverlay(member)) {
      return false;
    }
    TypeDeclaration enclosingType = member.getEnclosingTypeDescriptor().getTypeDeclaration();
    if (enclosingType.isJsEnum()) {
      return member.isEnumConstant();
    }
    if (enclosingType.isJsType() && canBeImplicitJsTypeMember(member)) {
      return true;
    }
    return false;
  }

  private static boolean canBeImplicitJsTypeMember(MemberDescriptor member) {
    if (member.isSynthetic()) {
      // Synthetic artifacts are ignored for jsinterop purposes.
      return false;
    }
    if (member.getEnclosingTypeDescriptor().isNative()) {
      // All native members are implicit JsMembers, regardless of visibility.
      return true;
    }
    if (member.getVisibility().isPublic()) {
      // Java component accessors will inherit JsInfo from the components so should not be
      // considered implicit JsMembers even though they are public.
      if (isJavaRecordComponentAccessor(member)) {
        return false;
      }
      return true;
    }
    // Java record components fields, although private, are implicit js members. Later in the
    // process, it will be used by public accessors to inherit JsInfo from.
    if (isJavaRecordComponentField(member)) {
      return true;
    }
    return false;
  }

  private static boolean isJavaRecordComponentAccessor(MemberDescriptor member) {
    return isFromJava(member)
        && member instanceof MethodDescriptor method
        && method.isRecordComponentAccessor();
  }

  private static boolean isJavaRecordComponentField(MemberDescriptor member) {
    return isFromJava(member)
        && member instanceof FieldDescriptor field
        && field.isRecordComponentField();
  }

  private static boolean isFromJava(MemberDescriptor member) {
    return member.getEnclosingTypeDescriptor().getTypeDeclaration().getSourceLanguage()
        == SourceLanguage.JAVA;
  }

  private static JsMemberType getJsMemberType(
      MemberDescriptor member, Annotation jsMemberAnnotation) {

    if (member.hasAnnotation("jsinterop.annotations.JsIgnore")
        || (jsMemberAnnotation == null && !isImplicitJsMember(member))) {
      return JsMemberType.NONE;
    }

    return switch (member) {
      case FieldDescriptor f -> JsMemberType.PROPERTY;
      case MethodDescriptor m when m.isConstructor() -> JsMemberType.CONSTRUCTOR;
      case MethodDescriptor m when isPropertyAccessor(m) -> getJsPropertyAccessorType(m);
      case MethodDescriptor m -> JsMemberType.METHOD;
      default -> JsMemberType.NONE;
    };
  }

  private static boolean isPropertyAccessor(MethodDescriptor method) {
    return method.hasAnnotation("jsinterop.annotations.JsProperty")
        || (method.getOrigin() == MethodOrigin.KOTLIN_PROPERTY_ACCESSOR
            && !method.hasAnnotation("jsinterop.annotations.JsMethod"));
  }

  private static JsMemberType getJsPropertyAccessorType(MethodDescriptor method) {
    if (method.getParameterDescriptors().size() == 1
        && isPrimitiveVoid(method.getReturnTypeDescriptor())) {
      return JsMemberType.SETTER;
    } else if (method.getParameterDescriptors().isEmpty()
        && (!isPrimitiveVoid(method.getReturnTypeDescriptor()) || isDebugger(method))) {
      return JsMemberType.GETTER;
    }
    return JsMemberType.UNDEFINED_ACCESSOR;
  }

  private static boolean isJsOverlay(MemberDescriptor member) {
    return member.hasAnnotation("jsinterop.annotations.JsOverlay") || isImplicitJsOverlay(member);
  }

  private static boolean isImplicitJsOverlay(MemberDescriptor member) {
    return member.isSynthetic()
        && (member.getEnclosingTypeDescriptor().isNative()
            || member.getEnclosingTypeDescriptor().isJsFunctionInterface());
  }

  private static boolean isDebugger(MethodDescriptor method) {
    return method.getName().equals("debugger") && method.isNative() && method.isStatic();
  }

  @Nullable
  private static String getJsNamespace(@Nullable Annotation annotation) {
    return annotation != null ? annotation.getStringValue("namespace") : null;
  }

  @Nullable
  private static String getJsName(@Nullable Annotation annotation) {
    return annotation != null ? annotation.getStringValue("name") : null;
  }

  private JsInteropAstUtils() {}
}
