"""Common utilities for creating J2CL targets and providers."""

load(":j2cl_js_common.bzl", "J2CL_JS_TOOLCHAIN_ATTRS", "create_js_lib_struct", "j2cl_js_provider")
load("//build_defs/internal_do_not_use:provider.bzl", _J2clInfo = "J2clInfo")
load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

# TODO(b/183965899): Update references to skip this re-export and remove it.
J2clInfo = _J2clInfo

def _get_jsinfo_provider(j2cl_info):
    return j2cl_info._private_.js_info

def _compile(
        ctx,
        srcs = [],
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        output_jar = None,
        javac_opts = [],
        kotlincopts = [],
        internal_transpiler_flags = {},
        generate_kythe_action = False,
        artifact_suffix = ""):
    name = ctx.label.name + artifact_suffix

    # Categorize the sources.
    js_srcs = []
    jvm_srcs = []
    for src in srcs:
        (js_srcs if src.extension in ["js", "zip"] else jvm_srcs).append(src)

    has_kotlin_srcs = any([src for src in jvm_srcs if src.extension == "kt"])

    # Validate the attributes.
    if not jvm_srcs:
        if deps:
            fail("deps not allowed without java or kotlin srcs")
        if js_srcs:
            fail("js sources not allowed without java or kotlin srcs")
    if not has_kotlin_srcs:
        if kotlincopts:
            fail("kotlincopts not allowed without kotlin sources")

    jvm_deps, js_deps = _split_deps(deps)
    jvm_exports, js_exports = _split_deps(exports)

    kotlincopts = DEFAULT_J2CL_KOTLINCOPTS + kotlincopts

    if not has_kotlin_srcs:
        # Avoid Kotlin toolchain for regular targets.
        jvm_provider = _java_compile(
            ctx,
            name,
            jvm_srcs,
            jvm_deps,
            jvm_exports,
            plugins,
            exported_plugins,
            output_jar,
            javac_opts,
            generate_kythe_action = generate_kythe_action,
        )
    else:
        jvm_provider = _kt_compile(
            ctx,
            name,
            jvm_srcs,
            jvm_deps,
            jvm_exports,
            plugins,
            exported_plugins,
            output_jar,
            javac_opts,
            kotlincopts = kotlincopts,
        )

    if jvm_srcs:
        output_js = ctx.actions.declare_directory("%s.js" % name)
        output_library_info = ctx.actions.declare_file("%s_library_info" % name)
        _j2cl_transpile(
            ctx,
            jvm_provider,
            js_srcs,
            output_js,
            output_library_info,
            internal_transpiler_flags,
            kotlincopts,
        )
        library_info = [output_library_info]
    else:
        output_js = None
        library_info = []

    # Don't pass anything to the js provider if we didn't transpile anything.
    # This case happens when j2cl_library exports another j2cl_library.
    js_provider_srcs = [output_js] if jvm_srcs else []

    return J2clInfo(
        _private_ = struct(
            java_info = jvm_provider,
            library_info = library_info,
            output_js = output_js,
            js_info = j2cl_js_provider(
                ctx,
                js_provider_srcs,
                js_deps,
                js_exports,
                artifact_suffix,
            ),
        ),
        _is_j2cl_provider = 1,
    )

def _split_deps(deps):
    """ Split the provider deps into Jvm and JS groups. """
    jvm_deps = []
    js_deps = []
    for d in deps:
        # There is no good way to test if a provider is of a particular type so here we are
        # checking existence of a property that is expected to be inside the provider.
        if hasattr(d, "_is_j2cl_provider"):
            # This is a j2cl provider.
            jvm_deps.append(d._private_.java_info)
            js_deps.append(d._private_.js_info)
        else:
            # This is a js provider
            js_deps.append(d)

    return (jvm_deps, js_deps)

def _java_compile(
        ctx,
        name,
        srcs = [],
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        output_jar = None,
        javac_opts = [],
        generate_kythe_action = False,
        mnemonic = "J2cl"):
    output_jar = output_jar or ctx.actions.declare_file("lib%s.jar" % name)
    stripped_java_srcs = [_strip_gwt_incompatible(ctx, name, srcs, mnemonic)] if srcs else []
    javac_opts = DEFAULT_J2CL_JAVAC_OPTS + javac_opts

    if generate_kythe_action and ctx.var.get("GROK_ELLIPSIS_BUILD", None):
        # An unused JAR that is only generated so that we run javac with the non-stripped sources
        # that kythe can index. Nothing should depend upon this output as it is not guaranteed
        # to succeed; it is only best effort for indexing.
        indexed_output_jar = ctx.actions.declare_file(name + "_j2cl_indexable.jar")
        java_common.compile(
            ctx,
            source_files = srcs,
            deps = deps,
            exports = exports,
            plugins = plugins,
            exported_plugins = exported_plugins,
            output = indexed_output_jar,
            java_toolchain = _get_java_toolchain(ctx),
            javac_opts = javac_opts,
        )

    return java_common.compile(
        ctx,
        source_jars = stripped_java_srcs,
        deps = deps,
        exports = exports,
        plugins = plugins,
        exported_plugins = exported_plugins,
        output = output_jar,
        java_toolchain = _get_java_toolchain(ctx),
        javac_opts = javac_opts,
    )

