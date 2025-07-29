load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# Use a separate library to ensure that string literal identity is preserved even across
# compilation boundaries.
integration_library(
    name = "constants",
    srcs = ["Constants.kt"],
)

integration_test(
    name = "strictequality",
    srcs = ["Main.kt"],
    deps = [":constants"],
)
