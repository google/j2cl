"""j2wasm_library build macro

Takes Java source, translates it into Wasm.
This is an experimental tool and should not be used.
"""

load("@rules_java//java:defs.bzl", "JavaPluginInfo")
load("//build_defs/internal_do_not_use:provider.bzl", "J2wasmInfo")
load(":j2cl_js_common.bzl", "JsInfo")
load(":j2wasm_common.bzl", "J2WASM_TOOLCHAIN_ATTRS", "j2wasm_common")

J2WASM_LIB_ATTRS = {
    "srcs": attr.label_list(allow_files = [".java", ".srcjar", ".jar", ".js"]),
    "deps": attr.label_list(providers = [[J2wasmInfo], [JsInfo]]),
    "exports": attr.label_list(providers = [[J2wasmInfo], [JsInfo]]),
    "plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "exec"),
    "exported_plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "exec"),
    "optimize_autovalue": attr.bool(default = True),
    "javacopts": attr.string_list(),
}

J2WASM_LIB_ATTRS.update(J2WASM_TOOLCHAIN_ATTRS)

def _impl_j2wasm_library_rule(ctx):
    extra_javacopts = ["-Adagger.fastInit=enabled"]
    if ctx.attr.optimize_autovalue:
        extra_javacopts.append("-Acom.google.auto.value.OmitIdentifiers")

    j2wasm_provider = j2wasm_common.compile(
        ctx = ctx,
        name = ctx.label.name,
        srcs = ctx.files.srcs,
        deps = _j2wasm_or_js_providers_of(ctx.attr.deps),
        exports = _j2wasm_or_js_providers_of(ctx.attr.exports),
        plugins = [p[JavaPluginInfo] for p in ctx.attr.plugins],
        exported_plugins = [p[JavaPluginInfo] for p in ctx.attr.exported_plugins],
        javac_opts = extra_javacopts + ctx.attr.javacopts,
    )

    outputs = [ctx.outputs.jar]
    output = j2wasm_provider._private_.output
    if output:
        outputs.append(output)

    return [DefaultInfo(files = depset(outputs)), j2wasm_provider]

def _j2wasm_or_js_providers_of(deps):
    return [_j2wasm_or_js_provider_of(d) for d in deps]

def _j2wasm_or_js_provider_of(dep):
    return dep[J2wasmInfo] if J2wasmInfo in dep else dep[JsInfo]

j2wasm_library = rule(
    implementation = _impl_j2wasm_library_rule,
    attrs = J2WASM_LIB_ATTRS,
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    fragments = ["java", "js"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
    },
)
