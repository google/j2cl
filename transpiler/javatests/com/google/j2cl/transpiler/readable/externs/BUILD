load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/readable:readable_example.bzl",
    "readable_example",
)

closure_js_library(
    name = "externs",
    srcs = ["foo_externs.js"],
)

# Creates verification target
readable_example(
    srcs = glob([
        "*.java",
        "*.native.js",
    ]),
    deps = [
        ":externs",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
