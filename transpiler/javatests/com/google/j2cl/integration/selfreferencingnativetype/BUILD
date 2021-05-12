load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    # Google owns the copyright
    licenses = ["unencumbered"],
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "selfreferencingnativetype",
    srcs = glob(["*.java"]),
    # Uses jsinterop features not supported in WASM.
    enable_wasm = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
