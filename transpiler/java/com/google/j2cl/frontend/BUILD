# Description:
#   This package contains frontend driver code.

package(
    default_visibility = [
        "//transpiler:__subpackages__",
    ],
    licenses = ["notice"],  # Apache 2.0
)

java_library(
    name = "frontend",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend/common",
        "//transpiler/java/com/google/j2cl/frontend/javac",
        "//transpiler/java/com/google/j2cl/frontend/jdt",
    ],
)
