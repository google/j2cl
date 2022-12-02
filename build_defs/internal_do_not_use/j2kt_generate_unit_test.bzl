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
load("@bazel_tools//tools/build_defs/kotlin/release/rules/native:native_rules.bzl", "kt_apple_test_library")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

# buildifier: disable=function-docstring-args
def j2kt_generate_unit_test(name, test_class, deps, platform = "J2KT-JVM", tags = []):
    """Macro for generating kotlin version of test adapter for kt_jvm test
    """

    test_input = generate_test_input(name, test_class)

    adapter_file_name = test_class.split(".")[-1] + "_Adapter_" + platform + ".kt"
    if platform == "J2KT-JVM":
        j2kt_jvm_library(
            name = name + "_lib",
            srcs = [test_input],
            deps = deps + [
                "//build_defs/internal_do_not_use:internal_junit_runtime-j2kt-jvm",
            ],
            exports = deps,
            javacopts = ["-AtestPlatform=J2KT"],
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
            javacopts = ["-AtestPlatform=J2KT"],
            testonly = 1,
            tags = tags,
        )

    # The Java annotation processor on the above target generates kotlin srcs code as resource
    # and then use kt_jvm_library to compile kotlin srcs and finally used it as runtime dependency in kt_jvm_test
    out_jar = ":lib" + name + "_lib.jar"

    native.genrule(
        name = name + "_transpile_gen",
        srcs = [out_jar],
        outs = [adapter_file_name],
        cmd = "\n".join([
            "unzip -q $(location %s) *.kt -d zip_out/" % out_jar,
            "cd zip_out/",
            "mkdir -p ../$(RULEDIR)",
            "for f in $$(find . -name *.kt); do mv $$f ../$@; done",
        ]),
        testonly = 1,
        tags = ["manual", "notap"],
    )
    if platform == "J2KT-JVM":
        kt_jvm_library(
            name = name,
            srcs = [":" + adapter_file_name],
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
            srcs = [":" + adapter_file_name],
            target_compatible_with = ["//third_party/bazel_platforms/os:ios"],
            deps = [
                ":" + name + "_lib",
                "//build_defs/internal_do_not_use:internal_junit_runtime-j2kt-native",
            ],
        )
