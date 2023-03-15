load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler:__subpackages__"],
    licenses = ["notice"],
)

kt_jvm_library(
    name = "objc",
    srcs = glob(["*.kt"]),
    deps = [
        "//transpiler/java/com/google/j2cl/transpiler/backend/kotlin/source",
    ],
)
