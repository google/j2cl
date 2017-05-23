package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "common",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:apache_commons_lang3",
        "//third_party:guava",
        "//third_party:jdt-core",
        "//transpiler/java/com/google/j2cl/problems",
    ],
)
