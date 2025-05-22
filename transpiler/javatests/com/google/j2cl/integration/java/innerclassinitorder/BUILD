load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "innerclassinitorder",
    srcs = glob(["*.java"]),
    # TODO(b/418843331): Extract cases with explicit qualifier in constructor
    #  calls to a separate integration test which will be disabled for Kotlin
    #  only, and then enable this test.
    enable_kt = False,
)
