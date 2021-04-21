"""Common utilities for creating J2WASM targets and providers."""

load(
    "//build_defs/internal_do_not_use:j2cl_common.bzl",
    "j2cl_common",
)
load("//build_defs/internal_do_not_use:provider.bzl", "J2wasmInfo")

def _compile(
        ctx,
        name,
        srcs = [],
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        output_jar = None,
        javac_opts = [],
        mnemonic = "J2Wasm"):
    java_provider = j2cl_common.java_compile(
        ctx = ctx,
        name = name,
        srcs = srcs,
        deps = _from_j2wasm_to_java_provider(deps),
        exports = _from_j2wasm_to_java_provider(exports),
        plugins = plugins,
        exported_plugins = exported_plugins,
        output_jar = output_jar,
        javac_opts = javac_opts,
        mnemonic = mnemonic,
    )

    return _create_j2wasm_provider(java_provider, deps + exports)

def _create_j2wasm_provider(java_provider, deps):
    return J2wasmInfo(
        _private_ = struct(
            transitive_srcs = depset(
                java_provider.source_jars,
                transitive = [d._private_.transitive_srcs for d in deps],
            ),
            transitive_classpath = depset(
                transitive = [d._private_.transitive_classpath for d in deps],
            ),
            java_info = java_provider,
        ),
    )

def _from_j2wasm_to_java_provider(j2wasm_providers):
    return [p._private_.java_info for p in j2wasm_providers]

def _to_j2wasm_name(name):
    """Convert a label name used in j2cl to be used in j2wasm"""
    if name.endswith("-j2cl"):
        name = name[:-5]
    return "%s-j2wasm" % name

j2wasm_common = struct(
    compile = _compile,
    to_j2wasm_name = _to_j2wasm_name,
)
