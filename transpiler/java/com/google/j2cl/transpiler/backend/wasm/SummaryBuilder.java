/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.backend.wasm;

import static java.util.function.Predicate.not;

import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.StringLiteralGettersCreator;
import com.google.j2cl.transpiler.ast.Type;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** Summarizes information where global knowledge will be required for bundling. */
public final class SummaryBuilder {

  public static final int NULL_TYPE = 0;

  private final Summary.Builder summary = Summary.newBuilder();
  private final Map<String, Integer> types = new HashMap<>();
  private final Map<String, Integer> declaredTypes = new HashMap<>();

  private final WasmGenerationEnvironment environment;

  SummaryBuilder(Library library, WasmGenerationEnvironment environment, Problems problems) {
    this.environment = environment;

    summaryTypeHierarchy(library);
    summarizeStringLiterals(library);
  }

  private void summaryTypeHierarchy(Library library) {
    library
        .streamTypes()
        .sorted(Comparator.comparing(t -> t.getDeclaration().getClassHierarchyDepth()))
        .forEach(this::addType);
  }

  private void addType(Type type) {
    if (type.isInterface()) {
      return;
    }

    int typeId = getDeclaredTypeId(type.getTypeDescriptor());

    TypeHierarchyInfo.Builder typeHierarchyInfoBuilder =
        TypeHierarchyInfo.newBuilder().setDeclaredTypeId(typeId);

    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor != null && !superTypeDescriptor.isNative()) {
      typeHierarchyInfoBuilder.setExtendsDeclaredTypeId(getDeclaredTypeId(superTypeDescriptor));
    }

    type.getSuperInterfaceTypeDescriptors().stream()
        .filter(not(DeclaredTypeDescriptor::isNative))
        .filter(not(DeclaredTypeDescriptor::isJsFunctionInterface))
        .forEach(t -> typeHierarchyInfoBuilder.addImplementsDeclaredTypeId(getDeclaredTypeId(t)));

    summary.addType(typeHierarchyInfoBuilder.build());
  }

  private int getDeclaredTypeId(DeclaredTypeDescriptor typeDescriptor) {
    String typeName = environment.getTypeSignature(typeDescriptor);
    // Note that the IDs start from '1' to reserve '0' for NULL_TYPE.
    return declaredTypes.computeIfAbsent(typeName, x -> declaredTypes.size() + 1);
  }

  private void summarizeStringLiterals(Library library) {
    // Replace stringliterals with the name of the literal getter method that will be synthesized
    // by the bundler.
    var stringLiteralGetterCreator = new StringLiteralGettersCreator();
    library.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteStringLiteral(StringLiteral stringLiteral) {
            return MethodCall.Builder.from(
                    stringLiteralGetterCreator.getOrCreateLiteralMethod(
                        getCurrentType(), stringLiteral, /* synthesizeMethod= */ false))
                .build();
          }
        });

    stringLiteralGetterCreator
        .getLiteralMethodByString()
        .forEach(
            (s, m) ->
                summary.addStringLiteral(
                    StringLiteralInfo.newBuilder()
                        .setLiteral(s)
                        .setEnclosingTypeName(
                            m.getEnclosingTypeDescriptor().getQualifiedBinaryName())
                        .setMethodName(m.getName())
                        .build()));
  }

  private Summary build() {
    summary.clearTypeReferenceMap();
    String[] typeReferenceMap = new String[types.size()];
    types.forEach((name, i) -> typeReferenceMap[i] = name);
    summary.addAllTypeReferenceMap(Arrays.asList(typeReferenceMap));
    String[] declaredTypeMap = new String[declaredTypes.size() + 1];
    // Add a spurious mapping for 0 for the cases where the type is absent, e.g. the supertype
    // of java.lang.Object.
    declaredTypeMap[NULL_TYPE] = "<no-type>";
    declaredTypes.forEach((name, i) -> declaredTypeMap[i] = name);
    summary.addAllDeclaredTypeMap(Arrays.asList(declaredTypeMap));
    return summary.build();
  }

  /** Serialize a LibraryInfo object into a JSON string. */
  @Nullable
  public String toJson(Problems problems) {
    try {
      return JsonFormat.printer().print(build());
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
      return null;
    }
  }

  public byte[] toByteArray() {
    return build().toByteArray();
  }
}
