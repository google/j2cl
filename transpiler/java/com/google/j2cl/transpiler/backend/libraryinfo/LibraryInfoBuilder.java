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
package com.google.j2cl.transpiler.backend.libraryinfo;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDeclarationStatement;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** Traverse types and gather execution flow information for building call graph. */
public final class LibraryInfoBuilder {

  public static final int NULL_TYPE = 0;
  private final LibraryInfo.Builder libraryInfo = LibraryInfo.newBuilder();
  private final Map<String, Integer> types = new HashMap<>();

  public void addType(
      Type type,
      String headerFilePath,
      String implFilePath,
      Map<MemberDescriptor, SourcePosition> outputSourceInfoByMember) {

    if (!isPrunableType(type.getTypeDescriptor())) {
      return;
    }

    TypeInfo.Builder typeInfoBuilder =
        TypeInfo.newBuilder()
            .setTypeId(getTypeId(type.getTypeDescriptor()))
            .setHeaderSourceFilePath(headerFilePath)
            .setImplSourceFilePath(implFilePath)
            .setJstypeInterface(type.isInterface() && type.getTypeDescriptor().isJsType());

    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor != null
        && !superTypeDescriptor.isNative()
        && !TypeDescriptors.isJavaLangObject(superTypeDescriptor)) {
      typeInfoBuilder.setExtendsType(getTypeId(superTypeDescriptor));
    }

    for (DeclaredTypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
      if (!superInterfaceType.isNative() && !superInterfaceType.isJsFunctionInterface()) {
        typeInfoBuilder.addImplementsType(getTypeId(superInterfaceType));
      }
    }

    // Collect references to getter and setter for the same field under the same key to
    // create only one MemberInfo instance that combines all the references appearing in their
    // bodies (see #getMemberId).
    Map<String, MemberInfo.Builder> memberInfoBuilders =
        Maps.newLinkedHashMapWithExpectedSize(type.getMembers().size());

    boolean hasConstantEntryPoint = false;
    for (Member member : type.getMembers()) {
      MemberDescriptor memberDescriptor = member.getDescriptor();

      if (memberDescriptor.hasJsNamespace()) {
        // Members with an explicit namespace members don't really belong to the type. Skip them
        // here, otherwise they would be an entry point for this type, and the type might be
        // unnecessarily retained by rta.
        continue;
      }

      if (memberDescriptor.getOrigin().isInstanceOfSupportMember()) {
        // InstanceOf support members should not be considered methods that are prunable if there
        // are no references, since the references are hidden by the runtime. In the end
        // InstanceOf support members are live whenever the type is live.
        continue;
      }

      if (memberDescriptor.isField() && !mayTriggerClinit(memberDescriptor)) {
        if (memberDescriptor.isCompileTimeConstant() && isJsAccessible(memberDescriptor)) {
          hasConstantEntryPoint = true;
        }
        // We don't need to record fields, there is not much value on pruning them. However there is
        // slight complication for fields that may trigger clinit which may (or may not) generated
        // as getter, so we need to record their usage (hence their data here as well).
        continue;
      }

      MemberInfo.Builder builder =
          memberInfoBuilders.computeIfAbsent(
              getMemberId(memberDescriptor),
              m -> createMemberInfo(memberDescriptor, outputSourceInfoByMember));

      collectReferencedTypesAndMethodInvocations(member, builder);
    }

    if (type.isOptimizedEnum()) {
      // Optimized enums initialize the enum constants at load time. We need to visit the load time
      // statements for collecting all method invocations used to initialize the enum fields and
      // associate them with those fields. When RTA will determine that an enum field is used, it
      // will make the method used to initialize that field live and will traverse it.
      for (Statement statement : type.getLoadTimeStatements()) {
        if (!(statement instanceof FieldDeclarationStatement)) {
          continue;
        }
        FieldDeclarationStatement fieldDeclarationStatement = (FieldDeclarationStatement) statement;
        if (!fieldDeclarationStatement.getFieldDescriptor().isEnumConstant()) {
          continue;
        }

        String fieldId = getMemberId(fieldDeclarationStatement.getFieldDescriptor());

        fieldDeclarationStatement.accept(
            new AbstractVisitor() {
              @Override
              public void exitMethodCall(MethodCall methodCall) {
                if (isJsAccessible(methodCall.getTarget())) {
                  // We don't record access to js accessible fields since they are never pruned.
                  return;
                }
                // We only expect static Method Invocation at that point.
                checkState(methodCall.getTarget().isStatic());

                memberInfoBuilders
                    .get(fieldId)
                    .addInvokedMethods(createMethodInvocation(methodCall.getTarget()));
              }
            });
      }
    }

