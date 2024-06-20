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

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.String.format;
import static java.util.Arrays.stream;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.StringUtils;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Generates all the syntactic .wat constructs for wasm. */
public class WasmConstructsGenerator {

  private final SourceBuilder builder;
  private final WasmGenerationEnvironment environment;

  public WasmConstructsGenerator(WasmGenerationEnvironment environment, SourceBuilder builder) {
    this.environment = environment;
    this.builder = builder;
  }

  void emitDataSegments(Library library) {
    library.accept(
        new AbstractVisitor() {
          @Override
          public void exitArrayLiteral(ArrayLiteral arrayLiteral) {
            if (canBeMovedToDataSegment(arrayLiteral)
                && environment.registerDataSegmentLiteral(
                    arrayLiteral, getCurrentType().getDeclaration().getQualifiedBinaryName())) {
              var dataElementName = environment.getDataElementNameForLiteral(arrayLiteral);
              builder.newLine();
              builder.append(
                  format("(data %s \"%s\")", dataElementName, toDataString(arrayLiteral)));
            }
          }
        });
  }

  private boolean canBeMovedToDataSegment(ArrayLiteral arrayLiteral) {
    return TypeDescriptors.isNonVoidPrimitiveType(
            arrayLiteral.getTypeDescriptor().getComponentTypeDescriptor())
        && arrayLiteral.getValueExpressions().stream().allMatch(NumberLiteral.class::isInstance);
  }

  /**
   * Encodes an array literal of primitive values as a sequence of bytes, in UTF8 encoding for
   * readability.
   */
  private String toDataString(ArrayLiteral arrayLiteral) {
    PrimitiveTypeDescriptor componentTypeDescriptor =
        (PrimitiveTypeDescriptor) arrayLiteral.getTypeDescriptor().getComponentTypeDescriptor();
    int sizeInBits = componentTypeDescriptor.getWidth();
    List<Expression> valueExpressions = arrayLiteral.getValueExpressions();

    // Preallocate the stringbuilder to hold the encoded data since its size its already known.
    StringBuilder sb = new StringBuilder(valueExpressions.size() * (sizeInBits / 8));
    for (Expression expression : valueExpressions) {
      NumberLiteral literal = (NumberLiteral) expression;
      long value;
      PrimitiveTypeDescriptor typeDescriptor = literal.getTypeDescriptor();
      if (TypeDescriptors.isPrimitiveFloat(typeDescriptor)) {
        value = Float.floatToRawIntBits(literal.getValue().floatValue());
      } else if (TypeDescriptors.isPrimitiveDouble(typeDescriptor)) {
        value = Double.doubleToRawLongBits(literal.getValue().doubleValue());
      } else {
        value = literal.getValue().longValue();
      }

      for (int s = sizeInBits; s > 0; s -= 8, value >>>= 8) {
        sb.append(StringUtils.escapeAsUtf8((int) (value & 0xFF)));
      }
    }
    return sb.toString();
  }

  /** Emits all wasm type definitions into a single rec group. */
  void emitLibraryRecGroup(Library library, List<ArrayTypeDescriptor> usedNativeArrayTypes) {
    builder.newLine();
    builder.append("(rec");
    builder.indent();

    emitDynamicDispatchMethodTypes();
    emitItableSupportTypes();
    emitNativeArrayTypes(usedNativeArrayTypes);
    emitForEachType(library, this::renderMonolithicTypeStructs, "type definition");

    builder.unindent();
    builder.newLine();
    builder.append(")");
  }

  private void emitItableSupportTypes() {
    builder.newLine();
    // The itable is a struct that contains only interface vtables. Interfaces are assigned an index
    // on this struct based on the classes that implement them.
    builder.append("(type $itable (sub (struct");
    builder.indent();
    for (int index = 0; index < environment.getItableSize(); index++) {
      builder.newLine();
      builder.append("(field (ref null struct))");
    }
    builder.unindent();
    builder.newLine();
    builder.append(")))");
  }

  public void emitGlobals(Library library) {
    emitStaticFieldGlobals(library);
  }

  /** Emit the type for all function signatures that will be needed to reference vtable methods. */
  void emitDynamicDispatchMethodTypes() {
    environment.collectMethodsThatNeedTypeDeclarations().forEach(this::emitFunctionType);
  }

