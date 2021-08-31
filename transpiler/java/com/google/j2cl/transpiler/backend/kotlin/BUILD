load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(
    default_visibility = ["//transpiler:__subpackages__"],
    licenses = ["notice"],  # Apache 2.0
)

kt_jvm_library(
    name = "kotlin",
    srcs = glob([
        "*.java",
        "*.kt",
    ]),
    deps = [
        "//third_party:gson",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/backend/common",
    ],
)
