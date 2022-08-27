load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "jsfunctionbridge",
    srcs = glob(["*.java"]),
    # TODO(b/243113496): Support RAW types.
    enable_kt = False,
    suppress = ["JSC_TYPE_MISMATCH"],
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
