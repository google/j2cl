load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "instanceinitorder",
    srcs = glob(["*.java"]),
    # TODO(b/287350926): Enable when field initialization with default value is
    #  fixed.
    enable_kt = False,
)
