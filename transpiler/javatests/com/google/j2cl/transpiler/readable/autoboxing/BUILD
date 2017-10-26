# Apache2
licenses(["notice"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "autoboxing",
    srcs = glob(["*.java"]),
    javacopts = [
        "-Xep:IdentityBinaryExpression:OFF",
        "-Xep:SelfEquality:OFF",
    ],
    deps = [
        "//third_party/java/gwt:gwt-javaemul-internal-annotations-j2cl",
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
    ],
)
