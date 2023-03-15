load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler:__subpackages__"],
    licenses = ["notice"],
)

kt_jvm_library(
    name = "kotlin",
    srcs = glob(["*.kt"]),
    deps = [
        "//third_party:guava",
        "//third_party:kotlin_compiler",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
    ],
)
