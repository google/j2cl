load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "objectdevirtualcalls",
    srcs = glob(["*.java"]),
    # TODO(b/227358347): kotlin.Any vs java.lang.Object in default Object.toString impl.
    enable_kt_native = False,
)
