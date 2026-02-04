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

load("//build_defs/internal_do_not_use/allowlists:j2kt_jvm.bzl", "J2KT_JVM_ALLOWLIST")
load("//build_defs/internal_do_not_use/allowlists:j2kt_native.bzl", "J2KT_NATIVE_ALLOWLIST")
load("//build_defs/internal_do_not_use/allowlists:j2kt_web.bzl", "J2KT_WEB_ENABLED", "J2KT_WEB_EXPERIMENT_ENABLED")
load("//build_defs/internal_do_not_use/allowlists:j2wasm.bzl", "J2WASM_ALLOWLIST")
load("//build_defs/internal_do_not_use/allowlists:kotlin.bzl", "KOTLIN_ALLOWLIST")
load(":j2cl_java_library.bzl", j2cl_library_rule = "j2cl_library")
load(":j2cl_library_build_test.bzl", "build_test")
load(":j2cl_util.bzl", "to_parallel_targets")
load(":j2kt_common.bzl", "j2kt_common")
load(":j2kt_library.bzl", "J2KT_JVM_LIB_ATTRS", "J2KT_NATIVE_LIB_ATTRS", "j2kt_jvm_library", "j2kt_native_library")
load(":j2wasm_common.bzl", "j2wasm_common")
load(":j2wasm_library.bzl", "J2WASM_LIB_ATTRS", "j2wasm_library")
load(":provider.bzl", "J2clInfo")

_KOTLIN_STDLIB_TARGET = "//build_defs/internal_do_not_use:kotlin_stdlib"
_JRE_J2KT_TARGET = "//third_party/java_src/xplat/j2kt/jre/java:jre-j2kt-web"

def _tree_artifact_proxy_impl(ctx):
    files = ctx.attr.j2cl_library[J2clInfo]._private_.output_js
    return DefaultInfo(files = depset([files]), runfiles = ctx.runfiles([files]))

_tree_artifact_proxy = rule(
    implementation = _tree_artifact_proxy_impl,
    attrs = {"j2cl_library": attr.label()},
)

# buildifier: disable=function-docstring-args
def j2cl_library(
        name,
        generate_build_test = None,
        generate_j2kt_native_library = None,
        generate_j2kt_jvm_library = None,
        generate_j2wasm_library = None,
        **kwargs):
    """Translates Java source into JS source encapsulated by a JsInfo provider.

    See j2cl_java_library.bzl#j2cl_library for the arguments.

    Implicit output targets:
      lib<name>.jar: A java archive containing the byte code.
      lib<name>-src.jar: A java archive containing the sources (source jar).
    """
    args = dict(kwargs)

    target_name = "//" + native.package_name() + ":" + name
    has_srcs = args.get("srcs") or args.get("kt_common_srcs")

    # If this is JRE itself, don't synthesize the JRE dep.
    if has_srcs and target_name != "//jre/java:jre":
        args["deps"] = args.get("deps", []) + [Label("//:jre")]

    j2cl_library_rule(
        name = name,
        **args
    )

    auto_generated_targets_tags = kwargs.get("tags", []) + ["manual", "notap", "no-ide"]

    # TODO(b/36549068): remove this workaround when tree artifacts can be
    # declared as the rule output.
    _tree_artifact_proxy(
        name = name + ".js",
        j2cl_library = ":" + name,
        visibility = ["//visibility:private"],
        tags = auto_generated_targets_tags,
        testonly = args.get("testonly", 0),
    )

    if has_srcs and (generate_build_test == None or generate_build_test):
        build_test(name, kwargs.get("tags", []))

    j2wasm_library_name = j2wasm_common.to_j2wasm_name(name)

    if generate_j2wasm_library == None:
        # By default refer back to allow list for implicit j2wasm target generation.
        generate_j2wasm_library = (
            not native.existing_rule(j2wasm_library_name) and
            J2WASM_ALLOWLIST.accepts(target_name)
        )

    if generate_j2wasm_library:
        j2wasm_args = _filter_j2wasm_attrs(dict(kwargs))

        to_parallel_targets("deps", j2wasm_args, j2wasm_common.to_j2wasm_name)
        to_parallel_targets("exports", j2wasm_args, j2wasm_common.to_j2wasm_name)

        j2wasm_library(
            name = j2wasm_library_name,
            tags = auto_generated_targets_tags + ["j2wasm"],
            **j2wasm_args
        )

    j2kt_native_library_name = j2kt_common.to_j2kt_native_name(name)

    if generate_j2kt_native_library == None:
        # By default refer back to allow list for implicit j2kt target generation.
        generate_j2kt_native_library = (
            not native.existing_rule(j2kt_native_library_name) and
            J2KT_NATIVE_ALLOWLIST.accepts(target_name)
        )

    if generate_j2kt_native_library:
        j2kt_args = _filter_j2kt_native_attrs(dict(kwargs))

        to_parallel_targets("deps", j2kt_args, j2kt_common.to_j2kt_native_name)
        to_parallel_targets("exports", j2kt_args, j2kt_common.to_j2kt_native_name)

        j2kt_native_library(
            name = j2kt_native_library_name,
            tags = auto_generated_targets_tags + ["j2kt", "ios"],
            **j2kt_args
        )

    j2kt_jvm_library_name = j2kt_common.to_j2kt_jvm_name(name)

    if generate_j2kt_jvm_library == None:
        # By default refer back to allow list for implicit j2kt target generation.
        generate_j2kt_jvm_library = (
            not native.existing_rule(j2kt_jvm_library_name) and
            J2KT_JVM_ALLOWLIST.accepts(target_name)
        )

    if generate_j2kt_jvm_library:
        j2kt_args = _filter_j2kt_jvm_attrs(dict(kwargs))

        to_parallel_targets("deps", j2kt_args, j2kt_common.to_j2kt_jvm_name)
        to_parallel_targets("exports", j2kt_args, j2kt_common.to_j2kt_jvm_name)

        j2kt_jvm_library(
            name = j2kt_jvm_library_name,
            tags = auto_generated_targets_tags + ["j2kt"],
            **j2kt_args
        )

_ALLOWED_ATTRS_J2KT_JVM = [key for key in J2KT_JVM_LIB_ATTRS] + ["visibility", "testonly"]

def _filter_j2kt_jvm_attrs(args):
    return {key: args[key] for key in _ALLOWED_ATTRS_J2KT_JVM if key in args}

_ALLOWED_ATTRS_J2KT_NATIVE = [key for key in J2KT_NATIVE_LIB_ATTRS] + ["visibility", "testonly"]

def _filter_j2kt_native_attrs(args):
    return {key: args[key] for key in _ALLOWED_ATTRS_J2KT_NATIVE if key in args}

_ALLOWED_ATTRS_WASM = [key for key in J2WASM_LIB_ATTRS] + ["visibility", "testonly"]

def _filter_j2wasm_attrs(args):
    return {key: args[key] for key in _ALLOWED_ATTRS_WASM if key in args}
