/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.generator;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.TypeVariable;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.common.FilePosition;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.generator.ImportGatherer.ImportCategory;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Generates JavaScript source impl files. */
public class JavaScriptImplGenerator extends JavaScriptGenerator {
  private NativeJavaScriptFile nativeSource;
  private final ClosureTypesGenerator closureTypesGenerator;

  protected final StatementTranspiler statementTranspiler;

  public static final String FILE_SUFFIX = ".impl.java.js";

  public JavaScriptImplGenerator(Problems problems, boolean declareLegacyNamespace, Type type) {
    super(problems, declareLegacyNamespace, type);
    this.statementTranspiler = new StatementTranspiler(sourceBuilder, environment);
    this.closureTypesGenerator = new ClosureTypesGenerator(environment);
  }

  private static String getMethodQualifiers(MethodDescriptor methodDescriptor) {
    String staticQualifier = methodDescriptor.isStatic() ? "static " : "";
    if (!methodDescriptor.isAbstract() && methodDescriptor.isJsAsync()) {
      // Do not emit the "async" modifier for abstract methods since jscompiler will emit
      // a warning due to the way async are transpiled down.
      return staticQualifier + "async ";
    }
    return staticQualifier;
  }

  /**
   * Emits the method header including (static) (async) (getter/setter) methodName(parametersList).
   */
  private void emitMethodHeader(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    sourceBuilder.append(getMethodQualifiers(methodDescriptor));
    sourceBuilder.emitWithMapping(
        method.getSourcePosition(),
        () -> sourceBuilder.append(ManglingNameUtils.getMangledName(methodDescriptor)));
    sourceBuilder.append("(");
    String separator = "";
    Variable varargsParameter = method.getJsVarargsParameter();
    for (Variable parameter : method.getParameters()) {
      sourceBuilder.append(separator);
      if (parameter == varargsParameter) {
        sourceBuilder.append("...");
      }
      sourceBuilder.emitWithMapping(
          // Only map parameters if they are named.
          AstUtils.emptySourcePositionIfNotNamed(parameter.getSourcePosition()),
          () -> sourceBuilder.append(environment.getUniqueNameForVariable(parameter)));
      separator = ", ";
    }
    sourceBuilder.append(") ");
  }

  @Override
  public String getSuffix() {
    return FILE_SUFFIX;
  }

  public void setNativeSource(NativeJavaScriptFile nativeSource) {
    this.nativeSource = checkNotNull(nativeSource);
  }

  @Override
  public String renderOutput() {
    try {
      renderImports();
      if (type.getDeclaration().isJsEnum()) {
        // TODO(b/117150539): Decide if native.js files are allowed on JsEnum or not, and implement
        // accordingly.
        renderClosureEnum();
      } else {
        renderClass();
      }
      renderExports();
      return sourceBuilder.build();
    } catch (RuntimeException e) {
      // Catch all unchecked exceptions and rethrow them with more context to make debugging easier.
      // Yes this is really being done on purpose.
      throw new RuntimeException(
          "Error generating source for type " + type.getDeclaration().getQualifiedBinaryName(), e);
    }
  }

  private void renderClosureEnum() {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    sourceBuilder.appendLines(
        "/**",
        " * @enum {"
            + closureTypesGenerator.getClosureTypeString(
                AstUtils.getJsEnumValueFieldType(typeDeclaration))
            + "}");
    sourceBuilder.newLine();
    if (type.getDeclaration().isDeprecated()) {
      sourceBuilder.appendln(" * @deprecated");
    }
    sourceBuilder.appendln(" */");
    sourceBuilder.append("const ");
    sourceBuilder.emitWithMapping(
        type.getSourcePosition(),
        () -> sourceBuilder.append(environment.aliasForType(typeDeclaration)));
    sourceBuilder.append(" = ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    for (Field field : type.getStaticFields()) {
      sourceBuilder.emitWithMemberMapping(
          field,
          () -> {
            if (field.getDescriptor().isDeprecated()) {
              sourceBuilder.appendln(" /** @deprecated */");
            }
            sourceBuilder.emitWithMapping(
                field.getSourcePosition(),
                () -> {
                  sourceBuilder.append(ManglingNameUtils.getMangledName(field.getDescriptor()));
                });
            sourceBuilder.append(" : ");
            renderExpression(field.getInitializer());
            sourceBuilder.append(",");
            sourceBuilder.newLine();
          });
    }
    sourceBuilder.closeBrace();
    sourceBuilder.append(";");
    sourceBuilder.newLines(2);
  }

