"""j2wasm_application build macro

Takes Java source, translates it into Wasm.
This is an experimental tool and should not be used.
"""

load(":j2wasm_library.bzl", "J2wasmInfo")

def _impl_j2wasm_application(ctx):
    srcs = _get_transitive_srcs(ctx.attr.deps)
    classpath = _get_transitive_classpath(ctx.attr.deps)

    # TODO(dramaix): Make library_info optional in the transpiler and remove it here.
    output_library_info = ctx.actions.declare_file("%s_library_info" % ctx.label.name)

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add_joined("-classpath", classpath, join_with = ctx.configuration.host_path_separator)
    args.add("-output", ctx.outputs.zip)
    args.add("-experimentalBackend", "WASM")
    args.add("-libraryinfooutput", output_library_info)
    args.add_all(srcs)

    ctx.actions.run(
        progress_message = "Transpiling to WASM %s" % ctx.label,
        inputs = depset(transitive = [srcs, classpath]),
        outputs = [ctx.outputs.zip, output_library_info],
        executable = ctx.executable._j2cl_transpiler,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2wasm",
    )

def _get_transitive_srcs(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.transitive_srcs for d in deps])

def _get_transitive_classpath(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.transitive_classpath for d in deps])

j2wasm_application = rule(
    implementation = _impl_j2wasm_application,
    attrs = {
        "deps": attr.label_list(providers = [J2wasmInfo]),
        "_j2cl_transpiler": attr.label(
            default = Label(
                "//build_defs/internal_do_not_use:BazelJ2clBuilder",
            ),
            cfg = "host",
            executable = True,
        ),
    },
    outputs = {
        # TODO(dramaix): Modify transpiler to accept something else than a zip output
        "zip": "%{name}.wasm.zip",
    },
)
