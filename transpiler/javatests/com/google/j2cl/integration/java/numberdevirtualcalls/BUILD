load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "numberdevirtualcalls",
    srcs = glob(["*.java"]),
    # TODO(b/239392817): Kotlinc artificially blocks toByte/toShort for Float/Double.
    enable_kt = False,
)
