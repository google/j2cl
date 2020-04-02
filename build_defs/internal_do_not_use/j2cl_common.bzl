"""Common utilities for creating J2CL targets and providers."""

load(":j2cl_js_common.bzl", "J2CL_JS_TOOLCHAIN_ATTRS", "create_js_lib_struct", "j2cl_js_provider")
load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

# Constructor for the Bazel provider for J2CL.
# Note that data under "_private_" considered private internal data so do not use.
J2clInfo = provider(fields = ["_private_", "_is_j2cl_provider"])

def _get_jsinfo_provider(j2cl_info):
    return j2cl_info._private_.js_info

def _compile(
        ctx,
        name,
        srcs = [],
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        output_jszip = None,
        output_jar = None,
        javac_opts = [],
        internal_transpiler_flags = {}):
    # Categorize the sources.
    js_srcs = []
    java_srcs = []
    for src in srcs:
        (js_srcs if src.extension in ["js", "zip"] else java_srcs).append(src)

    # Validate the attributes.
    if not java_srcs:
        if deps:
            fail("deps not allowed without java srcs")
        if js_srcs:
            fail("js sources not allowed without java srcs")

    java_deps, js_deps = _split_deps(deps)
    java_exports, js_exports = _split_deps(exports)

    output_jar = output_jar or ctx.actions.declare_file("lib%s.jar" % name)
    java_provider = _java_compile(
        ctx,
        name,
        java_srcs,
        java_deps,
        java_exports,
        plugins,
        exported_plugins,
        output_jar,
        javac_opts,
    )

    output_jszip = output_jszip or ctx.actions.declare_file("%s.js.zip" % name)

    if java_srcs:
        output_library_info = ctx.actions.declare_file("%s_library_info" % name)
        _j2cl_transpile(
            ctx,
            java_provider,
            js_srcs,
            output_jszip,
            output_library_info,
            internal_transpiler_flags,
        )
        library_info = [output_library_info]
    else:
        _create_empty_zip(ctx, output_jszip)
        library_info = []

    generate_tree_artifact = ctx.attr._enable_tree_artifact[BuildSettingInfo].value
    output_js = _unzip_output(ctx, output_jszip, name) if generate_tree_artifact else output_jszip

    # Don't pass anything to the js provider if we didn't transpile anything.
    # This case happens when j2cl_library exports another j2cl_library.
    js_provider_srcs = [output_js] if java_srcs else []

    return J2clInfo(
        _private_ = struct(
            java_info = java_provider,
            library_info = library_info,
            output_js = output_js,
            js_info = j2cl_js_provider(ctx, js_provider_srcs, js_deps, js_exports),
        ),
        _is_j2cl_provider = 1,
    )

def _unzip_output(ctx, jszip, name):
    output_dir = ctx.actions.declare_directory("%s_j2cl_js" % name)

    ctx.actions.run_shell(
        progress_message = "Generating TreeArtifact",
        inputs = [jszip],
        outputs = [output_dir],
        command = (
            "mkdir -p %s && " % output_dir.path +
            "%s x %s -d %s" % (ctx.executable._zip.path, jszip.path, output_dir.path)
        ),
        tools = [ctx.executable._zip],
        mnemonic = "J2clZip",
    )

    return output_dir

def _split_deps(deps):
    """ Split the provider deps into Java and JS groups. """
    java_deps = []
    js_deps = []
    for d in deps:
        # There is no good way to test if a provider is of a particular type so here we are
        # checking existence of a property that is expected to be inside the provider.
        if hasattr(d, "_is_j2cl_provider"):
            # This is a j2cl provider.
            java_deps.append(d._private_.java_info)
            js_deps.append(d._private_.js_info)
        else:
            # This is a js provider
            js_deps.append(d)

    return (java_deps, js_deps)

_empty_zip_contents = "\\x50\\x4b\\x05\\x06" + "\\x00" * 18

def _create_empty_zip(ctx, output_js_zip):
    ctx.actions.run_shell(
        outputs = [output_js_zip],
        command = "echo -ne  '%s' > '%s'" % (_empty_zip_contents, output_js_zip.path),
        mnemonic = "J2clZip",
    )

