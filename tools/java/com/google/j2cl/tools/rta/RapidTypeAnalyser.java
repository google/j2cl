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

import static com.google.common.base.Predicates.not;
import static java.util.stream.Collectors.toSet;

import com.google.j2cl.libraryinfo.InvocationKind;
import com.google.j2cl.libraryinfo.TypeInfo;
import java.util.Collection;
import java.util.List;
import java.util.Set;

final class RapidTypeAnalyser {

  static RtaResult analyse(List<TypeInfo> typeInfos) {
    List<Type> types = TypeGraphBuilder.build(typeInfos);

    // Go over the entry points to start the traversal.
    types.stream()
        .flatMap(t -> t.getMembers().stream())
        .filter(Member::isJsAccessible)
        .forEach(m -> onMemberReference(m.getDefaultInvocationKind(), m));

    return RtaResult.build(getUnusedTypes(types), getUnusedMembers(types));
  }

  private static Set<Type> getUnusedTypes(List<Type> types) {
    return types.stream().filter(not(Type::isLive)).collect(toSet());
  }

  private static Set<Member> getUnusedMembers(List<Type> types) {
    return types.stream()
        .flatMap(t -> t.getMembers().stream())
        .filter(not(Member::isLive))
        .collect(toSet());
  }

  private static void onMemberReference(InvocationKind invocationKind, Member member) {
    switch (invocationKind) {
      case DYNAMIC:
        traversePolymorphicReference(member);
        break;
      case STATIC:
        markTypeLive(member.getDeclaringType());
        markMemberLive(member);
        break;
      case INSTANTIATION:
        instantiate(member.getDeclaringType());
        markMemberLive(member);
        break;
      default:
        throw new AssertionError(invocationKind);
    }
  }

  private static void onTypeReference(Type type) {
    markTypeLive(type);
  }

  private static void markMemberLive(Member member) {
    if (member.isLive()) {
      return;
    }

    member.markLive();

    member.getReferencedTypes().forEach(RapidTypeAnalyser::onTypeReference);
    member.getReferencedMembers().forEach(RapidTypeAnalyser::onMemberReference);
  }

  private static void traversePolymorphicReference(Member member) {
    if (member.isFullyTraversed()) {
      return;
    }
    member.markFullyTraversed();

    markMemberPotentiallyLive(member);

    // Recursively unfold the overriding chain.
    for (Type overridingType : member.getOverridingTypes()) {
      Member newTargetMember = overridingType.getMemberByName(member.getName());
      traversePolymorphicReference(newTargetMember);
    }
  }

  private static void markMemberPotentiallyLive(Member member) {
    // Set of types inheriting this member
    List<Type> inheritingTypes = member.getInheritingTypes();

    if (containsInstantiatedTypes(inheritingTypes)) {
      markMemberLive(member);
    } else {
      // None of the types inheriting this polymorphic member is instantiated.
      // Register this member so that it gets marked live once an inheriting type is instantiated.
      inheritingTypes.forEach(t -> t.addPotentiallyLiveMember(member));
    }
  }

  private static void markTypeLive(Type type) {
    if (type.isLive()) {
      return;
    }

    type.markLive();

    // When a type is marked as live, we need to mark the super types as live too because their are
    // referred to in the class declaration (as supertypes or encoded as implementing interfaces).
    type.getSuperTypes().forEach(RapidTypeAnalyser::markTypeLive);
  }

  private static void instantiate(Type type) {
    if (type.isInstantiated()) {
      return;
    }
    type.instantiate();

    type.getPotentiallyLiveMembers().forEach(RapidTypeAnalyser::markMemberLive);
  }

  private static boolean containsInstantiatedTypes(Collection<Type> types) {
    return types.stream().anyMatch(Type::isInstantiated);
  }
}
