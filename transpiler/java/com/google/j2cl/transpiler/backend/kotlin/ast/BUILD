load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//transpiler/java/com/google/j2cl/transpiler/backend/kotlin:__subpackages__",
        "//transpiler/javatests/com/google/j2cl/transpiler/backend/kotlin:__subpackages__",
    ],
    licenses = ["notice"],
)

kt_jvm_library(
    name = "ast",
    srcs = glob(["*.kt"]),
    deps = [
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/backend/kotlin/common",
    ],
)
