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

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.AnonymousJavaType;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Positioned;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor.ImportCategory;
import com.google.j2cl.generator.visitors.ImportUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates JavaScript source impl files.
 */
public class JavaScriptImplGenerator extends JavaScriptGenerator {
  private String nativeSource;
  private String relativeSourceMapLocation;

  private String className;
  private String mangledTypeName;

  protected final StatementTranspiler statementTranspiler;

  public JavaScriptImplGenerator(
      Errors errors,
      boolean declareLegacyNamespace,
      JavaType javaType,
      SourceMapBuilder sourceMapBuilder) {
    super(errors, declareLegacyNamespace, javaType);
    this.className = environment.aliasForType(javaType.getDescriptor());
    this.mangledTypeName = ManglingNameUtils.getMangledName(javaType.getDescriptor());
    this.statementTranspiler =
        new StatementTranspiler(sourceBuilder, sourceMapBuilder, environment);
  }

  @Override
  public String getSuffix() {
    return ".impl.js";
  }

  public void setRelativeSourceMapLocation(String relativeSourceMapLocation) {
    Preconditions.checkArgument(relativeSourceMapLocation != null);
    this.relativeSourceMapLocation = relativeSourceMapLocation;
  }

  public void setNativeSource(String nativeSource) {
    Preconditions.checkArgument(nativeSource != null);
    this.nativeSource = nativeSource;
  }

  public String getJsDocName(TypeDescriptor typeDescriptor) {
    return JsDocNameUtils.getJsDocName(typeDescriptor, environment);
  }

  public String getJsDocName(TypeDescriptor typeDescriptor, boolean shouldUseClassName) {
    return JsDocNameUtils.getJsDocName(typeDescriptor, shouldUseClassName, environment);
  }

  public String getJsDocNames(List<TypeDescriptor> typeDescriptors) {
    return JsDocNameUtils.getJsDocNames(typeDescriptors, environment);
  }

  @Override
  public String renderOutput() {
    renderFileOverview();
    renderImports();
    renderTypeAnnotation();
    renderTypeBody();
    renderStaticFieldDeclarations();
    renderMarkImplementorCalls();
    renderNativeSource();
    renderExports();
    renderSourceMapLocation();
    return sourceBuilder.build();
  }

  private void renderFileOverview() {
    String transpiledFrom = javaType.getDescriptor().getRawTypeDescriptor().getBinaryName();
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(" * @fileoverview Impl transpiled from " + transpiledFrom + ".");
    sourceBuilder.appendln(" *");
    sourceBuilder.appendln(
        " * @suppress {suspiciousCode, transitionalSuspiciousCodeWarnings, uselessCode, const,"
            + " missingRequire}");
    sourceBuilder.appendln(" */");
  }

