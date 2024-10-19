load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load("//:benchmarking/java/com/google/j2cl/benchmarking/benchmark_library.bzl", "benchmark_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//benchmarking:__subpackages__"],
    licenses = ["notice"],
)

benchmark_library(
    name = "framework",
    srcs = glob(["*.java"]),
    j2cl_deps = [
        ":externs",
        "//third_party:jsinterop-annotations-j2cl",
        "//jre/java:javaemul_internal_annotations-j2cl",
    ],
    j2wasm_deps = [
        ":externs",
        "//third_party:jsinterop-annotations-j2wasm",
        "//jre/java:javaemul_internal_annotations-j2wasm",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations",
        "//third_party:jsinterop-annotations",
    ],
)

closure_js_library(
    name = "externs",
    srcs = ["externs.js"],
)
