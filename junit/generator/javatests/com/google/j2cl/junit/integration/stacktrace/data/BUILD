load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
    "java_and_j2cl_library",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace:__pkg__"],
    licenses = ["notice"],
)

j2cl_test_integration_test_data(
    name = "DuplicateFileNameStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        ":duplicate_names",
    ],
)

j2cl_test_integration_test_data(
    name = "KotlinDuplicateFileNameStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        ":duplicate_names",
    ],
)

java_and_j2cl_library(
    name = "duplicate_names",
    srcs = [
        "package1/DuplicateFileName.java",
        "package2/DuplicateFileName.java",
    ],
)

j2cl_test_integration_test_data(
    name = "AnonymousClassesStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "KotlinAnonymousClassesStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "CustomExceptionStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "KotlinCustomExceptionStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "JsExceptionStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "KotlinJsExceptionStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "JsExceptionNonJsConstructorStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "KotlinJsExceptionNonJsConstructorStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "LambdaStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "KotlinLambdaStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "SimpleThrowingStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    platforms = [
        "CLOSURE",
        "WASM",
    ],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "SimpleKotlinThrowingStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    platforms = [
        "CLOSURE",
    ],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "FillInStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "KotlinFillInStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "RecursiveStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "KotlinRecursiveStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInInstanceInitializer",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "KotlinThrowsInInstanceInitializer",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInBridgeMethod",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "KotlinThrowsInBridgeMethod",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInClassInitializer",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "KotlinThrowsInClassInitializer",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInConstructorTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "KotlinThrowsInConstructorTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInJsConstructorTest",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "KotlinThrowsInJsConstructorTest",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsInNativeJs",
    extra_defs = ["--inline_functions=false"],
    native_srcs = ["ThrowsInNativeJs.native.js"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "KotlinThrowsInNativeJs",
    extra_defs = ["--inline_functions=false"],
    native_srcs = ["KotlinThrowsInNativeJs.native.js"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsInJsFunction",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "KotlinThrowsInJsFunction",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsInJsProperty",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "KotlinThrowsInJsProperty",
    extra_defs = ["--inline_functions=false"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "ExceptionWithCauseStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

j2cl_test_integration_test_data(
    name = "KotlinExceptionWithCauseStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    deps = [":base"],
)

java_and_j2cl_library(
    name = "base",
    srcs = [
        "StacktraceTestBase.java",
    ],
)

closure_js_library(
    name = "stacktrace",
    srcs = ["throwing_js_class.js"],
    deps = ["//javascript/closure:base"],
)

j2cl_test_integration_test_data(
    name = "NativeStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    native_deps = [":stacktrace"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

j2cl_test_integration_test_data(
    name = "KotlinNativeStacktraceTest",
    extra_defs = ["--inline_functions=false"],
    native_deps = [":stacktrace"],
    deps = [
        ":base",
        "//third_party:jsinterop-annotations",
    ],
)

filegroup(
    name = "stacktrace-files",
    srcs = glob([
        "*.stacktrace.txt",
        "*.stacktrace_j2cl.txt",
        "*.stacktrace_j2cl_compiled.txt",
        "*.stacktrace_j2wasm.txt",
        "*.stacktrace_j2wasm_optimized.txt",
    ]),
)
