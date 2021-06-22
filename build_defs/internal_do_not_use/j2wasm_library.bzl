"""j2wasm_library build macro

Takes Java source, translates it into Wasm.
This is an experimental tool and should not be used.
"""

load(":j2cl_common.bzl", "J2CL_JAVA_TOOLCHAIN_ATTRS")
load(":j2wasm_common.bzl", "j2wasm_common")
load("//build_defs/internal_do_not_use:provider.bzl", "J2wasmInfo")

J2WASM_LIB_ATTRS = {
    "srcs": attr.label_list(allow_files = [".java", ".srcjar", ".jar"]),
    "deps": attr.label_list(providers = [J2wasmInfo]),
    "exports": attr.label_list(providers = [J2wasmInfo]),
    "plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "host"),
    "exported_plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "host"),
    "javacopts": attr.string_list(),
}

J2WASM_LIB_ATTRS.update(J2CL_JAVA_TOOLCHAIN_ATTRS)

def _impl_j2wasm_library_rule(ctx):
    plugin_provider = getattr(native, "JavaPluginInfo") if hasattr(native, "JavaPluginInfo") else JavaInfo
    default_j2cl_javac_opts = [
        # Avoid log site injection which introduces calls to unsupported APIs
        "-XDinjectLogSites=false",
    ]
    return [j2wasm_common.compile(
        ctx = ctx,
        name = ctx.label.name,
        srcs = ctx.files.srcs,
        deps = [d[J2wasmInfo] for d in ctx.attr.deps],
        exports = [e[J2wasmInfo] for e in ctx.attr.exports],
        plugins = [p[plugin_provider] for p in ctx.attr.plugins],
        exported_plugins = [p[plugin_provider] for p in ctx.attr.exported_plugins],
        output_jar = ctx.outputs.jar,
        javac_opts = default_j2cl_javac_opts + ctx.attr.javacopts,
    )]

j2wasm_library = rule(
    implementation = _impl_j2wasm_library_rule,
    attrs = J2WASM_LIB_ATTRS,
    fragments = ["java"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
    },
)
