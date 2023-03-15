load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "jsfunctionbridge",
    srcs = glob(["*.java"]),
    enable_kt_native = False,
    suppress = ["JSC_TYPE_MISMATCH"],
)
