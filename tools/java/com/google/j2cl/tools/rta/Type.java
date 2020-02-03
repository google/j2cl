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

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MoreCollectors;
import com.google.j2cl.libraryinfo.LibraryInfoBuilder;
import com.google.j2cl.libraryinfo.TypeInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

final class Type {
  private String name;
  private Optional<Type> superClass;
  private List<Type> superTypes;
  private boolean isInterface;
  private final LinkedHashMap<String, Member> membersByName = new LinkedHashMap<>();
  private String implSourceFile;
  private String headerSourceFile;
  private boolean live;
  private boolean instantiated;
  private final List<Member> potentiallyLiveMembers = new ArrayList<>();

  static Type buildFrom(TypeInfo typeInfo, String name) {
    Type type = new Type();
    type.name = name;
    // All classes extends at least j.l.Object except j.l.Object itself
    type.isInterface =
        typeInfo.getExtendsType() == LibraryInfoBuilder.NULL_TYPE
            && !"java.lang.Object".equals(name);
    type.headerSourceFile = typeInfo.getHeaderSourceFilePath();
    type.implSourceFile = typeInfo.getImplSourceFilePath();
    typeInfo
        .getMemberList()
        .forEach(memberInfo -> type.addMember(Member.buildFrom(memberInfo, type)));

    return type;
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

  Optional<Member> getMemberByName(String name) {
    return Optional.ofNullable(membersByName.get(name));
  }

  Optional<Member> getConstructor() {
    return Optional.ofNullable(membersByName.get("constructor"));
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
    this.superClass =
        superTypes.stream()
            .filter(Predicates.not(Type::isInterface))
            .collect(MoreCollectors.toOptional());
  }

  List<Type> getSuperTypes() {
    return superTypes;
  }

  Optional<Type> getSuperClass() {
    return superClass;
  }

  void markLive() {
    this.live = true;
  }

  boolean isLive() {
    return live;
  }

  boolean isInstantiated() {
    return instantiated;
  }

  void instantiate() {
    this.instantiated = true;
  }

  /** Returns the list of members that need to mark as live when the type becomes live. */
  List<Member> getPotentiallyLiveMembers() {
    return potentiallyLiveMembers;
  }

  void addPotentiallyLiveMember(Member member) {
    checkState(!isInstantiated());
    potentiallyLiveMembers.add(member);
  }
}