  void emitFunctionType(String typeName, MethodDescriptor m) {
    builder.newLine();
    builder.append(format("(type %s (func", typeName));
    emitFunctionParameterTypes(m);
    emitFunctionResultType(m);
    builder.append("))");
  }

  /**
   * Emit the necessary imports of binaryen intrinsics.
   *
   * <p>In order to communicate information to binaryen, binaryen provides intrinsic methods that
   * need to be imported.
   */
  void emitImportsForBinaryenIntrinsics() {

    // Emit the intrinsic for calls with no side effects. The intrinsic method exported name is
    // "call.without.effects" and can be used to convey to binaryen that a certain function call
    // does not have side effects.
    // Since the mechanism itself is a call, it needs to be correctly typed. As a result for each
    // different function type that appears in the AST as part of no-side-effect call, an import
    // with the right function type definition needs to be created.
    environment
        .collectMethodsNeedingIntrinsicDeclarations()
        .forEach(this::emitBinaryenIntrinsicImport);
  }

  void emitBinaryenIntrinsicImport(String typeName, MethodDescriptor m) {
    builder.newLine();
    builder.append(
        format(
            "(import \"binaryen-intrinsics\" \"call.without.effects\" " + "(func %s ", typeName));
    emitFunctionParameterTypes(m);
    builder.append(" (param funcref)");
    emitFunctionResultType(m);
    builder.append("))");
  }

  private void emitFunctionParameterTypes(MethodDescriptor methodDescriptor) {
    if (!methodDescriptor.isStatic()) {
      // Add the implicit parameter
      builder.append(
          format(
              " (param (ref %s))",
              environment.getWasmTypeName(TypeDescriptors.get().javaLangObject)));
    }
    methodDescriptor
        .getDispatchParameterTypeDescriptors()
        .forEach(t -> builder.append(format(" (param %s)", environment.getWasmType(t))));
  }

  private void emitFunctionResultType(MethodDescriptor methodDescriptor) {
    TypeDescriptor returnTypeDescriptor = methodDescriptor.getDispatchReturnTypeDescriptor();
    if (!TypeDescriptors.isPrimitiveVoid(returnTypeDescriptor)) {
      builder.append(format(" (result %s)", environment.getWasmType(returnTypeDescriptor)));
    }
  }

  public void emitExceptionTag() {
    // Declare a tag that will be used for Java exceptions. The tag has a single parameter that is
    // the Throwable object being thrown by the throw instruction.
    // The throw instruction will refer to this tag and will expect a single element in the stack
    // with the type $java.lang.Throwable.
    // TODO(b/277970998): Decide how to handle this hard coded import w.r.t. import generation.
    builder.newLine();
    builder.append("(import \"WebAssembly\" \"JSTag\" (tag $exception.event (param externref)))");
    // Add an export that uses the tag to workarund binaryen assuming the tag is never instantiated.
    builder.append(
        "(func $keep_tag_alive_hack (export \"_tag_hack_\") (param $param externref)  "
            + "(throw $exception.event (local.get $param)))");
  }

  private void renderMonolithicTypeStructs(Type type) {
    renderTypeStructs(type, /* isModular= */ false);
  }

  void renderModularTypeStructs(Type type) {
    renderTypeStructs(type, /* isModular= */ true);
  }

  private void renderTypeStructs(Type type, boolean isModular) {
    if (type.isNative()
        || type.getDeclaration().getWasmInfo() != null
        || AstUtils.isNonNativeJsEnum(type.getTypeDescriptor())) {
      return;
    }

    renderTypeVtableStruct(type);
    if (!type.isInterface()) {
      renderTypeStruct(type);
      if (!isModular) {
        renderClassItableStruct(type);
      }
    }
  }

  private void renderClassItableStruct(Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    if (!typeDeclaration.implementsInterfaces()) {
      return;
    }
    emitItableType(typeDeclaration, getInterfacesByItableIndex(typeDeclaration));
  }

  /**
   * Renders the struct for the vtable of a class or interface.
   *
   * <p>Vtables for interfaces include all methods from their superinterfaces. Calls to interface
   * methods will point to the subinterface, if possible.
   */
  private void renderTypeVtableStruct(Type type) {
    WasmTypeLayout wasmTypeLayout = environment.getWasmTypeLayout(type.getDeclaration());
    renderVtableStruct(type, wasmTypeLayout.getAllPolymorphicMethods());
  }

