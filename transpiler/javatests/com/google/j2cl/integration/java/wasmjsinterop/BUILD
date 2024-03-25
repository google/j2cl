load("//build_defs:rules.bzl", "j2wasm_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2wasm_library(
    name = "wasmjsinterop-j2wasm",
    srcs = glob([
        "super-wasm/*.java",
        "*.js",
    ]),
    deps = [
        "//third_party:jsinterop-annotations-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    name = "wasmjsinterop",
    srcs = glob(["*.java"]),
)
