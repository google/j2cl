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

load("//build_def:j2cl_java_library.bzl", j2cl_library_rule = "j2cl_library")
load("//build_def:j2cl_library_build_test.bzl", "build_test")

def j2cl_library(
        name,
        srcs = [],
        deps = [],
        exports = [],
        tags = [],
        native_srcs = [],
        generate_build_test = None,
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
      lib<name>.jar: A java archive containing the byte code.
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

    base_name = name
    srcs = srcs or []
    native_srcs = native_srcs or []
    tags = tags or []
    deps = deps or []
    exports = exports or []
    testonly = kwargs.get("testonly")

    if not srcs:
        if deps:
            fail("deps not allowed without srcs")
        if native_srcs:
            fail("native_srcs not allowed without srcs")
        if _js_srcs:
            fail("_js_srcs not allowed without srcs")
        if _js_deps:
            fail("_js_deps not allowed without srcs")

    target_name = native.package_name() + ":" + base_name

    # If this is JRE itself, don't synthesize the JRE dep.
    if srcs and target_name != "third_party/java_src/j2cl/jre/java:jre":
        deps = deps + ["//internal_do_not_use:jre"]

    java_library_kwargs = dict(kwargs)
    if _transpiler:
        java_library_kwargs["transpiler"] = _transpiler

    # TODO(goktug): remove workaround after b/71772385 is fixed
    dummy_class_name = base_name.replace("-", "__")
    dummy_src = dummy_class_name + "_gen"
    native.genrule(
        name = dummy_src,
        outs = ["dummy_/%s/package-info.java" % dummy_class_name],
        cmd = "echo \"package dummy_;\" > $@",
    )

    j2cl_library_rule(
        name = base_name,
        srcs = srcs,
        srcs_hack = [":" + dummy_src],
        deps = deps + _js_deps,
        exports = exports + _js_exports,
        native_srcs = native_srcs,
        js_srcs = _js_srcs,
        readable_source_maps = _readable_source_maps,
        declare_legacy_namespace = _declare_legacy_namespace,
        tags = tags,
        visibility = visibility,
        **java_library_kwargs
    )

    if srcs and (generate_build_test == None or generate_build_test):
        build_test(base_name, _test_externs_list, tags)
