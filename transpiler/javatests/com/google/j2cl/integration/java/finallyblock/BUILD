load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "finallyblock",
    srcs = glob(["*.java"]),
    # TODO(b/227349235): Getting InvalidMutabilityException with old memory model
    enable_kt_native = False,
    # TODO(b/170691676): Enable when full try/catch/finally semantics are
    # implemented.
    enable_wasm = False,
)
