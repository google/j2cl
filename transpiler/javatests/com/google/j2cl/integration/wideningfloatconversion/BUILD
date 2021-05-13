load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)
load("//build_defs:rules.bzl", "j2wasm_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

# Provide a custom j2wasm_library. Double and float are two different primitives
# types in wasm
j2wasm_library(
    name = "wideningfloat-j2wasm",
    srcs = ["super-wasm/Main.java"],
    javacopts = ["-XepDisableAllChecks"],
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    name = "wideningfloat",
    srcs = ["Main.java"],
)
