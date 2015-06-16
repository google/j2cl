package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "ast_processor",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/auto:auto_service",
        "//third_party/java/auto:common",
        "//third_party/java/jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast",
    ],
)
