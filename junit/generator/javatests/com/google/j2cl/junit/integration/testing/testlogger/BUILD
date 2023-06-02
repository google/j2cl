load("//build_defs:rules.bzl", "j2cl_library", "j2kt_jvm_library", "j2kt_native_library", "j2wasm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration:__subpackages__"],
    licenses = ["notice"],
)

java_library(
    name = "testlogger",
    srcs = glob(["*.java"]),
)

j2kt_jvm_library(
    name = "testlogger-j2kt-jvm",
    srcs = glob(["*.java"]),
)

j2kt_native_library(
    name = "testlogger-j2kt-native",
    srcs = glob(["*.java"]),
)

# TODO(b/280862756): Remove explicit j2wasm_library when j2cl_library generated one can handle js deps.
j2wasm_library(
    name = "testlogger-j2wasm",
    srcs = glob(["super/*.java"]),
    deps = [
        "//third_party:closure_testcase",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
    ],
)

j2cl_library(
    name = "testlogger-j2cl",
    srcs = glob(["super/*.java"]),
    deps = [
        "//third_party:closure_testcase",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
