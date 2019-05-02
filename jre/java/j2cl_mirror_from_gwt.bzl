"""j2cl_mirror_from_gwt build rule

Creates a j2cl_library target from existing GWT source by automatically
overlaying files from current directory.

"""

load("//build_defs:rules.bzl", "j2cl_library")

def _impl_source_copy(ctx):
    java_out_files = []
    for java_file in ctx.files.srcs:
        out_file_name = java_file.path
        if any([out_file_name.endswith(x) for x in ctx.attr.excludes]):
            continue

        java_file_artifact = ctx.actions.declare_file(out_file_name)
        java_out_files += [java_file_artifact]
        ctx.actions.run_shell(
            inputs = [java_file],
            outputs = [java_file_artifact],
            command = "cp $1 $2",
            arguments = [java_file.path, java_file_artifact.path],
        )

    return DefaultInfo(files = depset(java_out_files))

# Copies Java source from one place in the repo to another.
java_source_copy = rule(
    implementation = _impl_source_copy,
    attrs = {
        "srcs": attr.label_list(mandatory = True, allow_files = [".java"]),
        "excludes": attr.string_list(),
    },
)

def j2cl_mirror_from_gwt(
        name,
        mirrored_files,
        extra_js_srcs = [],
        excluded_srcs = [],
        deps = [],
        **kwargs):
    super_srcs = native.glob(["**/*.java"], exclude = excluded_srcs)
    native_srcs = native.glob(["**/*.js"]) + extra_js_srcs

    java_source_copy(
        name = name + "_copy",
        srcs = mirrored_files,
        excludes = super_srcs,
    )

    native.filegroup(
        name = name + "_java_files",
        srcs = [":" + name + "_copy"] + super_srcs,
    )

    native.filegroup(
        name = name + "_native_files",
        srcs = native_srcs,
    )

    j2cl_library(
        name = name,
        srcs = [
            ":" + name + "_java_files",
            ":" + name + "_native_files",
        ],
        deps = deps,
        **kwargs
    )
