# Apache2
licenses(["notice"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "instancefieldinitializer",
    srcs = glob(["*.java"]),
)

filegroup(
    name = "readable_js",
    data = glob(["*.js.txt"]),
)
