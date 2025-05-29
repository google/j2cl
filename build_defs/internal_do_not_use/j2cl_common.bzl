"""Common utilities for creating J2CL targets and providers."""

load("@rules_java//java:defs.bzl", "java_common")

load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")
load(":j2cl_js_common.bzl", "J2CL_JS_TOOLCHAIN_ATTRS", "j2cl_js_provider")
load(":provider.bzl", "J2clInfo")

def _get_jsinfo_provider(j2cl_info):
    return j2cl_info._private_.js_info

def _compile(
        ctx,
        srcs = [],
        kt_common_srcs = [],
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        backend = "CLOSURE",
        output_jar = None,
        javac_opts = [],
        kotlincopts = [],
        internal_transpiler_flags = {},
        artifact_suffix = "",
        is_j2kt_web_experiment_enabled = False):
    name = ctx.label.name + artifact_suffix
    java_toolchain = _get_java_toolchain(ctx)
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
            java_toolchain,
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
            java_toolchain,
            jvm_srcs,
            kt_common_srcs,
            jvm_deps,
            jvm_exports,
            plugins,
            exported_plugins,
            output_jar,
            javac_opts,
            kotlincopts = kotlincopts,
            is_j2kt_web_experiment_enabled = is_j2kt_web_experiment_enabled,
        )

    if has_srcs_to_transpile:
        output_js = ctx.actions.declare_directory("%s.js" % name)
        output_library_info = ctx.actions.declare_file("%s_library_info" % name)
        _j2cl_transpile(
            ctx,
            jvm_provider,
            jvm_deps,
            get_jdk_system(java_toolchain, javac_opts),
            js_srcs,
            output_js,
            output_library_info,
            backend,
            internal_transpiler_flags,
            kt_common_srcs,
            javac_opts,
            # Forcefully enable IR serialization. J2CL only needs this to have
            # Kotlinc deserialize IR from dependencies; we do not actually emit
            # any serialized IR (that all happens on the JVM side).
            kotlincopts + KOTLIN_SERIALIZE_IR_FLAGS,
        )
        library_info = [output_library_info]
    else:
        output_js = None
        library_info = []

    # Don't pass anything to the js provider if we didn't transpile anything.
    # This case happens when j2cl_library exports another j2cl_library.
    js_provider_srcs = [output_js] if has_srcs_to_transpile else []

    # TODO(b/284654149): Use the same provider for Closure and Wasm once the modular pipeline
    # graduates from being a prototype.
    if backend == "CLOSURE":
        js_info = j2cl_js_provider(
            ctx,
            js_provider_srcs,
            js_deps,
            js_exports,
            artifact_suffix,
        )
    else:
        # The reason to have special case here and create a different provider for Wasm is to avoid
        # running the modular transpilation action when building the monolithic j2wasm_application.
        # This provider avoid triggering the transpiler by avoiding using js_provider_sources which
        # are part of the output of the tranpilation. Instead this js provider will use the
        # js that are inputs to the rule.
        js_info = j2cl_js_provider(
            ctx = ctx,
            srcs = js_srcs,
            # These are exports, because they will need to be referenced by the j2wasm_application
            # eventually downstream, since the j2wasm application is built using the transitive
            # sources and will need the transitive dependencies exposed.
            exports = js_deps + js_exports,
            artifact_suffix = artifact_suffix,
        )

    return J2clInfo(
        _private_ = struct(
            java_info = jvm_provider,
            library_info = library_info,
            output_js = output_js,
            js_info = js_info,
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
        java_toolchain,
        srcs = [],
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        output_jar = None,
        javac_opts = [],
        mnemonic = "J2cl",
        strip_annotations = ["GwtIncompatible"]):
    output_jar = output_jar or ctx.actions.declare_file("lib%s.jar" % name)
    stripped_java_srcs = [_strip_incompatible_annotation(ctx, name, srcs, mnemonic, strip_annotations)] if srcs else []
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
            java_toolchain = java_toolchain,
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
        java_toolchain = java_toolchain,
        javac_opts = javac_opts,
    )

def _kt_compile(
        ctx,
        name,
        java_toolchain,
        srcs = [],
        common_srcs = [],
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        output_jar = None,
        javac_opts = [],
        kotlincopts = [],
        is_j2kt_web_experiment_enabled = False):
    fail("Kotlin frontend is disabled")

