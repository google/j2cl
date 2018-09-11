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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import com.google.j2cl.libraryinfo.InvocationKind;
import com.google.j2cl.libraryinfo.LibraryInfo;
import java.util.Collection;
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
    for (Type type : typeHierarchyGraph.getTypes()) {
      for (Member member : type.getMembers()) {
        if (member.isEntryPoint()) {
          if (member.isConstructor()) {
            onInstantiation(member);
          } else {
            onReference(member);
          }
        }
      }
    }

    return RtaResult.build(unusedTypes, unusedMembers);
  }

  private void onInstantiation(Member ctor) {
    checkState(ctor.isConstructor());

    instantiate(ctor.getDeclaringType());

    onReference(ctor);
  }

  private void onReference(Member member) {
    if (!unusedMembers.remove(member)) {
      // already live
      return;
    }

    markTypesAsLive(member.getReferencedTypes());

    // static references
    member.getReferencedMembers().get(InvocationKind.INSTANTIATION).forEach(this::onInstantiation);

    member.getReferencedMembers().get(InvocationKind.STATIC).forEach(this::onReference);

    // dynamic references
    getAllTargetsOfPolymorphicReferences(member).forEach(this::markPolymorphicMemberLive);
  }

  private void markPolymorphicMemberLive(Member member) {
    // Set of types inheriting this member
    Set<Type> inheritingTypes = typeHierarchyGraph.getTypesInheriting(member);
    // TODO(b/112859205): Some static references ($clinit or static fields or unknown members) are
    // flagged as dynamic references. Remove inheritingTypes.isEmpty() when it solved.
    if (inheritingTypes.isEmpty() || containsLiveTypes(inheritingTypes)) {
      onReference(member);
    } else {
      // none of the possible types for this member is live. Register this member,
      // so once one of the type become live, the member will be added to the live member
      // graph.
      inheritingTypes.forEach(c -> virtuallyLiveMemberPerType.put(c, member));
    }
  }

  private void markTypesAsLive(Collection<Type> typesList) {
    typesList.forEach(this::markTypeAsLive);
  }

  private void markTypeAsLive(Type type) {
    if (!unusedTypes.remove(type)) {
      // already live
      return;
    }

    // When a type is marked as live, we need to mark the super types as live too because their are
    // referred to in the class declaration (as supertypes or encoded as implementing interfaces).
    markTypesAsLive(type.getSuperTypes());
  }

  private Collection<Member> getAllTargetsOfPolymorphicReferences(Member member) {

    Collection<Member> dynamicsReferencedMembers =
        member.getReferencedMembers().get(InvocationKind.DYNAMIC);

    ImmutableList.Builder<Member> allPossibleReferencedMembers = ImmutableList.builder();

    dynamicsReferencedMembers.forEach(m -> getAllPossibleMembers(m, allPossibleReferencedMembers));

    return allPossibleReferencedMembers.build();
  }

  private void getAllPossibleMembers(
      Member targetMember, ImmutableList.Builder<Member> listBuilder) {
    listBuilder.add(targetMember);

    for (Type overridingType : typeHierarchyGraph.getTypesOverriding(targetMember)) {
      Member newTargetMember = overridingType.getMemberByName(targetMember.getName());
      getAllPossibleMembers(newTargetMember, listBuilder);
    }
  }

  private void instantiate(Type type) {
    if (!instantiatedTypes.add(type)) {
      // Type is already live
      return;
    }
    virtuallyLiveMemberPerType.get(type).forEach(this::onReference);

    // Also consider each instance member that is accessible from javascript as referenced.
    type.getMembers().stream().filter(RapidTypeAnalyser::isJsAccessible).forEach(this::onReference);
  }

  private static boolean isJsAccessible(Member member) {
    return member.isJsAccessible()
        && member.isInstanceMember()
        // TODO(b/114126660): don't flag LambdaAdaptorType members as JsAccessible in libraryinfo
        && !member.getDeclaringType().isLambdaAdaptorType();
  }

  private boolean containsLiveTypes(Set<Type> types) {
    return types.stream().anyMatch(instantiatedTypes::contains);
  }
}
