/*
 * Copyright 2020 Google Inc.
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
import static com.google.common.base.Predicates.not;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import com.google.j2cl.transpiler.backend.wasm.JsImportsGenerator.Imports;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

// TODO(goktug): Support cancellation for this backend..
/** Generates all the outputs for Wasm compilation. */
public class WasmGeneratorStage {

  private final Problems problems;
  private final Output output;
  private final Path libraryInfoOutputPath;
  private final String sourceMappingPathPrefix;
  private final boolean enableCustomDescriptors;
  private final boolean enableCustomDescriptorsJsInterop;
  private WasmGenerationEnvironment environment;

  /** Returns a generator stage that can emit code as strings. */
  public WasmGeneratorStage(Library library, Problems problems) {
    this(
        /* output= */ null,
        /* libraryInfoOutputPath= */ null,
        /* sourceMappingPathPrefix= */ null,
        /* enableCustomDescriptors= */ false,
        /* enableCustomDescriptorsJsInterop= */ false,
        problems);
    this.environment =
        new WasmGenerationEnvironment(
            library,
            JsImportsGenerator.collectImports(library, problems),
            sourceMappingPathPrefix,
            enableCustomDescriptors,
            enableCustomDescriptorsJsInterop,
            /* isModular= */ true);
  }

  private WasmGeneratorStage(
      Output output,
      Path libraryInfoOutputPath,
      String sourceMappingPathPrefix,
      boolean enableCustomDescriptors,
      boolean enableCustomDescriptorsJsInterop,
      Problems problems) {
    this.output = output;
    this.libraryInfoOutputPath = libraryInfoOutputPath;
    this.sourceMappingPathPrefix = sourceMappingPathPrefix;
    this.enableCustomDescriptors = enableCustomDescriptors;
    this.enableCustomDescriptorsJsInterop = enableCustomDescriptorsJsInterop;
    this.problems = problems;
  }

  public WasmGenerationEnvironment getEnvironment() {
    return environment;
  }

  public static void generateModularOutput(
      Library library,
      Output output,
      Path libraryInfoOutputPath,
      String sourceMappingPathPrefix,
      boolean enableCustomDescriptors,
      boolean enableCustomDescriptorsJsInterop,
      Problems problems) {
    new WasmGeneratorStage(
            output,
            libraryInfoOutputPath,
            sourceMappingPathPrefix,
            enableCustomDescriptors,
            enableCustomDescriptorsJsInterop,
            problems)
        .generateModularOutput(library);
  }

  private void generateModularOutput(Library library) {
    if (libraryInfoOutputPath != null) {
      OutputUtils.writeToFile(libraryInfoOutputPath, new byte[0], problems);
    }

    Imports jsImports = JsImportsGenerator.collectImports(library, problems);
    environment =
        new WasmGenerationEnvironment(
            library,
            jsImports,
            sourceMappingPathPrefix,
            enableCustomDescriptors,
            enableCustomDescriptorsJsInterop,
            /* isModular= */ true);
    problems.abortIfCancelled();

    SummaryBuilder summaryBuilder = new SummaryBuilder(library, environment, problems);

    JsImportsGenerator.collectImportSnippets(jsImports)
        .forEach(summaryBuilder::addSharedJsImportSnippet);

    jsImports.getModuleImports().forEach(summaryBuilder::addSharedJsImportRequireSnippet);
    problems.abortIfCancelled();

    collectUsedNativeArrayTypes(library)
        .forEach(
            t -> {
              if (t.isPrimitiveArray()) {
                // Emit all native primitive arrays into a different section in the summary
                // so that they can be emitted outside the rec group. Binaryen requires that the
                // i16 arrays used by the string operations be the plain i16 array and an array
                // of the same definition emitted in a rec group would be of a different type.
                summaryBuilder.addNativeArrayTypeSnippet(
                    environment.getWasmTypeName(t), emitToString(g -> g.emitWasmArrayType(t)));
              } else {
                // Non primitive arrays reference 'java.lang.Object' and need to be emitted after
                // its declaration.
                summaryBuilder.addSharedTypeSnippet(
                    environment.getWasmTypeName(t), emitToString(g -> g.emitWasmArrayType(t)));
              }
              summaryBuilder.addSharedGlobalSnippet(
                  environment.getWasmEmptyArrayGlobalName(t),
                  emitToString(g -> g.emitEmptyArraySingleton(t)));
            });
    problems.abortIfCancelled();

    environment
        .collectMethodsThatNeedTypeDeclarations()
        .forEach(
            (k, m) ->
                summaryBuilder.addSharedTypeSnippet(
                    k, emitToString(g -> g.emitFunctionType(k, m))));
    problems.abortIfCancelled();

    environment
        .collectMethodsNeedingIntrinsicDeclarations()
        .forEach(
            (k, m) ->
                summaryBuilder.addSharedWasmImportSnippet(
                    k, emitToString(g -> g.emitBinaryenIntrinsicImport(k, m))));

    output.write("summary.binpb", summaryBuilder.toByteArray());
    problems.abortIfCancelled();

    copyJavaSources(library);

    emitToFile(
        "types.wat",
        generator -> generator.emitForEachType(library, generator::renderModularTypeStructs));

    emitToFile(
        "imports.wat",
        generator -> generator.emitForEachType(library, generator::renderImportedMethods));

    emitToFile(
        "contents.wat",
        generator -> {
          generator.emitDataSegments(library);
          generator.emitGlobals(library);
          generator.emitClassDispatchTables(library, /* emitItableInitialization= */ false);
          generator.emitForEachType(library, generator::renderTypeMethods);
        });

    emitNameMappingFile(library, output);
    generateJsExterns(library);
  }

