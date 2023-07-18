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
    # Throws ArrayOutOfBoundsException instead of ClassCastException
    enable_kt_native = False,
    # JsFunction not supported in Wasm.
    enable_wasm = False,
)
