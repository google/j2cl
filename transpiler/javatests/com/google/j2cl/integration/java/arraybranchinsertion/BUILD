load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "arraybranchinsertion",
    srcs = glob(["*.java"]),
    # TODO(b/227554485): missing runtime array insertion checks (missing ArrayStoreException)
    enable_kt_native = False,
)
