load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "fieldshadowing",
    srcs = glob(["*.java"]),
    # Field shadowing is not allowed in Kotlin.
    enable_kt = False,
)
