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
    name = "arrayofboxedtype",
    srcs = glob(["*.java"]),
    # TODO(b/219913874): java.lang.Number translation to kotlin.Number
    enable_kt = False,
)
