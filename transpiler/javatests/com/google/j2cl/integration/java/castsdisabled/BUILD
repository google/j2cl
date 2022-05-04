load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "castsdisabled",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.checks.type": "DISABLED",
        "jre.checkedMode": "DISABLED",
    },
    enable_kt = False,
    # Disabling cast checks can not be done in WASM.
    enable_wasm = False,
)
