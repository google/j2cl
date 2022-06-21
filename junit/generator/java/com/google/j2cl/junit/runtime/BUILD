load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

package(
    licenses = ["notice"],
)

closure_js_library(
    name = "internal_parametrized_test_suite",
    testonly = 1,
    srcs = ["parameterized_test_suite.js"],
    licenses = ["notice"],
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    deps = [
        "//javascript/closure:base",
        "//third_party/javascript/closure/asserts",
        "//third_party/javascript/closure/object",
        "//third_party/javascript/closure/testing:testcase",
        "//third_party/javascript/closure/testing:testsuite",
    ],
)
