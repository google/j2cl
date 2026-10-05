load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    licenses = ["reciprocal"],
)

integration_library(
    name = "dependency",
    srcs = ["AutoValueDependency.java"],
    deps = [
        "//third_party:auto_value",
    ],
)

# TODO(b/264934554): Port this test to Kotlin when AutoValue is supported.
integration_test(
    srcs = glob(
        ["*.java"],
        exclude = ["AutoValueDependency.java"],
    ),
    closure_defines = {"jre.classMetadata": "'STRIPPED'"},
    deps = [
        ":dependency",
        "//third_party:auto_value",
    ],
)
