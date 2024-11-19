load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "switchexpression",
    srcs = glob(["*.java"]),
    # TODO(b/379697518): Enable once @DoNotAutobox is propagated by J2KT.
    enable_kt_web = False,
    javacopts = [
        "-source 14",
        "-target 14",
    ],
)
