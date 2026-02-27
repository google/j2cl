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

import static com.google.j2cl.transpiler.backend.wasm.WasmGenerationEnvironment.findSuperTypeWithJsPrototype;

import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.nio.file.Path;

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

  private boolean shouldGenerateExtern(DeclaredTypeDescriptor typeDescriptor) {
    // TODO(b/459918329): Support interfaces.
    if (typeDescriptor.isInterface()) {
      return false;
    }

    // Generate externs if the type has a JS prototype or if the type has a supertype with a JS
    // prototype. In practice, because j.l.Object has a JS prototype, this nearly all types (some
    // exclusions such as interfaces and native types)
    return findSuperTypeWithJsPrototype(typeDescriptor.getTypeDeclaration()) != null;
  }

  private void generateExtern(Type type) {
    SourceBuilder sb = new SourceBuilder();
    sb.appendln("/** @externs */");

    appendConstructor(sb, type);
    appendMethods(sb, type);

    // Output to externs/my.package.MyClass.externs.js
    output.write(
        Path.of(OUTPUT_PATH, type.getDeclaration().getQualifiedJsName() + ".externs.js").toString(),
        sb.build());
  }

  /** Appends the constructor extern for the given type. */
  private void appendConstructor(SourceBuilder sb, Type type) {
    String jsDoc = closureEnvironment.getJsDocForType(type, /* isWasmExtern= */ true);
    Method constructor =
        type.getMethods().stream()
            .filter(m -> isConstructor(m.getDescriptor()))
            .findFirst()
            .orElse(null);

    sb.appendln("");
    if (constructor == null) {
      // If no constructor, we still have to emit a constructor function so that type JSDoc
      // annotations and members can be added.
      sb.appendln("/**");
      appendJsDoc(sb, jsDoc);
      sb.appendln(" * @constructor");
      sb.appendln(" */");
      sb.appendln(
          String.format(
              "var %s = function() {};", closureEnvironment.aliasForType(type.getDeclaration())));
      return;
    }

    sb.appendln("/**");
    appendJsDoc(sb, jsDoc);
    sb.appendln(" * @constructor");
    sb.appendln(" */");
    sb.append(
        String.format("%s = function", closureEnvironment.aliasForType(type.getDeclaration())));
    closureEnvironment.emitParameters(sb, constructor);
    sb.appendln(" {};");
  }

  private void appendMethods(SourceBuilder sb, Type type) {
    for (Method method : type.getMethods()) {
      MethodDescriptor methodDescriptor = method.getDescriptor();
      if (!AstUtils.canBeReferencedExternallyWasm(methodDescriptor)
          // Exclude generated export bridges. They do not have enough information, such as what
          // they @Override, to generate the extern.
          || methodDescriptor.getOrigin().isWasmJsExport()
          || methodDescriptor.getOrigin().isWasmJsConstructorExport()
          // Constructors handled elsewhere.
          || isConstructor(methodDescriptor)) {
        continue;
      }

      sb.appendln("");
      sb.appendln("/**");
      appendJsDoc(sb, closureEnvironment.getJsDocForMethod(method));
      sb.appendln(" */");
      sb.append(
          String.format(
              "%s.%s = function",
              getMemberOwner(methodDescriptor), methodDescriptor.getSimpleJsName()));
      closureEnvironment.emitParameters(sb, method);
      sb.appendln(" {};");
    }
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

  private static boolean isConstructor(MethodDescriptor methodDescriptor) {
    return methodDescriptor.getOrigin()
        == MethodDescriptor.MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR;
  }

  private void generateExternsWiring(Type type) {
    SourceBuilder sb = new SourceBuilder();
    sb.appendln(String.format("goog.module('%s');", type.getDeclaration().getModuleName()));
    sb.appendln("");
    sb.appendln("const {constructorProxy} = goog.require('j2wasm.JsInteropRuntime');");
    sb.appendln("");
    sb.appendln(
        String.format(
            "exports = /** @type {typeof %s} */ constructorProxy('%s');",
            closureEnvironment.aliasForType(type.getDeclaration()),
            type.getDeclaration().getQualifiedJsName()));

    // Output to externs/my.package.MyClass.js
    output.write(
        Path.of(OUTPUT_PATH, type.getDeclaration().getQualifiedJsName() + ".js").toString(),
        sb.build());
  }
}
