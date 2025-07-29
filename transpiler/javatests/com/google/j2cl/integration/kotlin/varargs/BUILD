load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "varargs",
    srcs = glob(["**/*.kt"]),
)
