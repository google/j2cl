/*
 * Copyright 2023 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.j2cl.transpiler.backend.wasm.WasmGenerationEnvironment.getWasmInfo;
import static java.lang.String.format;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Generates a JavaScript imports mapping for the Wasm module. */
public final class JsImportsGenerator {

  /** Top-level module name in the imports map containing all generated imports. */
  public static final String MODULE = "imports";

  /** Represents the JavaScript imports for a Wasm module. */
  @AutoValue
  public abstract static class Imports {
    public abstract ImmutableMap<MethodDescriptor, JsMethodImport> getMethodImports();

    public abstract ImmutableSet<String> getModuleImports();

    public static Imports create(
        ImmutableMap<MethodDescriptor, JsMethodImport> methodImports,
        ImmutableSet<String> moduleImports) {
      return new AutoValue_JsImportsGenerator_Imports(methodImports, moduleImports);
    }
  }

  /** Collects JavaScript imports that are referenced in the library. */
  public static Imports collectImports(Library library) {
    ImmutableSet.Builder<String> moduleImports = ImmutableSet.builder();
    ImmutableMap.Builder<MethodDescriptor, JsMethodImport> methodImports = ImmutableMap.builder();
    library.accept(new ImportCollector(methodImports, moduleImports));
    return Imports.create(methodImports.buildOrThrow(), moduleImports.build());
  }

  /** Generates the JavaScript code to support the imports. */
  public static void generateOutputs(Output output, Imports imports, boolean enableJsInterop) {
    output.write(
        "imports.txt",
        generateOutputs(
            imports.getModuleImports(), collectImportSnippets(imports), enableJsInterop));
  }

  /** Generates the JavaScript code to support the imports. */
  public static String generateOutputs(
      Collection<String> requiredModules,
      Map<String, String> methodImports,
      boolean enableJsInterop) {
    SourceBuilder builder = new SourceBuilder();
    emitRequires(builder, requiredModules);
    emitImportsFunction(builder, methodImports, enableJsInterop);
    builder.newLine(); // Ends in a new line for human readability.
    return builder.build();
  }

  /** Collects the import snippets indexed by their keys. */
  static Map<String, String> collectImportSnippets(Imports imports) {
    return imports.getMethodImports().values().stream()
        .collect(
            toImmutableMap(
                JsMethodImport::getImportKey,
                JsImportsGenerator::createImportBody,
                // Distinct native methods can produce the same import key and body (e.g.,
                // overloads with different parameter types); ensure they produce same import body.
                (b1, b2) -> {
                  checkState(b1.equals(b2), "Conflicting import bodies: %s vs %s", b1, b2);
                  return b1;
                }));
  }

  private static void emitRequires(SourceBuilder builder, Collection<String> requiredModules) {
    requiredModules.stream()
        .sorted()
        .forEach(
            i -> {
              builder.append(createGoogRequire(i));
              builder.newLine();
            });
  }

  private static String createGoogRequire(String importedModule) {
    return format(
        "const %s = goog.require('%s');",
        WasmJsInteropClosureGenerationEnvironment.computeJsAlias(importedModule), importedModule);
  }

  /**
   * Emits the function returning the import object. The import object is a nested {@code Object} in
   * the form:
   *
   * <pre>{@code
   * return {
   *   'imports': {
   *     'functionName': () => functionName(),
   *   }
   * }
   * }</pre>
   */
  private static void emitImportsFunction(
      SourceBuilder builder, Map<String, String> methodImports, boolean enableJsInterop) {
    builder.newLine();
    builder.append("/** @return {!Object<string, *>} Wasm import object */");
    builder.newLine();
    builder.append("function getImports() ");
    builder.openBrace();
    builder.newLine();
    builder.append("return ");
    builder.openBrace();
    if (enableJsInterop) {
      builder.newLine();
      builder.append("'env': ");
      builder.openBrace();
      builder.newLine();
      // Emit the JS constructors list. This is populated by Wasm to include all the JS
      // constructors. The variable `jsConstructors` must be defined by the outer
      // j2wasm_application.
      builder.append("'constructors': jsConstructors,");
      builder.closeBrace();
      builder.append(",");
      emitJsPrototypeFactory(builder);
    }
    builder.newLine();
    // Add WebAssembly module. This is needed because the import is hardcoded in
    // `generateWasmModule` and there is no corresponding code in the stdlib.
    // TODO(b/277970998): Consider how to handle this.
    builder.append("'WebAssembly': WebAssembly,");
    builder.newLine();
    builder.append(format("'%s': ", MODULE));
    builder.openBrace();
    methodImports.entrySet().stream()
        .sorted(Entry.comparingByKey())
        .forEach(i -> {
          builder.newLine();
          builder.append(format("'%s': %s,", i.getKey(), i.getValue()));
        });
    builder.closeBrace();
    builder.closeBrace();
    builder.append(";");
    builder.closeBrace();
  }

