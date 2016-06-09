package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "ast",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/apache_commons_lang3",
        "//third_party/java/auto:auto_value",
        "//third_party/java/jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast/processors:visitor-codegen",
        "//transpiler/java/com/google/j2cl/ast/sourcemap",
        "//transpiler/java/com/google/j2cl/errors",
    ],
)
