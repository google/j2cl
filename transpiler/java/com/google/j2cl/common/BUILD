package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "common",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/eclipse:eclipse-jdt-core",
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/jakarta_velocity",
        "//third_party/java/jsr305_annotations",
        "//transpiler/java/com/google/j2cl/errors",
    ],
)
