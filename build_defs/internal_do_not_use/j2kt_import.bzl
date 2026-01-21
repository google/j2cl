"""j2kt_import build macro

Takes nonstandard input and repackages it with names that will allow the
j2kt_import() target to be directly depended upon from j2kt_library() targets.

Should only be used for importing annotation byte code, otherwise may result
in hard to debug errors!
"""

load("@rules_cc//cc/common:cc_common.bzl", "cc_common")
load("@rules_cc//cc/common:cc_info.bzl", "CcInfo")
load("@rules_java//java:defs.bzl", "JavaInfo")
load(":provider.bzl", "J2ktInfo")

def create_J2ktInfo_for_java_import(java_info):
    return J2ktInfo(
        _private_ = struct(
            java_info = java_info,
            transpile_header_out = [],
            j2kt_exports = depset(),
        ),
    )

def _j2kt_jvm_import_impl(ctx):
    kt_runtime_java_infos = []
    if ctx.attr.runtime:
        kt_runtime_java_infos.append(ctx.attr.runtime[JavaInfo])
    else:
        kt_runtime_java_infos.append(ctx.attr.jar[JavaInfo])

    return [create_J2ktInfo_for_java_import(ctx.attr.jar[JavaInfo])] + kt_runtime_java_infos

def _j2kt_native_import_impl(ctx):
    kt_native_infos = []
    swift_interop_infos = []
    default_files = [ctx.attr.jar[DefaultInfo].files]
    opt_providers = []

    return [
        DefaultInfo(files = depset(transitive = default_files)),
        create_J2ktInfo_for_java_import(ctx.attr.jar[JavaInfo]),
    ] + kt_native_infos + swift_interop_infos + opt_providers

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
        "jar": attr.label(
            providers = [JavaInfo],
            doc = """\
This Java Library is supplied to the binary at compilation time
 and is not visible at runtime.
""",
        ),
    },
    provides = [J2ktInfo],
)
