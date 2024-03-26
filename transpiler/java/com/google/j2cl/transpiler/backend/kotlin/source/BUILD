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
    name = "source",
    srcs = glob(["*.kt"]),
    deps = [
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/backend/common",
    ],
)
