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
    srcs = ["Constants.java"],
)

integration_test(
    name = "strictequality",
    srcs = ["Main.java"],
    # TODO(b/312494789): Enable when signature of extension functions in
    #  J2KT-runtime are correct in terms of primitive vs boxed type.
    enable_kt_web = False,
    deps = [":constants"],
)
