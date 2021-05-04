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
    name = "devirtualizedsupermethodcall",
    srcs = glob(["*.java"]),
    # TODO(b/186691983): Reenable wasm tests when j.l.Object.toString is fixed.
    enable_wasm = False,
)
