load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "castsenabled",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.checks.type": "ENABLED",
    },
    # TODO(b/183769034): Enable once the wasm traps vs exceptions is solved.
    enable_wasm = False,
)
