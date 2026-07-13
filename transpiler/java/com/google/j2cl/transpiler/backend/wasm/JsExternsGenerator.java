/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.backend.wasm;

import static com.google.common.collect.MoreCollectors.toOptional;
import static com.google.j2cl.transpiler.ast.AstUtils.findSuperTypeWithWasmJsExportsIncludingSelf;
import static com.google.j2cl.transpiler.ast.AstUtils.isWasmJsExportedType;

import com.google.common.collect.Streams;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.JsMemberType;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Generates JavaScript externs for allowing JavaScript callers to use exported JsTypes.
 *
 * <p>This is to provide typing information, for types and functions, for the exported JsTypes to
 * JavaScript callers. The actual concrete types are created at runtime by Wasm.
 */
final class JsExternsGenerator {

  private static final String OUTPUT_PATH = "externs";

  private final JsTypeNameResolver closureEnvironment = new JsTypeNameResolver();
  private final WasmGenerationEnvironment environment;
  private final Output output;

  private JsExternsGenerator(Output output, WasmGenerationEnvironment environment) {
    this.environment = environment;
    this.output = output;
  }

  /** Generates the JavaScript code to support the imports. */
  public static void generateOutputs(
      Output output, WasmGenerationEnvironment environment, Library library) {
    JsExternsGenerator externsGenerator = new JsExternsGenerator(output, environment);
    externsGenerator.generateExterns(library);
  }

  private void generateExterns(Library library) {
    if (!environment.isCustomDescriptorsJsInteropEnabled()) {
      return;
    }
    library
        .streamTypes()
        .filter(t -> shouldGenerateExtern(t.getTypeDescriptor()))
        .forEach(
            type -> {
              generateExtern(type);
              generateExternsWiring(type);
            });
  }

  static DeclaredTypeDescriptor findExportedSuperType(Type type) {
    return findSuperTypeWithWasmJsExportsIncludingSelf(type.getSuperTypeDescriptor());
  }

  static boolean shouldGenerateExtern(DeclaredTypeDescriptor typeDescriptor) {
    // Generate externs if this type or a supertype is visible to JS.
    return isWasmJsExportedType(typeDescriptor);
  }

  private void generateExtern(Type type) {
    SourceBuilder sb = new SourceBuilder();
    sb.appendln("/** @externs */");

    appendConstructor(sb, type);
    appendFields(sb, type);
    appendMethods(sb, type);

    // Output to externs/my.package.MyClass.externs.java.js
    output.write(
        Path.of(OUTPUT_PATH, type.getDeclaration().getQualifiedJsName() + ".externs.java.js")
            .toString(),
        sb.build());
  }

  /** Appends the constructor extern for the given type. */
  private void appendConstructor(SourceBuilder sb, Type type) {
    String jsDoc = closureEnvironment.getJsDocForType(type, /* isWasmExtern= */ true);

    Method factoryMethod = getExportedConstructor(type);

    sb.appendln("");
    if (factoryMethod == null) {
      // If no constructor, we still have to emit a constructor function so that type JSDoc
      // annotations and members can be added.
      sb.appendln("/**");
      appendJsDoc(sb, jsDoc);
      if (!type.isInterface()) {
        sb.appendln(" * @constructor");
      }
      sb.appendln(" */");
      sb.appendln(
          String.format(
              "var %s = function() {};", closureEnvironment.aliasForType(type.getDeclaration())));
      return;
    }

    sb.appendln("/**");
    appendJsDoc(sb, jsDoc);
    if (!type.isInterface()) {
      sb.appendln(" * @constructor");
    }
    sb.appendln(" */");
    sb.append(
        String.format("var %s = function", closureEnvironment.aliasForType(type.getDeclaration())));
    closureEnvironment.emitParameters(sb, factoryMethod);
    sb.appendln("{};");
  }

  private void appendFields(SourceBuilder sb, Type type) {
    streamExportedFields(type)
        .forEach(
            fieldDescriptor -> {
              sb.appendln("");
              sb.append("/** ");
              sb.append(closureEnvironment.getJsDocForField(fieldDescriptor, /* isPublic= */ true));
              sb.appendln(" */");
              sb.appendln(
                  String.format(
                      "%s.%s;",
                      getMemberOwner(fieldDescriptor), fieldDescriptor.getSimpleJsName()));
            });
  }

  private static class GetterSetterPair {
    private Method getter = null;
    private Method setter = null;

    /**
     * Returns a field descriptor representing the getter/setter pair which can be used to generate
     * the extern as if it were a field.
     */
    FieldDescriptor asFieldDescriptor() {
      MethodDescriptor primary = getter != null ? getter.getDescriptor() : setter.getDescriptor();
      TypeDescriptor typeDescriptor =
          getter != null
              ? getter.getDescriptor().getReturnTypeDescriptor()
              : setter.getDescriptor().getParameterTypeDescriptors().get(0);
      return FieldDescriptor.builder()
          .setEnclosingTypeDescriptor(primary.getEnclosingTypeDescriptor())
          .setName(primary.getSimpleJsName())
          .setTypeDescriptor(typeDescriptor)
          .setStatic(primary.isStatic())
          // If there is no setter, mark as compile time constant. It's not technically a compile
          // time constant, but this is needed to emit @const which is used for constants and
          // read-only properties.
          .setCompileTimeConstant(setter == null)
          .setOriginalJsInfo(
              primary.getJsInfo().toBuilder()
                  .setJsName(primary.getSimpleJsName())
                  .setJsMemberType(JsMemberType.PROPERTY)
                  .build())
          .build();
    }
  }

