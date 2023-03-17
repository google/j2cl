"""Common utilities for creating J2WASM targets and providers."""

load(
    "//build_defs/internal_do_not_use:j2cl_common.bzl",
    "j2cl_common",
)
load("//build_defs/internal_do_not_use:provider.bzl", "J2wasmInfo")
load(":j2cl_common.bzl", "split_deps")
load(":j2cl_js_common.bzl", "j2cl_js_provider")

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
    java_deps, js_deps = split_deps(deps)
    java_exports, js_exports = split_deps(exports)

    java_provider = j2cl_common.java_compile(
        ctx = ctx,
        name = name,
        srcs = srcs,
        deps = java_deps,
        exports = java_exports,
        plugins = plugins,
        exported_plugins = exported_plugins,
        output_jar = output_jar,
        javac_opts = javac_opts,
        mnemonic = mnemonic,
    )

    js_provider = j2cl_js_provider(
        ctx = ctx,
        # These are exports, because they will need to be referenced by the j2wasm_application
        # eventually downstream. They may not be direct dependencies.
        exports = js_deps + js_exports,
    )

    return _create_j2wasm_provider(java_provider, js_provider, deps + exports)

def _create_j2wasm_provider(java_provider, js_provider, deps):
    j2wasm_deps = [d for d in deps if hasattr(d, "_is_j2cl_provider")]
    return J2wasmInfo(
        _private_ = struct(
            transitive_srcs = depset(
                java_provider.source_jars,
                transitive = [d._private_.transitive_srcs for d in j2wasm_deps],
            ),
            transitive_classpath = depset(
                transitive = [d._private_.transitive_classpath for d in j2wasm_deps],
            ),
            java_info = java_provider,
            js_info = js_provider,
        ),
        _is_j2cl_provider = 1,
    )

def _to_j2wasm_name(name):
    """Convert a label name used in j2cl to be used in j2wasm"""
    if name.endswith("j2cl_proto"):
        name = name[:-10]
        return "%sj2wasm_proto" % name
    if name.endswith("-j2cl"):
        name = name[:-5]
    return "%s-j2wasm" % name

j2wasm_common = struct(
    compile = _compile,
    to_j2wasm_name = _to_j2wasm_name,
)
