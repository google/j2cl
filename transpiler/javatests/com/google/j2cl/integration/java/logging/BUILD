load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "logging",
    srcs = glob(["*.java"]),
    # TODO(b/227424033): Reenable once the JRE logging APIs are available.
    enable_kt_native = False,
    tags = ["manual"],
)