  private void renderVtableStruct(Type type, Collection<MethodDescriptor> methods) {
    emitWasmStruct(type, environment::getWasmVtableTypeName, () -> renderVtableEntries(methods));
  }

  private void renderVtableEntries(Collection<MethodDescriptor> methodDescriptors) {
    methodDescriptors.forEach(
        m -> {
          builder.newLine();
          builder.append(
              format(
                  "(field $%s (ref %s))", m.getMangledName(), environment.getFunctionTypeName(m)));
        });
  }

  private void emitStaticFieldGlobals(Library library) {
    library.streamTypes().forEach(this::emitStaticFieldGlobals);
  }

  private void emitStaticFieldGlobals(Type type) {
    var fields = type.getStaticFields();
    if (AstUtils.isNonNativeJsEnum(type.getTypeDescriptor()) || fields.isEmpty()) {
      return;
    }
    emitBeginCodeComment(type, "static fields");
    for (Field field : fields) {
      builder.newLine();
      builder.append("(global " + environment.getFieldName(field));

      if (field.isCompileTimeConstant()) {
        builder.append(
            format(" %s", environment.getWasmType(field.getDescriptor().getTypeDescriptor())));
        builder.indent();
        builder.newLine();
        ExpressionTranspiler.render(field.getInitializer(), builder, environment);
        builder.unindent();
      } else {
        builder.append(
            format(
                " (mut %s)", environment.getWasmType(field.getDescriptor().getTypeDescriptor())));
        builder.indent();
        builder.newLine();
        ExpressionTranspiler.render(
            AstUtils.getInitialValue(field.getDescriptor().getTypeDescriptor()),
            builder,
            environment);
        builder.unindent();
      }

      builder.newLine();
      builder.append(")");
    }
    emitEndCodeComment(type, "static fields");
  }

  void renderImportedMethods(Type type) {
    type.getMethods().stream().filter(environment::isJsImport).forEach(this::renderMethod);
  }

  void renderTypeMethods(Type type) {
    type.getMethods().stream()
        .filter(Predicate.not(environment::isJsImport))
        .forEach(this::renderMethod);
  }


  public void renderMethod(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    if ((methodDescriptor.isAbstract() && !methodDescriptor.isNative())
        || methodDescriptor.getWasmInfo() != null) {
      // Abstract methods don't generate any code, except if they are native; neither do methods
      // that have @Wasm annotation.
      return;
    }

    // TODO(b/264676817): Consider refactoring to have MethodDescriptor.isNative return true for
    // native constructors, or exposing isNativeConstructor from MethodDescriptor.
    boolean isNativeConstructor =
        methodDescriptor.getEnclosingTypeDescriptor().isNative()
            && methodDescriptor.isConstructor();
    JsMethodImport jsMethodImport = environment.getJsMethodImport(methodDescriptor);
    if (jsMethodImport == null && isNativeConstructor) {
      // TODO(b/279187295): These are implicit constructors of native types that don't really exist,
      // remove this check once they are removed from the AST.
      return;
    }
    builder.newLine();
    builder.newLine();
    builder.append(";;; " + method.getReadableDescription());
    builder.newLine();
    builder.append("(func " + environment.getMethodImplementationName(methodDescriptor));

    // Generate an import if the method is native. We don't use the normal qualified js name,
    // because it doesn't differentiate between js property getters and setters.
    if (jsMethodImport != null) {
      builder.append(
          format(
              " (import \"%s\" \"%s\") ",
              JsImportsGenerator.MODULE, jsMethodImport.getImportKey()));
    }

    if (method.isWasmEntryPoint()) {
      builder.append(" (export \"" + method.getWasmExportName() + "\")");
    }

    DeclaredTypeDescriptor enclosingTypeDescriptor = methodDescriptor.getEnclosingTypeDescriptor();

    // Emit parameters
    builder.indent();
    // Add the implicit "this" parameter to instance methods and constructors.
    if (isReceiverCastNeeded(methodDescriptor)) {
      builder.newLine();
      builder.append(format("(type %s)", environment.getFunctionTypeName(methodDescriptor)));
      builder.newLine();
      builder.append(
          format(
              "(param $this.untyped (ref %s))",
              environment.getWasmTypeName(TypeDescriptors.get().javaLangObject)));
    } else if (!method.isStatic() && !isNativeConstructor) {
      // Private methods and constructors receive the instance with the actual type.
      // Native constructors do not receive the instance.
      builder.newLine();
      builder.append(format("(param $this %s)", environment.getWasmType(enclosingTypeDescriptor)));
    }

    for (Variable parameter : method.getParameters()) {
      builder.newLine();
      builder.append(
          "(param "
              + environment.getDeclarationName(parameter)
              + " "
              + environment.getWasmType(parameter.getTypeDescriptor())
              + ")");
    }

    TypeDescriptor returnTypeDescriptor = methodDescriptor.getDispatchReturnTypeDescriptor();

    // Emit return type.
    if (!TypeDescriptors.isPrimitiveVoid(returnTypeDescriptor)) {
      builder.newLine();
      builder.append("(result " + environment.getWasmType(returnTypeDescriptor) + ")");
    }

    if (jsMethodImport != null) {
      // Imports don't define locals nor body.
      builder.unindent();
      builder.newLine();
      builder.append(")");
      return;
    }

    // Emit a source mapping at the entry of a method so that when stepping into a method
    // the debugger shows the right source line.
    StatementTranspiler.renderSourceMappingComment(method.getSourcePosition(), builder);

    // Emit locals.
    for (Variable variable : collectLocals(method)) {
      builder.newLine();
      builder.append(
          "(local "
              + environment.getDeclarationName(variable)
              + " "
              + environment.getWasmType(variable.getTypeDescriptor())
              + ")");
    }
    // Introduce the actual $this variable for polymorphic methods and cast the parameter to
    // the right type.
    if (isReceiverCastNeeded(methodDescriptor)) {
      builder.newLine();
      builder.append(format("(local $this %s)", environment.getWasmType(enclosingTypeDescriptor)));
      builder.newLine();
      // Use non-nullable `ref.cast` since the receiver of an instance method should
      // not be null, and it is ok to fail for devirtualized methods if it is.
      builder.append(
          format(
              "(local.set $this (ref.cast (ref %s) (local.get $this.untyped)))",
              environment.getWasmTypeName(enclosingTypeDescriptor)));
    }

    StatementTranspiler.render(method.getBody(), builder, environment);
    builder.unindent();
    builder.newLine();
    builder.append(")");

    // Declare a function that will be target of dynamic dispatch.
    if (methodDescriptor.isPolymorphic()) {
      builder.newLine();
      builder.append(
          format(
              "(elem declare func %s)",
              environment.getMethodImplementationName(method.getDescriptor())));
    }
  }

