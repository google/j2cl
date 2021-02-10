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
load(":j2wasm_library.bzl", "J2WASM_LIB_ATTRS", "j2wasm_library", "to_j2wasm_name")
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
    """
    args = dict(kwargs)

    # If this is JRE itself, don't synthesize the JRE dep.
    target_name = "//" + native.package_name() + ":" + name
    if args.get("srcs") and target_name != "//jre/java:jre":
        jre = Label("//:jre", relative_to_caller_repository = False)
        args["deps"] = (args.get("deps") or []) + [jre]

    j2cl_library_rule(
        name = name,
        **args
    )

    if args.get("srcs") and (generate_build_test == None or generate_build_test):
        build_test(name, kwargs.get("tags", []))

    j2wasm_library_name = to_j2wasm_name(name)

    if not native.existing_rule(j2wasm_library_name):
        j2wasm_args = _filter_j2wasm_attrs(dict(kwargs))

        _to_j2wasm_targets("deps", j2wasm_args)
        _to_j2wasm_targets("exports", j2wasm_args)
        j2wasm_args["tags"] = (j2wasm_args.get("tags") or []) + ["manual", "notap", "j2wasm"]

        j2wasm_library(
            name = j2wasm_library_name,
            **j2wasm_args
        )

_ALLOWED_ATTRS = [key for key in J2WASM_LIB_ATTRS] + ["tags", "visibility"]

def _filter_j2wasm_attrs(args):
    return {key: args[key] for key in _ALLOWED_ATTRS if key in args}

def _to_j2wasm_targets(key, args):
    labels = args.get(key)
    if not labels:
        return

    args[key] = [_to_j2wasm_target(label) for label in labels]

def _to_j2wasm_target(label):
    if type(label) == "string":
        return to_j2wasm_name(_absolute_label(label))

    # Label Object
    return label.relative(":%s" % to_j2wasm_name(label.name))

def _absolute_label(label):
    if label.startswith("//") or label.startswith("@"):
        if ":" in label:
            return label
        elif "/" in label:
            return "%s:%s" % (label, label.rsplit("/", 1)[-1])
        if not label.startswith("@"):
            fail("Unexpected label format: %s" % label)
        return "%s//:%s" % (label, label[1:])

    package_name = native.package_name()

    if label.startswith(":"):
        return "//%s%s" % (package_name, label)
    if ":" in label:
        return "//%s/%s" % (package_name, label)
    return "//%s:%s" % (package_name, label)
