package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "declarelegacynamespace",
    srcs = glob(["*.java"]),
    _declare_legacy_namespace = True,
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations-j2cl"],
)
