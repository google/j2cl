load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler/java/com/google/j2cl/transpiler/frontend/kotlin:__subpackages__"],
    licenses = ["notice"],
)

kt_jvm_library(
    name = "ir",
    srcs = glob(["*.kt"]),
    deps = [
        "//java/com/google/common/base",
        "//third_party:kotlin_compiler",
        "//third_party/kotlin/kotlin:kotlin_reflect",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
    ],
)
