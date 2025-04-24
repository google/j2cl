# Description:
#   This package contains frontend common code that is agnostic of the actual frontend used.

load("@rules_java//java:defs.bzl", "java_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//transpiler/java/com/google/j2cl/transpiler:__pkg__",
        "//transpiler/java/com/google/j2cl/transpiler/frontend:__subpackages__",
    ],
    licenses = ["notice"],
)

java_library(
    name = "common",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:asm",
        "//third_party:auto_value",
        "//third_party:error_prone_annotations",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
    ],
)
