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
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.MemberDescriptor;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Traverse types and gather execution flow information for building call graph. */
public final class LibraryInfoBuilder {
  /** Serialize a LibraryInfo object into a JSON string. */
  public static String toJson(LibraryInfo.Builder libraryInfo, Problems problems) {
    try {
      return JsonFormat.printer().print(libraryInfo);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
      return null;
    }
  }

  public static byte[] toByteArray(LibraryInfo.Builder libraryInfo) {
    return libraryInfo.build().toByteArray();
  }

  /** Gather information from a Type and create a TypeInfo object used to build the call graph. */
  public static TypeInfo build(
      Type type,
      String headerFilePath,
      String implFilePath,
      Map<Member, com.google.j2cl.common.SourcePosition> outputSourceInfoByMember) {
    String typeId = getTypeId(type);

    TypeInfo.Builder typeInfoBuilder =
        TypeInfo.newBuilder()
            .setTypeId(typeId)
            .setHeaderSourceFilePath(headerFilePath)
            .setImplSourceFilePath(implFilePath);

    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor != null && !superTypeDescriptor.isStarOrUnknown()) {
      typeInfoBuilder.setExtendsType(getTypeId(superTypeDescriptor));
    }

    for (DeclaredTypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
      typeInfoBuilder.addImplementsType(getTypeId(superInterfaceType));
    }

    // Collect references to getter and setter for the same field under the name of the field,
    // creating only one MemberInfo instance that combines all the references appearing in their
    // bodies.
    Map<String, MemberInfo.Builder> memberInfoBuilderByName =
        new LinkedHashMap<>(type.getMembers().size());

    boolean forceJsAccessible = isAccesssedFromJ2clBootstrapJsFiles(type.getTypeDescriptor());

    for (Member member : type.getMembers()) {
      MemberDescriptor memberDescriptor = member.getDescriptor();
      String memberName = getMemberId(memberDescriptor);
      // JsMembers and JsFunctions are marked as accessible by js.
      boolean isJsAccessible =
          ((memberDescriptor.isJsFunction() || memberDescriptor.isJsMember())
                  && !shouldNotBeJsAccessible(memberDescriptor))
              || forceJsAccessible;

      MemberInfo.Builder builder =
          memberInfoBuilderByName.computeIfAbsent(
              memberName,
              m ->
                  MemberInfo.newBuilder()
                      .setName(memberName)
                      .setPublic(memberDescriptor.getVisibility().isPublic())
                      .setStatic(member.isStatic())
                      .setJsAccessible(isJsAccessible));

      com.google.j2cl.common.SourcePosition jsSourcePosition = outputSourceInfoByMember.get(member);
      if (jsSourcePosition != null) {
        builder.setPosition(createSourcePosition(jsSourcePosition));
      }

      collectReferencedTypesAndMethodInvocations(member, builder);
    }

    return typeInfoBuilder
        .addAllMember(
            memberInfoBuilderByName.values().stream()
                .map(MemberInfo.Builder::build)
                .collect(Collectors.toList()))
        .build();
  }

  private static SourcePosition createSourcePosition(
      com.google.j2cl.common.SourcePosition position) {
    return SourcePosition.newBuilder()
        .setStart(position.getStartFilePosition().getLine())
        .setEnd(position.getEndFilePosition().getLine())
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
                createMethodInvocation(node.getTarget(), getInvocationKind(node)));
          }

          @Override
          public void exitInvocation(Invocation node) {
            MethodDescriptor target = node.getTarget();
            if (target.isJsFunction()) {
              // We don't record call to JsFunction interface methods because a it doesn't
              // generate any js type. The implementation of JsFunction interface will be marked as
              // accessible by js and will be live if the implementation type is instantiated.
              return;
            }

            methodInvocationSet.add(createMethodInvocation(target, getInvocationKind(node)));

            if (target.isConstructor()) {
              referencedTypes.add(getTypeId(target.getEnclosingTypeDescriptor()));
            }
          }
        });

    if (member.isStatic() && member.isNative()) {
      // hand-written native static method could potentially make a call to $clinit
      methodInvocationSet.add(
          createMethodInvocation(
              member.getDescriptor().getEnclosingTypeDescriptor().getClinitMethodDescriptor(),
              InvocationKind.STATIC));
    }

    memberInfoBuilder
        .clearReferencedTypes()
        .clearInvokedMethods()
        .addAllInvokedMethods(methodInvocationSet)
        .addAllReferencedTypes(referencedTypes);
  }

  private static MethodInvocation createMethodInvocation(
      MemberDescriptor memberDescriptor, InvocationKind invocationKind) {
    return MethodInvocation.newBuilder()
        .setMethod(getMemberId(memberDescriptor))
        .setEnclosingType(getTypeId(memberDescriptor.getEnclosingTypeDescriptor()))
        .setKind(invocationKind)
        .build();
  }

  private static InvocationKind getInvocationKind(MemberReference node) {
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

  /**
   * Members with these origins are marked as JsMembers for naming or boilerplate reasons, do not
   * consider them accessible by JavaScript code.
   */
  // TODO(b/116712070): make sure these members are not internally marked as JsMethod/JsConstructor,
  // So that this hack can be removed.
  private static final ImmutableSet<MemberDescriptor.Origin> NOT_ACCESSIBLE_BY_JS_ORIGINS =
      ImmutableSet.of(
          MethodOrigin.SYNTHETIC_CLASS_INITIALIZER,
          MethodOrigin.SYNTHETIC_ADAPT_LAMBDA,
          MethodOrigin.SYNTHETIC_LAMBDA_ADAPTOR_CONSTRUCTOR);

  /**
   * Returns {@code true} if the member is marked JsMethod/JsConstructor for convenience but not
   * supposed to be accessible from JavaScript code.
   */
  private static boolean shouldNotBeJsAccessible(MemberDescriptor memberDescriptor) {
    return NOT_ACCESSIBLE_BY_JS_ORIGINS.contains(memberDescriptor.getOrigin());
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
