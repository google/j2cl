package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

java_library(
    name = "processor-components",
    srcs = glob(["*.java"]),
    resources = glob(["*.vm"]),
    deps = [
        "//third_party:auto_common",
        "//third_party:auto_service",
        "//third_party:guava",
        "//third_party:jakarta_velocity",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast/annotations:annotations-internal",
    ],
)

java_plugin(
    name = "visitor-apt-processor",
    generates_api = 1,
    processor_class = "com.google.j2cl.ast.processors.J2clAstProcessor",
    deps = [":processor-components"],
)
