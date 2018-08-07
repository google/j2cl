/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.libraryinfo;

import static com.google.j2cl.ast.MethodDescriptor.CLINIT_METHOD_NAME;

import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.MemberDescriptor;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Traverse types and gather execution flow information for building call graph. */
public final class LibraryInfoBuilder {
  /** Serialize a LibraryInfo object into a JSON string. */
  public static String serialize(LibraryInfo.Builder libraryInfo) {
    try {
      return JsonFormat.printer().print(libraryInfo);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException("Unable to write library info", e);
    }
  }

  /** Gather information from a Type and create a TypeInfo object used to build the call graph. */
  public static TypeInfo build(Type type, String headerFilePath, String implFilePath) {
    TypeInfo.Builder typeInfoBuilder =
        TypeInfo.newBuilder()
            .setTypeId(getTypeId(type))
            .setHeaderSourceFilePath(headerFilePath)
            .setImplSourceFilePath(implFilePath);

    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor != null
        && !superTypeDescriptor.getTypeDeclaration().isStarOrUnknown()) {
      typeInfoBuilder.setExtendsType(getTypeId(superTypeDescriptor));
    }

    for (DeclaredTypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
      typeInfoBuilder.addImplementsType(getTypeId(superInterfaceType));
    }

    // JsType interface can be implemented and so instantiated on Javascript side.
    boolean isJsTypeInterface = type.isInterface() && type.getDeclaration().isJsType();
    boolean isJsInstantiable =
        isJsTypeInterface
            || type.getMethods().stream().anyMatch(m -> m.getDescriptor().isJsConstructor());

    typeInfoBuilder.setJsInstantiable(isJsInstantiable);

    // Collect references to getter and setter for the same field under the name of the field,
    // creating only one MemberInfo instance that combines all the references appearing in their
    // bodies.
    Map<String, MemberInfo.Builder> memberInfoBuilderByName =
        new LinkedHashMap<>(type.getMembers().size());

    for (Member member : type.getMembers()) {
      String memberName = getMemberId(member.getDescriptor());
      // $clinit is marked as a JsMethod so that the name is preserved (for example to have a
      // consistent name for calling from handwritten methods in native.js files within the same
      // class). Because $clinit is not really considered to be accessible from arbitrary
      // JavaScript, we don't consider it jsAccessible.
      boolean isJsAccessible =
          member.getDescriptor().isJsMember() && !CLINIT_METHOD_NAME.equals(memberName);

      MemberInfo.Builder builder =
          memberInfoBuilderByName.computeIfAbsent(
              memberName,
              m ->
                  MemberInfo.newBuilder()
                      .setName(memberName)
                      .setPublic(member.getDescriptor().getVisibility().isPublic())
                      .setStatic(member.isStatic())
                      .setJsAccessible(isJsAccessible));

      collectReferencedTypesAndMethodInvocations(member, builder);
    }

    return typeInfoBuilder
        .addAllMember(
            memberInfoBuilderByName
                .values()
                .stream()
                .map(MemberInfo.Builder::build)
                .collect(Collectors.toList()))
        .build();
  }

  private static void collectReferencedTypesAndMethodInvocations(
      Member member, MemberInfo.Builder memberInfoBuilder) {
    Set<MethodInvocation> methodInvocationSet =
        new LinkedHashSet<>(memberInfoBuilder.getInvokedMethodsList());
    Set<String> referencedTypes = new LinkedHashSet<>(memberInfoBuilder.getReferencedTypesList());

    member.accept(
        new AbstractVisitor() {
          @Override
          public void exitJavaScriptConstructorReference(JavaScriptConstructorReference node) {
            // In Javascript a Class is statically referenced by using it's constructor function.
            referencedTypes.add(getTypeId(node.getReferencedTypeDeclaration()));
          }

          @Override
          public void exitFieldAccess(FieldAccess node) {
            // Register static FieldAccess as getter/setter invocations. We are conservative here
            // because getter and setter functions has the same name: the name of the field. If a
            // field is accessed, we visits both getter and setter.
            methodInvocationSet.add(
                MethodInvocation.newBuilder()
                    .setMethod(ManglingNameUtils.getMangledName(node.getTarget()))
                    .setEnclosingType(getTypeId(node.getTarget().getEnclosingTypeDescriptor()))
                    .build());
          }

          @Override
          public void exitInvocation(Invocation node) {
            String enclosingType = getTypeId(node.getTarget().getEnclosingTypeDescriptor());

            methodInvocationSet.add(
                MethodInvocation.newBuilder()
                    .setMethod(getMemberId(node.getTarget()))
                    .setEnclosingType(enclosingType)
                    .setKind(getInvocationKind(node))
                    .build());

            if (node.getTarget().isConstructor()) {
              referencedTypes.add(enclosingType);
            }
          }
        });

    if (member.isStatic() && member.isNative()) {
      // hand-written native static method could potentially make a call to $clinit
      methodInvocationSet.add(
          MethodInvocation.newBuilder()
              .setMethod(CLINIT_METHOD_NAME)
              .setEnclosingType(getTypeId(member.getDescriptor().getEnclosingTypeDescriptor()))
              .build());
    }

    memberInfoBuilder
        .clearReferencedTypes()
        .clearInvokedMethods()
        .addAllInvokedMethods(methodInvocationSet)
        .addAllReferencedTypes(referencedTypes);
  }

  private static InvocationKind getInvocationKind(Invocation node) {
    if (node instanceof NewInstance) {
      return InvocationKind.INSTANTIATION;
    }

    if (node.getTarget().isStatic()
        // super call is always a direct call
        || node.getQualifier() instanceof SuperReference
        // A method call to a constructor is automatically a call to the super constructor,
        // otherwise it would have been a NewInstance.
        || node.getTarget().isConstructor()) {
      return InvocationKind.STATIC;
    }

    return InvocationKind.DYNAMIC;
  }

  private static String getTypeId(Type type) {
    return getTypeId(type.getDeclaration());
  }

  private static String getTypeId(DeclaredTypeDescriptor typeDescriptor) {
    return getTypeId(typeDescriptor.getTypeDeclaration());
  }

  private static String getTypeId(TypeDeclaration typeDeclaration) {
    return typeDeclaration.getModuleName();
  }

  private static String getMemberId(MemberDescriptor memberDescriptor) {
    if (memberDescriptor instanceof MethodDescriptor
        && !isPropertyAccessor((MethodDescriptor) memberDescriptor)) {
      return ManglingNameUtils.getMangledName((MethodDescriptor) memberDescriptor);
    }

    // Property (could be a property getter or setter)
    return ManglingNameUtils.getPropertyMangledName(memberDescriptor);
  }

  private static boolean isPropertyAccessor(MethodDescriptor methodDescriptor) {
    return methodDescriptor.isPropertyGetter() || methodDescriptor.isPropertySetter();
  }
}
