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
    # Throws ArrayOutOfBoundsException instead of ClassCastException
    enable_kt_native = False,
    enable_wasm_custom_descriptors_jsinterop = True,
)