  private static Stream<FieldDescriptor> streamExportedFields(Type type) {
    // Collect fields from getter/setter methods.
    var getterSetters = new LinkedHashMap<String, GetterSetterPair>();
    streamExportedMethods(type)
        .forEach(
            method -> {
              if (!method.getDescriptor().isJsProperty()) {
                return;
              }

              GetterSetterPair getterSetterPair =
                  getterSetters.computeIfAbsent(
                      method.getDescriptor().getSimpleJsName(), k -> new GetterSetterPair());
              if (method.getDescriptor().isJsPropertyGetter()) {
                getterSetterPair.getter = method;
              } else if (method.getDescriptor().isJsPropertySetter()) {
                getterSetterPair.setter = method;
              }
            });

    // Collect JsProperty-annotated fields.
    Stream<FieldDescriptor> fieldDescriptors =
        getterSetters.values().stream().map(GetterSetterPair::asFieldDescriptor);
    fieldDescriptors =
        Stream.concat(
            fieldDescriptors,
            type.getDeclaration().getDeclaredFieldDescriptors().stream()
                .filter(AstUtils::needsWasmJsExport));
    return fieldDescriptors;
  }

  private void appendMethods(SourceBuilder sb, Type type) {
    streamExportedMethods(type)
        .forEach(
            method -> {
              if (!method.getDescriptor().isJsMethod()) {
                return;
              }

              sb.appendln("");
              sb.appendln("/**");
              String jsDoc = closureEnvironment.getJsDocForMethod(getMethodForJsDoc(type, method));
              appendJsDoc(sb, jsDoc);
              sb.appendln(" */");
              sb.append(
                  String.format(
                      "%s.%s = function",
                      getMemberOwner(method.getDescriptor()),
                      method.getDescriptor().getSimpleJsName()));
              closureEnvironment.emitParameters(sb, method);
              sb.appendln("{};");
            });
  }

  private Method getMethodForJsDoc(Type type, Method method) {
    // We only generate externs for the first time a method is encountered in the inheritance chain,
    // and should not be emitted with @abstract.
    if (!type.isInterface() && method.isAbstract()) {
      return method.toBuilder()
          .setMethodDescriptor(method.getDescriptor().toBuilder().setAbstract(false).build())
          .build();
    }
    return method;
  }

  /**
   * Streams all exported methods for the given type, including js methods, js properties, and
   * accidental overrides.
   */
  private static Stream<Method> streamExportedMethods(Type type) {
    return Streams.concat(
        type.getMethods().stream().filter(m -> AstUtils.needsWasmJsExport(m.getDescriptor())),
        type.getTypeDescriptor().getAccidentalOverrides().stream()
            .filter(AstUtils::needsWasmJsExport)
            .map(
                accidentalOverride ->
                    Method.builder()
                        .setMethodDescriptor(accidentalOverride)
                        .setParameters(
                            AstUtils.createParameterVariables(
                                accidentalOverride.getParameterTypeDescriptors()))
                        .setSourcePosition(type.getSourcePosition())
                        .build()));
  }

  /** Returns the exported constructor for the given type, or null if there is none. */
  @Nullable
  private static Method getExportedConstructor(Type type) {
    return type.getMethods().stream()
        .filter(
            m ->
                m.getDescriptor().getOrigin()
                        == MethodDescriptor.MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR
                    && AstUtils.needsWasmJsExport(m.getDescriptor()))
        .collect(toOptional())
        .orElse(null);
  }

  private String getMemberOwner(MemberDescriptor memberDescriptor) {
    return closureEnvironment.aliasForType(memberDescriptor.getEnclosingTypeDescriptor())
        + (memberDescriptor.isStatic() ? "" : ".prototype");
  }

  /** Renders a JsDoc clause. The provided jsdoc can be single line or multi-line. */
  private void appendJsDoc(SourceBuilder sb, String jsDoc) {
    if (jsDoc.isEmpty()) {
      return;
    }
    if (jsDoc.endsWith("\n")) {
      sb.append(jsDoc);
    } else {
      // Single line jsdoc - turn it into a multi-line jsdoc by prepending a "*" and appending a
      // newline.
      sb.appendln(" *" + jsDoc);
    }
  }

  private void generateExternsWiring(Type type) {
    SourceBuilder sb = new SourceBuilder();
    sb.appendln(String.format("goog.module('%s');", type.getDeclaration().getModuleName()));
    sb.appendln("");

    String externName = closureEnvironment.aliasForType(type.getDeclaration());
    String simpleJsName = type.getDeclaration().getSimpleJsName();
    if (type.isInterface()) {
      generateTypeAlias(sb, simpleJsName, externName);
    } else {
      generateConstructorProxy(
          sb, externName, simpleJsName, type.getDeclaration().getQualifiedJsName());
    }

    sb.appendln(String.format("exports = %s;", simpleJsName));

    // Output to externs/my.package.MyClass.java.js
    output.write(
        Path.of(OUTPUT_PATH, type.getDeclaration().getQualifiedJsName() + ".java.js").toString(),
        sb.build());
  }

  private static void generateConstructorProxy(
      SourceBuilder sb, String externName, String simpleJsName, String qualifiedJsName) {
    sb.appendln("const {constructorProxy} = goog.require('j2wasm.JsInteropRuntime');");
    sb.appendln("");
    sb.appendln(String.format("/** @const {typeof %s} */", externName));
    sb.appendln(String.format("const %s = constructorProxy('%s');", simpleJsName, qualifiedJsName));
    sb.appendln("");
  }

  private static void generateTypeAlias(SourceBuilder sb, String simpleJsName, String externName) {
    sb.appendln(String.format("/** @typedef {%s} */", externName));
    sb.appendln(String.format("let %s;", simpleJsName));
    sb.appendln("");
  }
}
