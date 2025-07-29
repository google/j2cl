load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "strings",
    srcs = glob(["*.kt"]),
    # This test is temporary and not meant to be tested on the JVM.
    enable_jvm_test = False,
)
