# Description:
#   This builds the gwtincompatible stripping tool.  This tool looks for @GwtIncompatible
#   annotations on source code elements and comments them out so that they do not get parsed by
#   J2CL.

package(default_visibility = [
    "//third_party/java/j2cl:__subpackages__",
    "//:__subpackages__",
])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "gwtincompatible",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/apache_commons_lang3",
        "//third_party/java/args4j",
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/jsr305_annotations",
        "//:jdt-core-stable",
        "//transpiler/java/com/google/j2cl/frontend/common",
        "//transpiler/java/com/google/j2cl/problems",
    ],
)
