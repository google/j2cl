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

load(":j2cl_java_library.bzl", j2cl_library_rule = "j2cl_library")
load(":j2cl_library_build_test.bzl", "build_test")

def j2cl_library(
        name,
        native_srcs = [],
        generate_build_test = None,
        _js_srcs = [],
        _js_deps = [],
        _js_exports = [],
        **kwargs):
    """Translates Java source into JS source in a js_common.provider target.

    See j2cl_java_library.bzl#j2cl_library for the arguments.

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

    args = dict(kwargs)
    _append(args, "srcs", native_srcs)
    _append(args, "srcs", _js_srcs)
    _append(args, "deps", _js_deps)
    _append(args, "exports", _js_exports)

    hidden_arg_names = [i for i in args if i.startswith("_")]
    for arg_name in hidden_arg_names:
        args[arg_name[1:]] = args.pop(arg_name)

    # If this is JRE itself, don't synthesize the JRE dep.
    target_name = "//" + native.package_name() + ":" + name
    if args["srcs"] and target_name != "//jre/java:jre":
        args["deps"].append("//build_defs/internal_do_not_use:jre")

    # TODO(goktug): remove workaround after b/71772385 is fixed
    dummy_class_name = name.replace("-", "__")
    dummy_src = dummy_class_name + "_gen"
    native.genrule(
        name = dummy_src,
        outs = ["dummy_/%s/package-info.java" % dummy_class_name],
        cmd = "echo \"package dummy_;\" > $@",
    )

    j2cl_library_rule(
        name = name,
        srcs_hack = [":" + dummy_src],
        **args
    )

    if args["srcs"] and (generate_build_test == None or generate_build_test):
        build_test(name, kwargs.get("tags", []))

def _append(args, name, value):
    # TODO(goktug): Remove list() coercions after cleaning the callsites w/ depsets since it is
    #  slotted for deprecation in favor of explicit to_list calls.
    args[name] = list(args.get(name) or []) + list(value or [])
