package(default_visibility = ["//:__subpackages__"])

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
    name = "gwtincompatible",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/annotations:annotations-j2cl",
        "//java/com/google/common/io:io-j2cl",
    ],
)
