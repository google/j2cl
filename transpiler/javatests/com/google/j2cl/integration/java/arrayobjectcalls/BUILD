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
    name = "arrayobjectcalls",
    srcs = glob(["*.java"]),
    # TODO(b/227166206): Problem with getClass() emulation for arrays.
    enable_kt_native = False,
)
