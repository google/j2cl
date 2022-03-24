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
    name = "enums",
    srcs = glob(["*.java"]),
    # TODO(b/219913874): java.lang.Enum to kotlin.Enum translation
    enable_kt = False,
)
