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
package com.google.j2cl.transpiler;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.j2cl.common.StringUtils.unescapeWtf16;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Streams;
import com.google.common.io.Files;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.bazel.BazelWorker;
import com.google.j2cl.common.bazel.FileCache;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.StringLiteralGettersCreator;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import com.google.j2cl.transpiler.backend.wasm.ItableAllocator;
import com.google.j2cl.transpiler.backend.wasm.JsImportsGenerator;
import com.google.j2cl.transpiler.backend.wasm.JsMemberInfo;
import com.google.j2cl.transpiler.backend.wasm.SharedSnippet;
import com.google.j2cl.transpiler.backend.wasm.Summary;
import com.google.j2cl.transpiler.backend.wasm.SystemPropertyInfo;
import com.google.j2cl.transpiler.backend.wasm.TypeInfo;
import com.google.j2cl.transpiler.backend.wasm.WasmConstructsGenerator;
import com.google.j2cl.transpiler.backend.wasm.WasmGenerationEnvironment;
import com.google.j2cl.transpiler.backend.wasm.WasmGeneratorStage;
import com.google.j2cl.transpiler.frontend.jdt.JdtEnvironment;
import com.google.j2cl.transpiler.frontend.jdt.JdtParser;
import com.google.j2cl.transpiler.passes.RewriteReferenceEqualityOperations;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.MapOptionHandler;

/** Runs The J2wasmBundler as a worker. */
final class BazelJ2wasmBundler extends BazelWorker {

  private static final int CACHE_SIZE =
      Integer.parseInt(System.getProperty("j2cl.bundler.cachesize", "5000"));

  private static final FileCache<String> moduleContentsCache =
      new FileCache<>(BazelJ2wasmBundler::readModule, CACHE_SIZE);

  private static final FileCache<Summary> summaryCache =
      new FileCache<>(BazelJ2wasmBundler::readSummary, CACHE_SIZE);

  @Argument(required = true, usage = "The list of modular output directories", multiValued = true)
  List<Path> inputs = null;

  @Option(
      name = "-output",
      required = true,
      metaVar = "<path>",
      usage = "Directory or zip into which to place the bundled output.")
  Path output;

  @Option(
      name = "-jsimports",
      required = true,
      metaVar = "<path>",
      usage = "Directory or zip into which to place the JavaScript imports output.")
  Path jsimportPath;

  @Option(
      name = "-classpath",
      required = true,
      metaVar = "<path>",
      usage = "Specifies where to find all the class files for the application.")
  List<Path> classpaths;

  @Option(name = "-define", handler = MapOptionHandler.class, hidden = true)
  Map<String, String> defines = new HashMap<>();

  @Option(
      name = "-experimentalEnableWasmCustomDescriptorsJsInterop",
      usage = "Enables JsInterop with custom descriptors for Wasm.",
      hidden = true)
  boolean enableCustomDescriptorsJsInterop = false;

  @Override
  protected void run() {
    emitModuleFile();
    emitJsImportsFile();
  }

  private void emitModuleFile() {
    var typeGraph = new TypeGraph(getSummaries());

    // Create an environment to initialize the well known type descriptors to be able to synthesize
    // code.
    // TODO(b/294284380): consider removing JDT and manually synthesizing required types.
    new JdtEnvironment(
        new JdtParser(problems), classpaths, TypeDescriptors.getWellKnownTypeNames());

    var referencedSystemProperties =
        getSummaries()
            .flatMap(s -> s.getSystemPropertiesList().stream())
            .collect(
                toImmutableMap(
                    SystemPropertyInfo::getPropertyKey,
                    Function.identity(),
                    // Properties might be referenced in many times, only some requiring a value;
                    // keep properties that are required.
                    (p1, p2) -> p1.getIsRequired() ? p1 : p2))
            .values();

    // Synthesize globals and methods for string literals.
    synthesizeStringLiteralGetters(referencedSystemProperties);

    var generatorStage = new WasmGeneratorStage(library, problems);

    Stream<String> literalGetterMethods =
        compilationUnit.getTypes().stream()
            .flatMap(t -> t.getMethods().stream())
            .map(m -> generatorStage.emitToString(g -> g.renderMethod(m)));

    String literalGlobals = generatorStage.emitToString(g -> g.emitGlobals(library));

    ImmutableList<String> moduleContents =
        Streams.concat(
                Stream.of("(module "),
                streamDedupedValues(Summary::getNativeArrayTypeSnippetsList),
                Stream.of("(rec"),
                getModuleParts("types"),
                streamDedupedValues(Summary::getTypeSnippetsList),
                Stream.of(typeGraph.getTopLevelItableStructDeclaration()),
                typeGraph.getClasses().stream().map(TypeGraph.Type::getItableStructDeclaration),
                Stream.of(")"),
                Stream.of(getJsExportsTypes()),
                streamDedupedValues(Summary::getWasmImportSnippetsList),
                getModuleParts("imports"),
                Stream.of(generatorStage.emitToString(WasmConstructsGenerator::emitExceptionTag)),
                getModuleParts("contents"),
                streamDedupedValues(Summary::getGlobalSnippetsList),
                Stream.of(typeGraph.getEmptyItableDeclaration()),
                typeGraph.getClasses().stream().map(TypeGraph.Type::getItableInitialization),
                Stream.of(literalGlobals),
                literalGetterMethods,
                Stream.of(typeGraph.getItableInterfaceGetters(generatorStage.getEnvironment())),
                Stream.of(getJsPrototypes(typeGraph, generatorStage.getEnvironment())),
                Stream.of(getJsFunctions(typeGraph)),
                Stream.of(")"))
            .collect(toImmutableList());

    writeToFile(output.toString(), moduleContents, problems);
  }