  /**
   * Returns true if the method declares its receiver as `Object` and needs to cast it to the
   * appropriate type.
   *
   * <p>Note that constructors and private methods, that are also passed a receiver, can use the
   * enclosing type directly as they are are not overriding a supertype method and don't need to
   * match the signature.
   */
  private static boolean isReceiverCastNeeded(MethodDescriptor methodDescriptor) {
    return methodDescriptor.isPolymorphic()
        // Native, overlays and default methods (declared in the interface) only end up being called
        // via a static dispatch, hence they can use the more specific receiver type.
        && !methodDescriptor.isNative()
        && !methodDescriptor.isJsOverlay()
        && !methodDescriptor.isDefaultMethod();
  }

  private static List<Variable> collectLocals(Method method) {
    List<Variable> locals = new ArrayList<>();
    method
        .getBody()
        .accept(
            new AbstractVisitor() {
              @Override
              public void exitVariable(Variable variable) {
                locals.add(variable);
              }
            });
    return locals;
  }

  private void renderTypeStruct(Type type) {
    emitWasmStruct(type, environment::getWasmTypeName, () -> renderTypeFields(type));
  }

  private void renderTypeFields(Type type) {
    // The first field is always the vtable for class dynamic dispatch.
    builder.newLine();
    builder.append(
        format(
            "(field $vtable (ref %s))",
            environment.getWasmVtableTypeName(type.getTypeDescriptor())));
    // The second field is always the itable for interface method dispatch.
    builder.newLine();
    builder.append(
        format(
            "(field $itable (ref %s))", environment.getWasmItableTypeName(type.getDeclaration())));

    WasmTypeLayout wasmType = environment.getWasmTypeLayout(type.getDeclaration());
    for (FieldDescriptor fieldDescriptor : wasmType.getAllInstanceFields()) {
      builder.newLine();
      String fieldType = environment.getWasmFieldType(fieldDescriptor.getTypeDescriptor());

      // TODO(b/296475021): Cleanup the handling of the arrays elements field.
      if (!environment.isWasmArrayElementsField(fieldDescriptor)) {
        fieldType = format("(mut %s)", fieldType);
      }
      builder.append(format("(field %s %s)", environment.getFieldName(fieldDescriptor), fieldType));
    }
  }

