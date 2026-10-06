load("@rules_closure//closure:defs.bzl", "closure_js_library")
load("//build_defs:rules.bzl", "j2kt_jvm_library", "j2wasm_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

# Exclude js_library dep from j2kt_jvm_library.
j2kt_jvm_library(
    name = "readable-j2kt-jvm",
    srcs = glob(["*.java"]),
    j2kt_j2objc_interop_enabled = False,
    deps = ["//third_party:jsinterop-annotations-j2kt-jvm"],
)

j2wasm_library(
    name = "readable-j2wasm",
    srcs = glob(["*.java"]),
    deps = [
        ":externs",
        "//third_party:jsinterop-annotations-j2wasm",
    ],
)

closure_js_library(
    name = "externs",
    srcs = ["foo_externs.js"],
)

readable_example(
    srcs = glob(["*.java"]),
    # Native JsType does not make sense for j2kt-native.
    build_kt_native_readables = False,
    deps = [
        ":externs",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
