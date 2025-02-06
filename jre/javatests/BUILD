# Description:
#   j2cl/jre: JRE subset for transpilation.

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_binary")
load("//build_defs:rules.bzl", "j2cl_library", "j2kt_native_library")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_binary")
load("@rules_java//java:defs.bzl", "java_library")
load(":j2cl_multi_test.bzl", "j2cl_multi_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

JS_ONLY_SRCS = glob([
    "**/Js*.java",
    "**/*.js",
])

SRCS = glob(
    ["**/*.java"],
    exclude = JS_ONLY_SRCS,
)

ERROR_PRONE_SUPPRESSIONS = [
    # keep sorted
    "-Xep:CheckReturnValue:OFF",
    "-Xep:ComparableType:OFF",
    "-Xep:DoNotCall:OFF",
    "-Xep:EqualsIncompatibleType:OFF",
    "-Xep:IdentityBinaryExpression:OFF",
    "-Xep:MixedArrayDimensions:OFF",  # See go/lsc-mixedarraydimensions
    "-Xep:ReturnValueIgnored:OFF",
    "-Xep:SelfAssertion:OFF",
    "-Xep:SelfComparison:OFF",
    "-Xep:SelfEquality:OFF",
    "-Xep:SelfEquals:OFF",
]

java_library(
    name = "emul_tests_lib",
    testonly = 1,
    srcs = SRCS,
    javacopts = ERROR_PRONE_SUPPRESSIONS,
    deps = [
        "//jre/java:javaemul_internal_annotations",
        "//third_party:jsinterop-annotations",
        "//third_party:jspecify_annotations",
        "//third_party:junit",
    ],
)

j2cl_library(
    name = "emul_tests_lib-j2cl",
    testonly = 1,
    srcs = SRCS,
    # Safe here as this is for tests only and there are no downstream users.
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    javacopts = ERROR_PRONE_SUPPRESSIONS,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
        "//third_party:junit-j2cl",
    ],
)

j2kt_native_library(
    name = "emul_tests_lib-j2kt-native",
    testonly = 1,
    srcs = glob(
        ["**/*.java"],
        exclude = JS_ONLY_SRCS + [
            "com/google/j2cl/jre/AllTests.java",
            "com/google/j2cl/jre/EmulJava9Suite.java",
            "com/google/j2cl/jre/java9/**/*.java",
        ],
    ),
    javacopts = ERROR_PRONE_SUPPRESSIONS,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-native",
        "//third_party:jsinterop-annotations-j2kt-native",
        "//third_party:jspecify_annotations-j2kt-native",
        "//third_party:junit-j2kt-native",
    ],
)

# A test entry point for running JRE tests in open source.
# To run the test for all platforms:
#   bazel test //jre/javatests:OpenSourceJre
# To debug a failing web test target, bazel run <target>_debug. e.g.
#   bazel run //jre/javatests:OpenSourceJre-j2cl_debug
j2cl_multi_test(
    name = "OpenSourceJre",
    # Please change the tested class here.
    test_class = "com.google.j2cl.jre.java.lang.StringTest",
    deps = [":emul_tests_lib"],
)

closure_js_binary(
    name = "conformance_binary_opensource",
    dependency_mode = "SORT_ONLY",
    suppress_on_all_sources_in_transitive_closure = [
        "analyzerChecks",
        "deprecated",
    ],
    deps = ["//:jre"],
)

closure_js_binary(
    name = "conformance_binary_whitespace_only_opensource",
    compilation_level = "WHITESPACE_ONLY",
    dependency_mode = "SORT_ONLY",
    suppress_on_all_sources_in_transitive_closure = [
        "analyzerChecks",
        "deprecated",
    ],
    deps = ["//:jre"],
)

genrule(
    name = "file_empty_check",
    outs = ["file_empty_check.sh"],
    cmd = "echo '[ ! -s $$1 ]' > $@",
)

sh_test(
    name = "opensource_stderr_empty_test",
    srcs = ["file_empty_check.sh"],
    args = ["$(location conformance_binary_opensource-stderr.txt)"],
    data = ["conformance_binary_opensource-stderr.txt"],
    tags = ["j2cl"],
)

sh_test(
    name = "whitespace_only_opensource_stderr_empty_test",
    srcs = ["file_empty_check.sh"],
    args = ["$(location conformance_binary_whitespace_only_opensource-stderr.txt)"],
    data = ["conformance_binary_whitespace_only_opensource-stderr.txt"],
    tags = ["j2cl"],
)
