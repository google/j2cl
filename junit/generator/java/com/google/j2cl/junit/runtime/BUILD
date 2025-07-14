load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load("//build_defs:rules.bzl", "j2cl_library", "j2wasm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

closure_js_library(
    name = "internal_j2cl_test_suite",
    testonly = 1,
    srcs = ["j2cl_test_suite.js"],
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    deps = [
        ":internal_parametrized_test_suite",
        "//third_party:closure_testcase",
        "//third_party:closure_testsuite",
    ],
)

closure_js_library(
    name = "internal_parametrized_test_suite",
    testonly = 1,
    srcs = ["parameterized_test_suite.js"],
    lenient = True,
)

j2wasm_library(
    name = "jsunit_helpers-j2wasm",
    testonly = 1,
    srcs = ["JsUnitHelpers.java"],
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    deps = [
        ":internal_assumption_violated_exception-j2wasm",
        "//:jsinterop-annotations-j2wasm",
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:closure_testcase",
    ],
)

j2cl_library(
    name = "jsunit_helpers-j2cl",
    testonly = 1,
    srcs = ["JsUnitHelpers.java"],
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    deps = [
        ":internal_assumption_violated_exception-j2cl",
        "//:jsinterop-annotations-j2cl",
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:closure_testcase",
    ],
)

j2cl_library(
    name = "testcase_violator-j2cl",
    testonly = 1,
    srcs = ["TestCaseViolator.java"],
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    deps = ["//third_party:junit-j2cl"],
)

j2cl_library(
    name = "internal_assumption_violated_exception-j2cl",
    srcs = ["InternalAssumptionViolatedException.java"],
    visibility = [
        "//build_defs/internal_do_not_use:__pkg__",
        "//junit/emul/java:__pkg__",
    ],
)
