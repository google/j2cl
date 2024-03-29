load("//build_defs:rules.bzl", "j2wasm_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# Provide a custom j2wasm_library. Double and float are two different primitives
# types in wasm
j2wasm_library(
    name = "narrowingdoubletofloat-j2wasm",
    srcs = ["super-wasm/Main.java"],
    javacopts = ["-XepDisableAllChecks"],
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    name = "narrowingdoubletofloat",
    srcs = glob(["*.java"]),
    # Code running on the JVM honors float-double precision differences
    enable_jvm_test = False,
    enable_kt = False,
)
