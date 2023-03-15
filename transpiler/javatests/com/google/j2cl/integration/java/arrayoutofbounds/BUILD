load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "arrayoutofbounds",
    srcs = glob(["*.java"]),
    # ArrayOutBounds will not throw only for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
)
