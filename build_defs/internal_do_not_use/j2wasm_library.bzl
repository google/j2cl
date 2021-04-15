"""j2wasm_library build macro

Takes Java source, translates it into Wasm.
This is an experimental tool and should not be used.
"""

load(":j2cl_common.bzl", "J2CL_JAVA_TOOLCHAIN_ATTRS", "j2cl_common")

J2wasmInfo = provider("Internal J2wasm provider", fields = ["_private_"])

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
    java_provider = j2cl_common.java_compile(
        ctx = ctx,
        name = ctx.label.name,
        srcs = ctx.files.srcs,
        deps = _get_java_providers_from_j2wasm(ctx.attr.deps),
        exports = _get_java_providers_from_j2wasm(ctx.attr.exports),
        plugins = _get_java_providers(ctx.attr.plugins),
        exported_plugins = _get_java_providers(ctx.attr.exported_plugins),
        output_jar = ctx.outputs.jar,
        javac_opts = ctx.attr.javacopts,
        mnemonic = "J2wasm",
    )

    all_deps = ctx.attr.deps + ctx.attr.exports

    return [J2wasmInfo(
        _private_ = struct(
            transitive_srcs = _get_transitive_sources(java_provider, all_deps),
            transitive_classpath = _get_transitive_classpath(all_deps),
            java_info = java_provider,
        ),
    )]

def _get_transitive_sources(java_provider, deps):
    return depset(java_provider.source_jars, transitive = [d[J2wasmInfo]._private_.transitive_srcs for d in deps])

def _get_transitive_classpath(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.transitive_classpath for d in deps])

def _get_java_providers_from_j2wasm(libs):
    return [lib[J2wasmInfo]._private_.java_info for lib in libs]

def _get_java_providers(deps):
    return [d[JavaInfo] for d in deps]

j2wasm_library = rule(
    implementation = _impl_j2wasm_library_rule,
    attrs = J2WASM_LIB_ATTRS,
    fragments = ["java"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
    },
)

# Helpers methods
def to_j2wasm_name(name):
    """Convert a label name used in j2cl to be used in j2wasm"""
    if name.endswith("-j2cl"):
        name = name[:-5]
    return "%s-j2wasm" % name
