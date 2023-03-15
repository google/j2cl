load("//build_defs:rules.bzl", "j2cl_library")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

closure_js_library(
    name = "internal_parametrized_test_suite",
    testonly = 1,
    srcs = ["parameterized_test_suite.js"],
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    deps = [
        "//third_party:closure_asserts",
        "//third_party:closure_testcase",
        "//third_party:closure_testsuite",
    ],
)

j2cl_library(
    name = "testcase_violator-j2cl",
    testonly = 1,
    srcs = ["TestCaseViolator.java"],
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    deps = ["//third_party:junit-j2cl"],
)
