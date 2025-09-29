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
    # TODO(b/278942389): Enable when TestUtils.isJ2Kt() detects J2KT-web.
    #  The first failing test is testBox_numberAsDouble().
    enable_kt_web = False,
)