  /**
   * Emit a function that will be used to initialize the runtime at module instantiation time;
   * together with the required type definitions.
   */
  void emitDispatchTablesInitialization(Library library) {
    builder.newLine();
    // TODO(b/183994530): Initialize dynamic dispatch tables lazily.
    builder.append(";;; Initialize dynamic dispatch tables.");

    emitEmptyItableGlobal();
    emitClassDispatchTables(library, /* emitItableInitialization= */ true);
  }

  public void emitEmptyItableGlobal() {
    // Emit an empty itable that will be used for types that don't implement any interface.
    builder.newLine();
    builder.append("(global $itable.empty (ref $itable)");
    builder.indent();
    builder.newLine();
    builder.append("(struct.new_default $itable)");
    builder.unindent();
    builder.newLine();
    builder.append(")");
  }

  void emitClassDispatchTables(Library library, boolean emitItableInitialization) {
    // Populate all vtables.
    library
        .streamTypes()
        .filter(not(Type::isInterface))
        .filter(not(Type::isNative))
        .map(Type::getDeclaration)
        .filter(not(TypeDeclaration::isAbstract))
        .filter(type -> type.getWasmInfo() == null)
        .filter(not(AstUtils::isNonNativeJsEnum))
        .forEach(
            t -> {
              emitVtablesInitialization(t);
              if (emitItableInitialization) {
                emitItableInitialization(t);
              }
            });
    builder.newLine();
  }

  /** Emits the code to initialize the class vtable structure for {@code typeDeclaration}. */
  private void emitVtablesInitialization(TypeDeclaration typeDeclaration) {
    WasmTypeLayout wasmTypeLayout = environment.getWasmTypeLayout(typeDeclaration);

    emitBeginCodeComment(typeDeclaration, "vtable.init");
    builder.newLine();
    //  Create the class vtable for this type (which is either a class or an enum) and store it
    // in a global variable to be able to use it to initialize instance of this class.
    builder.append(
        format(
            "(global %s (ref %s)",
            environment.getWasmVtableGlobalName(typeDeclaration),
            environment.getWasmVtableTypeName(typeDeclaration)));
    builder.indent();
    emitVtableInitialization(typeDeclaration, wasmTypeLayout.getAllPolymorphicMethods());
    builder.unindent();
    builder.newLine();
    builder.append(")");

    typeDeclaration
        .getAllSuperInterfaces()
        .forEach(
            i -> {
              builder.newLine();
              builder.append(
                  format(
                      "(global %s (ref %s)",
                      environment.getWasmInterfaceVtableGlobalName(i, typeDeclaration),
                      environment.getWasmVtableTypeName(i)));
              builder.indent();
              initializeInterfaceVtable(wasmTypeLayout, i);
              builder.unindent();
              builder.newLine();
              builder.append(")");
            });

    emitEndCodeComment(typeDeclaration, "vtable.init");
  }

  /** Emits the code to initialize the Itable array for {@code typeDeclaration}. */
  private void emitItableInitialization(TypeDeclaration typeDeclaration) {
    if (!typeDeclaration.implementsInterfaces()) {
      return;
    }
    emitBeginCodeComment(typeDeclaration, "itable.init");

    // Create the struct of interface vtables of the required size and store it in a global variable
    // to be able to use it when objects of this class are instantiated.
    TypeDeclaration[] interfacesByItableIndex = getInterfacesByItableIndex(typeDeclaration);
    // Emit globals for each interface vtable

    builder.newLine();
    builder.append(
        format(
            "(global %s (ref %s)",
            environment.getWasmItableGlobalName(typeDeclaration),
            environment.getWasmItableTypeName(typeDeclaration)));
    builder.indent();
    builder.newLine();
    builder.append(format("(struct.new %s", environment.getWasmItableTypeName(typeDeclaration)));
    builder.indent();
    stream(interfacesByItableIndex)
        .forEach(
            i -> {
              builder.newLine();
              if (i == null) {
                builder.append(" (ref.null struct)");
                return;
              }
              builder.append(
                  format(
                      " (global.get %s)",
                      environment.getWasmInterfaceVtableGlobalName(i, typeDeclaration)));
            });
    builder.unindent();
    builder.newLine();
    builder.append(")");
    builder.unindent();
    builder.newLine();
    builder.append(")");
    emitEndCodeComment(typeDeclaration, "itable.init");
  }

