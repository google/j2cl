load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load("//build_defs:rules.bzl", "j2kt_jvm_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# Exclude native sources from j2kt_jvm_library ad j2kt_native_library.
j2kt_jvm_library(
    name = "readable-j2kt-jvm",
    srcs = glob(["*.java"]),
    deps = ["//third_party:jsinterop-annotations-j2kt-jvm"],
)

closure_js_library(
    name = "externs",
    srcs = ["foo_externs.js"],
)

readable_example(
    srcs = glob([
        "*.java",
        "*.native.js",
    ]),
    # Native JsType does not make sense for j2kt-native.
    build_kt_native_readables = False,
    generate_wasm_readables = False,
    deps = [
        ":externs",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
