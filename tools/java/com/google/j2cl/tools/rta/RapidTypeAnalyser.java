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
import com.google.common.collect.SetMultimap;
import com.google.j2cl.libraryinfo.InvocationKind;
import com.google.j2cl.libraryinfo.LibraryInfo;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

final class RapidTypeAnalyser {
  private final Set<Type> instantiatedTypes = new HashSet<>();
  private final Set<Type> unusedTypes;
  private final Set<Member> unusedMembers;
  private final SetMultimap<Type, Member> virtuallyLiveMemberPerType = HashMultimap.create();
  private final TypeHierarchyGraph typeHierarchyGraph;

  RapidTypeAnalyser(LibraryInfo libraryInfo) {
    typeHierarchyGraph = TypeHierarchyGraph.buildFrom(libraryInfo);
    unusedTypes = new HashSet<>(typeHierarchyGraph.getTypes());
    unusedMembers =
        typeHierarchyGraph.getTypes().stream()
            .flatMap(t -> t.getMembers().stream())
            .collect(Collectors.toSet());
  }

  RtaResult analyse() {
    // Go over the entry points to start the traversal.
    typeHierarchyGraph.getTypes().stream()
        .flatMap(t -> t.getMembers().stream())
        .filter(Member::isJsAccessible)
        .forEach(m -> onMemberReference(m.getDefaultInvocationKind(), m));

    return RtaResult.build(unusedTypes, unusedMembers);
  }

  private void onMemberReference(InvocationKind invocationKind, Member member) {
    switch (invocationKind) {
      case DYNAMIC:
        unfoldPolymorphicReference(member);
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

  private void onTypeReference(Type type) {
    markTypeLive(type);
  }

  private void markMemberLive(Member member) {
    if (!unusedMembers.remove(member)) {
      // already live
      return;
    }

    member.getReferencedTypes().forEach(this::onTypeReference);
    member.getReferencedMembers().forEach(this::onMemberReference);
  }

  private void unfoldPolymorphicReference(Member member) {
    // Set of types inheriting this member
    Set<Type> inheritingTypes = typeHierarchyGraph.getTypesInheriting(member);
    // TODO(b/112859205): Some static references ($clinit or static fields or unknown members) are
    // flagged as dynamic references. Remove inheritingTypes.isEmpty() when it solved.
    if (inheritingTypes.isEmpty() || containsLiveTypes(inheritingTypes)) {
      markMemberLive(member);
    } else {
      // none of the possible types for this member is live. Register this member,
      // so once one of the type become live, the member will be added to the live member
      // graph.
      inheritingTypes.forEach(c -> virtuallyLiveMemberPerType.put(c, member));
    }

    // Recursively unfold the overriding chain.
    for (Type overridingType : typeHierarchyGraph.getTypesOverriding(member)) {
      Member newTargetMember = overridingType.getMemberByName(member.getName());
      unfoldPolymorphicReference(newTargetMember);
    }
  }

  private void markTypeLive(Type type) {
    if (!unusedTypes.remove(type)) {
      // already live
      return;
    }

    // When a type is marked as live, we need to mark the super types as live too because their are
    // referred to in the class declaration (as supertypes or encoded as implementing interfaces).
    type.getSuperTypes().forEach(this::markTypeLive);
  }

  private void instantiate(Type type) {
    if (!instantiatedTypes.add(type)) {
      // Type is already live
      return;
    }
    virtuallyLiveMemberPerType.get(type).forEach(this::markMemberLive);
  }

  private boolean containsLiveTypes(Set<Type> types) {
    return types.stream().anyMatch(instantiatedTypes::contains);
  }
}