def get_bootclasspath(ctx):
    """Returns a depset containing the Java bootclasspath entries."""

    return _get_java_toolchain(ctx).bootclasspath

def get_bootclasspath_deps(ctx):
    """Returns a depset containing deps needed by the Java bootclasspath."""

    return _get_java_toolchain(ctx)._bootclasspath_info._auxiliary

def _get_java_toolchain(ctx):
    return ctx.attr._j2cl_java_toolchain[java_common.JavaToolchainInfo]

def get_jdk_system(java_toolchain, javac_opts):
    """ Returns the path to the system module directory.

    The path is returned in a single-element list or empty list if compiling the JRE itself.
    """

    # Heuristic to determine if we need to specify the jre module. Our jre defines --system=none.
    jdk_system_already_set = any([s.startswith("--system") for s in javac_opts])

    # TODO(b/197211878): Switch to a public API when available.
    return java_toolchain._bootclasspath_info._system_inputs.to_list() if not jdk_system_already_set else []

def _strip_incompatible_annotation(ctx, name, java_srcs, mnemonic, strip_annotations):
    # Paths are matched by Kythe to identify generated J2CL sources.
    output_file = ctx.actions.declare_file(name + "_j2cl_stripped-src.jar")

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add("-d", output_file)
    args.add_all(strip_annotations, format_each = "-annotation=%s")
    args.add_all(java_srcs)

    formatted_annotations = ", ".join(["@" + annotation for annotation in strip_annotations])
    ctx.actions.run(
        progress_message = "Stripping %s from %s" % (formatted_annotations, name),
        inputs = java_srcs,
        outputs = [output_file],
        executable = ctx.executable._j2cl_stripper,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {
            "supports-multiplex-workers": "1",
            "supports-multiplex-sandboxing": "1",
        },
        mnemonic = mnemonic + "Strip",
    )

    return output_file

def _j2cl_transpile(
        ctx,
        jvm_provider,
        jvm_deps,
        jdk_system,
        js_srcs,
        output_dir,
        library_info_output,
        backend,
        internal_transpiler_flags,
        kt_common_srcs,
        javac_opts,
        kotlincopts):
    """ Takes Java provider and translates it into Closure style JS in a zip bundle."""

    # Using source_jars from the jvm compilation since that includes APT generated src.
    # In the Kotlin case, source_jars also include the common sources.
    srcs = jvm_provider.source_jars + js_srcs

    bootclasspath = get_bootclasspath(ctx)
    direct_deps = depset(transitive = [bootclasspath] + [d.compile_jars for d in jvm_deps])

    if jvm_provider.compilation_info:
        compilation_classpath = [jvm_provider.compilation_info.compilation_classpath]
    else:
        # TODO(b/214609427): JavaInfo created through Starlark does not have compilation_info set.
        # We will compute the classpath manually using transitive_compile_time_jars.
        compilation_classpath = [d.transitive_compile_time_jars for d in jvm_deps]

    bootclasspath_deps = get_bootclasspath_deps(ctx)
    classpath = depset(transitive = [bootclasspath, bootclasspath_deps] + compilation_classpath)

    outputs = [output_dir, library_info_output]

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add_joined("-classpath", classpath, join_with = ctx.configuration.host_path_separator)
    args.add_joined("-directdeps", direct_deps, join_with = ctx.configuration.host_path_separator)
    args.add_all("-system", jdk_system, expand_directories = False)

    # TODO(b/416084067): Support Javac options with an allowlist.
    # Forward necessary options to invoke javac in the transpiler.
    for i in range(len(javac_opts)):
        # We currently only support separated ["-opt", "val"]. We do not support ["-opt=val"] or
        # ["-opt val"].
        if javac_opts[i] == "--patch-module":
            args.add("-javacOptions", javac_opts[i])
            args.add("-javacOptions", javac_opts[i + 1])

    # Explicitly format this as Bazel target labels can start with a @, which
    # can be misinterpreted as a flag file to load.
    args.add(ctx.label, format = "-targetLabel=%s")
    args.add("-output", output_dir.path)
    args.add("-libraryinfooutput", library_info_output)
    args.add("-experimentalJavaFrontend", ctx.attr._java_frontend[BuildSettingInfo].value)
    args.add("-experimentalBackend", backend)

    if ctx.attr._profiling_filter[BuildSettingInfo].value in str(ctx.label):
        profile_output = ctx.actions.declare_file(ctx.label.name + ".profile")
        outputs.append(profile_output)
        args.add("-profileOutput", profile_output)

    if backend == "WASM_MODULAR":
        # Add a prefix to where the Java source files will be located relative to the source map.
        args.add(output_dir.short_path, format = "-sourceMappingPathPrefix=%s/")

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
    args.add("-forbiddenAnnotation", "GwtIncompatible")
    args.add_all(srcs)

    output_type = "JavaScript" if backend == "CLOSURE" else "Wasm (Modular)"
    ctx.actions.run(
        progress_message = "Transpiling to %s %s" % (output_type, ctx.label),
        # kt_common_srcs are not read by the transpiler as they are already
        # included in the srcjars of srcs. However, params.add_all requires them
        # to be inputs in order to be properly expanded out into params.
        inputs = depset(srcs + kt_common_srcs + jdk_system, transitive = [classpath]),
        outputs = outputs,
        executable = ctx.executable._j2cl_transpiler,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {
            "supports-multiplex-workers": "1",
            "supports-multiplex-sandboxing": "1",
            "supports-worker-cancellation": "1",
        },
        mnemonic = "J2cl" if backend == "CLOSURE" else "J2wasm",
    )