  private void renderImports() {
    TypeDescriptor selfTypeDescriptor = javaType.getDescriptor().getRawTypeDescriptor();
    Import selfImport = new Import(selfTypeDescriptor.getSimpleName(), selfTypeDescriptor);

    // goog.module(...) declaration.
    sourceBuilder.appendln("goog.module('%s');", selfImport.getImplModulePath());
    if (declareLegacyNamespace && javaType.getDescriptor().isJsType()
        && !(javaType instanceof AnonymousJavaType)) {
      // Even if opted into declareLegacyNamespace, this only makes sense for classes that are
      // intended to be accessed from the native JS. Thus we only emit declareLegacyNamespace
      // for non-anonymous JsType classes.
      sourceBuilder.appendln("goog.module.declareLegacyNamespace();");
    }
    sourceBuilder.newLine();
    sourceBuilder.newLine();

    // goog.require(...) for eager imports.
    Map<String, String> aliasesByPath = new HashMap<>();
    List<Import> eagerImports = ImportUtils.sortedList(importsByCategory.get(ImportCategory.EAGER));
    if (!eagerImports.isEmpty()) {
      for (Import eagerImport : eagerImports) {
        String alias = eagerImport.getAlias();
        String path = eagerImport.getImplModulePath();
        String previousAlias = aliasesByPath.get(path);
        if (previousAlias == null) {
          sourceBuilder.appendln("let %s = goog.require('%s');", alias, path);
          aliasesByPath.put(path, alias);
        } else {
          // Do not goog.require second time to avoid JsCompiler warnings.
          sourceBuilder.appendln("let %s = %s;", alias, previousAlias);
        }
      }
      sourceBuilder.newLine();
    }

    // goog.forwardDeclare(...) for lazy imports.
    List<Import> lazyImports = ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY));
    if (!lazyImports.isEmpty()) {
      for (Import lazyImport : lazyImports) {
        String alias = lazyImport.getAlias();
        String path = lazyImport.getImplModulePath();
        sourceBuilder.appendln("let %s = goog.forwardDeclare('%s');", alias, path);
      }
      sourceBuilder.newLine();
    }

    // = window.Blah; for extern imports (this is really just alias creation).
    List<Import> externImports =
        ImportUtils.sortedList(importsByCategory.get(ImportCategory.EXTERN));
    if (!externImports.isEmpty()) {
      for (Import externImport : externImports) {
        String alias = externImport.getAlias();
        String path = externImport.getImplModulePath();
        sourceBuilder.appendln("/** @constructor */ let %s = %s;", alias, path);
      }
      sourceBuilder.newLine();
    }

    sourceBuilder.newLine();
  }

  private void renderTypeAnnotation() {
    if (javaType.isJsOverlayImpl()) {
      // Do nothing.
    } else if (javaType.isInterface()) {
      sourceBuilder.appendln("/**");
      sourceBuilder.appendln(" * @interface");
      if (javaType.getDescriptor().isParameterizedType()) {
        String templates = getJsDocNames(javaType.getDescriptor().getTypeArgumentDescriptors());
        sourceBuilder.appendln(" * @template %s", templates);
      }
      for (TypeDescriptor superInterfaceType : javaType.getSuperInterfaceTypeDescriptors()) {
        sourceBuilder.appendln(" * @extends {%s}", getJsDocName(superInterfaceType, true));
      }
      sourceBuilder.appendln(" */");
    } else { // Not an interface so it is a Class.
      if (!GeneratorUtils.hasJsDoc(javaType)) {
        return;
      }
      sourceBuilder.appendln("/**");
      if (javaType.isAbstract()) {
        sourceBuilder.appendln(" * Abstract class, do not instantiate.");
      }
      if (javaType.getDescriptor().isParameterizedType()) {
        String templates = getJsDocNames(javaType.getDescriptor().getTypeArgumentDescriptors());
        sourceBuilder.appendln(" * @template %s", templates);
      }
      if (javaType.getSuperTypeDescriptor().isParameterizedType()) {
        String templates = getJsDocName(javaType.getSuperTypeDescriptor(), true);
        sourceBuilder.appendln(" * @extends {%s}", templates);
      }
      for (TypeDescriptor superInterfaceType : javaType.getSuperInterfaceTypeDescriptors()) {
        sourceBuilder.appendln(" * @implements {%s}", getJsDocName(superInterfaceType, true));
      }
      sourceBuilder.appendln(" */");
    }
  }

  private void renderTypeBody() {
    String extendsClause = GeneratorUtils.getExtendsClause(javaType, environment);
    sourceBuilder.appendln("class %s %s{", className, extendsClause);
    environment.setEnclosingTypeDescriptor(javaType.getDescriptor());
    renderJavaTypeMethods();
    renderMarkImplementorMethod();
    renderIsInstanceMethod();
    renderIsAssignableFromMethod();
    renderCopyMethod();
    renderStaticFieldGettersSetters();
    renderClinit();
    renderInitMethod();
    environment.setEnclosingTypeDescriptor(null);
    sourceBuilder.appendln("};");
    sourceBuilder.newLine();
    renderClassMetadata();
    sourceBuilder.newLine();
    sourceBuilder.newLine();
  }

  private void renderJavaTypeMethods() {
    for (Method method : javaType.getMethods()) {
      if (method.isConstructor()) {
        String mangledNameOfCreate =
            ManglingNameUtils.getFactoryMethodMangledName(method.getDescriptor());
        if (javaType.containsJsMethod(mangledNameOfCreate)) {
          sourceBuilder.appendln("/**");
          sourceBuilder.appendln(" * Constructor function implementation is provided separately.");
          sourceBuilder.appendln(" */");
          sourceBuilder.newLine();
          continue;
        }
      }
      if (GeneratorUtils.shouldNotEmitCode(method)) {
        continue;
      }
      if (method.getDescriptor().isJsOverlay() && !method.getDescriptor().isStatic()) {
        continue;
      }
      sourceBuilder.appendln("/**");
      if (method.getJsDocDescription() != null && !method.getJsDocDescription().isEmpty()) {
        sourceBuilder.appendln(" * %s", method.getJsDocDescription());
      }
      if (method.isAbstract()) {
        sourceBuilder.appendln(" * Abstract method.");
      }
      if (method.isOverride() && !method.isConstructor()) {
        sourceBuilder.appendln(" * @override");
      }
      if (!method.getDescriptor().getTypeParameterTypeDescriptors().isEmpty()) {
        String templateParamNames =
            JsDocNameUtils.getJsDocNames(
                method.getDescriptor().getTypeParameterTypeDescriptors(), environment);
        sourceBuilder.appendln(" * @template %s", templateParamNames);
      }

      if (javaType.getDescriptor().isJsFunctionImplementation()
          && method.getDescriptor().isPolymorphic()
          && !method.getBody().getStatements().isEmpty()
          && !method.getDescriptor().getMethodName().startsWith("$ctor")) {
        sourceBuilder.appendln(" * @this {%s}", getJsDocName(javaType.getDescriptor()));
      }
      for (int i = 0; i < method.getParameters().size(); i++) {
        sourceBuilder.appendln(
            " * %s", GeneratorUtils.getParameterJsDocAnnotation(method, i, environment));
      }
      String returnTypeName = getJsDocName(method.getDescriptor().getReturnTypeDescriptor());
      if (!method.getDescriptor().isConstructor()) {
        sourceBuilder.appendln(" * @return {%s}", returnTypeName);
      }
      sourceBuilder.appendln(" * @%s", GeneratorUtils.visibilityForMethod(method));
      sourceBuilder.appendln(" */");
      if (method.isNative()) {
        sourceBuilder.appendln("// native " + GeneratorUtils.getMethodHeader(method, environment));
      } else {
        sourceBuilder.appendln(GeneratorUtils.getMethodHeader(method, environment));
        statementTranspiler.renderStatement(method.getBody());
      }
      sourceBuilder.newLine();
    }
  }

  private void renderMarkImplementorMethod() {
    if (!javaType.isInterface() || javaType.isJsOverlayImpl()) {
      return; // Only render markImplementor code for interfaces.
    }
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(" * Marks the provided class as implementing this interface.");
    sourceBuilder.appendln(" * @param {window.Function} classConstructor");
    sourceBuilder.appendln(" * @public");
    sourceBuilder.appendln(" */");
    sourceBuilder.appendln("static $markImplementor(classConstructor) {");
    for (TypeDescriptor superInterface : javaType.getSuperInterfaceTypeDescriptors()) {
      if (superInterface.isNative()) {
        continue;
      }
      String superInterfaceName = environment.aliasForType(superInterface);
      sourceBuilder.appendln("%s.$markImplementor(classConstructor);", superInterfaceName);
    }
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(" * @public {boolean}");
    sourceBuilder.appendln(" */");
    sourceBuilder.appendln("classConstructor.prototype.$implements__%s = true;", mangledTypeName);
    sourceBuilder.appendln("}");
    sourceBuilder.newLine();
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderIsInstanceMethod() {
    if (javaType.isInterface()) {
      renderIsInstanceForInterfaceType();
    } else { // Not an interface so it is a Class.
      renderIsInstanceForClassType();
    }
    sourceBuilder.newLine();
  }

  private void renderIsInstanceForClassType() {
    if (javaType.containsJsMethod(MethodDescriptor.IS_INSTANCE_METHOD_NAME)) {
      sourceBuilder.appendln("/**");
      sourceBuilder.appendln(" * $isInstance() function implementation is provided separately.");
      sourceBuilder.appendln(" */");
      return;
    }
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(
        " * Returns whether the provided instance is an instance of this class.");
    sourceBuilder.appendln(" * @param {*} instance");
    sourceBuilder.appendln(" * @return {boolean}");
    sourceBuilder.appendln(" * @public");
    sourceBuilder.appendln(" */");
    sourceBuilder.appendln("static $isInstance(instance) {");
    if (javaType.getDescriptor().isJsFunctionImplementation()) {
      sourceBuilder.appendln("return instance != null && instance.$is__%s;", mangledTypeName);
    } else {
      String className =
          environment.aliasForType(
              javaType.isJsOverlayImpl()
                  ? javaType.getNativeTypeDescriptor().getRawTypeDescriptor()
                  : javaType.getDescriptor());
      sourceBuilder.appendln("return instance instanceof %s;", className);
    }
    sourceBuilder.appendln("}");
  }

  private void renderIsInstanceForInterfaceType() {
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(
        " * Returns whether the provided instance is of a class that implements this");
    sourceBuilder.appendln(" * interface.");
    sourceBuilder.appendln(" * @param {*} instance");
    sourceBuilder.appendln(" * @return {boolean}");
    sourceBuilder.appendln(" * @public");
    sourceBuilder.appendln(" */");
    sourceBuilder.appendln("static $isInstance(instance) {");
    if (javaType.isJsOverlayImpl()) {
      sourceBuilder.appendln("return true;");
    } else if (javaType.getDescriptor().isJsFunctionInterface()) {
      sourceBuilder.appendln("return instance != null && typeof instance == \"function\";");
    } else {
      sourceBuilder.appendln(
          "return instance != null && instance.$implements__%s;", mangledTypeName);
    }
    sourceBuilder.appendln("}");
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderIsAssignableFromMethod() {
    if (javaType.isJsOverlayImpl()
        || javaType.containsJsMethod(MethodDescriptor.IS_ASSIGNABLE_FROM_METHOD_NAME)) {
      return; // Don't render for overlay types or if the method exists.
    }
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(" * Returns whether the provided class is or extends this class.");
    sourceBuilder.appendln(" * @param {window.Function} classConstructor");
    sourceBuilder.appendln(" * @return {boolean}");
    sourceBuilder.appendln(" * @public");
    sourceBuilder.appendln(" */");
    sourceBuilder.appendln("static $isAssignableFrom(classConstructor) {");
    if (javaType.isInterface()) { // For interfaces
      sourceBuilder.appendln(
          "return classConstructor != null && classConstructor.prototype.$implements__%s;",
          mangledTypeName);
    } else { // For classes
      BootstrapType.NATIVE_UTIL.getDescriptor();
      String utilAlias = environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());
      sourceBuilder.appendln(
          "return %s.$canCastClass(classConstructor, %s);", utilAlias, className);
    }
    sourceBuilder.appendln("}");
    sourceBuilder.newLine();
  }

  // TODO: Move this to the ast in a normalization pass.
  // TODO: may copy Objects methods (equals, hashCode, etc. ) as well.
  private void renderCopyMethod() {
    if (!javaType.getDescriptor().isJsFunctionImplementation()) {
      return; // Only render the $copy method for jsfunctions
    }
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(" * Copies the fields from {@code from} to {@code to}.");
    sourceBuilder.appendln(" * @param {%s} from", className);
    sourceBuilder.appendln(" * @param {*} to");
    sourceBuilder.appendln(" * @public");
    sourceBuilder.appendln(" */");
    sourceBuilder.appendln("static $copy(from, to) {");
    for (Field field : javaType.getInstanceFields()) {
      String fieldName = ManglingNameUtils.getMangledName(field.getDescriptor());
      sourceBuilder.appendln("to.%s = from.%s;", fieldName, fieldName);
    }
    sourceBuilder.appendln("// Marks the object is an instance of this class.");
    sourceBuilder.appendln("to.$is__%s = true;", mangledTypeName);
    sourceBuilder.appendln("}");
    sourceBuilder.newLine();
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderClassMetadata() {
    if (javaType.isJsOverlayImpl()) {
      return; // Don't render $getClass for overlay types.
    }
    String utilAlias = environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());
    String name = javaType.getDescriptor().getBinaryName();
    if (javaType.isInterface()) {
      sourceBuilder.appendln(
          "%s.$setClassMetadataForInterface(%s, '%s');", utilAlias, className, name);
    } else if (javaType.isEnum()) {
      sourceBuilder.appendln("%s.$setClassMetadataForEnum(%s, '%s');", utilAlias, className, name);
    } else {
      sourceBuilder.appendln("%s.$setClassMetadata(%s, '%s');", utilAlias, className, name);
    }
  }

  private void renderStaticFieldGettersSetters() {
    for (Field staticField : javaType.getStaticFields()) {
      if (staticField.isCompileTimeConstant()) {
        continue;
      }
      Visibility staticFieldVisibility = staticField.getDescriptor().getVisibility();
      String staticFieldType = getJsDocName(staticField.getDescriptor().getTypeDescriptor());
      String indirectStaticFieldName =
          ManglingNameUtils.getMangledName(staticField.getDescriptor());
      String directStaticFieldAccess =
          ManglingNameUtils.getMangledName(staticField.getDescriptor(), true);

      sourceBuilder.appendln("/**");
      sourceBuilder.appendln(" * A static field getter.");
      sourceBuilder.appendln(" * @return {%s}", staticFieldType);
      sourceBuilder.appendln(" * @%s", staticFieldVisibility.jsText);
      sourceBuilder.appendln(" */");
      sourceBuilder.appendln("static get %s() {", indirectStaticFieldName);
      sourceBuilder.appendln(
          "return (%s.$clinit(), %s.%s);", className, className, directStaticFieldAccess);
      sourceBuilder.appendln("}");
      sourceBuilder.newLine();

      sourceBuilder.appendln("/**");
      sourceBuilder.appendln(" * A static field setter.");
      sourceBuilder.appendln(" * @param {%s} value", staticFieldType);
      sourceBuilder.appendln(" * @return {void}", staticFieldType);
      sourceBuilder.appendln(" * @%s", staticFieldVisibility.jsText);
      sourceBuilder.appendln(" */");
      sourceBuilder.appendln("static set %s(value) {", indirectStaticFieldName);
      sourceBuilder.appendln(
          "(%s.$clinit(), %s.%s = value);", className, className, directStaticFieldAccess);
      sourceBuilder.appendln("}");
      sourceBuilder.newLine();
    }
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderClinit() {
    List<Import> lazyImports = ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY));
    if (!GeneratorUtils.needClinit(javaType, lazyImports)) {
      return;
    }
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(" * Runs inline static field initializers.");
    sourceBuilder.appendln(" * @public");
    sourceBuilder.appendln(" */");
    sourceBuilder.appendln("static $clinit() {");
    if (GeneratorUtils.needRewriteClinit(javaType)) {
      // Set this method to reference an empty function so that it cannot be called again.
      sourceBuilder.appendln("%s.$clinit = function() {};", className);
    }
    // goog.module.get(...) for lazy imports.
    Map<String, String> aliasesByPath = new HashMap<>();
    for (Import lazyImport : ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY))) {
      String alias = lazyImport.getAlias();
      String path = lazyImport.getImplModulePath();
      String previousAlias = aliasesByPath.get(path);
      if (previousAlias == null) {
        sourceBuilder.appendln("%s = goog.module.get('%s');", alias, path);
        aliasesByPath.put(path, alias);
      } else {
        // Do not goog.require second time to avoid JsCompiler warnings.
        sourceBuilder.appendln("%s = %s;", alias, previousAlias);
      }
    }
    if (GeneratorUtils.hasNonNativeSuperClass(javaType)) {
      // call the super class $clinit.
      TypeDescriptor superTypeDescriptor = javaType.getSuperTypeDescriptor();
      String superTypeName = environment.aliasForType(superTypeDescriptor);
      sourceBuilder.appendln("%s.$clinit();", superTypeName);
    }
    // Static field and static initializer blocks.
    renderInitializerElements(javaType.getStaticFieldsAndInitializerBlocks());

    sourceBuilder.appendln("}");
    sourceBuilder.newLine();
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderInitMethod() {
    if (javaType.isJsOverlayImpl() || javaType.isInterface()) {
      return;
    }
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(" * Runs instance field and block initializers.");
    sourceBuilder.appendln(" * @private");
    sourceBuilder.appendln(" */");
    String methodName = String.format("$init__%s", mangledTypeName);
    sourceBuilder.appendln("%s() {", methodName);
    renderInitializerElements(javaType.getInstanceFieldsAndInitializerBlocks());

    sourceBuilder.appendln("}");
  }

  private void renderInitializerElements(List<Positioned> initializerElements) {
    for (Positioned element : initializerElements) {
      if (element instanceof Field) {
        Field field = (Field) element;
        if (field.hasInitializer() && !field.isCompileTimeConstant()) {
          boolean isInstanceField = !field.getDescriptor().isStatic();
          String fieldInitializer = expressionToString(field.getInitializer());
          String fieldName =
              ManglingNameUtils.getMangledName(field.getDescriptor(), !isInstanceField);
          sourceBuilder.appendln(
              "%s.%s = %s;", isInstanceField ? "this" : className, fieldName, fieldInitializer);
        }
      } else if (element instanceof Block) {
        Block block = (Block) element;
        for (Statement initializer : block.getStatements()) {
          statementTranspiler.renderStatement(initializer);
        }
      } else {
        throw new UnsupportedOperationException("Unsupported element: " + element);
      }
    }
  }

  private void renderStaticFieldDeclarations() {
    for (Field staticField : javaType.getStaticFields()) {
      String jsDocType =
          JsDocNameUtils.getJsDocName(staticField.getDescriptor().getTypeDescriptor(), environment);
      String initialValue = expressionToString(GeneratorUtils.getInitialValue(staticField));
      if (staticField.isCompileTimeConstant()) {
        String publicFieldAccess =
            ManglingNameUtils.getMangledName(staticField.getDescriptor(), false);
        sourceBuilder.appendln("/**");
        sourceBuilder.appendln(" * @public {%s}", jsDocType);
        sourceBuilder.appendln(" * @const");
        sourceBuilder.appendln(" */");
        sourceBuilder.appendln("%s.%s = %s;", className, publicFieldAccess, initialValue);
      } else {
        String privateFieldAccess =
            ManglingNameUtils.getMangledName(staticField.getDescriptor(), true);
        sourceBuilder.appendln("/**");
        sourceBuilder.appendln(" * @private {%s}", jsDocType);
        sourceBuilder.appendln(" */");
        sourceBuilder.appendln("%s.%s = %s;", className, privateFieldAccess, initialValue);
      }
      sourceBuilder.newLine();
      sourceBuilder.newLine();
    }
  }

  /**
   * Here we call markImplementor on all interfaces such that the class can be queried at run time
   * to determine if it implements an interface.
   */
  private void renderMarkImplementorCalls() {
    if (javaType.isJsOverlayImpl()) {
      return; // Do nothing
    }
    if (javaType.isInterface()) {
      // TODO: remove cast after b/20102666 is handled in Closure.
      sourceBuilder.appendln(
          "%s.$markImplementor(/** @type {window.Function} */ (%s));", className, className);
    } else { // Not an interface so it is a Class.
      for (TypeDescriptor interfaceTypeDescriptor : javaType.getSuperInterfaceTypeDescriptors()) {
        if (interfaceTypeDescriptor.isNative()) {
          continue;
        }
        String interfaceName = environment.aliasForType(interfaceTypeDescriptor);
        sourceBuilder.appendln("%s.$markImplementor(%s);", interfaceName, className);
      }
    }
    sourceBuilder.newLine();
    sourceBuilder.newLine();
  }

  private void renderNativeSource() {
    if (nativeSource != null) {
      sourceBuilder.appendln("/**");
      sourceBuilder.appendln(" * Native Method Injection");
      sourceBuilder.appendln(" */");
      sourceBuilder.appendln(nativeSource);
      sourceBuilder.newLine();
    }
  }

  private void renderExports() {
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(" * Export class.");
    sourceBuilder.appendln(" */");
    sourceBuilder.appendln("exports = %s;", className);
  }

  private void renderSourceMapLocation() {
    if (relativeSourceMapLocation != null) {
      sourceBuilder.append(String.format("//# sourceMappingURL=%s", relativeSourceMapLocation));
    }
  }

  private String expressionToString(Expression expression) {
    return ExpressionTranspiler.transform(expression, environment);
  }
}