  public void renderClass() {
    renderTypeAnnotation();
    renderClassBody();
    renderClassMetadata();
    renderStaticFieldDeclarations();
    renderMarkImplementorCalls();
    renderNativeSource();
  }

  private void renderImports() {
    TypeDeclaration typeDeclaration = type.getDeclaration();

    // goog.module(...) declaration.
    sourceBuilder.appendln("goog.module('" + typeDeclaration.getImplModuleName() + "');");
    sourceBuilder.newLines(2);

    // goog.require(...) for eager imports.
    Map<String, String> aliasesByPath = new HashMap<>();
    Iterable<Import> eagerImports = sortImports(importsByCategory.get(ImportCategory.LOADTIME));
    for (Import eagerImport : eagerImports) {
      String alias = eagerImport.getAlias();
      String path = eagerImport.getImplModulePath();
      String previousAlias = aliasesByPath.get(path);
      if (previousAlias == null) {
        sourceBuilder.appendln("const " + alias + " = goog.require('" + path + "');");
        aliasesByPath.put(path, alias);
      } else {
        // Do not goog.require second time to avoid JsCompiler warnings.
        sourceBuilder.appendln("const " + alias + " = " + previousAlias + ";");
      }
    }
    if (!Iterables.isEmpty(eagerImports)) {
      sourceBuilder.newLine();
    }

    // goog.forwardDeclare(...) for lazy imports.
    Iterable<Import> lazyImports =
        sortImports(
            Iterables.concat(
                importsByCategory.get(ImportCategory.RUNTIME),
                importsByCategory.get(ImportCategory.JSDOC)));
    for (Import lazyImport : lazyImports) {
      String alias = lazyImport.getAlias();
      String path = lazyImport.getImplModulePath();
      sourceBuilder.appendln("let " + alias + " = goog.forwardDeclare('" + path + "');");
    }
    if (!Iterables.isEmpty(lazyImports)) {
      sourceBuilder.newLine();
    }

    sourceBuilder.newLine();
  }

  private void renderTypeAnnotation() {
    if (type.isJsOverlayImplementation()) {
      // Do nothing.
    } else if (type.isInterface()) {
      sourceBuilder.appendLines("/**", " * @interface");
      sourceBuilder.newLine();
      if (type.getDeclaration().hasTypeParameters()) {
        String templates =
            closureTypesGenerator.getCommaSeparatedClosureTypesString(
                type.getDeclaration().getTypeParameterDescriptors());
        sourceBuilder.appendln(" * @template " + templates);
      }
      for (DeclaredTypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
        renderIfClassExists(" * @extends {%s}", superInterfaceType, sourceBuilder);
      }
      if (type.getDeclaration().isDeprecated()) {
        sourceBuilder.appendln(" * @deprecated");
      }
      sourceBuilder.appendln(" */");
    } else { // Not an interface so it is a Class.
      SourceBuilder buffer = new SourceBuilder();
      if (type.isAbstract()) {
        buffer.appendln(" * @abstract");
      }
      if (type.getDeclaration().hasTypeParameters()) {
        String templates =
            closureTypesGenerator.getCommaSeparatedClosureTypesString(
                type.getDeclaration().getTypeParameterDescriptors());
        buffer.appendln(" * @template " + templates);
      }
      if (type.getDeclaration().isDeprecated()) {
        buffer.appendln(" * @deprecated");
      }
      if (type.getSuperTypeDescriptor() != null
          && type.getSuperTypeDescriptor().hasTypeArguments()) {
        // No need to render if it does not have type arguments as it will also appear in the
        // extends clause of the class definition.
        renderIfClassExists(" * @extends {%s}", type.getSuperTypeDescriptor(), buffer);
      }
      for (DeclaredTypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
        renderIfClassExists(" * @implements {%s}", superInterfaceType, buffer);
      }

      String annotation = buffer.build();
      if (!annotation.isEmpty()) {
        sourceBuilder.appendln("/**");
        sourceBuilder.append(annotation);
        sourceBuilder.appendln(" */");
      }
    }
  }

