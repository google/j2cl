# Description:
#   This package contains frontend driver code.

load("@rules_java//java:defs.bzl", "java_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//transpiler:__subpackages__",
    ],
    licenses = ["notice"],
)

java_library(
    name = "frontend",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/javac",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/jdt",
    ],
)
