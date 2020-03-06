load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "castsdisabled",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.checks.type": "DISABLED",
        "jre.checkedMode": "DISABLED",
    },
)
