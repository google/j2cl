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
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor.ImportCategory;
import com.google.j2cl.generator.visitors.ImportUtils;

import java.util.List;

/**
 * Generates JavaScript source impl files.
 */
public class JavaScriptImplGenerator extends JavaScriptGenerator {
  private String nativeSource;
  private String relativeSourceMapLocation;

  private String className;
  private String mangledTypeName;

  private boolean subclassesJsConstructorClass;

  protected StatementTransformer statementTransformer;

  public JavaScriptImplGenerator(Errors errors, boolean declareLegacyNamespace, JavaType javaType) {
    super(errors, declareLegacyNamespace, javaType);
    this.className = expressionToString(javaType.getDescriptor());
    this.mangledTypeName = ManglingNameUtils.getMangledName(javaType.getDescriptor());
    this.subclassesJsConstructorClass = javaType.getDescriptor().subclassesJsConstructorClass();
    statementTransformer = new StatementTransformer(sb, environment);
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
  public String toSource() {
    renderFileOverview();
    renderImports();
    renderTypeAnnotation();
    renderTypeBody();
    renderStaticFieldDeclarations();
    renderClassLiteralFieldDeclaration();
    renderMarkImplementorCalls();
    renderNativeSource();
    renderExports();
    renderSourceMapLocation();
    return sb.build();
  }

  private void renderFileOverview() {
    String transpiledFrom = javaType.getDescriptor().getRawTypeDescriptor().getBinaryName();
    sb.appendln("/**");
    sb.appendln(" * @fileoverview Impl transpiled from " + transpiledFrom + ".");
    sb.appendln(" *");
    sb.appendln(
        " * @suppress {suspiciousCode, transitionalSuspiciousCodeWarnings, uselessCode, const}");
    sb.appendln(" */");
  }

  private void renderImports() {
    TypeDescriptor selfTypeDescriptor = javaType.getDescriptor().getRawTypeDescriptor();
    Import selfImport = new Import(selfTypeDescriptor.getSimpleName(), selfTypeDescriptor);

    // goog.module(...) declaration.
    sb.appendln("goog.module('%s');", selfImport.getImplModulePath());
    if (declareLegacyNamespace) {
      sb.appendln("goog.module.declareLegacyNamespace();");
    }
    sb.newLine();
    sb.newLine();

    // goog.require(...) for eager imports.
    List<Import> eagerImports = ImportUtils.sortedList(importsByCategory.get(ImportCategory.EAGER));
    if (!eagerImports.isEmpty()) {
      for (Import eagerImport : eagerImports) {
        String alias = eagerImport.getAlias();
        String path = eagerImport.getImplModulePath();
        sb.appendln("let %s = goog.require('%s');", alias, path);
      }
      sb.newLine();
    }

    // goog.forwardDeclare(...) for lazy imports.
    List<Import> lazyImports = ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY));
    if (!lazyImports.isEmpty()) {
      for (Import lazyImport : lazyImports) {
        String alias = lazyImport.getAlias();
        String path = lazyImport.getImplModulePath();
        sb.appendln("let %s = goog.forwardDeclare('%s');", alias, path);
      }
      sb.newLine();
    }

    // = window.Blah; for extern imports (this is really just alias creation).
    List<Import> externImports =
        ImportUtils.sortedList(importsByCategory.get(ImportCategory.EXTERN));
    if (!externImports.isEmpty()) {
      for (Import externImport : externImports) {
        String alias = externImport.getAlias();
        String path = externImport.getImplModulePath();
        sb.appendln("let %s = %s;", alias, path);
      }
      sb.newLine();
    }

