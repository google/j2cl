package(default_visibility = ["//transpiler/javatests/com/google/j2cl/transpiler/sizetest:__pkg__"])

licenses(["notice"])  # Apache 2.0

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
    # TODO(b/118184493): remove when Java 11 language support is enabled.
    j2cl_library_tags = ["manual"],
)
