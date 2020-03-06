load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "variables",
    srcs = glob(["*.java"]),
    # TODO(b/118184493): remove when Java 10 language support is enabled.
    j2cl_library_tags = ["manual"],
)
