load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//transpiler/javatests/com/google/j2cl/transpiler/backend/kotlin:__subpackages__",
    ],
    licenses = ["notice"],
)

kt_jvm_library(
    name = "testing",
    testonly = True,
    srcs = glob(["*.kt"]),
    deps = [
        "//third_party:truth",
        "//transpiler/java/com/google/j2cl/transpiler/backend/kotlin/source",
    ],
)
