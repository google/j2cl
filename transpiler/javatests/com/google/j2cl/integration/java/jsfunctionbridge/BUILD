load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

integration_test(
    srcs = glob(["*.java"]),
    enable_kt_native = False,
    # TODO(b/527200669) Currently cannot pass arguments to generic JsFunction across JS/Wasm boundary.
    enable_wasm = False,
    suppress = ["JSC_TYPE_MISMATCH"],
)
