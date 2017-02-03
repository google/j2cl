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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.InitializerBlock;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.common.FilePosition;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatherer.ImportCategory;
import com.google.j2cl.problems.Problems;
import java.util.HashMap;
import java.util.Map;

/** Generates JavaScript source impl files. */
public class JavaScriptImplGenerator extends JavaScriptGenerator {
  private String nativeSource;
  private String relativeSourceMapLocation;

  private String className;
  private String mangledTypeName;

  protected final StatementTranspiler statementTranspiler;

  public static final String FILE_SUFFIX = ".impl.java.js";

  public JavaScriptImplGenerator(Problems problems, boolean declareLegacyNamespace, Type type) {
    super(problems, declareLegacyNamespace, type);
    this.className = environment.aliasForType(type.getDescriptor());
    this.mangledTypeName =
        ManglingNameUtils.getMangledName(type.getDescriptor().getUnsafeTypeDescriptor());
    this.statementTranspiler = new StatementTranspiler(sourceBuilder, environment);
  }

  @Override
  public String getSuffix() {
    return FILE_SUFFIX;
  }

  public void setRelativeSourceMapLocation(String relativeSourceMapLocation) {
    this.relativeSourceMapLocation = checkNotNull(relativeSourceMapLocation);
  }

  public void setNativeSource(String nativeSource) {
    this.nativeSource = checkNotNull(nativeSource);
  }

  public String getJsDocName(TypeDescriptor typeDescriptor) {
    return JsDocNameUtils.getJsDocName(typeDescriptor, environment);
  }

  public String getJsDocName(TypeDescriptor typeDescriptor, boolean shouldUseClassName) {
    return JsDocNameUtils.getJsDocName(typeDescriptor, shouldUseClassName, environment);
  }

  public String getJsDocNames(Iterable<TypeDescriptor> typeDescriptors) {
    return JsDocNameUtils.getJsDocNames(typeDescriptors, environment);
  }

  @Override
  public String renderOutput() {
    try {
      renderFileOverview(
          "suspiciousCode",
          "transitionalSuspiciousCodeWarnings",
          "uselessCode",
          "const",
          "missingRequire",
          "missingOverride");
      renderImports();
      renderTypeAnnotation();
      renderTypeBody();
      renderStaticFieldDeclarations();
      renderMarkImplementorCalls();
      renderNativeSource();
      renderExports();
      renderSourceMapLocation();
      recordFileLengthInSourceMap();
      return sourceBuilder.build();
    } catch (RuntimeException e) {
      // Catch all unchecked exceptions and rethrow them with more context to make debugging easier.
      // Yes this is really being done on purpose.
      throw new RuntimeException(
          "Error generating source for type " + type.getDescriptor().getQualifiedBinaryName(), e);
    }
  }

