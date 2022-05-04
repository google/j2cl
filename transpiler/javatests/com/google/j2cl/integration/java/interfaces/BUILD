load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "interfaces",
    srcs = glob(["*.java"]),
    enable_kt = False,
    j2cl_library_tags = ["manual"],
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
