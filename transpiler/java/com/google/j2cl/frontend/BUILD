package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "frontend",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/args4j",
        "//third_party/java/eclipse:eclipse-jdt-core-4.4",
        "//third_party/java/jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/ast/visitors",
        "//transpiler/java/com/google/j2cl/errors",
    ],
)