  /** Gets basic types and imports needed for configuring JS exports. */
  private String getJsExportsTypes() {
    if (!enableCustomDescriptorsJsInterop) {
      return "";
    }

    return """
    (type $js_prototypes_t (array (mut externref)))
    (type $js_functions_t (array (mut funcref)))
    (type $js_data_t (array (mut i8)))
    (type $js_configureAll_t
      (func (param (ref null $js_prototypes_t))
            (param (ref null $js_functions_t))
            (param (ref null $js_data_t))
            (param externref))
    )

    (import "wasm:js-prototypes" "configureAll" (func $js_configureAll (type $js_configureAll_t)))
    (import "env" "constructors" (global $js_constructors externref))
    """;
  }

  private String getJsPrototypes(TypeGraph typeGraph, WasmGenerationEnvironment environment) {
    if (!enableCustomDescriptorsJsInterop) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("(elem $js_prototypes externref\n");
    for (var type : typeGraph.getJsTypes()) {
      sb.append("  (global.get ");
      sb.append(type.getPrototypeGlobalName(environment));
      sb.append(")\n");
    }
    sb.append(")\n");
    return sb.toString();
  }

  private String getJsFunctions(TypeGraph typeGraph) {
    if (!enableCustomDescriptorsJsInterop) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    // Generate a dummy constructor for types that don't export one.
    // If the type has no js constructors, we will use this dummy constructor. We still need to
    // reference a constructor in the configuration data in order to configure a name for the type
    // in the constructors list.
    sb.append("(func $js_constructor_placeholder (result anyref)\n");
    sb.append("  (unreachable)\n");
    sb.append(")\n");
    sb.append("\n");

    // List exported methods for all types.
    sb.append("(elem $js_functions funcref\n");
    for (var type : typeGraph.getJsTypes()) {
      // Constructor
      if (type.jsConstructor() != null) {
        sb.append("  (ref.func ");
        sb.append(type.jsConstructor().getWasmName());
        sb.append(")\n");
      } else {
        sb.append("  (ref.func $js_constructor_placeholder)\n");
      }

      // All other members.
      type.streamJsMembersExceptConstructor()
          .forEach(
              m -> {
                sb.append("  (ref.func ");
                sb.append(m.getWasmName());
                sb.append(")\n");
              });
    }
    sb.append(")\n");
    return sb.toString();
  }

  private Stream<String> streamDedupedValues(
      Function<Summary, Collection<SharedSnippet>> snippetGetter) {
    return getDedupedSnippets(snippetGetter).values().stream();
  }

  private ImmutableMap<String, String> getDedupedSnippets(
      Function<Summary, Collection<SharedSnippet>> snippetGetter) {
    return getSummaries()
        .flatMap(s -> snippetGetter.apply(s).stream())
        .collect(toImmutableMap(SharedSnippet::getKey, SharedSnippet::getSnippet, (i1, i2) -> i1));
  }

