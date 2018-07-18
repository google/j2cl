"""j2cl_library build macro

Takes Java source, translates it into Closure style JS and surfaces it to the
rest of the build tree with a js_common.provider. Generally library rules dep on
other library rules for reference resolution and this build macro is no
exception. In particular the deps this rule needs for reference resolution are
java_library() targets which will have been created by other invocations of
this same j2cl_library build macro.


Example use:

# Effectively creates js_library(name="Foo") containing translated JS.
j2cl_library(
    name = "Foo",
    srcs = glob(["Foo.java"]),
    deps = [":Bar"]  # Directly depends on j2cl_library(name="Bar")
)

# Effectively creates js_library(name="Bar") containing the results.
j2cl_library(
    name = "Bar",
    srcs = glob(["Bar.java"]),
)

"""

load("//build_def:j2cl_java_library.bzl", "j2cl_java_library")
load("//build_def:j2cl_transpile.bzl", "j2cl_transpile")
load("//build_def:j2cl_util.bzl", "J2CL_OPTIMIZED_DEFS")
load("//tools/build_defs/label:def.bzl", "absolute_label")
load("//tools/build_defs/j2cl:def.bzl", "js_import")
load("//tools/build_rules:build_test.bzl", "build_test")

def _do_env_copy(env_restricted_artifact, unrestricted_artifact, testonly):
    """Copies an artifact from to remove build environment restrictions."""
    native.genrule(
        name = unrestricted_artifact + "_genrule",
        tools = [env_restricted_artifact],
        outs = [unrestricted_artifact],
        testonly = testonly,
        tags = ["notap", "manual"],
        cmd = "cp $(location %s) $@" % env_restricted_artifact,
        local = True,
    )

def _get_absolute_labels(args, key):
    """Returns the absolute label for the provided key if exists, otherwise empty."""
    labels = args.get(key) or []
    if not type(labels) in ["list", "depset"]:
        fail("Expected depset or list for the attribute '%s' in j2cl_library rule" % key)

    return [absolute_label(target) for target in labels]

