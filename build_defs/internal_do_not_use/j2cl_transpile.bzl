"""This module contains j2cl_transpile helper function."""

def j2cl_transpile(ctx, java_provider, js_srcs):
    """ Takes Java provider and translates it into Closure style JS in a zip bundle."""

    # Using source_jars of the java_library since that includes APT generated src.
    srcs = java_provider.source_jars + js_srcs
    classpath = java_provider.compilation_info.compilation_classpath

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add_joined("-classpath", classpath, join_with = ctx.configuration.host_path_separator)
    args.add("-output", ctx.outputs.zip_file)
    if ctx.attr.declare_legacy_namespace:
        args.add("-declarelegacynamespaces")
    if ctx.attr.readable_source_maps:
        args.add("-readablesourcemaps")
    if ctx.var.get("GROK_ELLIPSIS_BUILD", None):
        args.add("-generatekytheindexingmetadata")
    args.add_all(srcs)

    ctx.actions.run(
        progress_message = "Transpiling to JavaScript %s" % ctx.label,
        inputs = depset(srcs, transitive = [classpath]),
        outputs = [ctx.outputs.zip_file],
        executable = ctx.executable.transpiler,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2clTranspile",
    )

    return ctx.outputs.zip_file

# Private Args:
#   transpiler: J2CL compiler jar to use.
J2CL_TRANSPILE_ATTRS = {
    "readable_source_maps": attr.bool(default = False),
    "declare_legacy_namespace": attr.bool(default = False),
    "transpiler": attr.label(
        cfg = "host",
        executable = True,
        allow_files = True,
        default = Label("//build_defs/internal_do_not_use:BazelJ2clBuilder"),
    ),
}
