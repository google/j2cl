load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler:__subpackages__"],
    licenses = ["notice"],
)

kt_jvm_library(
    name = "lower",
    srcs = glob(["*.kt"]),
    deps = [
        "//java/com/google/common/collect",
        # Temporary dependency for the differ in DebugUtils. Should be removed
        # before the kotlin frontend is open-sourced.
        "//java/com/google/wrappers/util/diff:rediff",
        "//third_party:kotlin_compiler",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/kotlin/ir",
        "//third_party/kotlin/kotlin:kotlin_reflect",
    ],
)
