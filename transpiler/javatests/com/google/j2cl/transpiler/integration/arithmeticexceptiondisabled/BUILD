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
    name = "arithmeticexceptiondisabled",
    srcs = glob(["*.java"]),
    closure_defines = {"ARITHMETIC_EXCEPTION_CHECKS_ENABLED_": "false"},
)