  private void synthesizeStringLiteralGetters(
      Collection<SystemPropertyInfo> referencedSystemProperties) {

    var stringLiteralHolder =
        new com.google.j2cl.transpiler.ast.Type(
            SourcePosition.NONE,
            TypeDeclaration.newBuilder()
                .setQualifiedSourceName("wasm.stringLiteral.StringLiteralHolder")
                .setKind(Kind.CLASS)
                .build());

    compilationUnit.addType(stringLiteralHolder);

    var stringLiteralGetterCreator = new StringLiteralGettersCreator();

    // Synthesize the getters and forwarding methods for the string literals in the code.
    getSummaries()
        .flatMap(s -> s.getStringLiteralsList().stream())
        .forEach(
            s ->
                // Get descriptor for the getter and synthesize the method logic if it is the
                // first time it was found.
                synthesizeStringLiteralGetter(
                    stringLiteralHolder,
                    stringLiteralGetterCreator,
                    s.getEnclosingTypeName(),
                    s.getMethodName(),
                    unescapeWtf16(s.getContent())));

    // Synthesize the getters and forwarding methods for the string literals that are values of
    // system properties.
    referencedSystemProperties.forEach(
        p -> {
          var propertyKey = p.getPropertyKey();
          var value = defines.get(propertyKey);
          boolean isRequired = p.getIsRequired();
          MethodDescriptor systemGetPropertyGetter =
              AstUtils.getSystemGetPropertyGetter(propertyKey, isRequired);
          if (value == null) {
            if (isRequired) {
              problems.error("No value found for required property %s", propertyKey);
            }
            // Synthesize a getter that returns null.
            synthesizeAbsentPropertyMethod(systemGetPropertyGetter);
          } else {
            synthesizeStringLiteralGetter(
                stringLiteralHolder,
                stringLiteralGetterCreator,
                systemGetPropertyGetter.getEnclosingTypeDescriptor().getQualifiedSourceName(),
                systemGetPropertyGetter.getOrigin().getPrefix() + systemGetPropertyGetter.getName(),
                value);
          }
        });

    // Perform the rewriting on the newly synthesized string literal getters.
    new RewriteReferenceEqualityOperations().applyTo(compilationUnit);
  }

  private void synthesizeStringLiteralGetter(
      com.google.j2cl.transpiler.ast.Type stringLiteralHolder,
      StringLiteralGettersCreator stringLiteralGetterCreator,
      String enclosingTypeQualifiedSourceName,
      String methodName,
      String stringValue) {
    MethodDescriptor m =
        stringLiteralGetterCreator.getOrCreateLiteralMethod(
            stringLiteralHolder, new StringLiteral(stringValue), /* synthesizeMethod= */ true);

    // Synthesize the forwarding logic.
    TypeDeclaration typeDeclaration =
        TypeDeclaration.newBuilder()
            .setQualifiedSourceName(enclosingTypeQualifiedSourceName)
            .setKind(Kind.CLASS)
            .build();
    com.google.j2cl.transpiler.ast.Type type = getType(typeDeclaration);

    Method forwarderMethod = synthesizeForwardingMethod(m, typeDeclaration, methodName);
    type.addMember(forwarderMethod);
  }

  private static Method synthesizeForwardingMethod(
      MethodDescriptor literalGetter, TypeDeclaration fromType, String forwardingMethodName) {
    MethodDescriptor forwarderDescriptor =
        MethodDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(fromType.toDescriptor())
            .setName(forwardingMethodName)
            .setOrigin(MethodOrigin.SYNTHETIC_STRING_LITERAL_GETTER)
            .setStatic(true)
            .setReturnTypeDescriptor(TypeDescriptors.get().javaLangString)
            .build();
    return AstUtils.createForwardingMethod(
        SourcePosition.NONE,
        null,
        forwarderDescriptor,
        literalGetter,
        /* jsDocDescription= */ null);
  }

  /** Synthesizes a method that returns null to implement absent properties. */
  private void synthesizeAbsentPropertyMethod(MethodDescriptor propertyGetter) {
    var typeDeclaration = propertyGetter.getEnclosingTypeDescriptor().getTypeDeclaration();
    com.google.j2cl.transpiler.ast.Type type = getType(typeDeclaration);
    type.addMember(
        Method.newBuilder()
            .setMethodDescriptor(propertyGetter)
            .setSourcePosition(SourcePosition.NONE)
            .setStatements(
                ReturnStatement.newBuilder()
                    .setExpression(TypeDescriptors.get().javaLangString.getNullValue())
                    .setSourcePosition(SourcePosition.NONE)
                    .build())
            .build());
  }