def _kt_compile(
        ctx,
        name,
        srcs = [],
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        output_jar = None,
        javac_opts = [],
        kotlincopts = []):
    fail("Kotlin frontend is disabled")

def _get_java_toolchain(ctx):
    return ctx.attr._java_toolchain[java_common.JavaToolchainInfo]

def _strip_gwt_incompatible(ctx, name, java_srcs, mnemonic):
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
        mnemonic = mnemonic,
    )

    return output_file

def _j2cl_transpile(
        ctx,
        jvm_provider,
        js_srcs,
        output_dir,
        library_info_output,
        internal_transpiler_flags,
        kotlincopts):
    """ Takes Java provider and translates it into Closure style JS in a zip bundle."""

    # Using source_jars of the java_library since that includes APT generated src.
    srcs = jvm_provider.source_jars + js_srcs

    if jvm_provider.compilation_info:
        classpath = depset(
            jvm_provider.compilation_info.boot_classpath,
            transitive = [jvm_provider.compilation_info.compilation_classpath],
        )
    else:
        # TODO(b/214609427): JavaInfo created through Starlark does not have compilation_info set.
        # We will compute the classpath manually using transitive_compile_time_jars (note that
        # transitive_compile_time_jars contains current compiled code which should be excluded.)
        compiled_jars = {jar: True for jar in jvm_provider.compile_jars.to_list()}
        compilation_classpath = [
            jar
            for jar in jvm_provider.transitive_compile_time_jars.to_list()
            if jar not in compiled_jars
        ]
        classpath = depset(
            _get_java_toolchain(ctx).bootclasspath.to_list(),
            transitive = [depset(compilation_classpath)],
        )

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add_joined("-classpath", classpath, join_with = ctx.configuration.host_path_separator)
    args.add("-output", output_dir.path)
    args.add("-libraryinfooutput", library_info_output)
    args.add("-experimentalJavaFrontend", ctx.attr._java_frontend[BuildSettingInfo].value)
    for flag, value in internal_transpiler_flags.items():
        if value:
            args.add("-" + flag.replace("_", ""))
    if ctx.var.get("GROK_ELLIPSIS_BUILD", None) or (
        # Support Kythe integration testing that required metadata as part
        # of regular test run that can't use GROK_ELLIPSIS_BUILD.
        hasattr(ctx.attr, "tags") and "generate_kythe_metadata" in ctx.attr.tags
    ):
        args.add("-generatekytheindexingmetadata")
    args.add_all(kotlincopts, format_each = "-kotlincOptions=%s")
    args.add_all(srcs)

    #  TODO(b/217287994): Remove the ability to do transpiler override.
    j2cl_transpiler_override = None
    if hasattr(ctx.executable, "j2cl_transpiler_override"):
        j2cl_transpiler_override = ctx.executable.j2cl_transpiler_override

    ctx.actions.run(
        progress_message = "Transpiling to JavaScript %s" % ctx.label,
        inputs = depset(srcs, transitive = [classpath]),
        outputs = [output_dir, library_info_output],
        executable = j2cl_transpiler_override or ctx.executable._j2cl_transpiler,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2cl",
    )

DEFAULT_J2CL_JAVAC_OPTS = [
    # Avoid log site injection which introduces calls to unsupported APIs.
    "-XDinjectLogSites=false",
    # Avoid optimized JVM String concat which introduces calls to unsupported APIs.
    "-XDstringConcat=inline",
]

DEFAULT_J2CL_KOTLINCOPTS = [
    # KMP should be enabled to allow for passing common sources and using
    # expect/actual syntax.
    "-Xmulti-platform",
]

J2CL_JAVA_TOOLCHAIN_ATTRS = {
    "_java_toolchain": attr.label(
        default = Label("//build_defs/internal_do_not_use:j2cl_java_toolchain"),
    ),
    "_j2cl_stripper": attr.label(
        default = Label("//build_defs/internal_do_not_use:GwtIncompatibleStripper"),
        cfg = "host",
        executable = True,
    ),
}

J2CL_TOOLCHAIN_ATTRS = {
    "_j2cl_transpiler": attr.label(
        default = Label("//build_defs/internal_do_not_use:BazelJ2clBuilder"),
        cfg = "host",
        executable = True,
    ),
    "_jar": attr.label(
        cfg = "host",
        executable = True,
        default = Label("@bazel_tools//tools/jdk:jar"),
    ),
    "_java_frontend": attr.label(
        default = Label("//:experimental_java_frontend"),
    ),
}
J2CL_TOOLCHAIN_ATTRS.update(J2CL_JAVA_TOOLCHAIN_ATTRS)

J2CL_TOOLCHAIN_ATTRS.update(J2CL_JS_TOOLCHAIN_ATTRS)

j2cl_common = struct(
    compile = _compile,
    create_js_lib_struct = create_js_lib_struct,
    get_jsinfo_provider = _get_jsinfo_provider,
    java_compile = _java_compile,
)
