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
    name = "npecatch",
    srcs = glob(["*.java"]),
    enable_kt = True,
    # TODO(b/183769034): Enable once the wasm traps vs exceptions is solved.
    enable_wasm = False,
)