  public String emitToString(Consumer<WasmConstructsGenerator> emitter) {
    SourceBuilder builder = new SourceBuilder();
    WasmConstructsGenerator generator =
        new WasmConstructsGenerator(environment, builder, sourceMappingPathPrefix);

    emitter.accept(generator);

    return builder.build();
  }

  private void emitToFile(String filename, Consumer<WasmConstructsGenerator> emitter) {
    problems.abortIfCancelled();
    String content = emitToString(emitter);
    if (content.isEmpty()) {
      return;
    }
    output.write(filename, content);
  }

  public static void generateMonolithicOutput(
      Library library,
      Output output,
      Path libraryInfoOutputPath,
      String sourceMappingPathPrefix,
      boolean enableCustomDescriptors,
      boolean enableCustomDescriptorsJsInterop,
      Problems problems) {
    new WasmGeneratorStage(
            output,
            libraryInfoOutputPath,
            sourceMappingPathPrefix,
            enableCustomDescriptors,
            enableCustomDescriptorsJsInterop,
            problems)
        .generateMonolithicOutput(library);
  }

  private void generateMonolithicOutput(Library library) {
    copyJavaSources(library);
    generateWasmModule(library);
    generateJsImportsFile();
  }

  private void copyJavaSources(Library library) {
    library.getCompilationUnits().stream()
        .filter(not(CompilationUnit::isSynthetic))
        .forEach(
            compilationUnit ->
                output.copyFile(
                    compilationUnit.getFilePath(), compilationUnit.getPackageRelativePath()));
  }

  private void generateWasmModule(Library library) {
    environment =
        new WasmGenerationEnvironment(
            library, JsImportsGenerator.collectImports(library, problems));
    SourceBuilder builder = new SourceBuilder();
    WasmConstructsGenerator generator =
        new WasmConstructsGenerator(environment, builder, sourceMappingPathPrefix);

    List<ArrayTypeDescriptor> usedNativeArrayTypes = collectUsedNativeArrayTypes(library);

    builder.appendln(";;; Code generated by J2WASM");
    builder.append("(module");
    // Emit all types at the beginning of the module.
    generator.emitLibraryTypes(library, usedNativeArrayTypes);

    // Emit imports.
    generator.emitImportsForBinaryenIntrinsics();
    generator.emitForEachType(library, generator::renderImportedMethods);
    generator.emitExceptionTag();

    // Emit all the globals, e.g. vtable instances, etc.
    generator.emitDataSegments(library);
    generator.emitDispatchTablesInitialization(library);
    generator.emitEmptyArraySingletons(usedNativeArrayTypes);
    generator.emitGlobals(library);

    // Last, emit all methods at the very end so that the synthetic code generated above does
    // not inherit an incorrect source position.
    generator.emitForEachType(library, generator::renderTypeMethods);
    generator.emitItableInterfaceGetters(library);

    builder.newLine();
    builder.append(")");
    output.write("module.wat", builder.buildToList());
    emitNameMappingFile(library, output);
  }