    if (hasConstantEntryPoint) {
      // Ensure the type is not pruned by RTA.
      memberInfoBuilders.put(
          "$js_entry$",
          MemberInfo.newBuilder().setName("$js_entry$").setStatic(true).setJsAccessible(true));
    }

    libraryInfo.addType(
        typeInfoBuilder.addAllMember(
            memberInfoBuilders.values().stream()
                .map(MemberInfo.Builder::build)
                .collect(toImmutableList())));
  }

  private static MemberInfo.Builder createMemberInfo(
      MemberDescriptor memberDescriptor,
      Map<MemberDescriptor, SourcePosition> outputSourceInfoByMember) {
    MemberInfo.Builder memberInfoBuilder =
        MemberInfo.newBuilder()
            .setName(getMemberId(memberDescriptor))
            .setStatic(memberDescriptor.isStatic())
            .setJsAccessible(isJsAccessible(memberDescriptor));

    // See the limitations of member removal in b/177365417.
    SourcePosition position = outputSourceInfoByMember.get(memberDescriptor);
    if (position != null) {
      memberInfoBuilder.setPosition(
          com.google.j2cl.transpiler.backend.libraryinfo.SourcePosition.newBuilder()
              .setStart(position.getStartFilePosition().getLine())
              // For the minifier, end position is exclusive.
              .setEnd(position.getEndFilePosition().getLine() + 1)
              .build());
    }

    return memberInfoBuilder;
  }

  private void collectReferencedTypesAndMethodInvocations(
      Member member, MemberInfo.Builder memberInfoBuilder) {
    Set<MethodInvocation> invokedMethods =
        new LinkedHashSet<>(memberInfoBuilder.getInvokedMethodsList());

    // The set of types that are explicitly referenced in this member; these come from
    // JavaScriptConstructorReferences that appear in the AST from type literals, casts,
    // instanceofs and also the qualifier in every static member reference.
    // References to static members already include the enclosing class, so in order to avoid
    // redundancy in library info these types are tracked separately and removed.
    Set<Integer> explicitlyReferencedTypes =
        new LinkedHashSet<>(memberInfoBuilder.getReferencedTypesList());

    // The set of types that are implicitly referenced in this member; these come from static
    // Invocations in the AST, e.g. the enclosing class of a static method call. These types will be
    // preserved by RTA when seeing the reference to the static member so there is no need to
    // separately record them as referenced types.
    Set<Integer> typesReferencedViaStaticMemberReferences = new HashSet<>();

    member.accept(
        new AbstractVisitor() {
          @Override
          public void exitJavaScriptConstructorReference(JavaScriptConstructorReference node) {
            DeclaredTypeDescriptor referencedType =
                node.getReferencedTypeDeclaration().toRawTypeDescriptor();

            if (!isPrunableType(referencedType)) {
              return;
            }

            if (isJsAccessible(referencedType)) {
              return;
            }

            // No need to record references to parent or itself since they will be live regardless.
            if (member.getDescriptor().getEnclosingTypeDescriptor().isSubtypeOf(referencedType)) {
              return;
            }

            // In Javascript a Class is statically referenced by using its constructor function.
            explicitlyReferencedTypes.add(getTypeId(referencedType));
          }

          @Override
          public void exitFieldAccess(FieldAccess node) {
            FieldDescriptor target = node.getTarget();

            if (!isPrunableType(target.getEnclosingTypeDescriptor())) {
              return;
            }

            if (isJsAccessible(target)) {
              // We don't record access to js accessible fields since they are never pruned.
              return;
            }

            if (!mayTriggerClinit(target)) {
              return;
            }

            // Register static FieldAccess as getter/setter invocations. We are conservative here
            // because getter and setter functions have the same name: i.e. the name of the field.
            // If a field is accessed, we visit both getter and setter.
            addInvokedMethod(target);
          }

          @Override
          public void exitInvocation(Invocation node) {
            MethodDescriptor target = node.getTarget();

            if (!isPrunableType(target.getEnclosingTypeDescriptor())) {
              return;
            }

            if (isJsAccessible(target)) {
              // We don't record call to js accessible methods since they are never pruned.
              return;
            }

            // Only record a $clinit call if it is from a clinit itself. All other clinit calls are
            // from the entry points of the class and doesn't need recording since RapidTypeAnalyser
            // will make the clinit alive when it arrives to an entry point.
            if (target.getName().equals("$clinit")
                && !member.getDescriptor().getName().equals("$clinit")) {
              return;
            }

            // TODO(b/34928687): Remove after $loadmodule moved the AST.
            if (target.getName().equals("$loadModules")) {
              return;
            }

            addInvokedMethod(target);
          }

          private void addInvokedMethod(MemberDescriptor target) {
            invokedMethods.add(createMethodInvocation(target));
            if (!target.isInstanceMember()) {
              typesReferencedViaStaticMemberReferences.add(
                  getTypeId(target.getEnclosingTypeDescriptor()));
            }
          }
        });

    memberInfoBuilder
        .clearReferencedTypes()
        .clearInvokedMethods()
        .addAllInvokedMethods(invokedMethods)
        // Record only the explicit type references without the implicit ones which are redundant.
        .addAllReferencedTypes(
            Sets.difference(explicitlyReferencedTypes, typesReferencedViaStaticMemberReferences));
  }

  private MethodInvocation createMethodInvocation(MemberDescriptor memberDescriptor) {
    return MethodInvocation.newBuilder()
        .setMethod(getMemberId(memberDescriptor))
        .setEnclosingType(getTypeId(memberDescriptor.getEnclosingTypeDescriptor()))
        .build();
  }

  private int getTypeId(DeclaredTypeDescriptor typeDescriptor) {
    // Note that the IDs start from '1' to reserve '0' for NULL_TYPE.
    return types.computeIfAbsent(typeDescriptor.getQualifiedJsName(), x -> types.size() + 1);
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
  @Nullable
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
    // TODO(b/158014657): remove this once the bug is fixed.
    String mangledName =
        isPropertyAccessor(memberDescriptor)
            ? memberDescriptor.computePropertyMangledName()
            : memberDescriptor.getMangledName();

    // Avoid unintended collisions by using the separate namespace for static and non-static.
    return memberDescriptor.isInstanceMember() ? mangledName + "_$i" : mangledName;
  }

  private static boolean mayTriggerClinit(MemberDescriptor memberDescriptor) {
    return memberDescriptor.isStatic()
        && (!memberDescriptor.isCompileTimeConstant() || memberDescriptor.isEnumConstant());
  }

  private static boolean isPropertyAccessor(MemberDescriptor memberDescriptor) {
    if (!memberDescriptor.isMethod()) {
      return false;
    }
    MethodDescriptor methodDescriptor = (MethodDescriptor) memberDescriptor;
    return methodDescriptor.isPropertyGetter() || methodDescriptor.isPropertySetter();
  }

  private static boolean isPrunableType(DeclaredTypeDescriptor typeDescriptor) {
    return !typeDescriptor.isNative() && !typeDescriptor.isJsEnum();
  }

  private static boolean isJsAccessible(MemberDescriptor memberDescriptor) {
    // Note that we are not collecting bootstrap classes in references so we need to force them in.
    // There are 2 reasons for that:
    //  - We are inconsistent in marking their references wrt. being native or not.
    //  - They are frequently used and never pruned; recording them is wasteful.
    return isInBootstrap(memberDescriptor.getEnclosingTypeDescriptor())
        || memberDescriptor.canBeReferencedExternally();
  }

  private static boolean isJsAccessible(DeclaredTypeDescriptor typeDescriptor) {
    return isInBootstrap(typeDescriptor)
        || typeDescriptor.getDeclaredMemberDescriptors().stream()
            .filter(Predicates.not(MemberDescriptor::isInstanceMember))
            .anyMatch(MemberDescriptor::isJsMember);
  }

  private static boolean isInBootstrap(DeclaredTypeDescriptor typeDescriptor) {
    return TypeDescriptors.isBootstrapNamespace(typeDescriptor)
        || isAccesssedFromJ2clBootstrapJsFiles(typeDescriptor);
  }

  // There are references to non JsMember members of these types from JavaScript in the J2CL
  // runtime. For now we will consider and all their members accessible by JavaScript code.
  // TODO(b/29509857):  remove once the refactoring of the code in nativebootstrap and vmbootstrap
  // is completed and the references removed.
  private static final ImmutableSet<String> TYPES_ACCESSED_FROM_J2CL_BOOTSTRAP_JS =
      ImmutableSet.of(
          "java.lang.Boolean",
          "java.lang.Byte",
          "java.lang.Character",
          "java.lang.CharSequence",
          "java.lang.Class",
          "java.lang.Comparable",
          "java.lang.Double",
          "java.lang.Float",
          "java.lang.Integer",
          "java.lang.Long",
          "java.lang.Number",
          "java.lang.Object",
          "java.lang.Short",
          "java.lang.String",
          "java.lang.Void",
          "javaemul.internal.InternalPreconditions");

  private static boolean isAccesssedFromJ2clBootstrapJsFiles(
      DeclaredTypeDescriptor typeDescriptor) {
    return TYPES_ACCESSED_FROM_J2CL_BOOTSTRAP_JS.contains(typeDescriptor.getQualifiedSourceName());
  }
}