  /**
   * Renders the line using {@code formatString} only if {@code typeDescriptor} is an actual class
   * in JavaScript.
   *
   * <p>Used to render the @extends/@implements clauses.
   */
  private void renderIfClassExists(
      String formatString, DeclaredTypeDescriptor typeDescriptor, SourceBuilder sourceBuilder) {
    if (doesClassExistInJavaScript(typeDescriptor)) {
      String typeArgumentsString =
          typeDescriptor.hasTypeArguments()
              ? typeDescriptor.getTypeArgumentDescriptors().stream()
                  .map(td -> closureTypesGenerator.getClosureTypeString(td))
                  .collect(Collectors.joining(", ", "<", ">"))
              : "";

      sourceBuilder.appendln(
          String.format(
              formatString,
              environment.aliasForType(typeDescriptor.getTypeDeclaration()) + typeArgumentsString));
    }
  }

  private boolean doesClassExistInJavaScript(DeclaredTypeDescriptor type) {
    return !type.getTypeDeclaration().isStarOrUnknown() && !type.isJsFunctionInterface();
  }

  private void renderClassBody() {
    sourceBuilder.append("class ");
    sourceBuilder.emitWithMapping(
        type.getSourcePosition(),
        () -> sourceBuilder.append(environment.aliasForType(type.getDeclaration())));
    sourceBuilder.append(" " + getExtendsClause(type, environment));
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    renderTypeMethods();
    renderMarkImplementorMethod();
    renderIsInstanceMethod();
    renderIsAssignableFromMethod();
    renderCopyMethod();
    renderLoadModules();
    sourceBuilder.closeBrace();
    sourceBuilder.append(";");
    sourceBuilder.newLines(2);
  }

