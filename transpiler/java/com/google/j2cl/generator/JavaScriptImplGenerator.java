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
import com.google.debugging.sourcemap.FilePosition;
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
import com.google.j2cl.ast.sourcemap.SourcePosition;
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

  public JavaScriptImplGenerator(Errors errors, boolean declareLegacyNamespace, JavaType javaType) {
    super(errors, declareLegacyNamespace, javaType);
    this.className = environment.aliasForType(javaType.getDescriptor());
    this.mangledTypeName = ManglingNameUtils.getMangledName(javaType.getDescriptor());
    this.statementTranspiler = new StatementTranspiler(sourceBuilder, environment);
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
    sourceBuilder.appendLines(
        "/**",
        " * @fileoverview Impl transpiled from " + transpiledFrom + ".",
        " *",
        " * @suppress {suspiciousCode, transitionalSuspiciousCodeWarnings, uselessCode, const,"
            + " missingRequire}",
        " */");
    sourceBuilder.newLine();
  }

  private void renderImports() {
    TypeDescriptor selfTypeDescriptor = javaType.getDescriptor().getRawTypeDescriptor();
    Import selfImport = new Import(selfTypeDescriptor.getSimpleName(), selfTypeDescriptor);

    // goog.module(...) declaration.
    sourceBuilder.appendln("goog.module('" + selfImport.getImplModulePath() + "');");
    if (declareLegacyNamespace && javaType.getDescriptor().isJsType()
        && !(javaType instanceof AnonymousJavaType)) {
      // Even if opted into declareLegacyNamespace, this only makes sense for classes that are
      // intended to be accessed from the native JS. Thus we only emit declareLegacyNamespace
      // for non-anonymous JsType classes.
      sourceBuilder.appendln("goog.module.declareLegacyNamespace();");
    }
    sourceBuilder.newLines(2);

    // goog.require(...) for eager imports.
    Map<String, String> aliasesByPath = new HashMap<>();
    List<Import> eagerImports = ImportUtils.sortedList(importsByCategory.get(ImportCategory.EAGER));
    if (!eagerImports.isEmpty()) {
      for (Import eagerImport : eagerImports) {
        String alias = eagerImport.getAlias();
        String path = eagerImport.getImplModulePath();
        String previousAlias = aliasesByPath.get(path);
        if (previousAlias == null) {
          sourceBuilder.appendln("let " + alias + " = goog.require('" + path + "');");
          aliasesByPath.put(path, alias);
        } else {
          // Do not goog.require second time to avoid JsCompiler warnings.
          sourceBuilder.appendln("let " + alias + " = " + previousAlias + ";");
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
        sourceBuilder.appendln("let " + alias + " = goog.forwardDeclare('" + path + "');");
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
        sourceBuilder.appendln("/** @constructor */ let " + alias + " = " + path + ";");
      }
      sourceBuilder.newLine();
    }

    sourceBuilder.newLine();
  }

  private void renderTypeAnnotation() {
    if (javaType.isJsOverlayImplementation()) {
      // Do nothing.
    } else if (javaType.isInterface()) {
      sourceBuilder.appendLines("/**", " * @interface");
      sourceBuilder.newLine();
      if (javaType.getDescriptor().isParameterizedType()) {
        String templates = getJsDocNames(javaType.getDescriptor().getTypeArgumentDescriptors());
        sourceBuilder.appendln(" * @template " + templates);
      }
      for (TypeDescriptor superInterfaceType : javaType.getSuperInterfaceTypeDescriptors()) {
        sourceBuilder.appendln(" * @extends {" + getJsDocName(superInterfaceType, true) + "}");
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
        sourceBuilder.appendln(" * @template " + templates);
      }
      if (javaType.getSuperTypeDescriptor().isParameterizedType()) {
        String supertype = getJsDocName(javaType.getSuperTypeDescriptor(), true);
        sourceBuilder.appendln(" * @extends {" + supertype + "}");
      }
      for (TypeDescriptor superInterfaceType : javaType.getSuperInterfaceTypeDescriptors()) {
        sourceBuilder.appendln(" * @implements {" + getJsDocName(superInterfaceType, true) + "}");
      }
      sourceBuilder.appendln(" */");
    }
  }

  private void renderTypeBody() {
    String extendsClause = GeneratorUtils.getExtendsClause(javaType, environment);
    sourceBuilder.append("class " + className + " " + extendsClause);
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
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
    sourceBuilder.closeBrace();
    sourceBuilder.append(";");
    sourceBuilder.newLines(2);
    renderClassMetadata();
    sourceBuilder.newLines(2);
  }

  private void renderJavaTypeMethods() {
    for (Method method : javaType.getMethods()) {
      if (GeneratorUtils.shouldNotEmitCode(method)) {
        continue;
      }
      if (method.getDescriptor().isJsOverlay() && !method.getDescriptor().isStatic()) {
        continue;
      }
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
      if (!method.getDescriptor().getTypeParameterTypeDescriptors().isEmpty()) {
        String templateParamNames =
            JsDocNameUtils.getJsDocNames(
                method.getDescriptor().getTypeParameterTypeDescriptors(), environment);
        sourceBuilder.appendln(" * @template " + templateParamNames);
      }

      if (javaType.getDescriptor().isJsFunctionImplementation()
          && method.getDescriptor().isPolymorphic()
          && !method.getBody().getStatements().isEmpty()
          && !method.getDescriptor().getMethodName().startsWith("$ctor")) {
        sourceBuilder.appendln(" * @this {" + getJsDocName(javaType.getDescriptor()) + "}");
      }
      for (int i = 0; i < method.getParameters().size(); i++) {
        sourceBuilder.appendln(
            " * " + GeneratorUtils.getParameterJsDocAnnotation(method, i, environment));
      }
      String returnTypeName = getJsDocName(method.getDescriptor().getReturnTypeDescriptor());
      if (!method.getDescriptor().isConstructor()) {
        sourceBuilder.appendln(" * @return {" + returnTypeName + "}");
      }
      sourceBuilder.appendln(" * @" + GeneratorUtils.visibilityForMethod(method));
      sourceBuilder.appendln(" */");
      if (method.isNative()) {
        sourceBuilder.append("// native " + GeneratorUtils.getMethodHeader(method, environment));
      } else {
        sourceBuilder.append(GeneratorUtils.getMethodHeader(method, environment) + " ");
        statementTranspiler.renderStatement(method.getBody());
      }
      sourceBuilder.newLines(2);
    }
  }

  private void renderMarkImplementorMethod() {
    if (!javaType.isInterface() || javaType.isJsOverlayImplementation()) {
      return; // Only render markImplementor code for interfaces.
    }
    sourceBuilder.appendLines(
        "/**",
        " * Marks the provided class as implementing this interface.",
        " * @param {window.Function} classConstructor",
        " * @public",
        " */",
        "static $markImplementor(classConstructor) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    for (TypeDescriptor superInterface : javaType.getSuperInterfaceTypeDescriptors()) {
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
        "classConstructor.prototype.$implements__" + mangledTypeName + " = true;");
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderIsInstanceMethod() {
    if (javaType.containsMethod(MethodDescriptor.IS_INSTANCE_METHOD_NAME)) {
      sourceBuilder.appendLines(
          "/**", " * $isInstance() function implementation is provided separately.", " */");
      sourceBuilder.newLine();
      return;
    }
    if (javaType.isInterface()) {
      renderIsInstanceForInterfaceType();
    } else { // Not an interface so it is a Class.
      renderIsInstanceForClassType();
    }
  }

  private void renderIsInstanceForClassType() {
    sourceBuilder.appendLines(
        "/**",
        " * Returns whether the provided instance is an instance of this class.",
        " * @param {*} instance",
        " * @return {boolean}",
        " * @public",
        " */",
        "static $isInstance(instance) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    if (javaType.getDescriptor().isJsFunctionImplementation()) {
      sourceBuilder.appendln("return instance != null && instance.$is__" + mangledTypeName + ";");
    } else {
      String className =
          environment.aliasForType(
              javaType.isJsOverlayImplementation()
                  ? javaType.getNativeTypeDescriptor().getRawTypeDescriptor()
                  : javaType.getDescriptor());
      sourceBuilder.append("return instance instanceof " + className + ";");
    }
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  private void renderIsInstanceForInterfaceType() {
    sourceBuilder.appendLines(
        "/**",
        " * Returns whether the provided instance is of a class that implements this",
        " * interface.",
        " * @param {*} instance",
        " * @return {boolean}",
        " * @public",
        " */",
        "static $isInstance(instance) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    if (javaType.isJsOverlayImplementation()) {
      sourceBuilder.append("return true;");
    } else if (javaType.getDescriptor().isJsFunctionInterface()) {
      sourceBuilder.append("return instance != null && typeof instance == \"function\";");
    } else {
      sourceBuilder.append(
          "return instance != null && instance.$implements__" + mangledTypeName + ";");
    }
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderIsAssignableFromMethod() {
    if (javaType.isJsOverlayImplementation()
        || javaType.containsMethod(MethodDescriptor.IS_ASSIGNABLE_FROM_METHOD_NAME)) {
      return; // Don't render for overlay types or if the method exists.
    }
    sourceBuilder.appendLines(
        "/**",
        " * Returns whether the provided class is or extends this class.",
        " * @param {window.Function} classConstructor",
        " * @return {boolean}",
        " * @public",
        " */",
        "static $isAssignableFrom(classConstructor) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();

    if (javaType.isInterface()) { // For interfaces
      sourceBuilder.append(
          "return classConstructor != null && classConstructor.prototype.$implements__"
              + mangledTypeName
              + ";");
    } else { // For classes
      BootstrapType.NATIVE_UTIL.getDescriptor();
      String utilAlias = environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());
      sourceBuilder.append(
          "return " + utilAlias + ".$canCastClass(classConstructor, " + className + ");");
    }
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO: Move this to the ast in a normalization pass.
  // TODO: may copy Objects methods (equals, hashCode, etc. ) as well.
  private void renderCopyMethod() {
    if (!javaType.getDescriptor().isJsFunctionImplementation()) {
      return; // Only render the $copy method for jsfunctions
    }
    sourceBuilder.appendLines(
        "/**",
        " * Copies the fields from {@code from} to {@code to}.",
        " * @param {" + className + "} from",
        " * @param {*} to",
        " * @public",
        " */",
        "static $copy(from, to) ");
    sourceBuilder.openBrace();
    for (Field field : javaType.getInstanceFields()) {
      String fieldName = ManglingNameUtils.getMangledName(field.getDescriptor());
      sourceBuilder.newLine();
      sourceBuilder.append("to." + fieldName + " = from." + fieldName + ";");
    }
    sourceBuilder.newLine();
    sourceBuilder.appendLines(
        "// Marks the object is an instance of this class.",
        "to.$is__" + mangledTypeName + " = true;");
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderClassMetadata() {
    if (javaType.isJsOverlayImplementation()) {
      // Don't render $getClass for overlay types.
      // implementations.
      return;
    }
    String utilAlias = environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());
    String name = javaType.getDescriptor().getBinaryName();
    if (javaType.isInterface()) {
      sourceBuilder.appendln(
          utilAlias + ".$setClassMetadataForInterface(" + className + ", '" + name + "');");
    } else if (javaType.isEnum()) {
      sourceBuilder.appendln(
          utilAlias + ".$setClassMetadataForEnum(" + className + ", '" + name + "');");
    } else {
      sourceBuilder.appendln(utilAlias + ".$setClassMetadata(" + className + ", '" + name + "');");
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

      sourceBuilder.appendLines(
          "/**",
          " * A static field getter.",
          " * @return {" + staticFieldType + "}",
          " * @" + staticFieldVisibility.jsText,
          " */",
          "static get " + indirectStaticFieldName + "() ");
      sourceBuilder.openBrace();
      sourceBuilder.newLine();
      sourceBuilder.append(
          "return ("
              + className
              + ".$clinit(), "
              + className
              + "."
              + directStaticFieldAccess
              + ");");
      sourceBuilder.closeBrace();
      sourceBuilder.newLines(2);

      sourceBuilder.appendLines(
          "/**",
          " * A static field setter.",
          " * @param {" + staticFieldType + "} value",
          " * @return {void}",
          " * @" + staticFieldVisibility.jsText,
          " */",
          "static set " + indirectStaticFieldName + "(value) ");
      sourceBuilder.openBrace();
      sourceBuilder.newLine();
      sourceBuilder.append(
          "("
              + className
              + ".$clinit(), "
              + className
              + "."
              + directStaticFieldAccess
              + " = value);");
      sourceBuilder.closeBrace();
      sourceBuilder.newLines(2);
    }
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderClinit() {
    sourceBuilder.appendLines(
        "/**",
        " * Runs inline static field initializers.",
        " * @public",
        " */",
        "static $clinit() ");
    sourceBuilder.openBrace();
    if (GeneratorUtils.needRewriteClinit(javaType)) {
      // Set this method to reference an empty function so that it cannot be called again.
      sourceBuilder.newLine();
      sourceBuilder.append(className + ".$clinit = function() {};");
    }
    // goog.module.get(...) for lazy imports.
    Map<String, String> aliasesByPath = new HashMap<>();
    for (Import lazyImport : ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY))) {
      String alias = lazyImport.getAlias();
      String path = lazyImport.getImplModulePath();
      String previousAlias = aliasesByPath.get(path);
      if (previousAlias == null) {
        sourceBuilder.newLine();
        sourceBuilder.append(alias + " = goog.module.get('" + path + "');");
        aliasesByPath.put(path, alias);
      } else {
        // Do not goog.require second time to avoid JsCompiler warnings.
        sourceBuilder.newLine();
        sourceBuilder.append(alias + " = " + previousAlias + ";");
      }
    }
    if (GeneratorUtils.hasNonNativeSuperClass(javaType)) {
      // call the super class $clinit.
      TypeDescriptor superTypeDescriptor = javaType.getSuperTypeDescriptor();
      String superTypeName = environment.aliasForType(superTypeDescriptor);
      sourceBuilder.newLine();
      sourceBuilder.append(superTypeName + ".$clinit();");
    }
    // Static field and static initializer blocks.
    renderInitializerElements(javaType.getStaticFieldsAndInitializerBlocks());
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderInitMethod() {
    if (javaType.isJsOverlayImplementation() || javaType.isInterface()) {
      return;
    }
    sourceBuilder.appendLines(
        "/**",
        " * Runs instance field and block initializers.",
        " * @private",
        " */",
        "$init__" + mangledTypeName + "() ");
    sourceBuilder.openBrace();
    renderInitializerElements(javaType.getInstanceFieldsAndInitializerBlocks());
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  private void renderInitializerElements(List<Positioned> initializerElements) {
    for (Positioned element : initializerElements) {
      if (element instanceof Field) {
        Field field = (Field) element;
        if (field.hasInitializer() && !field.isCompileTimeConstant()) {
          sourceBuilder.newLine();
          FilePosition startPostion = sourceBuilder.getCurrentPosition();
          boolean isInstanceField = !field.getDescriptor().isStatic();
          String fieldName =
              ManglingNameUtils.getMangledName(field.getDescriptor(), !isInstanceField);
          sourceBuilder.append((isInstanceField ? "this" : className) + "." + fieldName + " = ");
          renderExpression(field.getInitializer());
          sourceBuilder.append(";");
          sourceBuilder.addMapping(
              field.getSourcePosition(),
              new SourcePosition(startPostion, sourceBuilder.getCurrentPosition()));
        }
      } else if (element instanceof Block) {
        Block block = (Block) element;
        for (Statement initializer : block.getStatements()) {
          sourceBuilder.newLine();
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
      if (staticField.isCompileTimeConstant()) {
        String publicFieldAccess =
            ManglingNameUtils.getMangledName(staticField.getDescriptor(), false);
        sourceBuilder.appendLines(
            "/**",
            " * @public {" + jsDocType + "}",
            " * @const",
            " */",
            className + "." + publicFieldAccess + " = ");
        renderExpression(GeneratorUtils.getInitialValue(staticField));
        sourceBuilder.append(";");
      } else {
        String privateFieldAccess =
            ManglingNameUtils.getMangledName(staticField.getDescriptor(), true);
        sourceBuilder.appendLines(
            "/**",
            " * @private {" + jsDocType + "}",
            " */",
            className + "." + privateFieldAccess + " = ");
        renderExpression(GeneratorUtils.getInitialValue(staticField));
        sourceBuilder.append(";");
      }
      // emit 2 empty lines
      sourceBuilder.newLines(3);
    }
  }

  /**
   * Here we call markImplementor on all interfaces such that the class can be queried at run time
   * to determine if it implements an interface.
   */
  private void renderMarkImplementorCalls() {
    if (javaType.isJsOverlayImplementation()) {
      return; // Do nothing
    }
    if (javaType.isInterface()) {
      // TODO: remove cast after b/20102666 is handled in Closure.
      sourceBuilder.appendln(
          className + ".$markImplementor(/** @type {window.Function} */ (" + className + "));");
    } else { // Not an interface so it is a Class.
      for (TypeDescriptor interfaceTypeDescriptor : javaType.getSuperInterfaceTypeDescriptors()) {
        if (interfaceTypeDescriptor.isNative()) {
          continue;
        }
        String interfaceName = environment.aliasForType(interfaceTypeDescriptor);
        sourceBuilder.appendln(interfaceName + ".$markImplementor(" + className + ");");
      }
    }
    sourceBuilder.newLines(2);
  }

  private void renderNativeSource() {
    if (nativeSource != null) {
      sourceBuilder.appendLines("/**", " * Native Method Injection", " */");
      sourceBuilder.newLine();
      sourceBuilder.appendln(nativeSource);
      sourceBuilder.newLine();
    }
  }

  private void renderExports() {
    sourceBuilder.appendLines("/**", " * Export class.", " */", "exports = " + className + ";");
    sourceBuilder.newLine();
  }

  private void renderSourceMapLocation() {
    if (relativeSourceMapLocation != null) {
      sourceBuilder.append("//# sourceMappingURL=" + relativeSourceMapLocation);
    }
  }

  private void renderExpression(Expression expression) {
    ExpressionTranspiler.render(expression, environment, sourceBuilder);
  }
}
