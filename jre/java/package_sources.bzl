"""
Creates a well formed srcjar from sources (i.e. properly rooted according to packages).
"""

def _filter(srcs, excludes):
    return [f for f in srcs if not any([f.path.endswith(x) for x in excludes])]

def _impl(ctx):
    zip_tool = ctx.executable._zip
    src_jar = ctx.outputs.srcjar
    super_excludes = [
        x.label.name.replace("super-wasm/", "").replace("super-kt/", "")
        for x in ctx.attr.super_srcs
    ]
    all_srcs = _filter(ctx.files.srcs, super_excludes)
    all_srcs += ctx.files.super_srcs
    all_srcs = _filter(all_srcs, ctx.attr.excludes)

    ctx.actions.run_shell(
        inputs = all_srcs,
        outputs = [src_jar],
        tools = [zip_tool],
        arguments = [f.path for f in all_srcs],
        command = "\n".join([
            "set -eu",
            "shopt -s extglob",
            # Process all source into a zip, paying attention to directory structure
            'zip_args="c %s"' % src_jar.path,
            'for src in "$@"',
            "do",
            # Extract source path relative to 'root'.
            # We will check all the potenital roots here...
            "  relative_name=${src#*/jre/java/}",
            "  relative_name=${relative_name#javasynth/}",
            '  zip_args+=" ${relative_name}=${src}"',
            "done",
            '"%s" $zip_args' % zip_tool.path,
        ]),
    )

    return DefaultInfo(files = depset([src_jar]))

package_sources = rule(
    implementation = _impl,
    attrs = {
        "srcs": attr.label_list(mandatory = True, allow_files = [".java"]),
        "super_srcs": attr.label_list(allow_files = [".java"]),
        "excludes": attr.string_list(),
        "_zip": attr.label(
            executable = True,
            cfg = "exec",
            default = Label("@bazel_tools//tools/zip:zipper"),
        ),
    },
    outputs = {"srcjar": "%{name}.srcjar"},
)
