load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "enumspecialfunctions",
    srcs = glob(["*.java"]),
    # TODO(b/235809337): Enum.valueOf() throws Exception instead of IllegalArgumentException in Kotlin native.
    enable_kt_native = False,
)
