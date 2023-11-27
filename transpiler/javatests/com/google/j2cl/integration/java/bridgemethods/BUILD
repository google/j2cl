load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "bridgemethods",
    srcs = glob(["*.java"]),
    # TODO(b/256681746): Improve TestUtils to detect Kotlin and fix the test.
    enable_kt_native = False,
    # TODO(b/246332093): Enable when bridge for parametrized method are
    #  generated correctly.
    enable_kt_web = False,
    suppress = ["JSC_TYPE_MISMATCH"],
)
