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
import com.google.j2cl.libraryinfo.LibraryInfo;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class RapidTypeAnalyser {

  static RtaResult analyse(List<LibraryInfo> libraryInfos) {
    List<Type> types = TypeGraphBuilder.build(libraryInfos);

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
        .filter(Type::isLive)
        .flatMap(t -> t.getMembers().stream())
        .filter(not(Member::isLive))
        .collect(toSet());
  }

  private static void onMemberReference(InvocationKind invocationKind, Member member) {
    switch (invocationKind) {
      case DYNAMIC:
        traversePolymorphicReference(member);
        break;
      case INSTANTIATION:
        instantiate(member.getDeclaringType());
        // Fall through.
      case STATIC:
        markTypeLive(member.getDeclaringType());
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
    markOverriddenMembersLive(member);
  }

  private static void traversePolymorphicReference(Member member) {
    if (member.isFullyTraversed()) {
      return;
    }
    member.markFullyTraversed();

    markMemberPotentiallyLive(member);

    // Recursively unfold the overriding chain.
    for (Type overridingType : member.getOverridingTypes()) {
      overridingType
          .getMemberByName(member.getName())
          .ifPresent(RapidTypeAnalyser::traversePolymorphicReference);
    }
  }

  private static void markOverriddenMembersLive(Member member) {
    if (!member.isPolymorphic()) {
      return;
    }

    Type type = member.getDeclaringType();
    for (Optional<Type> superClass = type.getSuperClass();
        superClass.isPresent();
        superClass = superClass.get().getSuperClass()) {
      Optional<Member> overriddenMember = superClass.get().getMemberByName(member.getName());
      if (!overriddenMember.isPresent()) {
        continue;
      }
      markMemberLive(overriddenMember.get());
      return;
    }
  }

  private static void markMemberPotentiallyLive(Member member) {
    Type declaringType = member.getDeclaringType();
    if (declaringType.isInstantiated()) {
      markMemberLive(member);
    } else {
      // Type is not instantiated, defer making it live until the type is instantiated.
      declaringType.addPotentiallyLiveMember(member);
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
    // constructors are implicitly live when the type is instantiated.
    type.getConstructor().ifPresent(RapidTypeAnalyser::markMemberLive);

    // Instantiate supertypes.
    type.getSuperClass().ifPresent(RapidTypeAnalyser::instantiate);

    type.getPotentiallyLiveMembers().forEach(RapidTypeAnalyser::markMemberLive);
  }

  private RapidTypeAnalyser() {}
}
