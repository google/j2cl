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


import com.google.common.collect.ImmutableSet;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.MemberDescriptor;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Traverse types and gather execution flow information for building call graph. */
public final class LibraryInfoBuilder {

  public static final int NULL_TYPE = 0;
  private final LibraryInfo.Builder libraryInfo = LibraryInfo.newBuilder();
  private final Map<String, Integer> types = new HashMap<>();

  public void addType(
      Type type,
      String headerFilePath,
      String implFilePath,
      Map<Member, com.google.j2cl.common.SourcePosition> outputSourceInfoByMember) {

    if (!isPrunableType(type.getTypeDescriptor())) {
      return;
    }

    TypeInfo.Builder typeInfoBuilder =
        TypeInfo.newBuilder()
            .setTypeId(getTypeId(type))
            .setHeaderSourceFilePath(headerFilePath)
            .setImplSourceFilePath(implFilePath);

    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor != null && !superTypeDescriptor.isNative()) {
      typeInfoBuilder.setExtendsType(getTypeId(superTypeDescriptor));
    }

    for (DeclaredTypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
      if (!superInterfaceType.isNative() && !superInterfaceType.isJsFunctionInterface()) {
        typeInfoBuilder.addImplementsType(getTypeId(superInterfaceType));
      }
    }

    // Collect references to getter and setter for the same field under the name of the field,
    // creating only one MemberInfo instance that combines all the references appearing in their
    // bodies.
    Map<String, MemberInfo.Builder> memberInfoBuilderByName =
        new LinkedHashMap<>(type.getMembers().size());

    // Note that we are not collecting bootstrap classes in references so we need to force them in.
    // There are 2 reasons for that:
    //  - We are inconsistent in marking their references wrt. being native or not.
    //  - They are frequently used and never pruned; recording them is wasteful.
    boolean forceJsAccessible =
        TypeDescriptors.isBootstrapNamespace(type.getTypeDescriptor())
            || isAccesssedFromJ2clBootstrapJsFiles(type.getTypeDescriptor());

    for (Member member : type.getMembers()) {
      MemberDescriptor memberDescriptor = member.getDescriptor();
      String memberName = getMemberId(memberDescriptor);
      boolean isJsAccessible = memberDescriptor.canBeReferencedExternally() || forceJsAccessible;

      MemberInfo.Builder builder =
          memberInfoBuilderByName.computeIfAbsent(
              memberName,
              m ->
                  MemberInfo.newBuilder()
                      .setName(memberName)
                      .setStatic(member.isStatic())
                      .setJsAccessible(isJsAccessible));

      com.google.j2cl.common.SourcePosition jsSourcePosition = outputSourceInfoByMember.get(member);
      if (jsSourcePosition != null) {
        builder.setPosition(createSourcePosition(jsSourcePosition));
      }

      collectReferencedTypesAndMethodInvocations(member, builder);
    }

