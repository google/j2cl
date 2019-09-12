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

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.j2cl.libraryinfo.InvocationKind;
import com.google.j2cl.libraryinfo.MemberInfo;
import java.util.ArrayList;
import java.util.List;

final class Member {
  static Member buildFrom(MemberInfo memberInfo, Type declaringType) {
    Member member = new Member();
    member.name = memberInfo.getName();
    member.isStatic = memberInfo.getStatic();
    member.jsAccessible = memberInfo.getJsAccessible();
    member.declaringType = declaringType;
    if (memberInfo.hasStartPosition() && memberInfo.hasEndPosition()) {
      member.startLine = memberInfo.getStartPosition().getLine();
      member.endLine = memberInfo.getEndPosition().getLine();
    }

    return member;
  }

  private String name;
  private Type declaringType;
  private boolean jsAccessible;
  private boolean isStatic;
  private int startLine = -1;
  private int endLine = -1;

  private boolean fullyTraversed;
  private boolean live;
  private List<Type> referencedTypes;
  private final Multimap<InvocationKind, Member> referencedMembers =
      MultimapBuilder.enumKeys(InvocationKind.class).arrayListValues().build();
  private final List<Type> overridingTypes = new ArrayList<>();
  private final List<Type> inheritingTypes = new ArrayList<>();

  private Member() {}

  Type getDeclaringType() {
    return declaringType;
  }

  boolean isJsAccessible() {
    return jsAccessible;
  }

  String getName() {
    return name;
  }

  int getStartLine() {
    return startLine;
  }

  int getEndLine() {
    return endLine;
  }

  InvocationKind getDefaultInvocationKind() {
    if (isStatic) {
      return InvocationKind.STATIC;
    } else if ("constructor".equals(name)) {
      return InvocationKind.INSTANTIATION;
    } else {
      return InvocationKind.DYNAMIC;
    }
  }

  boolean isLive() {
    return live;
  }

  void markLive() {
    this.live = true;
  }

  boolean isFullyTraversed() {
    return fullyTraversed;
  }

  void markFullyTraversed() {
    this.fullyTraversed = true;
  }

  List<Type> getReferencedTypes() {
    return referencedTypes;
  }

  void setReferencedTypes(List<Type> referencedTypes) {
    this.referencedTypes = referencedTypes;
  }

  Multimap<InvocationKind, Member> getReferencedMembers() {
    return referencedMembers;
  }

  void addReferencedMember(InvocationKind invocationKind, Member referencedMember) {
    referencedMembers.put(invocationKind, referencedMember);
  }

  /** Returns the list of types overriding the implementation of this member. */
  List<Type> getOverridingTypes() {
    return overridingTypes;
  }

  void addOverridingType(Type type) {
    overridingTypes.add(type);
  }

  /**
   * Returns the list of types inheriting the implementation of this member including the enclosing
   * type of the member.
   */
  List<Type> getInheritingTypes() {
    return inheritingTypes;
  }

  void addInheritingType(Type type) {
    inheritingTypes.add(type);
  }
}