  /**
   * Emits the JS prototype factory for JsInterop.
   *
   * <p>The prototype factory returns an empty object whenever any property is accessed. Wasm uses
   * this object as the JavaScript prototype and populates it with the exported methods.
   */
  private static void emitJsPrototypeFactory(SourceBuilder builder) {
    builder.newLine();
    builder.appendLines(
        "'prototypes': new Proxy({}, {",
        "    get(target, prop, receiver) {",
        "        return {};",
        "    }",
        "}),");
  }

  private static String createImportBody(JsMethodImport methodImport) {
    MethodDescriptor method = methodImport.getMethodDescriptor();
    String target = getTarget(methodImport);

    if (methodImport.emitAsMethodReference()) {
      return target;
    }
    if (method.isPropertyGetter()) {
      if (TypeDescriptors.isPrimitiveVoid(method.getReturnTypeDescriptor())) {
        // A void getter is only evaluated for its side effects, so emit it as a statement. This
        // also supports getters that are statements in JavaScript, e.g. `debugger`.
        return format("(%s) => { %s; }", formatInstanceParam(methodImport), target);
      }
      return format("(%s) => %s", formatInstanceParam(methodImport), target);
    }
    if (method.isPropertySetter()) {
      String instanceParam = formatInstanceParam(methodImport);
      String params = "/** ? */ $value";
      if (!instanceParam.isEmpty()) {
        params = instanceParam + ", " + params;
      }
      return format("(%s) => { %s = $value; }", params, target);
    }
    return synthesizeCallBody(target, formatInstanceParam(methodImport), method);
  }

  private static String getTarget(JsMethodImport methodImport) {
    MethodDescriptor method = methodImport.getMethodDescriptor();
    if (method.isConstructor()) {
      return "new " + methodImport.getJsQualifier();
    }
    String qualifier = method.isInstanceMember() ? "$instance" : methodImport.getJsQualifier();
    return AstUtils.buildQualifiedName(qualifier, methodImport.getJsName());
  }

  private static String synthesizeCallBody(
      String target, String instanceParam, MethodDescriptor method) {
    List<String> params = new ArrayList<>();
    List<String> args = new ArrayList<>();
    if (!instanceParam.isEmpty()) {
      params.add(instanceParam);
    }
    int fixedParameterCount =
        method.getParameterDescriptors().size() - (method.isVarargs() ? 1 : 0);
    for (int i = 0; i < fixedParameterCount; i++) {
      String paramName = "$p" + i;
      params.add("/** ? */ " + paramName);
      args.add(paramName);
    }
    if (method.isVarargs()) {
      params.add("/** !Array<?> */ $varargs");
      args.add("...$varargs");
    }
    return format("(%s) => %s(%s)", String.join(", ", params), target, String.join(", ", args));
  }

  private static String formatInstanceParam(JsMethodImport methodImport) {
    if (!methodImport.getMethodDescriptor().isInstanceMember()) {
      return "";
    }
    String qualifier = methodImport.getJsQualifier();
    String closureType =
        switch (qualifier) {
          case "?", "*", "string", "number", "boolean" -> qualifier;
          default -> "!" + qualifier;
        };
    return format("/** %s */ $instance", closureType);
  }

  private static class ImportCollector extends AbstractVisitor {
    private final ImmutableMap.Builder<MethodDescriptor, JsMethodImport> methodImports;
    private final ImmutableSet.Builder<String> moduleImports;

    ImportCollector(
        ImmutableMap.Builder<MethodDescriptor, JsMethodImport> methodImports,
        ImmutableSet.Builder<String> moduleImports) {
      this.methodImports = methodImports;
      this.moduleImports = moduleImports;
    }

    @Override
    public void exitMethod(Method method) {
      MethodDescriptor methodDescriptor = method.getDescriptor();
      if (!shouldGenerateImport(methodDescriptor)) {
        return;
      }

      JsMethodImport methodImport = JsMethodImport.create(method);
      methodImports.put(methodDescriptor, methodImport);
      if (methodImport.getModuleDependency() != null) {
        moduleImports.add(methodImport.getModuleDependency());
      }
    }
  }

  private static boolean shouldGenerateImport(MethodDescriptor methodDescriptor) {
    // If the method maps to a WASM instruction, we should not generate an import, instead the
    // WASM instruction will be emitted.
    if (getWasmInfo(methodDescriptor) != null) {
      return false;
    }

    return methodDescriptor.isNative();
  }

  private JsImportsGenerator() {}
}
