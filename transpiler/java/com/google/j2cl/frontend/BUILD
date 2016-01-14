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
        "//java/com/google/common/io",
        "//third_party/java/apache_commons_lang3",
        "//third_party/java/args4j",
        "//third_party/java/eclipse:eclipse-jdt-core",
        "//third_party/java/jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/errors",
        "//transpiler/java/com/google/j2cl/sourcemaps:tracking_interface",
    ],
)
