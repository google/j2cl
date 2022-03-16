"""j2kt_import build macro

Takes nonstandard input and repackages it with names that will allow the
j2kt_import() target to be directly depended upon from j2kt_library() targets.

Should only be used for importing annotation byte code, otherwise may result
in hard to debug errors!
"""

load(":provider.bzl", "J2ktInfo")

def _j2kt_import_impl(ctx):
    java_info = ctx.attr.jar[JavaInfo]
    return [
        J2ktInfo(
            _private_ = struct(
                transitive_srcs = depset(),
                transitive_classpath = java_info.compile_jars,
                java_info = java_info,
            ),
        ),
        java_info,
    ]

j2kt_jvm_import = rule(
    implementation = _j2kt_import_impl,
    fragments = ["java"],
    attrs = {"jar": attr.label(providers = [JavaInfo])},
)
