load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "jsfunction",
    srcs = glob(["*.java"]),
    enable_kt = False,
    # JsFunction not supported in WASM.
    enable_wasm = False,
)
