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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.j2cl.libraryinfo.InvocationKind;
import com.google.j2cl.libraryinfo.MemberInfo;
import java.util.List;

final class Member {
  static Member buildFrom(MemberInfo memberInfo, Type declaringType) {
    Member member = new Member();
    member.name = memberInfo.getName();
    member.isStatic = memberInfo.getStatic();
    member.jsAccessible = memberInfo.getJsAccessible();
    member.declaringType = declaringType;

    return member;
  }

  private final Multimap<InvocationKind, Member> referencedMembers = HashMultimap.create();
  private String name;
  private Type declaringType;
  private List<Type> referencedTypes;
  private boolean jsAccessible;
  private boolean isStatic;

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

  Multimap<InvocationKind, Member> getReferencedMembers() {
    return referencedMembers;
  }

  void addReferencedMember(InvocationKind invocationKind, Member referencedMember) {
    referencedMembers.put(invocationKind, referencedMember);
  }

  List<Type> getReferencedTypes() {
    return referencedTypes;
  }

  void setReferencedTypes(List<Type> referencedTypes) {
    this.referencedTypes = referencedTypes;
  }

  boolean isConstructor() {
    return "constructor".equals(getName());
  }

  /**
   * Return {@code true} if this member is an entry point.
   *
   * <p>An entry point is a member that can be statically called outside. JsAccessible constructors
   * and static members are considered as entry points of the call graph.
   */
  boolean isEntryPoint() {
    return isJsAccessible() && !isInstanceMember();
  }

  boolean isInstanceMember() {
    return !isConstructor() && !isStatic;
  }
}
