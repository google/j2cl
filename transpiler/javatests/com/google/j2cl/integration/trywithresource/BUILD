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
    name = "trywithresource",
    srcs = glob(["*.java"]),
    # TODO(b/170691676): Enable when full try/catch/finally semantics are
    # implemented.
    enable_wasm = False,
    j2cl_library_tags = ["manual"],
)
