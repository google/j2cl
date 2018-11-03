load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

package(default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace:__pkg__"])

# Apache2
licenses(["notice"])

load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
)

j2cl_test_integration_test_data(
    name = "AnonymousClassesStacktraceTest",
    extra_defs = ["--inline_functions=false"],
)

j2cl_test_integration_test_data(
    name = "LambdaStacktraceTest",
    extra_defs = ["--inline_functions=false"],
)

j2cl_test_integration_test_data(
    name = "SimpleThrowingStacktraceTest",
    extra_defs = ["--inline_functions=false"],
)

j2cl_test_integration_test_data(
    name = "RecursiveStacktraceTest",
    extra_defs = ["--inline_functions=false"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInInstanceInitializer",
    extra_defs = ["--inline_functions=false"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInBridgeMethod",
    extra_defs = ["--inline_functions=false"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInClassInitializer",
    extra_defs = ["--inline_functions=false"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInConstructorTest",
    extra_defs = ["--inline_functions=false"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInJsConstructorTest",
    extra_defs = ["--inline_functions=false"],
    deps = ["//third_party:gwt-jsinterop-annotations"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInNativeJs",
    extra_defs = ["--inline_functions=false"],
    native_srcs = ["ThrowsInNativeJs.native.js"],
    deps = ["//third_party:gwt-jsinterop-annotations"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInJsFunction",
    extra_defs = ["--inline_functions=false"],
    deps = ["//third_party:gwt-jsinterop-annotations"],
)

closure_js_library(
    name = "stacktrace",
    srcs = ["throwing_js_class.js"],
    deps = ["//javascript/closure:base"],
)

j2cl_test_integration_test_data(
    name = "NativeStacktraceTest",
    _js_deps = [":stacktrace"],
    extra_defs = ["--inline_functions=false"],
    deps = ["//third_party:gwt-jsinterop-annotations"],
)

filegroup(
    name = "stacktrace-files",
    srcs = glob(["*.stacktrace.txt"]),
)
