"""Common utilities for creating J2CL targets and providers."""

load(":provider.bzl", "J2clInfo")
load(":j2cl_js_common.bzl", "J2CL_JS_TOOLCHAIN_ATTRS", "create_js_lib_struct", "j2cl_js_provider")
load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

def _get_jsinfo_provider(j2cl_info):
    return j2cl_info._private_.js_info

def _compile(
        ctx,
        srcs = [],
        kt_common_srcs = [],
        kt_friend_jars = depset(),
        kt_exported_friend_jars = depset(),
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        backend = "CLOSURE",
        output_jar = None,
        javac_opts = [],
        kotlincopts = [],
        internal_transpiler_flags = {},
        artifact_suffix = ""):
    name = ctx.label.name + artifact_suffix

    jvm_srcs, js_srcs = split_srcs(srcs)

    has_srcs_to_transpile = (jvm_srcs or kt_common_srcs)
    has_kotlin_srcs = any([src for src in jvm_srcs if src.extension == "kt"]) or kt_common_srcs

    # Validate the attributes.
    if not has_srcs_to_transpile:
        if deps:
            fail("deps not allowed without java or kotlin srcs")
        if js_srcs:
            fail("js sources not allowed without java or kotlin srcs")
    if not has_kotlin_srcs:
        if kotlincopts:
            fail("kotlincopts not allowed without kotlin sources")

    jvm_deps, js_deps = split_deps(deps)
    jvm_exports, js_exports = split_deps(exports)

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
        )
    else:
        jvm_provider = _kt_compile(
            ctx,
            name,
            jvm_srcs,
            kt_common_srcs,
            jvm_deps,
            jvm_exports,
            plugins,
            exported_plugins,
            output_jar,
            javac_opts,
            kotlincopts = kotlincopts,
            friend_jars = kt_friend_jars,
        )

    if has_srcs_to_transpile:
        output_js = ctx.actions.declare_directory("%s.js" % name)
        output_library_info = ctx.actions.declare_file("%s_library_info" % name)
        _j2cl_transpile(
            ctx,
            jvm_provider,
            js_srcs,
            output_js,
            output_library_info,
            backend,
            internal_transpiler_flags,
            kt_common_srcs,
            kotlincopts,
            kt_friend_jars,
        )
        library_info = [output_library_info]
    else:
        output_js = None
        library_info = []

    # Don't pass anything to the js provider if we didn't transpile anything.
    # This case happens when j2cl_library exports another j2cl_library.
    js_provider_srcs = [output_js] if has_srcs_to_transpile else []

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
            kt_exported_friend_jars = kt_exported_friend_jars,
        ),
        _is_j2cl_provider = 1,
    )

def split_srcs(srcs):
    """ Split the srcs into Jvm and JS groups. """
    jvm_srcs = []
    js_srcs = []
    for src in srcs:
        (js_srcs if src.extension in ["js", "zip"] else jvm_srcs).append(src)
    return (jvm_srcs, js_srcs)

def split_deps(deps):
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
        mnemonic = "J2cl",
        strip_annotation = "GwtIncompatible"):
    output_jar = output_jar or ctx.actions.declare_file("lib%s.jar" % name)
    stripped_java_srcs = [_strip_incompatible_annotation(ctx, name, srcs, mnemonic, strip_annotation)] if srcs else []
    javac_opts = DEFAULT_J2CL_JAVAC_OPTS + javac_opts

    if ctx.var.get("GROK_ELLIPSIS_BUILD", None):
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
        common_srcs = [],
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        output_jar = None,
        javac_opts = [],
        kotlincopts = [],
        friend_jars = depset()):
    fail("Kotlin frontend is disabled")

def _get_java_toolchain(ctx):
    return ctx.attr._java_toolchain[java_common.JavaToolchainInfo]

def _strip_incompatible_annotation(ctx, name, java_srcs, mnemonic, strip_annotation):
    # Paths are matched by Kythe to identify generated J2CL sources.
    output_file = ctx.actions.declare_file(name + "_j2cl_stripped-src.jar")

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add("-d", output_file)
    args.add("-annotation", strip_annotation)
    args.add_all(java_srcs)

    ctx.actions.run(
        progress_message = "Stripping @%s from %s" % (strip_annotation, name),
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
        backend,
        internal_transpiler_flags,
        kt_common_srcs,
        kotlincopts,
        kt_friend_jars):
    """ Takes Java provider and translates it into Closure style JS in a zip bundle."""

    # Using source_jars from the jvm compilation since that includes APT generated src.
    # In the Kotlin case, source_jars also include the common sources.
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
        compiled_jars = [output.compile_jar for output in jvm_provider.java_outputs if output.compile_jar]
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
    args.add("-experimentalBackend", backend)
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
    args.add_joined(kt_friend_jars, format_joined = "-kotlincOptions=-Xfriend-paths=%s", join_with = ",")
    args.add_all(srcs)

    #  TODO(b/217287994): Remove the ability to do transpiler override.
    j2cl_transpiler_override = None
    if hasattr(ctx.executable, "j2cl_transpiler_override"):
        j2cl_transpiler_override = ctx.executable.j2cl_transpiler_override

    output_type = "JavaScript" if backend == "CLOSURE" else "Wasm (Modular)"
    ctx.actions.run(
        progress_message = "Transpiling to %s %s" % (output_type, ctx.label),
        # kt_common_srcs are not read by the transpiler as they are already
        # included in the srcjars of srcs. However, params.add_all requires them
        # to be inputs in order to be properly expanded out into params.
        inputs = depset(srcs + kt_common_srcs, transitive = [classpath, kt_friend_jars]),
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
    # Explicitly enable Java 11. Neeeded for open-source
    "-source 11",
    "-target 11",
]

DEFAULT_J2CL_KOTLINCOPTS = [
    # KMP should be enabled to allow for passing common sources and using
    # expect/actual syntax.
    "-Xmulti-platform",
    # Enable the serialization of the IR
    # Currently all IR elements are being serialized to workaround missing IR in
    # some instances, ex. a lambda within an inline member. See: b/263391416
    # TODO(b/264661698): Reduce to just serialization of inline functions.
    "-Xserialize-ir=all",
]

J2CL_JAVA_TOOLCHAIN_ATTRS = {
    "_java_toolchain": attr.label(
        default = Label("//build_defs/internal_do_not_use:j2cl_java_toolchain"),
    ),
    "_j2cl_stripper": attr.label(
        default = Label("//build_defs/internal_do_not_use:GwtIncompatibleStripper"),
        cfg = "exec",
        executable = True,
    ),
}

J2CL_TOOLCHAIN_ATTRS = {
    "_j2cl_transpiler": attr.label(
        default = Label("//build_defs/internal_do_not_use:BazelJ2clBuilder"),
        cfg = "exec",
        executable = True,
    ),
    "_jar": attr.label(
        cfg = "exec",
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
