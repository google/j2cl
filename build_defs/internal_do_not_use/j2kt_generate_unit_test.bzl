"""j2kt_generate_jsunit_suite build macro

Takes Java source that contains JUnit tests and translates it into a goog.testing.testSuite.


Example using a j2kt_jvm_test:

j2kt_jvm_library(
    name = "mytests",
    ...
)

j2kt_jvm_generate_unit_test(
    name = "CodeGen",
    deps = [":mytests"],
    test_class = "mypackage.MyTest",
)

"""

load(":j2kt_library.bzl", "j2kt_jvm_library", "j2kt_native_library")
load(":generate_test_input.bzl", "generate_test_input")
load("@bazel_tools//tools/build_defs/kotlin/native:rules.bzl", "kt_apple_test_library")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

# buildifier: disable=function-docstring-args
def j2kt_generate_unit_test(name, test_class, deps, platform = "J2KT-JVM", tags = []):
    """Macro for generating kotlin version of test adapter for kt_jvm test
    """

    test_input = generate_test_input(name, test_class)

    if platform == "J2KT-JVM":
        j2kt_jvm_library(
            name = name + "_lib",
            srcs = [test_input],
            deps = deps + [
                "//build_defs/internal_do_not_use:internal_junit_runtime-j2kt-jvm",
            ],
            exports = deps,
            javacopts = ["-AtestPlatform=J2KT-JVM"],
            testonly = 1,
            tags = tags,
        )

    else:
        j2kt_native_library(
            name = name + "_lib",
            srcs = [test_input],
            deps = deps + [
                "//build_defs/internal_do_not_use:internal_junit_runtime-j2kt-native",
            ],
            exports = deps,
            javacopts = ["-AtestPlatform=J2KT-NATIVE"],
            testonly = 1,
            tags = tags,
        )

    # The Java annotation processor on the above target generates kotlin srcs code as resource
    # and then use kt_jvm_library to compile kotlin srcs and finally used it as runtime dependency in kt_jvm_test
    out_jar = ":lib" + name + "_lib.jar"

    extract_kotlin_srcjar(
        name = name + "_transpile_gen",
        input_jar = out_jar,
    )

    if platform == "J2KT-JVM":
        kt_jvm_library(
            name = name,
            srcs = [":" + name + "_transpile_gen"],
            deps = [
                ":" + name + "_lib",
                "//third_party/kotlin/kotlin:kotlin_test_junit",
                "//build_defs/internal_do_not_use:internal_junit_runtime-j2kt-jvm",
            ],
            testonly = 1,
        )

    else:
        kt_apple_test_library(
            name = name,
            srcs = [":" + name + "_transpile_gen"],
            target_compatible_with = ["//third_party/bazel_platforms/os:ios"],
            deps = [
                ":" + name + "_lib",
                "//build_defs/internal_do_not_use:internal_junit_runtime-j2kt-native",
            ],
        )

def _extract_kotlin_srcjar(ctx):
    """Extracts the generated kotlin files from transpiled source jar.

    Returns tree artifact outputs of the extracted kotlin sources.
    """

    # The name of the artifact directory should be unique for each test target to avoid conflicts.
    # The last segment of each artifact name should be kotlin
    output_dir = ctx.actions.declare_directory(ctx.label.name + "/kotlin")

    ctx.actions.run_shell(
        inputs = [ctx.file.input_jar],
        outputs = [output_dir],
        command = "unzip -q %s *.kt -d %s" % (ctx.file.input_jar.path, output_dir.path),
    )

    return [DefaultInfo(files = depset([output_dir]))]

extract_kotlin_srcjar = rule(
    implementation = _extract_kotlin_srcjar,
    attrs = {
        "input_jar": attr.label(allow_single_file = [".jar"], mandatory = True),
    },
)
