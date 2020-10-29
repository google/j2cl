"""j2wasm_import build macro

Takes nonstandard input and repackages it with names that will allow the
j2wasm_import() target to be directly depended upon from j2wasm_library() targets.

Should only be used for importing annotation byte code, otherwise may result
in hard to debug errors!
"""

load(":j2wasm_library.bzl", "J2wasmInfo")

def _j2wasm_import_impl(ctx):
    java_info = ctx.attr.jar[JavaInfo]
    return [J2wasmInfo(
        _private_ = struct(
            transitive_srcs = depset(),
            transitive_classpath = java_info.compile_jars,
            java_info = java_info,
        ),
    )]

j2wasm_import = rule(
    implementation = _j2wasm_import_impl,
    fragments = ["java"],
    attrs = {"jar": attr.label(providers = [JavaInfo])},
)
