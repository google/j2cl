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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.libraryinfo.SourcePosition;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Wrapper Object containing the set of live types and live members discovered by the RTA algorithm.
 */
@AutoValue
abstract class RtaResult {
  abstract ImmutableList<String> getUnusedTypes();

  abstract ImmutableList<String> getUnusedMembers();

  abstract CodeRemovalInfo getCodeRemovalInfo();

  static RtaResult build(Collection<Type> types) {
    Builder builder = new AutoValue_RtaResult.Builder();
    CodeRemovalInfo.Builder codeRemovalInfoBuilder = CodeRemovalInfo.newBuilder();

    for (Type type : types) {
      if (type.isLive()) {
        ArrayList<LineRange> unusedLines = new ArrayList<>();
        for (Member member : type.getMembers()) {
          if (member.isLive()) {
            continue;
          }

          builder.unusedMembersBuilder().add(createMemberId(member));
          if (member.hasPosition()) {
            unusedLines.add(convertToLineRange(member.getPosition()));
          }
        }

        if (!unusedLines.isEmpty()) {
          unusedLines.sort((m1, m2) -> m1.getLineStart() - m2.getLineStart());
          codeRemovalInfoBuilder.addUnusedLines(
              UnusedLines.newBuilder()
                  .setFileKey(type.getImplSourceFile())
                  .addAllUnusedRanges(unusedLines)
                  .build());
        }

      } else {
        builder.unusedTypesBuilder().add(type.getName());
        codeRemovalInfoBuilder.addUnusedFiles(type.getHeaderSourceFile());
        codeRemovalInfoBuilder.addUnusedFiles(type.getImplSourceFile());
      }
    }

    return builder.setCodeRemovalInfo(codeRemovalInfoBuilder.build()).build();
  }

  private static LineRange convertToLineRange(SourcePosition position) {
    return LineRange.newBuilder()
        .setLineStart(position.getStart())
        .setLineEnd(position.getEnd())
        .build();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract ImmutableList.Builder<String> unusedTypesBuilder();

    abstract ImmutableList.Builder<String> unusedMembersBuilder();

    abstract Builder setCodeRemovalInfo(CodeRemovalInfo info);

    abstract RtaResult build();
  }

  private static String createMemberId(Member member) {
    return member.getDeclaringType().getName() + ":" + member.getName();
  }
}
