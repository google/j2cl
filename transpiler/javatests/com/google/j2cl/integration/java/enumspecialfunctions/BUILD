load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "enumspecialfunctions",
    srcs = glob(["*.java"]),
    # TODO(b/235809337): Enable jvm test once Enum.valueOf() is correctly implemented and the test adapted accordingly.
    enable_jvm_test = False,
    # TODO(b/235809337): Enum.valueOf() throws Exception instead of IllegalArgumentException in Kotlin native.
    enable_kt_native = False,
)