  /** Synthetic compilation unit for all types synthesized at bundling time. */
  private final CompilationUnit compilationUnit = CompilationUnit.createSynthetic("j2wasm-bundler");

  /** Synthetic library all types synthesized at bundling time. */
  private final Library library =
      Library.newBuilder().setCompilationUnits(ImmutableList.of(compilationUnit)).build();

  private final Map<TypeDeclaration, com.google.j2cl.transpiler.ast.Type> typesByDeclaration =
      new LinkedHashMap<>();

  private com.google.j2cl.transpiler.ast.Type getType(TypeDeclaration typeDeclaration) {
    return typesByDeclaration.computeIfAbsent(
        typeDeclaration,
        t -> {
          var newType = new com.google.j2cl.transpiler.ast.Type(SourcePosition.NONE, t);
          compilationUnit.addType(newType);
          return newType;
        });
  }

  /** Represents the inheritance structure of the whole application. */
  private static class TypeGraph {

    private static final int NO_TYPE_INDEX = 0;
    // The list of all interfaces.
    private final List<TypeGraph.Type> classes = new ArrayList<>();
    // The list of all classes.
    private final List<TypeGraph.Type> interfaces = new ArrayList<>();
    private final Map<String, TypeGraph.Type> typesByName = new LinkedHashMap<>();

    // List of exported JS types.
    private final List<TypeGraph.JsTypeInfo> jsTypes = new ArrayList<>();

    private final ItableAllocator<String> itableAllocator;

    private TypeGraph(Stream<Summary> summaries) {
      // Collect all types from all summaries.
      summaries.forEachOrdered(this::addToTypeGraph);
      this.itableAllocator = createItableAllocator();
    }

    private ItableAllocator<String> createItableAllocator() {
      SetMultimap<String, String> implementedInterfaceNamesByTypeName = LinkedHashMultimap.create();
      classes.forEach(
          c ->
              c.getImplementedInterfaces()
                  .forEach(i -> implementedInterfaceNamesByTypeName.put(c.getName(), i.getName())));
      ImmutableMap<String, String> superInterfaceNamesByTypeName =
          interfaces.stream()
              .filter(i -> i.superType != null)
              .collect(toImmutableMap(Type::getName, i -> i.superType.getName()));
      return new ItableAllocator<>(
          classes.stream().map(Type::getName).collect(toImmutableList()),
          implementedInterfaceNamesByTypeName::get,
          superInterfaceNamesByTypeName::get);
    }

    private void addToTypeGraph(Summary summary) {
      for (TypeInfo interfaceInfo : summary.getInterfacesList()) {
        var interfaceName = summary.getTypeNames(interfaceInfo.getTypeId());
        var interfaceType = new TypeGraph.Type(interfaceName);
        if (interfaceInfo.getExtendsType() != NO_TYPE_INDEX) {
          String superTypeName = summary.getTypeNames(interfaceInfo.getExtendsType());
          interfaceType.superType = checkNotNull(typesByName.get(superTypeName));
        }
        typesByName.put(interfaceName, interfaceType);
        interfaces.add(interfaceType);
      }

      for (TypeInfo typeInfo : summary.getTypesList()) {
        var name = summary.getTypeNames(typeInfo.getTypeId());
        var type =
            typesByName.computeIfAbsent(name, n -> new TypeGraph.Type(n, typeInfo.getAbstract()));
        classes.add(type);
        if (typeInfo.getExtendsType() != NO_TYPE_INDEX) {
          String superTypeName = summary.getTypeNames(typeInfo.getExtendsType());
          type.superType = checkNotNull(typesByName.get(superTypeName));
        }
        for (int interfaceId : typeInfo.getImplementsTypesList()) {
          String interfaceName = summary.getTypeNames(interfaceId);
          var interfaceType = typesByName.get(interfaceName);
          type.implementedInterfaces.add(interfaceType);
        }

        if (typeInfo.hasJsInfo()) {
          // Separate out constructors and static and instance members which are handled separately.
          JsMemberInfo jsConstructor = null;
          ImmutableList.Builder<JsMemberInfo> staticJsMembers = ImmutableList.builder();
          ImmutableList.Builder<JsMemberInfo> instanceJsMembers = ImmutableList.builder();
          for (JsMemberInfo member : typeInfo.getJsInfo().getJsMembersList()) {
            if (member.getKind() == JsMemberInfo.Kind.CONSTRUCTOR) {
              jsConstructor = member;
            } else if (member.getKind() == JsMemberInfo.Kind.METHOD) {
              if (member.getIsStatic()) {
                staticJsMembers.add(member);
              } else {
                instanceJsMembers.add(member);
              }
            }
          }

          var exportedType =
              new TypeGraph.JsTypeInfo(
                  typeInfo.getJsInfo().getQualifiedJsName(),
                  /* index= */ jsTypes.size(),
                  type.getExportedSupertype(),
                  jsConstructor,
                  /* staticJsMembers= */ staticJsMembers.build(),
                  /* instanceJsMembers= */ instanceJsMembers.build());

          jsTypes.add(exportedType);
          type.exportedType = exportedType;
        }
      }
    }

