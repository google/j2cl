load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "jsoverlaywithjsfunction",
    srcs = glob(["*.java"]),
    enable_wasm = False,  # TODO(b/273552727) JsFunction not supported in Wasm.
)
