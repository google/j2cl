"""j2cl_java_lib build rule.

This is similar to java_library but based on java_common.provider so we could
drive the compilation from skylark.

"""

load("//build_def:j2cl_transpile.bzl", "J2CL_TRANSPILE_ATTRS", "j2cl_transpile")

def _impl(ctx):
    srcs = [_strip_gwt_incompatible(ctx)] if ctx.files.srcs else []
    deps = [dep[JavaInfo] for dep in ctx.attr.deps]
    exports = [export[JavaInfo] for export in ctx.attr.exports]
    plugins = [p[JavaInfo] for p in ctx.attr.plugins]
    exported_plugins = [p[JavaInfo] for p in ctx.attr.exported_plugins]

    java_provider = java_common.compile(
        ctx,
        source_files = ctx.files.srcs_hack,
        source_jars = srcs,
        output = ctx.outputs.jar,
        javac_opts = java_common.default_javac_opts(
            ctx,
            java_toolchain_attr = "_java_toolchain",
        ),
        deps = deps,
        exports = exports,
        plugins = plugins,
        exported_plugins = exported_plugins,
        java_toolchain = ctx.attr._java_toolchain,
        host_javabase = ctx.attr._host_javabase,
    )

    j2cl_transpile(ctx, java_provider)

    return [
        DefaultInfo(files = depset([ctx.outputs.jar])),
        java_provider,
    ]

def _strip_gwt_incompatible(ctx):
    output_file = ctx.actions.declare_file(ctx.label.name + "_stripped-src.jar")

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add("-d", output_file)
    args.add_all(ctx.files.srcs)

    ctx.actions.run(
        progress_message = "Stripping @GwtIncompatible",
        inputs = ctx.files.srcs,
        outputs = [output_file],
        executable = ctx.executable._stripper,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "GwtIncompatibleStripper",
    )

    return output_file

j2cl_java_library = rule(
    implementation = _impl,
    attrs = dict(J2CL_TRANSPILE_ATTRS, **{
        "srcs": attr.label_list(allow_files = True),
        "srcs_hack": attr.label_list(allow_files = True),
        "deps": attr.label_list(providers = [JavaInfo]),
        "exports": attr.label_list(providers = [JavaInfo]),
        "plugins": attr.label_list(providers = [JavaInfo]),
        "exported_plugins": attr.label_list(providers = [JavaInfo]),
        "javacopts": attr.string_list(),
        "resources": attr.label_list(allow_files = True),  # TODO(goktug): remove
        "licenses": attr.license(),  # TODO(goktug): remove
        "_java_toolchain": attr.label(
            default = Label("//tools/jdk:toolchain"),
        ),
        "_host_javabase": attr.label(
            default = Label("//tools/jdk:current_host_java_runtime"),
            cfg = "host",
        ),
        "_stripper": attr.label(
            default = Label("//internal_do_not_use:GwtIncompatibleStripper"),
            cfg = "host",
            executable = True,
        ),
    }),
    fragments = ["java"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
        "zip_file": "%{name}.js.zip",
    },
)
