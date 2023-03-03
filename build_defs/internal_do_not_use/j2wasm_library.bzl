"""j2wasm_library build macro

Takes Java source, translates it into Wasm.
This is an experimental tool and should not be used.
"""

load(":j2cl_common.bzl", "J2CL_JAVA_TOOLCHAIN_ATTRS")
load(":j2cl_js_common.bzl", "J2CL_JS_TOOLCHAIN_ATTRS", "JS_PROVIDER_NAME")
load(":j2wasm_common.bzl", "j2wasm_common")
load("//build_defs/internal_do_not_use:provider.bzl", "J2wasmInfo")

J2WASM_LIB_ATTRS = {
    "srcs": attr.label_list(allow_files = [".java", ".srcjar", ".jar"]),
    "deps": attr.label_list(providers = [[J2wasmInfo], [JS_PROVIDER_NAME]]),
    "exports": attr.label_list(providers = [J2wasmInfo]),
    "plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "exec"),
    "exported_plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "exec"),
    "javacopts": attr.string_list(),
}

J2WASM_LIB_ATTRS.update(J2CL_JAVA_TOOLCHAIN_ATTRS)
J2WASM_LIB_ATTRS.update(J2CL_JS_TOOLCHAIN_ATTRS)

# Override the java toolchain to use the j2wasm one.
J2WASM_LIB_ATTRS.update({
    "_java_toolchain": attr.label(
        default = Label("//build_defs/internal_do_not_use:j2wasm_java_toolchain"),
    ),
})

def _impl_j2wasm_library_rule(ctx):
    plugin_provider = getattr(java_common, "JavaPluginInfo") if hasattr(java_common, "JavaPluginInfo") else JavaInfo
    return [j2wasm_common.compile(
        ctx = ctx,
        name = ctx.label.name,
        srcs = ctx.files.srcs,
        deps = _j2wasm_or_js_providers_of(ctx.attr.deps),
        exports = _j2wasm_or_js_providers_of(ctx.attr.exports),
        plugins = [p[plugin_provider] for p in ctx.attr.plugins],
        exported_plugins = [p[plugin_provider] for p in ctx.attr.exported_plugins],
        output_jar = ctx.outputs.jar,
        javac_opts = ctx.attr.javacopts,
    )]

def _j2wasm_or_js_providers_of(deps):
    return [_j2wasm_or_js_provider_of(d) for d in deps]

def _j2wasm_or_js_provider_of(dep):
    return dep[J2wasmInfo] if J2wasmInfo in dep else dep[JS_PROVIDER_NAME]

j2wasm_library = rule(
    implementation = _impl_j2wasm_library_rule,
    attrs = J2WASM_LIB_ATTRS,
    fragments = ["java", "js"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
    },
)