    List<TypeGraph.Type> getClasses() {
      return classes;
    }

    List<TypeGraph.JsTypeInfo> getJsTypes() {
      return jsTypes;
    }

    /** Emits the top-level itable struct. */
    String getTopLevelItableStructDeclaration() {
      StringBuilder sb = new StringBuilder();
      sb.append("(type $itable (sub (struct\n");
      // In the unoptimized itables each interface has its own slot.
      for (int i = 0; i < itableAllocator.getItableSize(); i++) {
        sb.append("  (field (ref null struct))\n");
      }
      sb.append(")))\n");
      return sb.toString();
    }

    public static final String EMPTY_ITABLE_NAME = "$itable.empty";

    public String getEmptyItableDeclaration() {
      StringBuilder sb = new StringBuilder();
      sb.append(format("(global %s (ref $itable) (struct.new $itable \n", EMPTY_ITABLE_NAME));
      for (int i = 0; i < itableAllocator.getItableSize(); i++) {
        sb.append("(ref.null struct)\n");
      }
      sb.append("))\n");
      return sb.toString();
    }

    public String getItableInterfaceGetters(WasmGenerationEnvironment environment) {
      SourceBuilder sourceBuilder = new SourceBuilder();
      WasmConstructsGenerator constructsGenerator =
          new WasmConstructsGenerator(
              environment, sourceBuilder, /* sourceMappingPathPrefix= */ null);
      interfaces.forEach(
          i -> {
            int itableFieldIndex = itableAllocator.getItableFieldIndex(i.name);
            String itableFieldIndexString =
                itableFieldIndex == -1 ? null : String.valueOf(itableFieldIndex);
            constructsGenerator.emitItableInterfaceGetter(
                environment.getWasmItableInterfaceGetter(i.name), itableFieldIndexString);
          });
      return sourceBuilder.build();
    }

    private class Type {
      private final String name;
      private Type superType;
      private final Set<Type> implementedInterfaces = new HashSet<>();
      private final boolean isAbstract;
      private TypeGraph.JsTypeInfo exportedType;

      public Type(String name, boolean isAbstract) {
        this.name = name;
        this.isAbstract = isAbstract;
      }

      public Type(String name) {
        this(name, false);
      }

      public String getName() {
        return name;
      }

      public Set<Type> getImplementedInterfaces() {
        return implementedInterfaces;
      }

      public boolean isSuperTypeOf(Type other) {
        if (other == null) {
          return false;
        }
        return this == other.superType || isSuperTypeOf(other.superType);
      }

      public String getItableTypeName() {
        if (implementedInterfaces.isEmpty()) {
          return "$itable";
        }
        return format("%s.itable", name);
      }

      /** Emits the itable struct type for a class. */
      public String getItableStructDeclaration() {
        if (implementedInterfaces.isEmpty()) {
          return "";
        }

        String superItableTypeName = superType == null ? "$itable" : superType.getItableTypeName();
        StringBuilder sb = new StringBuilder();
        sb.append(format("(type %s.itable (sub %s (struct \n", name, superItableTypeName));

        for (Type i : getItableFieldTypes()) {
          sb.append(
              format("  (field (ref %s))\n", i != null ? (i.name + ".vtable") : "null struct"));
        }
        sb.append(")))\n");
        return sb.toString();
      }

