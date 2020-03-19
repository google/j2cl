"""j2cl_library build macro

Takes Java source, translates it into Closure style JS and surfaces it to the
rest of the build tree with a js_common.provider. Generally library rules dep on
other library rules for reference resolution and this build macro is no
exception. In particular the deps this rule needs for reference resolution are
java_library() targets which will have been created by other invocations of
this same j2cl_library build macro.


Example use:

# Effectively creates closure_js_library(name="Foo") containing translated JS.
j2cl_library(
    name = "Foo",
    srcs = glob(["Foo.java"]),
    deps = [":Bar"]  # Directly depends on j2cl_library(name="Bar")
)

# Effectively creates closure_js_library(name="Bar") containing the results.
j2cl_library(
    name = "Bar",
    srcs = glob(["Bar.java"]),
)

"""

load(":j2cl_java_library.bzl", j2cl_library_rule = "j2cl_library")
load(":j2cl_library_build_test.bzl", "build_test")

def j2cl_library(
        name,
        generate_build_test = None,
        **kwargs):
    """Translates Java source into JS source in a js_common.provider target.

    See j2cl_java_library.bzl#j2cl_library for the arguments.

    Implicit output targets:
      lib<name>.jar: A java archive containing the byte code.
      lib<name>-src.jar: A java archive containing the sources (source jar).

    Args:
      srcs: Source files (.java or .srcjar) to compile.
      deps: Labels of other j2cl_library() rules.
            NOT labels of java_library() rules.
    """
    args = dict(kwargs)

    _ensureList(args, "srcs")
    _ensureList(args, "deps")

    hidden_arg_names = [i for i in args if i.startswith("_")]
    for arg_name in hidden_arg_names:
        args[arg_name[1:]] = args.pop(arg_name)

    # If this is JRE itself, don't synthesize the JRE dep.
    target_name = "//" + native.package_name() + ":" + name
    if args["srcs"] and target_name != "//jre/java:jre":
        args["deps"].append(Label("//build_defs/internal_do_not_use:jre", relative_to_caller_repository = False))

    j2cl_library_rule(
        name = name,
        **args
    )

    if args["srcs"] and (generate_build_test == None or generate_build_test):
        build_test(name, kwargs.get("tags", []))

def _ensureList(args, name):
    # TODO(goktug): Remove to_list() coercions after cleaning the callsites w/ depsets.
    old_value = args.get(name) or []
    if type(old_value) == type(depset()):
        old_value = old_value.to_list()

    args[name] = old_value + []
