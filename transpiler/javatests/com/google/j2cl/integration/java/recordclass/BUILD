load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    srcs = glob(["**/*.java"]),
    experimental_java_frontend = "javac",
    javacopts = [
        "-source 21",
        "-target 21",
    ],
)