      public String getItableInitialization() {
        if (isAbstract) {
          // Abstract classes don't have itable instances.
          return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(format("(global %s.itable (ref %s) ", name, getItableTypeName()));
        if (implementedInterfaces.isEmpty()) {
          sb.append(format("(global.get %s)", EMPTY_ITABLE_NAME));
        } else {
          sb.append(format("(struct.new %s \n", getItableTypeName()));
          for (Type i : getItableFieldTypes()) {
            sb.append(
                i == null
                    ? "(ref.null struct)\n"
                    : format("(global.get %s.vtable@%s)\n", i.name, this.name));
          }
          sb.append(")");
        }
        sb.append(")\n");
        return sb.toString();
      }

      private Type[] itableFieldTypes;

      /**
       * Computes the itable for this type: gets an array of interface types indexed by itable slot
       * assignment.
       */
      private Type[] getItableFieldTypes() {
        if (itableFieldTypes == null) {
          // Determine the type for each itable field by going through all implemented interfaces
          // and assigning it to the index as determined by the itable allocator. If the itable
          // index is shared by multiple interfaces, itable allocator guarantees that they are part
          // of the same inheritance chain; use the more specific type.
          itableFieldTypes = new Type[itableAllocator.getItableSize()];
          for (Type i : implementedInterfaces) {
            int itableIndex = itableAllocator.getItableFieldIndex(i.name);
            if (itableFieldTypes[itableIndex] == null
                || itableFieldTypes[itableIndex].isSuperTypeOf(i)) {
              itableFieldTypes[itableIndex] = i;
            } else {
              checkState(
                  i.isSuperTypeOf(itableFieldTypes[itableIndex]),
                  "Interfaces %s and %s are expected to be in same inheritance chain.",
                  i,
                  itableFieldTypes[itableIndex]);
            }
          }
        }
        return itableFieldTypes;
      }

      private TypeGraph.JsTypeInfo getExportedSupertype() {
        TypeGraph.Type currentType = superType;
        while (currentType != null) {
          if (currentType.exportedType != null) {
            return currentType.exportedType;
          }
          currentType = currentType.superType;
        }
        return null;
      }
    }

    private record JsTypeInfo(
        String qualifiedJsName,
        int index,
        JsTypeInfo superType,
        JsMemberInfo jsConstructor,
        ImmutableList<JsMemberInfo> staticJsMembers,
        ImmutableList<JsMemberInfo> instanceJsMembers) {

      String getPrototypeGlobalName(WasmGenerationEnvironment environment) {
        return environment.getJsPrototypeGlobalName(qualifiedJsName);
      }

      /**
       * Streams all members of this type, excluding the constructor. Static members appear first,
       * followed by instance members. Members should be referenced in the configuration data in the
       * order they appear in this stream.
       */
      Stream<JsMemberInfo> streamJsMembersExceptConstructor() {
        return Streams.concat(staticJsMembers.stream(), instanceJsMembers.stream());
      }
    }
  }

  private void emitJsImportsFile() {
    var requiredModules =
        getSummaries()
            .flatMap(s -> s.getJsImportRequiresList().stream())
            .distinct()
            .collect(toImmutableList());

    var jsImportsContents = getDedupedSnippets(Summary::getJsImportSnippetsList);

    writeToFile(
        jsimportPath.toString(),
        ImmutableList.of(
            JsImportsGenerator.generateOutputs(
                requiredModules,
                jsImportsContents,
                /* enableJsInterop= */ enableCustomDescriptorsJsInterop)),
        problems);
  }

  private Stream<Summary> getSummaries() {
    return inputs.stream()
        .map(d -> format("%s/summary.binpb", d))
        .filter(n -> new File(n).exists())
        .map(summaryCache::get);
  }

  private Stream<String> getModuleParts(String name) {
    return inputs.stream()
        .map(d -> format("%s/%s.wat", d, name))
        .filter(n -> new File(n).exists())
        .map(BazelJ2wasmBundler.moduleContentsCache::get);
  }

  private static Summary readSummary(Path summaryPath) throws IOException {
    try (InputStream inputStream =
        new BufferedInputStream(java.nio.file.Files.newInputStream(summaryPath))) {
      return Summary.parseFrom(inputStream);
    }
  }

  private static String readModule(Path modulePath) throws IOException {
    return java.nio.file.Files.readString(modulePath);
  }

  private static void writeToFile(String filePath, List<String> contents, Problems problems) {
    try {
      Files.asCharSink(new File(filePath), UTF_8).writeLines(contents);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.getMessage());
    }
  }

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelJ2wasmBundler::new);
  }
}
