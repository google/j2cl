# Description:
#   Jsni to native js file converter tool code.

package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

java_binary(
    name = "JsniConverter",
    main_class = "com.google.j2cl.tools.jsni.Runner",
    visibility = [
        "//:__subpackages__",
        "//third_party/java_src/jbox2d:__subpackages__",
    ],
    runtime_deps = [":jsni"],
)

java_library(
    name = "jsni",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:args4j",
        "//third_party:guava",
        "//third_party:jdt-core",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend",
    ],
)