    libraryInfo.addType(
        typeInfoBuilder.addAllMember(
            memberInfoBuilderByName.values().stream()
                .map(MemberInfo.Builder::build)
                .collect(Collectors.toList())));
  }

  private static SourcePosition createSourcePosition(
      com.google.j2cl.common.SourcePosition position) {
    return SourcePosition.newBuilder()
        .setStart(position.getStartFilePosition().getLine())
        .setEnd(position.getEndFilePosition().getLine())
        .build();
  }

  private void collectReferencedTypesAndMethodInvocations(
      Member member, MemberInfo.Builder memberInfoBuilder) {
    Set<MethodInvocation> methodInvocationSet =
        new LinkedHashSet<>(memberInfoBuilder.getInvokedMethodsList());
    Set<Integer> referencedTypes = new LinkedHashSet<>(memberInfoBuilder.getReferencedTypesList());

    member.accept(
        new AbstractVisitor() {
          @Override
          public void exitJavaScriptConstructorReference(JavaScriptConstructorReference node) {
            if (!isPrunableType(node.getReferencedTypeDeclaration().toRawTypeDescriptor())) {
              return;
            }

            if (TypeDescriptors.isBootstrapNamespace(node.getReferencedTypeDeclaration())) {
              // Bootstrap classes already included by marking them as js accessible.
              return;
            }

            // In Javascript a Class is statically referenced by using it's constructor function.
            referencedTypes.add(getTypeId(node.getReferencedTypeDeclaration()));
          }

          @Override
          public void exitFieldAccess(FieldAccess node) {
            FieldDescriptor target = node.getTarget();

            if (!isPrunableType(target.getEnclosingTypeDescriptor())) {
              return;
            }

            // TODO(b/34928687): Remove after $isinstance and $loadmodule moved the AST.
            if (target.getName().equals("prototype") || target.getName().equals("$copy")) {
              return;
            }

            if (target.getName().equals("$clinit")) {
              return;
            }

            // Register static FieldAccess as getter/setter invocations. We are conservative here
            // because getter and setter functions has the same name: the name of the field. If a
            // field is accessed, we visit both getter and setter.
            methodInvocationSet.add(createMethodInvocation(target));
          }

          @Override
          public void exitInvocation(Invocation node) {
            MethodDescriptor target = node.getTarget();

            if (!isPrunableType(target.getEnclosingTypeDescriptor())) {
              return;
            }

            if (TypeDescriptors.isBootstrapNamespace(target.getEnclosingTypeDescriptor())) {
              // Bootstrap classes already included by marking them as js accessible.
              return;
            }

            if (target.isJsFunction()) {
              // We don't record call to JsFunction interface methods because it doesn't
              // generate any js type. The implementation of JsFunction interface will be marked as
              // accessible by js and will be live if the implementation type is instantiated.
              return;
            }

            if (target.getName().equals("$clinit")) {
              return;
            }

            // TODO(b/34928687): Remove after $isinstance and $loadmodule moved the AST.
            if (target.getName().equals("$isInstance") || target.getName().equals("$loadModules")) {
              return;
            }

            methodInvocationSet.add(createMethodInvocation(target));
          }
        });

    // Remove redundant references recorded due to JavaScriptConstructorReferences which are already
    // recorded via invocations (e.g. all static calls introduce JavaScriptConstructorReference).
    methodInvocationSet.stream()
        .map(MethodInvocation::getEnclosingType)
        .forEach(referencedTypes::remove);

    memberInfoBuilder
        .clearReferencedTypes()
        .clearInvokedMethods()
        .addAllInvokedMethods(methodInvocationSet)
        .addAllReferencedTypes(referencedTypes);
  }

  private MethodInvocation createMethodInvocation(MemberDescriptor memberDescriptor) {
    return MethodInvocation.newBuilder()
        .setMethod(getMemberId(memberDescriptor))
        .setEnclosingType(getTypeId(memberDescriptor.getEnclosingTypeDescriptor()))
        .build();
  }

  private int getTypeId(Type type) {
    return getTypeId(type.getDeclaration());
  }

  private int getTypeId(DeclaredTypeDescriptor typeDescriptor) {
    return getTypeId(typeDescriptor.getTypeDeclaration());
  }

  private int getTypeId(TypeDeclaration typeDeclaration) {
    // Note that the IDs start from '1' to reserve '0' for NULL_TYPE.
    return types.computeIfAbsent(typeDeclaration.getModuleName(), x -> types.size() + 1);
  }

  private LibraryInfo build() {
    libraryInfo.clearTypeMap();
    String[] typeMap = new String[types.size() + 1];
    typeMap[NULL_TYPE] = "<no-type>";
    types.forEach((name, i) -> typeMap[i] = name);
    libraryInfo.addAllTypeMap(Arrays.asList(typeMap));
    return libraryInfo.build();
  }

  /** Serialize a LibraryInfo object into a JSON string. */
  public String toJson(Problems problems) {
    try {
      return JsonFormat.printer().print(build());
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
      return null;
    }
  }

  public byte[] toByteArray() {
    return build().toByteArray();
  }

  private static String getMemberId(MemberDescriptor memberDescriptor) {
    if (memberDescriptor instanceof MethodDescriptor
        && !isPropertyAccessor((MethodDescriptor) memberDescriptor)) {
      return ManglingNameUtils.getMangledName((MethodDescriptor) memberDescriptor);
    }

    // Property (could be a property getter or setter)
    return ManglingNameUtils.getPropertyMangledName(memberDescriptor);
  }

  private static boolean isPrunableType(DeclaredTypeDescriptor typeDescriptor) {
    return !typeDescriptor.isNative() && !typeDescriptor.isJsEnum();
  }

  private static boolean isPropertyAccessor(MethodDescriptor methodDescriptor) {
    return methodDescriptor.isPropertyGetter() || methodDescriptor.isPropertySetter();
  }

  // There are references to non JsMember members of these types from JavaScript in the J2CL
  // runtime. For now we will consider and all their members accessible by JavaScript code.
  // TODO(b/29509857):  remove once the refactoring of the code in nativebootstrap and vmbootstrap
  // is completed and the references removed.
  private static final ImmutableSet<String> TYPES_ACCESSED_FROM_J2CL_BOOTSTRAP_JS =
      ImmutableSet.of(
          "javaemul.internal.InternalPreconditions",
          "java.lang.Double",
          "java.lang.Boolean",
          "java.lang.Number",
          "java.lang.CharSequence",
          "java.lang.String",
          "java.lang.Class",
          "java.lang.Comparable",
          "java.lang.Integer");

  private static boolean isAccesssedFromJ2clBootstrapJsFiles(
      DeclaredTypeDescriptor typeDescriptor) {
    return TYPES_ACCESSED_FROM_J2CL_BOOTSTRAP_JS.contains(typeDescriptor.getQualifiedSourceName());
  }
}
