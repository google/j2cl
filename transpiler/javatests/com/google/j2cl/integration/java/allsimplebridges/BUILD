load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "allsimplebridges",
    srcs = glob(["*.java"]),
    # TODO(b/202428351): Support generics in J2KT
    enable_kt = False,
    suppress = ["JSC_UNKNOWN_OVERRIDE"],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
