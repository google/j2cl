load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)
load("//build_defs:rules.bzl", "j2wasm_library")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

closure_js_library(
    name = "foo",
    srcs = ["Foo.js"],
)

j2wasm_library(
    name = "wasmjsinterop-j2wasm",
    srcs = glob(["super-wasm/*.java"]),
    deps = [
        ":foo",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    name = "wasmjsinterop",
    srcs = glob(["*.java"]),
)