  private void renderImports() {
    TypeDeclaration selfTypeDeclaration = type.getDescriptor();

    // goog.module(...) declaration.
    sourceBuilder.appendln("goog.module('" + selfTypeDeclaration.getImplModuleName() + "');");
    if (declareLegacyNamespace && type.getDescriptor().isJsType() && !(type.isAnonymous())) {
      // Even if opted into declareLegacyNamespace, this only makes sense for classes that are
      // intended to be accessed from the native JS. Thus we only emit declareLegacyNamespace
      // for non-anonymous JsType classes.
      sourceBuilder.appendln("goog.module.declareLegacyNamespace();");
    }
    sourceBuilder.newLines(2);

    // goog.require(...) for eager imports.
    Map<String, String> aliasesByPath = new HashMap<>();
    Iterable<Import> eagerImports = sortImports(importsByCategory.get(ImportCategory.EAGER));
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
    if (!Iterables.isEmpty(eagerImports)) {
      sourceBuilder.newLine();
    }

    // goog.forwardDeclare(...) for lazy imports.
    Iterable<Import> lazyImports = sortImports(importsByCategory.get(ImportCategory.LAZY));
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
      if (type.getDescriptor().hasTypeParameters()) {
        String templates = getJsDocNames(type.getDescriptor().getTypeParameterDescriptors());
        sourceBuilder.appendln(" * @template " + templates);
      }
      for (TypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
        sourceBuilder.appendln(" * @extends {" + getJsDocName(superInterfaceType, true) + "}");
      }
      sourceBuilder.appendln(" */");
    } else { // Not an interface so it is a Class.
      SourceBuilder buffer = new SourceBuilder();
      if (type.isAbstract()) {
        buffer.appendln(" * @abstract");
      }
      if (type.getDescriptor().hasTypeParameters()) {
        String templates = getJsDocNames(type.getDescriptor().getTypeParameterDescriptors());
        buffer.appendln(" * @template " + templates);
      }
      if (type.getSuperTypeDescriptor() != null
          && type.getSuperTypeDescriptor().hasTypeArguments()) {
        String supertype = getJsDocName(type.getSuperTypeDescriptor(), true);
        buffer.appendln(" * @extends {" + supertype + "}");
      }
      for (TypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
        buffer.appendln(" * @implements {" + getJsDocName(superInterfaceType, true) + "}");
      }

      String annotation = buffer.build();
      if (!annotation.isEmpty()) {
        sourceBuilder.appendln("/**");
        sourceBuilder.append(annotation);
        sourceBuilder.appendln(" */");
      }
    }
  }

  private void renderTypeBody() {
    String extendsClause = GeneratorUtils.getExtendsClause(type, environment);
    sourceBuilder.append("class " + className + " " + extendsClause);
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    environment.setEnclosingTypeDescriptor(type.getDescriptor().getUnsafeTypeDescriptor());
    renderTypeMethods();
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

  private void renderTypeMethods() {
    for (Method method : type.getMethods()) {
      // JsOverlay methods are rendered in their own files.
      if (method.getDescriptor().isJsOverlay()) {
        continue;
      }

      if (method.isNative()) {
        // If the method is native, output JsDoc comments so it can serve as a template for
        // native.js. However if the method is pointing to a different namespace then there
        // is no point on doing that since it cannot be provided via a native.js file.
        if (method.getDescriptor().hasJsNamespace()) {
          continue;
        }
        renderMethodJsDoc(method);
        sourceBuilder.append("// native " + GeneratorUtils.getMethodHeader(method, environment));
      } else {
        renderMethodJsDoc(method);
        sourceBuilder.append(GeneratorUtils.getMethodHeader(method, environment) + " ");
        statementTranspiler.renderStatement(method.getBody());
      }
      sourceBuilder.newLines(2);
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
    if (!method.getDescriptor().getTypeParameterTypeDescriptors().isEmpty()) {
      String templateParamNames =
          JsDocNameUtils.getJsDocNames(
              method.getDescriptor().getTypeParameterTypeDescriptors(), environment);
      sourceBuilder.appendln(" * @template " + templateParamNames);
    }

    if (type.getDescriptor().isJsFunctionImplementation()
        && method.getDescriptor().isPolymorphic()
        && !method.getBody().getStatements().isEmpty()
        && !method.getDescriptor().getName().startsWith("$ctor")) {
      sourceBuilder.appendln(
          " * @this {" + getJsDocName(type.getDescriptor().getUnsafeTypeDescriptor()) + "}");
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
  }

  private void renderMarkImplementorMethod() {
    if (!type.isInterface() || type.isJsOverlayImplementation()) {
      return; // Only render markImplementor code for interfaces.
    }
    sourceBuilder.appendLines(
        "/**",
        " * Marks the provided class as implementing this interface.",
        " * @param {" + TypeDescriptors.NATIVE_FUNCTION.getQualifiedJsName() + "} classConstructor",
        " * @public",
        " */",
        "static $markImplementor(classConstructor) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    for (TypeDescriptor superInterface : type.getSuperInterfaceTypeDescriptors()) {
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
    if (type.containsMethod(MethodDescriptor.IS_INSTANCE_METHOD_NAME)) {
      sourceBuilder.appendLines(
          "/**", " * $isInstance() function implementation is provided separately.", " */");
      sourceBuilder.newLine();
      return;
    }
    if (type.isInterface()) {
      renderIsInstanceForInterfaceType();
    } else {
      checkState(type.isClass() || type.isEnum());
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
    if (type.getDescriptor().isJsFunctionImplementation()) {
      sourceBuilder.appendln("return instance != null && instance.$is__" + mangledTypeName + ";");
    } else {
      String className =
          environment.aliasForType(
              type.isJsOverlayImplementation()
                  ? type.getNativeTypeDescriptor().getRawTypeDescriptor()
                  : type.getDescriptor().getUnsafeTypeDescriptor());
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
    if (type.isJsOverlayImplementation()) {
      sourceBuilder.append("return true;");
    } else if (type.getDescriptor().isJsFunctionInterface()) {
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
    if (type.isJsOverlayImplementation()
        || type.containsMethod(MethodDescriptor.IS_ASSIGNABLE_FROM_METHOD_NAME)) {
      return; // Don't render for overlay types or if the method exists.
    }
    sourceBuilder.appendLines(
        "/**",
        " * Returns whether the provided class is or extends this class.",
        " * @param {" + TypeDescriptors.NATIVE_FUNCTION.getQualifiedJsName() + "} classConstructor",
        " * @return {boolean}",
        " * @public",
        " */",
        "static $isAssignableFrom(classConstructor) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();

    if (type.isInterface()) { // For interfaces
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
    if (!type.getDescriptor().isJsFunctionImplementation()) {
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
    for (Field field : type.getInstanceFields()) {
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
    String utilAlias = environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());

    String name = null;
    if (type.isJsOverlayImplementation()) {
      name = type.getNativeTypeDescriptor().getQualifiedJsName();
    } else if (type.getDescriptor().isJsFunctionInterface()) {
      name = "Function";
    } else {
      name = type.getDescriptor().getQualifiedBinaryName();
    }

    String obfuscatableName = utilAlias + ".$makeClassName('" + name + "')";
    if (type.isInterface()) {
      sourceBuilder.appendln(
          utilAlias
              + ".$setClassMetadataForInterface("
              + className
              + ", "
              + obfuscatableName
              + ");");
    } else if (type.isEnum()) {
      sourceBuilder.appendln(
          utilAlias + ".$setClassMetadataForEnum(" + className + ", " + obfuscatableName + ");");
    } else {
      sourceBuilder.appendln(
          utilAlias + ".$setClassMetadata(" + className + ", " + obfuscatableName + ");");
    }
  }

  private void renderStaticFieldGettersSetters() {
    for (Field staticField : type.getStaticFields()) {
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

    // Set this method to reference an empty function so that it will not be executed again.
    sourceBuilder.newLine();
    sourceBuilder.append(className + ".$clinit = function() {};");

    // goog.module.get(...) for lazy imports.
    for (Import lazyImport : sortImports(importsByCategory.get(ImportCategory.LAZY))) {
      String alias = lazyImport.getAlias();
      String path = lazyImport.getImplModulePath();
      sourceBuilder.newLine();
      sourceBuilder.append(alias + " = goog.module.get('" + path + "');");
    }

    // Static field and static initializer blocks.
    renderInitializerElements(type.getStaticMembers());

    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO(b/34928687): Move this to the ast in a normalization pass.
  private void renderInitMethod() {
    if (type.isJsOverlayImplementation() || type.isInterface()) {
      return;
    }
    sourceBuilder.appendLines(
        "/**",
        " * Runs instance field and block initializers.",
        " * @private",
        " */",
        "$init__" + mangledTypeName + "() ");
    sourceBuilder.openBrace();
    renderInitializerElements(type.getInstanceMembers());
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  private void renderInitializerElements(Iterable<Member> members) {
    for (Member member : members) {
      if (member instanceof Field) {
        Field field = (Field) member;
        if (field.hasInitializer() && !field.isCompileTimeConstant()) {
          sourceBuilder.newLine();
          FilePosition startPosition = sourceBuilder.getCurrentPosition();
          boolean isInstanceField = !field.getDescriptor().isStatic();
          String fieldName =
              ManglingNameUtils.getMangledName(field.getDescriptor(), !isInstanceField);
          sourceBuilder.append((isInstanceField ? "this" : className) + "." + fieldName + " = ");
          renderExpression(field.getInitializer());
          sourceBuilder.append(";");
          sourceBuilder.addMapping(
              field.getSourcePosition(),
              new SourcePosition(startPosition, sourceBuilder.getCurrentPosition()));
        }
      } else if (member instanceof InitializerBlock) {
        InitializerBlock block = (InitializerBlock) member;
        for (Statement initializer : block.getBlock().getStatements()) {
          sourceBuilder.newLine();
          statementTranspiler.renderStatement(initializer);
        }
      }
      // other members are not involved in class initialization, hence they are skipped here.
    }
  }

  private void renderStaticFieldDeclarations() {
    for (Field staticField : type.getStaticFields()) {
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
    if (type.isJsOverlayImplementation()) {
      return; // Do nothing
    }
    if (type.isInterface()) {
      // TODO: remove cast after b/20102666 is handled in Closure.
      sourceBuilder.appendln(
          className
              + ".$markImplementor(/** @type {"
              + TypeDescriptors.NATIVE_FUNCTION.getQualifiedJsName()
              + "} */ ("
              + className
              + "));");
    } else { // Not an interface so it is a Class.
      for (TypeDescriptor interfaceTypeDescriptor : type.getSuperInterfaceTypeDescriptors()) {
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
      sourceBuilder.appendLines(
          "/**",
          " * Native Method Injection",
          " */",
          "// Alias for the class defined in this module",
          "const __class = " + className + ";");
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

  /**
   * Give the SourceMap file construction library enough information to be able to generate all of
   * the required empty group elements between the last mapping and the end of the file.
   */
  // TODO(stalcup): switch to generator.setFileLength() when that becomes possible.
  private void recordFileLengthInSourceMap() {
    // The JS position must have non-zero size otherwise the mapping will be ignored.
    FilePosition beforePosition = sourceBuilder.getCurrentPosition();
    sourceBuilder.append(" ");
    FilePosition afterPosition = sourceBuilder.getCurrentPosition();
    SourcePosition jsPosition = new SourcePosition(beforePosition, afterPosition);

    sourceBuilder.addMapping(SourcePosition.DUMMY, jsPosition);
  }
}
