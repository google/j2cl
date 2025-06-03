load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "nullability",
    srcs = glob(["*.java"]),
    enable_nullability = True,
    deps = ["//third_party:jspecify_annotations"],
)
