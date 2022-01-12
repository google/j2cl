load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(
    default_visibility = ["//transpiler:__subpackages__"],
    licenses = ["notice"],  # Apache 2.0
)

kt_jvm_library(
    name = "kotlin",
    srcs = glob(["*.kt"]),
    # We cannot use :kotlin_compiler as build dependency because it will
    # conflict with the kotlin runtime used to compile this target.
    runtime_deps = ["//third_party:kotlin_compiler"],
    deps = [
        "//third_party:guava",
        "//third_party:kotlin_compiler_inlineable",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
    ],
)
