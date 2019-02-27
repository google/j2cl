"""Common utilities for creating J2CL targets and providers."""

load(":j2cl_js_common.bzl", "J2CL_JS_ATTRS", "J2CL_JS_TOOLCHAIN_ATTRS", "JS_PROVIDER_NAME", "j2cl_js_provider")

# Constructor for the Bazel provider for J2CL.
# Note that data under "_private_" considered private internal data so do not use.
J2clInfo = provider(fields = ["_private_"])

def _impl_j2cl_library(ctx):
    # Categorize the sources.
    js_srcs = []
    java_srcs = []
    for src in ctx.files.srcs:
        (js_srcs if src.extension in ["js", "zip"] else java_srcs).append(src)

    # Validate the attributes.
    if not java_srcs:
        if ctx.files.deps:
            fail("deps not allowed without java srcs")
        if js_srcs:
            fail("js sources not allowed without java srcs")

    java_provider = _java_compile(ctx, java_srcs)

    if java_srcs:
        output_js_zip = ctx.outputs.jszip
        output_library_info = ctx.actions.declare_file("%s_library_info" % ctx.attr.name)
        _j2cl_transpile(ctx, java_provider, js_srcs, output_js_zip, output_library_info)
        js_outputs = [output_js_zip]
        library_info = [output_library_info]
    else:
        # Make sure js zip is always created since it is a named output
        _create_empty_zip(ctx, ctx.outputs.jszip)
        js_outputs = []
        library_info = []

    # This is a workaround to b/35847804 to make sure the zip ends up in the runfiles.
    js_runfiles = _collect_runfiles(ctx, js_outputs, ctx.attr.deps + ctx.attr.exports)

    return struct(
        providers = [
            DefaultInfo(
                files = depset([ctx.outputs.jszip, ctx.outputs.jar]),
                runfiles = js_runfiles,
            ),
            J2clInfo(_private_ = struct(JavaInfo = java_provider, LibraryInfo = library_info)),
        ],
        **j2cl_js_provider(ctx, srcs = js_outputs, deps = ctx.attr.deps, exports = ctx.attr.exports)
    )

_empty_zip_contents = "\\x50\\x4b\\x05\\x06\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00"

def _create_empty_zip(ctx, output_js_zip):
    ctx.actions.run_shell(
        outputs = [output_js_zip],
        command = "echo -ne  '%s' > '%s'" % (_empty_zip_contents, output_js_zip.path),
    )

def _collect_runfiles(ctx, files, deps):
    transitive_runfiles = [d[DefaultInfo].default_runfiles.files for d in deps]
    return ctx.runfiles(
        files = files,
        transitive_files = depset(transitive = transitive_runfiles),
    )

def _java_compile(ctx, java_srcs):
    stripped_java_srcs = [_strip_gwt_incompatible(ctx, java_srcs)] if java_srcs else []
    java_deps = [d[J2clInfo]._private_.JavaInfo for d in ctx.attr.deps if J2clInfo in d]
    java_exports = [d[J2clInfo]._private_.JavaInfo for d in ctx.attr.exports if J2clInfo in d]
    plugins = [p[JavaInfo] for p in ctx.attr.plugins]
    exported_plugins = [p[JavaInfo] for p in ctx.attr.exported_plugins]

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
        source_files = ctx.files._srcs_hack,
        source_jars = stripped_java_srcs,
        deps = java_deps,
        exports = java_exports,
        plugins = plugins,
        exported_plugins = exported_plugins,
        output = ctx.outputs.jar,
        java_toolchain = java_toolchain,
        host_javabase = host_javabase,
        javac_opts = default_j2cl_javac_opts + ctx.attr.javacopts,
    )

def _strip_gwt_incompatible(ctx, java_srcs):
    output_file = ctx.actions.declare_file(ctx.label.name + "_stripped-src.jar")

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add("-d", output_file)
    args.add_all(java_srcs)

    ctx.actions.run(
        progress_message = "Stripping @GwtIncompatible from %s" % ctx.label.name,
        inputs = java_srcs,
        outputs = [output_file],
        executable = ctx.executable._j2cl_stripper,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2cl",
    )

    return output_file

def _j2cl_transpile(ctx, java_provider, js_srcs, output, library_info_output):
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
    args.add("-output", output)
    args.add("-libraryinfooutput", library_info_output)
    if ctx.attr.declare_legacy_namespace:
        args.add("-declarelegacynamespaces")
    if ctx.attr.readable_source_maps:
        args.add("-readablesourcemaps")
    if ctx.attr.readable_library_info:
        args.add("-readablelibraryinfo")
    if ctx.var.get("GROK_ELLIPSIS_BUILD", None):
        args.add("-generatekytheindexingmetadata")
    args.add_all(srcs)

    ctx.actions.run(
        progress_message = "Transpiling to JavaScript %s" % ctx.label,
        inputs = depset(srcs, transitive = [classpath]),
        outputs = [output, library_info_output],
        executable = ctx.executable._j2cl_transpiler,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2cl",
    )

_J2CL_TOOLCHAIN_ATTRS = {
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
        default = Label("//build_defs/internal_do_not_use:GwtIncompatibleStripper", relative_to_caller_repository = False),
        cfg = "host",
        executable = True,
    ),
    # TODO(goktug): remove workaround after b/71772385 is fixed
    "_srcs_hack": attr.label(default = Label("//build_defs/internal_do_not_use:dummy_src")),
}
_J2CL_TOOLCHAIN_ATTRS.update(J2CL_JS_TOOLCHAIN_ATTRS)

_J2CL_DEPRECATED_LIB_ATTRS = {
    "readable_source_maps": attr.bool(default = False),
    "readable_library_info": attr.bool(default = False),
    "declare_legacy_namespace": attr.bool(default = False),
}

_J2CL_LIB_ATTRS = {
    # TODO(goktug): Try to limit this further.
    "srcs": attr.label_list(allow_files = [".java", ".js", ".srcjar", ".jar", ".zip"]),
    "deps": attr.label_list(providers = [JS_PROVIDER_NAME]),
    "exports": attr.label_list(providers = [JS_PROVIDER_NAME]),
    "plugins": attr.label_list(providers = [JavaInfo]),
    "exported_plugins": attr.label_list(providers = [JavaInfo]),
    "javacopts": attr.string_list(),
    "licenses": attr.license(),
}
_J2CL_LIB_ATTRS.update(_J2CL_DEPRECATED_LIB_ATTRS)
_J2CL_LIB_ATTRS.update(_J2CL_TOOLCHAIN_ATTRS)
_J2CL_LIB_ATTRS.update(J2CL_JS_ATTRS)

j2cl_library = rule(
    implementation = _impl_j2cl_library,
    attrs = _J2CL_LIB_ATTRS,
    fragments = ["java", "js"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
        "jszip": "%{name}.js.zip",
    },
)

def _impl_java_import(ctx):
    return struct(
        providers = [J2clInfo(_private_ = struct(JavaInfo = ctx.attr.jar[JavaInfo], LibraryInfo = []))],
        **j2cl_js_provider(ctx)
    )

_J2CL_IMPORT_ATTRS = {
    "jar": attr.label(providers = [JavaInfo]),
    "licenses": attr.license(),
}
_J2CL_IMPORT_ATTRS.update(J2CL_JS_TOOLCHAIN_ATTRS)
_J2CL_IMPORT_ATTRS.update(J2CL_JS_ATTRS)

# helper rule to convert a Java target to a J2CL target.
j2cl_java_import = rule(
    implementation = _impl_java_import,
    attrs = _J2CL_IMPORT_ATTRS,
    fragments = ["java", "js"],
)
