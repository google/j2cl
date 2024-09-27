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
load(":j2cl_util.bzl", "to_parallel_targets")
load(":j2kt_common.bzl", "j2kt_common")
load(":j2kt_library.bzl", "J2KT_JVM_LIB_ATTRS", "J2KT_NATIVE_LIB_ATTRS", "j2kt_jvm_library", "j2kt_native_library")
load(":j2wasm_common.bzl", "j2wasm_common")
load(":j2wasm_library.bzl", "J2WASM_LIB_ATTRS", "j2wasm_library")
load(":provider.bzl", "J2clInfo", "J2wasmInfo")

# Packages that j2cl rule will generate j2kt jvm packages by default. Used to simplify test
# rules.
_J2KT_JVM_PACKAGES = [
    "third_party/java_src/animal_sniffer",
    "third_party/java_src/google_common/current/java/com/google/common/annotations",
    "benchmarking/java/com/google/j2cl/benchmarks/octane/raytrace",
    "build_defs/internal_do_not_use",
    "transpiler/javatests/com/google/j2cl/integration/java",
    "transpiler/javatests/com/google/j2cl/integration/testing",
    "transpiler/javatests/com/google/j2cl/readable/java",
    "jre/javatests",
    "junit/generator/java/com/google/j2cl/junit/apt",
    "junit/generator/java/com/google/j2cl/junit/runtime",
    "junit/generator/javatests/com/google/j2cl/junit/integration",
    "third_party/java_src/j2objc/annotations",
    "third_party/java/animal_sniffer",
    "third_party/java/auto",
    "third_party/java/checker_framework_annotations",
    "third_party/java/error_prone",
    "third_party/java/j2objc",
    "third_party/java/jsr250_annotations",
    "third_party/java/jsr330_inject",
    "third_party/java/junit",
]

# Packages that j2cl_library macro will generate j2kt web packages by default.
_J2KT_WEB_PACKAGES = [
    "samples/box2d/src/main/java",
    "transpiler/javatests/com/google/j2cl/integration/java",
    "transpiler/javatests/com/google/j2cl/readable/java",
]

_J2KT_WEB_DISABLED_TARGETS = [
]

# Packages that j2cl rule will generate j2kt native packages by default. Used to simplify test
# rules.
_J2KT_NATIVE_PACKAGES = [
    "java/com/google/thirdparty/publicsuffix",
    "third_party/java/animal_sniffer",
    "third_party/java/auto",
    "third_party/java/checker_framework_annotations",
    "third_party/java/error_prone",
    "third_party/java/findurl",
    "third_party/java/googicu",
    "third_party/java/j2objc",
    "third_party/java/joda_time",
    "third_party/java/jsr250_annotations",
    "third_party/java/jsr330_inject",
    "third_party/java/junit",
    "third_party/java_src/animal_sniffer",
    "third_party/java_src/findurl",
    "third_party/java_src/googicu",
    "build_defs/internal_do_not_use",
    "junit/generator/java/com/google/j2cl/junit/apt",
    "junit/generator/java/com/google/j2cl/junit/runtime",
    "junit/generator/javatests/com/google/j2cl/junit/integration",
    "transpiler/javatests/com/google/j2cl/integration/java",
    "transpiler/javatests/com/google/j2cl/integration/testing",
    "transpiler/javatests/com/google/j2cl/readable/java",
    "third_party/java_src/j2objc/annotations",
    "third_party/java_src/jsr330_inject",
]

_J2WASM_PACKAGES = [
    "third_party/java/animal_sniffer",
    "third_party/java/auto",
    "third_party/java/checker_framework_annotations",
    "third_party/java/dagger",
    "third_party/java/error_prone",
    "third_party/java/jakarta_inject",
    "third_party/java/jsr250_annotations",
    "third_party/java/jsr330_inject",
    "third_party/java/junit",
    "third_party/java/qo_metafile",
    "third_party/java/qo_ole",
    "third_party/java/re2j",
    "third_party/java/truth",
    "third_party/java_src/animal_sniffer",
    "third_party/java_src/dagger",
    "third_party/java_src/google_common/current",
    "third_party/java_src/jakarta_inject",
    "third_party/java_src/j2cl",
    "build_defs/internal_do_not_use",
    "junit",
    "samples",
    "third_party",
    "third_party/java_src/jsr330_inject",
    "third_party/java_src/qo_metafile",
    "third_party/java_src/qo_ole",
    "third_party/java_src/re2j",
    "third_party/java_src/truth",
]

