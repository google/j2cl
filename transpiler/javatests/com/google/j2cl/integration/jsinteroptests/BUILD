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
    name = "jsinteroptests",
    srcs = glob([
        "**/*.java",
        "**/*.js",
    ]),
    # TODO(b/118184493): remove when Java 9 language support is enabled.
    j2cl_library_tags = ["manual"],
    deps = [
        "//third_party:closure_reflect",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