  private TypeDeclaration[] getInterfacesByItableIndex(TypeDeclaration typeDeclaration) {
    Set<TypeDeclaration> superInterfaces = typeDeclaration.getAllSuperInterfaces();

    // Compute the itable for this type.
    int numSlots = environment.getItableSize();
    TypeDeclaration[] interfacesByItableIndex = new TypeDeclaration[numSlots];
    superInterfaces.forEach(
        superInterface ->
            interfacesByItableIndex[environment.getItableIndexForInterface(superInterface)] =
                superInterface);
    return interfacesByItableIndex;
  }

  /** Emits a specialized itable type for this type to allow for better optimizations. */
  private void emitItableType(
      TypeDeclaration typeDeclaration, TypeDeclaration[] interfacesByItableIndex) {
    // Create the specialized struct for the itable for this type. A specialized itable type will
    // be a subtype of the specialized itable type for its superclass. Note that the struct fields
    // get incrementally specialized in this struct in the subclasses as the interfaces are
    // implemented by them.
    builder.newLine();
    builder.append(
        format(
            "(type %s (sub %s (struct",
            environment.getWasmItableTypeName(typeDeclaration),
            environment.getWasmItableTypeName(typeDeclaration.getSuperTypeDeclaration())));
    builder.indent();
    for (int index = 0; index < environment.getItableSize(); index++) {
      builder.newLine();
      if (interfacesByItableIndex[index] == null) {
        // This type does not use the struct, so it is kept at the generic struct type.
        builder.append("(field (ref null struct))");
      } else {
        builder.append(
            format(
                "(field (ref %s))",
                environment.getWasmVtableTypeName(interfacesByItableIndex[index])));
      }
    }
    builder.unindent();
    builder.newLine();
    builder.append(")))");
  }

  private void initializeInterfaceVtable(
      WasmTypeLayout wasmTypeLayout, TypeDeclaration interfaceDeclaration) {
    WasmTypeLayout interfaceTypeLayout = environment.getWasmTypeLayout(interfaceDeclaration);
    ImmutableList<MethodDescriptor> interfaceMethodImplementations =
        interfaceTypeLayout.getAllPolymorphicMethodsByMangledName().values().stream()
            .map(wasmTypeLayout::getImplementationMethod)
            .collect(toImmutableList());
    emitVtableInitialization(interfaceDeclaration, interfaceMethodImplementations);
  }

  public void emitItableInterfaceGetters(Library library) {
    library
        .streamTypes()
        .filter(Type::isInterface)
        .map(Type::getDeclaration)
        .forEach(this::emitItableInterfaceGetter);
  }

  private void emitItableInterfaceGetter(TypeDeclaration typeDeclaration) {
    int fieldIndex = environment.getItableIndexForInterface(typeDeclaration);
    emitItableInterfaceGetter(
        environment.getWasmItableInterfaceGetter(typeDeclaration),
        fieldIndex == -1 ? null : String.valueOf(fieldIndex));
  }

  public void emitItableInterfaceGetter(String methodName, String fieldName) {
    builder.newLine();
    builder.append(
        format(
            "(func %s (param $object (ref null $java.lang.Object)) (result (ref null struct)) ",
            methodName));
    builder.indent();
    builder.newLine();
    if (fieldName == null) {
      // There is no need to assign a field to interfaces that are not implemented by any class. In
      // that case just return null to comply with the semantics of casts and instanceofs.
      builder.append("(ref.null struct)");
    } else {
      builder.append(
          format(
              "(struct.get $itable %s (struct.get $java.lang.Object $itable (local.get $object)))",
              fieldName));
    }
    builder.unindent();
    builder.newLine();
    builder.append(")");
  }

  /**
   * Creates and initializes the vtable for {@code implementedType} with the methods in {@code
   * methodDescriptors}.
   *
   * <p>This is used to initialize both class vtables and interface vtables. Each concrete class
   * will have a class vtable to implement the dynamic class method dispatch and one vtable for each
   * interface it implements to implement interface dispatch.
   */
  private void emitVtableInitialization(
      TypeDeclaration implementedType, Collection<MethodDescriptor> methodDescriptors) {
    builder.newLine();
    // Create an instance of the vtable for the type initializing it with the methods that are
    // passed in methodDescriptors.
    builder.append(format("(struct.new %s", environment.getWasmVtableTypeName(implementedType)));

    builder.indent();
    methodDescriptors.forEach(
        m -> {
          builder.newLine();
          builder.append(format("(ref.func %s)", environment.getMethodImplementationName(m)));
        });
    builder.unindent();
    builder.newLine();
    builder.append(")");
  }

