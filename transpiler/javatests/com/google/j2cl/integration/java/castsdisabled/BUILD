load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "castsdisabled",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.checks.type": "DISABLED",
        "jre.checkedMode": "DISABLED",
    },
    # Cast checks cannot be disabled for JVM, Wasm or J2KT.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
)
