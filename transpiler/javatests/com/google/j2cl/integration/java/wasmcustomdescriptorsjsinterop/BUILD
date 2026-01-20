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
    name = "wasmcustomdescriptorsjsinterop-j2wasm",
    srcs = glob([
        "super-wasm/*.java",
    ]),
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    srcs = glob(["*.java"]),
    # Features only applicable to Wasm.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm_custom_descriptors_jsinterop = True,
)
