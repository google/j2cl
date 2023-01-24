load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)
load("//build_defs:rules.bzl", "j2wasm_library")

package(licenses = ["notice"])

j2wasm_library(
    name = "wasmjsinterop-j2wasm",
    srcs = glob(["super-wasm/*.java"]),
    deps = ["//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm"],
)

integration_test(
    name = "wasmjsinterop",
    srcs = glob(["*.java"]),
)
