package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "fieldshadowing",
    srcs = glob(["*.java"]),
)
