load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    srcs = glob(["*.kt"]),
    defs = [
        # Turn on asserts since it is what this test is testing.
        "--remove_j2cl_asserts=false",
    ],
)
