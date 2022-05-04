load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "arrayoutofbounds",
    srcs = glob(["*.java"]),
    enable_kt = False,
    # This is testing that you can insert elements past the end of array;
    # functionality that is supported in JavaScript but not in WASM.
    enable_wasm = False,
)