def _java_compile(ctx, name, srcs, deps, exports, plugins, exported_plugins, output_jar, javac_opts):
    stripped_java_srcs = [_strip_gwt_incompatible(ctx, name, srcs)] if srcs else []

    default_j2cl_javac_opts = [
        # Avoid log site injection which introduces calls to unsupported APIs
        "-XDinjectLogSites=false",
    ]

    # Use find_java_toolchain / find_java_runtime_toolchain after the next Bazel release,
    # see: https://github.com/bazelbuild/bazel/issues/7186
    if hasattr(java_common, "JavaToolchainInfo"):
        java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo]
        host_javabase = ctx.attr._host_javabase[java_common.JavaRuntimeInfo]
    else:
        java_toolchain = ctx.attr._java_toolchain
        host_javabase = ctx.attr._host_javabase

    return java_common.compile(
        ctx,
        source_jars = stripped_java_srcs,
        deps = deps,
        exports = exports,
        plugins = plugins,
        exported_plugins = exported_plugins,
        output = output_jar,
        java_toolchain = java_toolchain,
        host_javabase = host_javabase,
        javac_opts = default_j2cl_javac_opts + javac_opts,
    )

def _strip_gwt_incompatible(ctx, name, java_srcs):
    # Paths are matched by Kythe to identify generated J2CL sources.
    output_file = ctx.actions.declare_file(name + "_j2cl_stripped-src.jar")

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add("-d", output_file)
    args.add_all(java_srcs)

    ctx.actions.run(
        progress_message = "Stripping @GwtIncompatible from %s" % name,
        inputs = java_srcs,
        outputs = [output_file],
        executable = ctx.executable._j2cl_stripper,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2cl",
    )

    return output_file

def _j2cl_transpile(
        ctx,
        java_provider,
        js_srcs,
        output_dir,
        library_info_output,
        internal_transpiler_flags):
    """ Takes Java provider and translates it into Closure style JS in a zip bundle."""

    # Using source_jars of the java_library since that includes APT generated src.
    srcs = java_provider.source_jars + js_srcs
    classpath = depset(
        java_provider.compilation_info.boot_classpath,
        transitive = [java_provider.compilation_info.compilation_classpath],
    )

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add_joined("-classpath", classpath, join_with = ctx.configuration.host_path_separator)
    args.add("-output", output_dir)
    args.add("-libraryinfooutput", library_info_output)
    for flag, value in internal_transpiler_flags.items():
        if value:
            args.add("-" + flag.replace("_", ""))
    if ctx.var.get("GROK_ELLIPSIS_BUILD", None):
        args.add("-generatekytheindexingmetadata")
    args.add_all(srcs)

    ctx.actions.run(
        progress_message = "Transpiling to JavaScript %s" % ctx.label,
        inputs = depset(srcs, transitive = [classpath]),
        outputs = [output_dir, library_info_output],
        executable = ctx.executable._j2cl_transpiler,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2cl",
    )

J2CL_TOOLCHAIN_ATTRS = {
    "_java_toolchain": attr.label(
        default = Label("//build_defs/internal_do_not_use:j2cl_java_toolchain"),
    ),
    "_host_javabase": attr.label(
        default = Label("@bazel_tools//tools/jdk:current_host_java_runtime"),
        cfg = "host",
    ),
    "_j2cl_transpiler": attr.label(
        default = Label("//build_defs/internal_do_not_use:BazelJ2clBuilder"),
        cfg = "host",
        executable = True,
    ),
    "_j2cl_stripper": attr.label(
        default = Label("//build_defs/internal_do_not_use:GwtIncompatibleStripper"),
        cfg = "host",
        executable = True,
    ),
    "_zip": attr.label(
        cfg = "host",
        executable = True,
        default = Label("@bazel_tools//tools/zip:zipper"),
    ),
    "_enable_tree_artifact": attr.label(
        default = Label("//:enable_experimental_tree_artifact_mode"),
    ),
}
J2CL_TOOLCHAIN_ATTRS.update(J2CL_JS_TOOLCHAIN_ATTRS)

j2cl_common = struct(
    compile = _compile,
    create_js_lib_struct = create_js_lib_struct,
    get_jsinfo_provider = _get_jsinfo_provider,
)
