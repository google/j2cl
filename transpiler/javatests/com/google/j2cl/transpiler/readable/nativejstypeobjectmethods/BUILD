package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "nativejstypeobjectmethods",
    srcs = glob(["*.java"]),
    js_deps = [":native_types"],
    deps = ["//jre/java:gwt-jsinterop-annotations"],
)

js_library(
    name = "native_types",
    srcs = glob(["native_sources/*.js"]),
)
