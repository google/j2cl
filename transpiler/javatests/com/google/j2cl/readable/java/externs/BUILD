load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
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
    generate_kt_readables = False,
    generate_wasm_readables = False,
    deps = [
        ":externs",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
