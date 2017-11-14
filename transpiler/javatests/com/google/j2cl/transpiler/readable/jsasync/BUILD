# Apache2
licenses(["notice"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "jsasync",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party/java/elemental2:elemental2-promise-j2cl",
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
    ],
)
