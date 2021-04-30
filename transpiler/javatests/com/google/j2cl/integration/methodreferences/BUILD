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
    name = "methodreferences",
    srcs = glob(["*.java"]),
    enable_rta = False,  # TODO(b/146458562): Enable RTA when this bug is fixed.
    enable_wasm = True,
    deps = [
        "//third_party:gwt-javaemul-internal-annotations-j2cl",
    ],
)
