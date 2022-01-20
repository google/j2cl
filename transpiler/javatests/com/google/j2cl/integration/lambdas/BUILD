load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "lambdas",
    srcs = glob(["*.java"]),
    # TODO(b/186272309): Enable when captures are computed in the AST.
    enable_wasm = False,
    j2cl_library_tags = ["manual"],
)
