# Description:
#   This package contains frontend that uses Eclispe/JDT to parse, typecheck and resolve Java code.

load("@rules_java//java:defs.bzl", "java_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//tools/java/com/google/j2cl/tools/gwtincompatible:__pkg__",
        "//transpiler/java/com/google/j2cl/transpiler:__pkg__",
        "//transpiler/java/com/google/j2cl/transpiler/frontend:__subpackages__",
    ],
    licenses = ["notice"],
)

java_library(
    name = "jdt",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:auto_value",
        "//third_party:error_prone_annotations",
        "//third_party:guava",
        "//third_party:jdt-core",
        "//third_party:jsinterop-annotations",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
    ],
)
