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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.libraryinfo.TypeInfo;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

final class Type {
  private String name;
  private List<Type> superTypes;
  private boolean isInterface;
  private final LinkedHashMap<String, Member> membersByName = new LinkedHashMap<>();
  private String implSourceFile;
  private String headerSourceFile;

  static Type buildFrom(TypeInfo typeInfo) {
    Type type = new Type();

    type.name = typeInfo.getTypeId();
    type.isInterface = isInterfaceType(typeInfo);
    type.headerSourceFile = typeInfo.getHeaderSourceFilePath();
    type.implSourceFile = typeInfo.getImplSourceFilePath();
    typeInfo
        .getMemberList()
        .forEach(memberInfo -> type.addMember(Member.buildFrom(memberInfo, type)));

    return type;
  }

  private static boolean isInterfaceType(TypeInfo typeInfo) {
    // All classes extends at least j.l.Object except j.l.Object itself
    return typeInfo.getExtendsType().isEmpty() && !"java.lang.Object".equals(typeInfo.getTypeId());
  }

  private Type() {}

  String getHeaderSourceFile() {
    return headerSourceFile;
  }

  String getImplSourceFile() {
    return implSourceFile;
  }

  boolean isInterface() {
    return isInterface;
  }

  Collection<Member> getMembers() {
    return membersByName.values();
  }

  Member getMemberByName(String name) {
    return membersByName.get(name);
  }

  void addMember(Member member) {
    checkState(!membersByName.containsKey(member.getName()));
    membersByName.put(member.getName(), member);
  }

  void addMembers(Collection<Member> members) {
    members.forEach(this::addMember);
  }

  String getName() {
    return name;
  }

  void setSuperTypes(List<Type> superTypes) {
    this.superTypes = ImmutableList.copyOf(superTypes);
  }

  List<Type> getSuperTypes() {
    return superTypes;
  }

  boolean isLambdaAdaptorType() {
    return name.contains(".$LambdaAdaptor");
  }

  boolean isInternalJreType() {
    return name.startsWith("javaemul.") || name.startsWith("vmbootstrap.");
  }
}
