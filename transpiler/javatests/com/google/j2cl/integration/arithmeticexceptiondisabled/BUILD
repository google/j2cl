load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "arithmeticexceptiondisabled",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.checks.numeric": "DISABLED",
        "jre.checkedMode": "DISABLED",
    },
    enable_kt = False,
    # This test makes sense only in JavaScript where integral divide by 0 can
    # be made not to throw.
    enable_wasm = False,
)
