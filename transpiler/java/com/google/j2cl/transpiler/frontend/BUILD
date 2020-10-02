# Description:
#   This package contains frontend driver code.

package(
    default_visibility = [
        "//transpiler:__subpackages__",
    ],
    licenses = ["notice"],
)

java_library(
    name = "frontend",
    srcs = glob(["*.java"]),
    deps = [
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/javac",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/jdt",
    ],
)
