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
    name = "instancequalifieronstaticfield",
    srcs = glob(["*.java"]),
    # TODO(b/227349235): Getting InvalidMutabilityException with old memory model
    enable_kt_native = False,
)
