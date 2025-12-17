load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    srcs = glob(["*.java"]),
    # TODO(b/466394693): make the normalization work with j2kt.
    enable_kt = False,
    experimental_java_frontend = "javac",
    javacopts = [
        "-source 21",
        "-target 21",
    ],
)