  public static void generateWasmExportMethods(
      List<Method> methods, Output output, Problems problems) {
    WasmGeneratorStage wasmGeneratorStage =
        new WasmGeneratorStage(
            output,
            /* libraryInfoOutputPath= */ null,
            /* sourceMappingPathPrefix= */ null,
            /* enableCustomDescriptors= */ false,
            /* enableCustomDescriptorsJsInterop= */ false,
            problems);
    wasmGeneratorStage.generateWasmExportMethods(methods);

    // Emit the name map for the generated methods.
    wasmGeneratorStage.emitNameMappingFile(methods, output);
  }

  private void generateWasmExportMethods(List<Method> methods) {
    if (methods.isEmpty()) {
      return;
    }

    // Create the type objects and add all the exported methods to the corresponding type to
    // initialize the WasmGenerationEnvironment.
    CompilationUnit cu = CompilationUnit.createSynthetic("wasm.exports");
    Map<TypeDeclaration, Type> typesByDeclaration = new LinkedHashMap<>();
    methods.forEach(
        m -> {
          TypeDeclaration typeDeclaration =
              m.getDescriptor().getEnclosingTypeDescriptor().getTypeDeclaration();
          Type type =
              typesByDeclaration.computeIfAbsent(
                  typeDeclaration, t -> new Type(SourcePosition.NONE, t));
          type.addMember(m);
        });
    typesByDeclaration.values().forEach(cu::addType);
    Library library = Library.newBuilder().setCompilationUnits(ImmutableList.of(cu)).build();
    environment =
        new WasmGenerationEnvironment(
            library, JsImportsGenerator.collectImports(library, problems));

    SourceBuilder builder = new SourceBuilder();
    WasmConstructsGenerator generator =
        new WasmConstructsGenerator(environment, builder, sourceMappingPathPrefix);

    methods.forEach(generator::renderMethod);
    output.write("contents.wat", builder.buildToList());
  }

  private List<ArrayTypeDescriptor> collectUsedNativeArrayTypes(Library library) {
    Set<ArrayTypeDescriptor> usedArrayTypes = new LinkedHashSet<>();
    // Collect native arrays from fields and variables; this covers all scenarios.
    library.accept(
        // TODO(b/303659726): Generalize a type visitor that could be used here and other places
        // like in ImportGatherer. Or consider emitting the one dimensional array type for all
        // native types.
        new AbstractVisitor() {
          @Override
          public void exitField(Field field) {
            collectIfArrayType(field.getDescriptor().getTypeDescriptor());
          }

          @Override
          public void exitVariable(Variable variable) {
            collectIfArrayType(variable.getTypeDescriptor());
          }

          private void collectIfArrayType(TypeDescriptor typeDescriptor) {
            if (!typeDescriptor.isArray()) {
              return;
            }

            ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
            if (arrayTypeDescriptor.isNativeWasmArray()) {
              usedArrayTypes.add(arrayTypeDescriptor);
            }
          }
        });

    return new ArrayList<>(usedArrayTypes);
  }

  private void generateJsImportsFile() {
    JsImportsGenerator.generateOutputs(
        output,
        environment.getJsImports(),
        /* enableJsInterop= */ enableCustomDescriptorsJsInterop);
  }

  private void generateJsExterns(Library library) {
    JsExternsGenerator.generateOutputs(output, environment, library);
  }

  /** Emits a symbol to name mapping file for all methods in the library. */
  private void emitNameMappingFile(Library library, Output output) {
    List<Method> methods = new ArrayList<>();

    library.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            methods.add(method);
          }
        });

    emitNameMappingFile(methods, output);
  }

  /** Emits a symbol to name mapping file for the supplied methods. */
  private void emitNameMappingFile(List<Method> methods, Output output) {
    SourceBuilder builder = new SourceBuilder();
    methods.forEach(m -> emitMethodMapping(m, builder));
    output.write("name.map", builder.build());
  }

  private void emitMethodMapping(Method method, SourceBuilder builder) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    String methodImplementationName = environment.getMethodImplementationName(methodDescriptor);
    checkState(methodImplementationName.startsWith("$"));
    builder.append(
        String.format(
            "%s:%s",
            methodImplementationName.substring(1), methodDescriptor.getQualifiedBinaryName()));
    builder.newLine();
  }
}
