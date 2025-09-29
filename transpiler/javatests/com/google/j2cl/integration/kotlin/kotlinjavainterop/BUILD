load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_library(
    name = "java_dep",
    srcs = glob(["*.java"]),
    enable_nullability = True,
    deps = [
        "//third_party:jspecify_annotations",
    ],
)

integration_test(
    srcs = glob(["*.kt"]),
    enable_kt = False,
    enable_wasm = False,
    deps = [":java_dep"],
)
