load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "interfacejsbridge",
    srcs = glob(["*.java"]),
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
