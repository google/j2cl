load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//transpiler/java/com/google/j2cl/transpiler/frontend:__subpackages__",
    ],
    licenses = ["notice"],
)

kt_jvm_library(
    name = "kotlin",
    srcs = glob(["*.kt"]),
    deps = [
        "//third_party:guava",
        "//third_party:jdt-core",
        "//third_party:kotlin_autofriends",
        "//third_party:kotlin_compiler",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/jdt",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/kotlin/ir",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/kotlin/lower",
    ],
)
