"""j2wasm_import build macro

Takes nonstandard input and repackages it with names that will allow the
j2wasm_import() target to be directly depended upon from j2wasm_library() targets.

Should only be used for importing annotation byte code, otherwise may result
in hard to debug errors!
"""

load("@rules_java//java:defs.bzl", "JavaInfo")
load(":j2cl_js_common.bzl", "J2CL_JS_TOOLCHAIN_ATTRS", "j2cl_js_provider")
load(":j2wasm_common.bzl", "j2wasm_common")

def _j2wasm_import_impl(ctx):
    java_info = ctx.attr.jar[JavaInfo]

    # TODO(b/445759849): Return J2clInfo.
    packaged_j2wasm_info = struct(
        _private_ = struct(
            transitive_modules = depset(),
            java_info = java_info,
            js_info = j2cl_js_provider(ctx),
        ),
        _is_j2cl_provider = 1,
    )

    # We must return a provider per feature set entry. Since the import doesn't change depending on
    # feature set selection, we can just return the same provider for all feature sets.
    return [j2wasm_common.create_j2wasm_info(
        lambda feature_set: packaged_j2wasm_info,
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
