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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.concat;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.libraryinfo.LibraryInfo;
import com.google.j2cl.libraryinfo.LibraryInfoBuilder;
import com.google.j2cl.libraryinfo.MemberInfo;
import com.google.j2cl.libraryinfo.MethodInvocation;
import com.google.j2cl.libraryinfo.TypeInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Give information about inheritance relationships between types. */
public class TypeGraphBuilder {

  static List<Type> build(List<LibraryInfo> libraryInfos) {
    Map<String, Type> typesByName = new HashMap<>();

    // Create all types and members.
    for (LibraryInfo libraryInfo : libraryInfos) {
      for (TypeInfo typeInfo : libraryInfo.getTypeList()) {
        Type type = Type.buildFrom(typeInfo, libraryInfo.getTypeMap(typeInfo.getTypeId()));
        typesByName.put(type.getName(), type);
      }
    }

    // Build cross-references between types and members
    for (LibraryInfo libraryInfo : libraryInfos) {
      addMemberReferences(typesByName, libraryInfo);
    }

    for (Type type : typesByName.values()) {
      for (Type superType : type.getSuperTypes()) {
        superType.addImmediateSubtype(type);
      }
    }

    return ImmutableList.copyOf(typesByName.values());
  }

  private static void addMemberReferences(Map<String, Type> typesByName, LibraryInfo libraryInfo) {
    for (TypeInfo typeInfo : libraryInfo.getTypeList()) {
      Type type = typesByName.get(libraryInfo.getTypeMap(typeInfo.getTypeId()));
      type.setSuperTypes(
          concat(Stream.of(typeInfo.getExtendsType()), typeInfo.getImplementsTypeList().stream())
              .filter(x -> x != LibraryInfoBuilder.NULL_TYPE)
              .map(x -> checkNotNull(typesByName.get(libraryInfo.getTypeMap(x))))
              .collect(toImmutableList()));

      for (MemberInfo memberInfo : typeInfo.getMemberList()) {
        Member member = type.getMemberByName(memberInfo.getName());
        member.setReferencedTypes(
            memberInfo.getReferencedTypesList().stream()
                .map(libraryInfo::getTypeMap)
                .map(typesByName::get)
                .collect(toImmutableList()));

        addMemberReferences(libraryInfo, memberInfo.getInvokedMethodsList(), member, typesByName);
      }
    }
  }

  private static void addMemberReferences(
      LibraryInfo libraryInfo,
      List<MethodInvocation> methodInvocations,
      Member member,
      Map<String, Type> typesByName) {
    for (MethodInvocation methodInvocation : methodInvocations) {
      Type enclosingType =
          typesByName.get(libraryInfo.getTypeMap(methodInvocation.getEnclosingType()));
      Member referencedMember = enclosingType.getMemberByName(methodInvocation.getMethod());
      member.addReferencedMember(referencedMember);
    }
  }

  private TypeGraphBuilder() {}
}
