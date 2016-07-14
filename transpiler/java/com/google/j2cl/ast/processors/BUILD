package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "processor-components",
    srcs = glob(["*.java"]),
    resources = glob(["*.vm"]),
    deps = [
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/auto:auto_service",
        "//third_party/java/auto:common",
        "//third_party/java/jakarta_velocity",
        "//third_party/java/jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast/annotations:annotations-internal",
    ],
)

java_plugin(
    name = "visitor-apt-processor",
    processor_class = "com.google.j2cl.ast.processors.J2clAstProcessor",
    deps = [":processor-components"],
)
