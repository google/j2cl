load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "box2d",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.checks.checkLevel": "NORMAL",
    },
    enable_bundle_size_report = True,
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/box2d:box2d_j2cl",
    ],
)