  private static String getExtendsClause(Type type, GenerationEnvironment environment) {
    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor == null || superTypeDescriptor.getTypeDeclaration().isStarOrUnknown()) {
      return "";
    }
    String superTypeName = environment.aliasForType(superTypeDescriptor);
    return String.format("extends %s ", superTypeName);
  }

  private void renderTypeMethods() {
    for (Method method : type.getMethods()) {
      sourceBuilder.emitWithMemberMapping(
          method,
          () -> {
            if (method.isNative()) {
              // If the method is native, output JsDoc comments so it can serve as a template for
              // native.js. However if the method is pointing to a different namespace then there
              // is no point on doing that since it cannot be provided via a native.js file.
              if (method.getDescriptor().hasJsNamespace()) {
                return;
              }
              renderMethodJsDoc(method);
              sourceBuilder.append("// native ");
              emitMethodHeader(method);
            } else {

              renderMethodJsDoc(method);
              emitMethodHeader(method);
              statementTranspiler.renderStatement(method.getBody());
            }
            sourceBuilder.newLines(2);
          });
    }
  }

  private void renderMethodJsDoc(Method method) {
    sourceBuilder.appendln("/**");
    if (method.getJsDocDescription() != null && !method.getJsDocDescription().isEmpty()) {
      sourceBuilder.appendln(" * " + method.getJsDocDescription());
    }
    if (method.isAbstract()) {
      sourceBuilder.appendln(" * @abstract");
    }
    if (method.isOverride() && !method.isConstructor()) {
      sourceBuilder.appendln(" * @override");
    }
    MethodDescriptor methodDescriptor = method.getDescriptor();
    if (!methodDescriptor.getTypeParameterTypeDescriptors().isEmpty()) {
      String templateParamNames =
          closureTypesGenerator.getCommaSeparatedClosureTypesString(
              methodDescriptor.getTypeParameterTypeDescriptors());
      sourceBuilder.appendln(" * @template " + templateParamNames);
    }

    if (type.getDeclaration().isJsFunctionImplementation()
        && methodDescriptor.isPolymorphic()
        && !method.getBody().getStatements().isEmpty()) {
      // TODO(b/120800425): Solve the object<->function duality in JsFunction implementations in a
      // more principled way.
      if (methodDescriptor.getName().startsWith("$ctor")) {
        // ctor treats the JsFunction implementation as a class to invoke java.lang.Object ctor
        // method. Do not redefine @this, instead suppress invalid casts to allow casting this to
        // a function if needed.
        sourceBuilder.appendln(" * @suppress {invalidCasts}");
      } else {
        // Using @this redefines enclosing class of a method, hence any template variables defined
        // in the class need to be declared in the method.
        for (TypeVariable typeVariable : type.getDeclaration().getTypeParameterDescriptors()) {
          sourceBuilder.appendln(
              " * @template " + closureTypesGenerator.getClosureTypeString(typeVariable));
        }
        sourceBuilder.appendln(
            " * @this {"
                + closureTypesGenerator.getClosureTypeString(type.getTypeDescriptor())
                + "}");
      }
    }
    for (int i = 0; i < method.getParameters().size(); i++) {
      String parameterName = environment.getUniqueNameForVariable(method.getParameters().get(i));
      sourceBuilder.appendln(
          " * @param {"
              + closureTypesGenerator.getJsDocForParameter(method, i)
              + "} "
              + parameterName);
    }
    String returnTypeName =
        closureTypesGenerator.getClosureTypeString(methodDescriptor.getReturnTypeDescriptor());
    if (!methodDescriptor.isConstructor() && needsReturnJsDoc(methodDescriptor)) {
      sourceBuilder.appendln(" * @return {" + returnTypeName + "}");
    }
    sourceBuilder.appendln(" * @" + methodDescriptor.getJsVisibility().jsText);
    if (methodDescriptor.isDeprecated()) {
      sourceBuilder.appendln(" * @deprecated");
    }
    sourceBuilder.appendln(" */");
  }

  private boolean needsReturnJsDoc(MethodDescriptor methodDescriptor) {
    return !TypeDescriptors.isPrimitiveVoid(methodDescriptor.getReturnTypeDescriptor())
        // If there are no @param and no @return, if there is @template jscompiler emits
        // a generic MISPLACED_ANNOTATION error.
        || (!methodDescriptor.getTypeParameterTypeDescriptors().isEmpty()
            && methodDescriptor.getParameterDescriptors().isEmpty());
  }

  private void renderMarkImplementorMethod() {
    if (!type.isInterface() || type.isJsOverlayImplementation()) {
      return; // Only render markImplementor code for interfaces.
    }
    sourceBuilder.appendLines(
        "/**",
        " * @param {"
            + TypeDescriptors.get().nativeFunction.getQualifiedJsName()
            + "} classConstructor",
        " * @public",
        " */",
        "static $markImplementor(classConstructor) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    for (DeclaredTypeDescriptor superInterface : type.getSuperInterfaceTypeDescriptors()) {
      if (superInterface.isNative()) {
        continue;
      }
      String superInterfaceName = environment.aliasForType(superInterface);
      sourceBuilder.appendln(superInterfaceName + ".$markImplementor(classConstructor);");
    }
    sourceBuilder.appendLines(
        "/**",
        " * @public {boolean}",
        " */",
        "classConstructor.prototype.$implements__"
            + ManglingNameUtils.getMangledName(type.getTypeDescriptor())
            + " = true;");
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO(b/34928687): Move this to the ast in a normalization pass.
  private void renderIsInstanceMethod() {
    if (type.isJsOverlayImplementation()
        && type.getOverlaidTypeDescriptor().isJsFunctionInterface()) {
      // JsFunction interface overlays do not need $isInstance.
      return;
    }
    if (type.containsMethod(MethodDescriptor.IS_INSTANCE_METHOD_NAME)) {
      sourceBuilder.appendLines(
          "/**", " * $isInstance() function implementation is provided separately.", " */");
      sourceBuilder.newLine();
      return;
    }
    sourceBuilder.appendLines(
        "/**",
        " * @param {?} instance",
        " * @return {boolean}",
        " * @public",
        " */",
        "static $isInstance(instance) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    if (type.getDeclaration().isJsFunctionImplementation()) {
      renderIsInstanceOfJsFunctionImplementationStatement(type.getTypeDescriptor());
    } else if (type.isJsOverlayImplementation()) {
      DeclaredTypeDescriptor overlaidTypeDescriptor = type.getOverlaidTypeDescriptor();
      if (overlaidTypeDescriptor.isJsEnum()) {
        renderIsInstanceOfJsEnumStatement(overlaidTypeDescriptor);
      } else if (overlaidTypeDescriptor.isInterface()) {
        // Since instanceof is forbidden this is only used for casting so null check is not needed.
        sourceBuilder.append("return true;");
      } else {
        renderIsInstanceOfClassStatement(type.getOverlaidTypeDescriptor());
      }
    } else if (type.isInterface()) {
      renderIsInstanceOfInterfaceStatement(type.getTypeDescriptor());
    } else {
      renderIsInstanceOfClassStatement(type.getTypeDescriptor());
    }
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  private void renderIsInstanceOfJsEnumStatement(DeclaredTypeDescriptor typeDescriptor) {
    if (AstUtils.isNonNativeJsEnum(typeDescriptor)) {
      sourceBuilder.append(
          "return "
              + environment.aliasForType(BootstrapType.ENUMS.getDeclaration())
              + ".isInstanceOf(instance, "
              + environment.aliasForType(typeDescriptor.getMetadataTypeDeclaration())
              + ");");
    } else if (typeDescriptor.getJsEnumInfo().hasCustomValue()) {
      DeclaredTypeDescriptor instanceOfValueType =
          AstUtils.getJsEnumValueFieldInstanceCheckType(typeDescriptor.getTypeDeclaration());
      sourceBuilder.append(
          "return "
              + environment.aliasForType(instanceOfValueType.getTypeDeclaration())
              + ".$isInstance(instance);");

    } else {
      // Native JsEnum of unknown value type.
      sourceBuilder.append("return true;");
    }
  }

  private void renderIsInstanceOfClassStatement(DeclaredTypeDescriptor typeDescriptor) {
    sourceBuilder.append(
        "return instance instanceof " + environment.aliasForType(typeDescriptor) + ";");
  }

  private void renderIsInstanceOfInterfaceStatement(DeclaredTypeDescriptor typeDescriptor) {
    sourceBuilder.append(
        "return instance != null && !!instance.$implements__"
            + ManglingNameUtils.getMangledName(typeDescriptor)
            + ";");
  }

  private void renderIsInstanceOfJsFunctionImplementationStatement(
      DeclaredTypeDescriptor typeDescriptor) {
    sourceBuilder.appendln(
        "return instance != null && !!instance.$is__"
            + ManglingNameUtils.getMangledName(typeDescriptor)
            + ";");
  }

  // TODO(b/34928687): Move this to the ast in a normalization pass.
  private void renderIsAssignableFromMethod() {
    if (type.isJsOverlayImplementation()
        || type.containsMethod(MethodDescriptor.IS_ASSIGNABLE_FROM_METHOD_NAME)) {
      return; // Don't render for overlay types or if the method exists.
    }
    sourceBuilder.appendLines(
        "/**",
        " * @param {"
            + TypeDescriptors.get().nativeFunction.getQualifiedJsName()
            + "} classConstructor",
        " * @return {boolean}",
        " * @public",
        " */",
        "static $isAssignableFrom(classConstructor) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();

    if (type.isInterface()) { // For interfaces
      sourceBuilder.append(
          "return classConstructor != null && !!classConstructor.prototype.$implements__"
              + ManglingNameUtils.getMangledName(type.getTypeDescriptor())
              + ";");
    } else { // For classes
      BootstrapType.NATIVE_UTIL.getDescriptor();
      String utilAlias = environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());
      sourceBuilder.append(
          "return "
              + utilAlias
              + ".$canCastClass(classConstructor, "
              + environment.aliasForType(type.getDeclaration())
              + ");");
    }
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO(b/34928687): Move this to the ast in a normalization pass.
  // TODO(b/80269359): may copy Objects methods (equals, hashCode, etc. ) as well.
  private void renderCopyMethod() {
    if (!type.getDeclaration().isJsFunctionImplementation()) {
      return; // Only render the $copy method for jsfunctions
    }
    sourceBuilder.appendLines(
        "/**",
        " * @param {" + environment.aliasForType(type.getDeclaration()) + "} from",
        " * @param {?} to",
        " * @public",
        " */",
        "static $copy(from, to) ");
    sourceBuilder.openBrace();
    for (Field field : type.getInstanceFields()) {
      String fieldName = ManglingNameUtils.getMangledName(field.getDescriptor());
      sourceBuilder.newLine();
      sourceBuilder.append("to." + fieldName + " = from." + fieldName + ";");
    }
    sourceBuilder.newLine();
    sourceBuilder.appendLines(
        "// Marks the object is an instance of this class.",
        "to.$is__" + ManglingNameUtils.getMangledName(type.getTypeDescriptor()) + " = true;");
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO(b/67965153): Move this to the ast in a normalization pass.
  private void renderClassMetadata() {
    if (type.isJsOverlayImplementation()
        && (type.getOverlaidTypeDescriptor().isJsFunctionInterface()
            || type.getOverlaidTypeDescriptor().isInterface())) {
      // JsFunction and Native interface overlays do not need class metadata.
      sourceBuilder.newLines(2);
      return;
    }

    String utilAlias = environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());

    TypeDeclaration targetTypeDescriptor =
        type.isJsOverlayImplementation()
            ? type.getOverlaidTypeDescriptor().getTypeDeclaration()
            : type.getDeclaration();

    String name =
        targetTypeDescriptor.isNative()
            // For native types the qualified JavaScript name is more useful to identify the
            // type, in particular for debugging.
            ? targetTypeDescriptor.getQualifiedJsName()
            : targetTypeDescriptor.getQualifiedBinaryName();

    String obfuscatableName = "'" + name + "'";
    String className = environment.aliasForType(type.getDeclaration());
    if (targetTypeDescriptor.isInterface()) {
      sourceBuilder.appendln(
          utilAlias
              + ".$setClassMetadataForInterface("
              + className
              + ", "
              + obfuscatableName
              + ");");
    } else if (targetTypeDescriptor.isEnum() && !targetTypeDescriptor.isJsEnum()) {
      // TODO(b/117525773): targetTypeDescriptor.isEnum should already be false for JsEnums,
      // making the second part of the condition unnecessary.
      sourceBuilder.appendln(
          utilAlias + ".$setClassMetadataForEnum(" + className + ", " + obfuscatableName + ");");
    } else {
      sourceBuilder.appendln(
          utilAlias + ".$setClassMetadata(" + className + ", " + obfuscatableName + ");");
    }
    sourceBuilder.newLines(2);
  }

  private void renderLoadModules() {
    MethodDescriptor methodDescriptor = AstUtils.getLoadModulesDescriptor(type.getTypeDescriptor());
    sourceBuilder.appendLines(
        "/**",
        " * @public",
        " */",
        "static " + ManglingNameUtils.getMangledName(methodDescriptor) + "() ");
    sourceBuilder.openBrace();

    // goog.module.get(...) for lazy imports.
    for (Import lazyImport : sortImports(importsByCategory.get(ImportCategory.RUNTIME))) {
      String alias = lazyImport.getAlias();
      String path = lazyImport.getImplModulePath();
      sourceBuilder.newLine();
      sourceBuilder.append(alias + " = goog.module.get('" + path + "');");
    }

    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  private void renderStaticFieldDeclarations() {
    for (Field staticField : type.getStaticFields()) {
      sourceBuilder.emitWithMemberMapping(
          staticField,
          () -> {
            statementTranspiler.renderStatement(
                AstUtils.declarationStatement(staticField, type.getSourcePosition()));
            // emit 2 empty lines
            sourceBuilder.newLines(3);
          });
    }
  }

  /**
   * Here we call markImplementor on all interfaces such that the class can be queried at run time
   * to determine if it implements an interface.
   */
  private void renderMarkImplementorCalls() {
    if (type.isJsOverlayImplementation()) {
      return; // Do nothing
    }

    String className = environment.aliasForType(type.getDeclaration());
    if (type.isInterface()) {
      // TODO(b/20102666): remove cast after b/20102666 is handled in Closure.
      sourceBuilder.appendln(
          className
              + ".$markImplementor(/** @type {"
              + TypeDescriptors.get().nativeFunction.getQualifiedJsName()
              + "} */ ("
              + className
              + "));");
    } else { // Not an interface so it is a Class.
      for (TypeDescriptor interfaceTypeDescriptor : type.getSuperInterfaceTypeDescriptors()) {
        if (interfaceTypeDescriptor.isNative()) {
          continue;
        }
        JavaScriptConstructorReference markImplementorConstructor =
            interfaceTypeDescriptor.getMetadataConstructorReference();
        renderExpression(markImplementorConstructor);
        sourceBuilder.appendln(".$markImplementor(" + className + ");");
      }
    }
    sourceBuilder.newLines(2);
  }

  private void renderNativeSource() {
    if (nativeSource == null) {
      return;
    }

    String className = environment.aliasForType(type.getDeclaration());
    sourceBuilder.appendln("/* NATIVE.JS EPILOG */");
    sourceBuilder.newLine();

    // A predictable unique alias to current type so native.js could reference it safely.
    String longAliasName =
        type.getDeclaration().getQualifiedSourceName().replace("_", "__").replace('.', '_');
    if (!className.equals(longAliasName)) {
      sourceBuilder.appendln("const " + longAliasName + " = " + className + ";");
      sourceBuilder.newLine();
    }

    int nativeSourceLine = 0;
    int currentByteOffset = 0;
    String content = nativeSource.getContent();
    for (String line : Splitter.on('\n').split(content)) {
      String trimmedLine = CharMatcher.whitespace().trimTrailingFrom(line);

      if (!trimmedLine.isEmpty()) {
        int firstNonWhitespaceColumn = CharMatcher.whitespace().negate().indexIn(trimmedLine);
        sourceBuilder.append(Strings.repeat(" ", firstNonWhitespaceColumn));
        // Only map the trimmed section of the line.
        sourceBuilder.emitWithMapping(
            SourcePosition.newBuilder()
                .setStartFilePosition(
                    FilePosition.newBuilder()
                        .setLine(nativeSourceLine)
                        .setColumn(firstNonWhitespaceColumn)
                        .setByteOffset(currentByteOffset + firstNonWhitespaceColumn)
                        .build())
                .setEndFilePosition(
                    FilePosition.newBuilder()
                        .setLine(nativeSourceLine)
                        .setColumn(trimmedLine.length())
                        .setByteOffset(currentByteOffset + trimmedLine.length())
                        .build())
                .setFilePath(nativeSource.getRelativeFilePath())
                .setName(type.getDeclaration().getQualifiedBinaryName() + ".<native>")
                .build(),
            () -> sourceBuilder.append(trimmedLine.substring(firstNonWhitespaceColumn)));
      }
      sourceBuilder.newLine();
      nativeSourceLine++;
      currentByteOffset += line.length() + 1;
    }
    sourceBuilder.newLine();
  }

  private void renderExports() {
    sourceBuilder.appendLines("exports = " + environment.aliasForType(type.getDeclaration()) + ";");
    // TODO(b/77961191): add a new line once the bug is resolved.
  }

  private void renderExpression(Expression expression) {
    ExpressionTranspiler.render(expression, environment, sourceBuilder);
  }
}
