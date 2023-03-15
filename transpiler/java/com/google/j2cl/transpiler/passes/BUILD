package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler:__subpackages__"],
    licenses = ["notice"],
)

java_library(
    name = "passes",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/common/visitor",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
    ],
)
