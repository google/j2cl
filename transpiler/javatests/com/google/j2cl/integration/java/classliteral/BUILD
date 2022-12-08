load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "classliteral",
    srcs = glob(["*.java"]),
    # TODO(b/253494330): Enable this when foo.class.getName() is handled properly for both Java and
    # Javascript.
    enable_jvm_test = False,
    # TODO(b/202428351): Generics support
    enable_kt = False,
)
