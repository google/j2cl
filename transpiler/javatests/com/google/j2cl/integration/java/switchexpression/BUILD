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
    # TODO(b/375058137): Enable when bug is fixed.
    enable_kt = False,
    # TODO(b/375058020): Enable when bug is fixed.
    enable_wasm = False,
    javacopts = [
        "-source 14",
        "-target 14",
    ],
)
