package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "frontend",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:apache_commons_lang3",
        "//third_party:args4j",
        "//third_party:guava",
        "//third_party:gwt-jsinterop-annotations",
        "//third_party:jdt-core",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/ast/common",
        "//transpiler/java/com/google/j2cl/ast/sourcemap",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/problems",
    ],
)
