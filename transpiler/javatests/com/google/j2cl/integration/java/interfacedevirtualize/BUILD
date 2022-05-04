load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "interfacedevirtualize",
    srcs = glob(["*.java"]),
    enable_kt = False,
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
