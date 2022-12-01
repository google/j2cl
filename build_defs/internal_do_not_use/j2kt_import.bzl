"""j2kt_import build macro

Takes nonstandard input and repackages it with names that will allow the
j2kt_import() target to be directly depended upon from j2kt_library() targets.

Should only be used for importing annotation byte code, otherwise may result
in hard to debug errors!
"""

load(":provider.bzl", "J2ktInfo")

def _j2kt_jvm_import_impl(ctx):
    kt_runtime_java_infos = []
    if ctx.attr.runtime:
        kt_runtime_java_infos.append(ctx.attr.runtime[JavaInfo])

    return [
        J2ktInfo(
            _private_ = struct(
                java_info = ctx.attr.jar[JavaInfo],
                java_srcs = depset(),
            ),
        ),
    ] + kt_runtime_java_infos

def _j2kt_native_import_impl(ctx):
    kt_native_infos = []

    return [
        J2ktInfo(
            _private_ = struct(
                java_info = ctx.attr.jar[JavaInfo],
                java_srcs = depset(),
            ),
        ),
    ] + kt_native_infos

j2kt_jvm_import = rule(
    implementation = _j2kt_jvm_import_impl,
    fragments = ["java"],
    attrs = {
        "jar": attr.label(providers = [JavaInfo]),
        "runtime": attr.label(providers = [JavaInfo]),
    },
    provides = [J2ktInfo],
)

j2kt_native_import = rule(
    implementation = _j2kt_native_import_impl,
    fragments = ["java"],
    attrs = {
        "jar": attr.label(providers = [JavaInfo]),
    },
    provides = [J2ktInfo],
)
