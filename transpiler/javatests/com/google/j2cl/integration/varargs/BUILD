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
    name = "varargs",
    srcs = glob(["**/*.java"]),
    deps = [
        "//third_party:gwt-javaemul-internal-annotations-j2cl",
    ],
)
