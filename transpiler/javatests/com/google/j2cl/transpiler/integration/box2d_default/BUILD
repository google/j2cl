package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "box2d",
    srcs = glob(["*.java"]),
    closure_defines = {
        "ASSERTIONS_ENABLED_": "false",
        "jre.checks.checkLevel": "NORMAL",
    },
    deps = [
        "//transpiler/javatests/com/google/j2cl/transpiler/integration/box2d:box2d_j2cl",
    ],
)
