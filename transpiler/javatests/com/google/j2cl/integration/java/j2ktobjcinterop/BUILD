load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "j2ktobjcinterop",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:j2objc_annotations",
    ],
)
