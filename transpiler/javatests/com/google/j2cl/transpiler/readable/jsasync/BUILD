# Apache2
licenses(["notice"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    srcs = glob(["*.java"]),
    defs = [
        "--allow_method_call_decomposing",
        "--nocollapse_properties",  # TODO(skill): remove once b/69456597 is fixed
    ],
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
