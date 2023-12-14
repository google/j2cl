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
package com.google.j2cl.tools.rta;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.transpiler.backend.libraryinfo.LibraryInfo;
import com.google.j2cl.transpiler.backend.libraryinfo.LibraryInfoBuilder;
import com.google.j2cl.transpiler.backend.libraryinfo.MemberInfo;
import com.google.j2cl.transpiler.backend.libraryinfo.MethodInvocation;
import com.google.j2cl.transpiler.backend.libraryinfo.TypeInfo;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Give information about inheritance relationships between types. */
class TypeGraphBuilder {

  static Collection<Type> build(List<LibraryInfo> libraryInfos) {
    Map<String, Type> typesByName = new LinkedHashMap<>();

    // Create all types and members.
    for (LibraryInfo libraryInfo : libraryInfos) {
      for (TypeInfo typeInfo : libraryInfo.getTypesList()) {
        Type type = Type.buildFrom(typeInfo, libraryInfo.getTypeNames(typeInfo.getTypeId()));
        typesByName.put(type.getName(), type);
      }
    }

    // Build cross-references between types and members
    for (LibraryInfo libraryInfo : libraryInfos) {
      buildCrossReferences(typesByName, libraryInfo);
    }

    return typesByName.values();
  }

  private static void buildCrossReferences(Map<String, Type> typesByName, LibraryInfo libraryInfo) {
    for (TypeInfo typeInfo : libraryInfo.getTypesList()) {
      Type type = typesByName.get(libraryInfo.getTypeNames(typeInfo.getTypeId()));

      int extendsId = typeInfo.getExtendsType();
      if (extendsId != LibraryInfoBuilder.NULL_TYPE) {
        Type superClass = typesByName.get(libraryInfo.getTypeNames(extendsId));
        superClass.addImmediateSubtype(type);
        type.setSuperClass(superClass);
      }

      for (int implementsId : typeInfo.getImplementsTypesList()) {
        Type superInterface = typesByName.get(libraryInfo.getTypeNames(implementsId));
        superInterface.addImmediateSubtype(type);
        type.addSuperInterface(superInterface);
      }

      for (MemberInfo memberInfo : typeInfo.getMembersList()) {
        Member member = type.getMemberByName(memberInfo.getName());

        for (int referencedId : memberInfo.getReferencedTypesList()) {
          Type referencedType = typesByName.get(libraryInfo.getTypeNames(referencedId));
          member.addReferencedType(checkNotNull(referencedType));
        }

        for (MethodInvocation methodInvocation : memberInfo.getInvokedMethodsList()) {
          String enclosingTypeName = libraryInfo.getTypeNames(methodInvocation.getEnclosingType());
          String methodName = methodInvocation.getMethod();

          Type enclosingType = typesByName.get(enclosingTypeName);
          member.addReferencedMember(
              checkNotNull(
                  enclosingType.getMemberByName(methodName),
                  "Missing %s.%s",
                  enclosingTypeName,
                  methodName));
        }
      }
    }
  }

  private TypeGraphBuilder() {}
}
