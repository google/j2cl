package(default_visibility = ["//transpiler/javatests/com/google/j2cl/transpiler/sizetest:__pkg__"])

# Apache2
licenses(["notice"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "lambdas",
    srcs = glob(["*.java"]),
)