  /** Emits a Wasm struct using nominal inheritance. */
  private void emitWasmStruct(
      Type type, Function<DeclaredTypeDescriptor, String> structNamer, Runnable fieldsRenderer) {
    boolean hasSuperType = type.getSuperTypeDescriptor() != null;
    builder.newLine();
    builder.append(String.format("(type %s (sub ", structNamer.apply(type.getTypeDescriptor())));
    if (hasSuperType) {
      builder.append(format("%s ", structNamer.apply(type.getSuperTypeDescriptor())));
    }
    builder.append("(struct");
    builder.indent();
    fieldsRenderer.run();

    builder.newLine();
    builder.append(")");
    builder.append(")");
    builder.unindent();
    builder.newLine();
    builder.append(")");
  }

  void emitNativeArrayTypes(List<ArrayTypeDescriptor> arrayTypes) {
    emitBeginCodeComment("Native Array types");
    arrayTypes.forEach(this::emitNativeArrayType);
    emitEndCodeComment("Native Array types");
  }

  void emitNativeArrayType(ArrayTypeDescriptor arrayTypeDescriptor) {
    String wasmArrayTypeName = environment.getWasmTypeName(arrayTypeDescriptor);
    builder.newLine();
    builder.append(
        format(
            "(type %s (array (mut %s)))",
            wasmArrayTypeName,
            environment.getWasmFieldType(arrayTypeDescriptor.getComponentTypeDescriptor())));
  }

  void emitEmptyArraySingletons(List<ArrayTypeDescriptor> arrayTypes) {
    emitBeginCodeComment("Empty array singletons");
    arrayTypes.forEach(this::emitEmptyArraySingleton);
    emitEndCodeComment("Empty array singletons");
  }

  void emitEmptyArraySingleton(ArrayTypeDescriptor arrayTypeDescriptor) {
    String wasmArrayTypeName = environment.getWasmTypeName(arrayTypeDescriptor);
    // Emit a global empty array singleton to avoid allocating empty arrays. */
    builder.newLine();
    builder.append(
        format(
            "(global %s (ref %s)",
            environment.getWasmEmptyArrayGlobalName(arrayTypeDescriptor), wasmArrayTypeName));
    builder.indent();
    builder.newLine();
    builder.append(format("(array.new_default %s (i32.const 0))", wasmArrayTypeName));
    builder.unindent();
    builder.newLine();
    builder.append(")");
    builder.newLine();
  }

  /**
   * Iterate through all the types in the library, supertypes first, calling the emitter for each of
   * them.
   */
  void emitForEachType(Library library, Consumer<Type> emitter, String comment) {
    library
        .streamTypes()
        .filter(t -> !AstUtils.isNonNativeJsEnum(t.getTypeDescriptor()))
        // Emit the types supertypes first.
        .sorted(Comparator.comparing(t -> t.getDeclaration().getTypeHierarchyDepth()))
        .forEach(
            type -> {
              emitBeginCodeComment(type, comment);
              emitter.accept(type);
              emitEndCodeComment(type, comment);
            });
  }

  private void emitBeginCodeComment(Type type, String section) {
    emitBeginCodeComment(type.getDeclaration(), section);
  }

  private void emitBeginCodeComment(TypeDeclaration typeDeclaration, String section) {
    emitBeginCodeComment(format("%s [%s]", typeDeclaration.getQualifiedSourceName(), section));
  }

  private void emitBeginCodeComment(String commentId) {
    builder.newLine();
    builder.append(";;; Code for " + commentId);
  }

  private void emitEndCodeComment(Type type, String section) {
    emitEndCodeComment(type.getDeclaration(), section);
  }

  private void emitEndCodeComment(TypeDeclaration typeDeclaration, String section) {
    emitEndCodeComment(format("%s [%s]", typeDeclaration.getQualifiedSourceName(), section));
  }

  private void emitEndCodeComment(String commentId) {
    builder.newLine();
    builder.append(";;; End of code for " + commentId);
  }
}
