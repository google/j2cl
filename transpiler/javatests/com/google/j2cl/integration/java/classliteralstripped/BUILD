load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "classliteralstripped",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.classMetadata": "'STRIPPED'",
        "goog.DEBUG": "false",  # Disable goog.DEBUG to avoid helping names to be unique.
        "goog.DISALLOW_TEST_ONLY_CODE": "false",
    },
    # Class literal stripping is only applicable for JS output.
    enable_jvm_test = False,
    # TODO(b/202428351): Generics support
    enable_kt = False,
)
