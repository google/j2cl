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
    name = "arrayliteral",
    srcs = glob(["*.java"]),
    # TODO(b/227554485): missing runtime array insertion checks (missing ArrayStoreException)
    enable_kt_native = False,
)
