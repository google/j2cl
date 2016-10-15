package com.google.j2cl.generator;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.ast.AnonymousType;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.errors.Errors;

/**
 * Generates files that are dependency forwarding proxies for native JsTypes.
 *
 * <p>Without these proxies, callers in other libraries who reference the native JsType classes
 * would have to directly import the native type for which the JsType is a proxy. This would be a
 * transitive import and illegal in some strict build systems.
 */
public class NativeJsTypeProxyGenerator extends JavaScriptGenerator {

  public static final String FILE_SUFFIX = ".java.js";

  public NativeJsTypeProxyGenerator(Errors errors, boolean declareLegacyNamespace, Type type) {
    super(errors, declareLegacyNamespace, type);
    checkArgument(type.getDescriptor().isNative() && !type.getDescriptor().isExtern());
  }

  @Override
  String getSuffix() {
    return FILE_SUFFIX;
  }

  @Override
  String renderOutput() {
    TypeDescriptor selfTypeDescriptor = type.getDescriptor().getRawTypeDescriptor();

    String binaryName = selfTypeDescriptor.getBinaryName();
    sourceBuilder.appendLines(
        "/**",
        " * @fileoverview native JsType proxy from " + binaryName,
        " */",
        "goog.module('" + selfTypeDescriptor.getModuleName() + "');");
    sourceBuilder.newLine();

    if (declareLegacyNamespace
        && selfTypeDescriptor.isJsType()
        && !(type instanceof AnonymousType)) {
      // Even if opted into declareLegacyNamespace, this only makes sense for classes that are
      // intended to be accessed from the native JS. Thus we only emit declareLegacyNamespace
      // for non-anonymous JsType classes.
      sourceBuilder.appendln("goog.module.declareLegacyNamespace();");
    }

    sourceBuilder.newLine();
    sourceBuilder.newLine();
    sourceBuilder.appendln("// Imports the real native type.");
    sourceBuilder.appendln(
        "let _nativeType = goog.require('" + selfTypeDescriptor.getProxiedQualifiedName() + "');");
    sourceBuilder.newLine();
    sourceBuilder.newLine();

    sourceBuilder.appendln("// Re-exports with a new name.");
    sourceBuilder.appendln("exports = _nativeType;");
    sourceBuilder.newLine();
    return sourceBuilder.build();
  }
}
