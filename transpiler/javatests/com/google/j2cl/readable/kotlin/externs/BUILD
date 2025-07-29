load("@rules_closure//closure:defs.bzl", "closure_js_library")
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
        "*.kt",
        "*.native.js",
    ]),
    deps = [
        ":externs",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
