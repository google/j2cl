package(
    default_visibility = ["//transpiler:__subpackages__"],
    licenses = ["notice"],  # Apache 2.0
)

java_library(
    name = "passes",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:auto_value",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
    ],
)
