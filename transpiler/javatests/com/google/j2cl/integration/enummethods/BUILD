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
    name = "enummethods",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    # Disabled because it uses native types to test the enum properties.
    enable_wasm = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
