load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "localclasswithcaptures",
    srcs = glob(["*.java"]),
    # TODO(b/202427872): Nested classes
    enable_kt = False,
)
