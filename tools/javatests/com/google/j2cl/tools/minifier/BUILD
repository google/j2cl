# Description:
#  Tests for a minifier library for J2CL mangled names.

package(default_visibility = [
    "//third_party/java/j2cl:__pkg__",
    "//:__subpackages__",
])

# Apache2
licenses(["notice"])

java_test(
    name = "J2clMinifierTest",
    srcs = glob(["*.java"]),
    data = [
        "//transpiler/javatests/com/google/j2cl/transpiler/readable/arithmeticexpressions:readable_js",
        "//transpiler/javatests/com/google/j2cl/transpiler/readable/implementsgenericinterface:readable_js",
        "//transpiler/javatests/com/google/j2cl/transpiler/readable/instancefieldinitializer:readable_js",
        "//transpiler/javatests/com/google/j2cl/transpiler/readable/staticfieldinitializer:readable_js",
    ],
    deps = [
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//java/com/google/devtools/build/runtime:runfiles",
        "//third_party/java/junit",
        "//tools/java/com/google/j2cl/tools/minifier",
    ],
)
