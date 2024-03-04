load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "systemgetproperty",
    srcs = glob(["*.java"]),
    # This test is specific to J2CL's implementation of System.getProperty().
    enable_jvm_test = False,
    enable_kt = False,
)
