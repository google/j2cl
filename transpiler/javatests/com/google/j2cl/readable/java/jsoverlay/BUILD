load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)
load("//build_defs:rules.bzl", "j2wasm_library")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

package(licenses = ["notice"])

# TODO(b/269494058) Remove if js srcs are allowed in j2wasm_library.
closure_js_library(
    name = "native_sources",
    srcs = glob(["native_sources/*.js"]),
    deps = ["//javascript/closure:base"],
)

j2wasm_library(
    name = "readable-j2wasm",
    srcs = glob(["super-wasm/*.java"]),
    deps = [
        ":native_sources",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
    ],
)

readable_example(
    srcs = glob([
        "*.java",
        "native_sources/*.js",
    ]),
    # TODO(b/202515129): Allow dependencies on native code (like js & iOS) in J2KT
    generate_kt_readables = False,
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