_KOTLIN_STDLIB_TARGET = "//build_defs/internal_do_not_use:kotlin_stdlib"
_JRE_J2KT_TARGET = "//third_party/java_src/xplat/j2kt/jre/java:jre-j2kt-web"

def _tree_artifact_proxy_impl(ctx):
    files = []
    if J2clInfo in ctx.attr.j2cl_library:
        files = ctx.attr.j2cl_library[J2clInfo]._private_.output_js
    elif J2wasmInfo in ctx.attr.j2cl_library:
        files = ctx.attr.j2cl_library[J2wasmInfo]._private_.wasm_modular_info.provider._private_.output_js
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

    # TODO(b/259727254): This doesn't cover all scenarios.
    has_kotlin_srcs = args.get("kt_common_srcs") or (
        args.get("srcs") and any([s for s in args.get("srcs") if s.endswith(".kt")])
    )

    is_j2kt_web_allowed = any([p for p in _J2KT_WEB_PACKAGES if native.package_name().startswith(p) and target_name not in _J2KT_WEB_DISABLED_TARGETS])

    # These arguments should not be set by the user.
    args["j2cl_transpiler_override"] = None
    args["j2kt_web_experiment_enabled"] = False

    if has_kotlin_srcs:
        # TODO(b/217287994): Replace with more traditional allow-listing.
        args["j2cl_transpiler_override"] = (
            "//build_defs/internal_do_not_use:BazelJ2clBuilderWithKotlinSupport"
        )

        if target_name != "//ktstdlib:j2cl_kt_stdlib":
            args["deps"] = args.get("deps", []) + [_KOTLIN_STDLIB_TARGET]

    elif is_j2kt_web_allowed:
        # Enable j2kt-web if the blaze flag is set to True
        args["j2kt_web_experiment_enabled"] = select({
            "//build_defs/internal_do_not_use:j2kt_web_enabled": True,
            "//conditions:default": False,
        })

        if has_srcs:
            # If j2kt-web is enabled, the java files are first transpiled to Kotlin before being
            # send to the J2CL transpiler. In that case, we need to use the j2cl transpiler binary
            # embedding the Kotlin frontend.
            args["j2cl_transpiler_override"] = select({
                "//build_defs/internal_do_not_use:j2kt_web_enabled": "//build_defs/internal_do_not_use:BazelJ2clBuilderWithKotlinSupport",
                "//conditions:default": None,
            })

            # If j2kt-web is enabled, we need to add _JRE_J2KT_TARGET (to resolve calls added by
            # j2kt) and _KOTLIN_STDLIB_TARGET as dependencies.
            args["deps"] = args.get("deps", []) + select({
                "//build_defs/internal_do_not_use:j2kt_web_enabled": [_JRE_J2KT_TARGET, _KOTLIN_STDLIB_TARGET],
                "//conditions:default": [],
            })

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
            any([p for p in _J2WASM_PACKAGES if native.package_name().startswith(p)])
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

        # TODO(b/36549068): remove this workaround when tree artifacts can be
        # declared as the rule output.
        _tree_artifact_proxy(
            name = j2wasm_library_name + ".modular",
            j2cl_library = ":" + j2wasm_library_name,
            visibility = ["//visibility:private"],
            tags = auto_generated_targets_tags,
            testonly = args.get("testonly", 0),
        )

    j2kt_native_library_name = j2kt_common.to_j2kt_native_name(name)

    if generate_j2kt_native_library == None:
        # By default refer back to allow list for implicit j2kt target generation.
        generate_j2kt_native_library = (
            not native.existing_rule(j2kt_native_library_name) and
            any([p for p in _J2KT_NATIVE_PACKAGES if native.package_name().startswith(p)])
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
            any([p for p in _J2KT_JVM_PACKAGES if native.package_name().startswith(p)])
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
