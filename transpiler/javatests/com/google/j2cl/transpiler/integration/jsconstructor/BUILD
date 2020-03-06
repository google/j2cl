load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
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
    name = "jsconstructor",
    srcs = glob(["*.java"]),
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
