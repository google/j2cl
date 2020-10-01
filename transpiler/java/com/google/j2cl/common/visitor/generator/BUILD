package(
    default_visibility = ["//transpiler:__subpackages__"],
    licenses = ["notice"],  # Apache 2.0
)

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
        "//transpiler/java/com/google/j2cl/common/visitor:visitor-internal",
    ],
)

java_plugin(
    name = "visitor-apt-processor",
    generates_api = 1,
    processor_class = "com.google.j2cl.common.visitor.generator.J2clAstProcessor",
    deps = [":processor-components"],
)
