load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "lateinit",
    srcs = glob(["*.kt"]),
    closure_defines = {
        "jre.checks.api": "DISABLED",
        "jre.checkedMode": "DISABLED",
    },
)
