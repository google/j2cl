load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "box2d",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.checks.checkLevel": "NORMAL",
    },
    enable_bundle_size_report = True,
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/java/box2d",
    ],
)
