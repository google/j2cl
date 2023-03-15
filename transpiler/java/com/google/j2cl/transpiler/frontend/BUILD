# Description:
#   This package contains frontend driver code.

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
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/javac",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/jdt",
    ],
)
