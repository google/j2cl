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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Wrapper Object containing the set of live types and live members discovered by the RTA algorithm.
 */
@AutoValue
abstract class RtaResult {
  abstract ImmutableList<String> getUnusedTypes();

  abstract ImmutableList<String> getUnusedMembers();

  abstract ImmutableList<String> getUnusedFiles();

  static RtaResult build(Set<Type> unusedTypeSet, Set<Member> unusedMemberSet) {
    return new AutoValue_RtaResult.Builder()
        .setUnusedTypes(toSortedList(unusedTypeSet.stream().map(Type::getName)))
        .setUnusedFiles(
            toSortedList(
                unusedTypeSet.stream()
                    .flatMap(t -> Stream.of(t.getHeaderSourceFile(), t.getImplSourceFile()))))
        .setUnusedMembers(toSortedList(unusedMemberSet.stream().map(RtaResult::createMemberId)))
        .build();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder setUnusedTypes(List<String> unusedTypes);

    abstract Builder setUnusedMembers(List<String> unusedMembers);

    abstract Builder setUnusedFiles(List<String> unusedFiles);

    abstract RtaResult build();
  }

  private static String createMemberId(Member member) {
    return member.getDeclaringType().getName() + ":" + member.getName();
  }

  private static <E> List<E> toSortedList(Stream<E> stream) {
    return stream.sorted().collect(toImmutableList());
  }
}
