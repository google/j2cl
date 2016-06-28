package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "arrayleafinsertion",
    srcs = glob(["*.java"]),
    closure_defines = {
        "ARRAY_CHECK_BOUNDS_": "true",
        "jre.checks.type": "ENABLED",
    },
)
