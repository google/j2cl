load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "fieldshadowing",
    srcs = glob(["*.java"]),
    # TODO(b/202433397): Render override modifier
    enable_kt = False,
)
