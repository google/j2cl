"""j2wasm_library build macro

Takes Java source, translates it into Wasm.
This is an experimental tool and should not be used.
"""

load(":j2cl_common.bzl", "J2CL_JAVA_TOOLCHAIN_ATTRS", "j2cl_common")

J2wasmInfo = provider("Internal J2wasm provider", fields = ["_private_"])

_J2WASM_LIB_ATTRS = {
    "srcs": attr.label_list(allow_files = [".java", ".srcjar", ".jar"]),
    "deps": attr.label_list(providers = [J2wasmInfo]),
    "exports": attr.label_list(providers = [J2wasmInfo]),
    "plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "host"),
    "exported_plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "host"),
    "javacopts": attr.string_list(),
}

_J2WASM_LIB_ATTRS.update(J2CL_JAVA_TOOLCHAIN_ATTRS)

def _impl_j2wasm_library(ctx):
    java_provider = j2cl_common.java_compile(
        ctx = ctx,
        name = ctx.label.name,
        srcs = ctx.files.srcs,
        deps = _to_java_providers(ctx.attr.deps),
        exports = _to_java_providers(ctx.attr.exports),
        plugins = ctx.attr.plugins,
        exported_plugins = ctx.attr.exported_plugins,
        output_jar = ctx.outputs.jar,
        javac_opts = ctx.attr.javacopts,
        mnemonic = "J2wasm",
    )

    all_deps = ctx.attr.deps + ctx.attr.exports

    return [J2wasmInfo(
        _private_ = struct(
            transitive_srcs = java_provider.transitive_source_jars,
            transitive_classpath = _get_transitive_classpath(java_provider, all_deps),
            java_info = java_provider,
        ),
    )]

# TODO(dramaix): Remove this when JRE is included in the srcs.
def _get_transitive_classpath(java_provider, deps):
    return depset(
        java_provider.compilation_info.boot_classpath,
        transitive = [java_provider.compilation_info.compilation_classpath] +
                     [d[J2wasmInfo]._private_.transitive_classpath for d in deps],
    )

def _to_java_providers(libs):
    return [lib[J2wasmInfo]._private_.java_info for lib in libs]

j2wasm_library = rule(
    implementation = _impl_j2wasm_library,
    attrs = _J2WASM_LIB_ATTRS,
    fragments = ["java"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
    },
)
