load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# TODO(b/264934554): Port this test to Kotlin when AutoValue is supported.
integration_test(
    name = "autovalue",
    srcs = glob(["*.java"]),
    closure_defines = {"jre.classMetadata": "'STRIPPED'"},
    deps = [
        "//third_party:auto_value",
        "//transpiler/javatests/com/google/j2cl/integration/java/autovalue",
    ],
)
