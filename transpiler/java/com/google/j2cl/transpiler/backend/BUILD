# Description:
#   This package contains backend driver code.

package(
    default_visibility = [
        "//transpiler:__subpackages__",
    ],
    licenses = ["notice"],
)

java_library(
    name = "backend",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/backend/closure",
        "//transpiler/java/com/google/j2cl/transpiler/passes",
    ],
)
