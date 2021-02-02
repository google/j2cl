"""
Creates a well formed srcjar from sources (i.e. properly rooted according to packages).
"""

def _overlay_sources(ctx, srcs, overlay_srcs, root = ""):
    all_srcs = [
        f
        for f in srcs
        if not any([f.path.endswith(x.label.name[len(root):]) for x in getattr(ctx.attr, overlay_srcs)])
    ]
    all_srcs += getattr(ctx.files, overlay_srcs)
    return all_srcs

def _impl(ctx):
    zip_tool = ctx.executable._zip
    src_jar = ctx.outputs.srcjar
    all_srcs = _overlay_sources(ctx, ctx.files.srcs, "super_srcs")
    all_srcs = _overlay_sources(ctx, all_srcs, "super_wasm_srcs", root = "super-wasm")
    all_srcs = [f for f in all_srcs if not any([f.path.endswith(x) for x in ctx.attr.excludes])]

    ctx.actions.run_shell(
        inputs = all_srcs,
        outputs = [src_jar],
        tools = [zip_tool],
        arguments = [f.path for f in all_srcs],
        command = "\n".join([
            "set -eu",
            "shopt -s extglob",
            # Process all source into a zip, paying attention to directory structure
            # This also generates a module-info for all sources.
            'zip_args="c %s"' % src_jar.path,
            'for src in "$@"',
            "do",
            # Extract source path relative to 'root'.
            # We will check all the potenital roots here...
            "  relative_name=${src#*/emul/}",
            "  relative_name=${relative_name#?(*/)jre/java/}",
            "  relative_name=${relative_name#javasynth/}",
            '  zip_args+=" ${relative_name}=${src}"',
            "done",
            # Zip all copied sources including the generated module-info
            '"%s" $zip_args' % zip_tool.path,
        ]),
    )

    return DefaultInfo(files = depset([src_jar]))

package_sources = rule(
    implementation = _impl,
    attrs = {
        "srcs": attr.label_list(mandatory = True, allow_files = [".java"]),
        "super_srcs": attr.label_list(allow_files = [".java"]),
        "super_wasm_srcs": attr.label_list(allow_files = [".java"]),
        "excludes": attr.string_list(),
        "_zip": attr.label(
            executable = True,
            cfg = "host",
            default = Label("@bazel_tools//tools/zip:zipper"),
        ),
    },
    outputs = {"srcjar": "%{name}.srcjar"},
)