def _create_js_lib_struct(j2cl_info, extra_providers = []):
    return [j2cl_info, j2cl_info._private_.js_info] + extra_providers

DEFAULT_J2CL_JAVAC_OPTS = [
    # Avoid log site injection which introduces calls to unsupported APIs.
    "-XDinjectLogSites=false",
    # Avoid optimized JVM String concat which introduces calls to unsupported APIs.
    "-XDstringConcat=inline",
    # Explicitly limit to Java 11 inputs as that is the max currently supported.
    # TODO(b/286447025): Bump to Java 21.
    "-source 11",
    "-target 11",
]

DEFAULT_J2CL_KOTLINCOPTS = [
    # KMP should be enabled to allow for passing common sources and using
    # expect/actual syntax.
    "-Xmulti-platform",
    # Have kotlinc's IR lowering passes generate objects for SAM implementations.
    # J2CL lowering passes cannot handle invokedynamic-based representations.
    "-Xsam-conversions=class",
    # TODO(b/347052390): Remove once the const evaluation optimization crash is fixed (KT-70391).
    "-Xignore-const-optimization-errors",
]

KOTLIN_SERIALIZE_IR_FLAGS = [
    # Enable the serialization of the IR
    # Currently all IR elements are being serialized to workaround missing IR in
    # some instances, ex. a lambda within an inline member. See: b/263391416
    # TODO(b/264661698): Reduce to just serialization of inline functions.
    "-Xserialize-ir=all",
]

J2CL_JAVA_TOOLCHAIN_ATTRS = {
    "_j2cl_java_toolchain": attr.label(
        default = Label("//jre/java:j2cl_java_toolchain"),
    ),
    "_j2cl_stripper": attr.label(
        default = Label("//tools/java/com/google/j2cl/tools/gwtincompatible:GwtIncompatibleStripper_worker"),
        cfg = "exec",
        executable = True,
    ),
}

J2CL_TOOLCHAIN_ATTRS = {
    "_j2cl_transpiler": attr.label(
        default = Label("//transpiler/java/com/google/j2cl/transpiler:BazelJ2clBuilder"),
        cfg = "exec",
        executable = True,
    ),
    "_java_frontend": attr.label(
        default = Label("//:experimental_java_frontend"),
    ),
    "_profiling_filter": attr.label(
        default = Label("//:profiling_filter"),
    ),
    "_zip": attr.label(
        executable = True,
        cfg = "exec",
        default = Label("@bazel_tools//tools/zip:zipper"),
    ),
}
J2CL_TOOLCHAIN_ATTRS.update(J2CL_JAVA_TOOLCHAIN_ATTRS)

J2CL_TOOLCHAIN_ATTRS.update(J2CL_JS_TOOLCHAIN_ATTRS)

j2cl_common = struct(
    compile = _compile,
    create_js_lib_struct = _create_js_lib_struct,
    get_jsinfo_provider = _get_jsinfo_provider,
    java_compile = _java_compile,
)