    sb.newLine();
  }

  private void renderTypeAnnotation() {
    if (javaType.isJsOverlayImpl()) {
      // Do nothing.
    } else if (javaType.isInterface()) {
      sb.appendln("/**");
      sb.appendln(" * @interface");
      if (javaType.getDescriptor().isParameterizedType()) {
        String templates = getJsDocNames(javaType.getDescriptor().getTypeArgumentDescriptors());
        sb.appendln(" * @template %s", templates);
      }
      for (TypeDescriptor superInterfaceType : javaType.getSuperInterfaceTypeDescriptors()) {
        sb.appendln(" * @extends {%s}", getJsDocName(superInterfaceType, true));
      }
      sb.appendln(" */");
    } else { // Not an interface so it is a Class.
      if (!GeneratorUtils.hasJsDoc(javaType)) {
        return;
      }
      sb.appendln("/**");
      if (javaType.isAbstract()) {
        sb.appendln(" * Abstract class, do not instantiate.");
      }
      if (javaType.getDescriptor().isParameterizedType()) {
        String templates = getJsDocNames(javaType.getDescriptor().getTypeArgumentDescriptors());
        sb.appendln(" * @template %s", templates);
      }
      if (javaType.getSuperTypeDescriptor().isParameterizedType()) {
        String templates = getJsDocName(javaType.getSuperTypeDescriptor(), true);
        sb.appendln(" * @extends {%s}", templates);
      }
      for (TypeDescriptor superInterfaceType : javaType.getSuperInterfaceTypeDescriptors()) {
        sb.appendln(" * @implements {%s}", getJsDocName(superInterfaceType, true));
      }
      sb.appendln(" */");
    }
  }

  private void renderTypeBody() {
    String extendsClause = GeneratorUtils.getExtendsClause(javaType, environment);
    sb.appendln("class %s %s{", className, extendsClause);
    renderConstructor();
    renderJavaTypeMethods();
    renderMarkImplementorMethod();
    renderIsInstanceMethod();
    renderIsAssignableFromMethod();
    renderCopyMethod();
    renderGetClassMethod();
    renderStaticFieldGettersSetters();
    renderClinit();
    renderInit();
    sb.appendln("};");
    sb.newLine();
    sb.newLine();
  }

  private void renderJavaTypeMethods() {
    for (Method method : javaType.getMethods()) {
      if (method.isConstructor()) {
        if (GeneratorUtils.isBoxedTypeAsPrimitive(javaType.getDescriptor())) {
          sb.appendln("/**");
          sb.appendln(" * Constructor function implementation is provided separately.");
          sb.appendln(" */");
          sb.newLine();
          continue;
        }
        renderFactoryCreateMethod(method);
        if (subclassesJsConstructorClass) {
          method = GeneratorUtils.createNonPrimaryCtor(method);
        }
      }
      if (GeneratorUtils.shouldNotEmitCode(method)) {
        continue;
      }
      if (method.getDescriptor().isJsOverlay() && !method.getDescriptor().isStatic()) {
        continue;
      }
      sb.appendln("/**");
      if (method.getJsDocDescription() != null && !method.getJsDocDescription().isEmpty()) {
        sb.appendln(" * %s", method.getJsDocDescription());
      }
      if (method.isAbstract()) {
        sb.appendln(" * Abstract method.");
      }
      if (method.isOverride() && !method.isConstructor()) {
        sb.appendln(" * @override");
      }
      if (!method.getDescriptor().getTypeParameterTypeDescriptors().isEmpty()) {
        String templateParamNames =
            JsDocNameUtils.getJsDocNames(
                method.getDescriptor().getTypeParameterTypeDescriptors(), environment);
        sb.appendln(" * @template %s", templateParamNames);
      }
      if (javaType.getDescriptor().isJsFunctionImplementation()
          && !method.getDescriptor().isStaticDispatch()
          && !method.getBody().getStatements().isEmpty()) {
        sb.appendln(
            " * @this {%s}", JsDocNameUtils.getJsDocName(javaType.getDescriptor(), environment));
      }
      for (String paramTypeName :
          GeneratorUtils.getParameterAnnotationsJsDoc(method, environment)) {
        sb.appendln(" * @param %s", paramTypeName);
      }
      String returnTypeName =
          JsDocNameUtils.getJsDocName(
              method.getDescriptor().getReturnTypeDescriptor(), environment);
      sb.appendln(" * @return {%s}", returnTypeName);
      sb.appendln(" * @%s", GeneratorUtils.visibilityForMethod(method));
      sb.appendln(" */");
      if (method.isNative()) {
        sb.appendln("  // native " + GeneratorUtils.getMethodHeader(method, environment));
      } else {
        sb.appendln(GeneratorUtils.getMethodHeader(method, environment));
        statementTransformer.renderStatement(method.getBody());
      }
      sb.newLine();
    }
  }

  private void renderMarkImplementorMethod() {
    if (!javaType.isInterface() || javaType.isJsOverlayImpl()) {
      return; // Only render markImplementor code for interfaces.
    }
    sb.appendln("/**");
    sb.appendln(" * Marks the provided class as implementing this interface.");
    sb.appendln(" * @param {window.Function} classConstructor");
    sb.appendln(" * @public");
    sb.appendln(" */");
    sb.appendln("static $markImplementor(classConstructor) {");
    for (TypeDescriptor superInterface : javaType.getSuperInterfaceTypeDescriptors()) {
      if (superInterface.isNative()) {
        continue;
      }
      String superInterfaceName = expressionToString(superInterface);
      sb.appendln("%s.$markImplementor(classConstructor);", superInterfaceName);
    }
    sb.appendln("/**");
    sb.appendln(" * @public {boolean}");
    sb.appendln(" */");
    sb.appendln("classConstructor.prototype.$implements__%s = true;", mangledTypeName);
    sb.appendln("}");
    sb.newLine();
  }

  private void renderFactoryCreateMethod(Method constructor) {
    String mangledNameOfCreate =
        ManglingNameUtils.getConstructorMangledName(constructor.getDescriptor());
    String mangledNameOfCtor = ManglingNameUtils.getCtorMangledName(constructor.getDescriptor());
    String parameterList = GeneratorUtils.getParameterList(constructor, environment);
    sb.appendln("/**");
    sb.appendln(" * A particular Java constructor as a factory method.");
    if (javaType.getDescriptor().isParameterizedType()) {
      String jsDocParams = getJsDocNames(javaType.getDescriptor().getTypeArgumentDescriptors());
      sb.appendln(" * @template %s", jsDocParams);
    }
    if (!constructor.getDescriptor().getTypeParameterTypeDescriptors().isEmpty()) {
      String jsDocParams =
          getJsDocNames(constructor.getDescriptor().getTypeParameterTypeDescriptors());
      sb.appendln(" * @template %s", jsDocParams);
    }
    for (String paramTypeName :
        GeneratorUtils.getParameterAnnotationsJsDoc(constructor, environment)) {
      sb.appendln(" * @param %s", paramTypeName);
    }
    sb.appendln(" * @return {!%s}", getJsDocName(javaType.getDescriptor(), true));
    sb.appendln(" * @%s", constructor.getDescriptor().getVisibility().jsText);
    sb.appendln(" */");
    sb.appendln("static %s(%s) {", mangledNameOfCreate, parameterList);
    if (subclassesJsConstructorClass) {
      // All jsConstructor classes and their subclasses must have all constructors delegate
      // to the primary constructor.
      if (constructor == AstUtils.getPrimaryConstructor(javaType)) {
        sb.appendln("return new %s(%s);", className, parameterList);
        sb.appendln("}");
        sb.newLine();
        return;
      }
    }
    sb.appendln("%s.$clinit();", className);
    if (subclassesJsConstructorClass) {
      String argumentsList = GeneratorUtils.getNewInstanceArguments(constructor, environment);
      sb.appendln("let $instance = new %s(%s);", className, argumentsList);
    } else {
      sb.appendln("let $instance = new %s;", className);
    }
    sb.appendln("$instance.%s(%s);", mangledNameOfCtor, parameterList);
    sb.appendln("return $instance;");
    sb.appendln("}");
    sb.newLine();
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderIsInstanceMethod() {
    if (javaType.isJsOverlayImpl()) {
      renderIsInstanceForJsOverlayType();
    } else if (javaType.isInterface()) {
      renderIsInstanceForInterfaceType();
    } else { // Not an interface so it is a Class.
      renderIsInstanceForClassType();
    }
    sb.newLine();
  }

  private void renderIsInstanceForClassType() {
    if (GeneratorUtils.superBoxedTypeAsPrimitive(javaType.getDescriptor())) {
      sb.appendln("/**");
      sb.appendln(" * $isInstance() function implementation is provided separately.");
      sb.appendln(" */");
      return;
    }
    sb.appendln("/**");
    sb.appendln(" * Returns whether the provided instance is an instance of this class.");
    sb.appendln(" * @return {boolean}");
    sb.appendln(" * @public");
    sb.appendln(" */");
    sb.appendln("static $isInstance(instance) {");
    if (javaType.getDescriptor().isJsFunctionImplementation()) {
      sb.appendln("return instance != null && instance.$is__%s;", mangledTypeName);
    } else {
      String className = expressionToString(javaType.getDescriptor());
      sb.appendln("return instance instanceof %s;", className);
    }
    sb.appendln("}");
  }

  private void renderIsInstanceForInterfaceType() {
    sb.appendln("/**");
    sb.appendln(" * Returns whether the provided instance is of a class that implements this");
    sb.appendln(" * interface.");
    sb.appendln(" * @return {boolean}");
    sb.appendln(" * @public");
    sb.appendln(" */");
    sb.appendln("static $isInstance(instance) {");
    if (javaType.getDescriptor().isJsFunctionInterface()) {
      sb.appendln("return instance != null && typeof instance == \"function\";");
    } else {
      sb.appendln("return instance != null && instance.$implements__%s;", mangledTypeName);
    }
    sb.appendln("}");
  }

  private void renderIsInstanceForJsOverlayType() {
    sb.appendln("/**");
    sb.appendln(" * Returns whether the provided instance is an instance of this class.");
    sb.appendln(" * @return {boolean}");
    sb.appendln(" * @public");
    sb.appendln(" */");
    if (javaType.isInterface()) {
      sb.appendln("static $isInstance(instance) { return true; }");
    } else {
      String nativeClassName =
          expressionToString(javaType.getNativeTypeDescriptor().getRawTypeDescriptor());
      sb.appendln(
          "static $isInstance(instance) { return instance instanceof %s; }", nativeClassName);
    }
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderIsAssignableFromMethod() {
    if (javaType.isJsOverlayImpl()) {
      return; // Don't render for overlay types.
    }
    sb.appendln("/**");
    sb.appendln(" * Returns whether the provided class is or extends this class.");
    sb.appendln(" * @param {window.Function} classConstructor");
    sb.appendln(" * @return {boolean}");
    sb.appendln(" * @public");
    sb.appendln(" */");
    sb.appendln("static $isAssignableFrom(classConstructor) {");
    if (javaType.isInterface()) { // For interfaces
      sb.appendln(
          "return classConstructor != null && classConstructor.prototype.$implements__%s;",
          mangledTypeName);
    } else { // For classes
      BootstrapType.NATIVE_UTIL.getDescriptor();
      String utilAlias = expressionToString(BootstrapType.NATIVE_UTIL.getDescriptor());
      sb.appendln("return %s.$canCastClass(classConstructor, %s);", utilAlias, className);
    }
    sb.appendln("}");
    sb.newLine();
  }

  // TODO: Move this to the ast in a normalization pass.
  // TODO: may copy Objects methods (equals, hashCode, etc. ) as well.
  private void renderCopyMethod() {
    if (!javaType.getDescriptor().isJsFunctionImplementation()) {
      return; // Only render the $copy method for jsfunctions
    }
    sb.appendln("/**");
    sb.appendln(" * Copies the fields from {@code from} to {@code to}.");
    sb.appendln(" * @param {%s} from", className);
    sb.appendln(" * @param {*} to");
    sb.appendln(" * @public");
    sb.appendln(" */");
    sb.appendln("static $copy(from, to) {");
    for (Field field : javaType.getInstanceFields()) {
      String fieldName = ManglingNameUtils.getMangledName(field.getDescriptor());
      sb.appendln("to.%s = from.%s;", fieldName, fieldName);
    }
    sb.appendln("// Marks the object is an instance of this class.");
    sb.appendln("to.$is__%s = true;", mangledTypeName);
    sb.appendln("}");
    sb.newLine();
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderGetClassMethod() {
    if (javaType.isJsOverlayImpl()) {
      return; // Don't render $getClass for overlay types.
    }
    String classAlias =
        expressionToString(TypeDescriptors.get().javaLangClass.getRawTypeDescriptor());
    String utilAlias = expressionToString(BootstrapType.NATIVE_UTIL.getDescriptor());
    sb.appendln("/**");
    sb.appendln(" * @return {%s}", classAlias);
    sb.appendln(" * @public", classAlias);
    sb.appendln(" */");
    sb.appendln("static $getClass() {");
    sb.appendln(className + ".$clinit();");
    sb.appendln("if (!%s.$class%s_) {", className, className);
    if (javaType.isInterface()) {
      sb.appendln("%s.$class%s_ = %s.$createForInterface(", className, className, classAlias);
    } else if (javaType.isEnum()) {
      sb.appendln("%s.$class%s_ = %s.$createForEnum(", className, className, classAlias);
    } else {
      sb.appendln("%s.$class%s_ = %s.$createForClass(", className, className, classAlias);
    }
    sb.appendln("%s.$generateId('%s'),", utilAlias, javaType.getDescriptor().getSimpleName());
    sb.appendln("%s.$generateId('%s'),", utilAlias, javaType.getDescriptor().getBinaryName());
    sb.append(
        String.format("%s.$generateId('%s')", utilAlias, javaType.getDescriptor().getSourceName()));
    if (javaType.isEnum()) {
      sb.appendln(",null);");
    } else {
      sb.appendln(");");
    }
    sb.appendln("}");
    sb.appendln("return %s.$class%s_;", className, className);
    sb.appendln("}");
    sb.newLine();

    if (!javaType.isJsOverlayImpl() && !javaType.isInterface()) {
      sb.appendln("/**");
      TypeDescriptor superTypeDescriptor = javaType.getSuperTypeDescriptor();
      if (!superTypeDescriptor.isNative()) {
        sb.appendln(" * @override");

      }
      sb.appendln(" * @return {%s}", classAlias);
      sb.appendln(" * @public");
      sb.appendln(" */");
      sb.appendln("m_getClass() { return %s.$getClass(); }", className);
      sb.newLine();
    }
  }

  private void renderJsConstructor() {
    Method primaryConstructor = AstUtils.getPrimaryConstructor(javaType);
    String parameters = GeneratorUtils.getParameterList(primaryConstructor, environment);
    String superCallArguments = GeneratorUtils.getSuperArguments(primaryConstructor, environment);
    sb.appendln("/**");
    sb.appendln(" * Real constructor.");
    sb.appendln(" * @public");
    sb.appendln(" */");
    sb.appendln("  constructor(%s) {", parameters);
    sb.appendln("    %s.$clinit();", className);
    sb.appendln("    super(%s);", superCallArguments);
    renderFieldsInitialValues();
    String mangledCtorName =
        ManglingNameUtils.getCtorMangledName(primaryConstructor.getDescriptor());
    sb.appendln("this.%s(%s);", mangledCtorName, parameters);
    sb.appendln("}");
    sb.newLine();
  }

  private void renderConstructor() {
    if (javaType.isJsOverlayImpl() || javaType.isInterface()) {
      return;
    }
    if (subclassesJsConstructorClass) {
      renderJsConstructor();
      return;
    }
    sb.appendln("/**");
    sb.appendln(" * Defines instance fields.");
    sb.appendln(" * @private");
    sb.appendln(" */");
    sb.appendln("constructor() {");
    sb.appendln("super();");
    renderFieldsInitialValues();
    sb.appendln("}");
    sb.newLine();
  }

  // Sets all the instance fields to their default java value.
  private void renderFieldsInitialValues() {
    for (Field field : javaType.getInstanceFields()) {
      String fieldType = getJsDocName(field.getDescriptor().getTypeDescriptor());
      String fieldName = ManglingNameUtils.getMangledName(field.getDescriptor());
      String intialValue = expressionToString(GeneratorUtils.getInitialValue(field));
      sb.appendln("/**");
      sb.appendln(" * @%s {%s}", field.getDescriptor().getVisibility().jsText, fieldType);
      sb.appendln(" */");
      sb.appendln("this.%s = %s;", fieldName, intialValue);
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

      sb.appendln("/**");
      sb.appendln(" * A static field getter.");
      sb.appendln(" * @return {%s}", staticFieldType);
      sb.appendln(" * @%s", staticFieldVisibility.jsText);
      sb.appendln(" */");
      sb.appendln("static get %s() {", indirectStaticFieldName);
      sb.appendln("%s.$clinit();", className);
      sb.appendln("return %s.%s;", className, directStaticFieldAccess);
      sb.appendln("}");
      sb.newLine();

      sb.appendln("/**");
      sb.appendln(" * A static field setter.");
      sb.appendln(" * @param {%s} value", staticFieldType);
      sb.appendln(" * @return {void}", staticFieldType);
      sb.appendln(" * @%s", staticFieldVisibility.jsText);
      sb.appendln(" */");
      sb.appendln("static set %s(value) {", indirectStaticFieldName);
      sb.appendln("%s.$clinit();", className);
      sb.appendln("%s.%s = value;", className, directStaticFieldAccess);
      sb.appendln("}");
      sb.newLine();
    }
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderClinit() {
    List<Import> lazyImports = ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY));
    if (!GeneratorUtils.needClinit(javaType, lazyImports)) {
      return;
    }
    sb.appendln("/**");
    sb.appendln(" * Runs inline static field initializers.");
    sb.appendln(" * @public");
    sb.appendln(" */");
    environment.setClinitEnclosingTypeDescriptor(javaType.getDescriptor());
    sb.appendln("static $clinit() {");
    if (GeneratorUtils.needRewriteClinit(javaType)) {
      // Set this method to reference an empty function so that it cannot be called again.
      sb.appendln("%s.$clinit = function() {};", className);
    }
    // goog.module.get(...) for lazy imports.
    for (Import lazyImport : ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY))) {
      String alias = lazyImport.getAlias();
      String path = lazyImport.getImplModulePath();
      sb.appendln("%s = goog.module.get('%s');", alias, path);
    }
    if (GeneratorUtils.needCallSuperClinit(javaType)) {
      // call the super class $clinit.
      TypeDescriptor superTypeDescriptor = javaType.getSuperTypeDescriptor();
      String superTypeName = expressionToString(superTypeDescriptor);
      sb.appendln("%s.$clinit();", superTypeName);
    }
    // Static field and static initializer blocks.
    for (Object element : javaType.getStaticFieldsAndInitializerBlocks()) {
      if (element instanceof Field) {
        Field field = (Field) element;
        if (field.hasInitializer() && !field.isCompileTimeConstant()) {
          String fieldInitializer = expressionToString(field.getInitializer());
          String fieldName = ManglingNameUtils.getMangledName(field.getDescriptor());
          sb.appendln("%s.$%s = %s;", className, fieldName, fieldInitializer);
        }
      } else if (element instanceof Block) {
        Block block = (Block) element;
        for (Statement initializer : block.getStatements()) {
          statementTransformer.renderStatement(initializer);
        }
      } else {
        throw new UnsupportedOperationException("Unsupported element: " + element);
      }
    }

    sb.appendln("}");
    sb.newLine();
    environment.setClinitEnclosingTypeDescriptor(null);
  }

  // TODO: Move this to the ast in a normalization pass.
  private void renderInit() {
    if (javaType.isJsOverlayImpl() || javaType.isInterface()) {
      return;
    }
    sb.appendln("/**");
    sb.appendln(" * Runs instance field and block initializers.");
    sb.appendln(" * @private");
    sb.appendln(" */");
    String methodName = String.format("$init__%s", mangledTypeName);
    sb.appendln("%s() {", methodName);
    for (Object element : javaType.getInstanceFieldsAndInitializerBlocks()) {
      if (element instanceof Field) {
        Field field = (Field) element;
        if (field.hasInitializer() && !field.isCompileTimeConstant()) {
          String fieldInitializer = expressionToString(field.getInitializer());
          String fieldName = ManglingNameUtils.getMangledName(field.getDescriptor());
          sb.appendln("this.%s = %s;", fieldName, fieldInitializer);
        }
      } else if (element instanceof Block) {
        Block block = (Block) element;
        for (Statement initializer : block.getStatements()) {
          statementTransformer.renderStatement(initializer);
        }
      } else {
        throw new UnsupportedOperationException("Unsupported element: " + element);
      }
    }

    sb.appendln("}");
  }

  private void renderStaticFieldDeclarations() {
    for (Field staticField : javaType.getStaticFields()) {
      String jsDocType =
          JsDocNameUtils.getJsDocName(staticField.getDescriptor().getTypeDescriptor(), environment);
      String initialValue = expressionToString(GeneratorUtils.getInitialValue(staticField));
      if (staticField.isCompileTimeConstant()) {
        String publicFieldAccess =
            ManglingNameUtils.getMangledName(staticField.getDescriptor(), false);
        sb.appendln("/**");
        sb.appendln(" * @public {%s}", jsDocType);
        sb.appendln(" * @const");
        sb.appendln(" */");
        sb.appendln("%s.%s = %s;", className, publicFieldAccess, initialValue);
      } else {
        String privateFieldAccess =
            ManglingNameUtils.getMangledName(staticField.getDescriptor(), true);
        sb.appendln("/**");
        sb.appendln(" * @private {%s}", jsDocType);
        sb.appendln(" */");
        sb.appendln("%s.%s = %s;", className, privateFieldAccess, initialValue);
      }
      sb.newLine();
      sb.newLine();
    }
  }

  private void renderClassLiteralFieldDeclaration() {
    String classAlias =
        expressionToString(TypeDescriptors.get().javaLangClass.getRawTypeDescriptor());
    if (javaType.isJsOverlayImpl()) {
      return;
    }
    sb.appendln("/**");
    sb.appendln(" * The class literal field.");
    sb.appendln(" * @private {%s}", classAlias);
    sb.appendln(" */");
    sb.appendln("%s.$class%s_ = null;", className, className);
    sb.newLine();
    sb.newLine();
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
      sb.appendln(
          "%s.$markImplementor(/** @type {window.Function} */ (%s));", className, className);
    } else { // Not an interface so it is a Class.
      for (TypeDescriptor interfaceTypeDescriptor : javaType.getSuperInterfaceTypeDescriptors()) {
        if (interfaceTypeDescriptor.isNative()) {
          continue;
        }
        String interfaceName = expressionToString(interfaceTypeDescriptor);
        sb.appendln("%s.$markImplementor(%s);", interfaceName, className);
      }
    }
    sb.newLine();
    sb.newLine();
  }

  private void renderNativeSource() {
    if (nativeSource != null) {
      sb.appendln("/**");
      sb.appendln(" * Native Method Injection");
      sb.appendln(" */");
      sb.appendln(nativeSource);
      sb.newLine();
    }
  }

  private void renderExports() {
    sb.appendln("/**");
    sb.appendln(" * Export class.");
    sb.appendln(" */");
    sb.appendln("exports = %s;", className);
  }

  private void renderSourceMapLocation() {
    if (relativeSourceMapLocation != null) {
      sb.append(String.format("//# sourceMappingURL=%s", relativeSourceMapLocation));
    }
  }
}
