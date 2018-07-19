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

import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.MemberDescriptor;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protos.j2cl.LibraryInfoOuterClass.LibraryInfo;
import com.google.protos.j2cl.LibraryInfoOuterClass.MemberInfo;
import com.google.protos.j2cl.LibraryInfoOuterClass.MethodInvocation;
import com.google.protos.j2cl.LibraryInfoOuterClass.TypeInfo;
import java.util.LinkedHashSet;
import java.util.Set;

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

    for (Member member : type.getMembers()) {
      if (member.isNative()) {
        // native members are not emitted so we don't need to visit them.
        continue;
      }

      typeInfoBuilder.addMember(collectMemberInfo(member));
    }

    return typeInfoBuilder.build();
  }

  private static MemberInfo collectMemberInfo(Member member) {
    Set<MethodInvocation> methodInvocationSet = new LinkedHashSet<>();
    Set<String> typesInstantiated = new LinkedHashSet<>();

    member.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethodCall(MethodCall node) {
            methodInvocationSet.add(
                MethodInvocation.newBuilder()
                    .setMethod(getMemberId(node.getTarget()))
                    .setEnclosingType(getTypeId(node.getTarget().getEnclosingTypeDescriptor()))
                    .build());
          }

          @Override
          public void exitJavaScriptConstructorReference(JavaScriptConstructorReference node) {
            // Register the JavaScriptConstructorReference as a fake static method invocation on the
            // type referenced by the Javascript constructor. That ensure that the type is not
            // pruned in case of the type is only accessed by references to its constructor
            // function.
            // TODO(b/111563903): find a better way to express class references.
            methodInvocationSet.add(
                MethodInvocation.newBuilder()
                    .setMethod("%%JavaScriptConstructorReference%%")
                    .setEnclosingType(getTypeId(node.getReferencedTypeDeclaration()))
                    .build());
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
          public void exitNewInstance(NewInstance node) {
            typesInstantiated.add(getTypeId(node.getTarget().getEnclosingTypeDescriptor()));
          }
        });

    return MemberInfo.newBuilder()
        .setName(getMemberId(member.getDescriptor()))
        .setPublic(member.getDescriptor().getVisibility().isPublic())
        .setStatic(member.isStatic())
        .setJsAccessible(member.getDescriptor().isJsMember())
        .addAllMethodsInvoked(methodInvocationSet)
        .addAllTypesInstantiated(typesInstantiated)
        .build();
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
