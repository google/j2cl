package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

java_library(
    name = "ast",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast/annotations",
        "//transpiler/java/com/google/j2cl/ast/common",
        "//transpiler/java/com/google/j2cl/ast/sourcemap",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/problems",
    ],
)
