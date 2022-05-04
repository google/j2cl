load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "strictequality",
    srcs = ["Main.java"],
    enable_kt = False,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
    ],
)
