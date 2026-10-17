"""
Creates a well formed srcjar from sources (i.e. properly rooted according to packages).
"""

def archive_relative_path(path):
    """Returns the srcjar entry name for a source path.

    Deletes the shortest prefix matching '*/jre/java/', then a leading 'javasynth/'
    only when that deletion produced it. A path that merely starts with 'jre/java/'
    is unchanged.
    """
    marker = "/jre/java/"
    index = path.find(marker)
    if index != -1:
        path = path[index + len(marker):]
        javasynth_prefix = "javasynth/"
        if path.startswith(javasynth_prefix):
            path = path[len(javasynth_prefix):]
    return path

def _impl(ctx):
    zip_tool = ctx.executable._zip
    src_jar = ctx.outputs.srcjar
    excludes = [
        x.label.name.replace("super-wasm/", "")
        for x in (ctx.attr.super_srcs + ctx.attr.excludes)
    ]
    all_srcs = [f for f in ctx.files.srcs if not any([f.path.endswith(x) for x in excludes])]
    all_srcs += ctx.files.super_srcs

    # c and the archive stay on the command line. The entry list goes through
    # @paramfile so Windows CreateProcess never sees one argument per source.
    args = ctx.actions.args()
    args.add("c")
    args.add(src_jar)

    entry_args = ctx.actions.args()
    entry_args.use_param_file("@%s", use_always = True)
    entry_args.set_param_file_format("multiline")
    entry_args.add_all([
        archive_relative_path(src.path) + "=" + src.path
        for src in all_srcs
    ])

    ctx.actions.run(
        inputs = all_srcs,
        outputs = [src_jar],
        executable = zip_tool,
        mnemonic = "J2clPackageSources",
        arguments = [args, entry_args],
    )

    return DefaultInfo(files = depset([src_jar]))

package_sources = rule(
    implementation = _impl,
    attrs = {
        "srcs": attr.label_list(mandatory = True, allow_files = [".java"]),
        "super_srcs": attr.label_list(allow_files = [".java"]),
        "excludes": attr.label_list(allow_files = [".java"]),
        "_zip": attr.label(
            executable = True,
            cfg = "exec",
            default = Label("@bazel_tools//tools/zip:zipper"),
        ),
    },
    outputs = {"srcjar": "%{name}.srcjar"},
)
