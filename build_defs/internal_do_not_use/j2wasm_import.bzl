"""j2wasm_import build macro

Takes nonstandard input and repackages it with names that will allow the
j2wasm_import() target to be directly depended upon from j2wasm_library() targets.

Should only be used for importing annotation byte code, otherwise may result
in hard to debug errors!
"""

load(":j2cl_js_common.bzl", "J2CL_JS_TOOLCHAIN_ATTRS", "j2cl_js_provider")
load(":provider.bzl", "J2wasmInfo")

def _j2wasm_import_impl(ctx):
    java_info = ctx.attr.jar[JavaInfo]
    return [J2wasmInfo(
        _private_ = struct(
            transitive_srcs = depset(),
            transitive_classpath = java_info.compile_jars,
            wasm_modular_info = struct(
                transitive_modules = depset(),
            ),
            java_info = java_info,
            js_info = j2cl_js_provider(ctx),
        ),
        _is_j2cl_provider = 1,
    )]

_J2WASM_IMPORT_ATTRS = {
    "jar": attr.label(providers = [JavaInfo]),
}
_J2WASM_IMPORT_ATTRS.update(J2CL_JS_TOOLCHAIN_ATTRS)

j2wasm_import = rule(
    implementation = _j2wasm_import_impl,
    fragments = ["java", "js"],
    attrs = _J2WASM_IMPORT_ATTRS,
)