def j2cl_library(
        name,
        srcs = [],
        tags = [],
        native_srcs = [],
        generate_build_test = None,
        js_deps_mgmt = "closure",
        visibility = None,
        _js_srcs = [],
        _js_deps = [],
        _js_exports = [],
        _readable_source_maps = False,
        _declare_legacy_namespace = False,
        _test_externs_list = [],
        _transpiler = None,
        **kwargs):
    """Translates Java source into JS source in a js_common.provider target.

    Implicit output targets:
      lib<name>-src.jar: A java archive containing the sources (source jar).

    Args:
      srcs: Source files (.java or .srcjar) to compile.
      native_srcs: Native js source files (.native.js). Native sources should be
          put next to main java file to match.
      deps: Labels of other j2cl_library() rules.
            NOT labels of java_library() rules.
    """
    # Private Args:
    #   _js_srcs: JavaScript source files (.js) to include in the bundle.
    #   _js_deps: Direct JavaScript dependencies needed by native code (either
    #       via srcs in _js_srcs or via JsInterop/native.js).
    #       For the JsInterop scenario, we encourage developers to create
    #       proper JsInterop stubs next to the js_library rule and create a
    #       j2cl_import rule there.
    #   _js_exports: Exported JavaScript dependencies.
    #   _declare_legacy_namespace: A temporary measure while onboarding Docs, do
    #       not use.
    #   _transpiler: J2CL compiler instance to use.

    # exit early to avoid parse errors when running under bazel
    if not hasattr(native, "js_library"):
        return

    base_name = name
    srcs = srcs or []
    native_srcs = native_srcs or []
    tags = tags or []
    testonly = kwargs.get("testonly")

    # Direct automated dep picking tools and grok away from internal targets.
    internal_tags = tags + ["avoid_dep", "no_grok"]
    java_exports = []
    java_deps = []
    js_deps = _js_deps[:]
    js_exports = _js_exports[:]

    exports = _get_absolute_labels(kwargs, "exports")
    deps = _get_absolute_labels(kwargs, "deps")

    if not srcs:
        if deps:
            fail("deps not allowed without srcs")
        if native_srcs:
            fail("native_srcs not allowed without srcs")
        if _js_srcs:
            fail("_js_srcs not allowed without srcs")
        if _js_deps:
            fail("_js_deps not allowed without srcs")

    for export in exports:
        java_exports += [export + "_java_library"]
        js_exports += [export]

    target_name = native.package_name() + ":" + base_name

    # If this is JRE itself, don't synthesize the JRE dep.
    if srcs and target_name != "third_party/java_src/j2cl/jre/java:jre":
        deps += ["//internal_do_not_use:jre"]

    for dep in deps:
        java_deps += [dep + "_java_library"]
        js_deps += [dep]

    java_library_kwargs = dict(kwargs)
    java_library_kwargs["deps"] = java_deps or []
    java_library_kwargs["exports"] = java_exports
    java_library_kwargs["restricted_to"] = ["//buildenv/j2cl:j2cl_compilation"]

    # TODO(goktug): remove workaround after b/71772385 is fixed
    dummy_class_name = base_name.replace("-", "_")
    dummy_src = dummy_class_name + "_gen"
    native.genrule(
        name = dummy_src,
        outs = ["dummy_/%s/package-info.java" % dummy_class_name],
        cmd = "echo \"package dummy_;\" > $@",
    )

    j2cl_java_library(
        name = base_name + "_java_library",
        srcs = srcs,
        srcs_hack = [":" + dummy_src],
        tags = internal_tags,
        visibility = visibility,
        **java_library_kwargs
    )

    jszip_name = None

    if srcs:
        extra_transpiler_args = {}
        if _transpiler:
            extra_transpiler_args["transpiler"] = _transpiler
        js_sources_from_transpile = ":" + base_name + "_j2cl_transpile.js.zip"
        j2cl_transpile(
            name = base_name + "_j2cl_transpile",
            javalib = ":" + base_name + "_java_library",
            native_srcs = native_srcs,
            js_srcs = _js_srcs,
            testonly = testonly,
            readable_source_maps = _readable_source_maps,
            declare_legacy_namespace = _declare_legacy_namespace,
            restricted_to = ["//buildenv/j2cl:j2cl_compilation"],
            tags = internal_tags,
            **extra_transpiler_args
        )

        # Uh-oh: _js_import needs to depend on restricted_to=j2cl_compilation targets,
        # which it uses as classpath elements. But the _js_import can't itself be
        # restricted-to=j2cl_compilation, as it needs to be usable from "normal" build
        # rules. Our solution is to defeat the constraints system by building the
        # java_library as a host dep (by putting it in genrule.tools).
        #
        # Context:
        # https://groups.google.com/a/google.com/d/topic/target-constraints/ss38OI0UC9k/discussion
        # https://groups.google.com/a/google.com/d/topic/j2cl-team/IdWC-X-ky3s/discussion
        # https://docs.google.com/document/d/1bCVADLTenSVvVBkJ_Ip3EahdPdmEu1eyQ2wPGbE8sgM/edit?disco=AAAAAfa3IZU
        #
        # TODO(cpovirk): Find a less evil solution, maybe based on http://b/27044764

        # Copy the js to an unrestricted environment.
        jszip_name = base_name + ".js.zip"
        _do_env_copy(js_sources_from_transpile, jszip_name, testonly)

        # Expose java sources similar to java_library
        java_src_jar = "lib" + base_name + "-src.jar"
        _do_env_copy("lib" + base_name + "_java_library-src.jar", java_src_jar, testonly)

    # This forces execution of j2cl_transpile() targets (both immediate and in the
    # dependency chain) when build has been invoked on the js_import target.
    # Additionally, this is used as a workaround to make sure the zip ends up in
    # the runfiles directory as described in b/35847804.
    js_data = ([jszip_name] if jszip_name else []) + depset(js_deps + js_exports).to_list()

    # Bring zip srcs into the js build tree
    js_import(
        name = base_name,
        deps = js_deps,
        deps_mgmt = js_deps_mgmt,
        exports = js_exports,
        srczip = jszip_name,
        tags = tags,
        testonly = testonly,
        data = js_data,
        visibility = visibility,
    )

    if generate_build_test == None:
        generate_build_test = True

    if generate_build_test and srcs:
        # Add an empty .js file to the js_binary build test compilation so that  jscompiler does not
        # error out when there are no .js source (e.g. all sources are @JsFunction).
        native.genrule(
            name = base_name + "_empty_js_file",
            cmd = "echo \"// empty file\" > $(OUTS)",
            outs = [base_name + "_empty_js_file.js"],
        )
        native.js_library(
            name = base_name + "_empty_js_file_lib",
            srcs = [base_name + "_empty_js_file"],
            tags = ["no_grok"],
        )
        native.js_binary(
            name = base_name + "_js_binary",
            deps = [
                base_name,
                base_name + "_empty_js_file_lib",
            ],
            defs = J2CL_OPTIMIZED_DEFS,
            externs_list = _test_externs_list,
            include_default_externs = "off" if _test_externs_list else "web",
            tags = internal_tags + ["no_grok"],
            compiler = "//javascript/tools/jscompiler:head",
            testonly = 1,
            visibility = ["//visibility:private"],
        )

        build_test(
            name = base_name + "_build_test",
            targets = [
                base_name,
                base_name + "_js_binary",
            ],
            tags = internal_tags,
        )
