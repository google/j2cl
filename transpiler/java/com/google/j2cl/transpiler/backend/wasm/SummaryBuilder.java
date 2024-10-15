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

import static com.google.j2cl.common.StringUtils.escapeAsWtf16;
import static java.util.function.Predicate.not;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.StringLiteralGettersCreator;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** Summarizes information where global knowledge will be required for bundling. */
public final class SummaryBuilder {

  public static final int NULL_TYPE = 0;

  private final Summary.Builder summary = Summary.newBuilder();
  private final Map<String, Integer> typeIdByTypeName = new HashMap<>();
  private final WasmGenerationEnvironment environment;

  SummaryBuilder(Library library, WasmGenerationEnvironment environment, Problems problems) {
    this.environment = environment;

    summaryTypeHierarchy(library);
    summarizeSystemGetPropertyCalls(library);
    summarizeStringLiterals(library);
  }

  void addSharedTypeSnippet(String key, String snippet) {
    summary.addTypeSnippets(SharedSnippet.newBuilder().setKey(key).setSnippet(snippet));
  }

  void addSharedGlobalSnippet(String key, String snippet) {
    summary.addGlobalSnippets(SharedSnippet.newBuilder().setKey(key).setSnippet(snippet));
  }

  void addSharedWasmImportSnippet(String key, String snippet) {
    summary.addWasmImportSnippets(SharedSnippet.newBuilder().setKey(key).setSnippet(snippet));
  }

  void addSharedJsImportSnippet(String key, String snippet) {
    summary.addJsImportSnippets(SharedSnippet.newBuilder().setKey(key).setSnippet(snippet));
  }

  void addSharedJsImportRequireSnippet(String snippet) {
    summary.addJsImportRequires(snippet);
  }

  private void summaryTypeHierarchy(Library library) {
    library
        .streamTypes()
        .sorted(Comparator.comparing(t -> t.getDeclaration().getTypeHierarchyDepth()))
        .forEach(this::addType);
  }

  private void addType(Type type) {
    if (type.isNative()
        || type.isOverlayImplementation()
        || type.getDeclaration().getWasmInfo() != null) {
      // none of these types have generated vtables so they need to be ignored in the summary.
      return;
    }

    int typeId = getTypeId(type.getTypeDescriptor());

    TypeInfo.Builder typeHierarchyInfoBuilder =
        TypeInfo.newBuilder().setTypeId(typeId).setAbstract(type.getDeclaration().isAbstract());

    TypeDeclaration superTypeDeclaration =
        WasmGenerationEnvironment.getTypeLayoutSuperTypeDeclaration(type.getDeclaration());
    if (superTypeDeclaration != null && !superTypeDeclaration.isNative()) {
      typeHierarchyInfoBuilder.setExtendsType(getTypeId(superTypeDeclaration.toDescriptor()));
    }

    if (type.isInterface()) {
      summary.addInterfaces(typeHierarchyInfoBuilder.build());
    } else {
      type.getDeclaration().getAllSuperInterfaces().stream()
          .filter(not(TypeDeclaration::isNative))
          .forEach(t -> typeHierarchyInfoBuilder.addImplementsTypes(getTypeId(t.toDescriptor())));

      summary.addTypes(typeHierarchyInfoBuilder.build());
    }
  }

  private int getTypeId(DeclaredTypeDescriptor typeDescriptor) {
    String typeName = environment.getTypeSignature(typeDescriptor);
    // Note that the IDs start from '1' to reserve '0' for NULL_TYPE.
    return typeIdByTypeName.computeIfAbsent(typeName, x -> typeIdByTypeName.size() + 1);
  }

  private void summarizeSystemGetPropertyCalls(Library library) {
    Set<String> allProperties = new LinkedHashSet<>();
    Set<String> requiredProperties = new LinkedHashSet<>();
    library.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethodCall(MethodCall methodCall) {
            MethodDescriptor target = methodCall.getTarget();
            String name = target.getName();
            if (target.getOrigin().isSystemGetPropertyGetter()) {
              allProperties.add(name);
            }
            if (target.getOrigin().isRequiredSystemGetPropertyGetter()) {
              requiredProperties.add(name);
            }
          }
        });
    summary.addAllSystemProperties(
        allProperties.stream()
            .map(
                pk ->
                    SystemPropertyInfo.newBuilder()
                        .setPropertyKey(pk)
                        .setIsRequired(requiredProperties.contains(pk))
                        .build())
            .collect(ImmutableList.toImmutableList()));
  }

  private void summarizeStringLiterals(Library library) {
    // Replace string literals with the name of the literal getter method that will be synthesized
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
                summary.addStringLiterals(
                    StringLiteralInfo.newBuilder()
                        .setContent(escapeAsWtf16(s))
                        .setEnclosingTypeName(
                            m.getEnclosingTypeDescriptor().getQualifiedSourceName())
                        .setMethodName(m.getName())
                        .build()));
  }

  private Summary build() {
    summary.clearTypeNames();
    String[] typeNames = new String[typeIdByTypeName.size() + 1];
    // Add a spurious mapping for 0 for the cases where the type is absent, e.g. the supertype
    // of java.lang.Object.
    typeNames[NULL_TYPE] = "<no-type>";
    typeIdByTypeName.forEach((name, i) -> typeNames[i] = name);
    summary.addAllTypeNames(Arrays.asList(typeNames));
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
